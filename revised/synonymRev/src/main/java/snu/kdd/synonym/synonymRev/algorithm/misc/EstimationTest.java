package snu.kdd.synonym.synonymRev.algorithm.misc;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.estimation.SampleEstimate;
import snu.kdd.synonym.synonymRev.index.JoinMinIndex;
import snu.kdd.synonym.synonymRev.index.NaiveIndex;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.Param;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class EstimationTest extends AlgorithmTemplate {

	public EstimationTest( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}

	public Validator checker;
	SampleEstimate estimate;
	private int qSize = 0;
	private int indexK = 0;
	private double sampleRatio = 0;
	private int joinThreshold = 1;
	private boolean joinMinRequired = true;

	NaiveIndex naiveIndex;
	JoinMinIndex joinMinIdx;

	private long maxSearchedEstNumRecords = 0;
	private long maxIndexedEstNumRecords = 0;

	public static BufferedWriter bw = null;

	public static BufferedWriter getWriter() {
		if( bw == null ) {
			try {
				bw = new BufferedWriter( new FileWriter( "Estimation_DEBUG.txt" ) );
			}
			catch( IOException e ) {
				e.printStackTrace();
			}
		}
		return bw;
	}

	public static void closeWriter() {
		if( bw != null ) {
			try {
				bw.close();
			}
			catch( IOException e ) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void preprocess() {
		super.preprocess();

		for( Record rec : query.indexedSet.get() ) {
			rec.preprocessSuffixApplicableRules();
			if( maxIndexedEstNumRecords < rec.getEstNumTransformed() ) {
				maxIndexedEstNumRecords = rec.getEstNumTransformed();
			}
		}
		if( !query.selfJoin ) {
			for( Record rec : query.searchedSet.get() ) {
				rec.preprocessSuffixApplicableRules();
				if( maxSearchedEstNumRecords < rec.getEstNumTransformed() ) {
					maxSearchedEstNumRecords = rec.getEstNumTransformed();
				}
			}
		}
		else {
			maxSearchedEstNumRecords = maxIndexedEstNumRecords;
		}
	}

	@Override
	public void run( Query query, String[] args ) throws IOException, ParseException {
		Param params = Param.parseArgs( args, stat, query );
		// Setup parameters
		checker = params.validator;
		qSize = params.qgramSize;
		indexK = params.indexK;
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

		actualJoinThreshold( 3 );

		actualJoinThreshold( 4 );

		actualJoinThreshold( 6 );

		actualJoinThreshold( 8 );

		actualJoinThreshold( 9 );

		actualJoinThreshold( 10 );

		actualJoinThreshold( 12 );

		actualJoinThreshold( 14 );

		actualJoinThreshold( 16 );

		actualJoinThreshold( 18 );

		actualJoinThreshold( 20 );

		actualJoinThreshold( 21 );

		actualJoinThreshold( 24 );

		actualJoinThreshold( 27 );

		actualJoinThreshold( 28 );

		actualJoinThreshold( 30 );

		actualJoinThreshold( 36 );

		actualJoinThreshold( 40 );

		actualJoinThreshold( 41 );

		actualJoinThreshold( 1000 );

		closeWriter();
	}

	private void buildJoinMinIndex( boolean writeResult ) {
		// Build an index
		joinMinIdx = new JoinMinIndex( indexK, qSize, stat, query, writeResult );
	}

	private void buildNaiveIndex( boolean writeResult, int joinThreshold ) {
		naiveIndex = NaiveIndex.buildIndex( joinThreshold / 2, stat, joinThreshold, writeResult, query );
	}

	private void findConstants( double sampleratio ) {
		// Sample
		estimate = new SampleEstimate( query, sampleratio, query.selfJoin );
		estimate.estimateWithSample( stat, checker, indexK, qSize );
	}

	private ArrayList<IntegerPair> join() {
		StopWatch buildTime = StopWatch.getWatchStarted( "Result_3_1_Index_Building_Time" );
		findConstants( sampleRatio );

		joinThreshold = estimate.findThetaUnrestricted( qSize, stat, maxIndexedEstNumRecords, maxSearchedEstNumRecords,
				query.oneSideJoin );

		if( Long.max( maxSearchedEstNumRecords, maxIndexedEstNumRecords ) <= joinThreshold ) {
			joinMinRequired = false;
		}

		Util.printLog( "Selected Threshold: " + joinThreshold );

		StopWatch stepTime = StopWatch.getWatchStarted( "Result_7_0_JoinMin_Index_Build_Time" );

		buildTime.start();
		if( joinMinRequired ) {
			buildJoinMinIndex( true );
		}
		int joinMinResultSize = 0;
		if( DEBUG.JoinMinNaiveON ) {
			if( joinMinRequired ) {
				stat.add( "Const_Gamma_Actual", String.format( "%.2f", joinMinIdx.gamma ) );
				stat.add( "Const_Gamma_SearchedSigCount_Actual", joinMinIdx.searchedTotalSigCount );
				stat.add( "Const_Gamma_CountTime_Actual", String.format( "%.2f", joinMinIdx.countTime ) );

				stat.add( "Const_Delta_Actual", String.format( "%.2f", joinMinIdx.delta ) );
				stat.add( "Const_Delta_IndexedSigCount_Actual", joinMinIdx.indexedTotalSigCount );
				stat.add( "Const_Delta_IndexTime_Actual", String.format( "%.2f", joinMinIdx.indexTime ) );
			}
			stepTime.stopAndAdd( stat );
			stepTime.resetAndStart( "Result_7_1_SearchEquiv_JoinMin_Time" );
		}
		buildTime.stopQuiet();
		StopWatch joinTime = StopWatch.getWatchStarted( "Result_3_2_Join_Time" );
		ArrayList<IntegerPair> rslt = new ArrayList<IntegerPair>();
		long joinstart = System.nanoTime();
		if( joinMinRequired ) {
			if( query.oneSideJoin ) {
				for( Record s : query.searchedSet.get() ) {
					// System.out.println( "test " + s + " " + s.getEstNumRecords() );
					if( s.getEstNumTransformed() > joinThreshold ) {
						joinMinIdx.joinRecordMaxKThres( indexK, s, rslt, true, null, checker, joinThreshold, query.oneSideJoin );
					}
				}
			}
			else {
				for( Record s : query.searchedSet.get() ) {
					joinMinIdx.joinRecordMaxKThres( indexK, s, rslt, true, null, checker, joinThreshold, query.oneSideJoin );
				}
			}

			joinMinResultSize = rslt.size();
			stat.add( "Join_Min_Result", joinMinResultSize );
			stat.add( "Stat_Equiv_Comparison", joinMinIdx.equivComparisons );
		}
		double joinminJointime = System.nanoTime() - joinstart;
		joinTime.stopQuiet();

		if( DEBUG.JoinMinNaiveON ) {
			Util.printLog( "After JoinMin Result: " + rslt.size() );
			stat.add( "Const_Epsilon_JoinTime_Actual", String.format( "%.2f", joinminJointime ) );
			if( joinMinRequired ) {
				stat.add( "Const_Epsilon_Predict_Actual", joinMinIdx.predictCount );
				stat.add( "Const_Epsilon_Actual", String.format( "%.2f", joinminJointime / joinMinIdx.predictCount ) );

				stat.add( "Const_EpsilonPrime_Actual", String.format( "%.2f", joinminJointime / joinMinIdx.comparisonCount ) );
				stat.add( "Const_EpsilonPrime_Comparison_Actual", joinMinIdx.comparisonCount );
			}
			stepTime.stopAndAdd( stat );
			stepTime.resetAndStart( "Result_7_2_Naive Index Building Time" );
		}

		buildTime.start();
		buildNaiveIndex( true, joinThreshold );
		buildTime.stopAndAdd( stat );

		if( DEBUG.JoinMinNaiveON ) {
			stat.add( "Const_Alpha_Actual", String.format( "%.2f", naiveIndex.alpha ) );
			stat.add( "Const_Alpha_IndexTime_Actual", String.format( "%.2f", naiveIndex.indexTime ) );
			stat.add( "Const_Alpha_ExpLength_Actual", String.format( "%.2f", naiveIndex.totalExpLength ) );

			stepTime.stopAndAdd( stat );
			stepTime.resetAndStart( "Result_7_3_SearchEquiv Naive Time" );
		}

		joinTime.start();
		@SuppressWarnings( "unused" )
		int naiveSearch = 0;
		long starttime = System.nanoTime();
		for( Record s : query.searchedSet.get() ) {
			if( s.getEstNumTransformed() > joinThreshold ) {
				continue;
			}
			else {
				naiveIndex.joinOneRecord( s, rslt );
				naiveSearch++;
			}
		}
		double joinNanoTime = System.nanoTime() - starttime;

		stat.add( "Join_Naive_Result", rslt.size() - joinMinResultSize );
		joinTime.stopAndAdd( stat );

		if( DEBUG.JoinMinNaiveON ) {
			stat.add( "Const_Beta_Actual", String.format( "%.2f", joinNanoTime / naiveIndex.totalExp ) );
			stat.add( "Const_Beta_JoinTime_Actual", String.format( "%.2f", joinTime ) );
			stat.add( "Const_Beta_TotalExp_Actual", String.format( "%.2f", naiveIndex.totalExp ) );

			stat.add( "Stat_Naive search count", naiveSearch );
			stepTime.stopAndAdd( stat );
		}
		buildTime.stopAndAdd( stat );
		return rslt;
	}

	private ArrayList<IntegerPair> actualJoinThreshold( int joinThreshold ) {
		System.out.println( "Threshold: " + joinThreshold );
		BufferedWriter bwEstimation = null;
		if( DEBUG.PrintEstimationON ) {
			bwEstimation = getWriter();
			try {
				bwEstimation.write( "Threshold " + joinThreshold + "\n" );
			}
			catch( IOException e ) {
				e.printStackTrace();
			}
		}

		long startTime = System.nanoTime();

		boolean joinMinRequired = true;
		if( Long.max( maxSearchedEstNumRecords, maxIndexedEstNumRecords ) <= joinThreshold ) {
			joinMinRequired = false;
		}

		if( joinMinRequired ) {
			buildJoinMinIndex( false );
		}
		int joinMinResultSize = 0;

		long joinMinBuildTime = System.nanoTime();
		System.out.println( "Threshold " + joinThreshold + " joinMin Index " + ( joinMinBuildTime - startTime ) );

		ArrayList<IntegerPair> rslt = new ArrayList<IntegerPair>();

		if( joinMinRequired ) {
			if( query.oneSideJoin ) {
				for( Record s : query.searchedSet.get() ) {
					// System.out.println( "test " + s + " " + s.getEstNumRecords() );
					if( s.getEstNumTransformed() > joinThreshold ) {
						joinMinIdx.joinRecordMaxKThres( indexK, s, rslt, false, null, checker, joinThreshold, query.oneSideJoin );
					}
				}
			}
			else {
				for( Record s : query.searchedSet.get() ) {
					joinMinIdx.joinRecordMaxKThres( indexK, s, rslt, false, null, checker, joinThreshold, query.oneSideJoin );
				}
			}

			joinMinResultSize = rslt.size();
			stat.add( "Join_Min_Result", joinMinResultSize );
			stat.add( "Stat_Equiv_Comparison", joinMinIdx.equivComparisons );
		}
		long joinMinJoinTime = System.nanoTime();

		if( joinMinRequired ) {
			if( DEBUG.PrintEstimationON ) {
				try {
					bwEstimation
							.write( "[Epsilon] " + ( joinMinJoinTime - joinMinBuildTime ) / (double) joinMinIdx.predictCount );
					bwEstimation.write( " JoinTime " + ( joinMinJoinTime - joinMinBuildTime ) );
					bwEstimation.write( " PredictedCount " + joinMinIdx.predictCount );
					bwEstimation.write( " ActualCount " + joinMinIdx.comparisonCount + "\n" );
				}
				catch( Exception e ) {
					e.printStackTrace();
				}
			}
			System.out.println( "[Epsilon] " + ( joinMinJoinTime - joinMinBuildTime ) / (double) joinMinIdx.predictCount );
			System.out.println( "[Epsilon] JoinTime " + ( joinMinJoinTime - joinMinBuildTime ) );
			System.out.println( "[Epsilon] PredictedCount " + joinMinIdx.predictCount );
			System.out.println( "[Epsilon] ActualCount " + joinMinIdx.comparisonCount );

			System.out.println( "Threshold " + joinThreshold + " joinMin Join " + ( joinMinJoinTime - joinMinBuildTime ) );
		}

		buildNaiveIndex( false, joinThreshold );
		long naiveBuildTime = System.nanoTime();
		System.out.println( "Threshold " + joinThreshold + " naive Index " + ( naiveBuildTime - joinMinJoinTime ) );

		int naiveSearch = 0;
		for( Record s : query.searchedSet.get() ) {
			if( s.getEstNumTransformed() > joinThreshold ) {
				continue;
			}
			else {
				naiveIndex.joinOneRecord( s, rslt );
				naiveSearch++;
			}
		}
		double naiveJoinTime = System.nanoTime();

		if( DEBUG.PrintEstimationON ) {
			try {
				bwEstimation.write( "[Beta] " + ( naiveJoinTime - naiveBuildTime ) / (double) naiveIndex.totalExp );
				bwEstimation.write( " JoinTime " + ( naiveJoinTime - naiveBuildTime ) );
				bwEstimation.write( " TotalExp " + naiveIndex.totalExp + "\n" );
			}
			catch( Exception e ) {
				e.printStackTrace();
			}
		}

		System.out.println( "[Beta] " + ( naiveJoinTime - naiveBuildTime ) / (double) naiveIndex.totalExp );
		System.out.println( "[Beta] JoinTime " + ( naiveJoinTime - naiveBuildTime ) );
		System.out.println( "[Beta] TotalExp " + naiveIndex.totalExp );

		System.out.println( "Naive Search " + naiveSearch );

		System.out.println( "Threshold " + joinThreshold + " naive Join " + ( naiveJoinTime - naiveBuildTime ) );
		System.out.println( "Total Time " + ( naiveJoinTime - startTime ) );

		System.out.println();

		if( DEBUG.PrintEstimationON ) {
			bwEstimation = getWriter();
			try {
				bwEstimation.write( "\n" );
			}
			catch( IOException e ) {
				e.printStackTrace();
			}
		}

		return rslt;
	}

	@Override
	public String getName() {
		return "EstimationTest";
	}

	@Override
	public String getVersion() {
		return "1.0";
	}
}
