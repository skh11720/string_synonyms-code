package snu.kdd.synonym.synonymRev.algorithm.delta;

import java.io.BufferedWriter;
import java.util.Set;

import snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.seq.PosQGramFilterDPDelta;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class JoinMinDeltaDPIndex extends JoinMinDeltaIndex {

	private PosQGramFilterDPDelta filter;

	long filteredQGramCount = 0; // number of filtered qgrams from records in the searchedSet
	long filterDPTime = 0; // number of filtered qgrams from records in the searchedSet

	public JoinMinDeltaDPIndex( int nIndex, int qSize, int deltaMax, StatContainer stat, Query query, int threshold,
			boolean writeResult ) {
		super( nIndex, qSize, deltaMax, stat, query, threshold, writeResult );
	}

	@Override
	public void joinRecordMaxKThres( int nIndex, Record recS, Set<IntegerPair> rslt, boolean writeResult, BufferedWriter bw,
			Validator checker, int threshold, boolean oneSideJoin ) {
		filter = new PosQGramFilterDPDelta( recS, qgramSize, deltaMax );
		super.joinRecordMaxKThres( nIndex, recS, rslt, writeResult, bw, checker, threshold, oneSideJoin );
	}
	
	@Override
	public void addStat( StatContainer stat ) {
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
