package snu.kdd.synonym.synonymRev.index;

import java.util.ArrayList;
import java.util.List;

import snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.seq.PosQGramFilterDP;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.WYK_HashMap;

public class JoinMinDPIndex extends JoinMinIndex {

	private long checkTPQTime = 0;
	private int checkTPQ = 0;

	public JoinMinDPIndex( int nIndex, int qSize, StatContainer stat, Query query, int threshold, boolean writeResult ) {
		super( nIndex, qSize, stat, query, threshold, writeResult );
	}
	
	@Override
	protected List<List<QGram>> getCandidatePQGrams( Record rec ) {
		List<List<QGram>> availableQGrams = rec.getQGrams( qgramSize );
		List<List<QGram>> candidatePQGrams = new ArrayList<List<QGram>>();
		PosQGramFilterDP filter = new PosQGramFilterDP(rec, qgramSize);
		for ( int k=0; k<availableQGrams.size(); ++k ) {
			if ( k >= idx.size() ) continue;
			WYK_HashMap<QGram, List<Record>> curidx = idx.get( k );
			List<QGram> qgrams = new ArrayList<QGram>();
			for ( QGram qgram : availableQGrams.get( k ) ) {
				if ( !curidx.containsKey( qgram ) ) continue;
				long ts = System.nanoTime();
				boolean isInTPQ = filter.existence( qgram, k );
				checkTPQTime += System.nanoTime() - ts;
				if ( isInTPQ ) qgrams.add( qgram );
				++checkTPQ;
			}
			candidatePQGrams.add( qgrams );
		}
		
		return candidatePQGrams;
	}
	
	public void addStat( StatContainer stat ) {
		super.addStat( stat );
		stat.add( "checkTPQ", checkTPQ );
		stat.add( "checkTPQTime", checkTPQTime/1e6 );
	}
}
