package snu.kdd.synonym.synonymRev.algorithm.misc;

import java.io.IOException;
import java.util.Arrays;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.PosQGramFilterDP;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.StopWatch;


public class PosQGramFilterDPTest {
	
	private static Boolean debug = false;
	
	
	public static void main( String[] args ) throws IOException {
		
		final int q = 2;
		SampleDataTest data = new SampleDataTest(q);
		final Query query = data.query;
		final ObjectArrayList<Record> sample_records = data.sample_records;
		final ObjectOpenHashSet<Rule> sample_rules = data.sample_rules;
		final ObjectArrayList<ObjectOpenHashSet<QGram>> sample_pos_qgrams = data.sample_pos_qgrams;
		
		int ridx = 0;
		StopWatch totalTime = StopWatch.getWatchStarted( "total time" );
		for (final Record record : sample_records) {
			ridx++;
//			if (ridx != 334) continue;
			System.out.println( "record "+ridx+": "+Arrays.toString( record.getTokensArray()) );
//		final Record record = sample_records.get( 4 );
//			inspect_record( record, query, q );
			PosQGramFilterDP posQGramFilterDP = new PosQGramFilterDP(record, q);
//			posQGramFilterDP.testIsSubstringOf();
//			posQGramFilterDP.testIsPrefixOf();
			for (int pos=0; pos<sample_pos_qgrams.size(); pos++ ) {
				ObjectOpenHashSet<QGram> set_tpq;
				if (pos < record.getQGrams( q ).size()) set_tpq = new ObjectOpenHashSet<>( record.getQGrams(q).get( pos ) );
				else set_tpq = new ObjectOpenHashSet<>();
				ObjectOpenHashSet<QGram> set_tpq_DP = new ObjectOpenHashSet<>();
				for (final QGram qgram : sample_pos_qgrams.get( pos )) {
//					System.out.println( "*******************************************" );
//					System.out.println( "pos qgram: ["+Arrays.toString( qgram.qgram )+", "+pos+"]");
					Boolean isInTPQ = posQGramFilterDP.existence( qgram, pos );
//					if (isInTPQ) System.out.println( "["+Arrays.toString( qgram.qgram )+", "+pos+"]\t"+isInTPQ );
					if (isInTPQ) set_tpq_DP.add( qgram );
					if (isInTPQ && !set_tpq.contains( qgram ) || !isInTPQ && set_tpq.contains( qgram )) {
						SampleDataTest.inspect_record( record, query, q );
						System.err.println( "record "+ridx );
						System.err.println( set_tpq.toString() );
						System.err.println( qgram+", "+isInTPQ );
						throw new AssertionError();
					}
				}
			}
		}
		totalTime.stop();
	} // end main
}
