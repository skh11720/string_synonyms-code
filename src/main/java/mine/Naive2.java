package mine;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import snu.kdd.synonym.tools.StatContainer;
import tools.Algorithm;
import tools.IntegerPair;
import tools.RuleTrie;
import tools.Rule_ACAutomata;
import tools.WYK_HashSet;

public class Naive2 extends Algorithm {
	/**
	 * Map each record to its own index
	 */
	HashMap<Record, Integer> rec2idx;
	Rule_ACAutomata automata;
	RuleTrie ruletrie;

	static int threshold = 1000;

	protected Naive2( String rulefile, String Rfile, String Sfile ) throws IOException {
		super( rulefile, Rfile, Sfile );
		rec2idx = new HashMap<Record, Integer>();
		for( int i = 0; i < tableR.size(); ++i )
			rec2idx.put( tableR.get( i ), i );
	}

	private List<IntegerPair> join() {
		automata = new Rule_ACAutomata( rulelist );
		ruletrie = new RuleTrie( rulelist );
		List<IntegerPair> rslt = new ArrayList<IntegerPair>();

		for( int idxS = 0; idxS < tableS.size(); ++idxS ) {
			Record recS = tableS.get( idxS );
			recS.preprocessRules( automata, false );
			recS.preprocessEstimatedRecords();
			long est = recS.getEstNumRecords();
			if( est > threshold )
				continue;
			ArrayList<Record> expanded = recS.expandAll( ruletrie );
			for( Record exp : expanded ) {
				ArrayList<Record> double_expanded = exp.expandAll( ruletrie );
				WYK_HashSet<Integer> candidates = new WYK_HashSet<Integer>();
				for( Record dexp : double_expanded ) {
					Integer idx = rec2idx.get( dexp );
					if( idx == null )
						continue;
					candidates.add( idx );
				}
				for( Integer idx : candidates ) {
					rslt.add( new IntegerPair( idx, idxS ) );
				}
			}
		}

		return rslt;
	}

	public void run() {
		long startTime = System.currentTimeMillis();
		List<IntegerPair> rslt = join();
		System.out.print( "Join finished" );
		System.out.println( " " + ( System.currentTimeMillis() - startTime ) );
		System.out.println( rslt.size() );

		try {
			BufferedWriter bw = new BufferedWriter( new FileWriter( "rslt.txt" ) );
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

	public static void main( String[] args ) throws IOException {
		if( args.length != 4 ) {
			printUsage();
			return;
		}
		String Rfile = args[ 0 ];
		String Sfile = args[ 1 ];
		String Rulefile = args[ 2 ];
		Naive2.threshold = Integer.valueOf( args[ 3 ] );

		long startTime = System.currentTimeMillis();
		Naive2 inst = new Naive2( Rulefile, Rfile, Sfile );
		System.out.print( "Constructor finished" );
		System.out.println( " " + ( System.currentTimeMillis() - startTime ) );
		inst.run();
	}

	private static void printUsage() {
		System.out.println( "Usage : <R file> <S file> <Rule file> <exp threshold>" );
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
