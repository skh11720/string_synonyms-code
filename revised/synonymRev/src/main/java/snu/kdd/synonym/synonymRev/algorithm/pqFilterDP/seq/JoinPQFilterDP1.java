package snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.seq;

import java.io.IOException;
import java.util.Set;

import javax.management.RuntimeErrorException;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
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

		if ( indexOpt.equals( "FTK" ) ) 
			idx = new PQFilterMHIndex( indexK, qgramSize, query.indexedSet.get(), query, stat, indexPosition, writeResult, true, 0 );
		else if ( indexOpt.equals( "FF" ) )
			idx = new PQFilterIndex( indexK, qgramSize, query.indexedSet.get(), query, globalOrder, stat );
		else throw new RuntimeException( "Unexpected error" );
		buildMapToken2qgram();
	}
	
	@Override
	protected Int2ObjectOpenHashMap<WYK_HashSet<QGram>> getCandidatePQGrams(Record rec) {
		Int2ObjectOpenHashMap<WYK_HashSet<QGram>> candidatePQGrams = new Int2ObjectOpenHashMap<WYK_HashSet<QGram>>();
		IntOpenHashSet candTokens = new IntOpenHashSet();
		for ( int i=0; i<rec.size(); i++ ) {
			for ( Rule rule : rec.getSuffixApplicableRules( i ) ) {
				for ( int token : rule.getRight() ) candTokens.add( token );
			}
		}

		for ( int pos : idx.getPosSet() ) {
			WYK_HashSet<QGram> candQGrams = new WYK_HashSet<QGram>();
			for ( int token : candTokens ) {
				Set<QGram> qgrams = mapToken2qgram.get( pos ).get( token );
				if (qgrams != null) candQGrams.addAll( qgrams );
			}
			candidatePQGrams.put( pos, candQGrams );
		}
		return candidatePQGrams;
	}
}