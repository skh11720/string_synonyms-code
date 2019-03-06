package snu.kdd.synonym.synonymRev.algorithm;

import java.util.Set;

import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.StatContainer;

public interface AlgorithmInterface {
	void run( Query query );
	void writeJSON();
	Set<IntegerPair> getResult();
	StatContainer getStat();
	void setWriteResult( boolean flag );
	String getVersion();
	String getName();
	String getNameWithParam();
}
