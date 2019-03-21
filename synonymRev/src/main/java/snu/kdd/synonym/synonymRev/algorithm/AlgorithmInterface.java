package snu.kdd.synonym.synonymRev.algorithm;

import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.tools.ResultSet;
import snu.kdd.synonym.synonymRev.tools.StatContainer;

public interface AlgorithmInterface {
	void run( Query query );
	void writeJSON();
	ResultSet getResult();
	StatContainer getStat();
	void setWriteResult( boolean flag );
	String getVersion();
	String getName();
	String getNameWithParam();
}
