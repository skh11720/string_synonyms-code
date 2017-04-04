package mine.hybrid;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;

import mine.JoinH2GramNoIntervalTree;
import mine.Naive1;
import mine.Record;
import mine.RecordIDComparator;
import snu.kdd.synonym.tools.StatContainer;
import tools.Algorithm;
import tools.IntegerPair;
import tools.Parameters;
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
 * It first build JoinMin(JoinH2Gram) index and then change threshold / modify
 * index in order to find the best execution time.
 */
public class Hybrid2GramWithOptTheta2 extends Algorithm {
	static boolean useAutomata = true;
	static boolean skipChecking = false;
	static int maxIndex = Integer.MAX_VALUE;
	static boolean compact = false;
	static boolean singleside;
	static Validator checker;

	RecordIDComparator idComparator;
	RuleTrie ruletrie;

	static String outputfile;

	int joinThreshold = 0;

	double alpha;
	double beta;
	double gamma;
	double delta;
	double epsilon;

	private static final WrappedInteger ONE = new WrappedInteger( 1 );
	private static final int RECORD_CLASS_BYTES = 64;

	/* private int intarrbytes(int len) {
	 * // Accurate bytes in 64bit machine is:
	 * // ceil(4 * len / 8) * 8 + 16
	 * return len * 4 + 16;
	 * } */

	/**
	 * Key: (token, index) pair<br/>
	 * Value: (min, max, record) triple
	 */
	/**
	 * Index of the records in S for the strings in T which has less or equal to
	 * 'threshold' 1-expandable strings
	 * (SL x TH)
	 */
	Map<Integer, Map<IntegerPair, List<Record>>> SL_TH_idx;
	/**
	 * Index of the records in S for the strings in T which has more than
	 * 'threshold' 1-expandable strings
	 * (SH x T)
	 */
	Map<Integer, Map<IntegerPair, List<Record>>> SH_T_idx;

	// Frequency counts
	Map<Integer, Map<IntegerPair, WrappedInteger>> SH_T_invokes;
	Map<Integer, Map<IntegerPair, WrappedInteger>> SL_TH_invokes;
	// Frequency counts
	Map<Integer, Map<IntegerPair, WrappedInteger>> SH_T_idx_count;
	Map<Integer, Map<IntegerPair, WrappedInteger>> SL_TH_idx_count;
	/**
	 * List of 1-expandable strings
	 */
	Map<Record, List<Integer>> setR;
	/**
	 * Estimated number of comparisons
	 */
	long est_SH_T_cmps;
	long est_SL_TH_cmps;

	long memlimit_expandedS;

	protected Hybrid2GramWithOptTheta2( String rulefile, String Rfile, String Sfile ) throws IOException {
		super( rulefile, Rfile, Sfile );
		idComparator = new RecordIDComparator();
		ruletrie = new RuleTrie( rulelist );
	}

