package mine;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import tools.Algorithm;
import tools.IntegerPair;
import tools.Parameters;
import tools.RandHash;
import tools.Rule;
import tools.RuleTrie;
import tools.StaticFunctions;
import tools.WYK_HashMap;
import tools.WYK_HashSet;
import validator.TopDownHashSetSinglePath_DS_SharedPrefix;
import validator.Validator;

/**
 * Same as JoinH2GramNoIntervalTree2, but utilizes minhash to estimate the set
 * union size while building an index
 * Assumption: records are sorted with their IDs.
 */
public class JoinH2GramNoIntervalTree2 extends Algorithm {
	public static boolean useAutomata = false;
	public static boolean skipChecking = false;
	public static int maxIndex = Integer.MAX_VALUE;
	public static boolean compact = true;
	public static boolean singleside = false;
	public static boolean exact2grams = false;

	RecordIDComparator idComparator;
	RuleTrie ruletrie;

	public static String outputfile;

	public static Validator checker;
	/**
	 * Key: (2gram, index) pair<br/>
	 * Value: (min, max, record) triple
	 */
	Map<Integer, Map<IntegerPair, List<Record>>> idx;

	public static double dirsampleratio = 0.01;
	public static int mhsize = 30;
	RandHash[] rhfunc;

	public long buildIndexTime;
	public long buildIndexTime1;
	public long buildIndexTime2;
	public long candExtractTime;
	public long joinTime;

	public double gamma;
	public double delta;
	public double epsilon;
	public double zeta;

	public JoinH2GramNoIntervalTree2( String rulefile, String Rfile, String Sfile ) throws IOException {
		super( rulefile, Rfile, Sfile );

		Record.setStrList( strlist );
		idComparator = new RecordIDComparator();
		ruletrie = new RuleTrie( rulelist );
		Record.setRuleTrie( ruletrie );
		rhfunc = new RandHash[ mhsize ];
		for( int i = 0; i < mhsize; ++i )
			rhfunc[ i ] = new RandHash();
	}

	public JoinH2GramNoIntervalTree2( Algorithm o ) {
		super( o );

		Record.setStrList( strlist );
		idComparator = new RecordIDComparator();
		ruletrie = new RuleTrie( rulelist );
		Record.setRuleTrie( ruletrie );
		rhfunc = new RandHash[ mhsize ];
		for( int i = 0; i < mhsize; ++i )
			rhfunc[ i ] = new RandHash();
	}

	class Directory {
		int count;
		int[] minhash;

		Directory() {
			count = 0;
			minhash = new int[ mhsize ];
			for( int i = 0; i < mhsize; ++i )
				minhash[ i ] = Integer.MAX_VALUE;
		}

		void add( int id ) {
			++count;
			for( int i = 0; i < mhsize; ++i ) {
				int hash = rhfunc[ i ].get( id );
				minhash[ i ] = Math.min( minhash[ i ], hash );
			}
		}
	}

