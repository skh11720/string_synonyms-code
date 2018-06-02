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
	}
	
	@Override
	protected List<List<QGram>> getCandidatePQGrams( Record rec ) {
		List<List<QGram>> availableQGrams = rec.getQGrams( qSize );
		List<List<QGram>> candidatePQGrams = new ArrayList<List<QGram>>();
		PosQGramFilterDPInc filter = new PosQGramFilterDPInc(rec, qSize);

		for ( int k=0; k<availableQGrams.size(); ++k ) {
			List<IntegerPair> qgramPrefixList = Util.getQGramPrefixList( availableQGrams.get( k ) );
//			System.out.println( "availableQGrams, "+k );
//			System.out.println( availableQGrams.get( k ) );
//			System.out.println( "qgramPrefixList, "+k );
//			System.out.println( qgramPrefixList );
			List<QGram> qgrams = new ArrayList<QGram>();
			for ( IntegerPair ipair : qgramPrefixList ) {
				int token = ipair.i1;
				int depth = ipair.i2;
				boolean isInTPQ = ((IncrementalDP)filter).existence( token, depth, k );
				if (isInTPQ && depth == qSize) {
					qgrams.add( new QGram( ((IncrementalDP)filter).getQGram() ) );
				}
			}
			candidatePQGrams.add( qgrams );
//			System.out.println( qgrams );
		}
		return candidatePQGrams;
	}
}
