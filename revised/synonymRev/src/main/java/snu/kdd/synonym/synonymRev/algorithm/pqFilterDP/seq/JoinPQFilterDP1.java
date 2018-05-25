package snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.seq;

import java.io.IOException;
import java.util.Set;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.index.JoinMHIndex;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.WYK_HashSet;

public class JoinPQFilterDP1 extends JoinPQFilterDPNaive {
	
	public JoinPQFilterDP1( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}

	@Override
	protected void buildIndex( boolean writeResult ) {
		int[] indexPosition = new int[ indexK ];
		for( int i = 0; i < indexK; i++ ) {
			indexPosition[ i ] = i;
		}
		idx = new JoinMHIndex( indexK, qgramSize, query.indexedSet.get(), query, stat, indexPosition, writeResult, true, 0 );
		buildMapToken2qgram();
	}
	
	@Override
	protected ObjectArrayList<WYK_HashSet<QGram>> getCandidatePQGrams(Record rec) {
		ObjectArrayList<WYK_HashSet<QGram>> candidatePQGrams = new ObjectArrayList<WYK_HashSet<QGram>>();
		IntOpenHashSet candTokens = new IntOpenHashSet();
		for ( int i=0; i<rec.size(); i++ ) {
			for ( Rule rule : rec.getSuffixApplicableRules( i ) ) {
				for ( int token : rule.getRight() ) candTokens.add( token );
			}
		}

		for ( int pos=0; pos<indexK; pos++ ) {
			WYK_HashSet<QGram> candQGrams = new WYK_HashSet<QGram>();
			for ( int token : candTokens ) {
				Set<QGram> qgrams = mapToken2qgram.get( pos ).get( token );
				if (qgrams != null) candQGrams.addAll( qgrams );
			}
			candidatePQGrams.add( candQGrams );
		}
		return candidatePQGrams;
	}
}