	private void buildIndex() throws IOException {
		long elements = 0;
		long predictCount = 0;
		long starttime = System.nanoTime();
		long totalSigCount = 0;
		// Build an index
		// Count Invokes per each (token, loc) pair
		Map<Integer, Map<IntegerPair, Directory>> invokes = new WYK_HashMap<Integer, Map<IntegerPair, Directory>>();
		// Map<LongIntPair, Integer> invokes = new HashMap<IntegerPairIntPair,
		// Integer>();
		for( Record rec : tableS ) {
			List<Set<IntegerPair>> available2Grams = exact2grams ? rec.getExact2Grams() : rec.get2Grams();
			for( Set<IntegerPair> set : available2Grams )
				totalSigCount += set.size();
			int searchmax = Math.min( available2Grams.size(), maxIndex );
			for( int i = 0; i < searchmax; ++i ) {
				Map<IntegerPair, Directory> curridx_invokes = invokes.get( i );
				if( curridx_invokes == null ) {
					curridx_invokes = new WYK_HashMap<IntegerPair, Directory>();
					invokes.put( i, curridx_invokes );
				}
				for( IntegerPair twogram : available2Grams.get( i ) ) {
					if( Math.random() > dirsampleratio )
						continue;
					Directory dir = curridx_invokes.get( twogram );
					if( dir == null ) {
						dir = new Directory();
						curridx_invokes.put( twogram, dir );
					}
					dir.add( rec.getID() );
				}
			}
		}

		buildIndexTime1 = System.nanoTime() - starttime;
		gamma = ( (double) buildIndexTime1 ) / totalSigCount;
		System.out.println( "Step 1 : " + buildIndexTime1 );
		starttime = System.nanoTime();

		totalSigCount = 0;
		idx = new WYK_HashMap<Integer, Map<IntegerPair, List<Record>>>();
		// idx = new HashMap<Integer, Map<IntegerPair, List<Record>>>();

		// BufferedOutputStream bw = new BufferedOutputStream(
		// new FileOutputStream("asdf"));
		int[] union_sig = new int[ mhsize ];
		for( Record rec : tableR ) {
			List<Set<IntegerPair>> available2Grams = exact2grams ? rec.getExact2Grams() : rec.get2Grams();
			for( Set<IntegerPair> set : available2Grams )
				totalSigCount += set.size();
			int[] range = rec.getCandidateLengths( rec.size() - 1 );
			int minIdx = -1;
			double minUnion = Double.POSITIVE_INFINITY;
			int searchmax = Math.min( range[ 0 ], maxIndex );
			for( int i = 0; i < searchmax; ++i ) {
				if( available2Grams.get( i ).isEmpty() )
					continue;
				Map<IntegerPair, Directory> curridx_invokes = invokes.get( i );
				// There is no invocation count: this is the minimum point
				if( curridx_invokes == null ) {
					minIdx = i;
					minUnion = 0;
					break;
				}

				// prepare for the union signature
				for( int j = 0; j < mhsize; ++j )
					union_sig[ j ] = Integer.MAX_VALUE;
				int invoke = 0;
				int maxsize = 0;
				Directory maxsizedir = null;

				for( IntegerPair twogram : available2Grams.get( i ) ) {
					Directory dir = curridx_invokes.get( twogram );
					if( dir == null )
						continue;
					else if( dir.count > maxsize ) {
						maxsize = dir.count;
						maxsizedir = dir;
					}
					for( int j = 0; j < mhsize; ++j )
						union_sig[ j ] = Math.min( union_sig[ j ], dir.minhash[ j ] );
					invoke += dir.count;
				}
				int inter = 0;
				double union = 0;
				if( maxsizedir != null ) {
					assert ( maxsize != 0 );
					for( int j = 0; j < mhsize; ++j )
						if( union_sig[ j ] == maxsizedir.minhash[ j ] )
							++inter;
					if( inter == 0 )
						union = Math.min( invoke, tableS.size() );
					else
						union = ( (double) maxsizedir.count * mhsize ) / inter;
				}
				if( union < minUnion ) {
					minIdx = i;
					minUnion = union;
				}
			}
			if( minIdx < 0 ) {
				System.out.println( "Error : minIdx < 0 at " + rec.toString() );
				System.exit( 1 );
			}
			assert ( minIdx >= 0 );
			predictCount += minUnion;

			Map<IntegerPair, List<Record>> curridx = idx.get( minIdx );
			if( curridx == null ) {
				curridx = new WYK_HashMap<IntegerPair, List<Record>>();
				// curridx = new HashMap<IntegerPair, List<Record>>();
				idx.put( minIdx, curridx );
			}
			for( IntegerPair twogram : available2Grams.get( minIdx ) ) {
				// write2File(bw, minIdx, twogram, rec.getID());
				if( true ) {
					List<Record> list = curridx.get( twogram );
					if( list == null ) {
						list = new ArrayList<Record>();
						curridx.put( twogram, list );
					}
					list.add( rec );
				}
			}
			elements += available2Grams.get( minIdx ).size();
		}
		// bw.close();
		System.out.println( "Predict : " + predictCount );
		System.out.println( "Idx size : " + elements );
		buildIndexTime2 = System.nanoTime() - starttime;
		System.out.println( "Step 2 : " + buildIndexTime2 );
		delta = ( (double) buildIndexTime2 ) / totalSigCount;

		int sum = 0;
		int ones = 0;
		long count = 0;
		///// Statistics
		for( Map<IntegerPair, List<Record>> curridx : idx.values() ) {
			WYK_HashMap<IntegerPair, List<Record>> tmp = (WYK_HashMap<IntegerPair, List<Record>>) curridx;
			if( sum == 0 )
				tmp.printStat();
			for( List<Record> list : curridx.values() ) {
				if( list.size() == 1 ) {
					++ones;
					continue;
				}
				sum++;
				count += list.size();
			}
		}
		System.out.println( "key-value pairs(all) : " + ( sum + ones ) );
		System.out.println( "iIdx size(all) : " + ( count + ones ) );
		System.out.println( "Rec per idx(all) : " + ( (double) ( count + ones ) ) / ( sum + ones ) );
		System.out.println( "key-value pairs(w/o 1) : " + sum );
		System.out.println( "iIdx size(w/o 1) : " + count );
		System.out.println( "Rec per idx(w/o 1) : " + ( (double) count ) / sum );
		System.out.println( "2Gram retrieval: " + Record.exectime );
		///// Statistics
		sum = 0;
		ones = 0;
		count = 0;
		for( Map<IntegerPair, Directory> curridx : invokes.values() ) {
			WYK_HashMap<IntegerPair, Directory> tmp = (WYK_HashMap<IntegerPair, Directory>) curridx;
			if( sum == 0 )
				tmp.printStat();
			for( Entry<IntegerPair, Directory> list : curridx.entrySet() ) {
				if( list.getValue().count == 1 ) {
					++ones;
					continue;
				}
				sum++;
				count += list.getValue().count;
			}
		}
		System.out.println( "key-value pairs(all) : " + ( sum + ones ) );
		System.out.println( "iIdx size(all) : " + ( count + ones ) );
		System.out.println( "Rec per idx(all) : " + ( (double) ( count + ones ) ) / ( sum + ones ) );
		System.out.println( "key-value pairs(w/o 1) : " + sum );
		System.out.println( "iIdx size(w/o 1) : " + count );
		System.out.println( "Rec per idx(w/o 1) : " + ( (double) count ) / sum );
	}

