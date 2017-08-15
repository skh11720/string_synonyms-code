package snu.kdd.synonym.synonymRev.algorithm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.estimation.SampleEstimate;
import snu.kdd.synonym.synonymRev.index.JoinMHIndex;
import snu.kdd.synonym.synonymRev.index.NaiveIndex;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.Param;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class JoinMHNaive extends AlgorithmTemplate {

	public JoinMHNaive( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}

	public Validator checker;
	SampleEstimate estimate;
	private int qSize = 0;
	private int indexK = 0;
	private double sampleRatio = 0;
	private int joinThreshold = 1;
	private boolean joinMHRequired = true;

	NaiveIndex naiveIndex;
	JoinMHIndex joinMHIdx;

	private long maxSearchedEstNumRecords = 0;
	private long maxIndexedEstNumRecords = 0;

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
	}

	private void buildJoinMHIndex() {
		// Build an index
		int[] index = new int[ indexK ];
		for( int i = 0; i < indexK; i++ ) {
			index[ i ] = i;
		}
		joinMHIdx = new JoinMHIndex( indexK, qSize, query.indexedSet.get(), query, stat, index, true, true, joinThreshold );
	}

	private void buildNaiveIndex() {
		naiveIndex = NaiveIndex.buildIndex( joinThreshold / 2, stat, joinThreshold, true, query );
	}

	private ArrayList<IntegerPair> join() {

		StopWatch buildTime = StopWatch.getWatchStarted( "Result_3_1_Index_Building_Time" );
		findConstants( sampleRatio );

		joinThreshold = estimate.findThetaJoinMHNaive( qSize, indexK, stat, maxIndexedEstNumRecords, maxSearchedEstNumRecords,
				query.oneSideJoin );

		if( Long.max( maxSearchedEstNumRecords, maxIndexedEstNumRecords ) <= joinThreshold ) {
			joinMHRequired = false;
		}

		Util.printLog( "Selected Threshold: " + joinThreshold );

		if( joinMHRequired ) {
			buildJoinMHIndex();
		}
		int joinMHResultSize = 0;

		buildTime.stopQuiet();
		StopWatch joinTime = StopWatch.getWatchStarted( "Result_3_2_Join_Time" );
		ArrayList<IntegerPair> rslt = new ArrayList<IntegerPair>();

		if( joinMHRequired ) {
			if( query.oneSideJoin ) {
				for( Record s : query.searchedSet.get() ) {
					// System.out.println( "test " + s + " " + s.getEstNumRecords() );
					if( s.getEstNumTransformed() > joinThreshold ) {
						joinMHIdx.joinOneRecordThres( indexK, s, rslt, checker, joinThreshold, query.oneSideJoin, indexK - 1 );
					}
				}
			}
			else {
				for( Record s : query.searchedSet.get() ) {
					joinMHIdx.joinOneRecordThres( indexK, s, rslt, checker, joinThreshold, query.oneSideJoin, indexK - 1 );
				}
			}

			joinMHResultSize = rslt.size();
			stat.add( "Join_MH_Result", joinMHResultSize );
			stat.add( "Stat_Equiv_Comparison", joinMHIdx.equivComparisons );
		}
		joinTime.stopQuiet();

		buildTime.start();
		buildNaiveIndex();
		buildTime.stopAndAdd( stat );

		if( DEBUG.JoinMHNaiveON ) {
			stat.add( "Const_Alpha_Actual", String.format( "%.2f", naiveIndex.alpha ) );
			stat.add( "Const_Alpha_IndexTime_Actual", String.format( "%.2f", naiveIndex.indexTime ) );
			stat.add( "Const_Alpha_ExpLength_Actual", String.format( "%.2f", naiveIndex.totalExpLength ) );
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

		stat.add( "Join_Naive_Result", rslt.size() - joinMHResultSize );
		joinTime.stopAndAdd( stat );

		if( DEBUG.JoinMHNaiveON ) {
			stat.add( "Const_Beta_Actual", String.format( "%.2f", joinNanoTime / naiveIndex.totalExp ) );
			stat.add( "Const_Beta_JoinTime_Actual", String.format( "%.2f", joinTime ) );
			stat.add( "Const_Beta_TotalExp_Actual", String.format( "%.2f", naiveIndex.totalExp ) );

			stat.add( "Stat_Naive search count", naiveSearch );
		}
		buildTime.stopAndAdd( stat );
		return rslt;
	}

	private void findConstants( double sampleratio ) {
		// Sample
		estimate = new SampleEstimate( query, sampleratio, query.selfJoin );
		estimate.estimateJoinMHNaiveWithSample( stat, checker, indexK, qSize );
	}

	@Override
	public String getName() {
		return "JoinMHNaive";
	}

	@Override
	public String getVersion() {
		return "2.0";
	}
}
