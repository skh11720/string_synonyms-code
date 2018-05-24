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
import snu.kdd.synonym.synonymRev.order.AbstractGlobalOrder;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;

public class RCTableSet {
	/*
	 * The table consists of maps.
	 * The key is a list of integers (i, aside, wside).
	 * The value is an RCEntry which contains the "smaller" values.
	 */

	private List<Map<IntegerPair, RCEntry>> rcTable;
	protected final AbstractGlobalOrder globalOrder;

	public RCTableSet( Record rec, AbstractGlobalOrder globalOrder ) {
//		System.out.println( "Record: "+rec.getID()+", "+Arrays.toString( rec.getTokensArray() ) );
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

			// DEBUG
//			Boolean debug = false;
//			if ( rec.getID() == 161 ) debug = true;
//			if ( debug) {
//				System.out.println( "mapEquivRuleSet" );
//				for ( IntegerPair key : mapEquivRuleSet.keySet() ) {
//					System.out.println( key+": "+mapEquivRuleSet.get( key ).toString() );
//				}
//			}

			Map<IntegerPair, RCEntry> map = new Object2ObjectOpenHashMap<IntegerPair, RCEntry>();
			for ( IntegerPair key2 : mapEquivRuleSet.keySet()) {
//				if (debug) System.out.println( "key2: "+key2 );
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
		
		private int[] orderList;
		private int[][] smaller;
//		private int[] smallerF;
//		private int[] smallerT;
		
		public RCEntry( Set<Rule>	ruleSet ) {
//			System.out.println( "RCEntry.ruleSet: "+ruleSet.toString() );
			// Enumerate all pos qgrams generated from rules in the ruleSet.
			IntOpenHashSet orderSet = new IntOpenHashSet();
			for ( Rule rule : ruleSet ) {
//				System.out.println( "rule: "+rule );
				int[] rhs = rule.getRight();
				for (int j=0; j<rhs.length; j++) {
					orderSet.add( rhs[j] );
				}
			}
			orderSet.add( Integer.MIN_VALUE ); // left end
			orderSet.add( Integer.MAX_VALUE ); // right end
			orderList = new int[orderSet.size()];
			orderSet.toArray( orderList );
			Arrays.sort( orderList );
//			System.out.println( Arrays.toString( tokenList ) );
//			System.out.println( Arrays.toString( pqgramList ) );
			
			// Fill the arrays with bounding values at the both ends.
			smaller = new int[3][orderList.length];
//			smallerF = new int[tokenList.length];
//			smallerT = new int[tokenList.length];
			for ( int i=0; i<3; i++ ) Arrays.fill( smaller[i], Integer.MAX_VALUE-1 );
//			Arrays.fill( smallerF, Integer.MAX_VALUE );
//			Arrays.fill( smallerT, Integer.MAX_VALUE );
			for ( Rule rule : ruleSet ) {
				IntOpenHashSet rule_orderSet = new IntOpenHashSet();
				int[] rhs = rule.getRight();
				for ( int j=0; j<rhs.length; j++ ) rule_orderSet.add( rhs[j] );
				int[] rule_orderList = new int[rule_orderSet.size()];
				rule_orderSet.toArray( rule_orderList );
				Arrays.sort( rule_orderList );
//				System.out.println( Arrays.toString( rule_tokenList ) );
				int j = 0;
				int n_small = 0;
				// Note that both rule_pqgramList and pqgramList are sorted.
				for ( int k=1; k<orderList.length-1; k++ ) {
					smaller[0][k] = Math.min( smaller[0][k], n_small );
					if ( j >= rule_orderList.length ) {
						smaller[1][k] = Math.min( smaller[1][k], n_small );
						continue;
					}
//						System.out.println( pqgram+", "+pqgramList[k]+": "+pqgram.compareTo( pqgramList[k] ) );
					if ( rule_orderList[j] < orderList[k] ) { // rule_pqgram < pqgramList[k]
//						System.out.println( "ruleSet:" );
//						for ( Rule rule0 : ruleSet ) {
//							System.out.println( rule0 );
//							for ( int token : rule0.getRight() ) System.out.print( "("+token+", "+globalOrder.getOrder( token )+"), " );
//							System.out.println(  );
//						}
//						System.out.println( "rule: "+rule );
//						System.out.println( "rule_orderList: "+Arrays.toString( rule_orderList )+", "+j );
//						System.out.println( "orderList: "+Arrays.toString( orderList )+", "+k );
						throw new RuntimeException("Unexpected error");
					}
					else if ( rule_orderList[j] > orderList[k] ) { // rule_pqgram > pqgramList[k]
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
			int order = token;
//			if ( flag == 0 ) arr = smaller;
//			else if ( flag == 1 ) arr = smallerF;
//			else if ( flag == 2 ) arr = smallerT;
//			else throw new RuntimeException("Unexpected error");
			
			int l = 0;
			int r = orderList.length;
			while ( l < r ) {
				int m = (l+r)/2;
				if ( order < orderList[m] ) { // token < tokenList[m]
					r = m;
				}
				else if ( order > orderList[m] ) { // token > tokenList[m]
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
				if ( orderList[r] == order ) return arr[r];
				else return Integer.MAX_VALUE;
			}
			else if ( flag == 1 ) {
				if ( orderList[r] == order ) return arr[r];
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
			str += "tokenList: "+Arrays.toString( orderList ) +"\n";
			str += "smaller: "+Arrays.toString( smaller[0] ) +"\n";
			str += "smallerF: "+Arrays.toString( smaller[1] ) +"\n";
			str += "smallerT: "+Arrays.toString( smaller[2] ) +"\n";
			return str;
		}

		private void setBound() {
			smaller[0][0] = smaller[1][0] = 0;
			smaller[0][orderList.length-1] = smaller[0][orderList.length-2]+1;
			smaller[1][orderList.length-1] = smaller[1][orderList.length-2];
			smaller[2][0] = smaller[2][orderList.length-1] = Integer.MAX_VALUE;
		}

	}
}