	static ByteBuffer buffer = ByteBuffer.allocate( 16 );

	@SuppressWarnings( "unused" )
	private static void write2File( BufferedOutputStream bos, int idx, IntegerPair twogram, int id ) throws IOException {
		buffer.clear();
		buffer.putInt( idx );
		buffer.putInt( twogram.i1 );
		buffer.putInt( twogram.i2 );
		buffer.putInt( id );
		bos.write( buffer.array() );
	}

	private List<IntegerPair> join() {
		List<IntegerPair> rslt = new ArrayList<IntegerPair>();
		long starttime = System.nanoTime() - Record.exectime;
		// long totalSigCount = 0;
		long tmp = 0;

		long appliedRules_sum = 0;
		long unioncount = 0;
		for( Record recS : tableS ) {
			List<Set<IntegerPair>> available2Grams = exact2grams ? recS.getExact2Grams() : recS.get2Grams();
			// for (Set<IntegerPair> set : available2Grams)
			// totalSigCount += set.size();
			int[] range = recS.getCandidateLengths( recS.size() - 1 );
			int searchmax = Math.min( available2Grams.size(), maxIndex );
			for( int i = 0; i < searchmax; ++i ) {
				Map<IntegerPair, List<Record>> curridx = idx.get( i );
				if( curridx == null )
					continue;
				List<List<Record>> candidatesList = new ArrayList<List<Record>>();
				for( IntegerPair twogram : available2Grams.get( i ) ) {
					List<Record> tree = curridx.get( twogram );

					if( tree == null )
						continue;
					List<Record> list = new ArrayList<Record>();
					for( Record e : tree )
						if( StaticFunctions.overlap( e.getMinLength(), e.getMaxLength(), range[ 0 ], range[ 1 ] ) )
							list.add( e );
					candidatesList.add( list );
					unioncount += list.size();
				}
				List<Record> candidates = StaticFunctions.union( candidatesList, idComparator );
				if( skipChecking )
					continue;
				else if( checker.getClass() == TopDownHashSetSinglePath_DS_SharedPrefix.class ) {
					// Sort records to utilize similar prefixes
					Collections.sort( candidates );
					Collections.reverse( candidates );
					computePrefixCount( candidates );
				}

				for( Record recR : candidates ) {
					long ruleiters = Validator.niterrules;
					long reccalls = Validator.recursivecalls;
					long entryiters = Validator.niterentry;
					long st = System.nanoTime();
					int compare = checker.isEqual( recR, recS );
					long duration = System.nanoTime() - st;
					ruleiters = Validator.niterrules - ruleiters;
					reccalls = Validator.recursivecalls - reccalls;
					entryiters = Validator.niterentry - entryiters;
					// bw.write(duration + "\t" + compare + "\t" + recR.size() + "\t"
					// + recR.getRuleCount() + "\t" + recR.getFirstRuleCount() + "\t"
					// + recS.size() + "\t" + recS.getRuleCount() + "\t"
					// + recS.getFirstRuleCount() + "\t" + ruleiters + "\t" + reccalls
					// + "\t" + entryiters + "\n");
					joinTime += duration;
					if( compare >= 0 ) {
						rslt.add( new IntegerPair( recR.getID(), recS.getID() ) );
						appliedRules_sum += compare;
					}
				}
			}
		}
		System.out.println( "Avg applied rules : " + appliedRules_sum + "/" + rslt.size() );
		if( checker.getClass() == TopDownHashSetSinglePath_DS_SharedPrefix.class ) {
			System.out.println( "Prefix freq : " + freq );
			System.out.println( "Prefix sumlength : " + sumlength );
		}
		candExtractTime = System.nanoTime() - Record.exectime - starttime - joinTime;
		System.out.println( "Cand extract time : " + candExtractTime + " / " + unioncount + " union" );
		System.out.println( "Join time : " + joinTime + " / " + Validator.checked + " pairs" );
		epsilon = ( (double) joinTime ) / Validator.checked;
		zeta = ( (double) candExtractTime ) / unioncount;

		System.out.println( "Usertime vs Wall-clock time : " + joinTime + " / " + tmp );

		return rslt;
	}

