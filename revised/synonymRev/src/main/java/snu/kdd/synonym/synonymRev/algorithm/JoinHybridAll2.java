package snu.kdd.synonym.synonymRev.algorithm;

import java.io.IOException;
import java.util.Random;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.estimation.SampleEstimate;
import snu.kdd.synonym.synonymRev.index.JoinMHIndex;
import snu.kdd.synonym.synonymRev.index.JoinMinFastIndex;
import snu.kdd.synonym.synonymRev.index.JoinMinIndex;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.Param;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.tools.WYK_HashSet;
import snu.kdd.synonym.synonymRev.validator.Naive;
import snu.kdd.synonym.synonymRev.validator.NaiveOneSide;
import snu.kdd.synonym.synonymRev.validator.TopDown;
import snu.kdd.synonym.synonymRev.validator.TopDownOneSide;
import snu.kdd.synonym.synonymRev.validator.Validator;


public class JoinHybridAll2 extends JoinHybridAll {

	SampleEstimateSelf estimate;
	protected double sampleRatioH;
	protected double sampleRatioB;

	public JoinHybridAll2( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
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

		stat.add( "MaxIndexedEstNumRecords", maxIndexedEstNumRecords );
		stat.add( "MaxSearchedEstNumRecords", maxSearchedEstNumRecords );
	}

