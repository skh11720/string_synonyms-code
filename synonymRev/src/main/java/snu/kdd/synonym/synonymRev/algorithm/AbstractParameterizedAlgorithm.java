package snu.kdd.synonym.synonymRev.algorithm;

import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.tools.ParamFactory;

public abstract class AbstractParameterizedAlgorithm extends AbstractAlgorithm {

	public AbstractParameterizedAlgorithm(Query query, String[] args) {
		super(query, args);
		param = ParamFactory.getParamInstance(this, args);
	}

	protected abstract void reportParamsToStat();
	
	@Override
	public void run() {
		reportParamsToStat();
		super.run();
	}
}