	private long freq = 0;
	private long sumlength = 0;

	private void computePrefixCount( List<Record> list ) {
		int[] prevTokens = new int[ 0 ];
		for( Record r : list ) {
			int[] tokens = r.getTokenArray();
			int bound = Math.min( prevTokens.length, tokens.length );
			for( int i = 0; i < bound; ++i )
				if( tokens[ i ] == prevTokens[ i ] )
					++freq;
			sumlength += tokens.length;
			prevTokens = tokens;
		}
	}

	private void buildIndexSingleSide() {
		long elements = 0;
		long predictCount = 0;
		// Build an index
		// Count Invokes per each (twogram, loc) pair
		Map<Integer, Map<IntegerPair, Integer>> invokes = new WYK_HashMap<Integer, Map<IntegerPair, Integer>>();
		for( Record rec : tableS ) {
			for( int i = 0; i < rec.size(); ++i ) {
				Map<IntegerPair, Integer> curridx_invokes = invokes.get( i );
				if( curridx_invokes == null ) {
					curridx_invokes = new WYK_HashMap<IntegerPair, Integer>();
					invokes.put( i, curridx_invokes );
				}
				IntegerPair twogram = rec.getOriginal2Gram( i );
				Integer count = curridx_invokes.get( twogram );
				if( count == null )
					count = 1;
				else
					count += 1;
				curridx_invokes.put( twogram, count );
			}
		}

		idx = new WYK_HashMap<Integer, Map<IntegerPair, List<Record>>>();
		for( Record rec : tableR ) {
			List<Set<IntegerPair>> available2Grams = exact2grams ? rec.getExact2Grams() : rec.get2Grams();
			int[] range = rec.getCandidateLengths( rec.size() - 1 );
			int minIdx = -1;
			int minInvokes = Integer.MAX_VALUE;
			int searchmax = Math.min( range[ 0 ], maxIndex );
			for( int i = 0; i < searchmax; ++i ) {
				int invoke = 0;
				Map<IntegerPair, Integer> curridx_invokes = invokes.get( i );
				// There is no invocation count: this is the minimum point
				if( curridx_invokes == null ) {
					minIdx = i;
					minInvokes = 0;
					break;
				}
				for( IntegerPair twogram : available2Grams.get( i ) ) {
					Integer count = curridx_invokes.get( twogram );
					if( count != null )
						invoke += count;
				}
				if( invoke < minInvokes ) {
					minIdx = i;
					minInvokes = invoke;
				}
			}

			predictCount += minInvokes;

			Map<IntegerPair, List<Record>> curridx = idx.get( minIdx );
			if( curridx == null ) {
				curridx = new WYK_HashMap<IntegerPair, List<Record>>();
				idx.put( minIdx, curridx );
			}
			for( IntegerPair twogram : available2Grams.get( minIdx ) ) {
				List<Record> list = curridx.get( twogram );
				if( list == null ) {
					list = new ArrayList<Record>();
					curridx.put( twogram, list );
				}
				list.add( rec );
			}
			elements += available2Grams.get( minIdx ).size();
		}
		System.out.println( "Predict : " + predictCount );
		System.out.println( "Idx size : " + elements );

		///// Statistics
		int sum = 0;
		long count = 0;
		for( Map<IntegerPair, List<Record>> curridx : idx.values() ) {
			for( List<Record> list : curridx.values() ) {
				if( list.size() == 1 )
					continue;
				sum++;
				count += list.size();
			}
		}
		System.out.println( "iIdx size : " + count );
		System.out.println( "Rec per idx : " + ( (double) count ) / sum );
	}

