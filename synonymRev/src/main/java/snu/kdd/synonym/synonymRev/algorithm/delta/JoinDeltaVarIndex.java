package snu.kdd.synonym.synonymRev.algorithm.delta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.index.AbstractIndex;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StaticFunctions;
import snu.kdd.synonym.synonymRev.tools.WYK_HashMap;
import snu.kdd.synonym.synonymRev.tools.WYK_HashSet;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class JoinDeltaVarIndex extends AbstractIndex {

	protected ArrayList<ArrayList<WYK_HashMap<QGram, List<Record>>>> idx;
	/*
	 * Inverted lists whose key are (pos, delta, qgram).
	 */

	protected final QGramDeltaGenerator qdgen;
	/*
	 * generate the delta-variants of an input qgram.
	 */
	
	protected Object2IntOpenHashMap<Record> indexedCountList;
	/*
	 * Keep the number of indexed positions for every string in the indexedSet.
	 */

	protected final int indexK;
	protected final int[] indexPosition;
	protected final int qSize;
	protected final int deltaMax;
	protected final boolean isSelfJoin;
  
	protected int maxPosition = 0;

	protected long candQGramCount = 0;
	protected long equivComparisons = 0;

	protected long candQGramCountTime = 0;
	protected long indexTime = 0;
	protected long filterTime = 0;
	protected long verifyTime = 0;
	
	protected final QGram qgram_pad;
	protected final QGram qgramDelta_pad;
	public static boolean useLF = true;
	public static boolean usePQF = true;

	public JoinDeltaVarIndex( int indexK, int qSize, int deltaMax, Query query, StatContainer stat ) {
		long ts = System.nanoTime();
		this.indexK = indexK;
		this.indexPosition = new int[indexK];
		setupIndexPosition();
		this.maxPosition = Arrays.stream(indexPosition).max().getAsInt();
		this.qSize = qSize;
		this.deltaMax = deltaMax;
		this.isSelfJoin = query.selfJoin;
		this.qdgen = new QGramDeltaGenerator(qSize, deltaMax);
		this.indexedCountList = new Object2IntOpenHashMap<>();
		
		int[] tokens = new int[qSize];
		Arrays.fill( tokens, Integer.MAX_VALUE );
		qgram_pad = new QGram( tokens );
		tokens = new int[qSize + deltaMax];
		Arrays.fill( tokens, Integer.MAX_VALUE );
		qgramDelta_pad = new QGram( tokens );

		this.idx  = new ArrayList<ArrayList<WYK_HashMap<QGram, List<Record>>>>();
		for ( int k=0; k<indexK; ++k ) {
			ArrayList<WYK_HashMap<QGram, List<Record>>> idxPos = new ArrayList<>();
			for ( int d=0; d<=deltaMax; ++d ) idxPos.add( new WYK_HashMap<>( query.indexedSet.size() ) );
			idx.add(idxPos);
		}
		
		for ( Record recT : query.indexedSet.recordList ) {
//			int lenT = recT.size();
			List<List<QGram>> availableQGrams = recT.getSelfQGrams(qSize+deltaMax, maxPosition + 1); // pos -> delta -> qgram
			int indexedCount = 0;

			for ( int i=0;i <indexPosition.length; ++i) {
				int actualIndex = indexPosition[i];
				if ( availableQGrams.size() <= actualIndex ) continue;
				++indexedCount;

				ArrayList<WYK_HashMap<QGram, List<Record>>> idxPos = idx.get(actualIndex);
				List<QGram> qgramList = availableQGrams.get(actualIndex);

				/*
				 * For each qgram, consdier all combinations of tokens given deltaMax.
				 * For instance, given q=2 and deltaMax=2, from a qgram ABCD, 
				 * delta=0: ABCD, 
				 * delta=1: ABC, ABD, ACD, BCD, 
				 * delta=2: AB, AC, AD, BC, BD, CD are generated.
				 */
				for (QGram qgram : qgramList) {
					/*
					 * An example
					 * q=3, deltaMax=2
					 * Given an extended qgram ABCDE
					 * All combinations of (3+2) choose 3: [0,1,2], [0,1,3], ...
					 * [0,1,2]: ABC, delta=0
					 * [0,1,3]: ABD, delta=1
					 * [0,1,4]: ABE, delta=2
					 * [0,2,3]: ACD, delta=1
					 * [0,2,4]: ACE, delta=2
					 * [0,3,4]: ADE, delta=2
					 * [1,2,3]: BCD, delta=1
					 * [1,2,4]: BCE, delta=2
					 * [1,3,4]: BDE, delta=2
					 * ...
					 */

					for ( Entry<QGram, Integer> entry : qdgen.getQGramDelta( qgram ) ) {
						QGram qgramDelta = entry.getKey();
						int delta = entry.getValue();
//						if ( recT.getID() == 677 ) System.out.println("recT qgramDelta: "+qgramDelta+", "+actualIndex+", "+delta);
						List<Record> recordList = idxPos.get( delta ).get( qgramDelta );
						if ( recordList == null ) {
							recordList = new ObjectArrayList<Record>();
							idxPos.get( delta ).put( qgramDelta, recordList );
						}
						recordList.add( recT );
					}
				} // end for qgram in qgramList
			} // end for i of indexPosition
			indexedCountList.put( recT, indexedCount );
		} // end for recT in indexedSet
		indexTime = System.nanoTime() - ts;
		
		stat.add("Stat_InvListCount", nInvList());
		stat.add("Stat_InvSize", indexSize());
	}

	protected void setupIndexPosition() {
		for ( int k=0; k<indexK; ++k ) indexPosition[k] = k;
	}

//	protected List<List<QGram>> getCandidatePQGrams( Record rec ) {
//		List<List<QGram>> availableQGrams = rec.getQGrams( qSize );
//		List<List<QGram>> candidatePQGrams = new ArrayList<List<QGram>>();
//		for ( int k=0; k<availableQGrams.size(); ++k ) {
//			List<QGram> qgrams = new ArrayList<QGram>();
//			for ( QGram qgram : availableQGrams.get( k ) ) {
//				for ( int kd=Math.max(0, k-deltaMax); kd<=k+deltaMax; ++kd ) {
//					if ( kd >= idxByPQgram.size() ) continue;
//					WYK_HashMap<QGram, List<Record>> curidx = idxByPQgram.get( kd );
//					if ( !curidx.containsKey( qgram ) ) continue;
//					qgrams.add( qgram );
//					break;
//				}
//			}
//			candidatePQGrams.add( qgrams );
//		}
//		return candidatePQGrams;
//	}
	
	protected List<List<Set<QGram>>> getCandidatePQGrams( Record rec ) {
		/*
		 * Return the lists of qgrams, where each list is indexed by pos and delta.
		 * key: (pos, delta)
		 */
		boolean debug = false;
//		if ( rec.getID() == 598 ) debug = true;
		List<List<QGram>> availableQGrams = rec.getQGrams( qSize+deltaMax, maxPosition+1 );
		if (debug) {
			System.out.println( "availableQGrams:" );
			for ( List<QGram> qgramList : availableQGrams ) {
				for ( QGram qgram : qgramList ) System.out.println( qgram );
			}
		}
		
		List< List<Set<QGram>>> candidatePQGrams = new ArrayList<List<Set<QGram>>>();
		for ( int k=0; k<availableQGrams.size(); ++k ) {
			if ( k >= idx.size() ) break;
			boolean qgram_pad_appended = false;
			List<WYK_HashMap<QGram, List<Record>>> curidx = idx.get( k );
			List<Set<QGram>> cand_pos = new ArrayList<Set<QGram>>();
			for ( int d=0; d<=deltaMax; ++d ) cand_pos.add( new WYK_HashSet<QGram>() );
			for ( QGram qgram : availableQGrams.get( k ) ) {
				if ( !qgram_pad_appended && qSize+deltaMax > 1 && qgram.qgram[1] == Integer.MAX_VALUE && k < availableQGrams.size()-1 ) {
					availableQGrams.get( k+1 ).add( qgramDelta_pad );
					qgram_pad_appended = true;
				}
//			List<QGram> qgrams = new ArrayList<QGram>();
				if (debug) System.out.println( "qgram: "+qgram );
				if (debug) System.out.println( "qgramDelta: "+qdgen.getQGramDelta( qgram ) );
				for ( Entry<QGram, Integer> entry: qdgen.getQGramDelta( qgram ) ) {
					QGram qgramDelta = entry.getKey();
					int delta_s = entry.getValue();
					for ( int delta_t=0; delta_t<=deltaMax; ++delta_t ) {
						if ( curidx.get( delta_t ).containsKey( qgramDelta ) ) {
							if (debug) System.out.println( "cand_pos.get("+delta_s+").add: "+qgramDelta );
							cand_pos.get( delta_s ).add( qgramDelta );
							break;
						}
					}
				} // end for (qgramDelta, delta)
//				qgrams.add( qgram );
			} // end for qgram in availableQGrams
			candidatePQGrams.add( cand_pos );
		} // end for k
		return candidatePQGrams;
//		return rec.getQGrams( qSize, maxPosition+1 );
	}

	@Override
	public void joinOneRecord( Record recS, Set<IntegerPair> rslt, Validator checker ) {
		long ts = System.nanoTime();
		Set<Record> candidates = new WYK_HashSet<Record>(100);
		Object2IntOpenHashMap<Record> candidatesCount = new Object2IntOpenHashMap<Record>();
		candidatesCount.defaultReturnValue(0);

		List<List<Set<QGram>>> candidateQGrams = getCandidatePQGrams( recS );
//		    for ( List<Set<QGram>> qgrams_pos : candidateQGrams ) {
//		        for ( Set<QGram> qgrams_delta : qgrams_pos ) {
//		            this.candQGramCount += qgrams_delta.size();
//		        }
//		    }
		long afterCandQGramTime = System.nanoTime();

		int[] range = recS.getTransLengths();
		for (int i = 0; i < indexK; ++i) {
			int actualIndex = indexPosition[i];
			if ( candidateQGrams.size() <= actualIndex ) continue;
//		        if (range[0] <= actualIndex) {
//		            continue;
//		        }

			// Given a position
			List<Set<QGram>> cand_qgrams_pos = candidateQGrams.get( actualIndex );
			Set<Record> ithCandidates = new WYK_HashSet<Record>();

			List<WYK_HashMap<QGram, List<Record>>> map = idx.get(i);

			for ( int delta_s=0; delta_s<=deltaMax; ++delta_s ) {
				if ( cand_qgrams_pos.size() <= delta_s ) break;
				this.candQGramCount += cand_qgrams_pos.get( delta_s ).size();
				for ( QGram qgram : cand_qgrams_pos.get( delta_s ) ) {
					for ( int delta_t=0; delta_t<=deltaMax; ++delta_t ) {
						List<Record> recordList = map.get( delta_t ).get( qgram );
						if ( recordList == null ) continue;

						// Perform length filtering.
						for ( Record recT : recordList ) {
							if ( !useLF || StaticFunctions.overlap(range[0]-deltaMax, range[1]+deltaMax, recT.size(), recT.size())) {
//		                        	if ( otherRecord.getID() == 5158 ) System.out.println( qgram+", "+i+", "+delta_s+", "+delta_t );
								ithCandidates.add(recT);
							}
							else ++checker.lengthFiltered;
						} // end for otherRecord in recordList
					} // end for delta_t
				} // end for qgram in cand_qgrams_pos
			} // end for delta_s

			for (Record recT : ithCandidates) candidatesCount.addTo( recT, 1 );
		} // end for i from 0 to indexK

		for ( Object2IntMap.Entry<Record> entry : candidatesCount.object2IntEntrySet() ) {
			Record record = entry.getKey();
			int recordCount = entry.getIntValue();
			// recordCount: number of lists containing the target record given recS
			// indexedCountList.getInt(record): number of pos qgrams which are keys of the target record in the index
//		        if ( recS.getID() == 598 && record.getID() == 677 ) System.out.println( record.getID()+", "+recordCount );

			if ( !usePQF || ( indexedCountList.getInt(record) <= recordCount || candidateQGrams.size() <= recordCount ) ) {
				candidates.add(record);
			}
			else ++checker.pqgramFiltered;
		}
		long afterFilterTime = System.nanoTime();

		equivComparisons += candidates.size();
		for (Record recR : candidates) {
//		    	try { bw.write( recS.getID()+"\t"+recR.getID()+"\n" ); bw.flush(); }
//		    	catch ( IOException e ) { e.printStackTrace(); }
			int compare = checker.isEqual(recS, recR);
			if (compare >= 0) {
//						rslt.add(new IntegerPair(recS.getID(), recR.getID()));
				AlgorithmTemplate.addSeqResult( recS, recR, rslt, isSelfJoin );
			}
		}
		long afterEquivTime = System.nanoTime();

		this.candQGramCountTime += afterCandQGramTime - ts;
		this.filterTime += afterFilterTime - afterCandQGramTime;
		this.verifyTime += afterEquivTime - afterFilterTime;
	} // end joinOneRecord

	@Override
	protected void postprocessAfterJoin( StatContainer stat ) {
		stat.add("Stat_EquivComparison", equivComparisons);
		stat.add("Stat_CandQGramCount", candQGramCount );
		stat.add("Stat_CandQGramCountTime", (long)(candQGramCountTime/1e6));
		stat.add("Stat_FilterTime", (long)(filterTime/1e6));
		stat.add("Stat_VerifyTime", (long)(verifyTime/1e6) );
	}

	private int nInvList() {
		int n = 0;
		for (int i=0; i<idx.size(); i++) {
			ArrayList<WYK_HashMap<QGram, List<Record>>> index_pos = idx.get( i );
			for ( WYK_HashMap<QGram, List<Record>> index_pos_delta : index_pos ) {
				n += index_pos_delta.size();
			}
		}
		return n;
	}

	private int indexSize() {
		int n = 0;
		for (int i=0; i<idx.size(); i++) {
			ArrayList<WYK_HashMap<QGram, List<Record>>> index_pos = idx.get( i );
			for ( WYK_HashMap<QGram, List<Record>> index_pos_delta : index_pos ) {
				for ( Entry<QGram, List<Record>> entry : index_pos_delta.entrySet() ) {
					n += entry.getValue().size();
				}
			}
		}
		return n;
	}
}
