package vldb17.seq;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.QGram;
import vldb17.ParamPkduck.GlobalOrder;

public class PkduckDPWithRC extends PkduckDP {
	
	private final RCTableSeq rcTable;
	
	private Boolean debug = false;
	
	public PkduckDPWithRC( Record rec, GlobalOrder globalOrder ) {
		super( rec, globalOrder );
		rcTable = new RCTableSeq( rec, globalOrder );
		if (rec.getID() == 0 ) debug = true;
		if (debug) System.out.println( rcTable );
	}

	@Override
	public Boolean isInSigU( QGram target_qgram, int k ) {
		/*
		 * Compute g[o][i][l] for o=0,1, i=0~|rec|, l=0~max(|recS|).
		 * g[1][i][l] is X_l in the MIT paper.
		 */
//		System.out.println( "PkduckIndex.isInSigU, "+target_qgram+", "+k );
		
		// initialize g.
		int[][][] g = new int[2][rec.size()+1][len_max_s+1];
		for (int o=0; o<2; o++) {
			for (int i=0; i<=rec.size(); i++ ) {
				Arrays.fill( g[o][i], inf ); // divide by 2 to prevent overflow
			}
		}
		g[0][0][0] = 0;

		// compute g[0][i][l].
		for (int i=1; i<=rec.size(); i++) {
			QGram current_qgram = availableQGrams.get( i-1 ).get( 0 );
			for (int l=1; l<=len_max_s; l++) {
				int comp = JoinPkduck.comparePosQGrams( current_qgram.qgram, i-1, target_qgram.qgram, k, globalOrder );
//				System.out.println( "comp: "+comp );
//				System.out.println( "g[0]["+i+"]["+l+"]: "+g[0][i][l] );
				if ( comp != 0 ) g[0][i][l] = Math.min( g[0][i][l], g[0][i-1][l-1] + (comp==-1?1:0) );
//				System.out.println( "g[0]["+(i-1)+"]["+(l-1)+"]: "+g[0][i-1][l-1] );
//				System.out.println( "g[0]["+i+"]["+l+"]: "+g[0][i][l] );
				Map<IntegerPair, RCTableSeq.RCEntry> map = rcTable.getMap( i, l );
				for ( Entry<IntegerPair, RCTableSeq.RCEntry> kvPair : map.entrySet() ) {
					int aside = kvPair.getKey().i1;
					int wside = kvPair.getKey().i2;
					RCTableSeq.RCEntry entry = kvPair.getValue();
					int num_smaller1 = entry.getSmaller( target_qgram.qgram, k, 1 );
					if ( i-aside >= 0 && l-wside >= 0 ) g[0][i][l] = Math.min(  g[0][i][l], g[0][i-aside][l-wside] + num_smaller1 );
				}
//				System.out.println( "g[0]["+i+"]["+l+"]: "+g[0][i][l] );
			}
		}
//		System.out.println(Arrays.deepToString(g[0]).replaceAll( "],", "]\n" ));
		
		// compute g[1][i][l].
		for (int i=1; i<=rec.size(); i++ ) {
			QGram current_qgram = availableQGrams.get( i-1 ).get( 0 );
			for (int l=1; l<=len_max_s; l++) {
				int comp = JoinPkduck.comparePosQGrams( current_qgram.qgram, i-1, target_qgram.qgram, k, globalOrder );
//				System.out.println( "comp: "+comp );
				if ( comp != 0 ) g[1][i][l] = Math.min( g[1][i][l], g[1][i-1][l-1] + (comp<0?1:0) );
				else g[1][i][l] = Math.min( g[1][i][l], g[0][i-1][l-1] );
//				System.out.println( "g[1]["+i+"]["+l+"]: "+g[1][i][l] );
				Map<IntegerPair, RCTableSeq.RCEntry> map = rcTable.getMap( i, l );
				for ( Entry<IntegerPair, RCTableSeq.RCEntry> kvPair : map.entrySet() ) {
					int aside = kvPair.getKey().i1;
					int wside = kvPair.getKey().i2;
					RCTableSeq.RCEntry entry = kvPair.getValue();
					int num_smaller0 = entry.getSmaller( target_qgram.qgram, k, 0 );
					int num_smaller2 = entry.getSmaller( target_qgram.qgram, k, 2 );
					if ( i-aside >= 0 && l-wside >= 0 ) {
						g[1][i][l] = Math.min(  g[1][i][l], g[1][i-aside][l-wside] + num_smaller0 );
						g[1][i][l] = Math.min(  g[1][i][l], g[0][i-aside][l-wside] + num_smaller2 );
					}
				}
//				System.out.println( "g[1]["+i+"]["+l+"]: "+g[1][i][l] );
			}
		}
		// DEBUG
		if (debug) System.out.println(Arrays.deepToString(g[0]).replaceAll( "],", "]\n" ));
		if (debug) System.out.println(Arrays.deepToString(g[1]).replaceAll( "],", "]\n" ));

		Boolean res = false;
		for (int l=1; l<=len_max_s; l++) res |= (g[1][rec.size()][l] == 0);
		return res;
	}
}
