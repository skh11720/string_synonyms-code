package snu.kdd.synonym.algorithm;

import java.io.IOException;

import mine.Record;
import snu.kdd.synonym.tools.StatContainer;
import tools.RuleTrie;

public class PrintRecordInfo extends AlgorithmTemplate {
	RuleTrie ruletrie;

	protected PrintRecordInfo( String rulefile, String Rfile, String Sfile, String outputPath ) throws IOException {
		super( rulefile, Rfile, Sfile, outputPath );

		Record.setStrList( strlist );
		ruletrie = new RuleTrie( getRulelist() );
		Record.setRuleTrie( ruletrie );
	}

	public void printInfo( int id ) {
		System.out.println( tableT.get( id ) );

	}

	public void main( String args[] ) throws IOException {
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
