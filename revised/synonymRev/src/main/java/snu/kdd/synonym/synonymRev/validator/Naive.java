package snu.kdd.synonym.synonymRev.validator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import snu.kdd.synonym.synonymRev.data.Record;

public class Naive extends Validator {

	public int isEqual( Record x, Record y ) {
		// If there is no pre-expanded records, do expand
		++checked;
//		if( x.equals( y ) ) {
		if (Arrays.equals( x.getTokensArray(), y.getTokensArray() )) {
			return 1;
		}

		List<Record> expandedX = x.expandAll();

		List<Record> expandedY = y.expandAll();

		HashSet<Record> hashSet = new HashSet<>();

		boolean bigX = true;
		if( expandedX.size() < expandedY.size() ) {
			bigX = false;
		}
		if( bigX ) {
			for( Record r : expandedX ) {
				hashSet.add( r );
			}
		}
		else {
			for( Record r : expandedY ) {
				hashSet.add( r );
			}
		}

		if( bigX ) {
			for( Record yPrime : expandedY ) {
				if( hashSet.contains( yPrime ) ) {
					return 1;
				}
			}
		}
		else {
			for( Record xPrime : expandedX ) {
				if( hashSet.contains( xPrime ) ) {
					return 1;
				}
			}
		}

		return -1;
	}

	public String getName() {
		return "NaiveValidator";
	}
}
