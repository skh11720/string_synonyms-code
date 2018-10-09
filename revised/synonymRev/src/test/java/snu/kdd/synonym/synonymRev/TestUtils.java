package snu.kdd.synonym.synonymRev;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import org.junit.Ignore;
import org.junit.Test;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import snu.kdd.synonym.synonymRev.algorithm.delta.QGramDeltaGenerator;
import snu.kdd.synonym.synonymRev.data.ACAutomataR;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.Util;

public class TestUtils {

	public static Query getTestQuery( long size ) throws IOException {
		return getTestQuery("AOL", size);
	}
	
	public static Query getTestQuery( String name, long size ) throws IOException {
		String osName = System.getProperty( "os.name" );
		String prefix = null;
		if ( osName.startsWith( "Windows" ) ) {
			prefix = "D:\\ghsong\\data\\synonyms\\";
//			String dataOnePath = "C:/users/ghsong/data/aol/splitted/aol_"+size+"_data.txt";
//			String dataTwoPath = "C:/users/ghsong/data/aol/splitted/aol_"+size+"_data.txt";
//			String rulePath = "C:/users/ghsong/data/wordnet/rules.noun";
		}
		else if ( osName.startsWith( "Linux" ) ) {
			prefix = "run/data_store/";
//			String dataOnePath = "run/data_store/aol/splitted/aol_"+size+"_data.txt";
//			String dataTwoPath = "run/data_store/aol/splitted/aol_"+size+"_data.txt";
//			String rulePath = "run/data_store/wordnet/rules.noun";
//			String outputPath = "output";
		}
		
		String sep = "\\" + File.separator;
		int ruleSize = 100000;
		
		String dataOnePath, dataTwoPath, rulePath;
		if ( name.equals( "AOL" )) {
			dataOnePath = prefix + String.format( "aol"+sep+"splitted"+sep+"aol_%d_data.txt", size );
			dataTwoPath = prefix + String.format( "aol"+sep+"splitted"+sep+"aol_%d_data.txt", size );
			rulePath = prefix + "wordnet"+sep+"rules.noun";
		}
		else if ( name.equals( "SPROT" ) ) {
			dataOnePath = prefix + String.format( "sprot"+sep+"splitted"+sep+"SPROT_two_%d.txt", size );
			dataTwoPath = prefix + String.format( "sprot"+sep+"splitted"+sep+"SPROT_two_%d.txt", size );
			rulePath = prefix + "sprot"+sep+"rule.txt";
		}
		else if ( name.equals( "USPS" ) ) {
			dataOnePath = prefix + String.format( "JiahengLu"+sep+"splitted"+sep+"USPS_%d.txt", size );
			dataTwoPath = prefix + String.format( "JiahengLu"+sep+"splitted"+sep+"USPS_%d.txt", size );
			rulePath = prefix + "JiahengLu"+sep+"USPS_rule.txt";
		}
		else if ( name.equals( "SYN_100K" ) ) {
			dataOnePath = prefix + String.format( "data"+sep+"1000000_5_%d_1.0_0.0_1.txt", size );
			dataTwoPath = prefix + String.format( "data"+sep+"1000000_5_%d_1.0_0.0_2.txt", size );
			rulePath = prefix + "rule"+sep+"30000_2_2_100000_0.0_0.txt";
		}
		else if ( name.equals( "SYN_300K" ) ) {
			dataOnePath = prefix + String.format( "data"+sep+"1000000_5_%d_1.0_0.0_1.txt", size );
			dataTwoPath = prefix + String.format( "data"+sep+"1000000_5_%d_1.0_0.0_2.txt", size );
			rulePath = prefix + "rule"+sep+"30000_2_2_300000_0.0_0.txt";
		}
		else if ( name.equals( "SYN_1M" ) ) {
			dataOnePath = prefix + String.format( "data"+sep+"1000000_5_%d_1.0_0.0_1.txt", size );
			dataTwoPath = prefix + String.format( "data"+sep+"1000000_5_%d_1.0_0.0_2.txt", size );
			rulePath = prefix + "rule"+sep+"30000_2_2_1000000_0.0_0.txt";
		}
		else if ( name.equals( "SYN_D_2.0" ) ) {
			dataOnePath = prefix + String.format("SYN"+sep+"SYN_D_2.0"+sep+"data"+sep+"100_5_%s_0.6_1.0E-5_1.txt", size );
			dataTwoPath = prefix + String.format("SYN"+sep+"SYN_D_2.0"+sep+"data"+sep+"100_5_%s_0.6_1.0E-5_2.txt", size );
			rulePath = prefix + String.format( "SYN"+sep+"SYN_D_2.0"+sep+"rule"+sep+"100_2_2_%s_0.6_0.txt", ruleSize );
		}
		else if ( name.equals( "SYN_D_2.5" ) ) {
			dataOnePath = prefix + String.format("SYN"+sep+"SYN_D_2.5"+sep+"data"+sep+"316_5_%s_0.6_1.0E-5_1.txt", size );
			dataTwoPath = prefix + String.format("SYN"+sep+"SYN_D_2.5"+sep+"data"+sep+"316_5_%s_0.6_1.0E-5_2.txt", size );
			rulePath = prefix + String.format( "SYN"+sep+"SYN_D_2.5"+sep+"rule"+sep+"316_2_2_%s_0.6_0.txt", ruleSize );
		}
		else if ( name.equals( "SYN_D_3.0" ) ) {
			dataOnePath = prefix + String.format("SYN"+sep+"SYN_D_3.0"+sep+"data"+sep+"1000_5_%s_0.6_1.0E-5_1.txt", size );
			dataTwoPath = prefix + String.format("SYN"+sep+"SYN_D_3.0"+sep+"data"+sep+"1000_5_%s_0.6_1.0E-5_2.txt", size );
			rulePath = prefix + String.format( "SYN"+sep+"SYN_D_3.0"+sep+"rule"+sep+"1000_2_2_%s_0.6_0.txt", ruleSize );
		}
		else if ( name.equals( "SYN_D_3.5" ) ) {
			dataOnePath = prefix + String.format("SYN"+sep+"SYN_D_3.5"+sep+"data"+sep+"3162_5_%s_0.6_1.0E-5_1.txt", size );
			dataTwoPath = prefix + String.format("SYN"+sep+"SYN_D_3.5"+sep+"data"+sep+"3162_5_%s_0.6_1.0E-5_2.txt", size );
			rulePath = prefix + String.format( "SYN"+sep+"SYN_D_3.5"+sep+"rule"+sep+"3162_2_2_%s_0.6_0.txt", ruleSize );
		}
		else if ( name.equals( "SYN_D_4.0" ) ) {
			dataOnePath = prefix + String.format("SYN"+sep+"SYN_D_4.0"+sep+"data"+sep+"10000_5_%s_0.6_1.0E-5_1.txt", size );
			dataTwoPath = prefix + String.format("SYN"+sep+"SYN_D_4.0"+sep+"data"+sep+"10000_5_%s_0.6_1.0E-5_2.txt", size );
			rulePath = prefix + String.format( "SYN"+sep+"SYN_D_4.0"+sep+"rule"+sep+"10000_2_2_%s_0.6_0.txt", ruleSize );
		}
		else if ( name.equals( "SYN_D_4.5" ) ) {
			dataOnePath = prefix + String.format("SYN"+sep+"SYN_D_4.5"+sep+"data"+sep+"31622_5_%s_0.6_1.0E-5_1.txt", size );
			dataTwoPath = prefix + String.format("SYN"+sep+"SYN_D_4.5"+sep+"data"+sep+"31622_5_%s_0.6_1.0E-5_2.txt", size );
			rulePath = prefix + String.format( "SYN"+sep+"SYN_D_4.5"+sep+"rule"+sep+"31622_2_2_%s_0.6_0.txt", ruleSize );
		}
		else if ( name.equals( "SYN_D_5.0" ) ) {
			dataOnePath = prefix + String.format("SYN"+sep+"SYN_D_5.0"+sep+"data"+sep+"100000_5_%s_0.6_1.0E-5_1.txt", size );
			dataTwoPath = prefix + String.format("SYN"+sep+"SYN_D_5.0"+sep+"data"+sep+"100000_5_%s_0.6_1.0E-5_2.txt", size );
			rulePath = prefix + String.format( "SYN"+sep+"SYN_D_5.0"+sep+"rule"+sep+"100000_2_2_%s_0.6_0.txt", ruleSize );
		}
		else if ( name.equals( "SYN_D_5.5" ) ) {
			dataOnePath = prefix + String.format("SYN"+sep+"SYN_D_5.5"+sep+"data"+sep+"316227_5_%s_0.6_1.0E-5_1.txt", size );
			dataTwoPath = prefix + String.format("SYN"+sep+"SYN_D_5.5"+sep+"data"+sep+"316227_5_%s_0.6_1.0E-5_2.txt", size );
			rulePath = prefix + String.format( "SYN"+sep+"SYN_D_5.5"+sep+"rule"+sep+"316227_2_2_%s_0.6_0.txt", ruleSize );
		}
		else if ( name.equals( "SYN_D_6.0" ) ) {
			dataOnePath = prefix + String.format("SYN"+sep+"SYN_D_6.0"+sep+"data"+sep+"1000000_5_%s_0.6_1.0E-5_1.txt", size );
			dataTwoPath = prefix + String.format("SYN"+sep+"SYN_D_6.0"+sep+"data"+sep+"1000000_5_%s_0.6_1.0E-5_2.txt", size );
			rulePath = prefix + String.format( "SYN"+sep+"SYN_D_6.0"+sep+"rule"+sep+"1000000_2_2_%s_0.6_0.txt", ruleSize );
		}
		else if ( name.equals( "SYN_K_0.0" ) ) {
			dataOnePath = prefix + String.format("SYN"+sep+"SYN_K_0.0"+sep+"data"+sep+"10000_5_%s_0.0_1.0E-5_1.txt", size );
			dataTwoPath = prefix + String.format("SYN"+sep+"SYN_K_0.0"+sep+"data"+sep+"10000_5_%s_0.0_1.0E-5_2.txt", size );
			rulePath = prefix + String.format( "SYN"+sep+"SYN_K_0.0"+sep+"rule"+sep+"10000_2_2_%s_0.0_0.txt", ruleSize );
		}
		else if ( name.equals( "SYN_K_0.2" ) ) {
			dataOnePath = prefix + String.format("SYN"+sep+"SYN_K_0.2"+sep+"data"+sep+"10000_5_%s_0.2_1.0E-5_1.txt", size );
			dataTwoPath = prefix + String.format("SYN"+sep+"SYN_K_0.2"+sep+"data"+sep+"10000_5_%s_0.2_1.0E-5_2.txt", size );
			rulePath = prefix + String.format( "SYN"+sep+"SYN_K_0.2"+sep+"rule"+sep+"10000_2_2_%s_0.2_0.txt", ruleSize );
		}
		else if ( name.equals( "SYN_K_0.4" ) ) {
			dataOnePath = prefix + String.format("SYN"+sep+"SYN_K_0.4"+sep+"data"+sep+"10000_5_%s_0.4_1.0E-5_1.txt", size );
			dataTwoPath = prefix + String.format("SYN"+sep+"SYN_K_0.4"+sep+"data"+sep+"10000_5_%s_0.4_1.0E-5_2.txt", size );
			rulePath = prefix + String.format( "SYN"+sep+"SYN_K_0.4"+sep+"rule"+sep+"10000_2_2_%s_0.4_0.txt", ruleSize );
		}
		else if ( name.equals( "SYN_K_0.6" ) ) {
			dataOnePath = prefix + String.format("SYN"+sep+"SYN_K_0.6"+sep+"data"+sep+"10000_5_%s_0.6_1.0E-5_1.txt", size );
			dataTwoPath = prefix + String.format("SYN"+sep+"SYN_K_0.6"+sep+"data"+sep+"10000_5_%s_0.6_1.0E-5_2.txt", size );
			rulePath = prefix + String.format( "SYN"+sep+"SYN_K_0.6"+sep+"rule"+sep+"10000_2_2_%s_0.6_0.txt", ruleSize );
		}
		else if ( name.equals( "SYN_K_0.8" ) ) {
			dataOnePath = prefix + String.format("SYN"+sep+"SYN_K_0.8"+sep+"data"+sep+"10000_5_%s_0.8_1.0E-5_1.txt", size );
			dataTwoPath = prefix + String.format("SYN"+sep+"SYN_K_0.8"+sep+"data"+sep+"10000_5_%s_0.8_1.0E-5_2.txt", size );
			rulePath = prefix + String.format( "SYN"+sep+"SYN_K_0.8"+sep+"rule"+sep+"10000_2_2_%s_0.8_0.txt", ruleSize );
		}
		else if ( name.equals( "SYN_K_1.0" ) ) {
			dataOnePath = prefix + String.format("SYN"+sep+"SYN_K_1.0"+sep+"data"+sep+"10000_5_%s_1.0_1.0E-5_1.txt", size );
			dataTwoPath = prefix + String.format("SYN"+sep+"SYN_K_1.0"+sep+"data"+sep+"10000_5_%s_1.0_1.0E-5_2.txt", size );
			rulePath = prefix + String.format( "SYN"+sep+"SYN_K_1.0"+sep+"rule"+sep+"10000_2_2_%s_1.0_0.txt", ruleSize );
		}
		else if ( name.equals( "SYN_L_11" ) ) {
			dataOnePath = prefix + String.format("SYN"+sep+"SYN_L_11"+sep+"data"+sep+"10000_11_%s_0.6_1.0E-5_1.txt", size );
			dataTwoPath = prefix + String.format("SYN"+sep+"SYN_L_11"+sep+"data"+sep+"10000_11_%s_0.6_1.0E-5_2.txt", size );
			rulePath = prefix + String.format( "SYN"+sep+"SYN_L_11"+sep+"rule"+sep+"10000_2_2_%s_0.6_0.txt", ruleSize );
		}
		else if ( name.equals( "SYN_L_3" ) ) {
			dataOnePath = prefix + String.format("SYN"+sep+"SYN_L_3"+sep+"data"+sep+"10000_3_%s_0.6_1.0E-5_1.txt", size );
			dataTwoPath = prefix + String.format("SYN"+sep+"SYN_L_3"+sep+"data"+sep+"10000_3_%s_0.6_1.0E-5_2.txt", size );
			rulePath = prefix + String.format( "SYN"+sep+"SYN_L_3"+sep+"rule"+sep+"10000_2_2_%s_0.6_0.txt", ruleSize );
		}
		else if ( name.equals( "SYN_L_5" ) ) {
			dataOnePath = prefix + String.format("SYN"+sep+"SYN_L_5"+sep+"data"+sep+"10000_5_%s_0.6_1.0E-5_1.txt", size );
			dataTwoPath = prefix + String.format("SYN"+sep+"SYN_L_5"+sep+"data"+sep+"10000_5_%s_0.6_1.0E-5_2.txt", size );
			rulePath = prefix + String.format( "SYN"+sep+"SYN_L_5"+sep+"rule"+sep+"10000_2_2_%s_0.6_0.txt", ruleSize );
		}
		else if ( name.equals( "SYN_L_7" ) ) {
			dataOnePath = prefix + String.format("SYN"+sep+"SYN_L_7"+sep+"data"+sep+"10000_7_%s_0.6_1.0E-5_1.txt", size );
			dataTwoPath = prefix + String.format("SYN"+sep+"SYN_L_7"+sep+"data"+sep+"10000_7_%s_0.6_1.0E-5_2.txt", size );
			rulePath = prefix + String.format( "SYN"+sep+"SYN_L_7"+sep+"rule"+sep+"10000_2_2_%s_0.6_0.txt", ruleSize );
		}
		else if ( name.equals( "SYN_L_9" ) ) {
			dataOnePath = prefix + String.format("SYN"+sep+"SYN_L_9"+sep+"data"+sep+"10000_9_%s_0.6_1.0E-5_1.txt", size );
			dataTwoPath = prefix + String.format("SYN"+sep+"SYN_L_9"+sep+"data"+sep+"10000_9_%s_0.6_1.0E-5_2.txt", size );
			rulePath = prefix + String.format( "SYN"+sep+"SYN_L_9"+sep+"rule"+sep+"10000_2_2_%s_0.6_0.txt", ruleSize );
		}
		else if ( name.equals( "SYN_S_-3.0" ) ) {
			dataOnePath = prefix + String.format("SYN"+sep+"SYN_S_-3.0"+sep+"data"+sep+"10000_5_%s_0.6_0.001_1.txt", size );
			dataTwoPath = prefix + String.format("SYN"+sep+"SYN_S_-3.0"+sep+"data"+sep+"10000_5_%s_0.6_0.001_2.txt", size );
			rulePath = prefix + String.format( "SYN"+sep+"SYN_S_-3.0"+sep+"rule"+sep+"10000_2_2_%s_0.6_0.txt", ruleSize );
		}
		else if ( name.equals( "SYN_S_-4.0" ) ) {
			dataOnePath = prefix + String.format("SYN"+sep+"SYN_S_-4.0"+sep+"data"+sep+"10000_5_%s_0.6_1.0E-4_1.txt", size );
			dataTwoPath = prefix + String.format("SYN"+sep+"SYN_S_-4.0"+sep+"data"+sep+"10000_5_%s_0.6_1.0E-4_2.txt", size );
			rulePath = prefix + String.format( "SYN"+sep+"SYN_S_-4.0"+sep+"rule"+sep+"10000_2_2_%s_0.6_0.txt", ruleSize );
		}
		else if ( name.equals( "SYN_S_-5.0" ) ) {
			dataOnePath = prefix + String.format("SYN"+sep+"SYN_S_-5.0"+sep+"data"+sep+"10000_5_%s_0.6_1.0E-5_1.txt", size );
			dataTwoPath = prefix + String.format("SYN"+sep+"SYN_S_-5.0"+sep+"data"+sep+"10000_5_%s_0.6_1.0E-5_2.txt", size );
			rulePath = prefix + String.format( "SYN"+sep+"SYN_S_-5.0"+sep+"rule"+sep+"10000_2_2_%s_0.6_0.txt", ruleSize );
		}
		else if ( name.equals( "SYN_S_-6.0" ) ) {
			dataOnePath = prefix + String.format("SYN"+sep+"SYN_S_-6.0"+sep+"data"+sep+"10000_5_%s_0.6_1.0E-6_1.txt", size );
			dataTwoPath = prefix + String.format("SYN"+sep+"SYN_S_-6.0"+sep+"data"+sep+"10000_5_%s_0.6_1.0E-6_2.txt", size );
			rulePath = prefix + String.format( "SYN"+sep+"SYN_S_-6.0"+sep+"rule"+sep+"10000_2_2_%s_0.6_0.txt", ruleSize );
		}
		else if ( name.equals( "SYN_S_0.0" ) ) {
			dataOnePath = prefix + String.format("SYN"+sep+"SYN_S_0.0"+sep+"data"+sep+"10000_5_%s_0.6_0.0_1.txt", size );
			dataTwoPath = prefix + String.format("SYN"+sep+"SYN_S_0.0"+sep+"data"+sep+"10000_5_%s_0.6_0.0_2.txt", size );
			rulePath = prefix + String.format( "SYN"+sep+"SYN_S_0.0"+sep+"rule"+sep+"10000_2_2_%s_0.6_0.txt", ruleSize );
		}



		else throw new RuntimeException();

		String outputPath = "output";
		boolean oneSideJoin = true;
		Query query = new Query(rulePath, dataOnePath, dataTwoPath, oneSideJoin, outputPath);

		final ACAutomataR automata = new ACAutomataR( query.ruleSet.get());
		for ( Record record : query.searchedSet.recordList ) {
			record.preprocessRules( automata );
			record.preprocessSuffixApplicableRules();
			record.preprocessTransformLength();
			record.preprocessTransformLength();
			record.preprocessEstimatedRecords();
		}

		if ( !query.selfJoin ) {
			for ( Record record : query.indexedSet.recordList ) {
				record.preprocessRules( automata );
				record.preprocessSuffixApplicableRules();
				record.preprocessTransformLength();
				record.preprocessTransformLength();
				record.preprocessEstimatedRecords();
			}
		}
		return query;
	}
	
