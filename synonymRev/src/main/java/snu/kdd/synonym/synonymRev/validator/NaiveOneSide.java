package snu.kdd.synonym.synonymRev.validator;

import java.util.Arrays;
import java.util.List;

import snu.kdd.synonym.synonymRev.data.Record;

public class NaiveOneSide extends Validator {

	public int isEqual( Record x, Record y ) {
		// If there is no pre-expanded records, do expand
		++checked;
//		if( x.equals( y ) ) {
		if (Arrays.equals( x.getTokensArray(), y.getTokensArray() )) {
			return 0;
		}

		List<Record> expandedX = x.expandAll();

		for( Record xPrime : expandedX ) {
//			if( xPrime.equals( y ) ) {{
			if (Arrays.equals( xPrime.getTokensArray(), y.getTokensArray() )) {
				return 1;
			}
		}

		return -1;
	}

	public String getName() {
		return "NaiveOneSideValidator";
	}
}
