package snu.kdd.synonym.synonymRev.algorithm.delta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.math3.util.KthSelector;

import java.util.Set;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
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

	protected ArrayList<ArrayList<WYK_HashMap<QGram, List<Record>>>> idxPD;
	/*
	 * Inverted lists whose key are (pos, delta, q-gram).
	 */

	protected ArrayList<Set<QGram>> pos2QGramSetMap;
	/*
	 * A map from a position to the set of q-grams in the idxPD.
	 */

	protected final QGramDeltaGenerator qdgen;
	/*
	 * generate the delta-variants of an input q-gram.
	 */
	
	protected Object2IntOpenHashMap<Record> indexedCountList;
	/*
	 * Keep the number of indexed positions for every string in the indexedSet.
	 */

	protected final int indexK;
	protected final int qSize;
	protected final int deltaMax;
	protected final boolean isSelfJoin;
  
//	protected int maxPosition = -1;

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
	public static boolean useSTPQ = true;
	
	private final int[] firstKPosArr;

	public JoinDeltaVarIndex( int indexK, int qSize, int deltaMax, Query query, StatContainer stat ) {
		long ts = System.nanoTime();
		this.indexK = indexK;
		this.qSize = qSize;
		this.deltaMax = deltaMax;
		this.isSelfJoin = query.selfJoin;
		this.qdgen = new QGramDeltaGenerator(qSize, deltaMax);
		this.indexedCountList = new Object2IntOpenHashMap<>();
		this.firstKPosArr = new int[indexK];
		for ( int k=0; k<indexK; ++k ) this.firstKPosArr[k] = k; // First Top K
	
		int[] tokens = new int[qSize];
		Arrays.fill( tokens, Integer.MAX_VALUE );
		qgram_pad = new QGram( tokens );
		tokens = new int[qSize + deltaMax];
		Arrays.fill( tokens, Integer.MAX_VALUE );
		qgramDelta_pad = new QGram( tokens );
	
		// build idxPD
		this.idxPD = new ArrayList<ArrayList<WYK_HashMap<QGram, List<Record>>>>();
		for ( Record recT : query.indexedSet.recordList ) insertRecordIntoIdxPD(recT);
			
		// build pos2QGramSetMap using idxPD
		this.pos2QGramSetMap = new ArrayList<Set<QGram>>();
		for ( int k=0; k<idxPD.size(); ++k ) {
			pos2QGramSetMap.add( new ObjectOpenHashSet<>() );
			ArrayList<WYK_HashMap<QGram, List<Record>>> idxPD_k = idxPD.get(k);
			Set<QGram> qgramSet_k = pos2QGramSetMap.get(k);
			for ( int d=0; d<idxPD_k.size(); ++d ) qgramSet_k.addAll( idxPD_k.get(d).keySet() );
		}
		indexTime = System.nanoTime() - ts;
		
		stat.add("Stat_InvListCount", nInvList());
		stat.add("Stat_InvSize", indexSize());
	}

	protected int[] getIndexPosition( Record recT ) {
		return this.firstKPosArr;
	}

	protected void insertRecordIntoIdxPD( Record recT ) {
		/*
		 * Insert a record in indexedSet (T) into idxPD.
		 * Additionally, update indexedCountList.
		 * 
		 * Supposed to be used in the index with the best top k positions.
		 */
		int[] idxPosArr = getIndexPosition(recT);
		int maxPosition = Arrays.stream(idxPosArr).max().getAsInt();
		List<List<QGram>> availableQGrams = recT.getSelfQGrams(qSize+deltaMax, maxPosition + 1); // pos -> delta -> qgram
		int indexedCount = 0;

		for ( int i=0; i<idxPosArr.length; ++i) {
			int actualIndex = idxPosArr[i];
			if ( availableQGrams.size() <= actualIndex ) continue;
			++indexedCount;

			while ( idxPD.size() <= actualIndex ) {
				ArrayList<WYK_HashMap<QGram, List<Record>>> idxPos = new ArrayList<>();
				for ( int d=0; d<=deltaMax; ++d ) idxPos.add( new WYK_HashMap<>() );
				idxPD.add(idxPos);
			}
			ArrayList<WYK_HashMap<QGram, List<Record>>> idxPD_k = idxPD.get(actualIndex);
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
					WYK_HashMap<QGram, List<Record>> idxPD_kd =  idxPD_k.get(delta);
//						if ( recT.getID() == 677 ) System.out.println("recT qgramDelta: "+qgramDelta+", "+actualIndex+", "+delta);
					if ( !idxPD_kd.containsKey(qgramDelta) ) idxPD_kd.put(qgramDelta, new ObjectArrayList<Record>());
//					System.out.println("AASSAAAS\t"+delta+", "+qgramDelta+", "+recT.getID());
					idxPD_kd.get(qgramDelta).add(recT);
				}
			} // end for qgram in qgramList
		} // end for i of indexPosition
		indexedCountList.put( recT, indexedCount );
	}

	protected List<List<Set<QGram>>> getCandidatePQGrams( Record rec ) {
		/*
		 * Return the lists of delta-variant positional qgrams in STPQ, where each list is indexed by pos and delta.
		 * key: (pos, delta)
		 */
		boolean debug = false;
//		if ( rec.getID() == 598 ) debug = true;
		List<List<QGram>> availableQGrams = rec.getQGrams( qSize+deltaMax, idxPD.size() );
		if (debug) {
			System.out.println( "availableQGrams:" );
			for ( List<QGram> qgramList : availableQGrams ) {
				for ( QGram qgram : qgramList ) System.out.println( qgram );
			}
		}
		
		List< List<Set<QGram>>> candidatePQGrams = new ArrayList<List<Set<QGram>>>();
		for ( int k=0; k<availableQGrams.size(); ++k ) {
			if ( k >= idxPD.size() ) break;
			boolean qgram_pad_appended = false;
			List<Set<QGram>> cand_pos = new ArrayList<Set<QGram>>();
			for ( int d=0; d<=deltaMax; ++d ) cand_pos.add( new WYK_HashSet<QGram>() );
			for ( QGram qgram : availableQGrams.get(k) ) {
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
					if ( !pos2QGramSetMap.get(k).contains(qgramDelta) ) continue;
					cand_pos.get(delta_s).add(qgramDelta);
				} // end for (qgramDelta, delta)
//				qgrams.add( qgram );
			} // end for qgram in availableQGrams
			candidatePQGrams.add( cand_pos );
		} // end for k
		return candidatePQGrams;
