package snu.kdd.synonym.synonymRev.algorithm;

import java.util.Collection;

import org.apache.commons.cli.CommandLine;

import snu.kdd.synonym.synonymRev.data.DataInfo;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.StatContainer;

public interface AlgorithmInterface {
	public String getName();
	public String getVersion();
	public void run();
	public void printStat();
	public void writeJSON( DataInfo dataInfo, CommandLine cmd );
	public Collection<IntegerPair> getResult();
	public StatContainer getStat();
	public void setWriteResult( boolean flag );
	public void writeResult();
}
