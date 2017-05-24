package snu.kdd.synonym.algorithm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import mine.Record;
import mine.RecordIDComparator;
import snu.kdd.synonym.data.DataInfo;
import snu.kdd.synonym.estimation.CountEntry;
import snu.kdd.synonym.estimation.SampleEstimate;
import snu.kdd.synonym.tools.JoinMinIndex;
import snu.kdd.synonym.tools.NaiveIndex;
import snu.kdd.synonym.tools.Param;
import snu.kdd.synonym.tools.StatContainer;
import snu.kdd.synonym.tools.StopWatch;
import snu.kdd.synonym.tools.Util;
import tools.DEBUG;
import tools.IntegerPair;
import tools.Rule;
import tools.RuleTrie;
import tools.StaticFunctions;
import validator.Validator;

/**
 * Given threshold, if a record has more than 'threshold' 1-expandable strings,
 * use an index to store them.
 * Otherwise, generate all 1-expandable strings and then use them to check
 * if two strings are equivalent.
 * Utilize only one index by sorting records according to their expanded size.
 * It first build JoinMin(JoinH2Gram) index and then change threshold / modify
 * index in order to find the best execution time.
 */
public class JoinHybridOpt_Q extends AlgorithmTemplate {
	static boolean useAutomata = true;
	static boolean skipChecking = false;
	static int maxIndex = Integer.MAX_VALUE;
	static boolean compact = false;
	static boolean singleside;
	static Validator checker;

	private int qSize = -1;

	RecordIDComparator idComparator;
	RuleTrie ruletrie;

	int joinThreshold = 0;

	SampleEstimate estimate;

	private boolean joinMinRequired = true;

	/**
	 * List of 1-expandable strings
	 */
	NaiveIndex naiveIndex;
	JoinMinIndex joinMinIdx;
	/**
	 * Estimated number of comparisons
	 */
	long est_cmps;
	long memlimit_expandedS;

	private double totalExpLengthNaiveIndex = 0;
	private double totalExpNaiveJoin = 0;

	private double partialExpLengthNaiveIndex[];
	private double partialExpNaiveJoin[];

	private long maxSearchedEstNumRecords;
	private long maxIndexedEstNumRecords;

	private DataInfo dataInfo;

	public JoinHybridOpt_Q( String rulefile, String Rfile, String Sfile, String outputfile, DataInfo dataInfo,
			boolean joinOneSide, StatContainer stat ) throws IOException {
		super( rulefile, Rfile, Sfile, outputfile, dataInfo, joinOneSide, stat );
		idComparator = new RecordIDComparator();
		ruletrie = new RuleTrie( rulelist );

		this.dataInfo = dataInfo;
	}

	@Override
	public void run( String[] args ) {
		Param params = Param.parseArgs( args, stat );
		// Setup parameters
		useAutomata = params.isUseACAutomata();
		skipChecking = params.isSkipChecking();
		maxIndex = params.getMaxIndex();
		compact = params.isCompact();
		singleside = params.isSingleside();
		checker = params.getValidator();
		qSize = params.getQGramSize();

		run( params.getSampleRatio() );
		Validator.printStats();
	}

	public void run( double sampleratio ) {
		StopWatch stepTime = null;

		if( DEBUG.JoinHybridON ) {
			stepTime = StopWatch.getWatchStarted( "Result_2_Preprocess_Total_Time" );
		}

		preprocess( compact, maxIndex, useAutomata );

		// Retrieve statistics

		if( DEBUG.JoinHybridON ) {
			stepTime.stopAndAdd( stat );
			stepTime.resetAndStart( "Result_3_Statistics_Time" );
			statistics();
			stepTime.stopAndAdd( stat );
			stepTime.resetAndStart( "Result_4_Find_Constants_Time" );
		}

		// Estimate constants
		findConstants( sampleratio );

		if( DEBUG.JoinHybridON ) {
			stepTime.stopAndAdd( stat );
			stepTime.resetAndStart( "Result_6_Find_Theta_Time" );
		}

		// joinThreshold = findTheta( Integer.MAX_VALUE );
		// joinThreshold = estimate.findTheta( qSize, maxIndex, stat, totalExpLengthNaiveIndex, totalExpNaiveJoin,
		// partialExpLengthNaiveIndex, partialExpNaiveJoin, maxIndexedEstNumRecords, maxSearchedEstNumRecords );

		joinThreshold = estimate.findThetaUnrestricted( qSize, stat, maxIndexedEstNumRecords, maxSearchedEstNumRecords );
		if( Long.max( maxSearchedEstNumRecords, maxIndexedEstNumRecords ) <= joinThreshold ) {
			joinMinRequired = false;
		}

		if( DEBUG.JoinHybridON ) {
			Util.printLog( "Join Threshold: " + joinThreshold );
			stepTime.stopAndAdd( stat );
			stepTime.resetAndStart( "Result_7_Join_Time" );
		}

		Collection<IntegerPair> rslt = join();

		if( DEBUG.JoinHybridON ) {
			stepTime.stopAndAdd( stat );

			Util.printLog( "Result size: " + rslt.size() );
			Util.printLog( "Union counter: " + StaticFunctions.union_cmp_counter );
		}

		writeResult( rslt );
	}

