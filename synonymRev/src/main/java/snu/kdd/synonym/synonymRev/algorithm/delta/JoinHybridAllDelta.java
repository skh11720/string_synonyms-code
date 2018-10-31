package snu.kdd.synonym.synonymRev.algorithm.delta;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import org.apache.commons.cli.ParseException;

import passjoin.PassJoinIndexForSynonyms;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.estimation.SampleEstimate;
import snu.kdd.synonym.synonymRev.index.JoinMHIndex;
import snu.kdd.synonym.synonymRev.index.JoinMHIndexInterface;
import snu.kdd.synonym.synonymRev.index.JoinMinIndex;
import snu.kdd.synonym.synonymRev.index.JoinMinIndexInterface;
import snu.kdd.synonym.synonymRev.index.NaiveIndex;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.Param;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.tools.WYK_HashSet;
import snu.kdd.synonym.synonymRev.validator.Validator;

/**
 * Given threshold, if a record has more than 'threshold' 1-expandable strings,
 * use an index to store them.
 * Otherwise, generate all 1-expandable strings and then use them to check
 * if two strings are equivalent.
 * Utilize only one index by sorting records according to their expanded size.
 * It first build JoinMin(JoinH2Gram) index and then change threshold / modify
 * index in order to find the best execution time.
 */
public class JoinHybridAllDelta extends AlgorithmTemplate {
	public JoinHybridAllDelta( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}

	public Validator checker;
	SampleEstimateDelta estimate;
	protected int qSize = 0;
	protected int indexK = 0;
	protected int deltaMax = -1;
	protected double sampleRatio = 0;
	protected int nEst = 1;
	protected int joinThreshold = 1;
	protected boolean joinQGramRequired = true;
	protected boolean joinMinSelected = false;

	protected PassJoinIndexForSynonyms naiveIndex;

	protected JoinMinDeltaIndex joinMinIdx = null;
	protected JoinMHDeltaIndex joinMHIdx = null;

	protected long maxSearchedEstNumRecords = 0;
	protected long maxIndexedEstNumRecords = 0;

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

