package vldb17.test;

import java.io.IOException;
import java.util.Set;

import org.apache.commons.cli.ParseException;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.algorithm.misc.SampleDataTest;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import vldb17.JoinPkduck;
import vldb17.PkduckIndex;
import vldb17.PkduckIndex.GlobalOrder;

public class PkduckTest {
	
	public static Query query;
	
	public static PkduckIndex indexTest(GlobalOrder globalOrder) {
		StatContainer stat = new StatContainer();
		PkduckIndex index = new PkduckIndex( query, stat, globalOrder, true );
		index.writeToFile( "tmp/PkduckIndex.txt" );
		stat.printResult();
		System.out.println( "PkduckTest.indexTest finished" );
		return index;
	}
	
	public static void dpTest(PkduckIndex index) {
		Set<QGram> qgram_cadidates = new ObjectOpenHashSet<QGram>();
		
		final int m = 100;
		final int n = query.searchedSet.size();
		
		for (int i=0; i<m; i++) {
			Record record = query.searchedSet.getRecord( i );
			for (QGram qgram : record.getSelfQGrams( 1, record.size() ).get( 0 )) {
				qgram_cadidates.add( qgram );
			}
		}
		
		long tic = 0;
		
		for (int i=0; i<n; i++) {
			long recordTime = System.currentTimeMillis();
			Record record = query.searchedSet.getRecord( i );
//			SampleDataTest.inspect_record( record, query, 1 );
			for (QGram qgram : qgram_cadidates) {
				Boolean isInSigU =  index.isInSigU( record, qgram, 0 );
//				System.out.println( qgram.toString()+" : "+isInSigU );
				assert qgram_cadidates.equals( record.getSelfQGrams( 1, 1 ).get( 0 ).get( 0 ) ) == isInSigU;
			}
			tic += System.currentTimeMillis() - recordTime;
			if (tic >= 1000) {
				tic -= 1000;
				System.out.println( i+" records are processed" );
			}
		}
		System.out.println( "PkduckTest.dpTest finished" );
	}
	
	public static void joinTest(GlobalOrder globalOrder) throws IOException, ParseException {
		StatContainer stat = new StatContainer();
		JoinPkduck joinPkduck = new JoinPkduck( query, stat );
			joinPkduck.run( query, new String[] {"-globalOrder", globalOrder.toString()});
	}
	
	public static void main( String[] args ) throws IOException, ParseException {
		SampleDataTest data = new SampleDataTest( 1 );
		query = data.query;
		PkduckIndex index;
		GlobalOrder globalOrder;

		globalOrder = GlobalOrder.PositionFirst;
		System.out.println( "Global order: "+globalOrder.name() );
//		index = indexTest(globalOrder);
//		dpTest(index);
		joinTest( globalOrder );

		globalOrder = GlobalOrder.TokenIndexFirst;
		System.out.println( "Global order: "+globalOrder.name() );
//		index = indexTest(globalOrder);
//		dpTest(index);
		joinTest( globalOrder );

	}
}
