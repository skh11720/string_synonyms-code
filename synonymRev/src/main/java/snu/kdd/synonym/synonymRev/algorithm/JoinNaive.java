package snu.kdd.synonym.synonymRev.algorithm;

import java.io.IOException;
import java.util.Set;

import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.index.NaiveIndex;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;

public class JoinNaive extends AlgorithmTemplate {

	public NaiveIndex idx;

	// statistics used for building indexes
	public double avgTransformed;

	public JoinNaive(Query query, String[] args) throws IOException, ParseException {
		super(query, args);
	}

	@Override
	public void preprocess() {
		super.preprocess();

		double estTransformed = 0.0;
		for( Record rec : query.searchedSet.get() ) {
			estTransformed += rec.getEstNumTransformed();
		}
		avgTransformed = estTransformed / query.searchedSet.size();
	}

	@Override
	public void run() {
		StopWatch stepTime = StopWatch.getWatchStarted( "Result_2_Preprocess_Total_Time" );
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

	public Set<IntegerPair> runAfterPreprocess( boolean addStat ) {
		// Index building
		StopWatch stepTime = null;
		if( addStat ) {
			stepTime = StopWatch.getWatchStarted( "Result_3_1_Index_Building_Time" );
		}
		else {
			if( DEBUG.SampleStatON ) {
				stepTime = StopWatch.getWatchStarted( "Sample_1_Naive_Index_Building_Time" );
			}
		}

		idx = new NaiveIndex( query, stat, addStat, avgTransformed );

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
		final Set<IntegerPair> rslt = idx.join( query, stat, null, writeResult );

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

//		if( DEBUG.NaiveON ) {
//			if( addStat ) {
//				idx.addStat( stat, "Counter_Join" );
//			}
//		}
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
		return "JoinNaive";
	}

	@Override
	public String getVersion() {
		/*
		 * 2.00: the latest version by yjpark
		 * 2.01: checkpoint
		 * 2.02: ignore strings with too many transformations
		 */
		return "2.02";
	}
}
