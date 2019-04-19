package vldb17;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.validator.Validator;
import vldb17.set.JoinPkduckOriginal;

public class GreedyValidatorOriginal extends Validator{
	
	private long nCorrect = 0;
	private final double theta;
	
	private final static Boolean getTime = false;
	private final static Boolean debugPrint = false;
	public long ts;
	public long totalTime = 0;
	public long ruleCopyTime = 0;
	public long computeScoreTime = 0;
	public long bestRuleTime = 0;
	public long removeRuleTime = 0;
	public long bTransformTime = 0;
	public long reconstTime = 0;
	public long compareTime = 0;
	
	private static final boolean debug_print_transform = false;
	
	public GreedyValidatorOriginal(double theta) {
		this.theta = theta;
	}

	@Override
	public int isEqual( Record x, Record y ) {
		/*
		 * -1: not equivalent
		 * 0: exactly same
		 * 1: equivalent, x -> y
		 * 2: equivalent, y -> x
		 */
		++checked;
		ts = System.nanoTime();
		int res;
		if( areSameString( x, y )) res = 0;
		else {
			double simx2y = getSimL2R( x, y, false );
			if ( simx2y >= theta ) res = 1;
//			else {
//				double simy2x = getSimL2R( y, x, false );
//				if ( simy2x >= theta ) res = 2;
//				else res = -1;
//			}
			else res = -1;
		}
		totalTime += System.nanoTime() - ts;
		// print output for debugging
		if ( debug_print_transform && (res == 1 || res == 2) ) {
			JoinPkduckOriginal.pw.println(x.getID() +"\t" + x.toString()+"\t"+Arrays.toString(x.getTokensArray())+"\n"+y.getID()+"\t"+y.toString()+"\t"+Arrays.toString(y.getTokensArray()));
			if ( res == 1 ) getSimL2R( x, y, true );
//			else getSimL2R( y, x, true );
		}
		return res;
	}
	
