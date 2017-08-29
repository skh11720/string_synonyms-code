package snu.kdd.synonym.synonymRev.validator;

import java.util.List;

import snu.kdd.synonym.synonymRev.data.Record;

public class NaiveOneSide extends Validator {

	public int isEqual( Record x, Record y ) {
		// If there is no pre-expanded records, do expand
		if( x.equals( y ) ) {
			return 1;
		}

		List<Record> expandedX = x.expandAll();

		for( Record xPrime : expandedX ) {
			if( xPrime.equals( y ) ) {
				return 1;
			}
		}

		return -1;
	}

	public String getName() {
		return "NaiveOneSide";
	}
}
