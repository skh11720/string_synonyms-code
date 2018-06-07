package snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.seq;

import java.io.IOException;
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
//		buildMapToken2qgram();
	}
	
	@Override
	protected Int2ObjectOpenHashMap<ObjectOpenHashSet<QGram>> getCandidatePQGrams(Record rec) {
		Int2ObjectOpenHashMap<ObjectOpenHashSet<QGram>> candidatePQGrams = new Int2ObjectOpenHashMap<ObjectOpenHashSet<QGram>>();
		int[][] transLen = rec.getTransLengthsAll();
//		boolean debug = false;
//		if ( rec.getID() == 19964 ) debug = true;
		for ( int i=0; i<rec.size(); i++ ) {
			for ( Rule rule : rec.getSuffixApplicableRules( i ) ) {
				int pad_token;
				if ( i != rec.size()-1 ) pad_token = -1;
				else pad_token = Integer.MAX_VALUE;
				int[] rhs_padded = Util.pad( rule.getRight(), rule.rightSize()+qgramSize-1, pad_token ); // pad with wildcards
//				if (debug) System.out.println( "pos: "+i+", rhs_padded: "+ Arrays.toString( rhs_padded ) );
				for ( int j_start=0; j_start<rule.rightSize(); ++j_start ) {
//					if (debug) System.out.println( "key exists: "+idx.invIndex.containsKey( new IntegerPair(j_start, rhs_padded[j_start]) ) );
//					if (debug) System.out.println( "qgramSet: "+idx.invIndex.get( new IntegerPair(j_start, rhs_padded[j_start]) ) );
					Set<QGram> qgramSet = idx.getQGramSet( 0, rhs_padded[j_start] );
//					if (debug) System.out.println( "j_start: "+0+", token: "+rhs_padded[j_start]+", qgramSet: "+qgramSet );
					if ( qgramSet == null ) continue;
					else qgramSet = new ObjectOpenHashSet<QGram>(qgramSet);
					for ( int j=1; j<qgramSize; ++j ) {
						int token = rhs_padded[j_start+j];
						if ( token == -1 ) continue;
						Set<QGram> otherQGramSet = idx.getQGramSet( j, token );
						if ( otherQGramSet == null ) {
							qgramSet = null;
							break;
						}
						else {
							qgramSet.retainAll( otherQGramSet );
							if ( qgramSet.size() == 0 ) break;
						}
					}
//					if (debug) System.out.println( "qgramSet: "+qgramSet );
					if ( qgramSet == null || qgramSet.size() == 0 ) continue;
					int minLen = i-rule.leftSize() >= 0 ? transLen[i-rule.leftSize()][0] : 0;
					int maxLen = i-rule.leftSize() >= 0 ? transLen[i-rule.leftSize()][1] : 0;
					for ( int len=minLen; len<=maxLen; ++len ) {
						int pos = len + j_start;
//						if (debug) System.out.println( "pos: "+pos +", qgramSet: "+qgramSet );
						if ( !candidatePQGrams.containsKey( pos ) ) candidatePQGrams.put( pos, new ObjectOpenHashSet<QGram>() );
						for ( QGram qgram : qgramSet ) candidatePQGrams.get( pos ).add( qgram );
					}
				}
			}
		}
		return candidatePQGrams;
	}
}