	public double getSimL2R( Record x, Record y, boolean expPrint ) {
		if ( expPrint ) {
			JoinPkduckOriginal.pw.println(x.getID()+" -> "+y.getID());
		}
		// Make a copy of applicable rules to x.
		List<PosRule> candidateRules = new ObjectArrayList<PosRule>( x.getNumApplicableRules() );
		for (int i=0; i<x.size(); i++) {
			for (Rule rule : x.getSuffixApplicableRules( i )) {
				candidateRules.add( new PosRule(rule, i) );
				if (expPrint) JoinPkduckOriginal.pw.println("cand rule: "+rule+"\t"+i);
			}
		}
		if (getTime) ruleCopyTime += System.nanoTime() - ts;
		
		Boolean[] bAvailable = new Boolean[x.size()];
		Arrays.fill( bAvailable, true );
		Boolean bTransformedAll = false;
		Set<Integer> tokenSet = new IntOpenHashSet( y.getTokensArray() );
		Set<PosRule> appliedRuleSet = new ObjectOpenHashSet<PosRule>();
		
		while (!bTransformedAll) {
			if (getTime) ts = System.nanoTime();
			if (debugPrint) System.out.println( "loop start" );
			if (debugPrint) System.out.println( "bAvailable: "+Arrays.toString( bAvailable ) );
			// Compute scores of the remaining candidate rules.
			float bestScore = -1;
			int bestRuleIdx = -1;
			for (int i=0; i<candidateRules.size(); i++) {
				PosRule rule = candidateRules.get( i );
				float score = 0;
				for (Integer token : rule.getRight()) {
					if ( tokenSet.contains( token ) ) score++;
				}
				score /= rule.rightSize();
				if (score > bestScore) {
					bestScore = score;
					bestRuleIdx = i;
				}
				
				if (debugPrint) System.out.println( rule.rule.toOriginalString(Record.tokenIndex)+"\t"+rule.pos+"\t"+score+"\t"+bestScore+"\t"+bestRuleIdx );
//				if (bestScore == 1) break;
			}
			
			if (getTime) {
				computeScoreTime += System.nanoTime() - ts;
				ts = System.nanoTime();
			}
			
			// Apply a rule with the largest score.
			PosRule bestRule = candidateRules.get( bestRuleIdx );
			for (int j=0; j<bestRule.leftSize(); j++) bAvailable[bestRule.pos-j] = false;
			candidateRules.remove( bestRuleIdx );
			appliedRuleSet.add( bestRule );
			if ( expPrint && !bestRule.rule.isSelfRule() ) JoinPkduckOriginal.pw.println( "APPLY"+(bestRule.rule.isSelfRule()?"":"*")+": "+bestRule.rule.toOriginalString(Record.tokenIndex));

			if (getTime) {
				bestRuleTime += System.nanoTime() - ts;
				ts = System.nanoTime();
			}
			
			// Update the remaining token set.
			for (Integer token : bestRule.getRight()) tokenSet.remove( token );
			
			// If the rule is not applicable anymore, remove it.
			for (int i=0; i<candidateRules.size(); i++) {
				PosRule rule = candidateRules.get( i );
				Boolean isValid = true;
				for (int j=0; j<rule.leftSize(); j++) isValid &= bAvailable[rule.pos-j];
				if (!isValid) candidateRules.remove( i-- );
			}
			
			if (getTime) {
				removeRuleTime += System.nanoTime() - ts;
				ts = System.nanoTime();
			}
			
			// Update bTransformedAll.
			bTransformedAll = true;
			for (int i=0; i<x.size(); i++) bTransformedAll &= !bAvailable[i];
			if (debugPrint) System.out.println( "bTransformedAll: "+bTransformedAll );

			if (getTime) {
				bTransformTime += System.nanoTime() - ts;
				ts = System.nanoTime();
			}
		} // end while

		// Construct the transformed string.
		int transformedSize = 0;
		for (PosRule rule : appliedRuleSet) transformedSize += rule.rightSize();
		int[] transformedRecord = new int[transformedSize];

		if (debugPrint) {
			System.out.println( "transformedSize: "+transformedSize );
			for (PosRule rule : appliedRuleSet) {
				System.out.println( rule.rule.toOriginalString(Record.tokenIndex)+", "+rule.pos );
			}
		}
		if (expPrint) {
			for (PosRule rule : appliedRuleSet) {
				JoinPkduckOriginal.pw.println( rule.rule+", "+rule.pos );
			}
		}
		
		for (int i=x.size()-1, j=transformedSize-1; i>=0;) {
			for (PosRule rule : appliedRuleSet) {
				if ( rule.pos == i ) {
					if (debugPrint) System.out.println( rule.rule.toOriginalString(Record.tokenIndex)+", "+rule.pos );
					for (int k=0; k<rule.rightSize(); k++) {
						if (debugPrint) System.out.println( i+"\t"+j+"\t"+k+"\t"+(j-k)+"\t"+(rule.rightSize()-k-1) );
						transformedRecord[j-k] = rule.getRight()[rule.rightSize()-k-1];
					}
					i -= rule.leftSize();
					j -= rule.rightSize();
					break;
				}
			}
		}

		if (getTime) {
			reconstTime += System.nanoTime() - ts;
			ts = System.nanoTime();
		}

		if (debugPrint) System.out.println( Arrays.toString( transformedRecord ) );
		if (expPrint) JoinPkduckOriginal.pw.println("transformed: "+(new Record(transformedRecord)).toString(Record.tokenIndex));
		IntOpenHashSet setTrans = new IntOpenHashSet(transformedRecord);
		IntOpenHashSet setY = new IntOpenHashSet(y.getTokens());
		double sim = Util.jaccard( transformedRecord, y.getTokensArray());
		if ( expPrint ) {
			JoinPkduckOriginal.pw.println("SIM: "+sim);
			JoinPkduckOriginal.pw.flush();
		}

		if (getTime) {
			compareTime += System.nanoTime() - ts;
			ts = System.nanoTime();
		}

		return sim;
	}
	
//	public int isEqual( Record x, Record y, Boolean bIsEqual ) {
//		int res = isEqual(x, y);
//		if (bIsEqual && res>=0) nCorrect++;
//		return res;
//	}
	
	@Override
	public void addStat( StatContainer stat ) {
		super.addStat( stat );
		stat.add( "Val_Correct_pair", nCorrect );
	}
	
	@Override
	public String getName() {
		return "GreedyValidatorOriginal";
	}

	private class PosRule {
		public Rule rule;
		public int pos;
		
		public PosRule( Rule rule, int pos ) {
			this.rule = rule;
			this.pos = pos;
		}
		
		public int leftSize() {
			return rule.leftSize();
		}
		
		public int rightSize() {
			return rule.rightSize();
		}
		
		public int[] getLeft() {
			return rule.getLeft();
		}
		
		public int[] getRight() {
			return rule.getRight();
		}
		
		@Override
		public int hashCode() {
			return rule.hashCode();
		}
	}
}