	@Test
	public void testLCS() {
		Random random = new Random(0);
		final int nTest = 1000;
		final int lz_max = 10;
		final int l_max = 10;
		final int nV = 10;
		
		for ( int itr=0; itr<nTest; ++itr ) {
			int error = -1;
			int lz = random.nextInt( lz_max+1 );
			int lx = lz + random.nextInt( l_max+1 );
			int ly = lz + random.nextInt( l_max+1 );
			int[] z = new int[lz];
			int[] x = new int[lx];
			int[] y = new int[ly];
			for ( int i=0; i<lz; ++i ) z[i] = random.nextInt( nV );
			IntOpenHashSet idxSet = new IntOpenHashSet();
			idxSet.clear();
			while ( idxSet.size() < lz ) idxSet.add( random.nextInt( lx ) );
			for ( int i=0, k=0; i<lx; ++i ) {
				if ( idxSet.contains( i ) ) x[i] = z[k++];
				else x[i] = error--;
			}
			idxSet.clear();
			while ( idxSet.size() < lz ) idxSet.add( random.nextInt( ly ) );
			for ( int j=0, k=0; j<ly; ++j ) {
				if ( idxSet.contains( j ) ) y[j] = z[k++];
				else y[j] = error--;
			}
//			System.out.println( "x: "+Arrays.toString(x) );
//			System.out.println( "y: "+Arrays.toString(y) );
//			System.out.println( "z: "+Arrays.toString(z) );
			assertEquals( lz, Util.lcs( x, y ) );
		}
	}
	
