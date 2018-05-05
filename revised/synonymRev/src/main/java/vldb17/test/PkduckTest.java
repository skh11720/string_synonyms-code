package vldb17.test;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.ParseException;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.algorithm.misc.SampleDataTest;
import snu.kdd.synonym.synonymRev.data.ACAutomataR;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.validator.NaiveOneSide;
import vldb17.GreedyValidator;
import vldb17.JoinPkduck;
import vldb17.PkduckDP;
import vldb17.PkduckDPWithRC;
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
	
	public static void dpTest(PkduckIndex index, Boolean useRuleComp) throws IOException {
		System.out.println( "PkduckTest.dpTest "+(useRuleComp?"with":"without")+" RC" );
		StatContainer stat = new StatContainer();
		JoinPkduck joinPkduck = new JoinPkduck( query, stat );
		List<ObjectOpenHashSet<QGram>> qgram_candidates = new ObjectArrayList<ObjectOpenHashSet<QGram>>();
		
		final int m = 10;
//		final int n = query.searchedSet.size();
		final int n = 3;
		
		for (int i=0; i<m; i++) {
			Record record = query.searchedSet.getRecord( i );
			for (int j=0; j<record.size(); j++) {
				if ( qgram_candidates.size() <= j ) qgram_candidates.add( new ObjectOpenHashSet<QGram>() );
				for (QGram qgram : record.getSelfQGrams( 1, record.size() ).get( j )) {
					qgram_candidates.get(j).add( qgram );
				}
			}
		}
		
		long tic = 0;
		
		for (int i=0; i<n; i++) {
			long recordTime = System.currentTimeMillis();
			Record record = query.searchedSet.getRecord( i );
			List<List<QGram>> availableQGrams = record.getQGrams( 1 );
			PkduckDP pkduckDP;
			if (useRuleComp) pkduckDP = new PkduckDPWithRC( record, joinPkduck);
			else pkduckDP = new PkduckDP( record, joinPkduck);
			SampleDataTest.inspect_record( record, query, 1 );
			for ( int pos=0; pos<qgram_candidates.size(); pos++) {
				for (QGram qgram : qgram_candidates.get( pos )) {
					Boolean isInSigU =  pkduckDP.isInSigU( record, qgram, 0 );
					
					// true answer
					Boolean answer = false;
					if ( availableQGrams.size() > pos ) answer = availableQGrams.get( pos ).contains( qgram );

					System.out.println( "["+qgram.toString()+", "+pos+"] : "+answer+"\t"+isInSigU );
//					assert answer == isInSigU;
				}
			}
			tic += System.currentTimeMillis() - recordTime;
			if (tic >= 1000) {
				tic -= 1000;
				System.out.println( i+" records are processed" );
			}
		}
		System.out.println( "PkduckTest.dpTest "+(useRuleComp?"with":"without")+" RC finised" );
	}
	
	public static void joinTest(GlobalOrder globalOrder) throws IOException, ParseException {
		StatContainer stat = new StatContainer();
		JoinPkduck joinPkduck = new JoinPkduck( query, stat );
		joinPkduck.run( query, new String[] {"-globalOrder", globalOrder.toString(), "-verify", "naive"});
	}

	public static void naiveValidatorTest() {
		NaiveOneSide checker = new NaiveOneSide();
		int n = query.searchedSet.size();
		int m = query.indexedSet.size();
		long sec = 0;
		long tic = 0;
		for (int i=0; i<n; i++) {
			long recordTime = System.currentTimeMillis();
			for (int j=0; j<m; j++) {
				Record recS = query.searchedSet.getRecord( i );
				Record recT = query.indexedSet.getRecord( j );
//				System.out.println( i+", "+j );
//				System.out.println( "Compare: "+recS+", "+recT );
//				System.out.println( "VALIDATE "+recS+" AND "+recT );
//				SampleDataTest.inspect_record( recS, query, 1 );
//				SampleDataTest.inspect_record( recT, query, 1 );
//				System.out.println( "recS: "+recS );
//				System.out.println( "recT: "+recT );
				int res = checker.isEqual( recS, recT );
				if (res >= 0) System.out.println( recS.getID()+", "+recT.getID() );
			}
			tic += System.currentTimeMillis() - recordTime;
			if (tic >= 1000) {
				tic -= 1000;
				sec++;
				System.out.println( sec+" sec: "+i+" records are processed, " );
			}
		}
	}
	
	public static void greedyValidatorTest() {
		GreedyValidator checker = new GreedyValidator( true );
		int n = query.searchedSet.size();
		int m = query.indexedSet.size();
		long sec = 0;
		long tic = 0;
		for (int i=0; i<n; i++) {
			long recordTime = System.currentTimeMillis();
			for (int j=0; j<m; j++) {
				Record recS = query.searchedSet.getRecord( i );
				Record recT = query.indexedSet.getRecord( j );
//				System.out.println( i+", "+j );
//				System.out.println( "Compare: "+recS+", "+recT );
//				System.out.println( "VALIDATE "+recS+" AND "+recT );
//				SampleDataTest.inspect_record( recS, query, 1 );
//				SampleDataTest.inspect_record( recT, query, 1 );
//				System.out.println( "recS: "+recS );
//				System.out.println( "recT: "+recT );
				int res = checker.isEqual( recS, recT );
				if (res >= 0) System.out.println( recS.getID()+", "+recT.getID() );
			}
			tic += System.currentTimeMillis() - recordTime;
			if (tic >= 1000) {
				tic -= 1000;
				sec++;
				System.out.print( sec+" sec: "+i+" records are processed, " );
				System.out.print( String.format( "%.3f, ", checker.ruleCopyTime/1.0e9 ) );
				System.out.print( String.format( "%.3f, ", checker.computeScoreTime/1.0e9 ) );
				System.out.print( String.format( "%.3f, ", checker.bestRuleTime/1.0e9 ) );
				System.out.print( String.format( "%.3f, ", checker.removeRuleTime/1.0e9 ) );
				System.out.print( String.format( "%.3f, ", checker.bTransformTime/1.0e9 ) );
				System.out.print( String.format( "%.3f, ", checker.reconstTime/1.0e9 ) );
				System.out.print( String.format( "%.3f, ", checker.compareTime/1.0e9 ) );
				System.out.println(  );
			}
		}
	}
	
	public static void loadData() throws IOException {
		
		// synthetic 
		final String dataOnePath = "D:\\ghsong\\data\\yjpark_data\\data\\1000000_5_15848_1.0_0.0_1.txt";
		final String dataTwoPath = "D:\\ghsong\\data\\yjpark_data\\data\\1000000_5_15848_1.0_0.0_2.txt";
		final String rulePath = "D:\\ghsong\\data\\yjpark_data\\rule\\30000_2_2_10000_0.0_0.txt";

		
		// USPS
//		final String dataOnePath = "D:\\ghsong\\data\\JiahengLu\\splitted\\USPS_10000.txt";
//		final String dataTwoPath = "D:\\ghsong\\data\\JiahengLu\\splitted\\USPS_10000.txt";
//		final String rulePath = "D:\\ghsong\\data\\JiahengLu\\USPS_rule.txt";

		// AOL
//		final String dataOnePath = "D:\\ghsong\\data\\aol\\splitted\\aol_10000_data.txt";
//		final String dataTwoPath = "D:\\ghsong\\data\\aol\\splitted\\aol_10000_data.txt";
//		final String rulePath = "D:\\ghsong\\data\\wordnet\\rules.noun";
		
		// SPROT
//		final String dataOnePath = "D:\\ghsong\\data\\sprot\\splitted\\SPROT_two_15848.txt";
//		final String dataTwoPath = "D:\\ghsong\\data\\sprot\\splitted\\SPROT_two_15848.txt";
//		final String rulePath = "D:\\ghsong\\data\\sprot\\rule.txt";

		

//		final String rulePath = "D:\\ghsong\\data\\yjpark_data\\rule2_30000_2_2_30000_0.0_0.txt";
		final String outputPath = "output";
		final Boolean oneSideJoin = true;
		query = new Query(rulePath, dataOnePath, dataTwoPath, oneSideJoin, outputPath);
//		final int q = 2;
		
		//[24397 3252 10978 5663]
  	
		/* DEBUG: additional data */
//		Rule rule0 = new Rule( new int[] {24397, 3252, 10978}, new int[] {777} );
		//System.out.println( rule0.toOriginalString( query.tokenIndex ) );
//		query.ruleSet.add(rule0);

		final ACAutomataR automata = new ACAutomataR( query.ruleSet.get());
		//System.exit(1);
		
		for (Record record : query.searchedSet.recordList) {
			record.preprocessRules( automata );
			record.preprocessSuffixApplicableRules();
			record.preprocessTransformLength();
		}
		
		System.out.println( "Number of records in S: "+query.searchedSet.size() );
		System.out.println( "Number of records in T: "+query.indexedSet.size() );
		System.out.println( "Number of rules: "+query.ruleSet.size() );
	}
	
	public static void main( String[] args ) throws IOException, ParseException {
		loadData();
		PkduckIndex index;
//		GlobalOrder[] globalOrderList = {GlobalOrder.PositionFirst, GlobalOrder.TokenIndexFirst};
		GlobalOrder[] globalOrderList = {GlobalOrder.PositionFirst};
		for (GlobalOrder globalOrder: globalOrderList) {
			System.out.println( "Global order: "+globalOrder.name() );
			index = indexTest(globalOrder);
			dpTest(index, false);
//			dpTest(index, true);
//			joinTest( globalOrder );
//			greedyValidatorTest();
//			naiveValidatorTest();
		}
	}
}
