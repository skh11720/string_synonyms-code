package snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.seq;

import java.util.List;
import java.util.Map;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.QGram;

public abstract class AbstractPQFilterIndex {

	protected Object2ObjectOpenHashMap<IntegerPair, Set<QGram>> invIndex;

	abstract public Map<QGram, List<Record>> get( int pos );

	abstract public Iterable<Integer> getPosSet();

	abstract public int getIndexedCount( Record rec );
	
	abstract public void writeToFile();

	public void buildInvertedIndex() {
		invIndex = new Object2ObjectOpenHashMap<IntegerPair, Set<QGram>>();
		for ( int pos : getPosSet() ) {
			Set<QGram> qgramSet = get(pos).keySet();
			for ( QGram qgram : qgramSet ) {
				for ( int j=0; j<qgram.qgram.length; ++j ) {
					int token = qgram.qgram[j];
					IntegerPair key = new IntegerPair( j, token );
					if ( !invIndex.containsKey( key ) ) invIndex.put( key, new ObjectOpenHashSet<QGram>() );
					invIndex.get( key ).add( qgram );
				}
			}
		}
	}
	
	public Set<QGram> getQGramSet( int j, int token ) {
		IntegerPair key = new IntegerPair(j, token);
		if ( !invIndex.containsKey( key ) ) return null;
		else return invIndex.get( key );
	}
}
