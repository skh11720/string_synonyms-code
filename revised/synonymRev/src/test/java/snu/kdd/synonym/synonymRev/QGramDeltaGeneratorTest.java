package snu.kdd.synonym.synonymRev;

import java.util.Arrays;
import java.util.Map.Entry;

import org.junit.Test;

import snu.kdd.synonym.synonymRev.algorithm.delta.QGramDeltaGenerator;
import snu.kdd.synonym.synonymRev.tools.QGram;

public class QGramDeltaGeneratorTest {
	
	private static final int INF = Integer.MAX_VALUE;

	@Test
	public void test() {
		int q = 2;
		int deltaMax = 2;
		QGramDeltaGenerator qdgen = new QGramDeltaGenerator( q, deltaMax );
		int[][] qgramList = {
				{10, 20, 30, 40},
				{20, 30, 40, INF},
				{30, 40, INF, INF},
		};
		for ( int[] qgram : qgramList ) {
			System.out.println( "qgram: "+Arrays.toString( qgram ) );
			Iterable<Entry<QGram, Integer>> qgramDeltaList = qdgen.getQGramDelta( new QGram(qgram) );
			for ( Entry<QGram, Integer> entry : qgramDeltaList ) {
				System.out.println( "\tqgramDelta: "+entry );
			}
		}
	}

}
