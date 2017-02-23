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
import tools.StaticFunctions;
import tools.WYK_HashSet;
import validator.Validator;

public class JoinB extends Algorithm {
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
	IntegerMap<IntervalTreeRW<Integer, Record>> idx;

	protected JoinB( String rulefile, String Rfile, String Sfile ) throws IOException {
		super( rulefile, Rfile, Sfile );
		idComparator = new RecordIDComparator();
	}

	private void buildIndex() {
		long elements = 0;
		// Build an index

		idx = new IntegerMap<IntervalTreeRW<Integer, Record>>();
		for( Record rec : tableR ) {
			IntegerSet[] availableTokens = rec.getAvailableTokens();
			int[] range = rec.getCandidateLengths( rec.size() - 1 );
			for( int token : availableTokens[ 0 ] ) {
				IntervalTreeRW<Integer, Record> list = idx.get( token );
				if( list == null ) {
					list = new IntervalTreeRW<Integer, Record>();
					idx.put( token, list );
				}
				list.insert( range[ 0 ], range[ 1 ], rec );
			}
			elements += availableTokens[ 0 ].size();
		}
		System.out.println( "Idx size : " + elements );

		// Statistics
		int sum = 0;
		long count = 0;
		for( IntervalTreeRW<Integer, Record> list : idx.values() ) {
			if( list.size() == 1 )
				continue;
			sum++;
			count += list.size();
		}
		System.out.println( "iIdx size : " + count );
		System.out.println( "Rec per idx : " + ( (double) count ) / sum );
	}

	private WYK_HashSet<IntegerPair> join() {
		WYK_HashSet<IntegerPair> rslt = new WYK_HashSet<IntegerPair>();

		for( Record recS : tableS ) {
			List<ArrayList<Record>> candidatesList = new ArrayList<ArrayList<Record>>();

			IntegerSet[] availableTokens = recS.getAvailableTokens();
			int[] range = recS.getCandidateLengths( recS.size() - 1 );
			for( int token : availableTokens[ 0 ] ) {
				IntervalTreeRW<Integer, Record> tree = idx.get( token );

				if( tree == null )
					continue;
				ArrayList<Record> candidates = tree.search( range[ 0 ], range[ 1 ] );
				Collections.sort( candidates, idComparator );
				candidatesList.add( candidates );
			}

			List<Record> candidates = StaticFunctions.union( candidatesList, idComparator );

			if( skipChecking )
				continue;
			for( Record recR : candidates ) {
				int compare = checker.isEqual( recR, recS );
				if( compare >= 0 )
					rslt.add( new IntegerPair( recR.getID(), recS.getID() ) );
			}
		}

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
		JoinB inst = new JoinB( Rulefile, Rfile, Sfile );
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
