package snu.kdd.synonym.synonymRev.index;

import java.util.Set;

import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.validator.Validator;

public interface JoinMHIndexInterface {
	public Set<IntegerPair> join(StatContainer stat, Query query, Validator checker, boolean writeResult);
	public void joinOneRecordThres( Record recS, Set<IntegerPair> rslt, Validator checker, int threshold, boolean oneSideJoin );
	public void writeToFile();
	public long getCountValue();
	public long getEquivComparisons();
	public double getGamma();
	public double getZeta();
	public double getEta();
}
