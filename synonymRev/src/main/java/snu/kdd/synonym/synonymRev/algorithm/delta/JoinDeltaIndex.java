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

	protected ArrayList<WYK_HashMap<QGram, List<Record>>> idxByPQgram;
	protected ArrayList<List<Record>> idxByLen;
	/*
	 * idxByPQGram is used to filter out non-matching pairs by applying the positional q-gram filtering.
	 * Since the filtering method can miss some matching pairs,
	 * we use idxByLen to find all of such pairs.
	 * The range of keys for idxByLen is from 0 to qgramSize * deltaMax - 1 = qd-1.
	 * idxByLen[0] is the list of recTs whose length is 1.
	 */
	protected final int qgramSize;
	protected final int deltaMax;
	protected final int qd;
	protected final boolean isSelfJoin;
  
	protected long indexTime;

	protected long candQGramCount = 0;
	protected long nCandByPQF = 0;
	protected long nCandByLen = 0;
	protected long equivComparisons = 0;
	protected long candQGramCountTime = 0;
	protected long filterTime = 0;
	protected long verifyTime = 0;
	
	public static boolean useLF = true;
	public static boolean usePQF = true;

	public JoinDeltaIndex( int qgramSize, int deltaMax, Query query, StatContainer stat ) {
		this.qgramSize = qgramSize;
		this.deltaMax = deltaMax;
		this.qd = qgramSize * deltaMax;
		this.isSelfJoin = query.selfJoin;

		long ts = System.nanoTime();
		idxByPQgram = new ArrayList<WYK_HashMap<QGram, List<Record>>>();
		idxByLen = new ArrayList<>();
		
		for ( Record recT : query.indexedSet.recordList ) {
			int lenT = recT.size();
			if ( lenT <= qd ) {
				while ( idxByLen.size() < lenT ) idxByLen.add( new ArrayList<>() ); 
				idxByLen.get(lenT-1).add(recT);
			}
			List<List<QGram>> availableQGrams = recT.getSelfQGrams(qgramSize);
			while ( idxByPQgram.size() < availableQGrams.size() ) idxByPQgram.add( new WYK_HashMap<>() );

			for ( int k=0; k<availableQGrams.size(); ++k ) {
				WYK_HashMap<QGram, List<Record>> kthMap = idxByPQgram.get(k);
				QGram qgram = availableQGrams.get(k).get(0);
//				if ( recT.getID() == 3232 ) System.out.println( qgram+", "+k);
				if ( !kthMap.containsKey(qgram) ) kthMap.put( qgram, new ArrayList<Record>() );
				kthMap.get(qgram).add(recT);
			}
		}
		indexTime = System.nanoTime() - ts;
		
		stat.add("Stat_IdxByPQgram_Size", sizeIdxByPQGram() );
		stat.add("Stat_IdxByLen_Size", sizeIdxByLen() );
	}

	@Override
	protected void postprocessAfterJoin(StatContainer stat) {
		stat.add( "Stat_CandQGramCount", candQGramCount );
		stat.add( "Stat_CandByPQF", nCandByPQF );
		stat.add( "Stat_CandByLen", nCandByLen );
		stat.add( "Result_5_1_Filter_Time", filterTime/1e6 );
		stat.add( "Result_5_2_Verify_Time", verifyTime/1e6 );
	}

	protected List<List<QGram>> getCandidatePQGrams( Record rec ) {
		List<List<QGram>> availableQGrams = rec.getQGrams( qgramSize );
		List<List<QGram>> candidatePQGrams = new ArrayList<List<QGram>>();
		for ( int k=0; k<availableQGrams.size(); ++k ) {
			List<QGram> qgrams = new ArrayList<QGram>();
			for ( QGram qgram : availableQGrams.get( k ) ) {
				for ( int kd=Math.max(0, k-deltaMax); kd<=k+deltaMax; ++kd ) {
					if ( kd >= idxByPQgram.size() ) continue;
					WYK_HashMap<QGram, List<Record>> curidx = idxByPQgram.get( kd );
					if ( !curidx.containsKey( qgram ) ) continue;
					qgrams.add( qgram );
					break;
				}
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
				for ( int kd=Math.max(0, k-deltaMax); kd<=k+deltaMax; ++kd ) {
//					if ( recS.getID() == 3235 ) System.out.println(qgram+", "+kd);
					if ( idxByPQgram.size() <= kd ) continue;
					if ( !idxByPQgram.get(kd).containsKey(qgram ) ) continue;
					for ( Record recT : idxByPQgram.get(kd).get(qgram) ) { kthCandidates.add( recT );
					}
				}
			} // end for qgram in availableQgrams.get(k)
			
			for ( Record recT : kthCandidates ) {
				useLF = false;
				if ( !useLF || StaticFunctions.overlap(rangeS[0] - deltaMax, rangeS[1] + deltaMax, recT.size(), recT.size())) {
					candidatesCount.addTo(recT, 1);
				}
				else ++checker.lengthFiltered;
			}
		} // end for k
		
		Set<Record> candidates = new WYK_HashSet<>();
		for ( Map.Entry<Record, Integer> entry : candidatesCount.entrySet() ) {
			Record recT = entry.getKey();
			int count = entry.getValue().intValue();
//			if ( recS.getID() == 3235 ) System.out.println(recT.getID()+", "+count);
			
			if ( !usePQF || count >= Math.max(rangeS[0], recT.size()) - qd ) {
				candidates.add( recT );
			}
		}
		nCandByPQF += candidates.size();
		
		// utilize idxByLen for short strings
		if ( rangeS[0] <= qd ) {
			for ( int l=0; l<qd; ++l ) {
				candidates.addAll( idxByLen.get(l) );
				nCandByLen += idxByLen.get(l).size();
			}
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

	protected int sizeIdxByPQGram() {
		int size = 0;
		for ( WYK_HashMap<QGram, List<Record>> posMap : idxByPQgram ) {
			for ( List<Record> invList : posMap.values() ) size += invList.size();
		}
		return size;
	}

	protected int sizeIdxByLen() {
		int size = 0;
		for ( List<Record> invList : idxByLen ) size += invList.size();
		return size;
	}
}
