package mine;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import snu.kdd.synonym.tools.StatContainer;
import tools.Algorithm;
import tools.IntIntRecordTriple;
import tools.IntegerMap;
import tools.IntegerPair;
import tools.IntegerSet;
import tools.Parameters;
import tools.StaticFunctions;
import tools.WYK_HashSet;
import validator.Validator;

public class JoinDNoIntervalTree extends Algorithm {
	static boolean useAutomata = true;
	static boolean skipChecking = false;
	static boolean compact = false;
	static String outputfile;
	RecordIDComparator idComparator;
	static int maxIndex = 3;
	static Validator checker;
	/**
	 * Key: token<br/>
	 * Value IntervalTree Key: length of record (min, max)<br/>
	 * Value IntervalTree Value: record
	 */
	ArrayList<IntegerMap<ArrayList<IntIntRecordTriple>>> idx;

	protected JoinDNoIntervalTree( String rulefile, String Rfile, String Sfile ) throws IOException {
		super( rulefile, Rfile, Sfile );
		idComparator = new RecordIDComparator();
	}

	private void buildIndex() {
		long elements = 0;
		// Build an index

		idx = new ArrayList<IntegerMap<ArrayList<IntIntRecordTriple>>>();
		for( int i = 0; i < maxIndex; ++i )
			idx.add( new IntegerMap<ArrayList<IntIntRecordTriple>>() );
		for( Record rec : tableT ) {
			IntegerSet[] availableTokens = rec.getAvailableTokens();
			int[] range = rec.getCandidateLengths( rec.size() - 1 );
			int boundary = Math.min( range[ 1 ], maxIndex );
			for( int i = 0; i < boundary; ++i ) {
				IntegerMap<ArrayList<IntIntRecordTriple>> map = idx.get( i );
				for( int token : availableTokens[ i ] ) {
					ArrayList<IntIntRecordTriple> list = map.get( token );
					if( list == null ) {
						list = new ArrayList<IntIntRecordTriple>();
						map.put( token, list );
					}
					list.add( new IntIntRecordTriple( range[ 0 ], range[ 1 ], rec ) );
				}
				elements += availableTokens[ i ].size();
			}
		}
		System.out.println( "Idx size : " + elements );

		for( int i = 0; i < maxIndex; ++i ) {
			IntegerMap<ArrayList<IntIntRecordTriple>> ithidx = idx.get( i );
			System.out.println( i + "th iIdx key-value pairs: " + ithidx.size() );
			// Statistics
			int sum = 0;
			int singlelistsize = 0;
			long count = 0;
			long sqsum = 0;
			for( ArrayList<IntIntRecordTriple> list : ithidx.values() ) {
				sqsum += list.size() * list.size();
				if( list.size() == 1 ) {
					++singlelistsize;
					continue;
				}
				sum++;
				count += list.size();
			}
			System.out.println( i + "th Single value list size : " + singlelistsize );
			System.out.println( i + "th iIdx size : " + count );
			System.out.println( i + "th Rec per idx : " + ( (double) count ) / sum );
			System.out.println( i + "th Sqsum : " + sqsum );
		}
	}

