package snu.kdd.synonym.synonymRev.algorithm;

import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.tools.ParamFactory;

public abstract class AbstractParameterizedAlgorithm extends AbstractAlgorithm {

	public AbstractParameterizedAlgorithm( String[] args ) {
		super(args);
		param = ParamFactory.getParamInstance(this, args);
	}

	protected abstract void reportParamsToStat();
	
	@Override
	public void run( Query query ) {
		reportParamsToStat();
		super.run(query);
	}
}
