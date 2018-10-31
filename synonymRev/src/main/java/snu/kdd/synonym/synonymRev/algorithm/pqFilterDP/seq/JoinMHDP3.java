package snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.seq;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.StatContainer;

public class JoinMHDP3 extends JoinPQFilterDP3 {
	
	public JoinMHDP3( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}

	@Override
	protected Int2ObjectOpenHashMap<ObjectOpenHashSet<QGram>> getCandidatePQGrams(Record rec) {
		Int2ObjectOpenHashMap<ObjectOpenHashSet<QGram>> candidatePQGrams = new Int2ObjectOpenHashMap<ObjectOpenHashSet<QGram>>();
		List<List<QGram>> availableQGrams = rec.getQGrams( qgramSize );
		for ( int pos=0; pos<availableQGrams.size(); ++pos ) {
			if ( !idx.getPosSet().contains( pos ) ) continue;
			Map<QGram, List<Record>> curidx = idx.get( pos );
			if ( !candidatePQGrams.containsKey( pos ) ) candidatePQGrams.put( pos, new ObjectOpenHashSet<QGram>() );
			ObjectOpenHashSet<QGram> qgramSet = candidatePQGrams.get( pos );
			for ( QGram qgram : availableQGrams.get( pos ) ) {
				if ( curidx.containsKey( qgram ) ) qgramSet.add(qgram);
			}
		}
		return candidatePQGrams;
	}
}