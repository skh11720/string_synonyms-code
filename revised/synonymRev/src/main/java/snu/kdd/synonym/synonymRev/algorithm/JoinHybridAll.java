package snu.kdd.synonym.synonymRev.algorithm;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import org.apache.commons.cli.ParseException;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.estimation.SampleEstimate;
import snu.kdd.synonym.synonymRev.estimation.SampleEstimateByRegression;
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
public class JoinHybridAll extends AlgorithmTemplate {
	public JoinHybridAll( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}

	public Validator checker;
	SampleEstimate estimate;
	protected int qSize = 0;
	protected int indexK = 0;
	protected double sampleRatio = 0;
	protected int joinThreshold = 1;
	protected boolean joinQGramRequired = true;
	protected boolean joinMinSelected = false;

	protected NaiveIndex naiveIndex;

	protected JoinMinIndexInterface joinMinIdx = null;
	protected JoinMHIndexInterface joinMHIdx = null;

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

		rslt = join();
		stepTime.stopAndAdd( stat );
		stat.addMemory( "Mem_4_Joined" );

		stepTime.resetAndStart( "Result_4_Write_Time" );
		writeResult();
		stepTime.stopAndAdd( stat );
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
		naiveIndex = new NaiveIndex( query.indexedSet, query, stat, true, joinThreshold, joinThreshold / 2 );
	}

	/**
	 * Although this implementation is not efficient, we did like this to measure
	 * the execution time of each part more accurate.
	 *
	 * @return
	 */
	protected Set<IntegerPair> join() {

		StopWatch buildTime = StopWatch.getWatchStarted( "Result_3_1_Index_Building_Time" );
		findConstants( sampleRatio );

		joinThreshold = estimate.findThetaJoinHybridAll( qSize, indexK, stat, maxIndexedEstNumRecords, maxSearchedEstNumRecords,
				query.oneSideJoin );

		joinMinSelected = estimate.getJoinMinSelected();

		if( Long.max( maxSearchedEstNumRecords, maxIndexedEstNumRecords ) <= joinThreshold ) {
			joinQGramRequired = false;
		}

		Util.printLog( "Selected Threshold: " + joinThreshold );

		StopWatch stepTime = StopWatch.getWatchStarted( "Result_7_0_JoinMin_Index_Build_Time" );

		if( joinQGramRequired ) {
			if( joinMinSelected ) {
				buildJoinMinIndex();
			}
			else {
				// joinMH selected
				buildJoinMHIndex();
			}
		}
		int joinMinResultSize = 0;
		if( DEBUG.JoinMinNaiveON ) {
			if( joinQGramRequired ) {
				if( joinMinSelected ) {
					stat.add( "Const_Lambda_Actual", String.format( "%.2f", joinMinIdx.getLambda() ) );
					stat.add( "Const_Lambda_IndexedSigCount_Actual", joinMinIdx.getIndexedTotalSigCount() );
					stat.add( "Const_Lambda_IndexTime_Actual", String.format( "%.2f", joinMinIdx.getIndexTime() ) );

					stat.add( "Const_Mu_Actual", String.format( "%.2f", joinMinIdx.getMu()) );
					stat.add( "Const_Mu_SearchedSigCount_Actual", joinMinIdx.getSearchedTotalSigCount() );
					stat.add( "Const_Mu_CountTime_Actual", String.format( "%.2f", joinMinIdx.getCountTime() ) );
				}
			}
			stepTime.stopAndAdd( stat );
			stepTime.resetAndStart( "Result_7_1_SearchEquiv_JoinMin_Time" );
		}
		buildTime.stopQuiet();
		StopWatch joinTime = StopWatch.getWatchStarted( "Result_3_2_Join_Time" );
		Set<IntegerPair> rslt = new ObjectOpenHashSet<IntegerPair>();
		long joinstart = System.nanoTime();
		if( joinQGramRequired ) {
			if( query.oneSideJoin ) {
				for( Record s : query.searchedSet.get() ) {
					// System.out.println( "test " + s + " " + s.getEstNumRecords() );
//					if ( s.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) continue;
					if( s.getEstNumTransformed() > joinThreshold ) {
						if( joinMinSelected ) {
							joinMinIdx.joinRecordMaxKThres( indexK, s, rslt, true, null, checker, joinThreshold, query.oneSideJoin );
						}
						else {
							joinMHIdx.joinOneRecordThres( s, rslt, checker, joinThreshold, query.oneSideJoin );
						}
					}
				}
			}
			else {
				for( Record s : query.searchedSet.get() ) {
//					if ( s.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) continue;
					if( joinMinSelected ) {
						joinMinIdx.joinRecordMaxKThres( indexK, s, rslt, true, null, checker, joinThreshold, query.oneSideJoin );
					}
					else {
						joinMHIdx.joinOneRecordThres( s, rslt, checker, joinThreshold, query.oneSideJoin );
					}
				}
			}

			joinMinResultSize = rslt.size();
			stat.add( "Join_Min_Result", joinMinResultSize );
			if( joinMinSelected ) {
				stat.add( "Stat_Equiv_Comparison", joinMinIdx.getEquivComparisons() );
			}
			else {
				stat.add( "Stat_Equiv_Comparison", joinMHIdx.getEquivComparisons() );
			}
		}
		double joinminJointime = System.nanoTime() - joinstart;
		joinTime.stopQuiet();

		if( DEBUG.JoinMinNaiveON ) {
			Util.printLog( "After JoinMin Result: " + rslt.size() );
			stat.add( "Const_Rho_JoinTime_Actual", String.format( "%.2f", joinminJointime ) );
			if( joinQGramRequired ) {
				stat.add( "Const_Rho_Predict_Actual", joinMinIdx.getPredictCount() );
				stat.add( "Const_Rho_Actual", String.format( "%.2f", joinminJointime / joinMinIdx.getPredictCount() ) );

				stat.add( "Const_RhoPrime_Actual", String.format( "%.2f", joinminJointime / joinMinIdx.getComparisonCount() ) );
				stat.add( "Const_RhoPrime_Comparison_Actual", joinMinIdx.getComparisonCount() );
			}
			stepTime.stopAndAdd( stat );
			stepTime.resetAndStart( "Result_7_2_Naive Index Building Time" );
		}

		buildTime.start();
		buildNaiveIndex();
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

	protected void findConstants( double sampleratio ) {
		// Sample
		estimate = new SampleEstimateByRegression( query, sampleratio, query.selfJoin );
		estimate.estimateJoinHybridWithSample( stat, checker, indexK, qSize );
		
		stat.add( "Estimate_Coeff1_Naive", estimate.coeff_naive_1);
		stat.add( "Estimate_Coeff2_Naive", estimate.coeff_naive_2);
		stat.add( "Estimate_Term1_Naive", estimate.naive_term1);
		stat.add( "Estimate_Term2_Naive", estimate.naive_term2[estimate.sampleSearchedSize-1]);
		stat.add( "Estimate_Time_Naive", estimate.estTime_naive );

		stat.add( "Estimate_Coeff1_Mh", estimate.coeff_mh_1);
		stat.add( "Esitmate_Coeff2_Mh", estimate.coeff_mh_2);
		stat.add( "Estimate_Coeff3_Mh", estimate.coeff_mh_3);
		stat.add( "Estimate_Term1_Mh", estimate.mh_term1);
		stat.add( "Estimate_Term2_Mh", estimate.mh_term2[estimate.sampleSearchedSize-1]);
		stat.add( "Estimate_Term3_Mh", estimate.mh_term3[estimate.sampleSearchedSize-1]);
		stat.add( "Estimate_Time_Mh", estimate.estTime_mh );

		stat.add( "Estimate_Coeff1_Min", estimate.coeff_min_1);
		stat.add( "Estimate_Coeff2_Min", estimate.coeff_min_2);
		stat.add( "Estimate_Coeff3_Min", estimate.coeff_min_3);
		stat.add( "Estimate_Term1_Min", estimate.min_term1);
		stat.add( "Estimate_Term2_Min", estimate.min_term2[estimate.sampleSearchedSize-1]);
		stat.add( "Estimate_Term3_Min", estimate.min_term3[estimate.sampleSearchedSize-1]);
		stat.add( "Estimate_Time_Min", estimate.estTime_min );
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
		 */
		return "2.64";
	}

	@Override
	public String getName() {
		return "JoinHybridAll";
	}
}
