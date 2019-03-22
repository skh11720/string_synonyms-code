package snu.kdd.synonym.synonymRev.algorithm;

public abstract class AbstractIndexBasedAlgorithm extends AbstractParameterizedAlgorithm {

	public AbstractIndexBasedAlgorithm(String[] args) {
		super(args);
	}

	protected abstract void buildIndex();
}
