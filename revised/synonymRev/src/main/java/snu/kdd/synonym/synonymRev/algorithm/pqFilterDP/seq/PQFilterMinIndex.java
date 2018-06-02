package snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.seq;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import snu.kdd.synonym.synonymRev.algorithm.misc.EstimationTest;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.index.JoinMinIndex;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.Histogram;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.MinPositionQueue;
import snu.kdd.synonym.synonymRev.tools.MinPositionQueue.MinPosition;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StaticFunctions;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.tools.WYK_HashMap;
import snu.kdd.synonym.synonymRev.tools.WYK_HashSet;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class PQFilterMinIndex extends JoinMinIndex {

	public PQFilterMinIndex( int nIndex, int qSize, StatContainer stat, Query query, int threshold, boolean writeResult ) {
		super( nIndex, qSize, stat, query, threshold, writeResult );
		// TODO Auto-generated constructor stub
	}
	
	@Override
	protected List<List<QGram>> getCandidatePQGrams( Record rec ) {
		List<List<QGram>> availableQGrams = rec.getQGrams( qSize );
		List<List<QGram>> candidatePQGrams = new ArrayList<List<QGram>>();
		PosQGramFilterDP filter = new PosQGramFilterDP(rec, qSize);
		for ( int k=0; k<availableQGrams.size(); ++k ) {
			List<QGram> qgrams = new ArrayList<QGram>();
			for ( QGram qgram : availableQGrams.get( k ) ) {
				if ( filter.existence( qgram, k ) ) qgrams.add( qgram );
			}
			candidatePQGrams.add( qgrams );
		}
		return candidatePQGrams;
	}
}
