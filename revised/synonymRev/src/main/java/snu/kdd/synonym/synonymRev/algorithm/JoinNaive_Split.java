package snu.kdd.synonym.synonymRev.algorithm;

import java.io.IOException;
import java.util.List;

import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.index.NaiveIndex_Split;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;

public class JoinNaive_Split extends AlgorithmTemplate {

	public NaiveIndex_Split idx;
	public long threshold = Long.MAX_VALUE;

	// staticitics used for building indexes
	double avgTransformed;

	public JoinNaive_Split( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}

	public void preprocess() {
		super.preprocess();

		double estTransformed = 0.0;
		for( Record rec : query.indexedSet.get() ) {
			estTransformed += rec.getEstNumTransformed();
		}
		avgTransformed = estTransformed / query.indexedSet.size();
	}

	@Override
	public void run( Query query, String[] args ) {
		this.threshold = Long.valueOf( args[ 0 ] );

		StopWatch stepTime = StopWatch.getWatchStarted( "Result_2_Preprocess_Total_Time" );

		if( DEBUG.NaiveON ) {
			stat.addPrimary( "cmd_threshold", threshold );
		}

		preprocess();

		stepTime.stopAndAdd( stat );
		stat.addMemory( "Mem_2_Preprocessed" );
		stepTime.resetAndStart( "Result_3_Run_Time" );

		rslt = runAfterPreprocess( true );

		stepTime.stopAndAdd( stat );
		stepTime.resetAndStart( "Result_4_Write_Time" );

		this.writeResult();

		stepTime.stopAndAdd( stat );
	}

	public List<IntegerPair> runAfterPreprocess( boolean addStat ) {
		// Index building
		StopWatch stepTime = null;
		if( addStat ) {
			stepTime = StopWatch.getWatchStarted( "Result_3_1_Index_Building_Time" );
		}

		idx = NaiveIndex_Split.buildIndex( avgTransformed, stat, threshold, addStat, query );

		if( addStat ) {
			stepTime.stopAndAdd( stat );
			stepTime.resetAndStart( "Result_3_2_Join_Time" );
			stat.addMemory( "Mem_3_BuildIndex" );
		}

		// Join
		final List<IntegerPair> rslt = idx.join( stat, threshold, addStat, query );

		if( addStat ) {
			stepTime.stopAndAdd( stat );
			stat.addMemory( "Mem_4_Joined" );
		}

		if( DEBUG.NaiveON ) {
			if( addStat ) {
				idx.addStat( stat, "Counter_Join" );
			}
		}

		return rslt;
	}

	@Override
	public String getName() {
		return "JoinNaiveSP";
	}

	@Override
	public String getVersion() {
		return "2.0";
	}

}