	// 700 ms
	@Ignore
	public void testGetCombinations() {
		// check the output
		for ( IntArrayList comb : Util.getCombinations( 7, 3 ) )
			System.out.println( comb );
		
		// measure the execution time
		final int nTest = 1000;
		final int n_max = 10;

		for ( int itr=0; itr<nTest; ++itr ) {
			for ( int n=0; n<=n_max; ++n ) {
				for ( int k=0; k<=n; ++k ) {
					Util.getCombinations( n, k ).size();
				}
			}
		}
	}

	@Ignore
	public void testGetCombinationsAll() {
		// check the output
		for ( IntArrayList comb : Util.getCombinationsAll( 7, 3 ) )
			System.out.println( comb );
	}
	
	@Test
	public void testQGramDeltaGenerator() {
		QGramDeltaGenerator qdgen = new QGramDeltaGenerator( 3, 0 );
		QGram qgram0 = new QGram( new int[] {10, 20, 30, 40, 50} );
		
		// check delta=0
		for ( java.util.Map.Entry<QGram, Integer> entry : qdgen.getQGramDelta( qgram0 ) ) {
			QGram qgramDelta = entry.getKey();
			int delta = entry.getValue();
			assertEquals( Arrays.toString( new int[] {10, 20, 30} ), Arrays.toString( qgramDelta.qgram ) );
			assertEquals( 0, delta );
		}

		// check delta>0
		qdgen = new QGramDeltaGenerator( 3, 2 );
		int[][] answer_qgram = new int[][] {
			{10, 20, 30},
			{10, 20, 40},
			{10, 20, 50},
			{10, 30, 40},
			{10, 30, 50},
			{10, 40, 50},
			{20, 30, 40},
			{20, 30, 50},
			{20, 40, 50},
			{30, 40, 50},
		};
		int[] answer_delta = {0, 1, 2, 1, 2, 2, 1, 2, 2, 2};
		int k = 0;
		for ( java.util.Map.Entry<QGram, Integer> entry : qdgen.getQGramDelta( qgram0 ) ) {
			QGram qgramDelta = entry.getKey();
			int delta = entry.getValue();
			assertEquals( Arrays.toString(answer_qgram[k]), Arrays.toString(qgramDelta.qgram) );
			assertEquals( answer_delta[k], delta );
			++k;
		}	
	}
}
