package snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.seq;

import java.util.ArrayList;
import java.util.List;

import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.index.JoinMinIndex;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.StatContainer;

public class PQFilterMinIndex extends JoinMinIndex {

	public long checkTPQ = 0;
	public long nCand = 0;

	public PQFilterMinIndex( int nIndex, int qSize, StatContainer stat, Query query, int threshold, boolean writeResult ) {
		super( nIndex, qSize, stat, query, threshold, writeResult );
		// TODO Auto-generated constructor stub
	}
	
	@Override
	protected List<List<QGram>> getCandidatePQGrams( Record rec ) {
		List<List<QGram>> availableQGrams = rec.getQGrams( qgramSize );
		List<List<QGram>> candidatePQGrams = new ArrayList<List<QGram>>();
		PosQGramFilterDP filter = new PosQGramFilterDP(rec, qgramSize);
		for ( int k=0; k<availableQGrams.size(); ++k ) {
			if ( k >= idx.size() ) continue;
			List<QGram> qgrams = new ArrayList<QGram>();
			for ( QGram qgram : availableQGrams.get( k ) ) {
				if ( !idx.get( k ).containsKey( qgram ) ) continue;
				if ( filter.existence( qgram, k ) ) qgrams.add( qgram );
				++checkTPQ;
			}
			candidatePQGrams.add( qgrams );
			nCand += qgrams.size();
		}
		return candidatePQGrams;
	}
}
