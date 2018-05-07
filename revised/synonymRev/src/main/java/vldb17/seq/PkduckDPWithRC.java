package vldb17.seq;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.QGram;
import vldb17.ParamPkduck.GlobalOrder;

public class PkduckDPWithRC extends PkduckDP {
	
	public PkduckDPWithRC( Record rec, GlobalOrder globalOrder, int len_max_S ) {
		super( rec, globalOrder, len_max_S	);
	}

	public PkduckDPWithRC( Record rec, JoinPkduck joinPkduck ) {
		super( rec, joinPkduck );
	}

	@Override
	public Boolean isInSigU( Record rec, QGram target_qgram, int k ) {
		/*
		 * Compute g[o][i][l] for o=0,1, i=0~|rec|, l=0~max(|recS|).
		 * g[1][i][l] is X_l in the MIT paper.
		 */
//		System.out.println( "PkduckIndex.isInSigU, "+target_qgram+", "+k );
		
		// initialize g.
		int[][][] g = new int[2][rec.size()+1][len_max_S+1];
		for (int o=0; o<2; o++) {
			for (int i=0; i<=rec.size(); i++ ) {
				Arrays.fill( g[o][i], inf ); // divide by 2 to prevent overflow
			}
		}
		g[0][0][0] = 0;
		
		// build the rule compression map.
		Map<IntegerPair, Map<IntegerPair, int[]>> rcTable = getRCTable( rec, target_qgram, k );

		// compute g[0][i][l].
		for (int i=1; i<=rec.size(); i++) {
			QGram current_qgram = availableQGrams.get( i-1 ).get( 0 );
			for (int l=1; l<=len_max_S; l++) {
				int comp = comparePosQGrams( current_qgram.qgram, i-1, target_qgram.qgram, k );
//				System.out.println( "comp: "+comp );
//				System.out.println( "g[0]["+i+"]["+l+"]: "+g[0][i][l] );
				if ( comp != 0 ) g[0][i][l] = Math.min( g[0][i][l], g[0][i-1][l-1] + (comp==-1?1:0) );
//				System.out.println( "g[0]["+(i-1)+"]["+(l-1)+"]: "+g[0][i-1][l-1] );
//				System.out.println( "g[0]["+i+"]["+l+"]: "+g[0][i][l] );
				Map<IntegerPair, int[]> map = rcTable.get( new IntegerPair(i,l) );
				for ( Entry<IntegerPair, int[]> entry : map.entrySet() ) {
					int aside = entry.getKey().i1;
					int wside = entry.getKey().i2;
					int[] num_smaller = entry.getValue();
					if ( i-aside >= 0 && l-wside >= 0 ) g[0][i][l] = Math.min(  g[0][i][l], g[0][i-aside][l-wside] + num_smaller[1] );
				}
//				System.out.println( "g[0]["+i+"]["+l+"]: "+g[0][i][l] );
			}
		}
//		System.out.println(Arrays.deepToString(g[0]).replaceAll( "],", "]\n" ));
		
		// compute g[1][i][l].
		for (int i=1; i<=rec.size(); i++ ) {
			QGram current_qgram = availableQGrams.get( i-1 ).get( 0 );
			for (int l=1; l<=len_max_S; l++) {
				int comp = comparePosQGrams( current_qgram.qgram, i-1, target_qgram.qgram, k );
//				System.out.println( "comp: "+comp );
				if ( comp != 0 ) g[1][i][l] = Math.min( g[1][i][l], g[1][i-1][l-1] + (comp<0?1:0) );
				else g[1][i][l] = Math.min( g[1][i][l], g[0][i-1][l-1] );
//				System.out.println( "g[1]["+i+"]["+l+"]: "+g[1][i][l] );
				Map<IntegerPair, int[]> map = rcTable.get( new IntegerPair(i,l) );
				for ( Entry<IntegerPair, int[]> entry : map.entrySet() ) {
					int aside = entry.getKey().i1;
					int wside = entry.getKey().i2;
					int[] num_smaller = entry.getValue();
					if ( i-aside >= 0 && l-wside >= 0 ) {
						g[1][i][l] = Math.min(  g[1][i][l], g[1][i-aside][l-wside] + num_smaller[0] );
						g[1][i][l] = Math.min(  g[1][i][l], g[0][i-aside][l-wside] + num_smaller[2] );
					}
				}
//				System.out.println( "g[1]["+i+"]["+l+"]: "+g[1][i][l] );
			}
		}
//		System.out.println(Arrays.deepToString(g[1]).replaceAll( "],", "]\n" ));

		Boolean res = false;
		for (int l=1; l<=len_max_S; l++) res |= (g[1][rec.size()][l] == 0);
		return res;
	}

	private Map<IntegerPair, Map<IntegerPair, int[]>> getRCTable( Record rec, QGram target_qgram, int k ) {
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
			for ( int l=1; l<=len_max_S; l++ ) {
				Map<IntegerPair, int[]> map = new Object2ObjectOpenHashMap<IntegerPair, int[]>();
				for ( Rule rule : rec.getSuffixApplicableRules( i-1 ) ) {
					int[] rhs = rule.getRight();
					int num_smaller = 0;
					Boolean isValidF = true;
					Boolean isValidT = false;
					for ( int j=0; j<rhs.length; j++ ) {
						isValidF &= !(target_qgram.equals( Arrays.copyOfRange( rhs, j, j+1 ) ) && l-rhs.length+j == k); 
						isValidT |= target_qgram.equals( Arrays.copyOfRange( rhs, j, j+1 ) ) && l-rhs.length+j == k;
						num_smaller += comparePosQGrams( Arrays.copyOfRange( rhs, j, j+1 ), l-rhs.length+j, target_qgram.qgram, k )==-1?1:0;
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
