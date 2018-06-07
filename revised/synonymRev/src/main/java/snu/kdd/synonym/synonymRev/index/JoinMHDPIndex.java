package snu.kdd.synonym.synonymRev.index;

import java.util.ArrayList;
import java.util.List;

import snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.seq.PosQGramFilterDP;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.WYK_HashMap;

public class JoinMHDPIndex extends JoinMHIndex {
	
	public long checkTPQ = 0;
	public long nCand = 0;
	public long candTime = 0;

	public JoinMHDPIndex( int indexK, int qgramSize, Iterable<Record> indexedSet, Query query, StatContainer stat,
			int[] indexPosition, boolean addStat, boolean useIndexCount, int threshold ) {
		super( indexK, qgramSize, indexedSet, query, stat, indexPosition, addStat, useIndexCount, threshold );
		// TODO Auto-generated constructor stub
	}
	
	protected List<List<QGram>> getCandidatePQGrams( Record rec ) {
		long ts = System.currentTimeMillis();
		List<List<QGram>> availableQGrams = rec.getQGrams( qgramSize );
		List<List<QGram>> candidatePQGrams = new ArrayList<List<QGram>>();
		PosQGramFilterDP filter = new PosQGramFilterDP(rec, qgramSize);
		for ( int k=0; k<availableQGrams.size(); ++k ) {
			if ( k >= joinMHIndex.size() ) continue;
			WYK_HashMap<QGram, List<Record>> curidx = joinMHIndex.get( k );
			List<QGram> qgrams = new ArrayList<QGram>();
			for ( QGram qgram : availableQGrams.get( k ) ) {
				if ( !curidx.containsKey( qgram ) ) continue;
				if ( filter.existence( qgram, k ) ) qgrams.add( qgram );
				++checkTPQ;
			}
			candidatePQGrams.add( qgrams );
		}
		
		candTime += System.currentTimeMillis() - ts;
		for ( List<QGram> qgrams : candidatePQGrams ) nCand += qgrams.size();
		return candidatePQGrams;
	}
}
