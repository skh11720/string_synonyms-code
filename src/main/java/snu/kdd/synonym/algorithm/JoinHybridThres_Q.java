package snu.kdd.synonym.algorithm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import mine.Record;
import mine.RecordIDComparator;
import snu.kdd.synonym.data.DataInfo;
import snu.kdd.synonym.tools.JoinMinIndex;
import snu.kdd.synonym.tools.NaiveIndex;
import snu.kdd.synonym.tools.Param;
import snu.kdd.synonym.tools.StatContainer;
import snu.kdd.synonym.tools.StopWatch;
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
 */
public class JoinHybridThres_Q extends AlgorithmTemplate {
	static boolean useAutomata = true;
	static boolean skipChecking = false;
	static int maxIndex = Integer.MAX_VALUE;
	static boolean compact = false;
	static int joinThreshold;
	static boolean singleside;
	static Validator checker;

	private int qSize = -1;

	// long lastTokenFiltered = 0;

	RecordIDComparator idComparator;
	RuleTrie ruletrie;

	/**
	 * Key: (token, index) pair<br/>
	 * Value: (min, max, record) triple
	 */
	/**
	 * Index of the records in R for the strings in S which has more than
	 * 'threshold' 1-expandable strings
	 */
	JoinMinIndex joinMinIdx;
	boolean joinMinRequired = true;

	/**
	 * List of 1-expandable strings
	 */
	NaiveIndex naiveIndex;

	public JoinHybridThres_Q( String rulefile, String Rfile, String Sfile, String outputfile, DataInfo dataInfo )
			throws IOException {
		super( rulefile, Rfile, Sfile, outputfile, dataInfo );
		idComparator = new RecordIDComparator();
		ruletrie = new RuleTrie( rulelist );
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
		Collections.sort( tableSearched, cmp );
		Collections.sort( tableIndexed, cmp );

		// Reassign ID
		for( int i = 0; i < tableSearched.size(); ++i ) {
			Record s = tableSearched.get( i );
			s.setID( i );
		}

		for( int i = 0; i < tableIndexed.size(); ++i ) {
			Record t = tableIndexed.get( i );
			t.setID( i );
		}

		// the last element has the most estimtated num records
		long maxSearchedEstNumRecords = tableSearched.get( tableSearched.size() - 1 ).getEstNumRecords();
		long maxIndexedEstNumRecords = tableIndexed.get( tableIndexed.size() - 1 ).getEstNumRecords();

		if( maxSearchedEstNumRecords <= joinThreshold && maxIndexedEstNumRecords <= joinThreshold ) {
			joinMinRequired = false;
			joinThreshold = Integer.max( (int) maxSearchedEstNumRecords, (int) maxIndexedEstNumRecords ) + 1;
		}

		System.out.println( "Max Indexed expanded size : " + maxIndexedEstNumRecords );
		System.out.println( "Max Searched expanded size : " + maxSearchedEstNumRecords );
		if( joinMinRequired ) {
			System.out.println( "JoinMin is not requied" );
		}
	}

	private void buildJoinMinIndex() {
		// Build an index
		// Count Invokes per each (token, loc) pair
		joinMinIdx = JoinMinIndex.buildIndex( tableSearched, tableIndexed, maxIndex, qSize, stat, true );
	}

	private void clearJoinMinIndex() {
		joinMinIdx.clear();
	}

	private void buildNaiveIndex() {
		// Build 1-expanded set for every record in R
		int initialSize;

		if( joinThreshold * tableIndexed.size() / 2.0 < 1e8 ) {
			initialSize = joinThreshold / 2;
		}
		else {
			initialSize = (int) ( 1e8 * 2.0 / tableIndexed.size() );
		}

		if( initialSize == 0 ) {
			initialSize = 1;
		}

		naiveIndex = NaiveIndex.buildIndex( tableIndexed, initialSize, stat, joinThreshold, true );
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
		stepTime.stopAndAdd( stat );

		stepTime.resetAndStart( "Result_7_1_SearchEquiv_JoinMin_Time" );

		ArrayList<IntegerPair> rslt = new ArrayList<IntegerPair>();
		if( joinMinRequired ) {
			for( Record s : tableSearched ) {
				joinMinIdx.joinRecordThres( s, rslt, true, null, checker, joinThreshold );
			}
			clearJoinMinIndex();
			stat.add( "AppliedRules Sum", joinMinIdx.appliedRulesSum );
		}
		// stat.add( "Last Token Filtered", lastTokenFiltered );

		stepTime.stopAndAdd( stat );

		stepTime.resetAndStart( "Result_7_2_Naive Index Building Time" );
		buildNaiveIndex();
		stepTime.stopAndAdd( stat );
		System.out.print( "Building Naive Index finished" );

		stepTime.resetAndStart( "Result_7_3_SearchEquiv Naive Time" );

		int naiveSearch = 0;
		for( Record s : tableSearched ) {
			if( s.getEstNumRecords() > joinThreshold ) {
				break;
			}
			else {
				naiveIndex.joinOneRecord( s, rslt );
				naiveSearch++;
			}
		}
		stat.add( "Naive search count", naiveSearch );
		naiveIndex.addStat( stat, "Counter_Join" );
		stepTime.stopAndAdd( stat );

		return rslt;
	}

	private void statistics() {
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

		System.out.println( "Average str length: " + strlengthsum + "/" + strs );
		System.out.println( "Average maxinvsearchrange: " + strmaxinvsearchrangesum + "/" + strs );
		System.out.println( "Maximum str length: " + maxstrlength );
		System.out.println( "Average rhs length: " + rhslengthsum + "/" + rules );
		System.out.println( "Maximum rhs length: " + maxrhslength );
	}

	public void run() {
		StopWatch stepTime = StopWatch.getWatchStarted( "Result_2_Preprocess_Total_Time" );
		preprocess( compact, maxIndex, useAutomata );
		stepTime.stopAndAdd( stat );
		System.out.print( "Preprocess finished" );

		// Retrieve statistics
		if( DEBUG.JoinHybridON ) {
			stepTime.resetAndStart( "Result_3_Statistics_Time" );
			statistics();
			stepTime.stopAndAdd( stat );
		}

		stepTime.resetAndStart( "Result_7_Join_Time" );
		Collection<IntegerPair> rslt = join();
		stepTime.stopAndAdd( stat );
		System.out.print( "Join finished" );

		System.out.println( "Result time " + rslt.size() );
		System.out.println( "Union counter: " + StaticFunctions.union_cmp_counter );

		writeResult( rslt );
	}

	@Override
	public String getVersion() {
		return "1.2";
	}

	@Override
	public String getName() {
		return "JoinHybridThres_Q";
	}

	@Override
	public void run( String[] args, StatContainer stat ) {
		this.stat = stat;

		Param params = Param.parseArgs( args, stat );
		// Setup parameters
		useAutomata = params.isUseACAutomata();
		skipChecking = params.isSkipChecking();
		maxIndex = params.getMaxIndex();
		compact = params.isCompact();
		joinThreshold = params.getJoinThreshold();
		singleside = params.isSingleside();
		checker = params.getValidator();
		qSize = params.getQGramSize();

		this.run();
		Validator.printStats();
	}
}