	private void buildJoinMinIndex() throws Exception {
		Runtime runtime = Runtime.getRuntime();

		long SH_T_elements = 0;
		long SL_TH_elements = 0;
		est_SH_T_cmps = 0;
		est_SL_TH_cmps = 0;
		// Build an index
		// Count Invokes per each (token, loc) pair
		SH_T_invokes = new WYK_HashMap<Integer, Map<IntegerPair, WrappedInteger>>();
		SL_TH_invokes = new WYK_HashMap<Integer, Map<IntegerPair, WrappedInteger>>();
		// Count records in each index
		SH_T_idx_count = new WYK_HashMap<Integer, Map<IntegerPair, WrappedInteger>>();
		SL_TH_idx_count = new WYK_HashMap<Integer, Map<IntegerPair, WrappedInteger>>();
		// Actually, tableT
		for( Record rec : tableS ) {
			// long prev = Record.exectime;
			List<Set<IntegerPair>> available2Grams = rec.get2Grams();
			// int sigs = 0;
			// for (Set<IntegerPair> set : available2Grams)
			// sigs += set.size();
			// bw.write(rec.toString() + " : " + sigs + " sigs / "
			// + (Record.exectime - prev) + " ns\n");
			int searchmax = Math.min( available2Grams.size(), maxIndex );
			// Every record is SH/TH record at the beginning
			boolean is_TH_Record = true;// rec.getEstNumRecords() > joinThreshold
			for( int i = 0; i < searchmax; ++i ) {
				Map<IntegerPair, WrappedInteger> curr_SH_T_invokes = SH_T_invokes.get( i );
				Map<IntegerPair, WrappedInteger> curr_SL_TH_invokes = SL_TH_invokes.get( i );
				if( curr_SH_T_invokes == null ) {
					curr_SH_T_invokes = new WYK_HashMap<IntegerPair, WrappedInteger>();
					curr_SL_TH_invokes = new WYK_HashMap<IntegerPair, WrappedInteger>();
					SH_T_invokes.put( i, curr_SH_T_invokes );
					SL_TH_invokes.put( i, curr_SL_TH_invokes );
				}
				for( IntegerPair twogram : available2Grams.get( i ) ) {
					WrappedInteger count = curr_SH_T_invokes.get( twogram );
					if( count == null ) {
						curr_SH_T_invokes.put( twogram, ONE );
					}
					else if( count == ONE ) {
						count = new WrappedInteger( 2 );
						curr_SH_T_invokes.put( twogram, count );
					}
					else
						count.increment();
					if( is_TH_Record ) {
						count = curr_SL_TH_invokes.get( twogram );
						if( count == null ) {
							curr_SL_TH_invokes.put( twogram, ONE );
						}
						else if( count == ONE ) {
							count = new WrappedInteger( 2 );
							curr_SL_TH_invokes.put( twogram, count );
						}
						else
							count.increment();
					}
				}
			}
		}

		// bw.close();
		System.out.println( "Bigram retrieval : " + Record.exectime );
		System.out.println( ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 + "MB used" );

		// Build an index for the strings in S which has more than 'threshold'
		// 1-expandable strings
		// SH_T_idx = new WYK_HashMap<Integer, Map<IntegerPair,
		// List<IntIntRecordTriple>>>();
		// SL_TH_idx = new WYK_HashMap<Integer, Map<IntegerPair,
		// List<IntIntRecordTriple>>>();

		// Actually, tableS
		for( Record rec : tableT ) {
			List<Set<IntegerPair>> available2Grams = rec.get2Grams();
			int[] range = rec.getCandidateLengths( rec.size() - 1 );
			int minIdx = -1;
			int minInvokes = Integer.MAX_VALUE;
			int searchmax = Math.min( range[ 0 ], maxIndex );
			boolean is_SH_record = true;// rec.getEstNumRecords() > joinThreshold;
			int[] invokearr = new int[ searchmax ];

			Map<Integer, Map<IntegerPair, WrappedInteger>> invokes = is_SH_record ? SH_T_invokes : SL_TH_invokes;
			Map<Integer, Map<IntegerPair, WrappedInteger>> idx_count = is_SH_record ? SH_T_idx_count : SL_TH_idx_count;

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

			// if (minInvokes > 400) {
			// System.out.println(rec.toString() + " : " +
			// Arrays.toString(invokearr));
			// }

			Map<IntegerPair, WrappedInteger> curr_idx_count = idx_count.get( minIdx );
			if( curr_idx_count == null ) {
				curr_idx_count = new WYK_HashMap<IntegerPair, WrappedInteger>();
				idx_count.put( minIdx, curr_idx_count );
			}
			for( IntegerPair twogram : available2Grams.get( minIdx ) ) {
				WrappedInteger count = curr_idx_count.get( twogram );
				if( count == null ) {
					curr_idx_count.put( twogram, ONE );
				}
				else if( count == ONE ) {
					count = new WrappedInteger( 2 );
					curr_idx_count.put( twogram, count );
				}
				else
					count.increment();
			}
			int elements = available2Grams.get( minIdx ).size();
			if( is_SH_record ) {
				est_SH_T_cmps += minInvokes;
				SH_T_elements += elements;
			}
			else {
				est_SL_TH_cmps += minInvokes;
				SL_TH_elements += elements;
			}
		}
		System.out.println( "Bigram retrieval : " + Record.exectime );
		System.out.println( ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 + "MB used" );
		memlimit_expandedS = (long) ( runtime.freeMemory() * 0.8 );

		System.out.println( "SH_T predict : " + est_SH_T_cmps );
		System.out.println( "SH_T idx size : " + SH_T_elements );
		System.out.println( "SL_TH predict : " + est_SL_TH_cmps );
		System.out.println( "SL_TH idx size : " + SL_TH_elements );
		System.out.println( WrappedInteger.count + " Wrapped Integers" );
		for( Entry<Integer, Map<IntegerPair, WrappedInteger>> e : SH_T_idx_count.entrySet() ) {
			System.out.println( e.getKey() + " : " + e.getValue().size() );
		}
	}