	private WYK_HashSet<IntegerPair> join() {
		WYK_HashSet<IntegerPair> rslt = new WYK_HashSet<IntegerPair>();
		long count = 0;

		long cand_sum[] = new long[ maxIndex ];
		long cand_sum_afterprune[] = new long[ maxIndex ];
		long cand_sum_afterunion[] = new long[ maxIndex ];
		int count_cand[] = new int[ maxIndex ];
		int count_empty[] = new int[ maxIndex ];
		for( Record recS : tableS ) {
			List<List<Record>> candidatesList = new ArrayList<List<Record>>();
			IntegerSet[] availableTokens = recS.getAvailableTokens();

			int[] range = recS.getCandidateLengths( recS.size() - 1 );
			int boundary = Math.min( range[ 0 ], maxIndex );
			for( int i = 0; i < boundary; ++i ) {
				List<List<Record>> ithCandidates = new ArrayList<List<Record>>();
				IntegerMap<ArrayList<IntIntRecordTriple>> map = idx.get( i );
				for( int token : availableTokens[ i ] ) {
					ArrayList<IntIntRecordTriple> tree = map.get( token );
					if( tree == null ) {
						++count_empty[ i ];
						continue;
					}
					cand_sum[ i ] += tree.size();
					++count_cand[ i ];
					List<Record> list = new ArrayList<Record>();
					for( IntIntRecordTriple e : tree )
						if( StaticFunctions.overlap( e.min, e.max, range[ 0 ], range[ 1 ] ) )
							list.add( e.rec );
					ithCandidates.add( list );
					cand_sum_afterprune[ i ] += list.size();
				}
				List<Record> union = StaticFunctions.union( ithCandidates, idComparator );
				candidatesList.add( union );
				cand_sum_afterunion[ i ] += union.size();
			}
			List<Record> candidates = StaticFunctions.intersection( candidatesList, idComparator );
			count += candidates.size();

			if( skipChecking )
				continue;
			for( Record recR : candidates ) {
				int compare = checker.isEqual( recR, recS );
				if( compare >= 0 )
					rslt.add( new IntegerPair( recR.getID(), recS.getID() ) );
			}
		}
		for( int i = 0; i < maxIndex; ++i ) {
			System.out.println( "Avg candidates(w/o empty) : " + cand_sum[ i ] + "/" + count_cand[ i ] );
			System.out.println( "Avg candidates(w/o empty, after prune) : " + cand_sum_afterprune[ i ] + "/" + count_cand[ i ] );
			System.out.println( "Avg candidates(w/o empty, after union) : " + cand_sum_afterunion[ i ] + "/" + count_cand[ i ] );
			System.out.println( "Empty candidates : " + count_empty[ i ] );
		}
		System.out.println( "comparisions : " + count );

		return rslt;
	}

	public void run() {
		long startTime = System.currentTimeMillis();
		preprocess( compact, maxIndex, useAutomata );
		System.out.print( "Preprocess finished" );
		System.out.println( " " + ( System.currentTimeMillis() - startTime ) );

		startTime = System.currentTimeMillis();
		buildIndex();
		System.out.print( "Building Index finished" );
		System.out.println( " " + ( System.currentTimeMillis() - startTime ) );

		startTime = System.currentTimeMillis();
		WYK_HashSet<IntegerPair> rslt = join();
		System.out.print( "Join finished" );
		System.out.println( " " + ( System.currentTimeMillis() - startTime ) );
		System.out.println( rslt.size() );

		System.out.println( "Set union items:" + StaticFunctions.union_item_counter );
		System.out.println( "Set union cmps:" + StaticFunctions.union_cmp_counter );
		System.out.println( "Set inter items:" + StaticFunctions.inter_item_counter );
		System.out.println( "Set inter cmps:" + StaticFunctions.inter_cmp_counter );

		try {
			BufferedWriter bw = new BufferedWriter( new FileWriter( outputfile ) );
			HashMap<Integer, ArrayList<Record>> tmp = new HashMap<Integer, ArrayList<Record>>();
			for( IntegerPair ip : rslt ) {
				if( !tmp.containsKey( ip.i1 ) )
					tmp.put( ip.i1, new ArrayList<Record>() );
				if( ip.i1 != ip.i2 )
					tmp.get( ip.i1 ).add( tableS.get( ip.i2 ) );
			}
			for( int i = 0; i < tableT.size(); ++i ) {
				if( !tmp.containsKey( i ) || tmp.get( i ).size() == 0 )
					continue;
				bw.write( tableT.get( i ).toString() + "\t" );
				bw.write( tmp.get( i ).toString() + "\n" );
			}
			bw.close();
		}
		catch( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main( String[] args ) throws IOException {
		Parameters params = Parameters.parseArgs( args );
		String Rfile = params.getInputX();
		String Sfile = params.getInputY();
		String Rulefile = params.getInputRules();
		outputfile = params.getOutput();
		maxIndex = params.getMaxIndex();

		// Setup parameters
		useAutomata = params.isUseACAutomata();
		skipChecking = params.isSkipChecking();
		compact = params.isCompact();
		checker = params.getValidator();

		long startTime = System.currentTimeMillis();
		JoinDNoIntervalTree inst = new JoinDNoIntervalTree( Rulefile, Rfile, Sfile );
		System.out.print( "Constructor finished" );
		System.out.println( " " + ( System.currentTimeMillis() - startTime ) );
		inst.run();

		Validator.printStats();
	}

	@Override
	public String getVersion() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void run( String[] args, StatContainer stat ) {
		// TODO Auto-generated method stub

	}
}
