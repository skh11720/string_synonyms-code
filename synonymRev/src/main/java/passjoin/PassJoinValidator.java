package passjoin;

import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class PassJoinValidator extends Validator {
	
	private final int deltaMax;
	
	public PassJoinValidator(int deltaMax) {
		this.deltaMax = deltaMax;
	}

	@Override
	public int isEqual(Record x, Record y) {
		if ( x.equals(y) ) 
			return 0;
		if (Util.edit(x.getTokensArray(), y.getTokensArray(), deltaMax, 0, 0, -1, -1) <= deltaMax)
			return 1;
		else return -1;
	}

	@Override
	public String getName() {
		return "PassJoinValidator";
	}
}
