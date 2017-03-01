package snu.kdd.synonym.algorithm;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import mine.JoinH2GramNoIntervalTree2;
import mine.Naive1;
import mine.Record;
import mine.RecordIDComparator;
import snu.kdd.synonym.tools.Param;
import snu.kdd.synonym.tools.StatContainer;
import tools.IntegerPair;
import tools.Parameters;
import tools.RandHash;
import tools.Rule;
import tools.RuleTrie;
import tools.StaticFunctions;
import tools.WYK_HashMap;
import validator.Validator;

/**
 * Given threshold, if a record has more than 'threshold' 1-expandable strings,
 * use an index to store them.
 * Otherwise, generate all 1-expandable strings and then use them to check
 * if two strings are equivalent.
 * It first build JoinMin(JoinH2Gram) index and then change threshold / modify
 * index in order to find the best execution time.
 * Same as Hybrid2GramWithOptTheta2, but
 * 1) utilizes minhash to estimate execution and
 * 2) use Hybrid2GramA1_2 instead of A1
 * time.
 */
public class JoinHybrid extends AlgorithmTemplate {
	static boolean useAutomata = true;
	static boolean skipChecking = false;
	static int maxIndex = Integer.MAX_VALUE;
	static boolean compact = false;
	static boolean singleside;
	static Validator checker;

	RecordIDComparator idComparator;
	RuleTrie ruletrie;

	static String outputfile;

	int joinThreshold = -1;

	double alpha;
	double beta;
	double gamma;
	double delta;
	double epsilon;
	double zeta;
	public static double dirsampleratio = 0.01;

	public static double idx_count_sampleratio = 0.01;
	public static int mhsize = 30;
	RandHash[] rhfunc;

	private static final int RECORD_CLASS_BYTES = 120;

	private static final Random rand = new Random();

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
	Map<Integer, Map<IntegerPair, List<Record>>> S_TH_idx = new WYK_HashMap<>();
	/**
	 * Index of the records in S for the strings in T which has more than
	 * 'threshold' 1-expandable strings
	 * (SH x T)
	 */
	Map<Integer, Map<IntegerPair, List<Record>>> SH_TL_idx = new WYK_HashMap<>();

	/**
	 * Frequency counts of the records in TH
	 */
	Map<Integer, Map<IntegerPair, Directory>> invokes;

	/**
	 * Inverted index of the records in TH
	 */
	Map<Integer, List<Directory>> inv_invokes;

	/**
	 * List of 1-expandable strings
	 */
	Map<Record, List<Integer>> setR;
	/**
	 * Estimated number of comparisons
	 */
	long est_S_TH_cmps;
	long est_SH_TL_cmps;

	long maxtheta;
	long[] currtheta;

	long[] expcostS;
	long[] expcostT;
	long[] cumulative_expcostS;
	long[] cumulative_expcostT;

	int minSHidx;
	int minTHidx;

	List<Record> sampleS;
	List<Record> sampleT;
	long[] expcostSampleT;

	public JoinHybrid( String rulefile, String Rfile, String Sfile, String outputFile ) throws IOException {
		super( rulefile, Rfile, Sfile, outputFile );
		idComparator = new RecordIDComparator();
		ruletrie = new RuleTrie( getRulelist() );
		rhfunc = new RandHash[ mhsize ];
		for( int i = 0; i < mhsize; ++i )
			rhfunc[ i ] = new RandHash();
		currtheta = new long[ 4 ];
	}

	class Directory {
		/**
		 * List of records which belongs to this directory.
		 * Assumption: records are inserted with increasing order of expanded size
		 */
		List<Record> list;
		/**
		 * Number of records belongs to SL/TL
		 */
		int[] Lsize;
		/**
		 * Minhash of records belongs to SH/TH
		 */
		int[][] H_minhash;
		/**
		 * Minhash of records belongs to SL/TL
		 */
		int[][] L_minhash;

		Directory() {
			list = new ArrayList<Record>();
			Lsize = new int[ 4 ];
			H_minhash = new int[ 4 ][ mhsize ];
			L_minhash = new int[ 4 ][ mhsize ];
			for( int i = 0; i < 4; ++i ) {
				for( int j = 0; j < mhsize; ++j ) {
					H_minhash[ i ][ j ] = Integer.MAX_VALUE;
					L_minhash[ i ][ j ] = Integer.MAX_VALUE;
				}
			}
		}

