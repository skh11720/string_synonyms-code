package snu.kdd.synonym.synonymRev.algorithm;

import java.io.IOException;
import java.util.Collection;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.synonymRev.data.DataInfo;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;

public interface AlgorithmInterface {
	public String getName();
	public String getVersion();
	public void run( Query query, String[] args ) throws IOException, ParseException;
	public void printStat();
	public void writeJSON( DataInfo dataInfo, CommandLine cmd );
	public Collection<IntegerPair> getResult();
	public void setWriteResult( boolean flag );
}
