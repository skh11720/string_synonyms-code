package snu.kdd.synonym.synonymRev.algorithm.delta;

import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.validator.Validator;

public abstract class AbstractDeltaValidator extends Validator {

	protected final int deltaMax;
	public final FuncDistGivenThres distGivenThres;
	public final FuncDistAll distAll;
	public final FuncDistTrivialCase distTrivialCase;

	protected long numEqual = 0;
	protected long numTrivial = 0;
	protected long numDeltaEqual = 0;
	protected long numDeltaTransEqual = 0;
	
	public AbstractDeltaValidator( int deltaMax, String strDistFunc ) {
		this.deltaMax = deltaMax;
		if ( strDistFunc.equals("edit") ) {
			distGivenThres = Util::edit;
			distAll = Util::edit_all;
			distTrivialCase = new FuncDistTrivialCase() {
				@Override
				public boolean check(Record x, Record y) {
					if ( x.getTransLengths()[0] <= deltaMax && y.size() <= deltaMax ) return true;
					else return false;
				}
			};
		}
		else if ( strDistFunc.equals("lcs") ) {
			distGivenThres = Util::lcs;
			distAll = Util::lcs_all;
			distTrivialCase = new FuncDistTrivialCase() {
				@Override
				public boolean check(Record x, Record y) {
					if ( x.getTransLengths()[0] + y.size() <= deltaMax ) return true;
					else return false;
				}
			};
		}
		else {
			throw new RuntimeException("Unknown distance function: "+strDistFunc);
		}
	}
	
	@FunctionalInterface
	protected interface FuncDistGivenThres {
		public int eval( int[] x, int[] y, int threshold );
	}
	
	@FunctionalInterface
	protected interface FuncDistAll {
		public int[] eval( int[] x, int[] y, int j0 );
	}
	
	@FunctionalInterface
	protected interface FuncDistTrivialCase {
		public boolean check( Record x, Record y );
	}
}
