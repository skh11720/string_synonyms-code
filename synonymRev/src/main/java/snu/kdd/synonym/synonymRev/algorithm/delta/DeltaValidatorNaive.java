package snu.kdd.synonym.synonymRev.algorithm.delta;

import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class DeltaValidatorNaive extends Validator {
	
	private int deltaMax;

	private long numEqual = 0;
	private long numDeltaEqual = 0;
	private long numDeltaTransEqual = 0;
	
	public DeltaValidatorNaive( int deltaMax ) {
		this.deltaMax = deltaMax;
	}

	@Override
	public int isEqual( Record x, Record y ) {
		// Check whether x -> y
		++checked;
		
		// trivial cases
		if ( areSameString( x, y )) {
			++numEqual;
			return 0; 
		}
		if ( Util.edit( x.getTokensArray(), y.getTokensArray() ) <= deltaMax ) {
			++numDeltaEqual;
			return 1;
		}
		
		// transform x
		for ( Record exp : x.expandAll() ) {
			if ( Util.edit( exp.getTokensArray(), y.getTokensArray() ) <= deltaMax ) {
				++numDeltaTransEqual;
				return 1;
			}
		}
		return -1;
	}

	@Override
	public void addStat( StatContainer stat ) {
		super.addStat(stat);
		stat.add( "Val_NumEqual", numEqual );
		stat.add( "Val_NumDeltaEqual", numDeltaEqual );
		stat.add( "Val_NumDeltaTransEqual", numDeltaTransEqual );
	}

	@Override
	public void printStats() {
		super.printStats();
		if( DEBUG.ValidateON ) {
			Util.printLog( "NumEqual: " + numEqual );
			Util.printLog( "NumDeltaEqual: " + numDeltaEqual );
			Util.printLog( "NumDeltaTransEqual: " + numDeltaTransEqual );
		}
	}

	@Override
	public String getName() {
		return "DeltaValidatorNaive";
	}

}
