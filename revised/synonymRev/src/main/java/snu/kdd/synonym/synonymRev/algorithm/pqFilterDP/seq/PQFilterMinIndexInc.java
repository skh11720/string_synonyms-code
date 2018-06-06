package snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.seq;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.Util;

public class PQFilterMinIndexInc extends PQFilterMinIndex {
	
	protected final int indexK;

	public PQFilterMinIndexInc( int nIndex, int qSize, StatContainer stat, Query query, int threshold, boolean writeResult ) {
		super( nIndex, qSize, stat, query, threshold, writeResult );
		// TODO Auto-generated constructor stub
		indexK = nIndex;
		DebugWriteToFile( "tmp/PQFilterMinIndexInc.txt" );
	}
	
	@Override
	protected List<List<QGram>> getCandidatePQGrams( Record rec ) {
		List<List<QGram>> availableQGrams = rec.getQGrams( qSize );
		List<List<QGram>> candidatePQGrams = new ArrayList<List<QGram>>();
		PosQGramFilterDPInc filter = new PosQGramFilterDPInc(rec, qSize);
//		boolean debug = false;
//		if ( rec.getID() == 15756 ) debug = true;

		for ( int k=0; k<availableQGrams.size(); ++k ) {
			if ( k >= idx.size() ) continue;
//			if ( debug ) System.out.println( "k: "+k );
//			if ( debug ) System.out.println( "availableQGrams.get(k): "+availableQGrams.get( k ) );

			List<QGram> qgrams0 = new ArrayList<QGram>();
			for ( QGram qgram : availableQGrams.get( k ) ) {
				if ( idx.get( k ).containsKey( qgram )) qgrams0.add( qgram );
			}
//			if ( debug ) System.out.println( "qgrams0: "+qgrams0 );
			if ( qgrams0.size() == 0 ) {
				candidatePQGrams.add( qgrams0 );
				continue;
			}
			List<IntegerPair> qgramPrefixList = Util.getQGramPrefixList( qgrams0 );
//			if ( debug ) System.out.println( "qgramPrefixList: "+qgramPrefixList );

//			List<IntegerPair> qgramPrefixList = Util.getQGramPrefixList( availableQGrams.get(k) );
			List<QGram> qgrams1 = new ArrayList<QGram>();
			for ( IntegerPair ipair : qgramPrefixList ) {
				int token = ipair.i1;
				int depth = ipair.i2;
				boolean isInTPQ = ((IncrementalDP)filter).existence( token, depth, k );
				++checkTPQ;
				if (isInTPQ && depth == qSize) {
					qgrams1.add( new QGram( ((IncrementalDP)filter).getQGram() ) );
				}
			}
//			if ( debug ) System.out.println( "qgrams1: "+qgrams1 );


			candidatePQGrams.add( qgrams1 );
			nCand += qgrams1.size();
		}
		return candidatePQGrams;
	}
}