//		return rec.getQGrams( qSize, maxPosition+1 );
	}
	
	protected List<List<Set<QGram>>> getTPQ( Record rec ) {
		/*
		 * Return the lists of delta-variant positional qgrams in TPQ.
		 * This function is called instead of getCandidatePQGrams() when useSTPQ is false.
		 * key: (pos, delta)
		 */
		List<List<Set<QGram>>> availableQGrams = new ObjectArrayList<>();
		for ( Record exp : rec.expandAll() ) {
			List<List<QGram>> selfQGramsList = exp.getSelfQGrams(qSize+deltaMax, exp.size());
			while ( availableQGrams.size() < selfQGramsList.size() ) {
				List<Set<QGram>> qgrams_k = new ObjectArrayList<>();
				for ( int d=0; d<=deltaMax; ++d ) qgrams_k.add( new ObjectOpenHashSet<>() );
				availableQGrams.add( qgrams_k );
			}
			for ( int k=0; k<selfQGramsList.size(); ++k ) {
				if ( k >= idxPD.size() ) break;
				QGram qgram = selfQGramsList.get( k ).get( 0 );
				for ( Entry<QGram, Integer> entry: qdgen.getQGramDelta( qgram ) ) {
					QGram qgramDelta = entry.getKey();
					int delta_t = entry.getValue();
					if ( !pos2QGramSetMap.get(k).contains( qgramDelta ) ) continue;
					availableQGrams.get(k).get(delta_t).add( qgramDelta );
				}
			}
		}
		return availableQGrams;
	}

	@Override
	public void joinOneRecord( Record recS, Set<IntegerPair> rslt, Validator checker ) {
		long ts = System.nanoTime();
		Set<Record> candidates = new WYK_HashSet<Record>(100);
		Object2IntOpenHashMap<Record> candidatesCount = new Object2IntOpenHashMap<Record>();
		candidatesCount.defaultReturnValue(0);

		// build the available q-grams from recS.
		List<List<Set<QGram>>> availableQGrams = null;
		if ( useSTPQ ) availableQGrams = getCandidatePQGrams(recS);
		else availableQGrams = getTPQ(recS);
		long afterCandQGramTime = System.nanoTime();
//		System.out.println("MBKDDFE");
//		System.out.println(availableQGrams.size());
//		for (List<Set<QGram>> qgram_k : availableQGrams ) {
//			for ( Set<QGram> qgramSet : qgram_k ) System.out.println(qgramSet.size());
//		}

		int[] range = recS.getTransLengths();
		for ( int k=0; k<idxPD.size(); ++k ) {
//			System.out.println(availableQGrams.size()+", "+k);
			if ( availableQGrams.size() <= k ) break;

			// Given a position
			List<Set<QGram>> cand_qgrams_pos = availableQGrams.get(k);
			List<WYK_HashMap<QGram, List<Record>>> map = idxPD.get(k);
			Set<Record> kthCandidates = new WYK_HashSet<Record>();

			for ( int delta_s=0; delta_s<=deltaMax; ++delta_s ) {
				if ( cand_qgrams_pos.size() <= delta_s ) break;
				this.candQGramCount += cand_qgrams_pos.get( delta_s ).size();
//				System.out.println("AKAJSKDLSD\t"+cand_qgrams_pos.get(delta_s).size() );
				for ( QGram qgram : cand_qgrams_pos.get( delta_s ) ) {
					for ( int delta_t=0; delta_t<=deltaMax; ++delta_t ) {
						List<Record> recordList = map.get( delta_t ).get( qgram );
						if ( recordList == null ) continue;

						// Perform length filtering.
						for ( Record recT : recordList ) {
							if ( !useLF || StaticFunctions.overlap(range[0]-deltaMax, range[1]+deltaMax, recT.size(), recT.size())) {
//		                        	if ( otherRecord.getID() == 5158 ) System.out.println( qgram+", "+i+", "+delta_s+", "+delta_t );
								kthCandidates.add(recT);
							}
							else ++checker.lengthFiltered;
						} // end for otherRecord in recordList
					} // end for delta_t
				} // end for qgram in cand_qgrams_pos
			} // end for delta_s

			for (Record recT : kthCandidates) candidatesCount.addTo( recT, 1 );
//			System.out.println(k+", "+kthCandidates.size());
		} // end for k

		for ( Object2IntMap.Entry<Record> entry : candidatesCount.object2IntEntrySet() ) {
			Record record = entry.getKey();
			int recordCount = entry.getIntValue();
			// recordCount: number of lists containing the target record given recS
			// indexedCountList.getInt(record): number of pos qgrams which are keys of the target record in the index
//		        if ( recS.getID() == 598 && record.getID() == 677 ) System.out.println( record.getID()+", "+recordCount );

			if ( !usePQF || ( indexedCountList.getInt(record) <= recordCount || availableQGrams.size() <= recordCount ) ) {
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
		for (int i=0; i<idxPD.size(); i++) {
			ArrayList<WYK_HashMap<QGram, List<Record>>> index_pos = idxPD.get( i );
			for ( WYK_HashMap<QGram, List<Record>> index_pos_delta : index_pos ) {
				n += index_pos_delta.size();
			}
		}
		return n;
	}

	private int indexSize() {
		int n = 0;
		for (int i=0; i<idxPD.size(); i++) {
			ArrayList<WYK_HashMap<QGram, List<Record>>> index_pos = idxPD.get( i );
			for ( WYK_HashMap<QGram, List<Record>> index_pos_delta : index_pos ) {
				for ( Entry<QGram, List<Record>> entry : index_pos_delta.entrySet() ) {
					n += entry.getValue().size();
				}
			}
		}
		return n;
	}
}
