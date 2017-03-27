package analysis;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import mine.Record;
import snu.kdd.synonym.tools.StatContainer;
import tools.Algorithm;
import tools.RuleTrie;
import tools.Rule_ACAutomata;

public class ExtractLessThanThreshold extends Algorithm {
	Rule_ACAutomata ruleatm;
	RuleTrie trie;

	protected ExtractLessThanThreshold( String rulefile, String Rfile, String Sfile ) throws IOException {
		super( rulefile, Rfile, Sfile );
		ruleatm = new Rule_ACAutomata( rulelist );
		trie = new RuleTrie( rulelist );
		Record.setRuleTrie( trie );
	}

	public static void main( String[] args ) throws IOException {
		if( args.length != 4 ) {
			printUsage();
			System.exit( 0 );
		}
		ExtractLessThanThreshold inst = new ExtractLessThanThreshold( args[ 1 ], args[ 0 ], args[ 0 ] );
		int threshold = Integer.parseInt( args[ 2 ] );
		inst.extract( threshold, args[ 3 ] );
	}

	private static void printUsage() {
		System.out.println( "Usage : [Data] [Rule] [Threshold] [Output]" );
	}

	private void extract( int threshold, String outputfile ) throws IOException {
		BufferedWriter bw = new BufferedWriter( new FileWriter( outputfile ) );
		for( Record str : tableT ) {
			str.preprocessRules( ruleatm, false );
			str.preprocessEstimatedRecords();
			long est = str.getEstNumRecords();
			if( est <= threshold )
				bw.write( str.toString( strlist ) + "\n" );
		}
		bw.close();
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

	@Override
	public String getName() {
		return "ExtractLessThanThreshold";
	}

	@Override
	public void run( String[] args, StatContainer stat ) {
		// TODO Auto-generated method stub

	}
}
