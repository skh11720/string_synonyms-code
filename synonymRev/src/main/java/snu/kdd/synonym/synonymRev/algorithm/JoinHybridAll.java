package snu.kdd.synonym.synonymRev.algorithm;

import java.util.Set;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.estimation.SampleEstimate;
import snu.kdd.synonym.synonymRev.index.JoinMHIndex;
import snu.kdd.synonym.synonymRev.index.JoinMinIndex;
import snu.kdd.synonym.synonymRev.index.NaiveIndex;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.tools.WYK_HashSet;
import snu.kdd.synonym.synonymRev.validator.TopDownOneSide;

/**
 * Given threshold, if a record has more than 'threshold' 1-expandable strings,
 * use an index to store them.
 * Otherwise, generate all 1-expandable strings and then use them to check
 * if two strings are equivalent.
 * Utilize only one index by sorting records according to their expanded size.
 * It first build JoinMin(JoinH2Gram) index and then change threshold / modify
 * index in order to find the best execution time.
 */
public class JoinHybridAll extends AbstractPosQGramBasedAlgorithm {

	SampleEstimate estimate;
	public final int indexK;
	public final double sampleH;
	protected int nEst;
	protected int joinThreshold = 1;
	protected boolean joinQGramRequired = true;
	protected boolean joinMinSelected = false;

	protected NaiveIndex naiveIndex;

	protected JoinMinIndex joinMinIdx = null;
	protected JoinMHIndex joinMHIdx = null;

	protected long maxSearchedEstNumRecords = 0;
	protected long maxIndexedEstNumRecords = 0;


	public JoinHybridAll(String[] args) {
		super(args);
		indexK = param.getIntParam("indexK");
		sampleH = param.getDoubleParam("sampleH");
	}
	
	@Override
	public void initialize() {
		super.initialize();
		checker = new TopDownOneSide();
	}

	@Override
	protected void reportParamsToStat() {
		stat.add("Param_indexK", indexK);
		stat.add("Param_qSize", qSize);
		stat.add("Param_sampleH", sampleH);
	}

	@Override
	public void preprocess() {
		super.preprocess();

		for( Record rec : query.searchedSet.get() ) {
			if( maxSearchedEstNumRecords < rec.getEstNumTransformed() ) {
				maxSearchedEstNumRecords = rec.getEstNumTransformed();
			}
		}
		stat.add( "MaxSearchedEstNumRecords", maxSearchedEstNumRecords );
	}
	
	@Override
	protected void executeJoin() {
		StopWatch estimateTime = StopWatch.getWatchStarted( "Result_2_1_Estimation_Time" );
		StatContainer statEst = new StatContainer();
		int[] list_thres = new int[nEst];
		double[] list_bestTime = new double[nEst];
		boolean[] list_minSelected= new boolean[nEst];
		for ( int i=0; i<nEst; ++i ) {
			findConstants( sampleH, statEst );
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
		
		buildIndex();
		
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
				if( joinMinSelected ) joinMinIdx.joinOneRecord( s, rsltPQGram, checker );
				else joinMHIdx.joinOneRecord( s, rsltPQGram, checker );
				++pqgramSearch;
				joinPQGramTime += System.nanoTime() - joinStartOne;
			}
			else {
				naiveIndex.joinOneRecord( s, rsltNaive, null );
				++naiveSearch;
				joinNaiveTime += System.nanoTime() - joinStartOne;
			}
		}
		long joinTime = System.nanoTime() - joinStart;

		stat.add( "Join_Naive_Result", rsltNaive.size() );
		stat.add( "Join_Min_Result", rsltPQGram.size() );
		stat.add( JOIN_AFTER_INDEX_TIME, joinTime/1e6 );
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
		rslt = new ObjectOpenHashSet<IntegerPair>();  
		rslt.addAll( rsltNaive );
		rslt.addAll( rsltPQGram );
	}

	@Override
	protected void runAfterPreprocess() {
		System.err.println("THIS METHOD IS NOT SUPPOSED TO BE CALLED!");
	}

	@Override
	protected void runAfterPreprocessWithoutIndex() {
		System.err.println("THIS METHOD IS NOT SUPPOSED TO BE CALLED!");
	}

	@Override
	protected void buildIndex() {
		StopWatch buildTime = StopWatch.getWatchStarted( INDEX_BUILD_TIME );
		if ( maxSearchedEstNumRecords <= joinThreshold ) {
			joinQGramRequired = false; // in this case both joinmh and joinmin are not used.
		}

		if( joinQGramRequired ) {
			if( joinMinSelected ) buildJoinMinIndex();
			else buildJoinMHIndex();
		}
		buildNaiveIndex();
		buildTime.stopAndAdd( stat );
	}

	protected void buildJoinMinIndex() {
		// Build an index
		joinMinIdx = new JoinMinIndex( indexK, qSize, stat, query, joinThreshold, true );
	}

	protected void buildJoinMHIndex() {
		// Build an index
		int[] index = new int[ indexK ];
		for( int i = 0; i < indexK; i++ ) {
			index[ i ] = i;
		}
		joinMHIdx = new JoinMHIndex( indexK, qSize, query.indexedSet.get(), query, stat, index, true, true, joinThreshold );
	}

	protected void buildNaiveIndex() {
		naiveIndex = new NaiveIndex( query, stat, true );
	}

	protected void findConstants( double sampleratio, StatContainer stat ) {
		// Sample
		estimate = new SampleEstimate( query, sampleratio, query.selfJoin );
//		estimate = new SampleEstimateByRegression( query, sampleratio, query.selfJoin );
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
		 * 2.6: the latest version by yjpark
		 * 2.61: ignore records with too many transformations
		 * ---- rollback 2.61
		 * 2.62: estimate by scaling up terms: 11/122/122
		 * 2.63: estimate by scaling up terms: 11/112/112
		 * 2.64: estimate using regression
		 * 2.65: nEst, conduct regression at most twice
		 */
		return "2.63";
	}

	@Override
	public String getName() {
		return "JoinHybridAll";
	}
}
