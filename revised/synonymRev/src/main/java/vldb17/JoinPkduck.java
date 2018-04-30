package vldb17;

import java.io.IOException;
import java.util.List;

import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.index.NaiveIndex;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;

public class JoinPkduck extends AlgorithmTemplate {

	public NaiveIndex idx;
	public long threshold = Long.MAX_VALUE;
	public final int qgramSize = 1; // a string is represented as a set of (token, pos) pairs.

	// staticitics used for building indexes
	double avgTransformed;

	public JoinPkduck( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}

	public void preprocess() {
		super.preprocess();

//		double estTransformed = 0.0;
//		for( Record rec : query.indexedSet.get() ) {
//			estTransformed += rec.getEstNumTransformed();
//		}
//		avgTransformed = estTransformed / query.indexedSet.size();
	}

	@Override
	public void run( Query query, String[] args ) {
//		this.threshold = Long.valueOf( args[ 0 ] );
		this.threshold = -1;

		StopWatch stepTime = StopWatch.getWatchStarted( "Result_2_Preprocess_Total_Time" );

		if( DEBUG.NaiveON ) {
			stat.addPrimary( "cmd_threshold", threshold );
		}

		preprocess();

		stepTime.stopAndAdd( stat );
		stat.addMemory( "Mem_2_Preprocessed" );
		stepTime.resetAndStart( "Result_3_Run_Time" );

		final List<IntegerPair> list = runAfterPreprocess( true );

		stepTime.stopAndAdd( stat );
		stepTime.resetAndStart( "Result_4_Write_Time" );

		this.writeResult( list );

		stepTime.stopAndAdd( stat );
	}

	public List<IntegerPair> runAfterPreprocess( boolean addStat ) {
		// Index building
		StopWatch stepTime = null;
		if( addStat ) {
			stepTime = StopWatch.getWatchStarted( "Result_3_1_Index_Building_Time" );
		}
		else {
//			if( DEBUG.SampleStatON ) {
//				stepTime = StopWatch.getWatchStarted( "Sample_1_Naive_Index_Building_Time" );
//			}
			try { throw new Exception("DISABLED CASE"); }
			catch( Exception e ) { e.printStackTrace(); }
		}

		idx = NaiveIndex.buildIndex( avgTransformed, stat, threshold, addStat, query );

		if( addStat ) {
			stepTime.stopAndAdd( stat );
			stepTime.resetAndStart( "Result_3_2_Join_Time" );
			stat.addMemory( "Mem_3_BuildIndex" );
		}
		else {
			if( DEBUG.SampleStatON ) {
				stepTime.stopAndAdd( stat );
				stepTime.resetAndStart( "Sample_2_Naive_Join_Time" );
			}
		}

		// Join
		final List<IntegerPair> rslt = idx.join( stat, threshold, addStat, query );

		if( addStat ) {
			stepTime.stopAndAdd( stat );
			stat.addMemory( "Mem_4_Joined" );
			stat.add( "Stat_Expanded", idx.totalExp );
		}
		else {
			if( DEBUG.SampleStatON ) {
				stepTime.stopAndAdd( stat );
				stat.add( "Stat_Expanded", idx.totalExp );
			}
		}

		if( DEBUG.NaiveON ) {
			if( addStat ) {
				idx.addStat( stat, "Counter_Join" );
			}
		}
		stat.add( "idx_skipped_counter", idx.skippedCount );

		return rslt;
	}

	public double getAlpha() {
		return idx.alpha;
	}

	public double getBeta() {
		return idx.beta;
	}

	@Override
	public String getName() {
		return "JoinPkduck";
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

}
