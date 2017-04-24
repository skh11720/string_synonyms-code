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
import tools.RuleTrie;

public class CheckQGram extends AlgorithmTemplate {
	RuleTrie ruletrie;

	public CheckQGram( String rulefile, String Rfile, String Sfile, String outputPath, DataInfo dataInfo ) throws IOException {
		super( rulefile, Rfile, Sfile, outputPath, dataInfo );

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

	public void printInfo( int id ) {
		// TODO: DEBUG
		// for( int i = 0; i < tableSearched.size(); i++ ) {
		// System.out.println( tableSearched.get( i ) );
		// }

		int error = 0;
		for( Record r : tableSearched ) {
			// System.out.println( "Checking " + r.getID() );
			List<Set<IntegerPair>> twogramList = r.get2Grams();

			List<Set<QGram>> qgrams = r.getQGrams( 2 );

			error += checkEquality( -1, r, twogramList, qgrams );

			int length = twogramList.size();

			for( int i = 0; i < length; i++ ) {
				twogramList = r.get2GramsWithBound( i );
				qgrams = r.getQGrams( 2, i );

				error += checkEquality( i, r, twogramList, qgrams );
			}

		}
		System.out.println( "Error: " + error );
		System.out.println( "Done" );
	}

	public int checkEquality( int idx, Record r, List<Set<IntegerPair>> twogramList, List<Set<QGram>> qgrams ) {
		int length = twogramList.size();

		if( length != qgrams.size() ) {
			System.out.println( "Size mismatch: " + r.getID() );
			return 1;
		}

		int count = 0;
		for( int i = 0; i < length; i++ ) {
			Set<IntegerPair> twograms = twogramList.get( i );
			for( QGram qgram : qgrams.get( i ) ) {
				IntegerPair twogram = new IntegerPair( qgram.qgram[ 0 ], qgram.qgram[ 1 ] );

				if( !twograms.remove( twogram ) ) {
					System.out.println( r.getID() + " " + idx + " TwoGrams not contain: " + twogram );
					count++;
				}
			}

			if( !twograms.isEmpty() ) {
				for( IntegerPair twogram : twograms ) {
					System.out.println( r.getID() + " " + idx + " QGrams not contain: " + twogram );
					count++;
				}
			}
		}
		return count;
	}

	public static void main( String args[] ) throws IOException {
		String rulefile = args[ 0 ];
		String Rfile = args[ 1 ];
		String Sfile = args[ 2 ];
		String outputPath = args[ 3 ];
		int recordId = Integer.parseInt( args[ 4 ] );
		boolean hybird = Boolean.parseBoolean( args[ 5 ] );

		DataInfo dataInfo = new DataInfo( Rfile, Sfile, rulefile );

		CheckQGram info = new CheckQGram( rulefile, Rfile, Sfile, outputPath, dataInfo );

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
