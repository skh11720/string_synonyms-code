package snu.kdd.synonym.algorithm;

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
import tools.LongIntPair;
import tools.QGram;
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

	private int qSize = -1;

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
	private static final CountEntry ZERO_ONE = new CountEntry( 0, 1 );
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

	class IndexEntry {
		List<Record> smallList;
		List<Record> largeList;

		IndexEntry() {
			smallList = new ArrayList<Record>();
			largeList = new ArrayList<Record>();
		}
	}

	public static class CountEntry {
		public int smallListSize;
		public int largeListSize;

		CountEntry() {
			smallListSize = 0;
			largeListSize = 0;
		}

		CountEntry( int small, int large ) {
			smallListSize = small;
			largeListSize = large;
		}

		void increaseLarge() {
			largeListSize++;
		}

		void fromLargeToSmall() {
			largeListSize--;
			smallListSize++;
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
	List<Map<QGram, Directory>> idx;
	List<Map<QGram, WrappedInteger>> T_invokes;

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
			Record t = tableSearched.get( i );
			t.setID( i );
		}
		long maxTEstNumRecords = tableSearched.get( tableSearched.size() - 1 ).getEstNumRecords();

		for( int i = 0; i < tableIndexed.size(); ++i ) {
			Record s = tableIndexed.get( i );
			s.setID( i );
		}
		long maxSEstNumRecords = tableIndexed.get( tableIndexed.size() - 1 ).getEstNumRecords();

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

	private long findThetaRevised( int maxThreshold, int q ) {
		List<Map<QGram, CountEntry>> positionalQCountMap = new ArrayList<Map<QGram, CountEntry>>();

		// count qgrams for each that will be searched
		for( Record rec : tableSearched ) {
			List<List<QGram>> availableQGrams = rec.getQGrams( q );
			int searchmax = Math.min( availableQGrams.size(), maxIndex );

			for( int i = positionalQCountMap.size(); i < searchmax; i++ ) {
				positionalQCountMap.add( new WYK_HashMap<QGram, CountEntry>() );
			}

			for( int i = 0; i < searchmax; ++i ) {
				Map<QGram, CountEntry> currPositionalCount = positionalQCountMap.get( i );

				List<QGram> positionalQGram = availableQGrams.get( i );
				for( QGram qgram : positionalQGram ) {
					CountEntry count = currPositionalCount.get( qgram );

					if( count == null ) {
						currPositionalCount.put( qgram, ZERO_ONE );
					}
					else if( count == ZERO_ONE ) {
						count = new CountEntry( 0, 2 );
						currPositionalCount.put( qgram, count );
					}
					else {
						count.increaseLarge();
					}
				}
			}
		}

		long bestThreshold = 0;
		double bestEstimatedTime = Double.MAX_VALUE;

		// TODO : compute estimated time of JoinMin only

		// since both tables are sorted with est num records, the two values are minimum est num records in both tables
		long threshold = Math.min( tableSearched.get( 0 ).getEstNumRecords(), tableIndexed.get( 0 ).getEstNumRecords() );

		int idSearched = 0;
		int idIndexed = 0;
		long naiveExpSize = 0;

		while( idSearched < tableSearched.size() ) {
			if( threshold > maxThreshold ) {
				System.out.println( "Stop searching due to maxTheta" );
				break;
			}

			// iterate through tableSearched
			while( idSearched < tableSearched.size() ) {
				Record s = tableSearched.get( idSearched );
				long expSize = s.getEstNumRecords();

				if( expSize > threshold ) {
					break;
				}

				// update count for JoinMin
				List<List<QGram>> availableQGrams = s.getQGrams( q, Integer.MAX_VALUE );

				for( int i = 0; i < availableQGrams.size(); i++ ) {
					List<QGram> qgrams = availableQGrams.get( i );
					Map<QGram, CountEntry> currPositionalCount = positionalQCountMap.get( i );
					for( QGram qg : qgrams ) {
						CountEntry entry = currPositionalCount.get( qg );
						entry.fromLargeToSmall();
					}
				}

				// update count for JoinNaive
				naiveExpSize += expSize;
			}

			// iterate through tableIndexed to count candidate when join two tables
			long joinMinCandidateCount = 0;
			while( idIndexed < tableIndexed.size() ) {
				Record t = tableIndexed.get( idIndexed );
				long expSize = t.getEstNumRecords();

				if( expSize > threshold ) {
					break;
				}

				LongIntPair result = t.getMinimumIndexSize( positionalQCountMap, threshold, q );
				joinMinCandidateCount += result.l;
			}

			double estimatedExecutionTime = getEstimatedTime( naiveExpSize, joinMinCandidateCount );

			if( bestEstimatedTime > estimatedExecutionTime ) {
				bestEstimatedTime = estimatedExecutionTime;
				bestThreshold = threshold;
			}
		}

		return bestThreshold;
	}

	private double getEstimatedTime( long naiveExpSize, long joinMinCandidateCount ) {
		return 0;
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
		long theta = Math.min( tableSearched.get( 0 ).getEstNumRecords(), tableIndexed.get( 0 ).getEstNumRecords() );

		// Number of bigrams generated by expanded TL records
		Map<Integer, Map<QGram, WrappedInteger>> TL_invokes = new WYK_HashMap<Integer, Map<QGram, WrappedInteger>>();

		// Prefix sums
		long currSLExpSize = 0;
		long currTLExpSize = 0;
		while( sidx < tableSearched.size() || tidx < tableIndexed.size() ) {
			if( theta > max_theta ) {
				break;
			}
			long next_theta = Long.MAX_VALUE;

			// Estimate new running time
			// Modify SL_TH_invokes, SL_TH_idx
			while( tidx < tableIndexed.size() ) {
				Record t = tableIndexed.get( tidx++ );
				long expSize = t.getEstNumRecords();

				if( expSize > theta ) {
					next_theta = Math.min( next_theta, expSize );
					break;
				}

				currTLExpSize += expSize;
				List<List<QGram>> qgrams = t.getQGrams( qSize );

				int searchRange = qgrams.size();

				for( int i = 0; i < searchRange; ++i ) {
					// Frequency count of i-th bigrams of TL records

					if( i >= idx.size() ) {
						continue;
					}

					Map<QGram, WrappedInteger> curr_invokes = TL_invokes.get( i );
					if( curr_invokes == null ) {
						curr_invokes = new WYK_HashMap<QGram, WrappedInteger>();
						TL_invokes.put( i, curr_invokes );
					}

					Map<QGram, Directory> curr_idx = idx.get( i );

					for( QGram curr_qgram : qgrams.get( i ) ) {
						// Update TL_invokes
						WrappedInteger count = curr_invokes.get( curr_qgram );
						if( count == null ) {
							curr_invokes.put( curr_qgram, ONE );
						}
						else if( count == ONE ) {
							curr_invokes.put( curr_qgram, new WrappedInteger( 2 ) );
						}
						else {
							count.increment();
						}

						// Update est_cmps
						Directory dir = curr_idx.get( curr_qgram );
						if( dir == null ) {
							continue;
						}
						est_cmps -= dir.SHsize;
					}
				}
				for( List<QGram> set : qgrams ) {
					set.clear();
				}
				qgrams.clear();
			}

			// Modify both indexes
			while( sidx < tableSearched.size() ) {
				Record s = tableSearched.get( sidx++ );
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
				List<List<QGram>> qgrams = s.getQGrams( qSize );
				int min_invokes = Integer.MAX_VALUE;
				int min_index = -1;
				/**
				 * @TODO
				 */
				for( int i = 0; i < s.getMinLength(); ++i ) {
					int sum_invokes = 0;
					for( QGram curr_qgram : qgrams.get( i ) ) {
						WrappedInteger count = T_invokes.get( i ).get( curr_qgram );
						if( count != null )
							sum_invokes += count.get();
					}
					if( sum_invokes < min_invokes ) {
						min_invokes = sum_invokes;
						min_index = i;
					}
				}
				Map<QGram, Directory> curr_idx = idx.get( min_index );
				assert ( curr_idx != null );
				for( QGram curr_twogram : qgrams.get( min_index ) ) {
					// Update index
					Directory dir = curr_idx.get( curr_twogram );
					++dir.SHsize;
				}
				for( List<QGram> set : qgrams ) {
					set.clear();
				}
				qgrams.clear();
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

		stat.addPrimary( "Auto_Best_Threshold", best_theta );
		stat.add( "Auto_Best_Estimated_Time", best_esttime );

		joinThreshold = best_theta;
	}

	private void buildJoinMinIndex() {
		Runtime runtime = Runtime.getRuntime();

		long elements = 0;
		est_cmps = 0;
		// Build an index
		// Count Invokes per each (token, loc) pair
		T_invokes = new ArrayList<Map<QGram, WrappedInteger>>();
		int invokesInitialized = 0;
		idx = new ArrayList<Map<QGram, Directory>>();

		// Actually, tableT
		StopWatch stepTime = StopWatch.getWatchStarted( "Result_5_1_Index Count Time" );

		ArrayList<Integer> countPerPosition = new ArrayList<Integer>();

		for( Record rec : tableIndexed ) {
			List<List<QGram>> availableQGrams = rec.getQGrams( qSize );
			int searchmax = Math.min( availableQGrams.size(), maxIndex );

			for( int i = invokesInitialized; i < searchmax; i++ ) {
				T_invokes.add( new WYK_HashMap<QGram, WrappedInteger>() );
				countPerPosition.add( 0 );
			}
			if( invokesInitialized < searchmax ) {
				invokesInitialized = searchmax;
			}

			for( int i = 0; i < searchmax; ++i ) {
				Map<QGram, WrappedInteger> curridx_invokes = T_invokes.get( i );

				List<QGram> available = availableQGrams.get( i );
				for( QGram qgram : available ) {
					WrappedInteger count = curridx_invokes.get( qgram );
					if( count == null ) {
						curridx_invokes.put( qgram, ONE );
					}
					else if( count == ONE ) {
						count = new WrappedInteger( 2 );
						curridx_invokes.put( qgram, count );
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
		// {
		// try {
		// for( int i = 0; i < T_invokes.size(); i++ ) {
		// BufferedWriter bw = new BufferedWriter( new FileWriter( "DEBUG_T_invokes" + i + ".txt" ) );
		// for( Entry<IntegerPair, WrappedInteger> entry : T_invokes.get( i ).entrySet() ) {
		// bw.write( entry.getKey() + " " + entry.getValue() + "\n" );
		// }
		// bw.close();
		// }
		// }
		// catch( Exception e ) {
		// e.printStackTrace();
		// }
		// }

		for( int i = 0; i < countPerPosition.size(); i++ ) {
			stat.add( String.format( "Stat_JoinMin_COUNT%02d", i ), countPerPosition.get( i ) );
		}

		System.out.println( "Bigram retrieval : " + Record.exectime );
		// System.out.println( ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 + "MB used for counting bigrams" );
		stat.add( "Mem_After_Counting_Bigrams", ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 );
		stepTime.stopAndAdd( stat );

		stepTime.resetAndStart( "Result_5_2_Indexing Time" );
		// Actually, tableS

		// TODO DEBUG
		// try {
		// BufferedWriter bw = new BufferedWriter( new FileWriter( "MinIndex.txt" ) );
		// boolean debug = false;

		for( Record rec : tableSearched ) {

			// if( rec.getID() < 100 ) {
			// debug = true;
			// bw.write( "Item " + rec.toString() );
			//
			// }
			// else {
			// debug = false;
			// }

			int[] range = rec.getCandidateLengths( rec.size() - 1 );
			int minIdx = -1;
			int minInvokes = Integer.MAX_VALUE;
			int searchmax = Math.min( range[ 0 ], maxIndex );

			List<List<QGram>> availableQGrams = rec.getQGrams( qSize, searchmax );

			for( int i = idx.size(); i < searchmax; i++ ) {
				idx.add( new WYK_HashMap<QGram, Directory>() );
			}

			// if( debug ) {
			// bw.write( "Search max : " + searchmax + "\n" );
			// }

			for( int i = 0; i < searchmax; ++i ) {
				Map<QGram, WrappedInteger> curr_invokes = T_invokes.get( i );
				if( curr_invokes == null ) {
					// there is no twogram in T with position i
					minIdx = i;
					minInvokes = 0;
					break;
				}
				int invoke = 0;

				for( QGram qgram : availableQGrams.get( i ) ) {
					WrappedInteger count = curr_invokes.get( qgram );

					// if( debug ) {
					// if( count != null ) {
					// bw.write( twogram + ":" + count + "\n" );
					// }
					// else {
					// bw.write( twogram + ":null\n" );
					// }
					// }

					if( count != null ) {
						// upper bound
						invoke += count.get();
					}
				}
				// if( debug ) {
				// bw.write( "count " + i + ":" + invoke + "\n" );
				// }
				if( invoke < minInvokes ) {
					minIdx = i;
					minInvokes = invoke;
				}
			}

			// if( debug ) {
			// bw.write( "min " + minIdx + " " + minInvokes + "\n" );
			// }

			Map<QGram, Directory> curr_idx = idx.get( minIdx );

			for( QGram qgram : availableQGrams.get( minIdx ) ) {
				Directory dir = curr_idx.get( qgram );
				if( dir == null ) {
					dir = new Directory();
					curr_idx.put( qgram, dir );
				}
				dir.list.add( rec );
			}
			elements += availableQGrams.get( minIdx ).size();
			est_cmps += minInvokes;
		}
		// bw.close();
		// }catch(
		//
		// IOException e)
		// {
		// e.printStackTrace();
		// }

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

	private void buildNaiveIndex() {
		// Build 1-expanded set for every record in R
		int count = 0;
		setR = new HashMap<Record, List<Integer>>();
		for( int i = 0; i < tableSearched.size(); ++i ) {
			Record rec = tableSearched.get( i );
			assert ( rec != null );
			if( rec.getEstNumRecords() > joinThreshold )
				continue;
			List<Record> expanded = rec.expandAll();
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

	private void clearJoinMinIndex() {
		for( Map<QGram, Directory> map : idx ) {
			map.clear();
		}
		idx.clear();
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
		for( Record s : tableIndexed ) {
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
		for( Record s : tableIndexed ) {
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

	private int searchEquivsByDynamicIndex( Record s, List<Map<QGram, Directory>> idx, List<IntegerPair> rslt ) {
		boolean is_TH_record = s.getEstNumRecords() > joinThreshold;

		int appliedRules_sum = 0;
		int idxSize = idx.size();
		List<List<QGram>> availableQGrams = s.getQGrams( qSize, idxSize );
		int[] range = s.getCandidateLengths( s.size() - 1 );
		int searchmax = Math.min( availableQGrams.size(), maxIndex );
		for( int i = 0; i < searchmax; ++i ) {
			if( i >= idx.size() ) {
				break;
			}

			Map<QGram, Directory> curr_idx = idx.get( i );

			Set<Record> candidates = new HashSet<Record>();
			for( QGram twogram : availableQGrams.get( i ) ) {
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
		ArrayList<Record> expanded = s.expandAll();
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

	private void findConstants( double sampleratio ) {
		// Sample
		Random rn = new Random( 0 );

		List<Record> sampleTlist = new ArrayList<Record>();
		List<Record> sampleSlist = new ArrayList<Record>();
		for( Record r : tableSearched ) {
			if( rn.nextDouble() < sampleratio ) {
				sampleTlist.add( r );
			}
		}
		for( Record s : tableIndexed ) {
			if( rn.nextDouble() < sampleratio ) {
				sampleSlist.add( s );
			}
		}
		List<Record> tmpR = tableSearched;
		List<Record> tmpS = tableIndexed;

		tableSearched = sampleTlist;
		tableIndexed = sampleSlist;

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
		joinmininst.qSize = qSize;
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
		tableSearched = tmpR;
		tableIndexed = tmpS;

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

	@Override
	public String getVersion() {
		return "1.1";
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
		qSize = params.getQGramSize();

		run( params.getSampleRatio() );
		Validator.printStats();
	}
}