	@Override
	protected void preprocess( boolean compact, int maxIndex, boolean useAutomata ) {
		super.preprocess( compact, maxIndex, useAutomata );

		// Sort R and S with expanded sizes
		Comparator<Record> cmp = new Comparator<Record>() {
			@Override
			public int compare( Record o1, Record o2 ) {
				long est1 = o1.getEstNumRecords();
				long est2 = o2.getEstNumRecords();
				return Long.compare( est1, est2 );
			}
		};

		long sortTime;
		if( DEBUG.JoinHybridON ) {
			sortTime = System.currentTimeMillis();
		}

		Collections.sort( tableSearched, cmp );
		Collections.sort( tableIndexed, cmp );

		if( DEBUG.JoinHybridON ) {
			stat.add( "Result_2_7_Preprocess_Sorting_Time", System.currentTimeMillis() - sortTime );
		}

		// Reassign ID and collect statistics for join naive
		partialExpLengthNaiveIndex = new double[ CountEntry.countMax ];
		partialExpNaiveJoin = new double[ CountEntry.countMax ];

		int currentIdx = 0;
		int nextThreshold = 10;

		for( int i = 0; i < tableSearched.size(); ++i ) {
			Record t = tableSearched.get( i );
			t.setID( i );

			long est = t.getEstNumRecords();
			totalExpNaiveJoin += est;

			while( currentIdx != CountEntry.countMax - 1 && est >= nextThreshold ) {
				nextThreshold *= 10;
				currentIdx++;
			}

			int idx = CountEntry.getIndex( est );
			partialExpNaiveJoin[ idx ] += est;
		}

		currentIdx = 0;
		nextThreshold = 10;
		for( int i = 0; i < tableIndexed.size(); ++i ) {
			Record s = tableIndexed.get( i );
			s.setID( i );

			long est = s.getEstNumRecords();
			double estLength = (double) s.getEstNumRecords() * (double) s.getTokenArray().length;
			totalExpLengthNaiveIndex += estLength;

			while( currentIdx != CountEntry.countMax - 1 && s.getEstNumRecords() >= nextThreshold ) {
				nextThreshold *= 10;
				currentIdx++;
			}

			int idx = CountEntry.getIndex( est );
			partialExpLengthNaiveIndex[ idx ] += estLength;
		}

		maxSearchedEstNumRecords = tableSearched.get( tableSearched.size() - 1 ).getEstNumRecords();
		maxIndexedEstNumRecords = tableIndexed.get( tableIndexed.size() - 1 ).getEstNumRecords();

		if( DEBUG.JoinHybridON ) {
			for( int i = 0; i < CountEntry.countMax; i++ ) {
				stat.add( "Preprocess_ExpLength_" + i, partialExpLengthNaiveIndex[ i ] );
				stat.add( "Preprocess_Exp_" + i, partialExpNaiveJoin[ i ] );
			}
			stat.add( "Preprocess_ExpLength_Total", totalExpLengthNaiveIndex );
			stat.add( "Preprocess_Exp_Total", totalExpNaiveJoin );
		}
	}

	private void buildJoinMinIndex() {
		// Build an index
		joinMinIdx = JoinMinIndex.buildIndex( tableSearched, tableIndexed, maxIndex, qSize, stat, true );
	}

	private void buildNaiveIndex() {
		naiveIndex = NaiveIndex.buildIndex( tableIndexed, joinThreshold / 2, stat, joinThreshold, false, oneSideJoin );
	}

