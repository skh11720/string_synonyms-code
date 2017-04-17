package snu.kdd.synonym.algorithm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mine.Record;
import mine.RecordIDComparator;
import snu.kdd.synonym.data.DataInfo;
import snu.kdd.synonym.tools.IntegerComparator;
import snu.kdd.synonym.tools.Param;
import snu.kdd.synonym.tools.StatContainer;
import snu.kdd.synonym.tools.StopWatch;
import tools.IntegerPair;
import tools.Rule;
import tools.RuleTrie;
import tools.StaticFunctions;
import tools.WYK_HashMap;
import validator.Validator;
import wrapped.WrappedInteger;

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
	List<Map<IntegerPair, List<Record>>> idx;

	/**
	 * List of 1-expandable strings
	 */
	Map<Record, List<Integer>> setR;
	private static final WrappedInteger ONE = new WrappedInteger( 1 );

	public JoinHybridThres_Q( String rulefile, String Rfile, String Sfile, String outputfile, DataInfo dataInfo )
			throws IOException {
		super( rulefile, Rfile, Sfile, outputfile, dataInfo );
		idComparator = new RecordIDComparator();
		ruletrie = new RuleTrie( rulelist );
	}

	private void buildJoinMinIndex() {
		Runtime runtime = Runtime.getRuntime();

		long elements = 0;
		long SL_TH_elements = 0;
		// Build an index
		// Count Invokes per each (token, loc) pair
		List<Map<IntegerPair, WrappedInteger>> invokes = new ArrayList<Map<IntegerPair, WrappedInteger>>();
		int invokesInitialized = 0;
		idx = new ArrayList<Map<IntegerPair, List<Record>>>();

		// Actually, tableT
		StopWatch stepTime = StopWatch.getWatchStarted( "Index Count Time" );

		for( Record rec : tableIndexed ) {
			List<Set<IntegerPair>> available2Grams = rec.get2Grams();
			int searchmax = Math.min( available2Grams.size(), maxIndex );

			for( int i = invokesInitialized; i < searchmax; i++ ) {
				invokes.add( new WYK_HashMap<IntegerPair, WrappedInteger>() );
				invokesInitialized = searchmax;
			}

			for( int i = 0; i < searchmax; ++i ) {
				Map<IntegerPair, WrappedInteger> curridx_invokes = invokes.get( i );

				Set<IntegerPair> available = available2Grams.get( i );
				for( IntegerPair twogram : available ) {
					WrappedInteger count = curridx_invokes.get( twogram );
					if( count == null ) {
						curridx_invokes.put( twogram, ONE );
					}
					else if( count == ONE ) {
						count = new WrappedInteger( 2 );
						curridx_invokes.put( twogram, count );
					}
					else {
						count.increment();
					}
				}
			}
		}

		System.out.println( "Bigram retrieval : " + Record.exectime );
		System.out.println( ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 + "MB used" );

		stepTime.stopAndAdd( stat );

		stepTime.resetAndStart( "Indexing Time" );
		// Actually, tableS
		for( Record rec : tableSearched ) {
			int[] range = rec.getCandidateLengths( rec.size() - 1 );
			int minIdx = -1;
			int minInvokes = Integer.MAX_VALUE;
			int searchmax = Math.min( range[ 0 ], maxIndex );
			int[] invokearr = new int[ searchmax ];

			List<Set<IntegerPair>> available2Grams = rec.get2GramsWithBound( searchmax );

			for( int i = idx.size(); i < searchmax; i++ ) {
				idx.add( new WYK_HashMap<IntegerPair, List<Record>>() );
			}

			for( int i = 0; i < searchmax; ++i ) {
				Map<IntegerPair, WrappedInteger> curr_invokes = invokes.get( i );
				if( curr_invokes == null ) {
					minIdx = i;
					minInvokes = 0;
					break;
				}
				int invoke = 0;
				for( IntegerPair twogram : available2Grams.get( i ) ) {
					WrappedInteger count = curr_invokes.get( twogram );
					if( count != null )
						invoke += count.get();
				}
				if( invoke < minInvokes ) {
					minIdx = i;
					minInvokes = invoke;
				}
				invokearr[ i ] = invoke;
			}

			Map<IntegerPair, List<Record>> curr_idx = idx.get( minIdx );

			for( IntegerPair twogram : available2Grams.get( minIdx ) ) {
				List<Record> list = curr_idx.get( twogram );
				if( list == null ) {
					list = new ArrayList<Record>();
					curr_idx.put( twogram, list );
				}
				list.add( rec );
			}
			elements += available2Grams.get( minIdx ).size();
		}
		System.out.println( "Bigram retrieval : " + Record.exectime );
		System.out.println( ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 + "MB used" );

		System.out.println( "SH_T idx size : " + elements );
		System.out.println( "SL_TH idx size : " + SL_TH_elements );
		System.out.println( WrappedInteger.count + " Wrapped Integers" );
		for( int i = 0; i < idx.size(); i++ ) {
			System.out.println( "JoinMin idx " + i + " size: " + idx.get( i ).size() );
		}
	}

	private void clearJoinMinIndex() {
		for( Map<IntegerPair, List<Record>> map : idx )
			map.clear();
		idx.clear();
	}

	private void buildNaiveIndex() {
		// Build 1-expanded set for every record in R
		int count = 0;
		setR = new HashMap<Record, List<Integer>>();
		for( int i = 0; i < tableSearched.size(); ++i ) {
			Record rec = tableSearched.get( i );
			assert ( rec != null );
			if( rec.getEstNumRecords() > joinThreshold )
				continue;
			List<Record> expanded = rec.expandAll( ruletrie );
			assert ( expanded.size() <= joinThreshold );
			assert ( !expanded.isEmpty() );
			for( Record expR : expanded ) {
				List<Integer> list = setR.get( expR );
				if( list == null ) {
					list = new ArrayList<Integer>( 5 );
					setR.put( expR, list );
				}

				if( !list.isEmpty() && list.get( list.size() - 1 ) == i ) {
					continue;
				}

				list.add( i );
			}
			++count;
		}
		long idxsize = 0;
		for( List<Integer> list : setR.values() ) {
			idxsize += list.size();
		}
		System.out.println( count + " records are 1-expanded and indexed" );
		System.out.println( "Total index size: " + idxsize );
	}

	/**
	 * Although this implementation is not efficient, we did like this to measure
	 * the execution time of each part more accurate.
	 * 
	 * @return
	 */
	private ArrayList<IntegerPair> join() {
		ArrayList<IntegerPair> rslt = new ArrayList<IntegerPair>();
		long appliedRules_sum = 0;

		long startTime = System.currentTimeMillis();
		StopWatch stepTime = StopWatch.getWatchStarted( "JoinMin Index Building Time" );
		buildJoinMinIndex();
		stepTime.stopAndAdd( stat );
		System.out.print( "Building JoinMin Index finished " + ( System.currentTimeMillis() - startTime ) );

		stepTime.resetAndStart( "SearchEquiv JoinMin Time" );
		long time1 = System.currentTimeMillis();
		// lastTokenFiltered = 0;
		for( Record s : tableIndexed ) {
			appliedRules_sum += searchEquivsByDynamicIndex( s, idx, rslt );
		}
		// stat.add( "Last Token Filtered", lastTokenFiltered );
		stat.add( "AppliedRules Sum", appliedRules_sum );
		stepTime.stopAndAdd( stat );
		time1 = System.currentTimeMillis() - time1;
		clearJoinMinIndex();

		startTime = System.currentTimeMillis();
		stepTime.resetAndStart( "Naive Index Building Time" );
		buildNaiveIndex();
		stepTime.stopAndAdd( stat );
		System.out.print( "Building Naive Index finished" );
		System.out.println( " " + ( System.currentTimeMillis() - startTime ) );

		stepTime.resetAndStart( "SearchEquiv Naive Time" );
		long time2 = System.currentTimeMillis();
		int naiveSearch = 0;
		for( Record s : tableIndexed ) {
			if( s.getEstNumRecords() > joinThreshold ) {
				continue;
			}
			else {
				searchEquivsByNaive1Expansion( s, rslt );
				naiveSearch++;
			}
		}
		stat.add( "Naive search count", naiveSearch );
		stepTime.stopAndAdd( stat );
		time2 = System.currentTimeMillis() - time2;

		System.out.println( "Avg applied rules : " + appliedRules_sum + "/" + rslt.size() );
		System.out.println( "SH_T + SL_TH : " + time1 );
		System.out.println( "SL_TL : " + time2 );

		return rslt;
	}

	private int searchEquivsByDynamicIndex( Record s, List<Map<IntegerPair, List<Record>>> idx, List<IntegerPair> rslt ) {
		boolean is_TH_record = s.getEstNumRecords() > joinThreshold;

		int appliedRules_sum = 0;
		int idxSize = idx.size();
		List<Set<IntegerPair>> available2Grams = s.get2GramsWithBound( idxSize );
		int[] range = s.getCandidateLengths( s.size() - 1 );
		int searchmax = Math.min( available2Grams.size(), maxIndex );
		for( int i = 0; i < searchmax; ++i ) {
			if( i >= idx.size() ) {
				break;
			}

			Map<IntegerPair, List<Record>> curr_idx = idx.get( i );

			Set<Record> candidates = new HashSet<Record>();
			for( IntegerPair twogram : available2Grams.get( i ) ) {
				List<Record> tree = curr_idx.get( twogram );

				if( tree == null ) {
					continue;
				}

				for( int j = tree.size() - 1; j >= 0; --j ) {
					Record rec = tree.get( j );
					if( !is_TH_record && rec.getEstNumRecords() <= joinThreshold ) {
						continue;
					}
					else if( StaticFunctions.overlap( rec.getMinLength(), rec.getMaxLength(), range[ 0 ], range[ 1 ] ) ) {
						// if( s.shareLastToken( rec ) ) {
						candidates.add( rec );
						// }
						// else {
						// lastTokenFiltered++;
						// }
					}
				}
			}

			if( skipChecking ) {
				continue;
			}

			for( Record recR : candidates ) {
				int compare = checker.isEqual( recR, s );
				if( compare >= 0 ) {
					rslt.add( new IntegerPair( recR.getID(), s.getID() ) );
					appliedRules_sum += compare;
				}
			}
		}
		return appliedRules_sum;
	}

	private void searchEquivsByNaive1Expansion( Record s, List<IntegerPair> rslt ) {
		ArrayList<List<Integer>> candidates = new ArrayList<List<Integer>>();
		ArrayList<Record> expanded = s.expandAll( ruletrie );
		for( Record exp : expanded ) {
			List<Integer> list = setR.get( exp );
			if( list == null ) {
				continue;
			}
			candidates.add( list );
		}
		List<Integer> union = StaticFunctions.union( candidates, new IntegerComparator() );
		for( Integer idx : union ) {
			rslt.add( new IntegerPair( idx, s.getID() ) );
		}
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

		System.out.println( "Average str length: " + strlengthsum + "/" + strs );
		System.out.println( "Average maxinvsearchrange: " + strmaxinvsearchrangesum + "/" + strs );
		System.out.println( "Maximum str length: " + maxstrlength );
		System.out.println( "Average rhs length: " + rhslengthsum + "/" + rules );
		System.out.println( "Maximum rhs length: " + maxrhslength );
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
		long maxTEstNumRecords = tableSearched.get( tableSearched.size() - 1 ).getEstNumRecords();

		for( int i = 0; i < tableIndexed.size(); ++i ) {
			Record t = tableIndexed.get( i );
			t.setID( i );
		}
		long maxSEstNumRecords = tableIndexed.get( tableIndexed.size() - 1 ).getEstNumRecords();

		System.out.println( "Max S expanded size : " + maxSEstNumRecords );
		System.out.println( "Max T expanded size : " + maxTEstNumRecords );
	}

	public void run() {
		StopWatch stepTime = StopWatch.getWatchStarted( "Preprocess Total Time" );
		preprocess( compact, maxIndex, useAutomata );
		stepTime.stopAndAdd( stat );
		System.out.print( "Preprocess finished" );

		// Retrieve statistics
		stepTime.resetAndStart( "Statistics Time" );
		statistics();
		stepTime.stopAndAdd( stat );

		stepTime.resetAndStart( "Join Total Time" );
		Collection<IntegerPair> rslt = join();
		stepTime.stopAndAdd( stat );
		System.out.print( "Join finished" );

		System.out.println( "Result time " + rslt.size() );
		System.out.println( "Union counter: " + StaticFunctions.union_cmp_counter );

		writeResult( rslt );
	}

	@Override
	public String getVersion() {
		return "1.0";
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

		this.run();
		Validator.printStats();
	}
}
