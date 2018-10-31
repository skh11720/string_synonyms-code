package snu.kdd.synonym.synonymRev.algorithm;

import java.io.IOException;
import java.util.Random;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.estimation.SampleEstimate;
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


public class JoinHybridAll3 extends JoinHybridAll {

	SampleEstimateSelf3 estimate;
	protected double sampleRatioH;
	protected double sampleRatioB;
	protected int sampleMax;

	public JoinHybridAll3( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}

	@Override
	public void run( Query query, String[] args ) throws IOException, ParseException {
		ParamSelf3 params = ParamSelf3.parseArgs( args, stat, query );
		// Setup parameters
		checker = params.validator;
		qSize = params.qgramSize;
		indexK = params.indexK;
		sampleRatioH = params.sampleRatioH;
		sampleRatioB = params.sampleRatioB;
		sampleMax = params.sampleMax;

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
		stat.add( "Join_PQGram_Result", rsltPQGram.size() );
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
			candQGramCountSum = ((JoinMinIndex)joinMinIdx).getCandQGramCountSum();
			equivComparison = joinMinIdx.getEquivComparisons();

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
		estimate = new SampleEstimateSelf3( query, sampleRatioH, sampleRatioB, query.selfJoin );
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
		this.stat.add( "Estimate_Coeff4_Min", estimate.coeff_min_4);
		this.stat.add( "Estimate_Term1_Min", estimate.min_term1);
		this.stat.add( "Estimate_Term2_Min", estimate.min_term2[estimate.sampleSearchedSize-1]);
		this.stat.add( "Estimate_Term3_Min", estimate.min_term3[estimate.sampleSearchedSize-1]);
		this.stat.add( "Estimate_Term4_Min", estimate.min_term4[estimate.sampleSearchedSize-1]);
		this.stat.add( "Estimate_Time_Min", estimate.estTime_min );
	}

	@Override
	public String getVersion() {
		return "1.01";
	}

	@Override
	public String getName() {
		return "JoinHybridAll3";
	}
}



class ParamSelf3 extends Param {

	static {
		argOptions.addOption( "sampleH", true, "Sampling Ratio H" );
		argOptions.addOption( "sampleB", true, "Sampling Ratio B" );
		argOptions.addOption( "sampleMax", true, "Maximum number of samples" );
	}

