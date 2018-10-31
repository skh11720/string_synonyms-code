package snu.kdd.synonym.synonymRev.algorithm.delta;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.synonymRev.algorithm.misc.SampleDataTest;
import snu.kdd.synonym.synonymRev.data.Dataset;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.index.NaiveIndex;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.Util;

public class NaiveDeltaIndex extends NaiveIndex {

	private Int2ObjectOpenHashMap<List<Record>> token2rec;
	private Int2ObjectOpenHashMap<List<Record>> len2rec;
	private Object2IntOpenHashMap<Record> counter;
	private final int deltaMax;
//	private final DeltaValidator checker;
	private long countTime = 0;
	private long candidateTime = 0;
	private long validateTime = 0;
	private long validateCount = 0;
	
	private final Query query; // for debugging

	public NaiveDeltaIndex( Dataset indexedSet, Query query, StatContainer stat, boolean addStat, int deltaMax, long threshold, double avgTransformed ) {
		super( indexedSet, query, stat, addStat, threshold, avgTransformed );
		counter = new Object2IntOpenHashMap<Record>();
		counter.defaultReturnValue( 0 );
		if ( deltaMax < 0 ) throw new RuntimeException("deltaMax must be a nonnegative integer, not "+deltaMax+".");
		this.deltaMax = deltaMax;
//		this.checker = new DeltaValidator( deltaMax );

		token2rec = new Int2ObjectOpenHashMap<List<Record>>();
		len2rec = new Int2ObjectOpenHashMap<List<Record>>();
		for ( Record rec : indexedSet.recordList ) {
			for ( int token : rec.getTokens() ) {
				if ( !token2rec.containsKey( token ) ) token2rec.put( token, new ObjectArrayList<Record>() );
				token2rec.get( token ).add( rec );
			}
			int len = rec.size();
			if ( !len2rec.containsKey( len ) ) len2rec.put( len, new ObjectArrayList<Record>() );
			len2rec.get( len ).add( rec );
		}
		
		
		this.query = query;
	}

	@Override
	public void joinOneRecord( Record recS, Set<IntegerPair> rslt ) {
		boolean debug = false;
//		if ( recS.getID() == 509 ) debug = true;
//		if ( recS.getID() == 677 ) debug = true;
		if (debug) {
			recS.preprocessSuffixApplicableRules();
			SampleDataTest.inspect_record( recS, query, 1 );
		}
		IntOpenHashSet checked = new IntOpenHashSet();
		if (debug) System.out.println( "recS: "+Arrays.toString( recS.getTokensArray() ) );
		for ( Record expS: recS.expandAll() ) {
//			if (debug) System.out.println( "expS: "+Arrays.toString( expS.getTokensArray() ) );
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
				if ( entry.getValue() >= Math.ceil((len_expS + recT.size() - deltaMax)/2 )) candidates.add(recT);
			}
			long afterCandidateTime = System.currentTimeMillis();
			
			for ( Record recT : candidates ) {
//				if ( recT.getID() == -1 ) throw new RuntimeException();
				if ( checked.contains( recT.getID() ) ) continue;
//				else checked.add( recT.getID() );
				if (debug) System.out.println( "expS: "+Arrays.toString( expS.getTokensArray() ) );
				if (debug) System.out.println( "recT: "+recT.getID()+", "+Arrays.toString( recT.getTokensArray() ) );
				++validateCount;
				int d_lcs = len_expS + recT.size() - 2*Util.lcs( expS.getTokensArray(), recT.getTokensArray() );
				if (debug) System.out.println( "d_lcs: "+d_lcs );
				if ( d_lcs <= deltaMax ) {
//					for ( int id : idx.get( recT ) ) {
//						if (debug) System.out.println( "output: "+recS.getID()+", "+id );
//						if ( recS.getID() == 598 ) System.out.println( "output: "+recS.getID()+", "+recT.getID() );
//						if ( recS.getID() == 677 ) System.out.println( "output: "+recS.getID()+", "+recT.getID() );
						AlgorithmTemplate.addSeqResult( recS, recT, rslt, isSelfJoin );
						checked.add( recT.getID() );
//					}
				}
			}
			long afterValidateTime = System.currentTimeMillis();
				
			countTime += afterCountTime - ts;
			candidateTime += afterCandidateTime - afterCountTime;
			validateTime += afterValidateTime - afterCandidateTime;
		}
		
		// add trivial solutions: |s'|+|t|<=deltaMax
		for ( int len=1; len<=deltaMax-recS.getMinTransLength(); ++len 	) {
			if ( !len2rec.containsKey( len ) ) continue;
			for ( Record recT : len2rec.get( len ) ) {
				if ( checked.contains( recT.getID() ) ) continue;
				AlgorithmTemplate.addSeqResult( recS, recT, rslt, isSelfJoin );
			}
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