	@Override
	public void run( Query query, String[] args ) throws IOException, ParseException {
		ParamSelf params = ParamSelf.parseArgs( args, stat, query );
		// Setup parameters
		checker = params.validator;
		qSize = params.qgramSize;
		indexK = params.indexK;
		sampleRatioH = params.sampleRatioH;
		sampleRatioB = params.sampleRatioB;

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
		joinMinIdx = new JoinMinFastIndex( indexK, qSize, stat, query, sampleRatioB, joinThreshold, true );
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
			findConstants( statEst );
			list_thres[i] = estimate.findThetaJoinHybridAll( qSize, indexK, statEst, maxIndexedEstNumRecords, maxSearchedEstNumRecords, query.oneSideJoin );
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

		if ( maxSearchedEstNumRecords <= joinThreshold ) {
			joinQGramRequired = false; // in this case both joinmh and joinmin are not used.
		}

//		StopWatch stepTime = StopWatch.getWatchStarted( "Result_7_0_JoinMin_Index_Build_Time" );
		if( joinQGramRequired ) {
			if( joinMinSelected ) buildJoinMinIndex();
			else buildJoinMHIndex();
		}
		buildNaiveIndex();

		buildTime.stopAndAdd( stat );

		// join
		Set<IntegerPair> rsltNaive = new WYK_HashSet<IntegerPair>();
		Set<IntegerPair> rsltPQGram = new WYK_HashSet<IntegerPair>();
		int pqgramSearch = 0;
		int naiveSearch = 0;
		int skipped = 0;
		long joinNaiveTime = 0;
		long joinPQGramTime = 0;
		long joinStart = System.nanoTime();
		for( Record s : query.searchedSet.get() ) {
			long joinStartOne = System.nanoTime();
			// System.out.println( "test " + s + " " + s.getEstNumRecords() );
			if ( s.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) {
				++skipped;
				continue;
			}
			if( joinQGramRequired && s.getEstNumTransformed() > joinThreshold ) {
				if( joinMinSelected ) joinMinIdx.joinRecordMaxKThres( indexK, s, rsltPQGram, true, null, checker, joinThreshold, query.oneSideJoin );
				else joinMHIdx.joinOneRecordThres( s, rsltPQGram, checker, joinThreshold, query.oneSideJoin );
				++pqgramSearch;
				joinPQGramTime += System.nanoTime() - joinStartOne;
			}
			else {
				naiveIndex.joinOneRecord( s, rsltNaive );
				++naiveSearch;
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
		stat.add( "Stat_PQGram_Search", pqgramSearch );
		stat.add( "Stat_Skipped", skipped );
		if (joinQGramRequired ) {
			long candQGramCountSum = 0;
			double candQGramAvgCount = 0;
			long equivComparison = 0;
			if( joinMinSelected ) {
				candQGramCountSum = ((JoinMinIndex)joinMinIdx).getCandQGramCountSum();
				equivComparison = joinMinIdx.getEquivComparisons();
			}
			else {
				candQGramCountSum = ((JoinMHIndex)joinMHIdx).getCandQGramCountSum();
				equivComparison = joinMHIdx.getEquivComparisons();
			}

			candQGramAvgCount = pqgramSearch == 0 ? 0 : 1.0 * candQGramCountSum / pqgramSearch;
			stat.add( "Stat_CandQGram_Sum", candQGramCountSum );
			stat.add( "Stat_CandQGram_Avg", candQGramAvgCount );
			stat.add( "Stat_Equiv_Comparison", equivComparison );
		}
		
		// evaluate the accuracy of estimation ???
		
		// return the final result
		Set<IntegerPair> rslt = new WYK_HashSet<>();
		rslt.addAll( rsltNaive );
		rslt.addAll( rsltPQGram );
		return rslt;
	}

	protected void findConstants(StatContainer stat ) {
		// Sample
		estimate = new SampleEstimateSelf( query, sampleRatioH, sampleRatioB, query.selfJoin );
		estimate.estimateJoinHybridWithSample( stat, checker, indexK, qSize );
		
		this.stat.add( "Estimate_Coeff1_Naive", estimate.coeff_naive_1);
		this.stat.add( "Estimate_Coeff2_Naive", estimate.coeff_naive_2);
		this.stat.add( "Estimate_Term1_Naive", estimate.naive_term1);
		this.stat.add( "Estimate_Term2_Naive", estimate.naive_term2[estimate.sampleSearchedSize-1]);
		this.stat.add( "Estimate_Time_Naive", estimate.estTime_naive );

		this.stat.add( "Estimate_Coeff1_Mh", estimate.coeff_mh_1);
		this.stat.add( "Esitmate_Coeff2_Mh", estimate.coeff_mh_2);
		this.stat.add( "Estimate_Coeff3_Mh", estimate.coeff_mh_3);
		this.stat.add( "Estimate_Term1_Mh", estimate.mh_term1);
		this.stat.add( "Estimate_Term2_Mh", estimate.mh_term2[estimate.sampleSearchedSize-1]);
		this.stat.add( "Estimate_Term3_Mh", estimate.mh_term3[estimate.sampleSearchedSize-1]);
		this.stat.add( "Estimate_Time_Mh", estimate.estTime_mh );

		this.stat.add( "Estimate_Coeff1_Min", estimate.coeff_min_1);
		this.stat.add( "Estimate_Coeff2_Min", estimate.coeff_min_2);
		this.stat.add( "Estimate_Coeff3_Min", estimate.coeff_min_3);
		this.stat.add( "Estimate_Term1_Min", estimate.min_term1);
		this.stat.add( "Estimate_Term2_Min", estimate.min_term2[estimate.sampleSearchedSize-1]);
		this.stat.add( "Estimate_Term3_Min", estimate.min_term3[estimate.sampleSearchedSize-1]);
		this.stat.add( "Estimate_Time_Min", estimate.estTime_min );
	}

	@Override
	public String getVersion() {
		/*
		 * 1.00: the initial version
		 */
		return "1.00";
	}

	@Override
	public String getName() {
		return "JoinHybridAll2";
	}
}



class ParamSelf extends Param {

	static {
		argOptions.addOption( "sampleH", true, "Sampling Ratio H" );
		argOptions.addOption( "sampleB", true, "Sampling Ratio B" );
	}

	public static ParamSelf parseArgs( String[] args, StatContainer stat, Query query ) throws IOException, ParseException {
		CommandLineParser parser = new DefaultParser();
		ParamSelf param = new ParamSelf();
		CommandLine cmd = parser.parse( argOptions, args );

		stat.add( cmd );

		if( cmd.hasOption( "K" ) ) {
			param.indexK = Integer.parseInt( cmd.getOptionValue( "K" ) );
		}

		if( cmd.hasOption( "qSize" ) ) {
			param.qgramSize = Integer.parseInt( cmd.getOptionValue( "qSize" ) );
		}

		if( cmd.hasOption( "sampleH" ) ) {
			param.sampleRatioH = Double.parseDouble( cmd.getOptionValue( "sampleH" ) );
		}

		if( cmd.hasOption( "sampleB" ) ) {
			param.sampleRatioB = Double.parseDouble( cmd.getOptionValue( "sampleB" ) );
		}
		
		if( cmd.hasOption( "naiveVal" ) ) {
			if( query.oneSideJoin ) {
				param.validator = new NaiveOneSide();
			}
			else {
				param.validator = new Naive();
			}
		}
		else {
			if( query.oneSideJoin ) {
				param.validator = new TopDownOneSide();
			}
			else {
				param.validator = new TopDown();
			}
		}

		return param;
	}

	public double sampleRatioH = -1;
	public double sampleRatioB = -1;
}



class SampleEstimateSelf extends SampleEstimate {
	
	protected double sampleRatioH;
	protected double sampleRatioB;

	public SampleEstimateSelf( final Query query, double sampleRatioH, double sampleRatioB, boolean isSelfJoin ) {
		super( query, sampleRatioH );
		this.sampleRatioH = sampleRatioH;
		this.sampleRatioB = sampleRatioB;
		long seed = System.currentTimeMillis();
		Util.printLog( "Random seed: " + seed );
		Random rn = new Random( seed );
		
		sampleSearchedList = sampleRecords( query.searchedSet.recordList, rn );
		if( isSelfJoin ) {
			for( Record r : sampleSearchedList ) {
				sampleIndexedList.add( r );
			}
		}
		else sampleIndexedList = sampleRecords( query.indexedSet.recordList, rn );

		Util.printLog( sampleSearchedList.size() + " Searched records are sampled" );
		Util.printLog( sampleIndexedList.size() + " Indexed records are sampled" );
		
		initialize();
	}
	
	public SampleEstimateSelf( final Query query, double sampleRatioH, double sampleRatioB, ObjectArrayList<Record> sampledSearchedList, ObjectArrayList<Record> sampleIndexedList ) {
		super( query, sampleRatioH );
		this.sampleRatioH = sampleRatioH;
		this.sampleRatioB = sampleRatioB;
		this.sampleSearchedList = sampledSearchedList;
		this.sampleIndexedList = sampleIndexedList;
		initialize();
	}
	
	protected long sampleJoinMin( JoinMinFastIndex joinmininst, Validator checker, int indexK ) {
		Set<IntegerPair> rslt = new ObjectOpenHashSet<IntegerPair>();

		long ts = System.nanoTime();
		for (int i = 0; i < sampleQuery.searchedSet.size(); i++) {
			Record recS = sampleQuery.searchedSet.getRecord( i );
			if ( recS.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) {
				min_term2[i] = ( i== 0? 0 : min_term2[i-1]);
				min_term3[i] = ( i== 0? 0 : min_term3[i-1]);
			}
			else {
				joinmininst.joinRecordMaxK( indexK, recS, rslt, false, null, checker, query.oneSideJoin );
				min_term2[i] = joinmininst.searchedTotalSigCount;
				min_term3[i] = joinmininst.equivComparisons;
			}
		}
		long joinTime = System.nanoTime() - ts;
		return joinTime;
	}

	public Object2DoubleMap<String> estimateJoinMin( StatContainer stat, Validator checker, int indexK, int qSize ) {
		// Infer lambda, mu and rho
		StatContainer tmpStat = new StatContainer();

		JoinMinFastIndex joinmininst = new JoinMinFastIndex( indexK, qSize, tmpStat, sampleQuery, 1.00, -1, false );
		long joinTime = sampleJoinMin( joinmininst, checker, indexK );

		if ( joinmininst.predictCount == 0 ) joinmininst.predictCount = 1; // prevent from dividing by zero
		min_term1 = joinmininst.indexedTotalSigCount;
		coeff_min_1 = joinmininst.indexRecordTime / (joinmininst.indexedTotalSigCount+1);
		coeff_min_2 = ( joinmininst.indexCountTime + joinmininst.candQGramTime + joinmininst.filterTime ) / (joinmininst.searchedTotalSigCount+1);
		coeff_min_3 = joinmininst.verifyTime / (joinmininst.equivComparisons+1);

		System.out.println( "estimateJoinMin" );
		System.out.println( "coeff_min_1: "+coeff_min_1 );
		System.out.println( "coeff_min_2: "+coeff_min_2 );
		System.out.println( "coeff_min_3: "+coeff_min_3 );
		System.out.println( "Min_Term_1: "+ min_term1 );
		System.out.println( "Min_Term_2: "+ min_term2[sampleSearchedList.size()-1] );
		System.out.println( "Min_Term_3: "+ min_term3[sampleSearchedList.size()-1] );
//		System.out.println( Arrays.toString( min_term2 ) );
//		System.out.println( Arrays.toString( min_term3 ) );
		estTime_min = getEstimateJoinMin( min_term1, min_term2[sampleSearchedList.size()-1], min_term3[sampleSearchedList.size()-1]);
		System.out.println( "Est_Time: "+ estTime_min );
		System.out.println( "Join_Time: "+String.format( "%.10e", (double)joinTime ) );

//		System.out.println( "est verify time: "+String.format( "%.10e", coeff_min_3*min_term3[sampleSearchedList.size()-1] ) );
//		System.out.println( "verify time: "+String.format( "%.10e", (double)(joinTime-joinmininst.candQGramCountTime) ) );
//
//		System.out.println( "est tpq time: "+String.format( "%.10e", coeff_min_2*min_term2[sampleSearchedList.size()-1] ) );
//		System.out.println( "tpq time: "+String.format( "%.10e", (double)(joinmininst.indexCountTime + joinmininst.candQGramCountTime) ) );
//		System.out.println( "tpq, indexCounttime: "+String.format( "%.10e", (double)(joinmininst.candQGramCountTime) ) );
//		System.out.println( "tpq, candQGramCounttime: "+String.format( "%.10e", (double)(joinmininst.candQGramCountTime) ) );
//		
//		System.out.println( "est index time: "+String.format( "%.10e", coeff_min_1*min_term1 ) );
//		System.out.println( "index time: "+String.format( "%.10e", (double)(joinmininst.indexTime) ) );

		Object2DoubleOpenHashMap<String> output = new Object2DoubleOpenHashMap<>();
		output.put( "Min_Coeff_1", coeff_min_1 );
		output.put( "Min_Coeff_2", coeff_min_2 );
		output.put( "Min_Coeff_3", coeff_min_3 );
		output.put( "Min_Term_1", min_term1 );
		output.put( "Min_Term_2", min_term2[sampleSearchedList.size()-1] );
		output.put( "Min_Term_3", min_term3[sampleSearchedList.size()-1] );
		output.put( "Min_Est_Time", (double)estTime_min );
		output.put( "Min_Join_Time", (double)joinTime );
		return output;
	}

	public double getEstimateJoinMin( double term1, double term2, double term3 ) {
		return coeff_min_1 * term1 / sampleRatioH * sampleRatioB 
				+ coeff_min_2 * term2 / sampleRatioH
				+ coeff_min_3 * term3 / sampleRatioH / sampleRatioH;
	}
}
