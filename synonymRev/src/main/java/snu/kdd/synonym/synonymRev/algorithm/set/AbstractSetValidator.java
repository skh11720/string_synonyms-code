package snu.kdd.synonym.synonymRev.algorithm.set;

import java.util.Arrays;

import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.validator.Validator;

abstract public class AbstractSetValidator extends Validator {
	
	protected final Boolean selfJoin;
	
	public AbstractSetValidator( Boolean selfJoin ) {
		this.selfJoin = selfJoin;
	}
	
	protected static int[] getSortedTokenArray( Record rec ) {
		int[] sorted = rec.getTokensArray().clone();
		Arrays.sort(sorted);
		return sorted;
	}

	protected static int[] getSortedArray( int[] arr ) {
		int[] sorted = arr.clone();
		Arrays.sort(sorted);
		return sorted;
	}
	
	protected static boolean areSameString( Record x, Record y ) {
		if( x.getTokenCount() != y.getTokenCount() ) {
			return false;
		}

		int[] x_sorted = getSortedTokenArray( x );
		int[] y_sorted = getSortedTokenArray( y );

		if (Arrays.equals( x_sorted, y_sorted )) return true;
		return false;
	}

	@Override
	public int isEqual( Record x, Record y ) {
		// Check whether x -> y
		++checked;
		int res;
		if( areSameString( x, y ) ) res = 0;
		else if ( isEqualOneSide( x, y ) > 0 ) res = 1;
//		else if ( !selfJoin && isEqualOneSide( y, x ) > 0 ) res = 1;
		else res = -1;
		return res;
	}
	
	abstract protected int isEqualOneSide( Record x, Record y );

	abstract public String getName();
}