	private WYK_HashSet<IntegerPair> joinSingleSide() {
		WYK_HashSet<IntegerPair> rslt = new WYK_HashSet<IntegerPair>();

		long appliedRules_sum = 0;
		for( Record recS : tableS ) {
			int minlength = recS.getMinLength();
			int maxlength = recS.getMaxLength();
			for( int i = 0; i < recS.size(); ++i ) {
				Map<IntegerPair, List<Record>> curridx = idx.get( i );
				if( curridx == null )
					continue;
				IntegerPair twogram = recS.getOriginal2Gram( i );
				List<Record> candidatesList = new ArrayList<Record>();
				List<Record> tree = curridx.get( twogram );

				if( tree == null )
					continue;
				for( Record e : tree )
					if( StaticFunctions.overlap( e.getMinLength(), e.getMaxLength(), minlength, maxlength ) )
						candidatesList.add( e );
				if( skipChecking )
					continue;
				for( Record recR : candidatesList ) {
					int compare = checker.isEqual( recR, recS );
					if( compare >= 0 ) {
						rslt.add( new IntegerPair( recR.getID(), recS.getID() ) );
						appliedRules_sum += compare;
					}
				}
			}
		}
		System.out.println( "Avg applied rules : " + appliedRules_sum + "/" + rslt.size() );

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

	public void run() {
		long startTime = System.nanoTime();
		preprocess( compact, maxIndex, useAutomata );
		System.out.print( "Preprocess finished" );
		System.out.println( " " + ( System.nanoTime() - startTime ) );

		List<RecordPair> list = runWithoutPreprocess();
		try {
			BufferedWriter bw = new BufferedWriter( new FileWriter( outputfile ) );
			for( RecordPair rp : list ) {
				Record s = rp.record1;
				Record t = rp.record2;
				if( !s.equals( t ) )
					bw.write( s.toString() + " == " + t.toString() + "\n" );
			}
			bw.close();
		}
		catch( IOException e ) {
		}
	}

	public List<RecordPair> runWithoutPreprocess() {
		// Retrieve statistics
		statistics();

		long startTime = System.nanoTime();
		if( singleside )
			buildIndexSingleSide();
		else
			try {
				buildIndex();
			}
			catch( Exception e ) {
				e.printStackTrace();
				System.exit( 0 );
			}
		buildIndexTime = System.nanoTime() - startTime;
		System.out.println( "Building Index finished " + buildIndexTime );
		System.gc();

		startTime = System.nanoTime();
		Collection<IntegerPair> rslt = ( singleside ? joinSingleSide() : join() );
		joinTime = System.nanoTime() - startTime;
		System.out.println( "Join finished " + joinTime + " ns" );
		System.out.println( rslt.size() );

		Map<Integer, Record> tableRmap = new WYK_HashMap<Integer, Record>();
		Map<Integer, Record> tableSmap = new WYK_HashMap<Integer, Record>();
		for( Record r : tableR )
			tableRmap.put( r.getID(), r );
		for( Record s : tableS )
			tableSmap.put( s.getID(), s );
		List<RecordPair> rlist = new ArrayList<RecordPair>();
		for( IntegerPair ip : rslt ) {
			Record r = tableRmap.get( ip.i1 );
			Record s = tableSmap.get( ip.i2 );
			rlist.add( new RecordPair( r, s ) );
		}
		Collections.sort( rlist );
		return rlist;
	}

	public void clearIndex() {
		if( idx != null )
			idx.clear();
		idx = null;
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
		compact = params.isCompact();
		checker = params.getValidator();
		exact2grams = params.isExact2Grams();

		long startTime = System.currentTimeMillis();
		JoinH2GramNoIntervalTree2 inst = new JoinH2GramNoIntervalTree2( Rulefile, Rfile, Sfile );
		System.out.print( "Constructor finished" );
		System.out.println( " " + ( System.currentTimeMillis() - startTime ) );
		inst.run();

		Validator.printStats();
	}
}