	public static ParamSelf3 parseArgs( String[] args, StatContainer stat, Query query ) throws IOException, ParseException {
		CommandLineParser parser = new DefaultParser();
		ParamSelf3 param = new ParamSelf3();
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

		if( cmd.hasOption( "sampleMax" ) ) {
			param.sampleRatioB = Integer.parseInt( cmd.getOptionValue( "sampleMax" ) );
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
	public int sampleMax = Integer.MAX_VALUE;
}



class SampleEstimateSelf3 extends SampleEstimate {
	
	protected double sampleRatioH;
	protected double sampleRatioB;
	public double coeff_min_4;
	public long[] min_term4;

	public SampleEstimateSelf3( final Query query, double sampleRatioH, double sampleRatioB, boolean isSelfJoin ) {
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
	
	public SampleEstimateSelf3( final Query query, double sampleRatioH, double sampleRatioB, ObjectArrayList<Record> sampledSearchedList, ObjectArrayList<Record> sampleIndexedList ) {
		super( query, sampleRatioH );
		this.sampleRatioH = sampleRatioH;
		this.sampleRatioB = sampleRatioB;
		this.sampleSearchedList = sampledSearchedList;
		this.sampleIndexedList = sampleIndexedList;
		initialize();
	}

	@Override
	public void initialize() {
		super.initialize();
		min_term4 = new long[sampleSearchedList.size()];
	}

	public void estimateJoinHybridWithSample( StatContainer stat, Validator checker, int indexK, int qSize ) {
		estimateJoinNaive( stat );
		estimateJoinMH( stat, checker, indexK, qSize );
		estimateJoinMin( stat, checker, indexK, qSize );
		
//		try {
//			bw_log.write( "estTimeNaive\t"+estTime_naive/1e6+"\n" );
//			bw_log.write( "estTimeJoinMH\t"+estTime_mh/1e6+"\n" );
//			bw_log.write( "estTimeJoinMin\t"+estTime_min/1e6+"\n" );
//		}
//		catch (IOException e ) { e.printStackTrace(); }

		}
	
	protected long sampleJoinMin( JoinMinFastIndex joinmininst, Validator checker, int indexK ) {
		Set<IntegerPair> rslt = new ObjectOpenHashSet<IntegerPair>();

		long ts = System.nanoTime();
		for (int i = 0; i < sampleQuery.searchedSet.size(); i++) {
			Record recS = sampleQuery.searchedSet.getRecord( i );
			if ( recS.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) {
				min_term2[i] = ( i== 0? 0 : min_term2[i-1]);
				min_term3[i] = ( i== 0? 0 : min_term3[i-1]);
				min_term4[i] = ( i== 0? 0 : min_term4[i-1]);
			}
			else {
				joinmininst.joinRecordMaxK( indexK, recS, rslt, false, null, checker, query.oneSideJoin );
				min_term2[i] = joinmininst.searchedTotalSigCount;
				min_term3[i] = joinmininst.searchedTotalSigCount;
				min_term4[i] = joinmininst.equivComparisons;
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
		coeff_min_2 = joinmininst.indexCountTime / (joinmininst.searchedTotalSigCount+1);
		coeff_min_3 = ( joinmininst.candQGramTime + joinmininst.filterTime ) / (joinmininst.searchedTotalSigCount+1);
		coeff_min_4 = joinmininst.verifyTime / (joinmininst.equivComparisons+1);

		System.out.println( "estimateJoinMin" );
		System.out.println( "coeff_min_1: "+coeff_min_1 );
		System.out.println( "coeff_min_2: "+coeff_min_2 );
		System.out.println( "coeff_min_3: "+coeff_min_3 );
		System.out.println( "coeff_min_4: "+coeff_min_4 );
		System.out.println( "Min_Term_1: "+ min_term1 );
		System.out.println( "Min_Term_2: "+ min_term2[sampleSearchedList.size()-1] );
		System.out.println( "Min_Term_3: "+ min_term3[sampleSearchedList.size()-1] );
		System.out.println( "Min_Term_4: "+ min_term4[sampleSearchedList.size()-1] );
//		System.out.println( Arrays.toString( min_term2 ) );
//		System.out.println( Arrays.toString( min_term3 ) );
		estTime_min = getEstimateJoinMin( min_term1, min_term2[sampleSearchedList.size()-1], min_term3[sampleSearchedList.size()-1], min_term4[sampleSearchedList.size()-1]);
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
		output.put( "Min_Coeff_4", coeff_min_4 );
		output.put( "Min_Term_1", min_term1 );
		output.put( "Min_Term_2", min_term2[sampleSearchedList.size()-1] );
		output.put( "Min_Term_3", min_term3[sampleSearchedList.size()-1] );
		output.put( "Min_Term_4", min_term4[sampleSearchedList.size()-1] );
		output.put( "Min_Est_Time", (double)estTime_min );
		output.put( "Min_Join_Time", (double)joinTime );
		return output;
	}

	public double getEstimateJoinMin( double term1, double term2, double term3, double term4 ) {
		return coeff_min_1 * term1 / sampleRatioH * sampleRatioB 
				+ coeff_min_2 * term2 / sampleRatioH * sampleRatioB
				+ coeff_min_3 * term3 / sampleRatioH
				+ coeff_min_4 * term4 / sampleRatioH / sampleRatioH;
	}

	public int findThetaJoinHybridAll( int qSize, int indexK, StatContainer stat, long maxIndexedEstNumRecords, long maxSearchedEstNumRecords, boolean oneSideJoin ) {
		// Find the best threshold
		int bestThreshold = 0;
//		double bestEstTime = Double.MAX_VALUE;

		// Indicates the minimum indices which have more that 'theta' expanded
		// records
//		int indexedIdx = 0;
		int sidx = 0;
		sampleSearchedNumEstTrans = 0;
//		long currentThreshold = Math.min( sampleSearchedList.get( 0 ).getEstNumTransformed(), sampleIndexedList.get( 0 ).getEstNumTransformed() );
		long currentThreshold = 0;
		long maxThreshold = Long.min( maxIndexedEstNumRecords, maxSearchedEstNumRecords );
//		int tableIndexedSize = sampleIndexedList.size();
		int tableSearchedSize = sampleSearchedList.size();

		boolean stop = false;
		if( maxThreshold == Long.MAX_VALUE ) {
			stop = true;
		}

		while( sidx < tableSearchedSize ) {
			if( currentThreshold > 100000 ) {
//				Util.printLog( "Current Threshold is more than 100000" );
				currentThreshold = Integer.MAX_VALUE;
				stop = true;
			}

			long nextThresholdIndexed = -1;
			long nextThresholdSearched = -1;

			for( ; sidx < tableSearchedSize; sidx++ ) {
				Record rec = sampleSearchedList.get( sidx );

				long est = rec.getEstNumTransformed();
				sampleSearchedNumEstTrans += est;
				if( est > currentThreshold ) {
					nextThresholdSearched = est;
					break;
				}
			}

			long nextThreshold;

			if( nextThresholdIndexed == -1 && nextThresholdSearched == -1 ) {
//				if( stop ) {
//					break;
//				}
				nextThreshold = Integer.MAX_VALUE;
			}
			else if( nextThresholdIndexed == -1 ) {
				nextThreshold = nextThresholdSearched;
			}
			else if( nextThresholdSearched == -1 ) {
				nextThreshold = nextThresholdIndexed;
			}
			else {
				nextThreshold = Long.min( nextThresholdSearched, nextThresholdIndexed );
			}

			long curr_naive_term2 = (sidx==0?0:naive_term2[sidx-1]);
			long curr_mh_term2 = (mh_term2[tableSearchedSize-1] - (sidx==0?0:mh_term2[sidx-1]));
			long curr_mh_term3 = (mh_term3[tableSearchedSize-1] - (sidx==0?0:mh_term3[sidx-1]));
			long curr_min_term2 = (min_term2[tableSearchedSize-1] - (sidx==0?0:min_term2[sidx-1]));
			long curr_min_term3 = (min_term3[tableSearchedSize-1] - (sidx==0?0:min_term3[sidx-1]));
			long curr_min_term4 = (min_term4[tableSearchedSize-1] - (sidx==0?0:min_term4[sidx-1]));
			
			double naiveEstimation = this.getEstimateNaive( naive_term1, curr_naive_term2);
			// currExpLengthSize: sum of lengths of records in sampleIndexedSet
			// currExpSize: sum of estimated number of transformations of records in sampleSearchedSet

			double joinmhEstimation = this.getEstimateJoinMH( mh_term1, curr_mh_term2, curr_mh_term3);
			// searchedTotalSigCount: the sum of the size of TPQ superset of records in sampleSearchedSet
			// indexedTotalSigCount: the number of pos qgrams from records in sampleIndexedSet
			// totalJoinMHInvokes: the sum of the minimum number of records to be verified with t for every t in sampleIndexedSet
			// removedJoinMHComparison: 

			double joinminEstimation = this.getEstimateJoinMin( min_term1, curr_min_term2, curr_min_term3, curr_min_term4 );
			// searchedTotalSigCount: the sum of the size of TPQ superset of records in sampleSearchedSet
			// indexedTotalSigCount: the number of pos qgrams from records in sampleIndexedSet

			boolean tempJoinMinSelected = joinminEstimation < joinmhEstimation;
//			boolean tempJoinMinSelected = true;

			System.out.print( (sidx)+"\t" );
			System.out.print( sampleSearchedNumEstTrans+"\t" );
			System.out.print(naive_term1+"\t");
			System.out.print(curr_naive_term2+"\t");
			System.out.print( mh_term1+"\t");
			System.out.print( curr_mh_term2+"\t" );
			System.out.print( curr_mh_term3+"\t" );
			System.out.print(min_term1+"\t");
			System.out.print(curr_min_term2+"\t");
			System.out.print(curr_min_term3+"\t");
			System.out.print(curr_min_term4+"\t");
			System.out.print("|\t");
			System.out.print(currentThreshold+"\t");
			System.out.print(naiveEstimation+"\t");
			System.out.print(joinmhEstimation+"\t");
			System.out.print((naiveEstimation+joinmhEstimation)+"\t");
			System.out.print(joinminEstimation+"\t");
			System.out.print((naiveEstimation+joinminEstimation)+"\t");
			System.out.println(  );
			
			try {
				bw_log.write( String.format( "%d\t", currentThreshold ) );

				// naive
				bw_log.write( String.format( "%d\t%d\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t|\t", 
						naive_term1, curr_naive_term2, coeff_naive_1*naive_term1/1e6, coeff_naive_2*curr_naive_term2/1e6, 
						getEstimateNaive( naive_term1, curr_naive_term2 )/1e6,
						coeff_naive_1*naive_term1/sampleRatio/1e6, coeff_naive_2*curr_naive_term2/sampleRatio/1e6,
						getEstimateNaive( naive_term1/sampleRatio, curr_naive_term2/sampleRatio )/1e6 
						) );

				// FKP
				bw_log.write( String.format( "%d\t%d\t%d\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t|\t", 
						mh_term1, curr_mh_term2, curr_mh_term3,
						coeff_mh_1*mh_term1/1e6, coeff_mh_2*curr_mh_term2/1e6, coeff_mh_3*curr_mh_term3/1e6,
						getEstimateJoinMH( mh_term1, curr_mh_term2, curr_mh_term3)/1e6,
						coeff_mh_1*mh_term1/sampleRatio/1e6, coeff_mh_2*curr_mh_term2/sampleRatio/1e6, 
						coeff_mh_3*curr_mh_term3/sampleRatio/sampleRatio/1e6, 
						getEstimateJoinMH( mh_term1/sampleRatio, curr_mh_term2/sampleRatio, curr_mh_term3/sampleRatio/sampleRatio)/1e6
						) );

				// BKP
				bw_log.write( 
						String.format( "%d\t%d\t%d\t%d\t", min_term1, curr_min_term2, curr_min_term3, curr_min_term4 ) + 
						String.format( "%.3f\t%.3f\t%.3f\t%.3f\t", coeff_min_1*min_term1/1e6, coeff_min_2*curr_min_term2/1e6, coeff_min_3*curr_min_term3/1e6, coeff_min_4*curr_min_term4/1e6 ) +
						String.format( "%.3f\t", getEstimateJoinMin( min_term1, curr_min_term2, curr_min_term3, curr_min_term4 )/1e6 ) +
						String.format( "%.3f\t%.3f\t%.3f\t%.3f\t", coeff_min_1*min_term1/sampleRatioH*sampleRatioB/1e6, coeff_min_2*curr_min_term2/sampleRatioH*sampleRatioB/1e6, 
						coeff_min_4*curr_min_term4/sampleRatioH/1e6, coeff_min_4*curr_min_term4/sampleRatioH/sampleRatioH/1e6 ) +
						String.format( "%.3f\t", getEstimateJoinMin( min_term1/sampleRatio, curr_min_term2/sampleRatio, curr_min_term4/sampleRatio/sampleRatio)/1e6 )
						);

				bw_log.write( "\n" );
				bw_log.flush();
			}
			catch ( IOException e ) { e.printStackTrace(); }

//			Util.printLog( String.format( "T: %d nT: %d NT: %.10e JT(JoinMH): %.10e TT: %.10e JT(JoinMin): %.10e TT: %.10e", currentThreshold, nextThreshold,
//					naiveEstimation, joinmhEstimation, naiveEstimation + joinmhEstimation, joinminEstimation, naiveEstimation + joinminEstimation ) );
//			Util.printLog( String.format( "T: %d nT: %d NT: %.10e JT(JoinMin): %.10e TT: %.10e", currentThreshold, nextThreshold,
//					naiveEstimation, joinminEstimation, naiveEstimation + joinminEstimation ) );
//			Util.printLog( "JoinMin Selected " + tempJoinMinSelected );

			double tempBestTime = naiveEstimation + joinminEstimation;

			if( tempJoinMinSelected ) tempBestTime += joinminEstimation;
			else tempBestTime += joinmhEstimation;

			if( bestEstTime > tempBestTime ) {
				bestEstTime = tempBestTime;
				joinMinSelected = tempJoinMinSelected;

				if( currentThreshold < Integer.MAX_VALUE ) {
					bestThreshold = (int) currentThreshold;
				}
				else {
					currentThreshold = Integer.MAX_VALUE;
				}

				if( DEBUG.SampleStatON ) {
					Util.printLog( "New Best " + bestThreshold + " with " + joinMinSelected );
				}
			}

			currentThreshold = nextThreshold;
			
			if (stop) break;
		} // end while searching best threshold
		
//		if ( getEstimateNaive( naive_term1/sampleRatio, naive_term2[sampleSearchedList.size()-1]/sampleRatio, naive_term3[sampleSearchedList.size()-1]/sampleRatio/sampleRatio ) < bestEstTime ) {
//			bestThreshold = Integer.MAX_VALUE;
//			bestEstTime = estTime_naive;
//		}

		stat.add( "Auto_Best_Threshold", bestThreshold );
		stat.add( "Auto_Best_Estimated_Time", bestEstTime );
		stat.add( "Auto_JoinMin_Selected", "" + joinMinSelected );
		return bestThreshold;
	}
}
