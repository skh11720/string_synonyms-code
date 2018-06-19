package snu.kdd.synonym.synonymRev.algorithm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.apache.commons.cli.ParseException;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.estimation.SampleEstimate_Split;
import snu.kdd.synonym.synonymRev.index.JoinMHIndex;
import snu.kdd.synonym.synonymRev.index.JoinMinIndex;
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
public class JoinHybridAll_NEW extends AlgorithmTemplate {
	public JoinHybridAll_NEW( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}

	public Validator checker;
	SampleEstimate_Split estimate;
	private int qSize = 0;
	private int indexK = 0;
	private double sampleRatio = 0;
	private int joinThreshold = 1;
	private boolean joinWithQGramFilteringRequired = true;
	private boolean joinMinSelectedForLowHigh = false;
	private boolean joinMinSelectedForHighHigh = false;

	NaiveIndex naiveIndex;
	
	// Added for HybridJoin
	// LowHighs are used as default (oneSideJoin)
	JoinMinIndex joinMinIdxLowHigh = null;
	JoinMHIndex joinMHIdxLowHigh = null;
	
	JoinMinIndex joinMinIdxHighHigh = null;
	JoinMHIndex joinMHIdxHighHigh = null;

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

		Collection<IntegerPair> rslt = join();
		stepTime.stopAndAdd( stat );
		stat.addMemory( "Mem_4_Joined" );

