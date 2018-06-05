package snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.seq;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.tools.WYK_HashSet;

public class JoinMHDP3 extends JoinPQFilterDP3 {
	
	public JoinMHDP3( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}

	@Override
	protected Int2ObjectOpenHashMap<WYK_HashSet<QGram>> getCandidatePQGrams(Record rec) {
		Int2ObjectOpenHashMap<WYK_HashSet<QGram>> candidatePQGrams = new Int2ObjectOpenHashMap<WYK_HashSet<QGram>>();
		List<List<QGram>> availableQGrams = rec.getQGrams( qgramSize );
		for ( int pos=0; pos<availableQGrams.size(); ++pos ) {
			if ( !idx.getPosSet().contains( pos ) ) continue;
			for ( QGram qgram : availableQGrams.get( pos ) ) {
				if ( !idx.get( pos ).containsKey( qgram ) ) continue;
				if ( !candidatePQGrams.containsKey( pos ) ) candidatePQGrams.put( pos, new WYK_HashSet<QGram>( availableQGrams.get( pos ) ) );
				candidatePQGrams.get( pos ).add(qgram);
			}
		}
		return candidatePQGrams;
	}
}