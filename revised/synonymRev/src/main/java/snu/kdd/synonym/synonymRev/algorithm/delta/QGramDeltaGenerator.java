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
	
	private final int qgramSize;
	private final int deltaMax;
	private final List<Entry<IntArrayList, Integer>> combList; // list of pairs of (comb, delta).

	public QGramDeltaGenerator( int qgramSize, int deltaMax ) {
		this.qgramSize = qgramSize;
		this.deltaMax = deltaMax;
		this.combList = new ArrayList<>();
		for ( IntArrayList comb : Util.getCombinations( qgramSize+deltaMax, qgramSize ) ) {
			int delta = comb.getInt( qgramSize-1 ) - qgramSize + 1;
			combList.add( new AbstractMap.SimpleEntry<IntArrayList, Integer>( comb, delta ) );
		}
	}
	
	public Iterable<Entry<QGram, Integer>> getQGramDelta( final QGram qgram ) {
		/*
		 * Return the list of pairs, where each pair consists of a qgram and the value of delta.
		 * Note: the length of the input qgram must be qgramSize+deltaMax.
		 */
		List<Entry<QGram, Integer>> qgramDeltaList = new ObjectArrayList<Entry<QGram, Integer>>();
		int[] qgramTokens = qgram.qgram;
		for ( Entry<IntArrayList, Integer> entry : combList ) {
			IntArrayList comb = entry.getKey();
			int delta = entry.getValue();
			int[] tokens = new int[qgramSize];
			for ( int i=0; i<qgramSize; ++i ) tokens[i] = qgramTokens[comb.getInt(i)];
//			if ( tokens[0] == Integer.MAX_VALUE ) continue; // this line incurs incorrect result.. why?
			QGram qgramDelta = new QGram( tokens );
			qgramDeltaList.add( new AbstractMap.SimpleEntry<QGram, Integer>( qgramDelta, delta ));
		}
		return qgramDeltaList;
	}
}
