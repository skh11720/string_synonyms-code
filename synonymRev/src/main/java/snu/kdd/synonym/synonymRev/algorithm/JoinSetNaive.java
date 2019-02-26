package snu.kdd.synonym.synonymRev.algorithm;

import java.util.List;
import java.util.Set;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.tools.WYK_HashMap;


public class JoinSetNaive extends AbstractAlgorithm {

	// staticitics used for building indexes
	double avgTransformed;
	
	private WYK_HashMap<IntOpenHashSet, IntArrayList> idxS;
	private WYK_HashMap<IntOpenHashSet, IntArrayList> idxT;
//	private long candTokenTime = 0;
//	private long isInSigUTime = 0;
//	private long filteringTime = 0;
//	private long validateTime = 0;
//	private long nScanList = 0;


	public JoinSetNaive(Query query, String[] args) {
		super(query, args);
	}
	
	@Override
	protected void executeJoin() {
		StopWatch stepTime = null;
		stepTime = StopWatch.getWatchStarted( INDEX_BUILD_TIME );
		buildIndex( false );
		stepTime.stopAndAdd( stat );
		stepTime.resetAndStart( JOIN_AFTER_INDEX_TIME );
		stat.addMemory( "Mem_3_BuildIndex" );

		rslt = join( stat, query, writeResultOn );
		stepTime.stopAndAdd( stat );
		stat.addMemory( "Mem_4_Joined" );
	}
	
	public void buildIndex(boolean addStat ) {
		idxT = new WYK_HashMap<IntOpenHashSet, IntArrayList>();
		for ( int i=0; i<query.indexedSet.size(); i++ ) {
			Record recT = query.indexedSet.getRecord( i );
			IntOpenHashSet key = new IntOpenHashSet(recT.getTokens());
			if ( !idxT.containsKey( key ) ) idxT.put( key, new IntArrayList() );
			idxT.get( key ).add(i);
		}

//		if ( !query.selfJoin ) {
//			idxS = new WYK_HashMap<IntOpenHashSet, IntArrayList>();
//			for ( int i=0; i<query.searchedSet.size(); i++ ) {
//				Record recS = query.searchedSet.getRecord( i );
//				IntOpenHashSet key = new IntOpenHashSet(recS.getTokens());
//				if ( !idxS.containsKey( key ) ) idxS.put( key, new IntArrayList() );
//				idxS.get( key ).add(i);
//			}
//		}
	}
	
	public Set<IntegerPair> join(StatContainer stat, Query query, boolean addStat) {
		ObjectOpenHashSet<IntegerPair> rslt = new ObjectOpenHashSet<IntegerPair>();
		if ( !query.oneSideJoin ) throw new RuntimeException("UNIMPLEMENTED CASE");
		
		// S -> S' ~ T
		for ( Record recS : query.searchedSet.recordList ) {
			if ( recS.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) continue;
			joinOneRecord( recS, rslt, idxT );
		}
		
//		if ( !query.selfJoin ) {
//			// T -> T' ~ S
//			for ( Record recT : query.indexedSet.recordList ) {
//				if ( recT.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) continue;
//				joinOneRecord( recT, rslt, idxS );
//			}
//		}
		return rslt;
	}
	
	private void joinOneRecord( Record rec, Set<IntegerPair> rslt, WYK_HashMap<IntOpenHashSet, IntArrayList> idx ) {
		final List<Record> expanded = rec.expandAll();
		for ( final Record exp : expanded ) {
			IntOpenHashSet key = new IntOpenHashSet( exp.getTokens() );
			final List<Integer> overlapidx = idx.get( key );
			if ( overlapidx == null ) continue;
			for ( int i : overlapidx ) {
				if ( query.selfJoin ) {
					int id_smaller = rec.getID() < i ? rec.getID() : i;
					int id_larger = rec.getID() >= i ? rec.getID() : i;
					rslt.add( new IntegerPair( id_smaller, id_larger) ); // it is better modify this to use addSetResult method...
				}
				else {
					if ( idx == idxT ) rslt.add( new IntegerPair( rec.getID(), i ) );
					else if ( idx == idxS ) rslt.add( new IntegerPair( i, rec.getID()) );
					else throw new RuntimeException("Unexpected error");
				}
			}
		}
	}

	@Override
	public String getName() {
		return "JoinSetNaive";
	}

	@Override
	public String getVersion() {
		/*
		 * 1.00: initial version
		 * 1.01: ignore records with too many transformations
		 */
		return "1.01";
	}
}
