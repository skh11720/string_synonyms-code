package snu.kdd.synonym.synonymRev.index;

import java.io.BufferedWriter;
import java.util.Set;

import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.validator.Validator;

public interface JoinMinIndexInterface {
	public Set<IntegerPair> joinMaxK( int indexK, boolean writeResult, StatContainer stat, Validator checker, Query query );
	public void joinRecordMaxKThres( int nIndex, Record recS, Set<IntegerPair> rslt, boolean writeResult, BufferedWriter bw, Validator checker, int threshold, boolean oneSideJoin );
	public void addStat( StatContainer stat );
	public double getLambda();
	public double getMu();
	public double getRho();
	public double getRhoPrime();
	public long getSearchedTotalSigCount();
	public long getIndexedTotalSigCount();
	public long getEquivComparisons();
	public long getComparisonTime();
	public long getAppliedRulesSum();
	public long getComparisonCount();
	public double getIndexTime();
	public double getCountTime();
	public long getPredictCount();
}
