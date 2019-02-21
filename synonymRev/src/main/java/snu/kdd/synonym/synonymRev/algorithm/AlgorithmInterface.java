package snu.kdd.synonym.synonymRev.algorithm;

import java.util.Set;

import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.StatContainer;

public interface AlgorithmInterface {
	public String getName();
	public String getVersion();
	public void run();
	public void writeJSON();
	public Set<IntegerPair> getResult();
	public StatContainer getStat();
	public void setWriteResult( boolean flag );
	public void writeResult();
}
