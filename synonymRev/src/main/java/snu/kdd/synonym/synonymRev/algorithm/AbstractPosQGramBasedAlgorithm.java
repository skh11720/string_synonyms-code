package snu.kdd.synonym.synonymRev.algorithm;

import snu.kdd.synonym.synonymRev.data.Query;

public abstract class AbstractPosQGramBasedAlgorithm extends AbstractIndexBasedAlgorithm {

	public final int qSize;
	protected boolean useLF, usePQF, useSTPQ;

	public AbstractPosQGramBasedAlgorithm(Query query, String[] args) {
		super(query, args);
		qSize = param.getIntParam("qSize");
		useLF = usePQF = useSTPQ = true;
	}

	@Override
	protected void executeJoin() {
		if ( usePQF ) runAfterPreprocess();
		else runAfterPreprocessWithoutIndex();
	}

	protected abstract void runAfterPreprocess();
	
	protected abstract void runAfterPreprocessWithoutIndex();
}
