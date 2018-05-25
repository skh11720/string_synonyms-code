package snu.kdd.synonym.synonymRev;

import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.seq.AbstractPosQGramFilterDP;
import snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.seq.PosQGramFilterDP;
import snu.kdd.synonym.synonymRev.data.ACAutomataR;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.Util;

public class PosQGramFilterDPTest {
	
	private static Query query;
	private static Record record;
	private static int q;
	private PosQGramFilterDP target;
	
	@BeforeClass
	public static void initialize() throws IOException {
		final String dataOnePath = "D:\\ghsong\\data\\aol\\splitted\\aol_10000_data.txt";
		final String dataTwoPath = "D:\\ghsong\\data\\aol\\splitted\\aol_10000_data.txt";
		final String rulePath = "D:\\ghsong\\data\\wordnet\\rules.noun";
		final String outputPath = "output";
		final Boolean oneSideJoin = true;
		query = new Query(rulePath, dataOnePath, dataTwoPath, oneSideJoin, outputPath);
		q = 1;
		record = query.searchedSet.getRecord( 0 );
		final ACAutomataR automata = new ACAutomataR( query.ruleSet.get());
		record.preprocessRules( automata );
		record.preprocessSuffixApplicableRules();
		record.preprocessTransformLength();
	}
	
	@Before
	public void beforeTest() {
		target = new PosQGramFilterDP( record, q );
	}
	
	@Test
	public void testExistence() {
//		fail( "Not yet implemented" );
	}

	@Test
	public void testIsPrefixOf() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Method method = AbstractPosQGramFilterDP.class.getDeclaredMethod( "isPrefixOf", int[].class, int.class, int.class, int[].class );
		method.setAccessible( true );
		Random random = new Random(2);
		int nTest = 10000;
		int nv = 10; // size of vocabulary
		for ( int itr=0; itr<nTest; itr++ ) {
//			System.out.println( "itr: "+itr );
			int patLen = random.nextInt( 5 ) + 1;
			int seqLen = random.nextInt( 15 ) + 6;
			int[] pat = random.ints( patLen, 0, nv ).toArray();
			int[] seq = random.ints( seqLen, 0, nv ).toArray();
			
			// true answer: trueAnswer[i][j] is true if pat[i,j] is a prefix of seq.
			boolean[][] trueAnswer = new boolean[patLen][patLen];
			for ( int i=0; i<patLen-1; i++ ) {
				for ( int j=i; j<patLen; j++ ) {
					if ( i == j) trueAnswer[i][j] = (pat[i] == seq[j-i]); 
					else trueAnswer[i][j] = trueAnswer[i][j-1] && ( pat[j] == seq[j-i] );
				}
			}

			// answer by the method
			for ( int i=0; i<patLen-1; i++ ) {
				for (int j=i; j<patLen; j++ ) {
					assertTrue( trueAnswer[i][j] == (boolean)(method.invoke( target, pat, i, j+1, seq )) );
				}
			}
		}
	}

	@Test
	public void testIsSuffixOf() {
//		fail( "Not yet implemented" );
	}

	@Test
	public void testIsSubstringOf() {
//		fail( "Not yet implemented" );
	}

}
