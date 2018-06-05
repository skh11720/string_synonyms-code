package snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.seq;

import java.util.List;
import java.util.Map;
import java.util.Set;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.index.JoinMHIndex;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.StatContainer;

public class PQFilterMHIndex extends AbstractPQFilterIndex {
	
	private JoinMHIndex idx;
	protected IntOpenHashSet posSet;

	public PQFilterMHIndex( int indexK, int qgramSize, Iterable<Record> indexedSet, Query query, StatContainer stat,
			int[] indexPosition, boolean addStat, boolean useIndexCount, int threshold ) {
		idx = new JoinMHIndex( indexK, qgramSize, indexedSet, query, stat, indexPosition, addStat, useIndexCount, threshold );
		posSet = new IntOpenHashSet();
		for ( int k=0; k<indexK; ++k ) posSet.add( indexPosition[k] );
		
		buildInvertedIndex();
	}

	@Override
	public Set<Integer> getPosSet() {
		return posSet;
	}

	@Override
	public Map<QGram, List<Record>> get( int pos ) {
		return idx.get( pos );
	}

	@Override
	public int getIndexedCount( Record rec ) {
		return idx.getIndexedCount( rec );
	}

	@Override
	public void writeToFile() {
		idx.writeToFile();
	}
}
