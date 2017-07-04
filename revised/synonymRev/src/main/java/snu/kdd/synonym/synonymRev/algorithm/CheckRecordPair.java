package snu.kdd.synonym.synonymRev.algorithm;

import java.io.IOException;

import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.validator.TopDown;
import snu.kdd.synonym.synonymRev.validator.TopDownOneSide;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class CheckRecordPair extends AlgorithmTemplate {

	protected CheckRecordPair( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}

	@Override
	protected void preprocess() {
		super.preprocess();
	}

	@Override
	public void run( Query query, String[] args ) throws IOException, ParseException {

	}

	public void printInfo( int sId, int tId ) {
		preprocess();

		Record s = query.searchedSet.getRecord( sId );
		System.out.println( s );

		Record t = query.searchedSet.getRecord( tId );
		System.out.println( t );

		Validator val = new TopDown();

		System.out.println( "Bi-directional: " + val.isEqual( s, t ) );

		val = new TopDownOneSide();
		System.out.println( "Bi-directional: " + val.isEqual( s, t ) );
	}

	public static void main( String args[] ) throws IOException {
		String rulefile = args[ 0 ];
		String Rfile = args[ 1 ];
		String Sfile = args[ 2 ];
		String outputPath = args[ 3 ];
		int sId = Integer.parseInt( args[ 4 ] );
		int tId = Integer.parseInt( args[ 5 ] );

		StatContainer stat = new StatContainer();
		Query query = new Query( rulefile, Rfile, Sfile, false, outputPath );
		CheckRecordPair info = new CheckRecordPair( query, stat );

		info.printInfo( sId, tId );
	}

	@Override
	public String getName() {
		return "PrintRecordInfo";
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

}
