package snu.kdd.synonym.synonymRev.algorithm.delta;

import java.io.IOException;
import java.util.Collection;

import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.synonymRev.algorithm.JoinHybridAll;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.Param;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;

/**
 * Given threshold, if a record has more than 'threshold' 1-expandable strings,
 * use an index to store them.
 * Otherwise, generate all 1-expandable strings and then use them to check
 * if two strings are equivalent.
 * Utilize only one index by sorting records according to their expanded size.
 * It first build JoinMin(JoinH2Gram) index and then change threshold / modify
 * index in order to find the best execution time.
 */
public class JoinHybridAllDelta extends JoinHybridAll {
	public JoinHybridAllDelta( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}

	private int deltaMax;

	@Override
	public void run( Query query, String[] args ) throws IOException, ParseException {
		Param params = Param.parseArgs( args, stat, query );
		// Setup parameters
		checker = params.validator;
		qSize = params.qgramSize;
		indexK = params.indexK;
		deltaMax = params.delta;
		sampleRatio = params.sampleRatio;

		StopWatch stepTime = StopWatch.getWatchStarted( "Result_2_Preprocess_Total_Time" );
		preprocess();
		stepTime.stopAndAdd( stat );
		// Retrieve statistics

		stepTime.resetAndStart( "Result_3_Run_Time" );
		// Estimate constants

		Collection<IntegerPair> rslt = join();
		stepTime.stopAndAdd( stat );
		stat.addMemory( "Mem_4_Joined" );

		stepTime.resetAndStart( "Result_4_Write_Time" );
		writeResult( rslt );
		stepTime.stopAndAdd( stat );
	}

	@Override
	protected void buildJoinMinIndex() {
		// Build an index
		joinMinIdx = new JoinMinDeltaIndex( indexK, qSize, deltaMax, stat, query, joinThreshold, true );
	}

	@Override
	protected void buildJoinMHIndex() {
		// Build an index
		int[] index = new int[ indexK ];
		for( int i = 0; i < indexK; i++ ) {
			index[ i ] = i;
		}
		joinMHIdx = new JoinMHDeltaIndex( indexK, qSize, deltaMax, query.indexedSet.get(), query, stat, index, true, true, joinThreshold );
	}

	@Override
	protected void buildNaiveIndex() {
		naiveIndex = new NaiveDeltaIndex( query.indexedSet, query, stat, true, deltaMax, joinThreshold, joinThreshold / 2 );
	}

	@Override
	public String getVersion() {
		return "1.00";
	}

	@Override
	public String getName() {
		return "JoinHybridAllDelta";
	}
}
