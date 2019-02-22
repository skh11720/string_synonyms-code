package snu.kdd.synonym.synonymRev.algorithm.delta;

import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.validator.Validator;

public abstract class AbstractDeltaValidator extends Validator {

	protected final int deltaMax;
	protected final FuncDistGivenThres distGivenThres;
	protected FuncDistAll distAll;

	protected long numEqual = 0;
	protected long numDeltaEqual = 0;
	protected long numDeltaTransEqual = 0;
	
	public AbstractDeltaValidator( int deltaMax, String strDistFunc ) {
		this.deltaMax = deltaMax;
		if ( strDistFunc.equals("edit") ) {
			distGivenThres = Util::edit;
			distAll = Util::edit_all;
		}
		else if ( strDistFunc.equals("lcs") ) {
			distGivenThres = Util::lcs;
			distAll = Util::lcs_all;
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
}
