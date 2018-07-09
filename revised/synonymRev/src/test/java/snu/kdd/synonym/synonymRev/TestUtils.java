package snu.kdd.synonym.synonymRev;

import static org.junit.Assert.assertEquals;

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

	public static Query getTestQuery( int size ) throws IOException {
		return getTestQuery("AOL", size);
	}
	
	public static Query getTestQuery( String name, int size ) throws IOException {
		String osName = System.getProperty( "os.name" );
		String prefix = null;
		if ( osName.startsWith( "Windows" ) ) {
			prefix = "C:/users/ghsong/data/";
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
		
		String dataOnePath, dataTwoPath, rulePath;
		if ( name.equals( "AOL" )) {
			dataOnePath = prefix + String.format( "aol/splitted/aol_%d_data.txt", size );
			dataTwoPath = prefix + String.format( "aol/splitted/aol_%d_data.txt", size );
			rulePath = prefix + "wordnet/rules.noun";
		}
		else if ( name.equals( "SPROT" ) ) {
			dataOnePath = prefix + String.format( "sprot/splitted/SPROT_two_%d.txt", size );
			dataTwoPath = prefix + String.format( "sprot/splitted/SPROT_two_%d.txt", size );
			rulePath = prefix + "sprot/rule.txt";
		}
		else if ( name.equals( "USPS" ) ) {
			dataOnePath = prefix + String.format( "JiahengLu/splitted/USPS_%d.txt", size );
			dataTwoPath = prefix + String.format( "JiahengLu/splitted/USPS_%d.txt", size );
			rulePath = prefix + "JiahengLu/USPS_rule.txt";
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
