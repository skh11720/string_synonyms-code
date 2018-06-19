package snu.kdd.synonym.synonymRev.algorithm.delta;

import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.synonymRev.data.Dataset;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.index.NaiveIndex;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.Util;

public class NaiveDeltaIndex extends NaiveIndex {

	private Int2ObjectOpenHashMap<List<Record>> token2rec;
	private Object2IntOpenHashMap<Record> counter;
	private final int deltaMax;
//	private final DeltaValidator checker;
	private long countTime = 0;
	private long candidateTime = 0;
	private long validateTime = 0;
	private long validateCount = 0;

	public NaiveDeltaIndex( Dataset indexedSet, Query query, StatContainer stat, boolean addStat, int deltaMax, long threshold, double avgTransformed ) {
		super( indexedSet, query, stat, addStat, threshold, avgTransformed );
		counter = new Object2IntOpenHashMap<Record>();
		counter.defaultReturnValue( 0 );
		if ( deltaMax < 0 ) throw new RuntimeException("deltaMax must be a nonnegative integer, not "+deltaMax+".");
		this.deltaMax = deltaMax;
//		this.checker = new DeltaValidator( deltaMax );

		token2rec = new Int2ObjectOpenHashMap<List<Record>>();
		for ( Record rec : idx.keySet() ) {
			for ( int token : rec.getTokens() ) {
				if ( !token2rec.containsKey( token ) ) token2rec.put( token, new ObjectArrayList<Record>() );
				token2rec.get( token ).add( rec );
			}
		}
	}

	@Override
	public void joinOneRecord( Record recS, Set<IntegerPair> rslt ) {
		for ( Record expS: recS.expandAll() ) {
			counter.clear();
			long ts = System.currentTimeMillis();
			int len_expS = expS.size();
			for ( int token : expS.getTokens() ) {
				if ( ! token2rec.containsKey( token ) ) continue;
				for ( Record recT: token2rec.get( token ) ) counter.addTo( recT, 1 );
			}
			long afterCountTime = System.currentTimeMillis();
			
			Set<Record> candidates = new ObjectOpenHashSet<Record>();
			for ( Entry<Record, Integer> entry : counter.entrySet() ) {
				Record recT = entry.getKey();
				if ( entry.getValue() >= (len_expS + recT.size() - deltaMax)/2 ) candidates.add(recT);
			}
			long afterCandidateTime = System.currentTimeMillis();
			
			for ( Record recT : candidates ) {
				++validateCount;
				if ( len_expS + recT.size() - 2*Util.lcs( expS.getTokensArray(), recT.getTokensArray() ) <= deltaMax ) {
					for ( int id : idx.get( recT ) ) AlgorithmTemplate.addSeqResult( recS, id, rslt, isSelfJoin );
				}
			}
			long afterValidateTime = System.currentTimeMillis();
				
			countTime += afterCountTime - ts;
			candidateTime += afterCandidateTime - afterCountTime;
			validateTime += afterValidateTime - afterCandidateTime;
		}
	}

	@Override
	public void addStatAfterJoin( StatContainer stat ) {
		stat.add( "Join_CountTime", countTime );
		stat.add( "Join_CandidateTime", candidateTime );
		stat.add( "Join_ValidateTime", validateTime );
		stat.add( "Join_ValidateCount", validateCount );
	}
}