		stat.add( "MaxIndexedEstNumRecords", maxIndexedEstNumRecords );
		stat.add( "MaxSearchedEstNumRecords", maxSearchedEstNumRecords );
	}

	@Override
	public void run( Query query, String[] args ) throws IOException, ParseException {
		Param params = Param.parseArgs( args, stat, query );
		// Setup parameters
		qSize = params.qgramSize;
		indexK = params.indexK;
		deltaMax = params.delta;
		checker = new DeltaValidatorTopDown( deltaMax );
		sampleRatio = params.sampleRatio;
		nEst = params.nEst;

		StopWatch stepTime = StopWatch.getWatchStarted( "Result_2_Preprocess_Total_Time" );
		preprocess();
		stepTime.stopAndAdd( stat );
		// Retrieve statistics

		stepTime.resetAndStart( "Result_3_Run_Time" );
		// Estimate constants

		rslt = join();
		stepTime.stopAndAdd( stat );
		stat.addMemory( "Mem_4_Joined" );

		stepTime.resetAndStart( "Result_4_Write_Time" );
		writeResult();
		stepTime.stopAndAdd( stat );
	}

	protected void buildJoinMinIndex() {
		// Build an index
		joinMinIdx = new JoinMinDeltaIndex( indexK, qSize, deltaMax, stat, query, joinThreshold, true );
	}

	protected void buildJoinMHIndex() {
		// Build an index
		int[] index = new int[ indexK ];
		for( int i = 0; i < indexK; i++ ) {
			index[ i ] = i;
		}
		joinMHIdx = new JoinMHDeltaIndex( indexK, qSize, deltaMax, query.indexedSet.get(), query, stat, index, true, true, joinThreshold );
	}

	protected void buildNaiveIndex() {
		naiveIndex = new PassJoinIndexForSynonyms( query, deltaMax, stat );
	}

	/**
	 * Although this implementation is not efficient, we did like this to measure
	 * the execution time of each part more accurate.
	 *
	 * @return
	 */
	protected Set<IntegerPair> join() {
		StopWatch estimateTime = StopWatch.getWatchStarted( "Result_2_1_Estimation_Time" );
		StatContainer statEst = new StatContainer();
		int[] list_thres = new int[nEst];
		double[] list_bestTime = new double[nEst];
		boolean[] list_minSelected= new boolean[nEst];
		for ( int i=0; i<nEst; ++i ) {
			findConstants( sampleRatio, statEst );
			list_thres[i] = estimate.findThetaJoinHybridAllDelta( qSize, indexK, statEst, maxIndexedEstNumRecords, maxSearchedEstNumRecords, query.oneSideJoin );
			list_minSelected[i] = estimate.getJoinMinSelected();
			list_bestTime[i] = estimate.bestEstTime;
		}
		double mean = 0, var = 0;
		for ( int i=0; i<nEst; ++i ) mean += list_thres[i];
		mean /= nEst;
		for ( int i=0; i<nEst; ++i ) var += (list_thres[i] - mean)*(list_thres[i] - mean);
		var /= nEst;
		double bestTime = Double.MAX_VALUE;
		for ( int i=0; i<nEst; ++i ) {
			if ( list_bestTime[i] < bestTime ) {
				bestTime = list_bestTime[i];
				joinThreshold = list_thres[i];
				joinMinSelected = list_minSelected[i];
			}
			System.out.println( list_thres[i]+"\t"+list_bestTime[i]+"\t"+list_minSelected[i] );
		}

		Util.printLog( "Selected Threshold: " + joinThreshold );
		Util.printLog( "JoinMinSelected: " + (joinMinSelected? "true":"false") );
		stat.add( "Estimate_Threshold", joinThreshold );
		stat.add( "Estimate_Repeat", nEst );
		stat.add( "Estimate_Best_Time", bestTime );
		stat.add( "Estimate_Mean_Threshold", mean );
		stat.add( "Estimate_Var_Threshold", var );
		stat.add( "Estimate_JoinMinSelected", joinMinSelected? "true":"false" );
		estimateTime.stopAndAdd( stat );
		
		StopWatch buildTime = StopWatch.getWatchStarted( "Result_3_1_Index_Building_Time" );
//		if( Long.max( maxSearchedEstNumRecords, maxIndexedEstNumRecords ) <= joinThreshold ) {
		if ( maxSearchedEstNumRecords <= joinThreshold ) {
			joinQGramRequired = false; // in this case both joinmh and joinmin are not used.
		}

//		StopWatch stepTime = StopWatch.getWatchStarted( "Result_7_0_JoinMin_Index_Build_Time" );
		if( joinQGramRequired ) {
			if( joinMinSelected ) buildJoinMinIndex();
			else buildJoinMHIndex();
		}
		buildNaiveIndex();

//		if( DEBUG.JoinMinNaiveON ) {
//			if( joinQGramRequired ) {
//				if( joinMinSelected ) {
//					stat.add( "Const_Lambda_Actual", String.format( "%.2f", joinMinIdx.getLambda() ) );
//					stat.add( "Const_Lambda_IndexedSigCount_Actual", joinMinIdx.getIndexedTotalSigCount() );
//					stat.add( "Const_Lambda_IndexTime_Actual", String.format( "%.2f", joinMinIdx.getIndexTime() ) );
//
//					stat.add( "Const_Mu_Actual", String.format( "%.2f", joinMinIdx.getMu()) );
//					stat.add( "Const_Mu_SearchedSigCount_Actual", joinMinIdx.getSearchedTotalSigCount() );
//					stat.add( "Const_Mu_CountTime_Actual", String.format( "%.2f", joinMinIdx.getCountTime() ) );
//				}
//			}
//			stepTime.stopAndAdd( stat );
//			stepTime.resetAndStart( "Result_7_1_SearchEquiv_JoinMin_Time" );
//		}
		buildTime.stopAndAdd( stat );

		// join
		Set<IntegerPair> rsltNaive = new WYK_HashSet<IntegerPair>();
		Set<IntegerPair> rsltPQGram = new WYK_HashSet<IntegerPair>();
		int naiveSearch = 0;
		long joinNaiveTime = 0;
		long joinPQGramTime = 0;
		long joinStart = System.nanoTime();
		for( Record s : query.searchedSet.get() ) {
			long joinStartOne = System.nanoTime();
			// System.out.println( "test " + s + " " + s.getEstNumRecords() );
			if ( s.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) continue;
			if( joinQGramRequired && s.getEstNumTransformed() > joinThreshold ) {
				if( joinMinSelected ) joinMinIdx.joinRecordMaxKThres( indexK, s, rsltPQGram, true, null, checker, joinThreshold, query.oneSideJoin );
				else joinMHIdx.joinOneRecordThres( s, rsltPQGram, checker, joinThreshold, query.oneSideJoin );
				joinPQGramTime += System.nanoTime() - joinStartOne;
			}
			else {
				naiveIndex.joinOneRecord( s, rsltNaive );
				naiveSearch++;
				joinNaiveTime += System.nanoTime() - joinStartOne;
			}
		}
		long joinTime = System.nanoTime() - joinStart;

		stat.add( "Join_Naive_Result", rsltNaive.size() );
		stat.add( "Join_Min_Result", rsltPQGram.size() );
		stat.add( "Result_3_2_Join_Time", joinTime/1e6 );
		stat.add( "Result_3_2_1_Join_Naive_Time", joinNaiveTime/1e6 );
		stat.add( "Result_3_2_2_Join_PQGram_Time", joinPQGramTime/1e6 );
		stat.add( "Stat_Naive_Search", naiveSearch );
		if (joinQGramRequired ) {
			if( joinMinSelected ) stat.add( "Stat_Equiv_Comparison", joinMinIdx.getEquivComparisons() );
			else stat.add( "Stat_Equiv_Comparison", joinMHIdx.getEquivComparisons() );
		}
		
		// evaluate the accuracy of estimation ???
		
		// return the final result
		Set<IntegerPair> rslt = new WYK_HashSet<>();
		rslt.addAll( rsltNaive );
		rslt.addAll( rsltPQGram );
		return rslt;
	}

	protected void findConstants( double sampleratio, StatContainer stat ) {
		// Sample
		estimate = new SampleEstimateDelta( query, deltaMax, sampleratio, query.selfJoin, false );
		estimate.estimateJoinHybridWithSample( stat, checker, indexK, qSize );
		
		stat.add( "Coeff_Naive_1", estimate.coeff_naive_1);
		stat.add( "Coeff_Naive_2", estimate.coeff_naive_2);
		stat.add( "Coeff_Naive_3", estimate.coeff_naive_3);
		stat.add( "Coeff_Mh_1", estimate.coeff_mh_1);
		stat.add( "Coeff_Mh_2", estimate.coeff_mh_2);
		stat.add( "Coeff_Mh_3", estimate.coeff_mh_3);
		stat.add( "Coeff_Min_1", estimate.coeff_min_1);
		stat.add( "Coeff_Min_2", estimate.coeff_min_2);
		stat.add( "Coeff_Min_3", estimate.coeff_min_3);
	}

	@Override
	public String getVersion() {
		/*
		 * 1.00: initial version
		 * 1.01: repeat estimation
		 * 1.02: stratified, start threshold search from 0 
		 * 1.03: modify naive estimation, FKP and BKP completely ignore non-upper records. can use naive only. scale up terms.
		 */
		return "1.03";
	}

	@Override
	public String getName() {
		return "JoinHybridAllDelta";
	}
}
