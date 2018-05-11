package snu.kdd.synonym.synonymRev.algorithm.pqFilterDP;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StaticFunctions;
import snu.kdd.synonym.synonymRev.tools.WYK_HashMap;
import snu.kdd.synonym.synonymRev.tools.WYK_HashSet;

public class JoinPQFilterDP3 extends JoinPQFilterDP1 {
	
//	private WYK_HashMap<Integer, ObjectArrayList<IntegerPair>> mapQGramPrefixList;
	/*
	 * mapQGramPrefixList[pos] has a list of (token, depth) pairs at position pos.
	 * The pairs are sorted in the preorder of keys in 
	 */

	public JoinPQFilterDP3( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}
	
	@Override
	protected void joinOneRecord( Record recS, List<IntegerPair> rslt ) {
		long startTime = System.currentTimeMillis();
		// Enumerate candidate pos-qgrams of recS.
		ObjectArrayList<WYK_HashSet<QGram>> candidatePQGrams = getCandidatePQGrams( recS );
		// Build mapQGramPrefixList from candidatePQGrams.
		WYK_HashMap<Integer, ObjectArrayList<IntegerPair>> mapQGramPrefixList = new WYK_HashMap<Integer, ObjectArrayList<IntegerPair>>(indexK);
		for ( int pos=0; pos<indexK; pos++ ) mapQGramPrefixList.put( pos, getQGramPrefixList( candidatePQGrams.get( pos ) ) );
		long afterCandidateTime = System.currentTimeMillis();


		// prepare filtering
		PosQGramFilterDPInc filter = new PosQGramFilterDPInc(recS, qgramSize);
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
				Boolean isInTPQ = filter.existence( token, depth, pos );
				dpTime += System.nanoTime() - startDPTime;
				if (isInTPQ && depth == qgramSize) {
					for ( Record recT : idx.get( pos ).get( new QGram(filter.qgram) ) ) {
						// length filtering
						if ( useLF ) {
							int[] otherRange = new int[2];
							if ( query.oneSideJoin ) {
								otherRange[0] = otherRange[1] = recT.getTokenCount();
							}
							else throw new RuntimeException("oneSideJoin is supported only.");
							if (!StaticFunctions.overlap(otherRange[0], otherRange[1], range[0], range[1])) {
								lengthFiltered++;
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
		
		Set<Record> candidatesAfterDP = new WYK_HashSet<Record>(100);
		for (Record recT : candidatesCount.keySet()) {
			if ( idx.indexedCountList.getInt( recT ) <= candidatesCount.getInt( recT ) ) candidatesAfterDP.add( recT );
		}
		long afterFilteringTime = System.currentTimeMillis();

		for ( Record recT : candidatesAfterDP ) {
			if ( checker.isEqual( recS, recT ) >= 0 ) 
				rslt.add( new IntegerPair( recS.getID(), recT.getID()) );
		}

		long afterValidateTime = System.currentTimeMillis();
		
		candPQGramTime += afterCandidateTime - startTime;
		filteringTime += afterFilteringTime - afterCandidateTime;
		validateTime += afterValidateTime - afterFilteringTime;
	}
}