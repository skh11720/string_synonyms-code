package snu.kdd.synonym.synonymRev.validator;

import java.util.Arrays;
import java.util.List;

import snu.kdd.synonym.synonymRev.data.Record;

public class SetNaiveOneSide extends Validator {

	public int isEqual( Record x, Record y ) {
		// If there is no pre-expanded records, do expand
		++checked;
//		if( x.equals( y ) ) {
		int[] x_sorted = getSortedTokenArray( x );
		int[] y_sorted = getSortedTokenArray( y );

		if (Arrays.equals( x_sorted, y_sorted )) {
			return 1;
		}

		List<Record> expandedX = x.expandAll();

		for( Record xPrime : expandedX ) {
			int[] xPrime_sorted = getSortedTokenArray( xPrime );
			if (Arrays.equals( xPrime_sorted, y_sorted)) {
				return 1;
			}
		}

		return -1;
	}

	public String getName() {
		return "SetNaiveOneSideValidator";
	}
	
	private int[] getSortedTokenArray( Record rec ) {
		int[] sorted = Arrays.copyOf( rec.getTokensArray(), rec.size() );
		Arrays.sort(sorted);
		return sorted;
	}
}
