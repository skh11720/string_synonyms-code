package snu.kdd.synonym.synonymRev.algorithm.delta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StaticFunctions;
import snu.kdd.synonym.synonymRev.tools.WYK_HashMap;
import snu.kdd.synonym.synonymRev.tools.WYK_HashSet;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class JoinMHDeltaIndexStrong extends JoinMHDeltaIndex {

	public JoinMHDeltaIndexStrong( int indexK, int qgramSize, int deltaMax, Iterable<Record> indexedSet, Query query,
			StatContainer stat, int[] indexPosition, boolean addStat, boolean useIndexCount, int threshold ) {
		super( indexK, qgramSize, deltaMax, indexedSet, query, stat, indexPosition, addStat, useIndexCount, threshold );
		// TODO Auto-generated constructor stub
	}

	@Override
	public void joinOneRecordThres( Record recS, Set<IntegerPair> rslt, Validator checker, int threshold, boolean oneSideJoin ) {
		long ts = System.nanoTime();
	    Set<Record> candidates = new WYK_HashSet<Record>(100);

	    boolean isUpperRecord = threshold <= 0 ? true : recS.getEstNumTransformed() > threshold;

	    List<Object2IntOpenHashMap<Record>> candidatesCount = new ArrayList<Object2IntOpenHashMap<Record>>();
	    for ( int delta_s=0; delta_s<=deltaMax; ++delta_s ) {
	    	Object2IntOpenHashMap<Record> candidatesCountDelta = new Object2IntOpenHashMap<Record>();
			candidatesCountDelta.defaultReturnValue(0);
			candidatesCount.add( candidatesCountDelta );
	    }

	    List<List<Set<QGram>>> candidateQGrams = getCandidatePQGrams( recS );
//	    for ( List<Set<QGram>> qgrams_pos : candidateQGrams ) {
//	        for ( Set<QGram> qgrams_delta : qgrams_pos ) {
//	            this.candQGramCount += qgrams_delta.size();
//	        }
//	    }
	    long afterCandQGramTime = System.nanoTime();

	    int[] range = recS.getTransLengths();
	    for (int i = 0; i < indexK; ++i) {
	        int actualIndex = indexPosition[i];
	        if ( candidateQGrams.size() <= actualIndex ) continue;
//	        if (range[0] <= actualIndex) {
//	            continue;
//	        }

	        // Given a position
	        List<Set<QGram>> cand_qgrams_pos = candidateQGrams.get( actualIndex );
	        List<Set<Record>> ithCandidates = new ArrayList<Set<Record>>();
	        for ( int delta_s=0; delta_s<=deltaMax; ++delta_s ) ithCandidates.add( new WYK_HashSet<>() );

	        List<WYK_HashMap<QGram, List<Record>>> map = index.get(i);

	        for ( int delta_s=0; delta_s<=deltaMax; ++delta_s ) {
	            if ( cand_qgrams_pos.size() <= delta_s ) break;
	            this.candQGramCount += cand_qgrams_pos.get( delta_s ).size();
	            for ( QGram qgram : cand_qgrams_pos.get( delta_s ) ) {
	            	if ( !isInTPQ( qgram, i, delta_s ) ) continue;
	                for ( int delta_t=0; delta_t<=deltaMax-delta_s; ++delta_t ) {
	                    List<Record> recordList = map.get( delta_t ).get( qgram );
	                    if ( recordList == null ) {
	                    	++emptyListCount;
	                        continue;
	                    }

	                    // Perform length filtering.
	                    for ( Record otherRecord : recordList ) {
	                        if (!isUpperRecord && otherRecord.getEstNumTransformed() <= threshold) {
	                            continue;
	                        }

	                        int[] otherRange = null;

	                        if (query.oneSideJoin) {
	                            otherRange = new int[2];
	                            otherRange[0] = otherRecord.getTokenCount();
	                            otherRange[1] = otherRecord.getTokenCount();
	                        } else otherRange = otherRecord.getTransLengths();

	                        if ( !useLF || StaticFunctions.overlap(otherRange[0]-deltaMax, otherRange[1], range[0]-deltaMax, range[1])) {
//	                        	if ( otherRecord.getID() == 5158 ) System.out.println( qgram+", "+i+", "+delta_s+", "+delta_t );
	                            for ( int delta=delta_s; delta<=deltaMax-delta_t; ++delta) ithCandidates.get( delta ).add(otherRecord);
	                        }
	                        else ++checker.lengthFiltered;
	                    } // end for otherRecord in recordList
	                } // end for delta_t
	            } // end for qgram in cand_qgrams_pos
	        } // end for delta_s
			for ( int delta=0; delta<=deltaMax; ++delta )
				for (Record otherRecord : ithCandidates.get( delta )) candidatesCount.get( delta ).addTo( otherRecord, 1 );
	    } // end for i from 0 to indexK

	    for ( int delta=0; delta<=deltaMax; ++delta ) {
			ObjectIterator<Object2IntMap.Entry<Record>> iter = candidatesCount.get( delta ).object2IntEntrySet().iterator();
			while (iter.hasNext()) {
				Object2IntMap.Entry<Record> entry = iter.next();
				Record record = entry.getKey();
				if ( candidates.contains( record ) ) continue;
				int recordCount = entry.getIntValue();
				// recordCount: number of lists containing the target record given recS
				// indexedCountList.getInt(record): number of pos qgrams which are keys of the target record in the index
	//	        if ( recS.getID() == 5158 ) System.out.println( record.getID()+", "+recordCount );

				if ( !usePQF || ( indexedCountList.getInt(record) <= recordCount || indexedCountList.getInt(recS) <= recordCount ) ) {
	//	        if ( Math.min( Math.max( record.size()-deltaMax, 1 ), indexedCountList.getInt(record) ) <= recordCount || indexedCountList.getInt(recS) <= recordCount)
					candidates.add(record);
				}
				else ++checker.pqgramFiltered;
			}
	    }
	    long afterFilterTime = System.nanoTime();

	    equivComparisons += candidates.size();
	    predictCount += candidates.size();
	    for (Record recR : candidates) {
	        int compare = checker.isEqual(recS, recR);
	        if (compare >= 0) {
//					rslt.add(new IntegerPair(recS.getID(), recR.getID()));
	            AlgorithmTemplate.addSeqResult( recS, recR, rslt, query.selfJoin );
	        }
	    }
	    long afterEquivTime = System.nanoTime();

	    this.candQGramCountTime += afterCandQGramTime - ts;
	    this.filterTime += afterFilterTime - afterCandQGramTime;
	    this.equivTime += afterEquivTime - afterFilterTime;
	} // end joinOneRecordThres
}
