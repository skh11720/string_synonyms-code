package vldb17.set;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import vldb17.ParamPkduck.GlobalOrder;

public class RCTableSet {
	/*
	 * The table consists of maps.
	 * The key is a list of integers (i, aside, wside).
	 * The value is an RCEntry which contains the "smaller" values.
	 */

	private List<Map<IntegerPair, RCEntry>> rcTable;
	protected final GlobalOrder globalOrder;

	public RCTableSet( Record rec, GlobalOrder globalOrder ) {
		this.globalOrder = globalOrder;
		rcTable = new ObjectArrayList<Map<IntegerPair, RCEntry>>();
		for ( int i=1; i<=rec.size(); i++ ) {
			Map<IntegerPair, Set<Rule>> mapEquivRuleSet = new Object2ObjectOpenHashMap<IntegerPair, Set<Rule>>();
			for ( Rule rule : rec.getSuffixApplicableRules( i-1 ) ) {
				int aside = rule.leftSize();
				int wside = rule.rightSize();
				IntegerPair key2 = new IntegerPair(aside, wside);
				if ( !mapEquivRuleSet.containsKey( key2 ) ) mapEquivRuleSet.put( key2, new ObjectOpenHashSet<Rule>() );
				mapEquivRuleSet.get( key2 ).add( rule );
			}

			Map<IntegerPair, RCEntry> map = new Object2ObjectOpenHashMap<IntegerPair, RCEntry>();
			for ( IntegerPair key2 : mapEquivRuleSet.keySet()) {
				RCEntry entry = new RCEntry( mapEquivRuleSet.get( key2 ) );
				map.put( key2, entry );
			}
			rcTable.add( map );
		} // end for i
	}
	
	public Map<IntegerPair, RCEntry> getMap( int i ) {
		return rcTable.get( i );
	}

	public class RCEntry {
		/*
		 * 	"smaller" contains the least number of smaller tokens for all applicable rules.
		 * 	"smallerF" contains the least number of smaller tokens for all applicable rules which DO NOT GENERATE the target pos q-gram.
		 * 	"smallerT" contains the least number of smaller tokens for all applicable rules which GENERATE the target pos q-gram.
		 */
		
		private int[] tokenList;
		private int[] smaller;
		private int[] smallerF;
		private int[] smallerT;
		
		public RCEntry( Set<Rule>	ruleSet ) {
			// Enumerate all pos qgrams generated from rules in the ruleSet.
			IntOpenHashSet tokenSet = new IntOpenHashSet();
			for ( Rule rule : ruleSet ) {
//				System.out.println( "rule: "+rule );
				int[] rhs = rule.getRight();
				for (int j=0; j<rhs.length; j++) {
					tokenSet.add( rhs[j] );
				}
			}
			tokenList = new int[tokenSet.size()];
			tokenSet.toArray();
			Arrays.sort( tokenList );
//			System.out.println( Arrays.toString( pqgramList ) );
			
			// Fill the arrays.
			smaller = new int[tokenList.length];
			smallerF = new int[tokenList.length];
			smallerT = new int[tokenList.length];
			Arrays.fill( smaller, Integer.MAX_VALUE );
			Arrays.fill( smallerF, Integer.MAX_VALUE );
			Arrays.fill( smallerT, Integer.MAX_VALUE );
			for ( Rule rule : ruleSet ) {
				IntOpenHashSet rule_tokenSet = new IntOpenHashSet( rule.getRight() );
				int[] rule_tokenList = new int[rule_tokenSet.size()];
				rule_tokenSet.toArray( rule_tokenList );
				Arrays.sort( rule_tokenList );
				int j = 0;
				int n_small = 0;
				// Note that both rule_pqgramList and pqgramList are sorted.
				for ( int k=0; k<tokenList.length; k++ ) {
					smaller[k] = Math.min( smaller[k], n_small );
					if ( j >= rule_tokenList.length ) {
						smallerF[k] = Math.min( smallerF[k], n_small );
						continue;
					}
//						System.out.println( pqgram+", "+pqgramList[k]+": "+pqgram.compareTo( pqgramList[k] ) );
					if ( rule_tokenList[j] < tokenList[k] ) { // rule_pqgram < pqgramList[k]
						throw new RuntimeException("Unexpected error");
					}
					else if ( rule_tokenList[j] > tokenList[k] ) { // rule_pqgram > pqgramList[k]
						smallerF[k] = Math.min( smallerF[k], n_small );
					}
					else { // rule_pqgram == pqgramList[k]
						smallerT[k] = Math.min( smallerF[k], n_small++ );
						++j;
					}
				}
//				System.out.println( "rule_pqgramList: "+Arrays.toString( rule_pqgramList ) );
			}
//			System.out.println( "pqgramList: "+Arrays.toString( pqgramList ) );
//			System.out.println( "smaller: "+Arrays.toString( smaller ) );
//			System.out.println( "smallerF: "+Arrays.toString( smallerF ) );
//			System.out.println( "smallerT: "+Arrays.toString( smallerT ) );
//			
//			int[] test_idx = {1, 2, 5, 7};
//			for ( int idx : test_idx ) {
//				if ( idx >= pqgramList.length ) break;
//				System.out.println( pqgramList[idx]+": "+getSmaller( pqgramList[idx], 0 )+", "+getSmaller( pqgramList[idx], 1 )+", "+getSmaller( pqgramList[idx], 2 ) );
//			}
		}
		
		public int getSmaller( int token, int flag ) {
			/*
			 * flag == 0: smaller
			 * flag == 1: smallerF
			 * flag == 2: smallerT
			 */
			int[] arr;
			if ( flag == 0 ) arr = smaller;
			else if ( flag == 1 ) arr = smallerF;
			else if ( flag == 2 ) arr = smallerT;
			else throw new RuntimeException("Unexpected error");
			
			int l = 0;
			int r = arr.length;
			while ( l < r ) {
				int m = (l+r)/2;
				if ( token < tokenList[m] ) { // token < tokenList[m]
					r = m;
				}
				else if ( token > tokenList[m] ) { // token > tokenList[m]
					l = m+1;
				}
				else { // token == tokenList[m]
					return arr[m];
				}
			}
			return arr[r];
		}
	}
}
