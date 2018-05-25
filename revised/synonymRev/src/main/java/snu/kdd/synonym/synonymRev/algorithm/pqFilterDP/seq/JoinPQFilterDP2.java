package snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.seq;

import java.io.IOException;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.index.JoinMHIndex;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StaticFunctions;
import snu.kdd.synonym.synonymRev.tools.WYK_HashMap;
import snu.kdd.synonym.synonymRev.tools.WYK_HashSet;

public class JoinPQFilterDP2 extends JoinPQFilterDPNaive {
	
	private WYK_HashMap<Integer, ObjectArrayList<IntegerPair>> mapQGramPrefixList;
	/*
	 * mapQGramPrefixList[pos] has a list of (token, depth) pairs at position pos.
	 * The pairs are sorted in the preorder of keys in 
	 */

	public JoinPQFilterDP2( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}

	@Override
	protected void buildIndex( boolean writeResult ) {
		int[] indexPosition = new int[ indexK ];
		for( int i = 0; i < indexK; i++ ) {
			indexPosition[ i ] = i;
		}
		idx = new JoinMHIndex( indexK, qgramSize, query.indexedSet.get(), query, stat, indexPosition, writeResult, true, 0 );
		
		mapQGramPrefixList = new WYK_HashMap<Integer, ObjectArrayList<IntegerPair>>(indexK);
		for ( int pos=0; pos<indexK; pos++ ) {
			ObjectArrayList<IntegerPair> qgramPrefixList = getQGramPrefixList( idx.get( pos ).keySet() );
			mapQGramPrefixList.put( pos, qgramPrefixList );
		}
	}
	
	@Override
	protected void joinOneRecord( Record recS, Set<IntegerPair> rslt ) {
		long startTime = System.currentTimeMillis();
		// Enumerate candidate pos-qgrams of recS.
		long afterCandidateTime = System.currentTimeMillis();

		// prepare filtering
		AbstractPosQGramFilterDP filter;
		if ( useTopDown ) filter = new PosQGramFilterDPIncTopDown(recS, qgramSize);
		else filter = new PosQGramFilterDPInc(recS, qgramSize);
		Object2IntOpenHashMap<Record> candidatesCount = new Object2IntOpenHashMap<Record>();
		candidatesCount.defaultReturnValue(-1);
		int[] range = recS.getTransLengths();

		// Scan the index and verify candidate record pairs.
		for ( int pos=0; pos<indexK; pos++ ) {
			for ( IntegerPair ipair : mapQGramPrefixList.get( pos )) {
				checkTPQ++;
				int token = ipair.i1;
				int depth = ipair.i2;
				long startDPTime = System.nanoTime();
				Boolean isInTPQ = ((IncrementalDP)filter).existence( token, depth, pos );
				dpTime += System.nanoTime() - startDPTime;
				if (isInTPQ && depth == qgramSize) {
					for ( Record recT : idx.get( pos ).get( new QGram( ((IncrementalDP)filter).getQGram() ) ) ) {
						// length filtering
						if ( useLF ) {
							int[] otherRange = new int[2];
							if ( query.oneSideJoin ) {
								otherRange[0] = otherRange[1] = recT.getTokenCount();
							}
							else throw new RuntimeException("oneSideJoin is supported only.");
							if (!StaticFunctions.overlap(otherRange[0], otherRange[1], range[0], range[1])) {
								++checker.filtered;
								continue;
							}
						}
						
						// count the number of appearance of recT in the index.
						int candCount = candidatesCount.getInt( recT );
						if (candCount == -1) candidatesCount.put( recT, 1 );
						else candidatesCount.put( recT, candCount+1 );
					}
				}
			}
		}
		
		Set<Record> candidatesAfterDP = new WYK_HashSet<Record>();
		for (Record recT : candidatesCount.keySet()) {
			if ( idx.indexedCountList.getInt( recT ) <= candidatesCount.getInt( recT ) ) candidatesAfterDP.add( recT );
		}
		long afterFilteringTime = System.currentTimeMillis();

		for ( Record recT : candidatesAfterDP ) {
			int comp = checker.isEqual( recS, recT );
			if ( comp >= 0 ) addSeqResult( recS, recT, rslt );
		}

		long afterValidateTime = System.currentTimeMillis();
		
		candPQGramTime += afterCandidateTime - startTime;
		filteringTime += afterFilteringTime - afterCandidateTime;
		validateTime += afterValidateTime - afterFilteringTime;
	}
	
//	@Override
//	public String getName() {
//		return "JoinPQFilterDP2";
//	}
//
//	@Override
//	public String getVersion() {
//		return "1.0";
//	}

}
