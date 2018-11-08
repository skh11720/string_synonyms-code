package snu.kdd.synonym.synonymRev.algorithm.delta;

import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class DeltaValidatorNaive extends Validator {
	
	protected final int deltaMax;

	protected long numEqual = 0;
	protected long numDeltaEqual = 0;
	protected long numDeltaTransEqual = 0;
	
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
		if ( Util.edit( x.getTokensArray(), y.getTokensArray(), deltaMax ) <= deltaMax ) {
			++numDeltaEqual;
			return 1;
		}
		
		// transform x
		if ( isDeltaTransEqual(x, y) ) {
			++numDeltaTransEqual;
			return 1;
		}
		
		// otherwise
		return -1;
	}
	
	protected boolean isDeltaTransEqual( Record x, Record y ) {
		for ( Record exp : x.expandAll() ) {
			if ( Util.edit( exp.getTokensArray(), y.getTokensArray(), deltaMax ) <= deltaMax ) {
//				if ( y.size() > deltaMax ) {
//					System.out.println("DKFJDLFKSDJFD\t"+x.getID()+", "+y.getID());
//					try {System.in.read();}
//					catch (IOException e) {e.printStackTrace();}
//				}
				return true;
			}
		}
		return false;
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
