package snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.seq;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.synonym.synonymRev.algorithm.misc.SampleDataTest;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.QGramComparator;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StaticFunctions;
import snu.kdd.synonym.synonymRev.tools.Util;
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
	protected void joinOneRecord( Record recS, Set<IntegerPair> rslt ) {

		Boolean debug = false;
//		if ( recS.getID() == 1901 ) debug = true;
		if (debug) SampleDataTest.inspect_record( recS, query, qgramSize );

		long startTime = System.currentTimeMillis();
		// Enumerate candidate pos-qgrams of recS.
		Int2ObjectOpenHashMap<WYK_HashSet<QGram>> candidatePQGrams = getCandidatePQGrams( recS );
		// Build mapQGramPrefixList from candidatePQGrams.
		WYK_HashMap<Integer, List<IntegerPair>> mapQGramPrefixList = new WYK_HashMap<Integer, List<IntegerPair>>(indexK);
		for ( int pos : idx.getPosSet() ) {
			if ( !candidatePQGrams.containsKey( pos ) ) continue;
			mapQGramPrefixList.put( pos, Util.getQGramPrefixList( candidatePQGrams.get( pos ) ) );
		}
		long afterCandidateTime = System.currentTimeMillis();

		if (debug) {
			for ( int pos : idx.getPosSet() ) {
				if ( candidatePQGrams.containsKey( pos ) ) {
					System.out.println( "pos: "+pos );
					System.out.println( "\ncandidatePQGrams" );
					for ( QGram qgram : candidatePQGrams.get( pos ) ) {
						System.out.println( qgram.toString() );
					}
					System.out.println( "\ncandidatePQGrams, sorted" );
					List<QGram> keyList = new ObjectArrayList<QGram>( candidatePQGrams.get( pos ));
					keyList.sort( new QGramComparator() );
					for ( QGram qgram : keyList ) {
						System.out.println( qgram.toString() );
					}
				}

				if ( mapQGramPrefixList.containsKey( pos ) ) {
					System.out.println( "\nmapQGramPrefixList" );
					for ( IntegerPair ipair : mapQGramPrefixList.get( pos ) ) {
						System.out.println( ipair.toString() );
					}
				}
			}
		}

		// prepare filtering
		AbstractPosQGramFilterDP filter;
		if ( useTopDown ) filter = new PosQGramFilterDPIncTopDown(recS, qgramSize);
		else filter = new PosQGramFilterDPInc(recS, qgramSize);
		Object2IntOpenHashMap<Record> candidatesCount = new Object2IntOpenHashMap<Record>();
		candidatesCount.defaultReturnValue(-1);
		int[] range = recS.getTransLengths();

		// Scan the index and verify candidate record pairs.
		for ( int pos : idx.getPosSet() ) {
			if ( !mapQGramPrefixList.containsKey( pos ) ) continue;
			for ( IntegerPair ipair : mapQGramPrefixList.get( pos )) {
				checkTPQ++;
				int token = ipair.i1;
				int depth = ipair.i2;
				long startDPTime = System.nanoTime();
				Boolean isInTPQ = ((IncrementalDP)filter).existence( token, depth, pos );
				dpTime += System.nanoTime() - startDPTime;
				if (debug && depth == qgramSize) System.out.println( "["+Arrays.toString( ((IncrementalDP)filter ).getQGram() ) + ", "+pos+"] , "+isInTPQ );
				if (isInTPQ && depth == qgramSize) {
					QGram key = new QGram( ((IncrementalDP)filter).getQGram() );
					if ( !idx.get( pos ).containsKey( key ) ) continue;
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
			if ( idx.getIndexedCount( recT ) <= candidatesCount.getInt( recT ) ) candidatesAfterDP.add( recT );
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
}