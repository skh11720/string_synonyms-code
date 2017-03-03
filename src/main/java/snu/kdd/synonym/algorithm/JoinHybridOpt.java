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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import mine.Naive1;
import mine.Record;
import mine.RecordIDComparator;
import snu.kdd.synonym.tools.Param;
import snu.kdd.synonym.tools.StatContainer;
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
 * Utilize only one index by sorting records according to their expanded size.
 * It first build JoinMin(JoinH2Gram) index and then change threshold / modify
 * index in order to find the best execution time.
 */
public class JoinHybridOpt extends AlgorithmTemplate {
	static boolean useAutomata = true;
	static boolean skipChecking = false;
	static int maxIndex = Integer.MAX_VALUE;
	static boolean compact = false;
	static boolean singleside;
	static Validator checker;

	RecordIDComparator idComparator;
	RuleTrie ruletrie;

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
	Map<Integer, Map<IntegerPair, Directory>> idx;
	Map<Integer, Map<IntegerPair, WrappedInteger>> T_invokes;

	/**
	 * List of 1-expandable strings
	 */
	Map<Record, List<Integer>> setR;
	/**
	 * Estimated number of comparisons
	 */
	long est_cmps;

	long memlimit_expandedS;

	public JoinHybridOpt( String rulefile, String Rfile, String Sfile, String outputfile ) throws IOException {
		super( rulefile, Rfile, Sfile, outputfile );
		idComparator = new RecordIDComparator();
		ruletrie = new RuleTrie( rulelist );
	}