	@SuppressWarnings( "unused" )
	private void checkLongestIndex() {
		Comparator<Entry<IntegerPair, WrappedInteger>> cmp = new Comparator<Entry<IntegerPair, WrappedInteger>>() {
			@Override
			public int compare( Entry<IntegerPair, WrappedInteger> o1, Entry<IntegerPair, WrappedInteger> o2 ) {
				return Integer.compare( o1.getValue().get(), o2.getValue().get() );
			}
		};

		PriorityQueue<Entry<IntegerPair, WrappedInteger>> pq = new PriorityQueue<Entry<IntegerPair, WrappedInteger>>( 10, cmp );
		for( Map<IntegerPair, WrappedInteger> curr_idx_count : SH_T_idx_count.values() ) {
			for( Entry<IntegerPair, WrappedInteger> e : curr_idx_count.entrySet() ) {
				if( pq.size() < 10 )
					pq.add( e );
				else if( cmp.compare( pq.peek(), e ) < 0 ) {
					pq.poll();
					pq.add( e );
				}
			}
		}

		Set<IntegerPair> watch = new HashSet<IntegerPair>();
		for( Entry<IntegerPair, WrappedInteger> e : pq ) {
			watch.add( e.getKey() );
			String bigram = Record.twoGram2String( e.getKey() );
			System.out.println( bigram + ": " + e.getValue().get() );
		}

		for( Record recS : tableT ) {
			if( !recS.toString().startsWith( "real estate" ) )
				continue;
			System.out.println( recS.toString() + " :" );
			int searchmax = recS.getMinLength();
			Map<Integer, Map<IntegerPair, WrappedInteger>> invokes = SH_T_invokes;
			List<Set<IntegerPair>> twograms = recS.get2Grams();
			for( int i = 0; i < searchmax; ++i ) {
				Map<IntegerPair, WrappedInteger> curr_invokes = invokes.get( i );
				if( curr_invokes == null ) {
					break;
				}
				int invoke = 0;
				for( IntegerPair twogram : twograms.get( i ) ) {
					WrappedInteger count = curr_invokes.get( twogram );
					if( count != null )
						invoke += count.get();
					System.out.println( i + " | " + Record.twoGram2String( twogram ) + " : " + count.get() );
				}
				System.out.println( i + " | sum " + invoke );
			}
		}
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

		long time1 = System.currentTimeMillis();
		for( Record s : tableS ) {
			appliedRules_sum += searchEquivsByDynamicIndex( s, SH_T_idx, rslt );
		}
		time1 = System.currentTimeMillis() - time1;

		long time2 = System.currentTimeMillis();
		for( Record s : tableS ) {
			if( s.getEstNumRecords() > joinThreshold )
				appliedRules_sum += searchEquivsByDynamicIndex( s, SL_TH_idx, rslt );
		}
		time2 = System.currentTimeMillis() - time2;

		long time3 = System.currentTimeMillis();
		for( Record s : tableS ) {
			if( s.getEstNumRecords() > joinThreshold )
				continue;
			else
				searchEquivsByNaive1Expansion( s, rslt );
		}
		time3 = System.currentTimeMillis() - time3;

		System.out.println( "Avg applied rules : " + appliedRules_sum + "/" + rslt.size() );
		System.out.println( "large S : " + time1 );
		System.out.println( "small S + large R : " + time2 );
		System.out.println( "small S + small S: " + time3 );

		return rslt;
	}

