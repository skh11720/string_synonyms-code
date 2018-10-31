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
import snu.kdd.synonym.synonymRev.validator.Validator;

public class GreedyValidator extends Validator{
	
	private final Boolean oneSideJoin;
	private long nCorrect = 0;
	
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
	
	public GreedyValidator(Boolean oneSideJoin) {
		this.oneSideJoin = oneSideJoin;
		if (!this.oneSideJoin) 
			throw new RuntimeException("GreedyValidator currently does not accept bothSidejoin.");
	}

	@Override
	public int isEqual( Record x, Record y ) {
		++checked;
		if( areSameString( x, y )) return 0; 
		
		if (this.oneSideJoin) {
			ts = System.nanoTime();
			// Make a copy of applicable rules to x.
			List<PosRule> candidateRules = new ObjectArrayList<PosRule>( x.getNumApplicableRules() );
			for (int i=0; i<x.size(); i++) {
				for (Rule rule : x.getSuffixApplicableRules( i )) {
					candidateRules.add( new PosRule(rule, i) );
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
					
					if (bestScore == 1) break;
					if (debugPrint) System.out.println( rule.rule+"\t"+rule.pos+"\t"+score+"\t"+bestScore+"\t"+bestRuleIdx );
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
					System.out.println( rule.rule+", "+rule.pos );
				}
			}
			
			for (int i=x.size()-1, j=transformedSize-1; i>=0;) {
				for (PosRule rule : appliedRuleSet) {
					if ( rule.pos == i ) {
						if (debugPrint) System.out.println( rule.rule+", "+rule.pos );
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
			Boolean res;
			res = Arrays.equals( transformedRecord, y.getTokensArray() );

			if (getTime) {
				compareTime += System.nanoTime() - ts;
				ts = System.nanoTime();
			}

			totalTime += System.nanoTime() - ts;
			if (res) return 1;
			else return -1;
		}
		else {
			throw new RuntimeException("GreedyValidator currently does not accept bothSidejoin.");
		}
	}
	
	public int isEqual( Record x, Record y, Boolean bIsEqual ) {
		int res = isEqual(x, y);
		if (bIsEqual && res>=0) nCorrect++;
		return res;
	}
	
	@Override
	public void addStat( StatContainer stat ) {
		super.addStat( stat );
		stat.add( "Val_Correct_pair", nCorrect );
	}
	
	@Override
	public String getName() {
		return "GreedyValidator";
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