		// Add a record
		void add( long[] theta, Record rec, long rectheta ) {
			assert ( theta.length == 4 );
			list.add( rec );
			for( int i = 0; i < 4; ++i )
				// SL/TL record
				if( rectheta <= theta[ i ] )
					++Lsize[ i ];
			for( int i = 0; i < mhsize; ++i ) {
				int hash = rhfunc[ i ].get( rec.getID() );
				for( int j = 0; j < 4; ++j )
					// SL/TL record
					if( rectheta <= theta[ j ] )
						L_minhash[ j ][ i ] = Math.min( L_minhash[ j ][ i ], hash );
					// SH/TH record
					else
						H_minhash[ j ][ i ] = Math.min( H_minhash[ j ][ i ], hash );
			}
		}

		/**
		 * Update current directory: change the first H record to L record.
		 * (in idx-th index)
		 */
		void moveFirstH2L( int idx, Record rec, int[] mh ) {
			if( !( 0 <= Lsize[ idx ] && Lsize[ idx ] < list.size() ) ) {
				System.err.println( "[moveFirstH2L] size assertion failed" );
			}
			assert ( 0 <= Lsize[ idx ] && Lsize[ idx ] < list.size() );

			Record firstH = list.get( Lsize[ idx ]++ );
			if( firstH != rec ) {
				System.err.println( "[moveFirstH2L] firstH does not equal to rec" );
			}
			assert ( firstH == rec );

			// Update H_minhash
			for( int i = 0; i < mhsize; ++i ) {
				if( mh[ i ] == H_minhash[ idx ][ i ] ) {
					H_minhash[ idx ][ i ] = Integer.MAX_VALUE;
					for( int j = Lsize[ idx ]; j < list.size(); ++j ) {
						Record recH = list.get( j );
						int rechash = rhfunc[ i ].get( recH.getID() );
						H_minhash[ idx ][ i ] = Math.min( rechash, H_minhash[ idx ][ i ] );
					}
				}
			}

			// Update L_minhash
			for( int i = 0; i < mhsize; ++i )
				L_minhash[ idx ][ i ] = Math.min( L_minhash[ idx ][ i ], mh[ i ] );
		}
	}

	private void computeInvocationCounts( long[] theta ) {
		// Build an index
		// Count Invokes per each (token, loc) pair
		invokes = new WYK_HashMap<Integer, Map<IntegerPair, Directory>>();
		inv_invokes = new WYK_HashMap<Integer, List<Directory>>();
		// Actually, tableT
		for( Record rec : sampleT ) {
			long estTheta = rec.getEstNumRecords();
			List<Set<IntegerPair>> available2Grams = rec.get2Grams();
			int searchmax = Math.min( available2Grams.size(), maxIndex );

			for( int i = 0; i < searchmax; ++i ) {
				Map<IntegerPair, Directory> curr_invokes = null;
				curr_invokes = invokes.get( i );
				if( curr_invokes == null ) {
					curr_invokes = new WYK_HashMap<IntegerPair, Directory>();
					invokes.put( i, curr_invokes );
				}
				for( IntegerPair twogram : available2Grams.get( i ) ) {
					Directory count = curr_invokes.get( twogram );
					if( count == null ) {
						count = new Directory();
						curr_invokes.put( twogram, count );
					}
					/**
					 * @TODO
					 */
					count.add( theta, rec, estTheta );

					List<Directory> curr_inv_invokes = inv_invokes.get( rec.getID() );
					if( curr_inv_invokes == null ) {
						curr_inv_invokes = new ArrayList<Directory>( 3 );
						inv_invokes.put( rec.getID(), curr_inv_invokes );
					}
					curr_inv_invokes.add( count );
				}
			}
		}
		System.out.println( "Bigram retrieval : " + Record.exectime );
		currtheta = theta;
	}

