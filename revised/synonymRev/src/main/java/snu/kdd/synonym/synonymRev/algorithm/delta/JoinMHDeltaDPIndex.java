package snu.kdd.synonym.synonymRev.algorithm.delta;

import java.util.Set;

import snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.seq.PosQGramFilterDPDelta;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class JoinMHDeltaDPIndex extends JoinMHDeltaIndex {
	
	private PosQGramFilterDPDelta filter;

	long filteredQGramCount = 0; // number of filtered qgrams from records in the searchedSet
	long filterDPTime = 0; // number of filtered qgrams from records in the searchedSet

	public JoinMHDeltaDPIndex( int indexK, int qgramSize, int deltaMax, Iterable<Record> indexedSet, Query query,
			StatContainer stat, int[] indexPosition, boolean addStat, boolean useIndexCount, int threshold ) {
		super( indexK, qgramSize, deltaMax, indexedSet, query, stat, indexPosition, addStat, useIndexCount, threshold );
	}
	
	@Override
	public void joinOneRecordThres( Record recS, Set<IntegerPair> rslt, Validator checker, int threshold, boolean oneSideJoin ) {
		filter = new PosQGramFilterDPDelta( recS, qgramSize, deltaMax );
		super.joinOneRecordThres( recS, rslt, checker, threshold, oneSideJoin );
	}
	
	@Override
	protected void addStat( StatContainer stat ) {
		super.addStat( stat );
		stat.add("Stat_FilteredQGramCount", filteredQGramCount );
		stat.add("Stat_FilterDPTime", (long)(filterDPTime/1e6));
	}

	@Override
	protected boolean isInTPQ( QGram qgram, int k, int delta ) {
		long ts = System.nanoTime();
		boolean result = filter.existence( qgram, k, delta );
		filterDPTime += System.nanoTime() - ts;
		if (result) return true;
		else {
			++filteredQGramCount;
			return false;
		}
	}
}