	private int searchEquivsByDynamicIndex( Record s, Map<Integer, Map<IntegerPair, List<Record>>> idx, List<IntegerPair> rslt ) {
		int appliedRules_sum = 0;
		List<Set<IntegerPair>> available2Grams = s.get2Grams();
		int[] range = s.getCandidateLengths( s.size() - 1 );
		int searchmax = Math.min( available2Grams.size(), maxIndex );
		for( int i = 0; i < searchmax; ++i ) {
			Map<IntegerPair, List<Record>> curr_idx = idx.get( i );
			if( curr_idx == null )
				continue;
			List<List<Record>> candidatesList = new ArrayList<List<Record>>();
			for( IntegerPair twogram : available2Grams.get( i ) ) {
				List<Record> tree = curr_idx.get( twogram );

				if( tree == null )
					continue;
				List<Record> list = new ArrayList<Record>();
				for( Record r : tree )
					if( StaticFunctions.overlap( r.getMinLength(), r.getMaxLength(), range[ 0 ], range[ 1 ] ) )
						list.add( r );
				candidatesList.add( list );
			}
			List<Record> candidates = StaticFunctions.union( candidatesList, idComparator );
			if( skipChecking )
				continue;
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

	private class IntegerComparator implements Comparator<Integer> {
		@Override
		public int compare( Integer o1, Integer o2 ) {
			return o1.compareTo( o2 );
		}
	}

	private void searchEquivsByNaive1Expansion( Record s, List<IntegerPair> rslt ) {
		ArrayList<List<Integer>> candidates = new ArrayList<List<Integer>>();
		ArrayList<Record> expanded = s.expandAll( ruletrie );
		for( Record exp : expanded ) {
			List<Integer> list = setR.get( exp );
			if( list == null )
				continue;
			candidates.add( list );
		}
		List<Integer> union = StaticFunctions.union( candidates, new IntegerComparator() );
		for( Integer idx : union )
			rslt.add( new IntegerPair( idx, s.getID() ) );
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

		for( Rule rule : rulelist ) {
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
		long maxSEstNumRecords = 0;
		long maxTEstNumRecords = 0;
		for( int i = 0; i < tableT.size(); ++i ) {
			Record s = tableT.get( i );
			s.setID( i );
			maxSEstNumRecords = Math.max( maxSEstNumRecords, s.getEstNumRecords() );
		}
		for( int i = 0; i < tableS.size(); ++i ) {
			Record t = tableS.get( i );
			t.setID( i );
			maxTEstNumRecords = Math.max( maxTEstNumRecords, t.getEstNumRecords() );
		}

		System.out.println( "Max S expanded size : " + maxSEstNumRecords );
		System.out.println( "Max T expanded size : " + maxTEstNumRecords );
	}

	public void run( double sampleratio ) {
		long startTime = System.currentTimeMillis();
		preprocess( compact, maxIndex, useAutomata );
		System.out.print( "Preprocess finished" );
		System.out.println( " " + ( System.currentTimeMillis() - startTime ) );

		// Retrieve statistics
		statistics();

		// Estimate constants
		findConstants( sampleratio );

		startTime = System.currentTimeMillis();
		try {
			buildJoinMinIndex();
			// checkLongestIndex();
		}
		catch( Exception e ) {
		}
		System.out.print( "Building Index finished" );
		System.out.println( " " + ( System.currentTimeMillis() - startTime ) );

		// Modify index to get optimal theta
		startTime = System.currentTimeMillis();
		findTheta( Integer.MAX_VALUE );
		System.out.print( "Estimation finished" );
		System.out.println( " " + ( System.currentTimeMillis() - startTime ) );
		System.exit( 1 );

		startTime = System.currentTimeMillis();
		Collection<IntegerPair> rslt = join();
		System.out.print( "Join finished" );
		System.out.println( " " + ( System.currentTimeMillis() - startTime ) );
		System.out.println( rslt.size() );
		System.out.println( "Union counter: " + StaticFunctions.union_cmp_counter );

		try {
			BufferedWriter bw = new BufferedWriter( new FileWriter( outputfile ) );
			for( IntegerPair ip : rslt ) {
				if( ip.i1 != ip.i2 )
					bw.write(
							tableT.get( ip.i1 ).toString( strlist ) + "\t==\t" + tableT.get( ip.i2 ).toString( strlist ) + "\n" );
			}
			bw.close();
		}
		catch( IOException e ) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings( "static-access" )
	private void findConstants( double sampleratio ) {
		// Sample
		List<Record> sampleTlist = new ArrayList<Record>();
		List<Record> sampleSlist = new ArrayList<Record>();
		for( Record r : tableT )
			if( Math.random() < sampleratio )
				sampleTlist.add( r );
		for( Record s : tableS )
			if( Math.random() < sampleratio )
				sampleSlist.add( s );
		List<Record> tmpR = tableT;
		tableT = sampleTlist;
		List<Record> tmpS = tableS;
		tableS = sampleSlist;

		System.out.println( sampleTlist.size() + " R records are sampled" );
		System.out.println( sampleSlist.size() + " S records are sampled" );

		// Infer alpha and beta
		Naive1 naiveinst = new Naive1( this );
		Naive1.threshold = 100;
		naiveinst.runWithoutPreprocess();
		alpha = naiveinst.alpha;
		beta = naiveinst.beta;

		// Infer gamma, delta and epsilon
		JoinH2GramNoIntervalTree joinmininst = new JoinH2GramNoIntervalTree( this );
		joinmininst.useAutomata = useAutomata;
		joinmininst.skipChecking = skipChecking;
		joinmininst.maxIndex = maxIndex;
		joinmininst.compact = compact;
		joinmininst.checker = checker;
		joinmininst.outputfile = outputfile;
		try {
			joinmininst.runWithoutPreprocess( true );
		}
		catch( Exception e ) {
			e.printStackTrace();
		}
		gamma = joinmininst.gamma;
		delta = joinmininst.delta;
		epsilon = joinmininst.epsilon;
		System.out.println( "Bigram computation time : " + Record.exectime );
		Validator.printStats();

		// Restore
		tableT = tmpR;
		tableS = tmpS;

		System.out.println( "Alpha : " + alpha );
		System.out.println( "Beta : " + beta );
		System.out.println( "Gamma : " + gamma );
		System.out.println( "Delta : " + delta );
		System.out.println( "Epsilon : " + epsilon );
	}

	private void findTheta( int max_theta ) {
		// Find the best threshold
		long starttime = System.nanoTime();
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

		// Prefix sums
		long currSLExpSize = 0;
		long currTLExpSize = 0;
		while( sidx < tableT.size() || tidx < tableS.size() ) {
			if( theta > max_theta )
				break;
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
					/**
					 * Frequency count of i-th bigrams of TH records
					 */
					Map<IntegerPair, WrappedInteger> curr_invokes = SL_TH_invokes.get( i );
					if( curr_invokes == null ) {
						curr_invokes = new WYK_HashMap<IntegerPair, WrappedInteger>();
						SL_TH_invokes.put( i, curr_invokes );
					}
					/**
					 * Size of the index using i-th bigrams of SL records
					 */
					Map<IntegerPair, WrappedInteger> curr_idx_count = SL_TH_idx_count.get( i );
					for( IntegerPair curr_twogram : twograms.get( i ) ) {
						// Decrease frequency count of TH records
						WrappedInteger count = curr_invokes.get( curr_twogram );
						if( count == ONE )
							curr_invokes.put( curr_twogram, null );
						else if( count != null && count.get() > 0 )
							count.decrement();

						// Update est_SL_TH_cmps
						if( curr_idx_count != null ) {
							count = curr_idx_count.get( curr_twogram );
							if( count != null )
								est_SL_TH_cmps -= count.get();
						}
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
				int SH_T_min_invokes = Integer.MAX_VALUE;
				int SL_TH_min_invokes = Integer.MAX_VALUE;
				int SL_TH_min_index = -1;
				for( int i = 0; i < s.getMinLength(); ++i ) {
					int SH_T_count = 0;
					int SL_TH_count = 0;
					for( IntegerPair curr_twogram : twograms.get( i ) ) {
						WrappedInteger count = SH_T_invokes.get( i ).get( curr_twogram );
						if( count != null )
							SH_T_count += count.get();
						count = SL_TH_invokes.get( i ).get( curr_twogram );
						if( count != null )
							SL_TH_count += count.get();
					}
					if( SH_T_count < SH_T_min_invokes )
						SH_T_min_invokes = SH_T_count;
					if( SL_TH_count < SL_TH_min_invokes ) {
						SL_TH_min_invokes = SL_TH_count;
						SL_TH_min_index = i;
					}
				}
				// Modify SH_T_idx
				// Modify SL_TH_idx
				Map<IntegerPair, WrappedInteger> curr_idx_count = SL_TH_idx_count.get( SL_TH_min_index );
				if( curr_idx_count == null ) {
					curr_idx_count = new WYK_HashMap<IntegerPair, WrappedInteger>();
					SL_TH_idx_count.put( SL_TH_min_index, curr_idx_count );
				}
				for( IntegerPair curr_twogram : twograms.get( SL_TH_min_index ) ) {
					WrappedInteger count = curr_idx_count.get( curr_twogram );
					if( count == null )
						curr_idx_count.put( curr_twogram, ONE );
					else if( count == ONE ) {
						count = new WrappedInteger( 2 );
						curr_idx_count.put( curr_twogram, count );
					}
					else
						count.increment();
				}
				est_SH_T_cmps -= SH_T_min_invokes;
				est_SL_TH_cmps += SL_TH_min_invokes;
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
			esttimes[ 2 ] = (long) ( epsilon * est_SH_T_cmps );
			esttimes[ 3 ] = (long) ( epsilon * est_SL_TH_cmps );
			long esttime = esttimes[ 0 ] + esttimes[ 1 ] + esttimes[ 2 ] + esttimes[ 3 ];
			if( esttime < best_esttime ) {
				best_theta = (int) theta;
				best_esttime = esttime;
				best_esttimes = esttimes;
			}
			if( theta == 10 || theta == 30 || theta == 100 || theta == 300 || theta == 1000 || theta == 3000 ) {
				System.out.println( "T=" + theta + " : " + esttime );
				System.out.println( Arrays.toString( esttimes ) );
				System.out.println( "Mem : " + memcost + " / " + memlimit_expandedS );
			}
			theta = next_theta;
		}
		System.out.print( "Best threshold : " + best_theta );
		System.out.println( " with running time " + best_esttime );
		System.out.println( Arrays.toString( best_esttimes ) );
		long duration = System.nanoTime() - starttime;
		System.out.println( "Find theta with " + duration + "ns" );
	}

	public static void main( String[] args ) throws IOException {
		Parameters params = Parameters.parseArgs( args );
		String Rfile = params.getInputX();
		String Sfile = params.getInputY();
		String Rulefile = params.getInputRules();
		outputfile = params.getOutput();

		// Setup parameters
		useAutomata = params.isUseACAutomata();
		skipChecking = params.isSkipChecking();
		maxIndex = params.getMaxIndex();
		compact = params.isCompact();
		singleside = params.isSingleside();
		checker = params.getValidator();

		long startTime = System.currentTimeMillis();
		Hybrid2GramWithOptTheta2 inst = new Hybrid2GramWithOptTheta2( Rulefile, Rfile, Sfile );
		System.out.print( "Constructor finished" );
		System.out.println( " " + ( System.currentTimeMillis() - startTime ) );
		inst.run( params.getSampleRatio() );
		Validator.printStats();
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

	@Override
	public String getName() {
		return "Hybrid2GramWithOptTheta2";
	}

	@Override
	public void run( String[] args, StatContainer stat ) {
		// TODO Auto-generated method stub

	}
}
