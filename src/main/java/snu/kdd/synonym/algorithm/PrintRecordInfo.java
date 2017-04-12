package snu.kdd.synonym.algorithm;

import java.io.IOException;

import mine.Record;
import snu.kdd.synonym.tools.StatContainer;
import tools.Rule;
import tools.RuleTrie;

public class PrintRecordInfo extends AlgorithmTemplate {
	RuleTrie ruletrie;

	protected PrintRecordInfo( String rulefile, String Rfile, String Sfile, String outputPath ) throws IOException {
		super( rulefile, Rfile, Sfile, outputPath );

		Record.setStrList( strlist );
		ruletrie = new RuleTrie( getRulelist() );
		Record.setRuleTrie( ruletrie );

		this.preprocess( true, Integer.MAX_VALUE, false );
	}

	public void printInfo( int id ) {
		Record r = tableT.get( id );
		System.out.println( r );

		int length = r.getTokenArray().length;

		for( int i = 0; i < length; i++ ) {
			Rule[] rlist = r.getApplicableRules( i );
			for( int x = 0; x < rlist.length; x++ ) {
				System.out.println( rlist[ x ].toTextString( Record.strlist ) );
			}
		}

	}

	public static void main( String args[] ) throws IOException {
		String rulefile = args[ 0 ];
		String Rfile = args[ 1 ];
		String Sfile = args[ 2 ];
		String outputPath = args[ 3 ];

		PrintRecordInfo info = new PrintRecordInfo( rulefile, Rfile, Sfile, outputPath );

		info.printInfo( Integer.parseInt( args[ 4 ] ) );
	}

	@Override
	public String getName() {
		return "PrintRecordInfo";
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

	@Override
	public void run( String[] args, StatContainer stat ) {
		// TODO Auto-generated method stub

	}

}
