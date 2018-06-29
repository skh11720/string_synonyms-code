package snu.kdd.synonym.synonymRev.algorithm;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.ParseException;

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


public class JoinSetNaive extends AlgorithmTemplate {

	// staticitics used for building indexes
	double avgTransformed;
	
	private WYK_HashMap<IntOpenHashSet, IntArrayList> idxS;
	private WYK_HashMap<IntOpenHashSet, IntArrayList> idxT;
	private long candTokenTime = 0;
	private long isInSigUTime = 0;
	private long filteringTime = 0;
	private long validateTime = 0;
	private long nScanList = 0;

	public JoinSetNaive( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}

	public void preprocess() {
		super.preprocess();
		
		for (Record rec : query.indexedSet.get()) {
			rec.preprocessSuffixApplicableRules();
		}
		
		if ( !query.selfJoin ) {
			for ( Record rec : query.searchedSet.get()) {
				rec.preprocessSuffixApplicableRules();
			}
		}

		
//		double estTransformed = 0.0;
//		for( Record rec : query.indexedSet.get() ) {
//			estTransformed += rec.getEstNumTransformed();
//		}
//		avgTransformed = estTransformed / query.indexedSet.size();
	}

	@Override
	public void run( Query query, String[] args ) throws IOException, ParseException {
//		this.threshold = Long.valueOf( args[ 0 ] );
//		ParamPkduck params = ParamPkduck.parseArgs( args, stat, query );
//		this.threshold = -1;

		StopWatch stepTime = StopWatch.getWatchStarted( "Result_2_Preprocess_Total_Time" );

		preprocess();

		stepTime.stopAndAdd( stat );
		stat.addMemory( "Mem_2_Preprocessed" );
		stepTime.resetAndStart( "Result_3_Run_Time" );

		rslt = runAfterPreprocess( true );

		stepTime.stopAndAdd( stat );
		stepTime.resetAndStart( "Result_4_Write_Time" );

		this.writeResult( rslt );

		stepTime.stopAndAdd( stat );
//		checker.addStat( stat );
	}

	public Set<IntegerPair> runAfterPreprocess( boolean addStat ) {
		// Index building
		StopWatch stepTime = null;
		if( addStat ) {
			stepTime = StopWatch.getWatchStarted( "Result_3_1_Index_Building_Time" );
		}
		else {
//			if( DEBUG.SampleStatON ) {
//				stepTime = StopWatch.getWatchStarted( "Sample_1_Naive_Index_Building_Time" );
//			}
			try { throw new Exception("UNIMPLEMENTED CASE"); }
			catch( Exception e ) { e.printStackTrace(); }
		}

		buildIndex( false );

		if( addStat ) {
			stepTime.stopAndAdd( stat );
			stepTime.resetAndStart( "Result_3_2_Join_Time" );
			stat.addMemory( "Mem_3_BuildIndex" );
		}
		else {
			if( DEBUG.SampleStatON ) {
				stepTime.stopAndAdd( stat );
				stepTime.resetAndStart( "Sample_2_Pkduck_Join_Time" );
			}
		}

		// Join
		final Set<IntegerPair> rslt = join( stat, query, addStat );

		if( addStat ) {
			stepTime.stopAndAdd( stat );
			stat.addMemory( "Mem_4_Joined" );
		}
//		else {
//			if( DEBUG.SampleStatON ) {
//				stepTime.stopAndAdd( stat );
//				stat.add( "Stat_Expanded", idx.totalExp );
//			}
//		}
//
//		if( DEBUG.NaiveON ) {
//			if( addStat ) {
//				idx.addStat( stat, "Counter_Join" );
//			}
//		}
//		stat.add( "idx_skipped_counter", idx.skippedCount );

		return rslt;
	}
	
	public void buildIndex(boolean addStat ) {
		idxT = new WYK_HashMap<IntOpenHashSet, IntArrayList>();
		for ( int i=0; i<query.indexedSet.size(); i++ ) {
			Record recT = query.indexedSet.getRecord( i );
			IntOpenHashSet key = new IntOpenHashSet(recT.getTokens());
			if ( !idxT.containsKey( key ) ) idxT.put( key, new IntArrayList() );
			idxT.get( key ).add(i);
		}

		if ( !query.selfJoin ) {
			idxS = new WYK_HashMap<IntOpenHashSet, IntArrayList>();
			for ( int i=0; i<query.searchedSet.size(); i++ ) {
				Record recS = query.searchedSet.getRecord( i );
				IntOpenHashSet key = new IntOpenHashSet(recS.getTokens());
				if ( !idxS.containsKey( key ) ) idxS.put( key, new IntArrayList() );
				idxS.get( key ).add(i);
			}
		}
	}
	
	public Set<IntegerPair> join(StatContainer stat, Query query, boolean addStat) {
		ObjectOpenHashSet<IntegerPair> rslt = new ObjectOpenHashSet<IntegerPair>();
		if ( !query.oneSideJoin ) throw new RuntimeException("UNIMPLEMENTED CASE");
		
		// S -> S' ~ T
		for ( Record recS : query.searchedSet.recordList ) {
			joinOneRecord( recS, rslt, idxT );
		}
		
		if ( !query.selfJoin ) {
			// T -> T' ~ S
			for ( Record recT : query.indexedSet.recordList ) {
				joinOneRecord( recT, rslt, idxS );
			}
		}
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
		return "1.0";
	}
}
