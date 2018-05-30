package snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.seq;

import java.util.stream.IntStream;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.index.JoinMHIndex;
import snu.kdd.synonym.synonymRev.tools.StatContainer;

public class PQFilterMHIndex extends JoinMHIndex implements PQFilterIndexInterface {
	
	protected IntOpenHashSet posSet;

	public PQFilterMHIndex( int indexK, int qgramSize, Iterable<Record> indexedSet, Query query, StatContainer stat,
			int[] indexPosition, boolean addStat, boolean useIndexCount, int threshold ) {
		super( indexK, qgramSize, indexedSet, query, stat, indexPosition, addStat, useIndexCount, threshold );
		posSet = new IntOpenHashSet();
		for ( int k=0; k<indexK; ++k ) posSet.add( indexPosition[k] );
	}

	public Iterable<Integer> getPosSet() {
		return posSet;
	}
}
