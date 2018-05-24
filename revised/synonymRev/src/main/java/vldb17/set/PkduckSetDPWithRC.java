package vldb17.set;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.order.AbstractGlobalOrder;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;

public class PkduckSetDPWithRC extends PkduckSetDP {
	
	private final RCTableSet rcTable;
//	private Boolean debug = false;

	public PkduckSetDPWithRC( Record rec, AbstractGlobalOrder globalOrder ) {
		super( rec, globalOrder );
		rcTable = new RCTableSet( rec, globalOrder );
//		if (rec.getID() == 0) debug = true;
//		if (debug) System.out.println( rcTable );
	}

	@Override
	public Boolean isInSigU( int target ) {
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

		// compute g[0][i][l].
		for (int i=1; i<=rec.size(); i++) {
			int token = tokens[i-1];
			int comp = globalOrder.compare( token, target );
			Map<IntegerPair, RCTableSet.RCEntry> map = rcTable.getMap( i-1 );
			for (int l=1; l<=len_max_S; l++) {
//				System.out.println( "comp: "+comp );
//				System.out.println( "g[0]["+i+"]["+l+"]: "+g[0][i][l] );
				if ( comp != 0 ) g[0][i][l] = Math.min( g[0][i][l], safeAdd(g[0][i-1][l-1], (comp<0?1:0) ) );
//				System.out.println( "g[0]["+(i-1)+"]["+(l-1)+"]: "+g[0][i-1][l-1] );
//				System.out.println( "g[0]["+i+"]["+l+"]: "+g[0][i][l] );
				for ( Entry<IntegerPair, RCTableSet.RCEntry> kvPair : map.entrySet() ) {
					int aside = kvPair.getKey().i1;
					int wside = kvPair.getKey().i2;
					RCTableSet.RCEntry entry = kvPair.getValue();
					int num_smaller1 = entry.getSmaller( target, 1 );
					if ( i-aside >= 0 && l-wside >= 0 ) g[0][i][l] = Math.min( g[0][i][l], safeAdd(g[0][i-aside][l-wside], num_smaller1) );
				}
//				System.out.println( "g[0]["+i+"]["+l+"]: "+g[0][i][l] );
			}
		}
//		System.out.println(Arrays.deepToString(g[0]).replaceAll( "],", "]\n" ));
		
		// compute g[1][i][l].
		for (int i=1; i<=rec.size(); i++ ) {
			int token = tokens[i-1];
			int comp = globalOrder.compare( token, target );
			Map<IntegerPair, RCTableSet.RCEntry> map = rcTable.getMap( i-1 );
			for (int l=1; l<=len_max_S; l++) {
//				System.out.println( "comp: "+comp );
				if ( comp != 0 ) g[1][i][l] = Math.min( g[1][i][l], safeAdd(g[1][i-1][l-1], (comp<0?1:0) ) );
				else g[1][i][l] = Math.min( g[1][i][l], g[0][i-1][l-1] );
//				System.out.println( "g[1]["+i+"]["+l+"]: "+g[1][i][l] );
				for ( Entry<IntegerPair, RCTableSet.RCEntry> kvPair : map.entrySet() ) {
					int aside = kvPair.getKey().i1;
					int wside = kvPair.getKey().i2;
					RCTableSet.RCEntry entry = kvPair.getValue();
					int num_smaller0 = entry.getSmaller( target, 0 );
					int num_smaller2 = entry.getSmaller( target, 2 );
					if ( i-aside >= 0 && l-wside >= 0 ) {
						g[1][i][l] = Math.min(  g[1][i][l], safeAdd(g[1][i-aside][l-wside], num_smaller0) );
						if ( num_smaller2 != Integer.MAX_VALUE ) g[1][i][l] = Math.min(  g[1][i][l], safeAdd(g[0][i-aside][l-wside], num_smaller2) );
					}
				}
//				System.out.println( "g[1]["+i+"]["+l+"]: "+g[1][i][l] );
			}
		}
//		System.out.println(Arrays.deepToString(g[1]).replaceAll( "],", "]\n" ));

		Boolean res = false;
		for (int l=1; l<=len_max_S; l++) res |= (g[1][rec.size()][l] == 0);
		
		// DEBUG
//		if (debug) System.out.println(Arrays.deepToString(g[0]).replaceAll( "],", "]\n" ));
//		if (debug) System.out.println(Arrays.deepToString(g[1]).replaceAll( "],", "]\n" ));
		return res;
	}
}
