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
	protected int qSize = 0;
	protected int indexK = 0;
	protected int joinThreshold = 1;
	protected boolean joinMHRequired = true;

	NaiveIndex naiveIndex;
	JoinMHIndex joinMHIndex;

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
		checker.addStat( stat );
	}

	protected void buildJoinMHIndex(int threshold) {
		// Build an index
		int[] index = new int[ indexK ];
		for( int i = 0; i < indexK; i++ ) {
			index[ i ] = i;
		}

		joinMHIndex = new JoinMHIndex( indexK, qSize, query.indexedSet.get(), query, stat, index, true, true, threshold );
	}

	protected void buildNaiveIndex() {
		naiveIndex = NaiveIndex.buildIndex( joinThreshold / 2, stat, joinThreshold, true, query );
	}

	/**
	 * Although this implementation is not efficient, we did like this to measure
	 * the execution time of each part more accurate.
	 *
	 * @return
	 */
	protected ArrayList<IntegerPair> join() {
		StopWatch buildTime = StopWatch.getWatchStarted( "Result_3_1_Index_Building_Time" );
		StopWatch stepTime = StopWatch.getWatchStarted( "Result_7_0_JoinMin_Index_Build_Time" );
		if( joinMHRequired ) {
			buildJoinMHIndex( joinThreshold );
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
		buildTime.stopQuiet();

		StopWatch joinTime = StopWatch.getWatchStarted( "Result_3_2_Join_Time" );

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
			stat.add( "nCandQgrams", joinMHIndex.countValue );
			// stat.add( "Stat_Equiv_Comparison", joinMHIndex.equivComparisons );
		}

		joinTime.stopQuiet();

		double joinmhJointime = System.nanoTime() - joinstart;

		if( DEBUG.JoinMHNaiveON ) {
			Util.printLog( "After JoinMin Result: " + rslt.size() );
			stat.add( "Const_Epsilon_JoinTime_Actual", String.format( "%.2f", joinmhJointime ) );
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

		buildTime.start();
		buildNaiveIndex();
		buildTime.stopAndAdd( stat );

		if( DEBUG.JoinMHNaiveON ) {
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
		joinTime.stopAndAdd( stat );
		double joinNanoTime = System.nanoTime() - starttime;

		stat.add( "Join_Naive_Result", rslt.size() - joinMinResultSize );

		if( DEBUG.JoinMHNaiveON ) {
			stat.add( "Const_Beta_Actual", String.format( "%.2f", joinNanoTime / naiveIndex.totalExp ) );
			stat.add( "Const_Beta_JoinTime_Actual", String.format( "%.2f", joinTime ) );
			stat.add( "Const_Beta_TotalExp_Actual", String.format( "%.2f", naiveIndex.totalExp ) );

			stepTime.stopAndAdd( stat );
		}
		stat.add( "Stat_Naive_search_count", naiveSearch );

		return rslt;
	}

	@Override
	public String getVersion() {
		return "2.5";
	}

	@Override
	public String getName() {
		return "JoinMHNaiveThres";
	}
}