	/**
	 * Although this implementation is not efficient, we did like this to measure
	 * the execution time of each part more accurate.
	 * 
	 * @return
	 */
	private ArrayList<IntegerPair> join() {
		StopWatch stepTime = StopWatch.getWatchStarted( "Result_7_0_JoinMin_Index_Build_Time" );
		if( joinMinRequired ) {
			buildJoinMinIndex();
		}

		if( DEBUG.JoinHybridON ) {
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

		ArrayList<IntegerPair> rslt = new ArrayList<IntegerPair>();
		long joinstart = System.nanoTime();
		if( joinMinRequired ) {
			for( Record s : tableSearched ) {
				joinMinIdx.joinRecordThres( s, rslt, true, null, checker, joinThreshold );
			}
		}
		double joinminJointime = System.nanoTime() - joinstart;

		if( DEBUG.JoinHybridON ) {
			Util.printLog( "After JoinMin Result: " + rslt.size() );
			stat.add( "Const_Epsilon_JoinTime_Actual", String.format( "%.2f", joinminJointime ) );
			if( joinMinRequired ) {
				stat.add( "Const_Epsilon_Predict_Actual", joinMinIdx.predictCount );
				stat.add( "Const_Epsilon_Actual", String.format( "%.2f", joinminJointime / joinMinIdx.predictCount ) );

				// TODO DEBUG
				stat.add( "Const_EpsilonPrime_Actual", String.format( "%.2f", joinminJointime / joinMinIdx.comparisonCount ) );
				stat.add( "Const_EpsilonPrime_Comparison_Actual", joinMinIdx.comparisonCount );
			}
			stepTime.stopAndAdd( stat );
			stepTime.resetAndStart( "Result_7_2_Naive Index Building Time" );
		}

		buildNaiveIndex();

		if( DEBUG.JoinHybridON ) {
			stat.add( "Const_Alpha_Actual", String.format( "%.2f", naiveIndex.alpha ) );
			stat.add( "Const_Alpha_IndexTime_Actual", String.format( "%.2f", naiveIndex.indexTime ) );
			stat.add( "Const_Alpha_ExpLength_Actual", String.format( "%.2f", naiveIndex.totalExpLength ) );

			stepTime.stopAndAdd( stat );
			stepTime.resetAndStart( "Result_7_3_SearchEquiv Naive Time" );
		}

		@SuppressWarnings( "unused" )
		int naiveSearch = 0;
		long starttime = System.nanoTime();
		for( Record s : tableSearched ) {
			if( s.getEstNumRecords() > joinThreshold ) {
				continue;
			}
			else {
				naiveIndex.joinOneRecord( s, rslt );
				naiveSearch++;
			}
		}
		double joinTime = System.nanoTime() - starttime;

		if( DEBUG.JoinHybridON ) {
			stat.add( "Const_Beta_Actual", String.format( "%.2f", joinTime / naiveIndex.totalExp ) );
			stat.add( "Const_Beta_JoinTime_Actual", String.format( "%.2f", joinTime ) );
			stat.add( "Const_Beta_TotalExp_Actual", String.format( "%.2f", naiveIndex.totalExp ) );

			stat.add( "Stat_Naive search count", naiveSearch );
			stepTime.stopAndAdd( stat );
		}

		return rslt;
	}

	public void statistics() {
		long strlengthsum = 0;
		long strmaxinvsearchrangesum = 0;
		int strs = 0;
		int maxstrlength = 0;

		long rhslengthsum = 0;
		int rules = 0;
		int maxrhslength = 0;

		for( Record rec : tableSearched ) {
			strmaxinvsearchrangesum += rec.getMaxInvSearchRange();
			int length = rec.getTokenArray().length;
			++strs;
			strlengthsum += length;
			maxstrlength = Math.max( maxstrlength, length );
		}
		for( Record rec : tableIndexed ) {
			strmaxinvsearchrangesum += rec.getMaxInvSearchRange();
			int length = rec.getTokenArray().length;
			++strs;
			strlengthsum += length;
			maxstrlength = Math.max( maxstrlength, length );
		}

		for( Rule rule : getRulelist() ) {
			int length = rule.getTo().length;
			++rules;
			rhslengthsum += length;
			maxrhslength = Math.max( maxrhslength, length );
		}

		Util.printLog( "Average str length: " + strlengthsum + "/" + strs );
		Util.printLog( "Average maxinvsearchrange: " + strmaxinvsearchrangesum + "/" + strs );
		Util.printLog( "Maximum str length: " + maxstrlength );
		Util.printLog( "Average rhs length: " + rhslengthsum + "/" + rules );
		Util.printLog( "Maximum rhs length: " + maxrhslength );
	}

	private void findConstants( double sampleratio ) {
		// Sample
		estimate = new SampleEstimate( tableSearched, tableIndexed, sampleratio, dataInfo.isSelfJoin() );
		estimate.estimateWithSample( stat, this, checker, qSize );
	}

	@Override
	public String getVersion() {
		return "1.2";
	}

	@Override
	public String getName() {
		return "JoinHybridOpt_Q";
	}

	class Directory {
		List<Record> list;
		int SHsize;

		Directory() {
			list = new ArrayList<Record>();
			SHsize = 0;
		}
	}
}
