package snu.kdd.synonym.synonymRev.algorithm.delta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.MinPositionQueue;
import snu.kdd.synonym.synonymRev.tools.MinPositionQueue.MinPosition;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.StatContainer;

public class JoinDeltaVarBKIndex extends JoinDeltaVarIndex {

//	protected ArrayList<ArrayList<WYK_HashMap<QGram, List<Record>>>> idx;
//	/*
//	 * Inverted lists whose key are (pos, delta, qgram).
//	 */
//
//	protected final QGramDeltaGenerator qdgen;
//	/*
//	 * generate the delta-variants of an input qgram.
//	 */
//	
//	protected Object2IntOpenHashMap<Record> indexedCountList;
//	/*
//	 * Keep the number of indexed positions for every string in the indexedSet.
//	 */

	protected final List<Object2IntOpenHashMap<QGram>> countMapVTPQ;
	/*
	 * A map from a pair of (position, qgram) to the number of records in searchedSet such that each record contains [qgram, position] in VTPQ.
	 */

	protected final Int2IntOpenHashMap posCounter;
	/*
	 * the distribution of selected positions.
	 */
	
	protected final double sampleB;

	public JoinDeltaVarBKIndex( Query query, int indexK, int qSize, int deltaMax, double sampleB ) {
		super(query, indexK, qSize, deltaMax);
		this.sampleB = sampleB;
		this.posCounter = new Int2IntOpenHashMap();
		this.countMapVTPQ = countQGramsInSTPQ();
	}
	
	protected List<Record> prepareCountQGrams() {
		/*
		 * return a sample of records, instead of the whole records in searchedSet.
		 */
		Random rn = new Random( System.currentTimeMillis() );
		ObjectArrayList<Record> searchedList = new ObjectArrayList<>();
		for( Record r : query.searchedSet.recordList ) {
			if ( r.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) continue;
			if( rn.nextDouble() < this.sampleB ) {
				searchedList.add( r );
			}
		}
		return searchedList;
	}

	@Deprecated
	protected List<Set<QGram>> getVarPQ( Record rec ) {
		/*
		 * Compute VarPQ of rec.
		 * The result is a map from a position to the corresponding q-grams and their variants.
		 */
		List<Set<QGram>> varPQ = new ObjectArrayList<>();
		List<List<QGram>> pqgramSet = rec.getSelfQGrams(qSize+deltaMax);
		for ( int k=0; k<pqgramSet.size(); ++k ) {
			Set<QGram> varPQ_k = new ObjectOpenHashSet<>();
			QGram qgram = pqgramSet.get(k).get(0);
			for ( Entry<QGram, Integer> entry: qdgen.getQGramDelta( qgram ) ) {
				QGram qgramDelta = entry.getKey();
				varPQ_k.add(qgramDelta);
			}
			varPQ.add(varPQ_k);
		}
		return varPQ;
	}
	
	protected List<Object2IntOpenHashMap<QGram>> countQGramsInSTPQ() {
		/*
		 * Count the number of qgrams in the STPQ and their delta-variants of records in searchedSet.
		 * The result (countMapVTPQ) is a map from (position, qgram) to the count.
		 */
		List<Object2IntOpenHashMap<QGram>> countMapVTPQ = new ArrayList<Object2IntOpenHashMap<QGram>>();
		List<Record> searchedList = prepareCountQGrams();

		// build the inverted k
		for( Record recS : searchedList ) {
			List<List<Set<QGram>>> varTPQ = null;
			if ( useSTPQ ) varTPQ = getVarSTPQ(recS, false);
			else varTPQ = getVarTPQ(recS, false);

			// initialize the inverted lists for the new positions
			int searchmax = varTPQ.size();
			while ( countMapVTPQ.size() < searchmax ) countMapVTPQ.add( new Object2IntOpenHashMap<QGram>() );

			// count qgrams
			for( int k=0; k<searchmax; ++k ) {
				Object2IntOpenHashMap<QGram> countMapVTPQ_k = countMapVTPQ.get(k);
				List<Set<QGram>> varTPQ_k = varTPQ.get(k);
				for ( int d=0; d<varTPQ_k.size(); ++d ) {
					for ( QGram qgram : varTPQ_k.get(d) ) countMapVTPQ_k.addTo(qgram, 1);
				}
			}
		} // end for rec in searchedSet
		
		return countMapVTPQ;
	}

	@Override
	protected IntArrayList getIndexPosition( Record recT ) {
		// find best K positions for recT.
		IntArrayList posList = new IntArrayList();
		int searchmax = Math.min( recT.size(), countMapVTPQ.size() );
		List<List<QGram>> PQt = recT.getSelfQGrams( qSize+deltaMax, searchmax );

		MinPositionQueue mpq = new MinPositionQueue( indexK );
		for ( int k=0; k<searchmax; ++k ) {
			Object2IntOpenHashMap<QGram> countMapVTPQ_k = countMapVTPQ.get(k);
			QGram qgram = PQt.get(k).get(0);
			int count = 0;
			for ( Entry<QGram, Integer> entry : qdgen.getQGramDelta(qgram) ) {
				QGram qgramDelta = entry.getKey();
				count += countMapVTPQ_k.getInt(qgramDelta);
			}
			mpq.add( k, count );
		}

		while( !mpq.isEmpty() ) {
			MinPosition minPos = mpq.poll();
			int minIdx = minPos.positionIndex;
			posCounter.addTo( minIdx, 1 );
			posList.add(minIdx);
		}

		return posList;
	}

	@Override
	protected void postprocessAfterJoin( StatContainer stat ) {
		super.postprocessAfterJoin(stat);
		stat.add( "posDistribution", posCounter.toString() );
	}
}
