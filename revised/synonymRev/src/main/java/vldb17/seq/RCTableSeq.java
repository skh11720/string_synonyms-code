package vldb17.seq;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.order.QGramGlobalOrder;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.PosQGram;

public class RCTableSeq {
	/*
	 * The table consists of two cascaded map.
	 * The key is a list of integers (i, l, aside, wside).
	 * The value is an RCEntry which contains the "smaller" values.
	 */

	private Map<IntegerPair, Map<IntegerPair, RCEntry>> rcTable;
	protected final QGramGlobalOrder globalOrder;

	public RCTableSeq( Record rec, QGramGlobalOrder globalOrder ) {
		this.globalOrder = globalOrder;
		int len_max_s = rec.getMaxTransLength();
		rcTable = new Object2ObjectOpenHashMap<IntegerPair, Map<IntegerPair, RCEntry>>();
		for ( int i=1; i<=rec.size(); i++ ) {
			Map<IntegerPair, Set<Rule>> mapEquivRuleSet = new Object2ObjectOpenHashMap<IntegerPair, Set<Rule>>();
			for ( Rule rule : rec.getSuffixApplicableRules( i-1 ) ) {
				int aside = rule.leftSize();
				int wside = rule.rightSize();
				IntegerPair key2 = new IntegerPair(aside, wside);
				if ( !mapEquivRuleSet.containsKey( key2 ) ) mapEquivRuleSet.put( key2, new ObjectOpenHashSet<Rule>() );
				mapEquivRuleSet.get( key2 ).add( rule );
			}

			for ( int l=1; l<=len_max_s; l++ ) {
				Map<IntegerPair, RCEntry> map = new Object2ObjectOpenHashMap<IntegerPair, RCEntry>();
				for ( IntegerPair key2 : mapEquivRuleSet.keySet()) {
					if ( key2.i2 > l ) continue;
					RCEntry entry = new RCEntry( mapEquivRuleSet.get( key2 ), l );
					map.put( key2, entry );
				}
				IntegerPair key1 = new IntegerPair( i, l );
				rcTable.put( key1, map );
			} // end for l
		} // end for i
	}
	
	public Map<IntegerPair, RCEntry> getMap( int i, int l ) {
		return rcTable.get( new IntegerPair(i, l) );
	}

	@Override
	public String toString() {
		String str = "";
		for ( IntegerPair key1 : rcTable.keySet() ) {
			str += "key1: "+key1+"\n";
			Map<IntegerPair, RCEntry> map = rcTable.get( key1 );
			for ( IntegerPair key2 : map.keySet()) {
				str += "key2: "+key2+"\n";
				RCEntry entry = map.get( key2 );
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
		 */
		
		private PosQGram[] pqgramList;
		private int[] smaller;
		private int[] smallerF;
		private int[] smallerT;
		
		public RCEntry( Set<Rule>	ruleSet, int l ) {
//			System.out.println( "l: "+l );
			// Enumerate all pos qgrams generated from rules in the ruleSet.
			Set<PosQGram> pqgramSet = new ObjectOpenHashSet<>();
			for ( Rule rule : ruleSet ) {
//				System.out.println( "rule: "+rule );
				int[] rhs = rule.getRight();
				for (int j=0; j<rhs.length; j++) {
					pqgramSet.add( new PosQGram( Arrays.copyOfRange( rhs, j, j+1 ) , l-rhs.length+j ) );
				}
			}
			pqgramList = new PosQGram[pqgramSet.size()];
			pqgramSet.toArray( pqgramList );
			Arrays.sort( pqgramList, globalOrder.pqgramComparator );
//			System.out.println( Arrays.toString( pqgramList ) );
			
			// Fill the arrays.
			smaller = new int[pqgramList.length];
			smallerF = new int[pqgramList.length];
			smallerT = new int[pqgramList.length];
			Arrays.fill( smaller, Integer.MAX_VALUE );
			Arrays.fill( smallerF, Integer.MAX_VALUE );
			Arrays.fill( smallerT, Integer.MAX_VALUE );
			for ( Rule rule : ruleSet ) {
				int[] rhs = rule.getRight();
				PosQGram[] rule_pqgramList = new PosQGram[rhs.length];
				for ( int j=0; j<rhs.length; j++) rule_pqgramList[j] = new PosQGram( Arrays.copyOfRange( rhs, j, j+1 ), l-rhs.length+j );
				Arrays.sort( rule_pqgramList, globalOrder.pqgramComparator );
				int j = 0;
				int n_small = 0;
				// Note that both rule_pqgramList and pqgramList are sorted.
				for ( int k=0; k<pqgramList.length; k++ ) {
					smaller[k] = Math.min( smaller[k], n_small );
					if ( j >= rhs.length ) {
						smallerF[k] = Math.min( smallerF[k], n_small );
						continue;
					}
//						System.out.println( pqgram+", "+pqgramList[k]+": "+pqgram.compareTo( pqgramList[k] ) );
					int comp = globalOrder.compare( rule_pqgramList[j], pqgramList[k] );
					if ( comp == -1 ) { // rule_pqgram < pqgramList[k]
						throw new RuntimeException("Unexpected error");
					}
					else if ( comp == 1 ) { // rule_pqgram > pqgramList[k]
						smallerF[k] = Math.min( smallerF[k], n_small );
					}
					else { // rule_pqgram == pqgramList[k]
						smallerT[k] = Math.min( smallerT[k], n_small++ );
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
		
		public int getSmaller( PosQGram pqgram, int flag ) {
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
				if ( globalOrder.compare( pqgram, pqgramList[m] ) < 0 ) { // pqgram < pqgramList[m]
					r = m;
				}
				else if ( globalOrder.compare( pqgram, pqgramList[m] ) > 0 ) { // pqgram > pqgramList[m]
					l = m+1;
				}
				else { // pqgram == pqgramList[m]
					return arr[m];
				}
			}
			return arr[r];
		}

		public int getSmaller( int[] qgram, int pos, int flag ) {
			return getSmaller( new PosQGram(qgram, pos), flag );
		}

		@Override
		public String toString() {
			String str = "";
			str += "tokenList: "+Arrays.toString( pqgramList ) +"\n";
			str += "smaller: "+Arrays.toString( smaller ) +"\n";
			str += "smallerF: "+Arrays.toString( smallerF ) +"\n";
			str += "smallerT: "+Arrays.toString( smallerT ) +"\n";
			return str;
		}
	}
}