	/**
	 * Update invocation count. prevtheta must be less than new theta.
	 * 
	 * @param idx
	 *            Index of the invocation count index to update
	 * @param prevtheta
	 * @param theta
	 */
	private void updateInvocationCounts_IncrTheta( int idx, long prevtheta, long theta ) {
		assert ( prevtheta < theta );
		int beginidx = findMaxIdx( expcostSampleT, prevtheta ) + 1;

		// Actually, tableT
		// Move from TH to TL
		int[] minhash = new int[ mhsize ];
		for( int tidx = beginidx; tidx < sampleT.size(); ++tidx ) {
			Record rec = sampleT.get( tidx );
			long exprecs = rec.getEstNumRecords();
			assert ( exprecs == expcostSampleT[ tidx ] );
			if( rec.getEstNumRecords() > theta )
				break;
			assert ( exprecs <= theta );

			// Compute minhash
			for( int i = 0; i < mhsize; ++i )
				minhash[ i ] = rhfunc[ i ].get( rec.getID() );

			// Move current record from H to L
			List<Directory> dirlist = inv_invokes.get( rec.getID() );
			if( dirlist != null )
				for( Directory dir : dirlist )
					dir.moveFirstH2L( idx, rec, minhash );
		}
		currtheta[ idx ] = theta;
	}

	private void copyInvocationCounts( int fromidx, int toidx ) {
		for( Map<IntegerPair, Directory> list : invokes.values() ) {
			for( Directory dir : list.values() ) {
				dir.Lsize[ toidx ] = dir.Lsize[ fromidx ];
				// Copy minhash values
				for( int mh = 0; mh < mhsize; ++mh ) {
					dir.H_minhash[ toidx ][ mh ] = dir.H_minhash[ fromidx ][ mh ];
					dir.L_minhash[ toidx ][ mh ] = dir.L_minhash[ fromidx ][ mh ];
				}
			}
		}
	}

	/**
	 * If theta == -1, find the maximum theta as well.
	 * Else, use the given theta.
	 */
	private void estMaxTheta( int theta ) {
		Runtime runtime = Runtime.getRuntime();
		System.out.println( ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 + "MB used" );
		long memlimit = theta < 0 ? runtime.freeMemory() / 2 : Long.MAX_VALUE;
		if( theta >= 0 )
			maxtheta = theta;

		minSHidx = minTHidx = 0;
		long currexpanded = 0;
		long memcost = 0;
		for( Record s : tableR ) {
			long expanded = s.getEstNumRecords();
			if( expanded >= Integer.MAX_VALUE )
				break;
			else if( theta >= 0 ) {
				if( expanded > theta )
					break;
				++minSHidx;
			}
			else if( expanded != currexpanded ) {
				minSHidx = s.getID();
				currexpanded = expanded;
			}
			memcost += memcost( s.getEstExpandCost(), expanded );
			if( memcost > memlimit )
				break;
		}
		if( theta < 0 ) {
			if( memcost > memlimit )
				maxtheta = currexpanded - 1;
			else {
				maxtheta = currexpanded;
				minSHidx = tableR.size();
			}
		}

		currexpanded = 0;
		memcost = 0;
		for( Record t : tableS ) {
			long expanded = t.getEstNumRecords();
			if( expanded >= Integer.MAX_VALUE )
				break;
			else if( theta >= 0 ) {
				if( expanded > theta )
					break;
				++minTHidx;
			}
			else if( expanded != currexpanded ) {
				minTHidx = t.getID();
				currexpanded = expanded;
			}
			memcost += memcost( t.getEstExpandCost(), expanded );
			if( memcost > memlimit )
				break;
		}
		if( theta < 0 ) {
			if( memcost > memlimit )
				maxtheta = Math.max( maxtheta, currexpanded - 1 );
			else {
				maxtheta = Math.max( maxtheta, currexpanded );
				minTHidx = tableS.size();
			}
		}
	}

