package vldb17.seq;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.order.AbstractGlobalOrder;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.QGram;

@Deprecated
public class PkduckDPTopDownWithRC extends PkduckDPTopDown {

	private Map<IntegerPair, Map<IntegerPair, int[]>> rcTable;
	
	public PkduckDPTopDownWithRC( Record rec, AbstractGlobalOrder globalOrder ) {
		super( rec, globalOrder );
	}

	@Override
	public Boolean isInSigU( int target_token, int k ) {
		/*
		 * Compute g[o][i][l] for o=0,1, i=0~|rec|, l=0~max(|recS|).
		 * g[1][i][l] is X_l in the MIT paper.
		 */
		
		this.k = k;
		g.clear();

		// build the rule compression map.
		rcTable = getRCTable( rec, target_token, k );
		
		for (int l=1; l<=len_max_s; l++) {
			int val = isInSigURecursive( rec.size(), l, 1 );
			if ( val == 0 ) return true;
		}
		return false;
	}

	@Override
	public int isInSigURecursive( int i, int l, int o ) {
		
		// base cases.
		if ( i <= 0 && l <= 0 && o == 0 ) return 0;
		if ( i <= 0 || l <= 0 ) return inf;
		
		// memoization.
		IntTriple key = new IntTriple( i, l, o );
		if ( g.containsKey( key ) ) return g.getInt( key );

		// recursion.
		int current_token = tokens[i-1];
		int comp = globalOrder.compare( current_token, i-1, target_token, k );
		if ( o == 0 ) {
			// compute g[0][i][l].
//				System.out.println( "comp: "+comp );
//				System.out.println( "g[0]["+i+"]["+l+"]: "+g[0][i][l] );
			if ( comp != 0 ) update( isInSigURecursive( i-1, l-1, o ) + (comp==-1?1:0), i, l, o );
//				System.out.println( "g[0]["+(i-1)+"]["+(l-1)+"]: "+g[0][i-1][l-1] );
//				System.out.println( "g[0]["+i+"]["+l+"]: "+g[0][i][l] );
			Map<IntegerPair, int[]> map = rcTable.get( new IntegerPair(i,l) );
			for ( Entry<IntegerPair, int[]> entry : map.entrySet() ) {
				int aside = entry.getKey().i1;
				int wside = entry.getKey().i2;
				int[] num_smaller = entry.getValue();
				if ( i-aside >= 0 && l-wside >= 0 ) update( isInSigURecursive( i-aside, l-wside, o ) + num_smaller[1], i, l, o );
			}
		}
		else { // o == 1
			// compute g[1][i][l].
			if ( comp != 0 ) update( isInSigURecursive( i-1, l-1, o ) + (comp<0?1:0), i, l, o );
			else update( isInSigURecursive( i-1, l-1, 0 ), i, l, o );
	//				System.out.println( "g[1]["+i+"]["+l+"]: "+g[1][i][l] );
			Map<IntegerPair, int[]> map = rcTable.get( new IntegerPair(i,l) );
			for ( Entry<IntegerPair, int[]> entry : map.entrySet() ) {
				int aside = entry.getKey().i1;
				int wside = entry.getKey().i2;
				int[] num_smaller = entry.getValue();
				if ( i-aside >= 0 && l-wside >= 0 ) {
					update( isInSigURecursive( i-aside, l-wside, 1 ) + num_smaller[0], i, l, o );
					update( isInSigURecursive( i-aside, l-wside, 0 ) + num_smaller[2], i, l, o ); 
				}
			}
		}
		return g.getInt( key );
	}

	private Map<IntegerPair, Map<IntegerPair, int[]>> getRCTable( Record rec, int target_token, int k ) {
		/*
		 * Return the Rule Compression Table.
		 * The table consists of two cascaded map.
		 * The key is a list of integers (i, l, aside, wside).
		 * The value is a list of integers (n1, n2, n3), which are:
		 * 		n1 is the least number of smaller tokens for all applicable rules.
		 * 		n2 is the least number of smaller tokens for all applicable rules which DO NOT GENERATE the target pos q-gram.
		 * 		n3 is the least number of smaller tokens for all applicable rules which GENERATE the target pos q-gram.
		 */
		Map<IntegerPair, Map<IntegerPair, int[]>> rcTable = new Object2ObjectOpenHashMap<IntegerPair, Map<IntegerPair, int[]>>();
		for ( int i=1; i<=rec.size(); i++ ) {
			for ( int l=1; l<=len_max_s; l++ ) {
				Map<IntegerPair, int[]> map = new Object2ObjectOpenHashMap<IntegerPair, int[]>();
				for ( Rule rule : rec.getSuffixApplicableRules( i-1 ) ) {
					int[] rhs = rule.getRight();
					int num_smaller = 0;
					Boolean isValidF = true;
					Boolean isValidT = false;
					for ( int j=0; j<rhs.length; j++ ) {
						isValidF &= !(target_token == rhs[j] && l-rhs.length+j == k); 
						isValidT |= target_token == rhs[j]  && l-rhs.length+j == k;
						num_smaller += globalOrder.compare( rhs[j], l-rhs.length+j, target_token, k )==-1?1:0;
					}
					int aside = rule.leftSize();
					int wside = rule.rightSize();
					int[] num_smallerList;
					IntegerPair key = new IntegerPair(aside, wside);
					if ( map.get( key ) == null ) num_smallerList = new int[] {inf, inf, inf};
					else num_smallerList = map.get( key );
					num_smallerList[0] = Math.min( num_smallerList[0], num_smaller );
					if (isValidF) num_smallerList[1] = Math.min( num_smallerList[1], num_smaller );
					if (isValidT) num_smallerList[2] = Math.min( num_smallerList[2], num_smaller );
					map.put( key, num_smallerList );
				}
				rcTable.put( new IntegerPair(i, l), map );
			}
		}
		return rcTable;
	}
}
