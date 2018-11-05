package snu.kdd.synonym.synonymRev.algorithm.delta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.index.AbstractIndex;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StaticFunctions;
import snu.kdd.synonym.synonymRev.tools.WYK_HashMap;
import snu.kdd.synonym.synonymRev.tools.WYK_HashSet;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class JoinDeltaIndex extends AbstractIndex {

	protected ArrayList<WYK_HashMap<QGram, List<Record>>> idx;
	protected final int qgramSize;
	protected final int deltaMax;
	protected final int qd;
	protected final boolean isSelfJoin;
  
	protected long indexTime;

	protected long candQGramCount = 0;
	protected long equivComparisons = 0;
	protected long candQGramCountTime = 0;
	protected long filterTime = 0;
	protected long verifyTime = 0;
	
	public static boolean useLF = true;
	public static boolean usePQF = true;

	public JoinDeltaIndex( int qgramSize, int deltaMax, Iterable<Record> indexedSet, Query query, StatContainer stat ) {
		this.qgramSize = qgramSize;
		this.deltaMax = deltaMax;
		this.qd = qgramSize * deltaMax;
		this.isSelfJoin = query.selfJoin;

		long ts = System.nanoTime();
		idx = new ArrayList<WYK_HashMap<QGram, List<Record>>>();
		
		for ( Record recT : indexedSet ) {
			List<List<QGram>> availableQGrams = recT.getSelfQGrams(qgramSize);
			while ( idx.size() < availableQGrams.size() ) idx.add( new WYK_HashMap<>() );

			for ( int k=0; k<availableQGrams.size(); ++k ) {
				WYK_HashMap<QGram, List<Record>> kthMap = idx.get(k);
				QGram qgram = availableQGrams.get(k).get(0);
				if ( !kthMap.containsKey(qgram) ) kthMap.put( qgram, new ArrayList<Record>() );
				kthMap.get(qgram).add(recT);
			}
		}
		indexTime = System.nanoTime() - ts;
	}

	@Override
	protected void postprocessAfterJoin(StatContainer stat) {
		stat.add( "Stat_CandQGramCount", candQGramCount );
		stat.add( "Result_5_1_Filter_Time", filterTime/1e6 );
		stat.add( "Result_5_2_Verify_Time", verifyTime/1e6 );
	}

	protected List<List<QGram>> getCandidatePQGrams( Record rec ) {
		List<List<QGram>> availableQGrams = rec.getQGrams( qgramSize );
		List<List<QGram>> candidatePQGrams = new ArrayList<List<QGram>>();
		for ( int k=0; k<availableQGrams.size(); ++k ) {
			if ( k >= idx.size() ) continue;
			WYK_HashMap<QGram, List<Record>> curidx = idx.get( k );
			List<QGram> qgrams = new ArrayList<QGram>();
			for ( QGram qgram : availableQGrams.get( k ) ) {
				if ( !curidx.containsKey( qgram ) ) continue;
				qgrams.add( qgram );
			}
			candidatePQGrams.add( qgrams );
		}
		return candidatePQGrams;
	}

	public void joinOneRecord( Record recS, Set<IntegerPair> rslt, Validator checker ) {
	    long ts = System.nanoTime();
		List<List<QGram>> availableQGrams = getCandidatePQGrams( recS );
		for (List<QGram> list : availableQGrams) {
			candQGramCount += list.size();
		}
		long afterCandQgramTime = System.nanoTime();
		
		int[] rangeS = recS.getTransLengths();
		Object2IntOpenHashMap<Record> candidatesCount = new Object2IntOpenHashMap<Record>();
		candidatesCount.defaultReturnValue(0);
		for ( int k=0; k<availableQGrams.size(); ++k ) {
			ObjectOpenHashSet<Record> kthCandidates = new ObjectOpenHashSet<Record>();
			for ( QGram qgram : availableQGrams.get(k) ) {
				if ( !idx.get(k).containsKey(qgram ) ) continue;
				for ( Record recT : idx.get(k).get(qgram) ) {
					if ( !useLF || StaticFunctions.overlap(rangeS[0] - deltaMax, rangeS[1] + deltaMax, recT.size(), recT.size())) {
						kthCandidates.add( recT );
					}
					else ++checker.lengthFiltered;
				}
			} // end for qgram in availableQgrams.get(k)
			
			for ( Record recT : kthCandidates ) candidatesCount.addTo(recT, 1);
		} // end for k
		
		Set<Record> candidates = new WYK_HashSet<>();
		for ( Map.Entry<Record, Integer> entry : candidatesCount.entrySet() ) {
			Record recT = entry.getKey();
			int count = entry.getValue().intValue();
			
			if ( !usePQF || count >= Math.max(rangeS[0], recT.size()) - qd ) {
				candidates.add( recT );
			}
			else ++checker.pqgramFiltered;
		}
		long afterFilterTime = System.nanoTime();
		
		equivComparisons += candidates.size();
		for ( Record recT : candidates ) {
			if ( checker.isEqual( recS, recT ) >= 0 ) {
				AlgorithmTemplate.addSeqResult(recS, recT, rslt, isSelfJoin );
			}
		}
		long afterVerifyTime = System.nanoTime();

		candQGramCountTime += afterCandQgramTime - ts;
		filterTime += afterFilterTime - afterCandQgramTime;
		verifyTime += afterVerifyTime - afterFilterTime;
	}
}
