package snu.kdd.synonym.synonymRev.algorithm.delta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.index.AbstractIndex;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.ResultSet;
import snu.kdd.synonym.synonymRev.tools.Stat;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StaticFunctions;
import snu.kdd.synonym.synonymRev.tools.WYK_HashMap;
import snu.kdd.synonym.synonymRev.tools.WYK_HashSet;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class JoinDeltaSimpleIndex extends AbstractIndex {

	protected ArrayList<WYK_HashMap<QGram, List<Record>>> idxByPQgram;
	protected ArrayList<List<Record>> idxByLen;
	/*
	 * idxByPQGram is used to filter out non-matching pairs by applying the positional q-gram filtering.
	 * Since the filtering method can miss some matching pairs,
	 * we use idxByLen to find all of such pairs.
	 * The range of keys for idxByLen is from 0 to qSize * deltaMax - 1 = qd-1.
	 * idxByLen[0] is the list of recTs whose length is 1.
	 */
	protected final int qSize;
	protected final int deltaMax;
	protected final int qd;
	protected final boolean isSelfJoin;
  
	protected long indexTime;

	protected long candQGramCount = 0;
	protected long nCandByPQF = 0;
	protected long nCandByLen = 0;
	protected long candQGramCountTime = 0;
	protected long filterTime = 0;
	protected long verifyTime = 0;
	
	public static boolean useLF = true;
	public static boolean usePQF = true;

	public JoinDeltaSimpleIndex( int qSize, int deltaMax, Query query ) {
		this.qSize = qSize;
		this.deltaMax = deltaMax;
		this.qd = qSize * deltaMax;
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
			List<List<QGram>> availableQGrams = recT.getSelfQGrams(qSize);
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
	}

	@Override
	public void joinOneRecord( Record recS, ResultSet rslt, Validator checker ) {
	    long ts = System.nanoTime();
		List<List<QGram>> availableQGrams = getCandidatePQGrams( recS );
		for (List<QGram> list : availableQGrams) {
			candQGramCount += list.size();
		}
		long afterCandQgramTime = System.nanoTime();
		
		Object2IntOpenHashMap<Record> candidatesCount = getCandidatesCount(recS, availableQGrams);
		Set<Record> candidates = getCandidates(recS, candidatesCount);
		long afterFilterTime = System.nanoTime();
		
		verify(recS, candidates, checker, rslt);
		long afterVerifyTime = System.nanoTime();

		candQGramCountTime += afterCandQgramTime - ts; // time to enumerate delta-variants of qgrams in STPQ for a string recS
		filterTime += afterFilterTime - afterCandQgramTime; // time to filter out by applying the length and pos q-gram filtering
		verifyTime += afterVerifyTime - afterFilterTime; // time to verify the remaining string pairs
	}

	protected List<List<QGram>> getCandidatePQGrams( Record rec ) {
		List<List<QGram>> availableQGrams = rec.getQGrams( qSize );
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

	protected Object2IntOpenHashMap<Record> getCandidatesCount( final Record recS, final List<List<QGram>> availableQGrams ) {
		return getCandidatesCount(recS, availableQGrams, null);
	}

	protected Object2IntOpenHashMap<Record> getCandidatesCount( final Record recS, final List<List<QGram>> availableQGrams, Set<Record> candidates ) {
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
					for ( Record recT : idxByPQgram.get(kd).get(qgram) ) { 
						kthCandidates.add( recT );
					}
				}
			} // end for qgram in availableQgrams.get(k)
			
			for ( Record recT : kthCandidates ) {
				if ( candidates != null && !candidates.contains(recT) ) continue;
				if ( !useLF || StaticFunctions.overlap(rangeS[0] - deltaMax, rangeS[1] + deltaMax, recT.size(), recT.size())) {
					candidatesCount.addTo(recT, 1);
				}
			}
		} // end for k
		return candidatesCount;
	}
	
	protected Set<Record> getCandidates( final Record recS, final Object2IntOpenHashMap<Record> candidatesCount ) {
		int[] rangeS = recS.getTransLengths();
		Set<Record> candidates = new WYK_HashSet<>();
		for ( Map.Entry<Record, Integer> entry : candidatesCount.entrySet() ) {
			Record recT = entry.getKey();
			int count = entry.getValue().intValue();
//			if ( recS.getID() == 3235 ) System.out.println(recT.getID()+", "+count);
			if ( !usePQF || count >= Math.max(rangeS[0], recT.size()) - qd ) {
				candidates.add( recT );
			}
		}
		final int thisNCandByPQF = candidates.size();
		nCandByPQF += candidates.size();

		// utilize idxByLen for short strings
		if ( rangeS[0] <= qd ) {
			for ( int l=1; l<=idxByLen.size(); ++l ) {
				if ( !useLF || StaticFunctions.overlap(rangeS[0] - deltaMax, rangeS[1] + deltaMax, l, l )) { // apply the length filtering 
					candidates.addAll( idxByLen.get(l-1) );
				}
			}
		}
		nCandByLen += candidates.size() - thisNCandByPQF;
		return candidates;
	}

	protected void verify( final Record recS, Set<Record> candidates, Validator checker, ResultSet rslt ) {
		for ( Record recT : candidates ) {
			if ( rslt.contains(recS, recT) ) continue;
			if ( checker.isEqual( recS, recT ) >= 0 ) {
				rslt.add(recS, recT);
			}
		}
	}

	@Override
	protected void postprocessAfterJoin(StatContainer stat) {
		stat.add(Stat.INDEX_SIZE, sizeIdxByPQGram() );
		stat.add(Stat.LEN_INDEX_SIZE, sizeIdxByLen() );
		stat.add(Stat.CAND_PQGRAM_COUNT, candQGramCount );
		stat.add(Stat.CAND_BY_PQGRAM, nCandByPQF );
		stat.add(Stat.CAND_BY_LEN, nCandByLen );
		stat.add(Stat.CAND_PQGRAM_TIME, candQGramCountTime/1e6 );
		stat.add(Stat.FILTER_TIME, filterTime/1e6 );
		stat.add(Stat.VERIFY_TIME, verifyTime/1e6 );
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