		stepTime.resetAndStart( "Result_4_Write_Time" );
		writeResult( rslt );
		stepTime.stopAndAdd( stat );
	}

	private void buildJoinMinIndex( int mode ) {
		// Build an index
		query.setTargetIndexSet( mode ); // 0:Whole, 1:LowHigh, 2:HighHigh
		if ( mode == 2 ) { // HighHigh
			joinMinIdxHighHigh = new JoinMinIndex( indexK, qSize, stat, query, joinThreshold, true );
		}
		else { // LowHigh, Default
			joinMinIdxLowHigh = new JoinMinIndex( indexK, qSize, stat, query, joinThreshold, true );
		}
		query.setTargetIndexSet( 0 ); 
	}

	private void buildJoinMHIndex( int mode ) {
		// Build an index
		int[] index = new int[ indexK ];
		for( int i = 0; i < indexK; i++ ) {
			index[ i ] = i;
		}
		query.setTargetIndexSet( mode ); // 0:Whole, 1:LowHigh, 2:HighHigh
		if ( mode == 2 ) { // HighHigh
			joinMHIdxHighHigh = new JoinMHIndex( indexK, qSize, query.targetIndexedSet.get(), query, stat, index, true, true, joinThreshold );
		}
		else { // LowHigh, Default
			joinMHIdxLowHigh = new JoinMHIndex( indexK, qSize, query.targetIndexedSet.get(), query, stat, index, true, true, joinThreshold );
		}
		query.setTargetIndexSet( 0 ); // Default 0
	}

	private void buildNaiveIndex() {
		naiveIndex = new NaiveIndex( query.indexedSet, query, stat, true, joinThreshold, joinThreshold / 2 );
	}

	private Set<IntegerPair> join() {

		StopWatch buildTime = StopWatch.getWatchStarted( "Result_3_1_Index_Building_Time" );
		findConstants( sampleRatio );
		
		//Original
		joinThreshold = estimate.findThetaJoinHybridAll( qSize, indexK, stat, maxIndexedEstNumRecords, maxSearchedEstNumRecords,
				query.oneSideJoin );
		joinMinSelectedForLowHigh = estimate.getJoinMinSelectedLowHigh();
		joinMinSelectedForHighHigh = estimate.getJoinMinSelectedHighHigh();
		
		int Min4LowHigh = (joinMinSelectedForLowHigh) ? 1 : 0;
		int Min4HighHigh = (joinMinSelectedForHighHigh) ? 1 : 0;
		
		stat.add( "Auto_Best_Threshold", joinThreshold );
		stat.add( "joinMinSelectedForLowHigh", Min4LowHigh );
		stat.add( "joinMinSelectedForHighHigh", Min4HighHigh );
		
//		// For Testing
//		ArrayList<Integer> thresholds = new ArrayList<Integer>();
//		for (int i=0; i< 10; i++) {
//			// Modify the constants
//			// JoinMin( gamma, delta, epsilon ), Naive ( alpha, beta ), JoinMH ( eta, theta, iota )
//			estimate.gamma = estimate.gamma * 0.8;
//			estimate.delta = estimate.delta * 0.8;
//			estimate.epsilon = estimate.epsilon * 0.8;
//			
//			// Calculate the threshold with modified constants
//			int newJoinThreshold = estimate.findThetaJoinHybridAll( qSize, indexK, stat, maxIndexedEstNumRecords, maxSearchedEstNumRecords,
//					query.oneSideJoin );
//			thresholds.add(newJoinThreshold);
//		}
//		Util.printLog( "Original Selected Threshold: " + joinThreshold );
//		for (int threshold : thresholds) {
//			Util.printLog( "new threshold: " + threshold );
//		}
		
		
		if( Long.max( maxSearchedEstNumRecords, maxIndexedEstNumRecords ) <= joinThreshold ) {
			joinWithQGramFilteringRequired = false;
		}


		StopWatch stepTime = StopWatch.getWatchStarted( "Result_7_0_JoinMin_Index_Build_Time" );

		if( joinWithQGramFilteringRequired ) {
			if( query.oneSideJoin ) {
				if( joinMinSelectedForLowHigh ) {
					buildJoinMinIndex(0);
				}
				else {
					buildJoinMHIndex(0);
				}
			}
			else {
				// Divide the Indexed part into 2 groups and use the different indexing(Min-BK, MH-FK)
				query.divideIndexedSet( joinThreshold );
				if( joinMinSelectedForLowHigh ) {
					buildJoinMinIndex(1);
				}
				else {
					buildJoinMHIndex(1);
				}
				if( joinMinSelectedForHighHigh ) {
					buildJoinMinIndex(2);
				}
				else {
					buildJoinMHIndex(2);
				}
			}
			
		}
		int joinMinResultSize = 0;
		// TODO:: Debug Messages & adding stats are not modified as using 2 different indexes
		if( DEBUG.JoinMinNaiveON ) {
			if( joinWithQGramFilteringRequired ) {
				if( joinMinSelectedForLowHigh ) {
					stat.add( "Const_Gamma_Actual", String.format( "%.2f", joinMinIdxLowHigh.gamma ) );
					stat.add( "Const_Gamma_SearchedSigCount_Actual", joinMinIdxLowHigh.searchedTotalSigCount );
					stat.add( "Const_Gamma_CountTime_Actual", String.format( "%.2f", joinMinIdxLowHigh.countTime ) );

					stat.add( "Const_Delta_Actual", String.format( "%.2f", joinMinIdxLowHigh.delta ) );
					stat.add( "Const_Delta_IndexedSigCount_Actual", joinMinIdxLowHigh.indexedTotalSigCount );
					stat.add( "Const_Delta_IndexTime_Actual", String.format( "%.2f", joinMinIdxLowHigh.indexTime ) );
				}
			}
			stepTime.stopAndAdd( stat );
			stepTime.resetAndStart( "Result_7_1_SearchEquiv_JoinMin_Time" );
		}
		buildTime.stopQuiet();
		
		
		// Join!
		StopWatch joinTime = StopWatch.getWatchStarted( "Result_3_2_Join_Time" );
		Set<IntegerPair> rslt = new ObjectOpenHashSet<IntegerPair>();
		long joinstart = System.nanoTime();
		if( joinWithQGramFilteringRequired ) {
			if( query.oneSideJoin ) {
				// No need to be changed, By Default, it uses *LowHigh Index
				for( Record s : query.searchedSet.get() ) {
					// System.out.println( "test " + s + " " + s.getEstNumRecords() );
					if( s.getEstNumTransformed() > joinThreshold ) {
						if( joinMinSelectedForLowHigh ) {
							joinMinIdxLowHigh.joinRecordMaxKThres( indexK, s, rslt, true, null, checker, joinThreshold,
									query.oneSideJoin );
						}
						else {
							joinMHIdxLowHigh.joinOneRecordThres( indexK, s, rslt, checker, joinThreshold, query.oneSideJoin,
									indexK - 1 );
						}
					}
				}
			}
			else {
				for( Record s : query.searchedSet.get() ) {
					if( joinMinSelectedForHighHigh ) {
						joinMinIdxHighHigh.joinRecordMaxKThres( indexK, s, rslt, true, null, checker, joinThreshold, query.oneSideJoin );
					}
					else {
						joinMHIdxHighHigh.joinOneRecordThres( indexK, s, rslt, checker, joinThreshold, query.oneSideJoin, indexK - 1 );
					}
					if( s.getEstNumTransformed() > joinThreshold ) {
						if( joinMinSelectedForLowHigh ) {
							joinMinIdxLowHigh.joinRecordMaxKThres( indexK, s, rslt, true, null, checker, joinThreshold, query.oneSideJoin );
						}
						else {
							joinMHIdxLowHigh.joinOneRecordThres( indexK, s, rslt, checker, joinThreshold, query.oneSideJoin, indexK - 1 );
						}
					}
				}
			}

			joinMinResultSize = rslt.size();
			stat.add( "Join_Min_Result", joinMinResultSize );
			if( joinMinSelectedForLowHigh ) {
				stat.add( "Stat_Equiv_Comparison", joinMinIdxLowHigh.equivComparisons );
			}
			else {
				stat.add( "Stat_Equiv_Comparison", joinMHIdxLowHigh.equivComparisons );
			}
		}
		double joinminJointime = System.nanoTime() - joinstart;
		joinTime.stopQuiet();

		if( DEBUG.JoinMinNaiveON ) {
			Util.printLog( "After JoinMin Result: " + rslt.size() );
			stat.add( "Const_Epsilon_JoinTime_Actual", String.format( "%.2f", joinminJointime ) );
			if( joinWithQGramFilteringRequired ) {
				stat.add( "Const_Epsilon_Predict_Actual", joinMinIdxLowHigh.predictCount );
				stat.add( "Const_Epsilon_Actual", String.format( "%.2f", joinminJointime / joinMinIdxLowHigh.predictCount ) );

				stat.add( "Const_EpsilonPrime_Actual", String.format( "%.2f", joinminJointime / joinMinIdxLowHigh.comparisonCount ) );
				stat.add( "Const_EpsilonPrime_Comparison_Actual", joinMinIdxLowHigh.comparisonCount );
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

	private void findConstants( double sampleratio ) {
		// Sample
		estimate = new SampleEstimate_Split( query, sampleratio, query.selfJoin );
		estimate.estimateJoinHybridWithSample( stat, checker, indexK, qSize );
	}

	@Override
	public String getVersion() {
		return "2.7";
	}

	@Override
	public String getName() {
		return "JoinHybridAll_NEW";
	}
}
