package mine.hybrid;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
 */
public class Hybrid2GramA1 extends Algorithm {
	static boolean useAutomata = true;
	static boolean skipChecking = false;
	static int maxIndex = Integer.MAX_VALUE;
	static boolean compact = false;
	static int joinThreshold;
	static boolean singleside;
	static Validator checker;

	RecordIDComparator idComparator;
	RuleTrie ruletrie;

	static String outputfile;

	/**
	 * Key: (token, index) pair<br/>
	 * Value: (min, max, record) triple
	 */
	/**
	 * Index of the records in R for the strings in S which has less or equal to
	 * 'threshold' 1-expandable strings
	 */
	Map<Integer, Map<IntegerPair, List<Record>>> SL_TH_idx;
	/**
	 * Index of the records in R for the strings in S which has more than
	 * 'threshold' 1-expandable strings
	 */
	Map<Integer, Map<IntegerPair, List<Record>>> SH_T_idx;
	/**
	 * List of 1-expandable strings
	 */
	Map<Record, List<Integer>> setR;
	private static final WrappedInteger ONE = new WrappedInteger( 1 );

	protected Hybrid2GramA1( String rulefile, String Rfile, String Sfile ) throws IOException {
		super( rulefile, Rfile, Sfile );
		idComparator = new RecordIDComparator();
		ruletrie = new RuleTrie( rulelist );
	}

	private void buildJoinMinIndex() {
		Runtime runtime = Runtime.getRuntime();

		long SH_T_elements = 0;
		long SL_TH_elements = 0;
		// Build an index
		// Count Invokes per each (token, loc) pair
		Map<Integer, Map<IntegerPair, WrappedInteger>> SH_T_invokes = new WYK_HashMap<Integer, Map<IntegerPair, WrappedInteger>>();
		Map<Integer, Map<IntegerPair, WrappedInteger>> SL_TH_invokes = new WYK_HashMap<Integer, Map<IntegerPair, WrappedInteger>>();

		SL_TH_idx = new WYK_HashMap<Integer, Map<IntegerPair, List<Record>>>();
		SH_T_idx = new WYK_HashMap<Integer, Map<IntegerPair, List<Record>>>();

		// Actually, tableT
		for( Record rec : tableS ) {
			List<Set<IntegerPair>> available2Grams = rec.get2Grams();
			int searchmax = Math.min( available2Grams.size(), maxIndex );
			boolean is_TH_Record = rec.getEstNumRecords() > joinThreshold;
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
			boolean is_SH_record = rec.getEstNumRecords() > joinThreshold;
			int[] invokearr = new int[ searchmax ];

			Map<Integer, Map<IntegerPair, WrappedInteger>> invokes = is_SH_record ? SH_T_invokes : SL_TH_invokes;
			Map<Integer, Map<IntegerPair, List<Record>>> idx = is_SH_record ? SH_T_idx : SL_TH_idx;

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

			Map<IntegerPair, List<Record>> curr_idx = idx.get( minIdx );
			if( curr_idx == null ) {
				curr_idx = new WYK_HashMap<IntegerPair, List<Record>>();
				idx.put( minIdx, curr_idx );
			}
			for( IntegerPair twogram : available2Grams.get( minIdx ) ) {
				List<Record> list = curr_idx.get( twogram );
				if( list == null ) {
					list = new ArrayList<Record>();
					curr_idx.put( twogram, list );
				}
				list.add( rec );
			}
			int elements = available2Grams.get( minIdx ).size();
			if( is_SH_record ) {
				SH_T_elements += elements;
			}
			else {
				SL_TH_elements += elements;
			}
		}
		System.out.println( "Bigram retrieval : " + Record.exectime );
		System.out.println( ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 + "MB used" );

		System.out.println( "SH_T idx size : " + SH_T_elements );
		System.out.println( "SL_TH idx size : " + SL_TH_elements );
		System.out.println( WrappedInteger.count + " Wrapped Integers" );
		for( Entry<Integer, Map<IntegerPair, List<Record>>> e : SH_T_idx.entrySet() ) {
			System.out.println( e.getKey() + " : " + e.getValue().size() );
		}
	}

	private void clearJoinMinIndex() {
		for( Map<IntegerPair, List<Record>> map : SL_TH_idx.values() )
			map.clear();
		SL_TH_idx.clear();
		for( Map<IntegerPair, List<Record>> map : SH_T_idx.values() )
			map.clear();
		SH_T_idx.clear();
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

		clearJoinMinIndex();

		startTime = System.currentTimeMillis();
		buildNaiveIndex();
		System.out.print( "Building JoinMin Index finished" );
		System.out.println( " " + ( System.currentTimeMillis() - startTime ) );

		long time3 = System.currentTimeMillis();
		for( Record s : tableS ) {
			if( s.getEstNumRecords() > joinThreshold )
				continue;
			else
				searchEquivsByNaive1Expansion( s, rslt );
		}
		time3 = System.currentTimeMillis() - time3;

		System.out.println( "Avg applied rules : " + appliedRules_sum + "/" + rslt.size() );
		System.out.println( "SH_T : " + time1 );
		System.out.println( "SL_TH : " + time2 );
		System.out.println( "SL_TL : " + time3 );

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
				for( Record rec : tree )
					if( StaticFunctions.overlap( rec.getMinLength(), rec.getMaxLength(), range[ 0 ], range[ 1 ] ) )
						list.add( rec );
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

	public void run() {
		long startTime = System.currentTimeMillis();
		preprocess( compact, maxIndex, useAutomata );
		System.out.print( "Preprocess finished" );
		System.out.println( " " + ( System.currentTimeMillis() - startTime ) );

		// Retrieve statistics
		statistics();

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
		joinThreshold = params.getJoinThreshold();
		singleside = params.isSingleside();
		checker = params.getValidator();

		long startTime = System.currentTimeMillis();
		Hybrid2GramA1 inst = new Hybrid2GramA1( Rulefile, Rfile, Sfile );
		System.out.print( "Constructor finished" );
		System.out.println( " " + ( System.currentTimeMillis() - startTime ) );
		inst.run();
		Validator.printStats();
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

	@Override
	public String getName() {
		return "Hybrid2GramA1";
	}

	@Override
	public void run( String[] args, StatContainer stat ) {
		// TODO Auto-generated method stub

	}
}
