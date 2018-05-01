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
	
	private final static Boolean debug = false;
	
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
			// Make a copy of applicable rules to x.
			List<PosRule> candidateRules = new ObjectArrayList<PosRule>( x.getNumApplicableRules() );
			for (int i=0; i<x.size(); i++) {
				for (Rule rule : x.getSuffixApplicableRules( i ))  {
					candidateRules.add( new PosRule(rule, i) );
				}
			}
			
			Boolean[] bAvailable = new Boolean[x.size()];
			Arrays.fill( bAvailable, true );
			Boolean bTransformedAll = false;
			Set<Integer> tokenSet = new IntOpenHashSet( y.getTokensArray() );
			Set<PosRule> appliedRuleSet = new ObjectOpenHashSet<PosRule>();
			
			while (!bTransformedAll) {
				if (debug) System.out.println( "loop start" );
				if (debug) System.out.println( "bAvailable: "+Arrays.toString( bAvailable ) );
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
					if (debug) System.out.println( rule.rule+"\t"+rule.pos+"\t"+score+"\t"+bestScore+"\t"+bestRuleIdx );
				}
				
				// Apply a rule with the largest score.
				PosRule bestRule = candidateRules.get( bestRuleIdx );
				for (int j=0; j<bestRule.leftSize(); j++) bAvailable[bestRule.pos-j] = false;
				candidateRules.remove( bestRuleIdx );
				appliedRuleSet.add( bestRule );
				
				// Update the remaining token set.
				for (Integer token : bestRule.getRight()) tokenSet.remove( token );
				
				// If the rule is not applicable anymore, remove it.
				for (int i=0; i<candidateRules.size(); i++) {
					PosRule rule = candidateRules.get( i );
					Boolean isValid = true;
					for (int j=0; j<rule.leftSize(); j++) isValid &= bAvailable[rule.pos-j];
					if (!isValid) candidateRules.remove( i-- );
				}
				
				// Update bTransformedAll.
				bTransformedAll = true;
				for (int i=0; i<x.size(); i++) bTransformedAll &= !bAvailable[i];
				if (debug) System.out.println( "bTransformedAll: "+bTransformedAll );
			} // end while
			
			// Construct the transformed string.
			int transformedSize = 0;
			for (PosRule rule : appliedRuleSet) transformedSize += rule.rightSize();
			int[] transformedRecord = new int[transformedSize];

			if (debug) {
				System.out.println( "transformedSize: "+transformedSize );
				for (PosRule rule : appliedRuleSet) {
					System.out.println( rule.rule+", "+rule.pos );
				}
			}
			
			for (int i=x.size()-1, j=transformedSize-1; i>=0;) {
				for (PosRule rule : appliedRuleSet) {
					if ( rule.pos == i ) {
						if (debug) System.out.println( rule.rule+", "+rule.pos );
						for (int k=0; k<rule.rightSize(); k++) {
							if (debug) System.out.println( i+"\t"+j+"\t"+k+"\t"+(j-k)+"\t"+(rule.rightSize()-k-1) );
							transformedRecord[j-k] = rule.getRight()[rule.rightSize()-k-1];
						}
						i -= rule.leftSize();
						j -= rule.rightSize();
						break;
					}
				}
			}

			if (debug) System.out.println( Arrays.toString( transformedRecord ) );
			if ( Arrays.equals( transformedRecord, y.getTokensArray() )) return 1;
			else return -1;
		}
		else {
			throw new RuntimeException("GreedyValidator currently does not accept bothSidejoin.");
		}
	}
	
//	public int isEqual( Record x, Record y, Boolean bIsIqual ) {
//		
//	}
	
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
	}
}
