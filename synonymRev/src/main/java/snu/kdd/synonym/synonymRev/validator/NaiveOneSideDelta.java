package snu.kdd.synonym.synonymRev.validator;

import java.util.Arrays;
import java.util.List;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.Util;

public class NaiveOneSideDelta extends Validator {
	
	private final int deltaMax;
	
	public NaiveOneSideDelta( int deltaMax ) {
		this.deltaMax = deltaMax;
	}

	public int isEqual( Record x, Record y ) {
		// If there is no pre-expanded records, do expand
		++checked;
//		if( x.equals( y ) ) {
		if (Arrays.equals( x.getTokensArray(), y.getTokensArray() )) {
			return 0;
		}

		List<Record> expandedX = x.expandAll();

		for ( int d=0; d<=deltaMax; ++d ) {
			for ( int dx=0; dx<=d; ++dx ) {
				/*
				 * dx errors from xPrime, dy = d-dx errors from y
				 */
				int dy = d - dx;
				if ( dy > y.size() ) continue;
				List<IntArrayList> combList_y = Util.getCombinations( y.size(), y.size()-dy );
				for( Record xPrime : expandedX ) {
					if ( dx > xPrime.size() ) continue;
					List<IntArrayList> combList_x = Util.getCombinations( xPrime.size(), xPrime.size()-dx );
					
					for ( IntArrayList comb_x : combList_x ) {
						int[] x0 = Util.getSubsequence( xPrime.getTokensArray(), comb_x );
						for ( IntArrayList comb_y : combList_y ) {
							int[] y0 = Util.getSubsequence( y.getTokensArray(), comb_y );
							if ( x0 == null && y0 == null ) return 1;
							else if ( x0 == null || y0 == null ) continue;
							else if ( Arrays.equals( x0, y0 ) ) return 1;
						}
					}
				}
			}
		}

		return -1;
	}
	
	public String getName() {
		return "NaiveOneSideValidator";
	}
}
