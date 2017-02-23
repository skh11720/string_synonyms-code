package mine;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import tools.Algorithm;
import tools.IntegerMap;
import tools.IntegerPair;
import tools.IntegerSet;
import tools.Parameters;
import tools.Rule_ACAutomata;
import tools.StaticFunctions;
import tools.WYK_HashSet;
import validator.Validator;

public class JoinD extends Algorithm {
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
	ArrayList<IntegerMap<IntervalTreeRW<Integer, Record>>> idx;

	protected JoinD( String rulefile, String Rfile, String Sfile ) throws IOException {
		super( rulefile, Rfile, Sfile );
		idComparator = new RecordIDComparator();
	}

	private void preprocess() {
		Rule_ACAutomata automata = new Rule_ACAutomata( rulelist );

		long currentTime = System.currentTimeMillis();
		// Preprocess each records in R
		for( Record rec : tableR ) {
			rec.preprocessRules( automata, true );
		}
		long time = System.currentTimeMillis() - currentTime;
		System.out.println( "Preprocess rules : " + time );

		currentTime = System.currentTimeMillis();
		for( Record rec : tableR ) {
			rec.preprocessLengths();
		}
		time = System.currentTimeMillis() - currentTime;
		System.out.println( "Preprocess lengths: " + time );

		currentTime = System.currentTimeMillis();
		for( Record rec : tableR ) {
			rec.preprocessAvailableTokens( maxIndex );
		}
		time = System.currentTimeMillis() - currentTime;
		System.out.println( "Preprocess available tokens: " + time );

		currentTime = System.currentTimeMillis();
		for( Record rec : tableR ) {
			rec.preprocessEstimatedRecords();
		}
		time = System.currentTimeMillis() - currentTime;
		System.out.println( "Preprocess est records: " + time );

		// Preprocess each records in S
		for( Record rec : tableS ) {
			rec.preprocessRules( automata, true );
			rec.preprocessLengths();
			rec.preprocessAvailableTokens( maxIndex );
			rec.preprocessEstimatedRecords();
		}
	}

	private void buildIndex() {
		long elements = 0;
		// Build an index

		idx = new ArrayList<IntegerMap<IntervalTreeRW<Integer, Record>>>();
		for( int i = 0; i < maxIndex; ++i )
			idx.add( new IntegerMap<IntervalTreeRW<Integer, Record>>() );
		for( Record rec : tableR ) {
			IntegerSet[] availableTokens = rec.getAvailableTokens();
			int[] range = rec.getCandidateLengths( rec.size() - 1 );
			int boundary = Math.min( range[ 1 ], maxIndex );
			for( int i = 0; i < boundary; ++i ) {
				IntegerMap<IntervalTreeRW<Integer, Record>> map = idx.get( i );
				for( int token : availableTokens[ i ] ) {
					IntervalTreeRW<Integer, Record> list = map.get( token );
					if( list == null ) {
						list = new IntervalTreeRW<Integer, Record>();
						map.put( token, list );
					}
					list.insert( range[ 0 ], range[ 1 ], rec );
				}
				elements += availableTokens[ i ].size();
			}
		}
		System.out.println( "Idx size : " + elements );

		// Statistics
		int sum = 0;
		long count = 0;
		for( IntegerMap<IntervalTreeRW<Integer, Record>> map : idx ) {
			for( IntervalTreeRW<Integer, Record> list : map.values() ) {
				if( list.size() == 1 )
					continue;
				sum++;
				count += list.size();
			}
		}
		System.out.println( "iIdx size : " + count );
		System.out.println( "Rec per idx : " + ( (double) count ) / sum );
	}

	private WYK_HashSet<IntegerPair> join() {
		WYK_HashSet<IntegerPair> rslt = new WYK_HashSet<IntegerPair>();
		int count = 0;

		for( Record recS : tableS ) {
			List<List<Record>> candidatesList = new ArrayList<List<Record>>();
			IntegerSet[] availableTokens = recS.getAvailableTokens();
			int[] range = recS.getCandidateLengths( recS.size() - 1 );
			int boundary = Math.min( range[ 0 ], maxIndex );
			for( int i = 0; i < boundary; ++i ) {
				List<List<Record>> ithCandidates = new ArrayList<List<Record>>();
				IntegerMap<IntervalTreeRW<Integer, Record>> map = idx.get( i );
				for( int token : availableTokens[ i ] ) {
					IntervalTreeRW<Integer, Record> tree = map.get( token );
					if( tree == null )
						continue;
					List<Record> candidates = tree.search( range[ 0 ], range[ 1 ] );
					Collections.sort( candidates, idComparator );
					ithCandidates.add( candidates );
				}
				candidatesList.add( StaticFunctions.union( ithCandidates, idComparator ) );
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
		System.out.println( "comparisions : " + count );

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
		preprocess();
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
			BufferedWriter bw = new BufferedWriter( new FileWriter( "rslt.txt" ) );
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
		JoinD inst = new JoinD( Rulefile, Rfile, Sfile );
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
	public void run( String[] args ) {
		// TODO Auto-generated method stub
		
	}
}