	private int memcost( long lengthsum, long exps ) {
		int memcost = 0;
		// Size for the integer arrays
		memcost += 4 * lengthsum + 16 * exps;
		// Size for the Record instance
		memcost += RECORD_CLASS_BYTES * exps;
		// Pointers in the inverted index
		memcost += 8 * exps;
		// Pointers in the Hashmap (in worst case)
		// Our hashmap filling ratio is 0.5: 24 / 0.5 = 48
		memcost += 48 * exps;

		return memcost;
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

		long time1 = 0;
		long time2 = 0;
		for( Record s : tableS ) {
			long time = System.currentTimeMillis();
			if( s.getEstNumRecords() > joinThreshold ) {
				appliedRules_sum += searchEquivsByDynamicIndex( s, S_TH_idx, rslt );
				time1 += System.currentTimeMillis() - time;
			}
			else {
				appliedRules_sum += searchEquivsByDynamicIndex( s, SH_TL_idx, rslt );
				time2 += System.currentTimeMillis() - time;
			}
		}

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
		Collections.sort( tableR, cmp );
		Collections.sort( tableS, cmp );

		// Reassign ID
		// Compute exp records
		expcostS = new long[ tableR.size() ];
		expcostT = new long[ tableS.size() ];
		cumulative_expcostS = new long[ tableR.size() ];
		cumulative_expcostT = new long[ tableS.size() ];
		long maxSEstNumRecords = 0;
		long maxTEstNumRecords = 0;
		long sum = 0;
		for( int i = 0; i < tableR.size(); ++i ) {
			Record s = tableR.get( i );
			s.setID( i );
			long est = s.getEstNumRecords();
			expcostS[ i ] = est;
			cumulative_expcostS[ i ] = ( est > Integer.MAX_VALUE ) ? -1 : ( sum += est );
			maxSEstNumRecords = Math.max( maxSEstNumRecords, est );
		}
		sum = 0;
		for( int i = 0; i < tableS.size(); ++i ) {
			Record t = tableS.get( i );
			t.setID( i );
			long est = t.getEstNumRecords();
			expcostT[ i ] = est;
			cumulative_expcostT[ i ] = ( est > Integer.MAX_VALUE ) ? -1 : ( sum += est );
			maxTEstNumRecords = Math.max( maxTEstNumRecords, est );
		}

		System.out.println( "Max S expanded size : " + maxSEstNumRecords );
		System.out.println( "Max T expanded size : " + maxTEstNumRecords );
	}

