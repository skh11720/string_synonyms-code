package snu.kdd.synonym.synonymRev.algorithm.delta;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.Util;

public class QGramDeltaGenerator {
	
	private final int qSize;
	private final int deltaMax;
	private final List<Entry<IntArrayList, Integer>> combList; // list of pairs of (comb, delta).

	public QGramDeltaGenerator( int qSize, int deltaMax ) {
		this.qSize = qSize;
		this.deltaMax = deltaMax;
		this.combList = new ArrayList<>();
		for ( IntArrayList comb : Util.getCombinations( qSize+deltaMax, qSize ) ) {
			int delta = comb.getInt( qSize-1 ) - qSize + 1;
			combList.add( new AbstractMap.SimpleEntry<IntArrayList, Integer>( comb, delta ) );
		}
	}
	
	public Iterable<Entry<QGram, Integer>> getQGramDelta( final QGram qgram ) {
		/*
		 * Return the list of pairs, where each pair consists of a qgram and the value of delta.
		 * Note: the length of the input qgram must be qSize+deltaMax.
		 */
		List<Entry<QGram, Integer>> qgramDeltaList = new ObjectArrayList<Entry<QGram, Integer>>();
		int[] qgramTokens = qgram.qgram;
		for ( Entry<IntArrayList, Integer> entry : combList ) {
			IntArrayList comb = entry.getKey();
			int delta = entry.getValue();
			int[] tokens = new int[qSize];
			// selecting PAD as an error is invalid; ignore such cases.
			int j = 0;
			int pick = comb.getInt( j );
			for ( int i=0; i<qSize+deltaMax; ++i ) {
				if ( i == pick ) {
					tokens[j++] = qgramTokens[i];
					if ( j >= tokens.length ) break;
					pick = comb.getInt( j );
				}
				else if ( qgramTokens[i] == Integer.MAX_VALUE ) break;
//				tokens[i] = qgramTokens[comb.getInt(i)];
			}
//			for ( int i=0; i<qSize; ++i ) tokens[i] = qgramTokens[comb.getInt(i)];
			if ( tokens[0] == Integer.MAX_VALUE ) continue;
			if ( j == tokens.length ) {
				QGram qgramDelta = new QGram( tokens );
				qgramDeltaList.add( new AbstractMap.SimpleEntry<QGram, Integer>( qgramDelta, delta ));
			}
		}
		return qgramDeltaList;
	}
}
