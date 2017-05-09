package snu.kdd.synonym.algorithm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import mine.Record;
import mine.RecordIDComparator;
import snu.kdd.synonym.data.DataInfo;
import snu.kdd.synonym.estimation.SampleEstimate;
import snu.kdd.synonym.tools.JoinMinIndex;
import snu.kdd.synonym.tools.NaiveIndex;
import snu.kdd.synonym.tools.Param;
import snu.kdd.synonym.tools.StatContainer;
import snu.kdd.synonym.tools.StopWatch;
import tools.DEBUG;
import tools.IntegerPair;
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

	SampleEstimate estimate;

	private static final WrappedInteger ONE = new WrappedInteger( 1 );

	private static final int RECORD_CLASS_BYTES = 64;
	private boolean joinMinRequired = true;

	/* private int intarrbytes(int len) {
	 * // Accurate bytes in 64bit machine is:
	 * // ceil(4 * len / 8) * 8 + 16
	 * return len * 4 + 16;
	 * } */

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

	public JoinHybridOpt_Q( String rulefile, String Rfile, String Sfile, String outputfile, DataInfo dataInfo )
			throws IOException {
		super( rulefile, Rfile, Sfile, outputfile, dataInfo );
		idComparator = new RecordIDComparator();
		ruletrie = new RuleTrie( rulelist );
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

	public void run( double sampleratio ) {
		StopWatch stepTime = StopWatch.getWatchStarted( "Result_2_Preprocess_Total_Time" );
		preprocess( compact, maxIndex, useAutomata );
		stepTime.stopAndAdd( stat );

		// Retrieve statistics

		if( DEBUG.JoinHybridON ) {
			stepTime.resetAndStart( "Result_3_Statistics_Time" );
			statistics();
			stepTime.stopAndAdd( stat );
		}

		// Estimate constants
		stepTime.resetAndStart( "Result_4_Find_Constants_Time" );
		findConstants( sampleratio );
		stepTime.stopAndAdd( stat );

		stepTime.resetAndStart( "Result_5_JoinMin_Index_Build_Time" );
		// checkLongestIndex();

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

		// Reassign ID and collect statistics for join naive
		partialExpLengthNaiveIndex = new double[ 4 ];
		partialExpNaiveJoin = new double[ 4 ];

		int currentIdx = 0;
		int nextThreshold = 10;

		for( int i = 0; i < tableSearched.size(); ++i ) {
			Record t = tableSearched.get( i );
			t.setID( i );

			double est = t.getEstNumRecords();
			totalExpNaiveJoin += est;

			while( currentIdx != 3 && est >= nextThreshold ) {
				nextThreshold *= 10;
				currentIdx++;
			}
			partialExpNaiveJoin[ currentIdx ] += est;
		}

		for( int i = 0; i < tableIndexed.size(); ++i ) {
			Record s = tableIndexed.get( i );
			s.setID( i );

			double est = s.getEstNumRecords() * s.getTokenArray().length;
			totalExpLengthNaiveIndex += est;

			while( currentIdx != 3 && est >= nextThreshold ) {
				nextThreshold *= 10;
				currentIdx++;
			}
			partialExpLengthNaiveIndex[ currentIdx ] += est;
		}

		maxSearchedEstNumRecords = tableSearched.get( tableSearched.size() - 1 ).getEstNumRecords();
		maxIndexedEstNumRecords = tableIndexed.get( tableIndexed.size() - 1 ).getEstNumRecords();

		// DEBUG
		if( DEBUG.JoinHybridON ) {
			for( int i = 0; i < 4; i++ ) {
				stat.add( "Preprocess_ExpLength_" + i, partialExpLengthNaiveIndex[ i ] );
				stat.add( "Preprocess_Exp_" + i, partialExpNaiveJoin[ i ] );
			}
			stat.add( "Preprocess_ExpLength_Total", totalExpLengthNaiveIndex );
			stat.add( "Preprocess_Exp_Total", totalExpNaiveJoin );
		}
	}

	private long findThetaRevised( int maxThreshold ) {
		List<Map<QGram, CountEntry>> positionalQCountMap = new ArrayList<Map<QGram, CountEntry>>();

		// count qgrams for each that will be searched
		for( Record rec : tableSearched ) {
			List<List<QGram>> availableQGrams = rec.getQGrams( qSize );
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
						currPositionalCount.put( qgram, count );
					}
					else {
						count.increase( rec.getEstNumRecords() );
					}
				}
			}
		}
		// since both tables are sorted with est num records, the two values are minimum est num records in both tables
		long threshold = 1;

		long bestThreshold = Long.max( maxSearchedEstNumRecords, maxIndexedEstNumRecords );
		double bestEstimatedTime = estimate.alpha * totalExpLengthNaiveIndex + estimate.beta * totalExpNaiveJoin;

		// estimate time if only naive algorithm is used

		double diffExpNaiveJoin = 0;
		double diffExpLengthNaiveIndex = 0;

		int indexedIdx = tableIndexed.size() - 1;
		int searchedIdx = tableSearched.size() - 1;

		for( int thresholdExponent = 0; thresholdExponent < 4; thresholdExponent++ ) {
			threshold = threshold * 10;
			if( threshold > maxThreshold ) {
				System.out.println( "Stop searching due to maxTheta" );
				break;
			}

			double naiveTime;

		}

		return bestThreshold;
	}

	private void findTheta( int max_theta ) {
		long elements = 0;
		est_cmps = 0;
		// Build an index
		// Count Invokes per each (token, loc) pair
		List<Map<QGram, WrappedInteger>> invokes = new ArrayList<Map<QGram, WrappedInteger>>();

		List<Map<QGram, Directory>> idx = new ArrayList<Map<QGram, Directory>>();

		// Actually, tableT
		StopWatch stepTime = StopWatch.getWatchStarted( "Result_5_1_Index Count Time" );

		ArrayList<Integer> countPerPosition = new ArrayList<Integer>();

		for( Record rec : tableSearched ) {
			List<List<QGram>> availableQGrams = rec.getQGrams( qSize );
			int searchmax = Math.min( availableQGrams.size(), maxIndex );

			for( int i = invokes.size(); i < searchmax; i++ ) {
				invokes.add( new WYK_HashMap<QGram, WrappedInteger>() );
				countPerPosition.add( 0 );
			}

			for( int i = 0; i < searchmax; ++i ) {
				Map<QGram, WrappedInteger> curridx_invokes = invokes.get( i );

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

		for( int i = 0; i < countPerPosition.size(); i++ ) {
			stat.add( String.format( "Stat_JoinMin_COUNT%02d", i ), countPerPosition.get( i ) );
		}

		System.out.println( "Bigram retrieval : " + Record.exectime );
		// System.out.println( ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 + "MB used for counting bigrams" );
		stat.add( "Mem_1_After_Counting_Bigrams", ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 );
		stepTime.stopAndAdd( stat );

		stepTime.resetAndStart( "Result_5_2_Indexing Time" );
		// Actually, tableS

		for( Record rec : tableIndexed ) {
			int[] range = rec.getCandidateLengths( rec.size() - 1 );
			int minIdx = -1;
			int minInvokes = Integer.MAX_VALUE;
			int searchmax = Math.min( range[ 0 ], maxIndex );

			List<List<QGram>> availableQGrams = rec.getQGrams( qSize, searchmax );

			for( int i = idx.size(); i < searchmax; i++ ) {
				idx.add( new WYK_HashMap<QGram, Directory>() );
			}

			for( int i = 0; i < searchmax; ++i ) {
				Map<QGram, WrappedInteger> curr_invokes = invokes.get( i );
				if( curr_invokes == null ) {
					// there is no twogram in T with position i
					minIdx = i;
					minInvokes = 0;
					break;
				}
				int invoke = 0;

				for( QGram qgram : availableQGrams.get( i ) ) {
					WrappedInteger count = curr_invokes.get( qgram );

					if( count != null ) {
						// upper bound
						invoke += count.get();
					}
				}

				if( invoke < minInvokes ) {
					minIdx = i;
					minInvokes = invoke;
				}
			}

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

		System.out.println( "Bigram retrieval : " + Record.exectime );
		// System.out.println( ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 + "MB used for JoinMinIdx" );
		memlimit_expandedS = (long) ( runtime.freeMemory() * 0.8 );

		stat.add( "Mem_2_After_JoinMin", ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 );
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

		int tableIndexedSize = tableIndexed.size();
		int tableSearchedSize = tableSearched.size();
		while( sidx < tableIndexedSize || tidx < tableSearchedSize ) {
			if( theta > max_theta ) {
				break;
			}
			long next_theta = Long.MAX_VALUE;

			// Estimate new running time
			// Modify SL_TH_invokes, SL_TH_idx
			while( tidx < tableSearchedSize ) {
				Record t = tableSearched.get( tidx++ );
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
			while( sidx < tableIndexedSize ) {
				Record s = tableIndexed.get( sidx++ );
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
						WrappedInteger count = invokes.get( i ).get( curr_qgram );
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
			esttimes[ 0 ] = (long) ( estimate.alpha * currSLExpSize );
			esttimes[ 1 ] = (long) ( estimate.beta * currTLExpSize );
			esttimes[ 2 ] = (long) ( estimate.epsilon * est_cmps );
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

		if( maxSearchedEstNumRecords < joinThreshold && maxIndexedEstNumRecords < joinThreshold ) {
			joinMinRequired = false;
			// joinThreshold = Integer.max( (int) maxSearchedEstNumRecords, (int) maxIndexedEstNumRecords ) + 1;
		}

		joinThreshold = best_theta;
	}

	private void buildJoinMinIndex() {
		// Build an index
		// Count Invokes per each (token, loc) pair
		joinMinIdx = JoinMinIndex.buildIndex( tableSearched, tableIndexed, maxIndex, qSize, stat, true );
	}

	private void buildNaiveIndex() {
		naiveIndex = NaiveIndex.buildIndex( tableIndexed, joinThreshold / 2, stat, joinThreshold, false );
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
		}
		System.out.println( "After JoinMin Result: " + rslt.size() );
		stepTime.stopAndAdd( stat );

		stepTime.resetAndStart( "Result_7_2_Naive Index Building Time" );
		buildNaiveIndex();
		stepTime.stopAndAdd( stat );

		stepTime.resetAndStart( "Result_7_3_SearchEquiv Naive Time" );

		int naiveSearch = 0;
		for( Record s : tableSearched ) {
			if( s.getEstNumRecords() > joinThreshold ) {
				continue;
			}
			else {
				naiveIndex.joinOneRecord( s, rslt );
				naiveSearch++;
			}
		}
		stat.add( "Stat_Naive search count", naiveSearch );
		stepTime.stopAndAdd( stat );

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

		System.out.println( "Average str length: " + strlengthsum + "/" + strs );
		System.out.println( "Average maxinvsearchrange: " + strmaxinvsearchrangesum + "/" + strs );
		System.out.println( "Maximum str length: " + maxstrlength );
		System.out.println( "Average rhs length: " + rhslengthsum + "/" + rules );
		System.out.println( "Maximum rhs length: " + maxrhslength );
	}

	private void findConstants( double sampleratio ) {
		// Sample
		estimate = new SampleEstimate( tableSearched, tableIndexed, sampleratio );
		estimate.estimateWithSample( stat, this, checker, qSize );
	}

	@Override
	public String getVersion() {
		return "1.1";
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

	public static class CountEntry {
		public int count[];

		CountEntry() {
			// 0 : 1 ~ 10
			// 1 : 11 ~ 100
			// 2 : 101 ~ 1000
			// 3 : 1001 ~ infinity
			count = new int[ 4 ];
		}

		public void increase( long exp ) {
			count[ getIndex( exp ) ]++;
		}

		private int getIndex( long number ) {
			if( number <= 10 ) {
				return 0;
			}
			else if( number <= 100 ) {
				return 1;
			}
			else if( number <= 1000 ) {
				return 2;
			}
			return 3;
		}

	}

}