	public void run( double sampleratio ) {
		long startTime = System.currentTimeMillis();
		preprocess( compact, maxIndex, useAutomata );
		System.out.print( "Preprocess finished" );
		System.out.println( " " + ( System.currentTimeMillis() - startTime ) + "ms" );

		// Retrieve statistics
		statistics();

		// Compute maximum theta first
		estMaxTheta( -1 );

		// Estimate constants
		findConstants( sampleratio );

		startTime = System.currentTimeMillis();
		// checkLongestIndex();
		System.out.print( "Building Index finished" );
		System.out.println( " " + ( System.currentTimeMillis() - startTime ) + "ms" );

		// Modify index to get optimal theta
		startTime = System.currentTimeMillis();
		findThetaByBinarySrc();
		System.out.print( "Binary src Estimation finished" );
		System.out.println( " " + ( System.currentTimeMillis() - startTime ) + "ms" );

		startTime = System.currentTimeMillis();
		findThetaByLinearSrc();
		System.out.print( "Linear src Estimation finished" );
		System.out.println( " " + ( System.currentTimeMillis() - startTime ) + "ms" );

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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@SuppressWarnings( "static-access" )
	private void findConstants( double sampleratio ) {
		// Sample
		List<Record> sampleRlist = new ArrayList<Record>();
		List<Record> sampleSlist = new ArrayList<Record>();
		for( Record r : tableR )
			if( rand.nextDouble() < sampleratio )
				sampleRlist.add( r );
		for( Record s : tableS )
			if( rand.nextDouble() < sampleratio )
				sampleSlist.add( s );
		List<Record> tmpR = tableR;
		tableR = sampleSlist;
		List<Record> tmpS = tableS;
		tableS = sampleSlist;

		System.out.println( sampleRlist.size() + " R records are sampled" );
		System.out.println( sampleSlist.size() + " S records are sampled" );
		System.out.println( "Estimated maximum theta : " + maxtheta );

		// Infer alpha and beta
		Naive1 naiveinst = new Naive1( this );
		Naive1.threshold = ( maxtheta / 2 );
		naiveinst.runWithoutPreprocess();
		alpha = naiveinst.alpha;
		beta = naiveinst.beta;
		naiveinst = null;
		System.gc();

		// Infer gamma, delta and epsilon
		JoinH2GramNoIntervalTree2 joinmininst = new JoinH2GramNoIntervalTree2( this );
		joinmininst.useAutomata = useAutomata;
		joinmininst.skipChecking = skipChecking;
		joinmininst.maxIndex = maxIndex;
		joinmininst.compact = compact;
		joinmininst.checker = checker;
		joinmininst.outputfile = outputfile;
		joinmininst.runWithoutPreprocess();
		gamma = joinmininst.gamma;
		delta = joinmininst.delta;
		epsilon = joinmininst.epsilon;
		zeta = joinmininst.zeta;
		System.out.println( "Bigram computation time : " + Record.exectime );
		joinmininst = null;
		System.gc();
		Validator.printStats();

		// Restore
		tableR = tmpR;
		tableS = tmpS;

		System.out.println( "Alpha : " + alpha );
		System.out.println( "Beta : " + beta );
		System.out.println( "Gamma : " + gamma );
		System.out.println( "Delta : " + delta );
		System.out.println( "Epsilon : " + epsilon );
		System.out.println( "Zeta : " + zeta );
	}

	private void findThetaByBinarySrc() {
		long bestTheta = -1;
		long bestExecTime = Long.MAX_VALUE;

		// Sample records
		sampleS = new ArrayList<Record>();
		sampleT = new ArrayList<Record>();
		for( Record recS : tableR )
			if( rand.nextDouble() < idx_count_sampleratio )
				sampleS.add( recS );
		for( Record recT : tableS )
			if( rand.nextDouble() < dirsampleratio )
				sampleT.add( recT );
		expcostSampleT = new long[ sampleT.size() ];
		for( int i = 0; i < sampleT.size(); ++i )
			expcostSampleT[ i ] = sampleT.get( i ).getEstNumRecords();

		for( int i = 0; i < 4; ++i )
			currtheta[ i ] = ( maxtheta * i ) / 3;
		// Compute initial invocation counts
		computeInvocationCounts( currtheta );
		EstTime[] list = new EstTime[ 4 ];
		for( int i = 0; i < 4; ++i ) {
			list[ i ] = new EstTime();
			list[ i ].l1 = currtheta[ i ];
			list[ i ].l2 = estTime( i );
			list[ i ].i = i;
		}

		try {
			BufferedWriter bw = new BufferedWriter( new FileWriter( "findThetaByBinarySrc.txt" ) );

			System.out.println( "Max theta : " + maxtheta + "\n" );
			bw.write( "Max theta : " + maxtheta );
			// Do binary(?) search
			while( true ) {
				Arrays.sort( list );
				if( list[ 0 ].l1 >= list[ 1 ].l1 - 1 && list[ 1 ].l1 >= list[ 2 ].l1 - 1 && list[ 2 ].l1 >= list[ 3 ].l1 - 1 )
					break;

				// Identify type
				for( int i = 0; i < 4; ++i ) {
					System.out.print( "Theta/time = " + list[ i ].l1 + "/" + ( list[ i ].l2 / 1000000 ) + "ms " );
					bw.write( "Theta/time = " + list[ i ].l1 + "/" + ( list[ i ].l2 / 1000000 ) + "ms " );
				}
				System.out.println();
				bw.newLine();

				// a < b < c < d : can prune b ~ d
				if( list[ 0 ].l2 <= list[ 1 ].l2 && list[ 1 ].l2 <= list[ 2 ].l2 && list[ 2 ].l2 <= list[ 3 ].l2 ) {
					// Update c
					long theta_c = ( list[ 0 ].l1 * 2 + list[ 1 ].l1 ) / 3;
					copyInvocationCounts( list[ 0 ].i, list[ 2 ].i );
					updateInvocationCounts_IncrTheta( list[ 2 ].i, list[ 0 ].l1, theta_c );
					list[ 2 ].l1 = currtheta[ list[ 2 ].i ];
					list[ 2 ].l2 = estTime( list[ 2 ].i );

					// Update d
					long theta_d = ( list[ 0 ].l1 + list[ 1 ].l1 * 2 ) / 3;
					copyInvocationCounts( list[ 2 ].i, list[ 3 ].i );
					updateInvocationCounts_IncrTheta( list[ 3 ].i, list[ 2 ].l1, theta_d );
					list[ 3 ].l1 = currtheta[ list[ 3 ].i ];
					list[ 3 ].l2 = estTime( list[ 3 ].i );
				}
				// a > b < c < d : can prune c ~ d
				else if( list[ 0 ].l2 > list[ 1 ].l2 && list[ 1 ].l2 <= list[ 2 ].l2 && list[ 2 ].l2 <= list[ 3 ].l2 ) {
					// Update d
					long theta_d = 0;
					if( list[ 1 ].l1 - list[ 0 ].l1 < list[ 2 ].l1 - list[ 1 ].l1 ) {
						// d = (b + c) / 2
						theta_d = ( list[ 1 ].l1 + list[ 2 ].l1 ) / 2;
						copyInvocationCounts( list[ 1 ].i, list[ 3 ].i );
						updateInvocationCounts_IncrTheta( list[ 3 ].i, list[ 1 ].l1, theta_d );
					}
					else {
						// d = (a + b) / 2
						theta_d = ( list[ 0 ].l1 + list[ 1 ].l1 ) / 2;
						copyInvocationCounts( list[ 0 ].i, list[ 3 ].i );
						updateInvocationCounts_IncrTheta( list[ 3 ].i, list[ 0 ].l1, theta_d );
					}
					list[ 3 ].l1 = currtheta[ list[ 3 ].i ];
					list[ 3 ].l2 = estTime( list[ 3 ].i );
				}
				// a > b > c < d : can prune a ~ b
				else if( list[ 0 ].l2 > list[ 1 ].l2 && list[ 1 ].l2 > list[ 2 ].l2 && list[ 2 ].l2 <= list[ 3 ].l2 ) {
					// Update a
					long theta_a = 0;
					if( list[ 2 ].l1 - list[ 1 ].l1 < list[ 3 ].l1 - list[ 2 ].l1 ) {
						// a = (c + d) / 2
						theta_a = ( list[ 2 ].l1 + list[ 3 ].l1 ) / 2;
						copyInvocationCounts( list[ 2 ].i, list[ 0 ].i );
						updateInvocationCounts_IncrTheta( list[ 0 ].i, list[ 2 ].l1, theta_a );
					}
					else {
						// a = (b + c) / 2
						theta_a = ( list[ 1 ].l1 + list[ 2 ].l1 ) / 2;
						copyInvocationCounts( list[ 1 ].i, list[ 0 ].i );
						updateInvocationCounts_IncrTheta( list[ 0 ].i, list[ 1 ].l1, theta_a );
					}
					list[ 0 ].l1 = currtheta[ list[ 0 ].i ];
					list[ 0 ].l2 = estTime( list[ 0 ].i );
				}
				// a > b > c > d : can prune a ~ c
				else if( list[ 0 ].l2 > list[ 1 ].l2 && list[ 1 ].l2 > list[ 2 ].l2 && list[ 2 ].l2 > list[ 3 ].l2 ) {
					// Update a
					long theta_a = ( list[ 2 ].l1 * 2 + list[ 3 ].l1 ) / 3;
					copyInvocationCounts( list[ 2 ].i, list[ 0 ].i );
					updateInvocationCounts_IncrTheta( list[ 0 ].i, list[ 2 ].l1, theta_a );
					list[ 0 ].l1 = currtheta[ list[ 0 ].i ];
					list[ 0 ].l2 = estTime( list[ 0 ].i );

					// Update b
					long theta_b = ( list[ 2 ].l1 + list[ 3 ].l1 * 2 ) / 3;
					copyInvocationCounts( list[ 0 ].i, list[ 1 ].i );
					updateInvocationCounts_IncrTheta( list[ 1 ].i, list[ 0 ].l1, theta_b );
					list[ 1 ].l1 = currtheta[ list[ 1 ].i ];
					list[ 1 ].l2 = estTime( list[ 1 ].i );
				}
				else {
					// Perform linear search
					System.out.flush();
					System.err.println( "Cannot perform binary src" );
					System.err.flush();
					for( long theta = list[ 0 ].l1; theta <= list[ 3 ].l1; ++theta ) {
						System.out.println( "Theta = " + theta );
						long esttime = estTime( list[ 0 ].i );
						if( esttime < bestExecTime ) {
							bestTheta = theta;
							bestExecTime = esttime;
						}
						updateInvocationCounts_IncrTheta( list[ 0 ].i, theta, theta + 1 );
					}
					break;
				}
			}
			bw.close();
		}
		catch( Exception e ) {
			e.printStackTrace();
			System.exit( 1 );
		}

		// Not found by linear src
		if( bestTheta == -1 )
			for( int i = 0; i < 4; ++i ) {
				if( bestExecTime > list[ i ].l2 ) {
					bestTheta = list[ i ].l1;
					bestExecTime = list[ i ].l2;
				}
			}

		System.out.println( "Best theta = " + bestTheta );
		System.out.println( "With exectime = " + bestExecTime + "ns" );
	}

	private void findThetaByLinearSrc() {
		long bestTheta = -1;
		long bestExecTime = Long.MAX_VALUE;

		for( int i = 0; i < 4; ++i )
			currtheta[ i ] = 0;
		// Compute initial invocation counts
		computeInvocationCounts( currtheta );
		EstTime[] list = new EstTime[ 4 ];
		for( int i = 0; i < 4; ++i ) {
			list[ i ] = new EstTime();
			list[ i ].l1 = currtheta[ i ];
			list[ i ].l2 = estTime( i );
			list[ i ].i = i;
		}

		try {
			BufferedWriter bw = new BufferedWriter( new FileWriter( "findThetaByLinearSrc.txt" ) );

			System.out.println( "Max theta : " + maxtheta + "\n" );
			bw.write( "Max theta : " + maxtheta );
			// Perform linear search
			for( long theta = 0; theta <= maxtheta; ++theta ) {
				System.out.println( "Theta = " + theta );
				long esttime = estTime( list[ 0 ].i );
				if( esttime < bestExecTime ) {
					bestTheta = theta;
					bestExecTime = esttime;
				}
				updateInvocationCounts_IncrTheta( list[ 0 ].i, theta, theta + 1 );
			}
			bw.close();
		}
		catch( Exception e ) {
			e.printStackTrace();
			System.exit( 1 );
		}
		System.out.println( "Best theta = " + bestTheta );
		System.out.println( "With exectime = " + bestExecTime + "ns" );
	}

	private class EstTime implements Comparable<EstTime> {
		// Theta
		long l1;
		// Exec time
		long l2;
		// Index
		int i;

		@Override
		public int compareTo( EstTime o ) {
			int cmp = Long.compare( l1, o.l1 );
			if( cmp == 0 )
				return Long.compare( l2, o.l2 );
			return cmp;
		}

		@Override
		public String toString() {
			return String.format( "<%d %d %d>", l1, l2, i );
		}
	}

	private long estTime( int diridx ) {
		// Compute Naive part
		int sidx = findMaxIdx( expcostS, currtheta[ diridx ] );
		int tidx = findMaxIdx( expcostT, currtheta[ diridx ] );
		// 1) Build index
		long naivebldidxtime = sidx == -1 ? 0 : (long) ( alpha * cumulative_expcostS[ sidx ] );
		// 2) Access index
		long naiveaccessidxtime = tidx == -1 ? 0 : (long) ( beta * cumulative_expcostT[ tidx ] );
		long naivetime = naivebldidxtime + naiveaccessidxtime;

		// Compute index part
		long[] retrievetime = new long[ 2 ];
		long[] equivchecktime = new long[ 2 ];
		int[] minhash = new int[ mhsize ];
		for( Record s : sampleS ) {
			boolean is_SH_record = ( s.getEstNumRecords() > currtheta[ diridx ] );
			int searchmax = Math.min( s.getMinLength(), maxIndex );
			List<Set<IntegerPair>> twograms = s.get2Grams();

			// 0) Find the best position
			// mapidx = 0 : Join with records in TL
			// mapidx = 1 : Join with records in TH
			for( int mapidx = 0; mapidx < 2; ++mapidx ) {
				if( !is_SH_record && mapidx == 0 )
					continue;
				int bestUnionSize = Integer.MAX_VALUE;
				double bestCandSize = Double.POSITIVE_INFINITY;
				for( int j = 0; j < searchmax; ++j ) {
					// If there is no invocations on the current position,
					// this position is the best position.
					Map<IntegerPair, Directory> curr_invokes = invokes.get( j );
					if( curr_invokes == null ) {
						bestUnionSize = 0;
						bestCandSize = 0;
						break;
					}

					int unionsize = 0;
					double candsize = 0;
					int maxsize = 0;
					int[] maxsizehash = null;
					for( int k = 0; k < mhsize; ++k )
						minhash[ k ] = Integer.MAX_VALUE;
					Set<IntegerPair> curr_twograms = twograms.get( j );
					for( IntegerPair twogram : curr_twograms ) {
						Directory dir = curr_invokes.get( twogram );
						if( dir == null )
							continue;
						// Join with records in TL
						int listsize = mapidx == 0 ? dir.Lsize[ diridx ] : dir.list.size() - dir.Lsize[ diridx ];
						int[] listminhash = mapidx == 0 ? dir.L_minhash[ diridx ] : dir.H_minhash[ diridx ];
						// Join with records in TH
						if( maxsize < listsize ) {
							maxsize = listsize;
							maxsizehash = listminhash;
						}
						unionsize += listsize;
						for( int k = 0; k < mhsize; ++k ) {
							int hash = listminhash[ k ];
							minhash[ k ] = Math.min( minhash[ k ], hash );
						}
					}
					// Estimate union size
					if( maxsizehash != null ) {
						int inter = 0;
						assert ( maxsize != 0 );
						for( int k = 0; k < mhsize; ++k )
							if( minhash[ k ] == maxsizehash[ k ] )
								++inter;
						if( inter == 0 )
							candsize = Math.min( tableS.size(), unionsize );
						else
							candsize = ( (double) maxsize * mhsize ) / inter;
					}
					if( bestCandSize > candsize ) {
						bestUnionSize = unionsize;
						bestCandSize = candsize;
					}
				}

				// 1) Retrieve candidates
				retrievetime[ mapidx ] += (long) ( zeta * bestUnionSize );

				// 2) Equivalence check
				equivchecktime[ mapidx ] += (long) ( epsilon * bestCandSize );
			}
		}
		retrievetime[ 0 ] /= idx_count_sampleratio * dirsampleratio;
		retrievetime[ 1 ] /= idx_count_sampleratio * dirsampleratio;
		equivchecktime[ 0 ] /= idx_count_sampleratio * dirsampleratio;
		equivchecktime[ 1 ] /= idx_count_sampleratio * dirsampleratio;
		long STHtime = retrievetime[ 1 ] + equivchecktime[ 1 ];
		long SHTLtime = retrievetime[ 0 ] + equivchecktime[ 0 ];
		long totaltime = naivetime + STHtime + SHTLtime;

		System.out.println( "Naive : " + naivebldidxtime + " + " + naiveaccessidxtime + " = " + naivetime );
		System.out.println( "SH x TL : " + retrievetime[ 0 ] + " + " + equivchecktime[ 0 ] + " = " + SHTLtime );
		System.out.println( "S x TH : " + retrievetime[ 1 ] + " + " + equivchecktime[ 1 ] + " = " + STHtime );
		System.out.println( "Total time: " + naivetime + " + " + SHTLtime + " + " + STHtime + " = " + totaltime );
		return naivetime + STHtime + SHTLtime;
	}

	/**
	 * Find the maximum index which expands less or equal to 'theta' records.
	 */
	private int findMaxIdx( long[] list, long theta ) {
		if( list[ 0 ] > theta )
			return -1;

		int min = 0;
		int max = list.length;
		while( min < max - 1 ) {
			int curr = ( max + min ) / 2;
			if( list[ curr ] > theta )
				max = curr;
			else
				min = curr;
		}
		return min;
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
		JoinHybrid inst = new JoinHybrid( Rulefile, Rfile, Sfile, outputfile );
		inst.joinThreshold = params.getJoinThreshold();
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
		return "JoinHybrid";
	}

	@Override
	public void run( String[] args, StatContainer stat ) {
		this.stat = stat;

		Param params = Param.parseArgs( args );

		useAutomata = params.isUseACAutomata();
		skipChecking = params.isSkipChecking();
		maxIndex = params.getMaxIndex();
		compact = params.isCompact();
		singleside = params.isSingleside();
		checker = params.getValidator();

		long startTime = System.currentTimeMillis();
		this.joinThreshold = params.getJoinThreshold();
		System.out.print( "Constructor finished" );
		System.out.println( " " + ( System.currentTimeMillis() - startTime ) );
		this.run( params.getSampleRatio() );
		Validator.printStats();
	}
}
