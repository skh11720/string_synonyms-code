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
import snu.kdd.synonym.tools.Util;
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

		// if( DEBUG.JoinHybridON ) {
		// stepTime.resetAndStart( "Result_3_Statistics_Time" );
		// statistics();
		// stepTime.stopAndAdd( stat );
		// }

		// Estimate constants
		stepTime.resetAndStart( "Result_4_Find_Constants_Time" );
		findConstants( sampleratio );
		stepTime.stopAndAdd( stat );

		stepTime.resetAndStart( "Result_5_JoinMin_Index_Build_Time" );
		// checkLongestIndex();

		stepTime.stopAndAdd( stat );

		// Modify index to get optimal theta
		stepTime.resetAndStart( "Result_6_Find_Theta_Time" );
		// joinThreshold = findTheta( Integer.MAX_VALUE );
		joinThreshold = findThetaRevised( Integer.MAX_VALUE );
		if( Long.max( maxSearchedEstNumRecords, maxIndexedEstNumRecords ) <= joinThreshold ) {
			joinMinRequired = false;
		}
		stepTime.stopAndAdd( stat );

		stepTime.resetAndStart( "Result_7_Join_Time" );
		Collection<IntegerPair> rslt = join();
		stepTime.stopAndAdd( stat );

		Util.printLog( "Result size: " + rslt.size() );
		Util.printLog( "Union counter: " + StaticFunctions.union_cmp_counter );

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

		currentIdx = 0;
		nextThreshold = 10;
		for( int i = 0; i < tableIndexed.size(); ++i ) {
			Record s = tableIndexed.get( i );
			s.setID( i );

			double est = (double) s.getEstNumRecords() * (double) s.getTokenArray().length;
			totalExpLengthNaiveIndex += est;

			while( currentIdx != 3 && s.getEstNumRecords() >= nextThreshold ) {
				nextThreshold *= 10;
				currentIdx++;
			}
			partialExpLengthNaiveIndex[ currentIdx ] += est;
		}

		maxSearchedEstNumRecords = tableSearched.get( tableSearched.size() - 1 ).getEstNumRecords();
		maxIndexedEstNumRecords = tableIndexed.get( tableIndexed.size() - 1 ).getEstNumRecords();

		if( DEBUG.JoinHybridON ) {
			for( int i = 0; i < 4; i++ ) {
				stat.add( "Preprocess_ExpLength_" + i, partialExpLengthNaiveIndex[ i ] );
				stat.add( "Preprocess_Exp_" + i, partialExpNaiveJoin[ i ] );
			}
			stat.add( "Preprocess_ExpLength_Total", totalExpLengthNaiveIndex );
			stat.add( "Preprocess_Exp_Total", totalExpNaiveJoin );
		}
	}

	private int findThetaRevised( int maxThreshold ) {
		List<Map<QGram, CountEntry>> positionalQCountMap = new ArrayList<Map<QGram, CountEntry>>();

		// count qgrams for each that will be searched
		double searchedTotalSigCount = 0;

		for( Record rec : tableSearched ) {
			List<List<QGram>> availableQGrams = rec.getQGrams( qSize );
			int searchmax = Math.min( availableQGrams.size(), maxIndex );

			for( int i = positionalQCountMap.size(); i < searchmax; i++ ) {
				positionalQCountMap.add( new WYK_HashMap<QGram, CountEntry>() );
			}

			long qgramCount = 0;
			for( int i = 0; i < searchmax; ++i ) {
				Map<QGram, CountEntry> currPositionalCount = positionalQCountMap.get( i );

				List<QGram> positionalQGram = availableQGrams.get( i );
				qgramCount += positionalQGram.size();
				for( QGram qgram : positionalQGram ) {
					CountEntry count = currPositionalCount.get( qgram );

					if( count == null ) {
						count = new CountEntry();
						currPositionalCount.put( qgram, count );
					}

					count.increase( rec.getEstNumRecords() );

				}
			}

			searchedTotalSigCount += qgramCount;
		}

		// since both tables are sorted with est num records, the two values are minimum est num records in both tables
		int threshold = 1;

		long bestThreshold = Long.max( maxSearchedEstNumRecords, maxIndexedEstNumRecords );
		double bestEstimatedTime = estimate.getEstimateNaive( totalExpLengthNaiveIndex, totalExpNaiveJoin );

		if( DEBUG.JoinHybridON ) {
			stat.add( "Est_Theta_Start_Threshold", bestThreshold );
			stat.add( "Est_Theta_3_1_NaiveTime", bestEstimatedTime );
			stat.add( "Est_Theta_3_2_JoinMinTime", 0 );
			stat.add( "Est_Theta_3_3_TotalTime", bestEstimatedTime );

			stat.add( "Const_Beta_JoinTime_3", String.format( "%.2f", totalExpNaiveJoin * estimate.beta ) );
			stat.add( "Const_Beta_TotalExp_3", String.format( "%.2f", totalExpNaiveJoin ) );

			stat.add( "Const_Alpha_IndexTime_3", String.format( "%.2f", totalExpLengthNaiveIndex * estimate.alpha ) );
			stat.add( "Const_Alpha_ExpLength_3", String.format( "%.2f", totalExpLengthNaiveIndex ) );
		}

		int startThresIndex;
		if( bestThreshold > 1000 ) {
			startThresIndex = 2;
			threshold = 1000;
		}
		else if( bestThreshold > 100 ) {
			startThresIndex = 1;
			threshold = 100;
		}
		else if( bestThreshold > 10 ) {
			startThresIndex = 0;
			threshold = 10;
		}
		else {
			startThresIndex = -1;
		}

		// estimate time if only naive algorithm is used

		int indexedIdx = tableIndexed.size() - 1;

		double indexedTotalSigCount = 0;

		double fixedInvokes = 0;

		for( int thresholdExponent = startThresIndex; thresholdExponent >= 0; thresholdExponent-- ) {

			// estimate naive time
			double diffExpNaiveJoin = partialExpNaiveJoin[ thresholdExponent ];
			double diffExpLengthNaiveIndex = partialExpLengthNaiveIndex[ thresholdExponent ];

			for( int i = 0; i < thresholdExponent; i++ ) {
				diffExpNaiveJoin += partialExpNaiveJoin[ i ];
				diffExpLengthNaiveIndex += partialExpLengthNaiveIndex[ i ];
			}

			double naiveTime = estimate.getEstimateNaive( diffExpLengthNaiveIndex, diffExpNaiveJoin );

			if( DEBUG.JoinHybridON ) {
				stat.add( "Const_Beta_JoinTime_" + thresholdExponent, String.format( "%.2f", diffExpNaiveJoin * estimate.beta ) );
				stat.add( "Const_Beta_TotalExp_" + thresholdExponent, String.format( "%.2f", diffExpNaiveJoin ) );

				stat.add( "Const_Alpha_IndexTime_" + thresholdExponent,
						String.format( "%.2f", diffExpLengthNaiveIndex * estimate.alpha ) );
				stat.add( "Const_Alpha_ExpLength_" + thresholdExponent, String.format( "%.2f", diffExpLengthNaiveIndex ) );
			}

			// estimate joinmin time

			// process records with large expanded sizes
			int recordIdx = indexedIdx;

			for( ; recordIdx >= 0; recordIdx-- ) {
				Record rec = tableIndexed.get( recordIdx );

				if( rec.getEstNumRecords() <= threshold ) {
					break;
				}

				int[] range = rec.getCandidateLengths( rec.size() - 1 );
				int searchmax = Math.min( range[ 0 ], positionalQCountMap.size() );

				List<List<QGram>> availableQGrams = rec.getQGrams( qSize, searchmax );
				if( thresholdExponent == startThresIndex ) {
					for( List<QGram> set : availableQGrams ) {
						indexedTotalSigCount += set.size();
					}
				}

				int minIdx = 0;
				double minInvokes = Double.MAX_VALUE;

				for( int i = 0; i < searchmax; ++i ) {
					if( availableQGrams.get( i ).isEmpty() ) {
						continue;
					}

					// There is no invocation count: this is the minimum point
					if( i >= positionalQCountMap.size() ) {
						minIdx = i;
						minInvokes = 0;
						break;
					}

					Map<QGram, CountEntry> curridx_invokes = positionalQCountMap.get( i );
					if( curridx_invokes.size() == 0 ) {
						minIdx = i;
						minInvokes = 0;
						break;
					}

					int invoke = 0;

					for( QGram qgram : availableQGrams.get( i ) ) {
						CountEntry count = curridx_invokes.get( qgram );
						if( count != null ) {
							// upper bound
							invoke += count.total;
						}
					}
					if( invoke < minInvokes ) {
						minIdx = i;
						minInvokes = invoke;
					}
				}

				fixedInvokes += minInvokes;
			}

			indexedIdx = recordIdx;

			double variableInvokes = 0;
			for( ; recordIdx >= 0; recordIdx-- ) {
				Record rec = tableIndexed.get( recordIdx );

				if( rec.getEstNumRecords() <= threshold ) {
					break;
				}

				int[] range = rec.getCandidateLengths( rec.size() - 1 );
				int searchmax = Math.min( range[ 0 ], positionalQCountMap.size() );

				List<List<QGram>> availableQGrams = rec.getQGrams( qSize, searchmax );
				if( thresholdExponent == startThresIndex ) {
					for( List<QGram> set : availableQGrams ) {
						indexedTotalSigCount += set.size();
					}
				}

				int minIdx = 0;
				double minInvokes = Double.MAX_VALUE;

				for( int i = 0; i < searchmax; ++i ) {
					if( availableQGrams.get( i ).isEmpty() ) {
						continue;
					}

					// There is no invocation count: this is the minimum point
					if( i >= positionalQCountMap.size() ) {
						minIdx = i;
						minInvokes = 0;
						break;
					}

					Map<QGram, CountEntry> curridx_invokes = positionalQCountMap.get( i );
					if( curridx_invokes.size() == 0 ) {
						minIdx = i;
						minInvokes = 0;
						break;
					}

					int invoke = 0;

					for( QGram qgram : availableQGrams.get( i ) ) {
						CountEntry count = curridx_invokes.get( qgram );
						if( count != null ) {
							// upper bound
							for( int c = thresholdExponent + 1; c < 4; c++ ) {
								invoke += count.count[ c ];
							}
						}
					}
					if( invoke < minInvokes ) {
						minIdx = i;
						minInvokes = invoke;
					}
				}
				variableInvokes += minInvokes;
			}

			double joinminTime = estimate.getEstimateJoinMin( searchedTotalSigCount, indexedTotalSigCount,
					fixedInvokes + variableInvokes );
			double totalTime = naiveTime + joinminTime;

			if( DEBUG.JoinHybridON ) {
				stat.add( "Est_Theta_" + thresholdExponent + "_1_NaiveTime", naiveTime );
				stat.add( "Est_Theta_" + thresholdExponent + "_2_JoinMinTime", joinminTime );
				stat.add( "Est_Theta_" + thresholdExponent + "_3_TotalTime", totalTime );
			}

			if( bestEstimatedTime > totalTime ) {
				bestEstimatedTime = totalTime;
				bestThreshold = threshold;
			}

			threshold = threshold / 10;
		}

		stat.add( "Auto_Best_Threshold", bestThreshold );

		if( bestThreshold > Integer.MAX_VALUE ) {
			return Integer.MAX_VALUE;
		}

		return (int) bestThreshold;
	}

	@Deprecated
	private int findTheta( int max_theta ) {
		long elements = 0;
		est_cmps = 0;
		// Build an index
		// Count Invokes per each (token, loc) pair
		List<Map<QGram, WrappedInteger>> invokes = new ArrayList<Map<QGram, WrappedInteger>>();

		List<Map<QGram, Directory>> idx = new ArrayList<Map<QGram, Directory>>();

		// Actually, tableT
		StopWatch stepTime = null;

		if( DEBUG.JoinHybridON ) {
			stepTime = StopWatch.getWatchStarted( "Result_5_1_Index Count Time" );
		}

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

		if( DEBUG.JoinHybridON ) {
			for( int i = 0; i < countPerPosition.size(); i++ ) {
				stat.add( String.format( "Stat_JoinMin_COUNT%02d", i ), countPerPosition.get( i ) );
			}
		}

		if( DEBUG.JoinHybridON ) {
			// Util.printLog( ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 + "MB used for counting bigrams" );
			stat.add( "Mem_1_After_Counting_Bigrams", ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 );
			stepTime.stopAndAdd( stat );

			stepTime.resetAndStart( "Result_5_2_Indexing Time" );
		}

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

		// Util.printLog( ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 + "MB used for JoinMinIdx" );
		memlimit_expandedS = (long) ( runtime.freeMemory() * 0.8 );

		if( DEBUG.JoinHybridON ) {
			stat.add( "Mem_2_After_JoinMin", ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 );
			stat.add( "Stat_Predicted_Comparisons", est_cmps );
			stat.add( "Stat_JoinMin_Index_Size", elements );
			stat.add( "Stat_Wrapped Integers", WrappedInteger.count );

			for( int i = 0; i < idx.size(); i++ ) {
				if( idx.get( i ).size() != 0 ) {
					// Util.printLog( "JoinMin idx " + i + " size: " + idx.get( i ).size() );
					stat.add( String.format( "Stat_JoinMin_IDX%02d", i ), idx.get( i ).size() );
				}
			}

			stepTime.stopAndAdd( stat );
		}

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
				Util.printLog( "Memory budget exceeds at " + theta );
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
				Util.printLog( "T=" + theta + " : esttime " + esttime );
				Util.printLog( Arrays.toString( esttimes ) );
				Util.printLog( "Mem : " + memcost + " / " + memlimit_expandedS );
			}
			theta = next_theta;
		}
		Util.printLog( "Best threshold : " + best_theta + " with running time " + best_esttime );
		Util.printLog( Arrays.toString( best_esttimes ) );

		stat.addPrimary( "Auto_Best_Threshold", best_theta );
		stat.add( "Auto_Best_Estimated_Time", best_esttime );

		if( maxSearchedEstNumRecords < joinThreshold && maxIndexedEstNumRecords < joinThreshold ) {
			joinMinRequired = false;
			// joinThreshold = Integer.max( (int) maxSearchedEstNumRecords, (int) maxIndexedEstNumRecords ) + 1;
		}

		return best_theta;
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

		if( DEBUG.JoinHybridON ) {
			if( joinMinRequired ) {
				stat.add( "Const_Gamma_Actual", joinMinIdx.gamma );
				stat.add( "Const_Delta_Actual", joinMinIdx.delta );
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
			stat.add( "Const_Epsilon_Predict_Actual", String.format( "%.2f", joinMinIdx.predictCount ) );
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
		estimate = new SampleEstimate( tableSearched, tableIndexed, sampleratio );
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

	public static class CountEntry {
		public int count[];
		public int total;

		CountEntry() {
			// 0 : 1 ~ 10
			// 1 : 11 ~ 100
			// 2 : 101 ~ 1000
			// 3 : 1001 ~ infinity
			count = new int[ 4 ];
		}

		public void increase( long exp ) {
			count[ getIndex( exp ) ]++;
			total++;
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
