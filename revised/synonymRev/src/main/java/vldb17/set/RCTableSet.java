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
import snu.kdd.synonym.synonymRev.order.QGramGlobalOrder;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;

public class RCTableSet {
	/*
	 * The table consists of maps.
	 * The key is a list of integers (i, aside, wside).
	 * The value is an RCEntry which contains the "smaller" values.
	 */

	private List<Map<IntegerPair, RCEntry>> rcTable;
	protected final QGramGlobalOrder globalOrder;

	public RCTableSet( Record rec, QGramGlobalOrder globalOrder ) {
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
	
	@Override
	public String toString() {
		String str = "";
		for ( int i=0; i<rcTable.size(); i++ ) {
			str += "pos: "+i+"\n";
			Map<IntegerPair, RCEntry> map = rcTable.get( i );
			for ( IntegerPair key : map.keySet()) {
				str += "key: "+key+"\n";
				RCEntry entry = map.get( key );
				str += entry.toString();
			}
		}
		return str;
	}
	
	public class RCEntry {
		/*
		 * 	"smaller" contains the least number of smaller tokens for all applicable rules.
		 * 	"smallerF" contains the least number of smaller tokens for all applicable rules which DO NOT GENERATE the target pos q-gram.
		 * 	"smallerT" contains the least number of smaller tokens for all applicable rules which GENERATE the target pos q-gram.
		 * 
		 * 	The three arrays are merged into a 2d-array "smaller".
		 */
		
		private int[] tokenList;
		private int[][] smaller;
//		private int[] smallerF;
//		private int[] smallerT;
		
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
			tokenSet.add( Integer.MIN_VALUE ); // left end
			tokenSet.add( Integer.MAX_VALUE ); // right end
			tokenList = new int[tokenSet.size()];
			tokenSet.toArray( tokenList );
			Arrays.sort( tokenList );
//			System.out.println( Arrays.toString( tokenList ) );
//			System.out.println( Arrays.toString( pqgramList ) );
			
			// Fill the arrays with bounding values at the both ends.
			smaller = new int[3][tokenList.length];
//			smallerF = new int[tokenList.length];
//			smallerT = new int[tokenList.length];
			for ( int i=0; i<3; i++ ) Arrays.fill( smaller[i], Integer.MAX_VALUE-1 );
//			Arrays.fill( smallerF, Integer.MAX_VALUE );
//			Arrays.fill( smallerT, Integer.MAX_VALUE );
			for ( Rule rule : ruleSet ) {
				IntOpenHashSet rule_tokenSet = new IntOpenHashSet( rule.getRight() );
				int[] rule_tokenList = new int[rule_tokenSet.size()];
				rule_tokenSet.toArray( rule_tokenList );
				Arrays.sort( rule_tokenList );
//				System.out.println( Arrays.toString( rule_tokenList ) );
				int j = 0;
				int n_small = 0;
				// Note that both rule_pqgramList and pqgramList are sorted.
				for ( int k=1; k<tokenList.length-1; k++ ) {
					smaller[0][k] = Math.min( smaller[0][k], n_small );
					if ( j >= rule_tokenList.length ) {
						smaller[1][k] = Math.min( smaller[1][k], n_small );
						continue;
					}
//						System.out.println( pqgram+", "+pqgramList[k]+": "+pqgram.compareTo( pqgramList[k] ) );
					if ( rule_tokenList[j] < tokenList[k] ) { // rule_pqgram < pqgramList[k]
						throw new RuntimeException("Unexpected error");
					}
					else if ( rule_tokenList[j] > tokenList[k] ) { // rule_pqgram > pqgramList[k]
						smaller[1][k] = Math.min( smaller[1][k], n_small );
					}
					else { // rule_pqgram == pqgramList[k]
						smaller[2][k] = Math.min( smaller[2][k], n_small++ );
						++j;
					}
				}
//				System.out.println( "rule_pqgramList: "+Arrays.toString( rule_pqgramList ) );
			}
			
			setBound();
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
			 * flag == 0: smaller[0]
			 * flag == 1: smaller[1]
			 * flag == 2: smaller[2]
			 */
			int[] arr = smaller[flag];
//			if ( flag == 0 ) arr = smaller;
//			else if ( flag == 1 ) arr = smallerF;
//			else if ( flag == 2 ) arr = smallerT;
//			else throw new RuntimeException("Unexpected error");
			
			int l = 0;
			int r = tokenList.length;
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
//			try {int a = arr[r];}
//			catch ( Exception e ) {
//				System.err.println( token );
//				System.err.println( Arrays.toString( tokenList ) );
//				System.err.println( Arrays.toString( smaller ) );
//				System.err.println( Arrays.toString( smallerF ) );
//				System.err.println( Arrays.toString( smallerT ) );
//				System.err.println( ""+l+", "+r );
//				System.exit( 1 );
//			}
			if ( flag == 2 ) {
				if ( tokenList[r] == token ) return arr[r];
				else return Integer.MAX_VALUE;
			}
			else if ( flag == 1 ) {
				if ( tokenList[r] == token ) return arr[r];
				else return smaller[0][r];
			}
			else return arr[r];
//			if ( r == arr.length ) return arr[r-1] + 1;
//			if ( flag == 2 ) return tokenList[r] == token? arr[r]: Integer.MAX_VALUE;
//			else return arr[r];
//			return arr[r];
		}
		
		@Override
		public String toString() {
			String str = "";
			str += "tokenList: "+Arrays.toString( tokenList ) +"\n";
			str += "smaller: "+Arrays.toString( smaller[0] ) +"\n";
			str += "smallerF: "+Arrays.toString( smaller[1] ) +"\n";
			str += "smallerT: "+Arrays.toString( smaller[2] ) +"\n";
			return str;
		}

		private void setBound() {
			smaller[0][0] = smaller[1][0] = 0;
			smaller[0][tokenList.length-1] = smaller[0][tokenList.length-2]+1;
			smaller[1][tokenList.length-1] = smaller[1][tokenList.length-2];
			smaller[2][0] = smaller[2][tokenList.length-1] = Integer.MAX_VALUE;
		}

	}
}
