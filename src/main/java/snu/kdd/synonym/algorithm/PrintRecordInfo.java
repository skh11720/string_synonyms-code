package snu.kdd.synonym.algorithm;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import mine.Record;
import snu.kdd.synonym.data.DataInfo;
import snu.kdd.synonym.tools.StatContainer;
import tools.IntegerPair;
import tools.QGram;
import tools.Rule;
import tools.RuleTrie;

public class PrintRecordInfo extends AlgorithmTemplate {
	RuleTrie ruletrie;

	protected PrintRecordInfo( String rulefile, String Rfile, String Sfile, String outputPath, DataInfo dataInfo,
			boolean joinOneSide ) throws IOException {
		super( rulefile, Rfile, Sfile, outputPath, dataInfo, joinOneSide );

		ruletrie = new RuleTrie( getRulelist() );
		Record.setRuleTrie( ruletrie );

		this.stat = new StatContainer();
		this.preprocess( true, Integer.MAX_VALUE, false );
	}

	@Override
	protected void preprocess( boolean compact, int maxIndex, boolean useAutomata ) {
		super.preprocess( compact, maxIndex, useAutomata );
	}

	private void hybridPreprocess() {
		// Sort R and S with expanded sizes
		Comparator<Record> cmp = new Comparator<Record>() {
			@Override
			public int compare( Record o1, Record o2 ) {
				long est1 = o1.getEstNumRecords();
				long est2 = o2.getEstNumRecords();
				return Long.compare( est1, est2 );
			}
		};
		Collections.sort( tableSearched, cmp );
		Collections.sort( tableIndexed, cmp );

		// Reassign ID
		for( int i = 0; i < tableSearched.size(); ++i ) {
			Record t = tableSearched.get( i );
			t.setID( i );
		}
		long maxTEstNumRecords = tableSearched.get( tableSearched.size() - 1 ).getEstNumRecords();

		for( int i = 0; i < tableIndexed.size(); ++i ) {
			Record s = tableIndexed.get( i );
			s.setID( i );
		}
		long maxSEstNumRecords = tableIndexed.get( tableIndexed.size() - 1 ).getEstNumRecords();

		System.out.println( "Max S expanded size : " + maxSEstNumRecords );
		System.out.println( "Max T expanded size : " + maxTEstNumRecords );
	}

	@SuppressWarnings( "deprecation" )
	public void printInfo( int id ) {
		// TODO: DEBUG
		// for( int i = 0; i < tableSearched.size(); i++ ) {
		// System.out.println( tableSearched.get( i ) );
		// }

		Record r = tableSearched.get( id );
		System.out.println( r );

		int length = r.getTokenArray().length;

		for( int i = 0; i < length; i++ ) {
			Rule[] rlist = r.getApplicableRules( i );
			if( rlist != null ) {
				for( int x = 0; x < rlist.length; x++ ) {
					System.out.println( rlist[ x ].toTextString( Record.strlist ) );
				}
			}
			else {
				System.out.println( "Rule List null!" );
			}
		}

		long startTime = System.nanoTime();
		System.out.println( "\nTwoGram" );
		List<Set<IntegerPair>> twogram = r.get2Grams();
		for( int i = 0; i < twogram.size(); i++ ) {
			System.out.println( "Position " + i );
			for( IntegerPair pair : twogram.get( i ) ) {
				System.out.println( pair.toStrString() );
			}
		}
		System.out.println( "Time: " + ( System.nanoTime() - startTime ) );

		for( int i = 0; i < r.getTokenArray().length; i++ ) {
			System.out.println(
					"Candidate length " + i + " " + r.getCandidateLengths( i )[ 0 ] + " " + r.getCandidateLengths( i )[ 1 ] );
		}

		System.out.println( "\nTwoGramWithBound " );
		int[] range = r.getCandidateLengths( r.size() - 1 );

		startTime = System.nanoTime();
		// System.out.println( "Range " + range[ 0 ] );
		twogram = r.get2GramsWithBound( range[ 0 ] );
		for( int i = 0; i < twogram.size(); i++ ) {
			System.out.println( "Position " + i );
			for( IntegerPair pair : twogram.get( i ) ) {
				System.out.println( pair.toStrString() );
			}
		}
		System.out.println( "Time: " + ( System.nanoTime() - startTime ) );

		System.out.println( "\nQgram" );
		startTime = System.nanoTime();
		List<List<QGram>> qgrams = r.getQGrams( 2 );
		for( int i = 0; i < qgrams.size(); i++ ) {
			System.out.println( "Position " + i );
			for( QGram qgram : qgrams.get( i ) ) {
				System.out.println( qgram.toStrString( Record.strlist ) );
			}
		}
		System.out.println( "Time: " + ( System.nanoTime() - startTime ) );

		System.out.println( "\nQgramWithBound" );
		startTime = System.nanoTime();
		qgrams = r.getQGrams( 2, range[ 0 ] );
		for( int i = 0; i < qgrams.size(); i++ ) {
			System.out.println( "Position " + i );
			for( QGram qgram : qgrams.get( i ) ) {
				System.out.println( qgram.toStrString( Record.strlist ) );
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
		boolean hybird = Boolean.parseBoolean( args[ 5 ] );

		DataInfo dataInfo = new DataInfo( Rfile, Sfile, rulefile );

		PrintRecordInfo info = new PrintRecordInfo( rulefile, Rfile, Sfile, outputPath, dataInfo, false );

		if( hybird ) {
			System.out.println( "Preprocessing for hybrid" );
			info.hybridPreprocess();
		}

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

	@Override
	public void run( String[] args, StatContainer stat ) {

	}

}