	private void buildJoinMinIndex() {
		Runtime runtime = Runtime.getRuntime();

		long elements = 0;
		est_cmps = 0;
		// Build an index
		// Count Invokes per each (token, loc) pair
		idx = new WYK_HashMap<Integer, Map<IntegerPair, Directory>>();
		T_invokes = new WYK_HashMap<Integer, Map<IntegerPair, WrappedInteger>>();
		// Actually, tableT
		for( Record rec : tableS ) {
			// long prev = Record.exectime;
			List<Set<IntegerPair>> available2Grams = rec.get2Grams();
			int searchmax = Math.min( available2Grams.size(), maxIndex );
			// Every record is SH/TH record at the beginning
			for( int i = 0; i < searchmax; ++i ) {
				Map<IntegerPair, WrappedInteger> curr_invokes = T_invokes.get( i );
				if( curr_invokes == null ) {
					curr_invokes = new WYK_HashMap<IntegerPair, WrappedInteger>();
					T_invokes.put( i, curr_invokes );
				}
				for( IntegerPair twogram : available2Grams.get( i ) ) {
					WrappedInteger count = curr_invokes.get( twogram );
					if( count == null ) {
						curr_invokes.put( twogram, ONE );
					}
					else if( count == ONE ) {
						count = new WrappedInteger( 2 );
						curr_invokes.put( twogram, count );
					}
					else
						count.increment();
				}
			}
		}

		System.out.println( "Bigram retrieval : " + Record.exectime );
		System.out.println( ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 + "MB used" );

		// Actually, tableS
		for( Record rec : tableR ) {
			List<Set<IntegerPair>> available2Grams = rec.get2Grams();
			int[] range = rec.getCandidateLengths( rec.size() - 1 );
			int minIdx = -1;
			int minInvokes = Integer.MAX_VALUE;
			int searchmax = Math.min( range[ 0 ], maxIndex );
			int[] invokearr = new int[ searchmax ];

			for( int i = 0; i < searchmax; ++i ) {
				Map<IntegerPair, WrappedInteger> curr_invokes = T_invokes.get( i );
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

			Map<IntegerPair, Directory> curr_idx = idx.get( minIdx );
			if( curr_idx == null ) {
				curr_idx = new WYK_HashMap<IntegerPair, Directory>();
				idx.put( minIdx, curr_idx );
			}
			for( IntegerPair twogram : available2Grams.get( minIdx ) ) {
				Directory dir = curr_idx.get( twogram );
				if( dir == null ) {
					dir = new Directory();
					curr_idx.put( twogram, dir );
				}
				dir.list.add( rec );
			}
			int count = available2Grams.get( minIdx ).size();
			est_cmps += minInvokes;
			elements += count;
		}
		System.out.println( "Bigram retrieval : " + Record.exectime );
		System.out.println( ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 + "MB used" );
		memlimit_expandedS = (long) ( runtime.freeMemory() * 0.8 );

		System.out.println( "predict : " + est_cmps );
		System.out.println( "idx size : " + elements );
		System.out.println( WrappedInteger.count + " Wrapped Integers" );
		for(

		Entry<Integer, Map<IntegerPair, Directory>> e : idx.entrySet() ) {
			System.out.println( e.getKey() + " : " + e.getValue().size() );
		}
	}

	private void clearJoinMinIndex() {
		for( Map<IntegerPair, Directory> map : idx.values() )
			map.clear();
		idx.clear();
	}

	private void buildNaiveIndex() {
		// Build 1-expanded set for every record in R
		int count = 0;
		setR = new HashMap<Record, List<Integer>>();
		for( int i = 0; i < tableR.size(); ++i ) {
			Record rec = tableR.get( i );
			assert ( rec != null );
			if( rec.getEstNumRecords() > joinThreshold )
				continue;
			List<Record> expanded = rec.expandAll( ruletrie );
			assert ( expanded.size() <= joinThreshold );
			assert ( !expanded.isEmpty() );
			for( Record expR : expanded ) {
				if( !setR.containsKey( expR ) )
					setR.put( expR, new ArrayList<Integer>( 5 ) );
				List<Integer> list = setR.get( expR );
				assert ( list != null );
				if( !list.isEmpty() && list.get( list.size() - 1 ) == i )
					continue;
				list.add( i );
			}
			++count;
		}
		long idxsize = 0;
		for( List<Integer> list : setR.values() )
			idxsize += list.size();
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
		buildJoinMinIndex();
		System.out.print( "Building JoinMin Index finished" );
		System.out.println( " " + ( System.currentTimeMillis() - startTime ) );

		long time1 = System.currentTimeMillis();
		for( Record s : tableS )
			appliedRules_sum += searchEquivsByDynamicIndex( s, idx, rslt );
		time1 = System.currentTimeMillis() - time1;
		clearJoinMinIndex();

		startTime = System.currentTimeMillis();
		buildNaiveIndex();
		System.out.print( "Building Naive Index finished" );
		System.out.println( " " + ( System.currentTimeMillis() - startTime ) );

		long time2 = System.currentTimeMillis();
		for( Record s : tableS ) {
			if( s.getEstNumRecords() > joinThreshold )
				continue;
			else
				searchEquivsByNaive1Expansion( s, rslt );
		}
		time2 = System.currentTimeMillis() - time2;

		System.out.println( "Avg applied rules : " + appliedRules_sum + "/" + rslt.size() );
		System.out.println( "SH_T + SL_TH : " + time1 );
		System.out.println( "SL_TL : " + time2 );

		return rslt;
	}

	private int searchEquivsByDynamicIndex( Record s, Map<Integer, Map<IntegerPair, Directory>> idx, List<IntegerPair> rslt ) {
		boolean is_TH_record = s.getEstNumRecords() > joinThreshold;
		int appliedRules_sum = 0;
		List<Set<IntegerPair>> available2Grams = s.get2Grams();
		int[] range = s.getCandidateLengths( s.size() - 1 );
		int searchmax = Math.min( available2Grams.size(), maxIndex );
		for( int i = 0; i < searchmax; ++i ) {
			Map<IntegerPair, Directory> curr_idx = idx.get( i );
			if( curr_idx == null )
				continue;
			List<List<Record>> candidatesList = new ArrayList<List<Record>>();
			for( IntegerPair twogram : available2Grams.get( i ) ) {
				Directory tree = curr_idx.get( twogram );

				if( tree == null )
					continue;
				List<Record> list = new ArrayList<Record>();
				for( int j = is_TH_record ? 0 : tree.SHsize; j < tree.list.size(); ++j ) {
					Record r = tree.list.get( j );
					if( StaticFunctions.overlap( r.getMinLength(), r.getMaxLength(), range[ 0 ], range[ 1 ] ) )
						list.add( r );
				}
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

		for( Record rec : tableR ) {
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
		Collections.sort( tableR, cmp );
		Collections.sort( tableS, cmp );

		// Reassign ID
		long maxSEstNumRecords = 0;
		long maxTEstNumRecords = 0;
		for( int i = 0; i < tableR.size(); ++i ) {
			Record s = tableR.get( i );
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
							tableR.get( ip.i1 ).toString( strlist ) + "\t==\t" + tableR.get( ip.i2 ).toString( strlist ) + "\n" );
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
		// TODO remove fixed seed 0
		Random rn = new Random( 0 );

		List<Record> sampleRlist = new ArrayList<Record>();
		List<Record> sampleSlist = new ArrayList<Record>();
		for( Record r : tableR )
			if( rn.nextDouble() < sampleratio )
				sampleRlist.add( r );
		for( Record s : tableS )
			if( rn.nextDouble() < sampleratio )
				sampleSlist.add( s );
		List<Record> tmpR = tableR;
		tableR = sampleRlist;
		List<Record> tmpS = tableS;
		tableS = sampleRlist;

		System.out.println( sampleRlist.size() + " R records are sampled" );
		System.out.println( sampleSlist.size() + " S records are sampled" );

		// Infer alpha and beta
		Naive1 naiveinst = new Naive1( this );
		Naive1.threshold = 100;
		naiveinst.runWithoutPreprocess();
		alpha = naiveinst.alpha;
		beta = naiveinst.beta;

		// Infer gamma, delta and epsilon
		JoinMin joinmininst = new JoinMin( this );
		joinmininst.useAutomata = useAutomata;
		joinmininst.skipChecking = skipChecking;
		joinmininst.maxIndex = maxIndex;
		joinmininst.compact = compact;
		joinmininst.checker = checker;
		joinmininst.outputfile = outputfile;
		try {
			System.out.println( "Joinmininst run" );
			joinmininst.runWithoutPreprocess();
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

		// Restore
		tableR = tmpR;
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

		// Memory cost for storing expanded tableR
		long memcost = 0;

		// Indicates the minimum indices which have more that 'theta' expanded
		// records
		int sidx = 0;
		int tidx = 0;
		long theta = Math.min( tableR.get( 0 ).getEstNumRecords(), tableS.get( 0 ).getEstNumRecords() );

		// Number of bigrams generated by expanded TL records
		Map<Integer, Map<IntegerPair, WrappedInteger>> TL_invokes = new WYK_HashMap<Integer, Map<IntegerPair, WrappedInteger>>();

		// Prefix sums
		long currSLExpSize = 0;
		long currTLExpSize = 0;
		while( sidx < tableR.size() || tidx < tableS.size() ) {
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
					// Frequency count of i-th bigrams of TL records
					Map<IntegerPair, WrappedInteger> curr_invokes = TL_invokes.get( i );
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
						if( curr_idx == null )
							continue;
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
			while( sidx < tableR.size() ) {
				Record s = tableR.get( sidx++ );
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
		String outputfile = params.getOutput();

		// Setup parameters
		useAutomata = params.isUseACAutomata();
		skipChecking = params.isSkipChecking();
		maxIndex = params.getMaxIndex();
		compact = params.isCompact();
		singleside = params.isSingleside();
		checker = params.getValidator();

		long startTime = System.currentTimeMillis();
		JoinHybridOpt inst = new JoinHybridOpt( Rulefile, Rfile, Sfile, outputfile );
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
		return "JoinHybridOpt";
	}

	@Override
	public void run( String[] args, StatContainer stat ) {
		this.stat = stat;

		Param params = Param.parseArgs( args );
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
