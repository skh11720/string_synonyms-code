package snu.kdd.synonym.synonymRev.algorithm;

import java.io.IOException;
import java.util.List;

import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.StatContainer;

public class PrintRecordInfo extends AlgorithmTemplate {

	protected PrintRecordInfo( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}

	@Override
	protected void preprocess() {
		super.preprocess();
	}

	@Override
	public void run( Query query, String[] args ) throws IOException, ParseException {

	}

	@SuppressWarnings( "deprecation" )
	public void printInfo( int id ) {
		preprocess();

		Record r = query.searchedSet.getRecord( id );
		System.out.println( r );

		int length = r.getTokenCount();

		for( int i = 0; i < length; i++ ) {
			Rule[] rlist = r.getApplicableRules( i );
			if( rlist != null ) {
				for( int x = 0; x < rlist.length; x++ ) {
					System.out.println( rlist[ x ].toOriginalString( query.tokenIndex ) );
				}
			}
			else {
				System.out.println( "Rule List null!" );
			}
		}

		System.out.println( "\nQgram" );
		long startTime = System.nanoTime();
		List<List<QGram>> qgrams = r.getQGrams( 2 );
		for( int i = 0; i < qgrams.size(); i++ ) {
			System.out.println( "Position " + i );
			for( QGram qgram : qgrams.get( i ) ) {
				System.out.println( qgram.toString( query.tokenIndex ) );
			}
		}
		System.out.println( "Time: " + ( System.nanoTime() - startTime ) );

		System.out.println( "\nQgramWithBound" );
		startTime = System.nanoTime();
		qgrams = r.getQGrams( 2, r.getMinTransLength() );
		for( int i = 0; i < qgrams.size(); i++ ) {
			System.out.println( "Position " + i );
			for( QGram qgram : qgrams.get( i ) ) {
				System.out.println( qgram.toString( query.tokenIndex ) );
			}
		}
		System.out.println( "Time: " + ( System.nanoTime() - startTime ) );

		// System.out.println( "\nExpanded strings with new implementations" );
		// List<Record> expanded = r.expandAll();
		// for( Record e : expanded ) {
		// System.out.println( e );
		// }
	}

	public static void main( String args[] ) throws IOException {
		String rulefile = args[ 0 ];
		String Rfile = args[ 1 ];
		String Sfile = args[ 2 ];
		String outputPath = args[ 3 ];
		int recordId = Integer.parseInt( args[ 4 ] );

		StatContainer stat = new StatContainer();
		Query query = new Query( rulefile, Rfile, Sfile, false, outputPath );
		PrintRecordInfo info = new PrintRecordInfo( query, stat );

		info.printInfo( recordId );
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
