package snu.kdd.synonym.algorithm;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
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

	RecordIDComparator idComparator;
	RuleTrie ruletrie;

	int joinThreshold = 0;

	// alpha: Naive indexing time per transformed strings of table T
	double alpha;
	// beta: Navie join time per transformed strings of table S
	double beta;
	// gamma: JoinMin counting twogram time per twograms of table S
	double gamma;
	// delta: JoinMin indexing time per twograms of table T
	double delta;
	// epsilon: JoinMin join time per candidate of table S
	double epsilon;

	private static final WrappedInteger ONE = new WrappedInteger( 1 );
	private static final int RECORD_CLASS_BYTES = 64;

	/* private int intarrbytes(int len) {
	 * // Accurate bytes in 64bit machine is:
	 * // ceil(4 * len / 8) * 8 + 16
	 * return len * 4 + 16;
	 * } */

	class Directory {
		List<Record> list;
		int SHsize;

		Directory() {
			list = new ArrayList<Record>();
			SHsize = 0;
		}
	}

	/**
	 * Key: (token, index) pair<br/>
	 * Value: (min, max, record) triple
	 */
	/**
	 * Index of the records in S
	 * (SL x TH)
	 */
	List<Map<IntegerPair, Directory>> idx;
	List<Map<IntegerPair, WrappedInteger>> T_invokes;

	/**
	 * List of 1-expandable strings
	 */
	Map<Record, List<Integer>> setR;
	/**
	 * Estimated number of comparisons
	 */
	long est_cmps;

	long memlimit_expandedS;

	public JoinHybridOpt_Q( String rulefile, String Rfile, String Sfile, String outputfile, DataInfo dataInfo )
			throws IOException {
		super( rulefile, Rfile, Sfile, outputfile, dataInfo );
		idComparator = new RecordIDComparator();
		ruletrie = new RuleTrie( rulelist );
	}

	private void buildJoinMinIndex() {
		Runtime runtime = Runtime.getRuntime();

		long elements = 0;
		est_cmps = 0;
		// Build an index
		// Count Invokes per each (token, loc) pair
		T_invokes = new ArrayList<Map<IntegerPair, WrappedInteger>>();
		int invokesInitialized = 0;
		idx = new ArrayList<Map<IntegerPair, Directory>>();

		// Actually, tableT
		StopWatch stepTime = StopWatch.getWatchStarted( "Result_5_1_Index Count Time" );

		ArrayList<Integer> countPerPosition = new ArrayList<Integer>();

		for( Record rec : tableS ) {
			List<Set<IntegerPair>> available2Grams = rec.get2Grams();
			int searchmax = Math.min( available2Grams.size(), maxIndex );

			for( int i = invokesInitialized; i < searchmax; i++ ) {
				T_invokes.add( new WYK_HashMap<IntegerPair, WrappedInteger>() );
				countPerPosition.add( 0 );
			}
			if( invokesInitialized < searchmax ) {
				invokesInitialized = searchmax;
			}

			for( int i = 0; i < searchmax; ++i ) {
				Map<IntegerPair, WrappedInteger> curridx_invokes = T_invokes.get( i );

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

				int newSize = countPerPosition.get( i ) + available.size();

				countPerPosition.set( i, newSize );
			}
		}

		// DEBUG
		{
			try {
				for( int i = 0; i < T_invokes.size(); i++ ) {
					BufferedWriter bw = new BufferedWriter( new FileWriter( "DEBUG_T_invokes" + i + ".txt" ) );
					for( Entry<IntegerPair, WrappedInteger> entry : T_invokes.get( i ).entrySet() ) {
						bw.write( entry.getKey() + " " + entry.getValue() + "\n" );
					}
					bw.close();
				}
			}
			catch( Exception e ) {
				e.printStackTrace();
			}
		}

		for( int i = 0; i < countPerPosition.size(); i++ ) {
			stat.add( String.format( "Stat_JoinMin_COUNT%02d", i ), countPerPosition.get( i ) );
		}

		System.out.println( "Bigram retrieval : " + Record.exectime );
		// System.out.println( ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 + "MB used for counting bigrams" );
		stat.add( "Mem_After_Counting_Bigrams", ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 );
		stepTime.stopAndAdd( stat );

		stepTime.resetAndStart( "Result_5_2_Indexing Time" );
		// Actually, tableS

		// DEBUG
		try {
			BufferedWriter bw = new BufferedWriter( new FileWriter( "MinIndex.txt" ) );
			boolean debug = false;

			for( Record rec : tableT ) {

				if( rec.getID() == 0 ) {
					debug = true;
				}
				else {
					debug = false;
				}

				int[] range = rec.getCandidateLengths( rec.size() - 1 );
				int minIdx = -1;
				int minInvokes = Integer.MAX_VALUE;
				int searchmax = Math.min( range[ 0 ], maxIndex );

				List<Set<IntegerPair>> available2Grams = rec.get2GramsWithBound( searchmax );

				for( int i = idx.size(); i < searchmax; i++ ) {
					idx.add( new WYK_HashMap<IntegerPair, Directory>() );
				}

				bw.write( "Search max : " + searchmax + "\n" );

				for( int i = 0; i < searchmax; ++i ) {
					Map<IntegerPair, WrappedInteger> curr_invokes = T_invokes.get( i );
					if( curr_invokes == null ) {
						// there is no twogram in T with position i
						minIdx = i;
						minInvokes = 0;
						break;
					}
					int invoke = 0;

					for( IntegerPair twogram : available2Grams.get( i ) ) {
						WrappedInteger count = curr_invokes.get( twogram );
						if( debug ) {
							bw.write( twogram + ":" + count + "\n" );
						}
						if( count != null ) {
							// upper bound
							invoke += count.get();
						}
					}
					if( debug ) {
						bw.write( "count " + i + ":" + invoke + "\n" );
					}
					if( invoke < minInvokes ) {
						minIdx = i;
						minInvokes = invoke;
					}
				}

				Map<IntegerPair, Directory> curr_idx = idx.get( minIdx );

				for( IntegerPair twogram : available2Grams.get( minIdx ) ) {
					Directory dir = curr_idx.get( twogram );
					if( dir == null ) {
						dir = new Directory();
						curr_idx.put( twogram, dir );
					}
					dir.list.add( rec );
				}
				elements += available2Grams.get( minIdx ).size();
				est_cmps += minInvokes;
			}
			bw.close();
		}
		catch( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println( "Bigram retrieval : " + Record.exectime );
		// System.out.println( ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 + "MB used for JoinMinIdx" );
		memlimit_expandedS = (long) ( runtime.freeMemory() * 0.8 );

		stat.add( "Mem_After_JoinMin", ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 );
		stat.add( "Stat_Predicted_Comparisons", est_cmps );
		stat.add( "Stat_JoinMin_Index_Size", elements );
		stat.add( "Stat_Wrapped Integers", WrappedInteger.count );

		for( int i = 0; i < idx.size(); i++ ) {
			if( idx.get( i ).size() != 0 ) {
				// System.out.println( "JoinMin idx " + i + " size: " + idx.get( i ).size() );
				stat.add( String.format( "Stat_JoinMin_IDX%02d", i ), idx.get( i ).size() );
			}
		}
		stepTime.stopAndAdd( stat );
	}

	private void clearJoinMinIndex() {
		for( Map<IntegerPair, Directory> map : idx ) {
			map.clear();
		}
		idx.clear();
	}

	private void buildNaiveIndex() {
		// Build 1-expanded set for every record in R
		int count = 0;
		setR = new HashMap<Record, List<Integer>>();
		for( int i = 0; i < tableT.size(); ++i ) {
			Record rec = tableT.get( i );
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

		StopWatch stepTime = StopWatch.getWatchStarted( "Result_7_1_SearchEquiv_JoinMin_Time" );
		long time1 = System.currentTimeMillis();
		for( Record s : tableS ) {
			appliedRules_sum += searchEquivsByDynamicIndex( s, idx, rslt );
		}
		stat.add( "Stat_Join_AppliedRules Sum", appliedRules_sum );
		System.out.println( "After JoinMin Result: " + rslt.size() );
		stepTime.stopAndAdd( stat );
		time1 = System.currentTimeMillis() - time1;
		clearJoinMinIndex();

		stepTime.resetAndStart( "Result_7_2_Naive Index Building Time" );
		buildNaiveIndex();
		stepTime.stopAndAdd( stat );

		stepTime.resetAndStart( "Result_7_3_SearchEquiv Naive Time" );
		long time2 = System.currentTimeMillis();
		int naiveSearch = 0;
		for( Record s : tableS ) {
			if( s.getEstNumRecords() > joinThreshold ) {
				continue;
			}
			else {
				searchEquivsByNaive1Expansion( s, rslt );
				naiveSearch++;
			}
		}
		stat.add( "Stat_Naive search count", naiveSearch );
		stepTime.stopAndAdd( stat );
		time2 = System.currentTimeMillis() - time2;

		System.out.println( "Stat_Avg applied rules : " + appliedRules_sum + "/" + rslt.size() );
		System.out.println( "SH_T + SL_TH : " + time1 );
		System.out.println( "SL_TL : " + time2 );

		return rslt;
	}

	private int searchEquivsByDynamicIndex( Record s, List<Map<IntegerPair, Directory>> idx, List<IntegerPair> rslt ) {
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

			Map<IntegerPair, Directory> curr_idx = idx.get( i );

			Set<Record> candidates = new HashSet<Record>();
			for( IntegerPair twogram : available2Grams.get( i ) ) {
				Directory tree = curr_idx.get( twogram );

				if( tree == null ) {
					continue;
				}

				for( int j = tree.list.size() - 1; j >= 0; --j ) {
					Record rec = tree.list.get( j );
					if( !is_TH_record && rec.getEstNumRecords() <= joinThreshold ) {
						continue;
					}
					if( StaticFunctions.overlap( rec.getMinLength(), rec.getMaxLength(), range[ 0 ], range[ 1 ] ) ) {
						candidates.add( rec );
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

		for( Record rec : tableT ) {
			strmaxinvsearchrangesum += rec.getMaxInvSearchRange();
			int length = rec.getTokenArray().length;
			++strs;
			strlengthsum += length;
			maxstrlength = Math.max( maxstrlength, length );
		}
		for( Record rec : tableS ) {
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
		Collections.sort( tableT, cmp );
		Collections.sort( tableS, cmp );

		// Reassign ID
		for( int i = 0; i < tableT.size(); ++i ) {
			Record t = tableT.get( i );
			t.setID( i );
		}
		long maxTEstNumRecords = tableT.get( tableT.size() - 1 ).getEstNumRecords();

		for( int i = 0; i < tableS.size(); ++i ) {
			Record s = tableS.get( i );
			s.setID( i );
		}
		long maxSEstNumRecords = tableS.get( tableS.size() - 1 ).getEstNumRecords();

		System.out.println( "Max S expanded size : " + maxSEstNumRecords );
		System.out.println( "Max T expanded size : " + maxTEstNumRecords );
	}

	public void run( double sampleratio ) {
		StopWatch stepTime = StopWatch.getWatchStarted( "Result_2_Preprocess_Total_Time" );
		preprocess( compact, maxIndex, useAutomata );
		stepTime.stopAndAdd( stat );

		// Retrieve statistics
		stepTime.resetAndStart( "Result_3_Statistics_Time" );
		statistics();
		stepTime.stopAndAdd( stat );

		// Estimate constants
		stepTime.resetAndStart( "Result_4_Find_Constants_Time" );
		findConstants( sampleratio );
		stepTime.stopAndAdd( stat );

		stepTime.resetAndStart( "Result_5_JoinMin_Index_Build_Time" );
		try {
			buildJoinMinIndex();
			// checkLongestIndex();
		}
		catch( Exception e ) {
			e.printStackTrace();
		}
		stepTime.stopAndAdd( stat );

		// Modify index to get optimal theta
		stepTime.resetAndStart( "Result_6_Find_Theta_Time" );
		findTheta( Integer.MAX_VALUE );
		stepTime.stopAndAdd( stat );

		stepTime.resetAndStart( "Result_7_Join_Time" );
		Collection<IntegerPair> rslt = join();
		stepTime.stopAndAdd( stat );

		System.out.println( "Result size: " + rslt.size() );
		System.out.println( "Union counter: " + StaticFunctions.union_cmp_counter );

		writeResult( rslt );
	}

	private void findConstants( double sampleratio ) {
		// Sample
		Random rn = new Random( 0 );

		List<Record> sampleTlist = new ArrayList<Record>();
		List<Record> sampleSlist = new ArrayList<Record>();
		for( Record r : tableT ) {
			if( rn.nextDouble() < sampleratio ) {
				sampleTlist.add( r );
			}
		}
		for( Record s : tableS ) {
			if( rn.nextDouble() < sampleratio ) {
				sampleSlist.add( s );
			}
		}
		List<Record> tmpR = tableT;
		List<Record> tmpS = tableS;

		tableT = sampleTlist;
		tableS = sampleSlist;

		System.out.println( sampleTlist.size() + " T records are sampled" );
		System.out.println( sampleSlist.size() + " S records are sampled" );

		stat.add( "Stat_Sample T size", sampleTlist.size() );
		stat.add( "Stat_Sample S size", sampleSlist.size() );

		// Infer alpha and beta
		JoinNaive1 naiveinst = new JoinNaive1( this, stat );
		naiveinst.threshold = 100;
		naiveinst.runWithoutPreprocess( false );
		alpha = naiveinst.alpha;
		beta = naiveinst.beta;

		// Infer gamma, delta and epsilon
		JoinMin_Q joinmininst = new JoinMin_Q( this, stat );
		joinmininst.useAutomata = useAutomata;
		joinmininst.skipChecking = skipChecking;
		joinmininst.maxIndex = maxIndex;
		joinmininst.compact = compact;
		JoinMin_Q.checker = checker;
		joinmininst.outputfile = null;
		try {
			System.out.println( "Joinmininst run" );
			joinmininst.runWithoutPreprocess( false );
			System.out.println( "Joinmininst run done" );
		}
		catch( Exception e ) {
			e.printStackTrace();
		}
		gamma = joinmininst.gamma;
		delta = joinmininst.delta;
		epsilon = joinmininst.epsilon;
		System.out.println( "Bigram computation time : " + Record.exectime );
		Validator.printStats();

		// Restore tables
		tableT = tmpR;
		tableS = tmpS;

		System.out.println( "Alpha : " + alpha );
		System.out.println( "Beta : " + beta );
		System.out.println( "Gamma : " + gamma );
		System.out.println( "Delta : " + delta );
		System.out.println( "Epsilon : " + epsilon );

		stat.add( "Const_Alpha", String.format( "%.2f", alpha ) );
		stat.add( "Const_Beta", String.format( "%.2f", beta ) );
		stat.add( "Const_Gamma", String.format( "%.2f", gamma ) );
		stat.add( "Const_Delta", String.format( "%.2f", delta ) );
		stat.add( "Const_Epsilon", String.format( "%.2f", epsilon ) );
	}

	private void findTheta( int max_theta ) {
		// Find the best threshold
		int best_theta = 0;
		long best_esttime = Long.MAX_VALUE;
		long[] best_esttimes = null;

		// Memory cost for storing expanded tableT
		long memcost = 0;

		// Indicates the minimum indices which have more that 'theta' expanded
		// records
		int sidx = 0;
		int tidx = 0;
		long theta = Math.min( tableT.get( 0 ).getEstNumRecords(), tableS.get( 0 ).getEstNumRecords() );

		// Number of bigrams generated by expanded TL records
		Map<Integer, Map<IntegerPair, WrappedInteger>> TL_invokes = new WYK_HashMap<Integer, Map<IntegerPair, WrappedInteger>>();

		// Prefix sums
		long currSLExpSize = 0;
		long currTLExpSize = 0;
		while( sidx < tableT.size() || tidx < tableS.size() ) {
			if( theta > max_theta ) {
				break;
			}
			long next_theta = Long.MAX_VALUE;
			// Estimate new running time
			// Modify SL_TH_invokes, SL_TH_idx
			while( tidx < tableS.size() ) {
				Record t = tableS.get( tidx++ );
				long expSize = t.getEstNumRecords();
				if( expSize > theta ) {
					next_theta = Math.min( next_theta, expSize );
					break;
				}
				currTLExpSize += expSize;
				List<Set<IntegerPair>> twograms = t.get2Grams();
				for( int i = 0; i < t.getMaxLength(); ++i ) {
					// Frequency count of i-th bigrams of TL records
					Map<IntegerPair, WrappedInteger> curr_invokes = TL_invokes.get( i );
					if( i >= idx.size() ) {
						continue;
					}
					Map<IntegerPair, Directory> curr_idx = idx.get( i );
					if( curr_invokes == null ) {
						curr_invokes = new WYK_HashMap<IntegerPair, WrappedInteger>();
						TL_invokes.put( i, curr_invokes );
					}
					for( IntegerPair curr_twogram : twograms.get( i ) ) {
						// Update TL_invokes
						WrappedInteger count = curr_invokes.get( curr_twogram );
						if( count == null )
							curr_invokes.put( curr_twogram, ONE );
						else if( count == ONE )
							curr_invokes.put( curr_twogram, new WrappedInteger( 2 ) );
						else
							count.increment();

						// Update est_cmps
						Directory dir = curr_idx.get( curr_twogram );
						if( dir == null )
							continue;
						est_cmps -= dir.SHsize;
					}
				}
				for( Set<IntegerPair> set : twograms )
					set.clear();
				twograms.clear();
			}

			// Modify both indexes
			while( sidx < tableT.size() ) {
				Record s = tableT.get( sidx++ );
				long expSize = s.getEstNumRecords();
				if( expSize > theta ) {
					next_theta = Math.min( next_theta, expSize );
					break;
				}
				long expmemsize = s.getEstExpandCost();
				currSLExpSize += expSize;
				// Size for the integer arrays
				memcost += 4 * expmemsize + 16 * expSize;
				// Size for the Record instance
				memcost += RECORD_CLASS_BYTES * expSize;
				// Pointers in the inverted index
				memcost += 8 * expSize;
				// Pointers in the Hashmap (in worst case)
				// Our hashmap filling ratio is 0.5: 24 / 0.5 = 48
				memcost += 48 * expSize;
				if( memcost > memlimit_expandedS ) {
					next_theta = Math.min( next_theta, expSize );
					break;
				}

				// Count the reduced invocation counts
				List<Set<IntegerPair>> twograms = s.get2Grams();
				int min_invokes = Integer.MAX_VALUE;
				int min_index = -1;
				/**
				 * @TODO
				 */
				for( int i = 0; i < s.getMinLength(); ++i ) {
					int sum_invokes = 0;
					for( IntegerPair curr_twogram : twograms.get( i ) ) {
						WrappedInteger count = T_invokes.get( i ).get( curr_twogram );
						if( count != null )
							sum_invokes += count.get();
					}
					if( sum_invokes < min_invokes ) {
						min_invokes = sum_invokes;
						min_index = i;
					}
				}
				Map<IntegerPair, Directory> curr_idx = idx.get( min_index );
				assert ( curr_idx != null );
				for( IntegerPair curr_twogram : twograms.get( min_index ) ) {
					// Update index
					Directory dir = curr_idx.get( curr_twogram );
					++dir.SHsize;
				}
				for( Set<IntegerPair> set : twograms )
					set.clear();
				twograms.clear();
			}
			if( memcost > memlimit_expandedS ) {
				System.out.println( "Memory budget exceeds at " + theta );
				break;
			}

			long[] esttimes = new long[ 4 ];
			esttimes[ 0 ] = (long) ( alpha * currSLExpSize );
			esttimes[ 1 ] = (long) ( beta * currTLExpSize );
			esttimes[ 2 ] = (long) ( epsilon * est_cmps );
			long esttime = esttimes[ 0 ] + esttimes[ 1 ] + esttimes[ 2 ];
			if( esttime < best_esttime ) {
				best_theta = (int) theta;
				best_esttime = esttime;
				best_esttimes = esttimes;
			}
			if( theta == 10 || theta == 30 || theta == 100 || theta == 300 || theta == 1000 || theta == 3000 ) {
				System.out.println( "T=" + theta + " : esttime " + esttime );
				System.out.println( Arrays.toString( esttimes ) );
				System.out.println( "Mem : " + memcost + " / " + memlimit_expandedS );
			}
			theta = next_theta;
		}
		System.out.println( "Best threshold : " + best_theta + " with running time " + best_esttime );
		System.out.println( Arrays.toString( best_esttimes ) );

		stat.addPrimary( "Best Threshold", best_theta );
		stat.add( "Best Estimated Time", best_esttime );

		joinThreshold = best_theta;
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

	@Override
	public String getName() {
		return "JoinHybridOpt_Q";
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
		singleside = params.isSingleside();
		checker = params.getValidator();

		run( params.getSampleRatio() );
		Validator.printStats();
	}
}
