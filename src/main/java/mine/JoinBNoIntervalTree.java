package mine;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import tools.Algorithm;
import tools.IntIntRecordTriple;
import tools.IntegerMap;
import tools.IntegerPair;
import tools.IntegerSet;
import tools.Parameters;
import tools.StaticFunctions;
import tools.WYK_HashSet;
import validator.Validator;

public class JoinBNoIntervalTree extends Algorithm {
	static boolean useAutomata = true;
	static boolean skipChecking = false;
	static boolean compact = false;
	static String outputfile;

	RecordIDComparator idComparator;

	static Validator checker;

	/**
	 * Key: token<br/>
	 * Value IntervalTree Key: length of record (min, max)<br/>
	 * Value IntervalTree Value: record
	 */
	IntegerMap<ArrayList<IntIntRecordTriple>> idx;

	protected JoinBNoIntervalTree( String rulefile, String Rfile, String Sfile ) throws IOException {
		super( rulefile, Rfile, Sfile );
		idComparator = new RecordIDComparator();
	}

	private void buildIndex() {
		long elements = 0;
		// Build an index

		idx = new IntegerMap<ArrayList<IntIntRecordTriple>>();
		for( Record rec : tableR ) {
			// All available tokens at the first position
			IntegerSet[] availableTokens = rec.getAvailableTokens();
			// All available equivalent string lengths
			int[] range = rec.getCandidateLengths( rec.size() - 1 );
			for( int token : availableTokens[ 0 ] ) {
				ArrayList<IntIntRecordTriple> list = idx.get( token );
				if( list == null ) {
					list = new ArrayList<IntIntRecordTriple>();
					idx.put( token, list );
				}
				list.add( new IntIntRecordTriple( range[ 0 ], range[ 1 ], rec ) );
			}
			// Number of replicas of current record
			elements += availableTokens[ 0 ].size();
		}
		System.out.println( "Idx size : " + elements );

		// Statistics
		System.out.println( "iIdx key-value pairs: " + idx.size() );
		int sum = 0;
		int singlelistsize = 0;
		long count = 0;
		for( ArrayList<IntIntRecordTriple> list : idx.values() ) {
			if( list.size() == 1 ) {
				++singlelistsize;
				continue;
			}
			sum++;
			count += list.size();
		}
		System.out.println( "Single value list size : " + singlelistsize );
		System.out.println( "iIdx size : " + count );
		System.out.println( "Rec per key-value pair : " + ( (double) count ) / sum );
	}

	private WYK_HashSet<IntegerPair> join() {
		WYK_HashSet<IntegerPair> rslt = new WYK_HashSet<IntegerPair>();

		// Union하는 set의 평균 개수 및 동시에 union하는 set의 개수
		long set_union_count = 0;
		long set_union_sum = 0;
		long set_union_setsize_sum = 0;
		// inverted index의 key 조회횟수의 합
		long sum = 0;
		for( Record recS : tableS ) {
			List<List<Record>> candidatesList = new ArrayList<List<Record>>();

			IntegerSet[] availableTokens = recS.getAvailableTokens();
			int asdf = availableTokens[ 0 ].size();
			sum += asdf;
			int[] range = recS.getCandidateLengths( recS.size() - 1 );
			for( int token : availableTokens[ 0 ] ) {
				ArrayList<IntIntRecordTriple> tree = idx.get( token );

				if( tree == null )
					continue;
				List<Record> list = new ArrayList<Record>();
				for( IntIntRecordTriple e : tree )
					if( StaticFunctions.overlap( e.min, e.max, range[ 0 ], range[ 1 ] ) )
						list.add( e.rec );
				set_union_setsize_sum += list.size();
				candidatesList.add( list );
			}

			List<Record> candidates = StaticFunctions.union( candidatesList, idComparator );
			set_union_sum = candidatesList.size();
			++set_union_count;

			if( skipChecking )
				continue;
			for( Record recR : candidates ) {
				int compare = checker.isEqual( recR, recS );
				if( compare >= 0 )
					rslt.add( new IntegerPair( recR.getID(), recS.getID() ) );
			}
		}
		System.out.println( "Key membership check : " + sum );
		System.out.println( "set_union_count: " + set_union_count );
		System.out.println( "set_union_sum: " + set_union_sum );
		System.out.println( "set_union_setsize_sum: " + set_union_setsize_sum );

		return rslt;
	}

	@SuppressWarnings( "unused" )
	private List<Record> mergeCandidatesWithHashSet( List<ArrayList<Record>> list ) {
		IntegerMap<Record> set = new IntegerMap<Record>();
		for( ArrayList<Record> candidates : list )
			for( Record rec : candidates )
				set.put( rec.getID(), rec );
		List<Record> candidates = new ArrayList<Record>();
		for( Record rec : set.values() )
			candidates.add( rec );
		return candidates;
	}

	public void run() {
		long startTime = System.currentTimeMillis();
		preprocess( compact, 1, useAutomata );
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

		try {
			BufferedWriter bw = new BufferedWriter( new FileWriter( outputfile ) );
			HashMap<Integer, ArrayList<Record>> tmp = new HashMap<Integer, ArrayList<Record>>();
			for( IntegerPair ip : rslt ) {
				if( !tmp.containsKey( ip.i1 ) )
					tmp.put( ip.i1, new ArrayList<Record>() );
				if( ip.i1 != ip.i2 )
					tmp.get( ip.i1 ).add( tableS.get( ip.i2 ) );
			}
			for( int i = 0; i < tableR.size(); ++i ) {
				if( !tmp.containsKey( i ) || tmp.get( i ).size() == 0 )
					continue;
				bw.write( tableR.get( i ).toString() + "\t" );
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

		// Setup parameters
		useAutomata = params.isUseACAutomata();
		skipChecking = params.isSkipChecking();
		compact = params.isCompact();
		checker = params.getValidator();

		long startTime = System.currentTimeMillis();
		JoinBNoIntervalTree inst = new JoinBNoIntervalTree( Rulefile, Rfile, Sfile );
		System.out.print( "Constructor finished" );
		System.out.println( " " + ( System.currentTimeMillis() - startTime ) );
		inst.run();
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
	public void run( String[] args ) {
		// TODO Auto-generated method stub
		
	}
}
