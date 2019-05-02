package passjoin;

import snu.kdd.synonym.synonymRev.algorithm.delta.AbstractDeltaValidator;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class PassJoinValidator extends AbstractDeltaValidator {

	public final FuncPartialDistGivenThres partialDistGivenThres;
	
	public PassJoinValidator( int deltaMax, String strDistFunc ) {
		super(deltaMax, strDistFunc);

		if ( strDistFunc.equals("edit") ) {
			partialDistGivenThres = Util::edit;
		}
		else if ( strDistFunc.equals("lcs") ) {
			partialDistGivenThres = Util::lcs;
		}
		else {
			throw new RuntimeException("Unknown distance function: "+strDistFunc);
		}
	}

	@Override
	public int isEqual(Record x, Record y) {
		if ( x.equals(y) ) 
			return 0;
		if ( distGivenThres.eval(x.getTokensArray(), y.getTokensArray(), deltaMax) <= deltaMax )
			return 1;
		else return -1;
	}

	@Override
	public String getName() {
		return "PassJoinValidator";
	}

	@FunctionalInterface
	protected interface FuncPartialDistGivenThres {
		public int eval( int[] x, int[] y, int threshold, int xpos, int ypos, int xlen, int ylen );
	}
}
