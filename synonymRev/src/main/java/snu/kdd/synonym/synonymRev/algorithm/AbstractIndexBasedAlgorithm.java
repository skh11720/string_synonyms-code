package snu.kdd.synonym.synonymRev.algorithm;

import snu.kdd.synonym.synonymRev.data.Query;

public abstract class AbstractIndexBasedAlgorithm extends AlgorithmTemplate {

	public AbstractIndexBasedAlgorithm(Query query, String[] args) {
		super(query, args);
	}

	protected abstract void buildIndex( boolean writeResult );
}
