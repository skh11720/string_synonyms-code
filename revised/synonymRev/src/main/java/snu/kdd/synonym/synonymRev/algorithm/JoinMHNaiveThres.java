package snu.kdd.synonym.synonymRev.algorithm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.index.JoinMHIndex;
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
public class JoinMHNaiveThres extends AlgorithmTemplate {
	public JoinMHNaiveThres( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}

	public Validator checker;
	private int qSize = 0;
	private int indexK = 0;
	private int joinThreshold = 1;
	private boolean joinMHRequired = true;

	NaiveIndex naiveIndex;
	JoinMHIndex joinMHIndex;

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
		joinThreshold = params.threshold;

		StopWatch stepTime = StopWatch.getWatchStarted( "Result_2_Preprocess_Total_Time" );
		preprocess();
		stepTime.stopAndAdd( stat );
		// Retrieve statistics

		stepTime.resetAndStart( "Result_3_Run_Time" );
		// Estimate constants

		if( Long.max( maxSearchedEstNumRecords, maxIndexedEstNumRecords ) <= joinThreshold ) {
			joinMHRequired = false;
		}

		Util.printLog( "Selected Threshold: " + joinThreshold );

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

		joinMHIndex = new JoinMHIndex( indexK, qSize, query.indexedSet.get(), query, stat, index, true, true );
	}

	private void buildNaiveIndex() {
		naiveIndex = NaiveIndex.buildIndex( joinThreshold / 2, stat, joinThreshold, false, query );
	}

	/**
	 * Although this implementation is not efficient, we did like this to measure
	 * the execution time of each part more accurate.
	 *
	 * @return
	 */
	private ArrayList<IntegerPair> join() {
		StopWatch stepTime = StopWatch.getWatchStarted( "Result_7_0_JoinMin_Index_Build_Time" );
		if( joinMHRequired ) {
			buildJoinMHIndex();
		}
		int joinMinResultSize = 0;
		if( DEBUG.JoinMHNaiveON ) {
			if( joinMHRequired ) {
				// stat.add( "Const_Gamma_Actual", String.format( "%.2f", joinMHIndex.gamma ) );
				// stat.add( "Const_Gamma_SearchedSigCount_Actual", joinMHIndex.searchedTotalSigCount );
				// stat.add( "Const_Gamma_CountTime_Actual", String.format( "%.2f", joinMHIndex.countTime ) );
				//
				// stat.add( "Const_Delta_Actual", String.format( "%.2f", joinMHIndex.delta ) );
				// stat.add( "Const_Delta_IndexedSigCount_Actual", joinMHIndex.indexedTotalSigCount );
				// stat.add( "Const_Delta_IndexTime_Actual", String.format( "%.2f", joinMHIndex.indexTime ) );
			}
			stepTime.stopAndAdd( stat );
			stepTime.resetAndStart( "Result_7_1_SearchEquiv_JoinMin_Time" );
		}

		ArrayList<IntegerPair> rslt = new ArrayList<IntegerPair>();
		long joinstart = System.nanoTime();
		if( joinMHRequired ) {
			if( query.oneSideJoin ) {
				for( Record s : query.searchedSet.get() ) {
					// System.out.println( "test " + s + " " + s.getEstNumRecords() );
					if( s.getEstNumTransformed() > joinThreshold ) {
						joinMHIndex.joinOneRecordThres( indexK, s, rslt, checker, joinThreshold, query.oneSideJoin, indexK - 1 );
					}
				}
			}
			else {
				for( Record s : query.searchedSet.get() ) {
					joinMHIndex.joinOneRecordThres( indexK, s, rslt, checker, joinThreshold, query.oneSideJoin, indexK - 1 );
				}
			}

			joinMinResultSize = rslt.size();
			stat.add( "Join_Min_Result", joinMinResultSize );
			// stat.add( "Stat_Equiv_Comparison", joinMHIndex.equivComparisons );
		}
		double joinminJointime = System.nanoTime() - joinstart;

		if( DEBUG.JoinMHNaiveON ) {
			Util.printLog( "After JoinMin Result: " + rslt.size() );
			stat.add( "Const_Epsilon_JoinTime_Actual", String.format( "%.2f", joinminJointime ) );
			if( joinMHRequired ) {
				// stat.add( "Const_Epsilon_Predict_Actual", joinMHIndex.predictCount );
				// stat.add( "Const_Epsilon_Actual", String.format( "%.2f", joinminJointime / joinMHIndex.predictCount ) );
				//
				// stat.add( "Const_EpsilonPrime_Actual", String.format( "%.2f", joinminJointime / joinMHIndex.comparisonCount ) );
				// stat.add( "Const_EpsilonPrime_Comparison_Actual", joinMHIndex.comparisonCount );
			}
			stepTime.stopAndAdd( stat );
			stepTime.resetAndStart( "Result_7_2_Naive Index Building Time" );
		}

		buildNaiveIndex();

		if( DEBUG.JoinMHNaiveON ) {
			stat.add( "Const_Alpha_Actual", String.format( "%.2f", naiveIndex.alpha ) );
			stat.add( "Const_Alpha_IndexTime_Actual", String.format( "%.2f", naiveIndex.indexTime ) );
			stat.add( "Const_Alpha_ExpLength_Actual", String.format( "%.2f", naiveIndex.totalExpLength ) );

			stepTime.stopAndAdd( stat );
			stepTime.resetAndStart( "Result_7_3_SearchEquiv Naive Time" );
		}

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
		double joinTime = System.nanoTime() - starttime;

		stat.add( "Join_Naive_Result", rslt.size() - joinMinResultSize );

		if( DEBUG.JoinMHNaiveON ) {
			stat.add( "Const_Beta_Actual", String.format( "%.2f", joinTime / naiveIndex.totalExp ) );
			stat.add( "Const_Beta_JoinTime_Actual", String.format( "%.2f", joinTime ) );
			stat.add( "Const_Beta_TotalExp_Actual", String.format( "%.2f", naiveIndex.totalExp ) );

			stat.add( "Stat_Naive search count", naiveSearch );
			stepTime.stopAndAdd( stat );
		}

		return rslt;
	}

	@Override
	public String getVersion() {
		return "2.0";
	}

	@Override
	public String getName() {
		return "JoinMHNaiveThres";
	}
}
