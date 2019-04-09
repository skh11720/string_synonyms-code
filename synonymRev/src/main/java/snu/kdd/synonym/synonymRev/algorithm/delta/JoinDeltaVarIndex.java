package snu.kdd.synonym.synonymRev.algorithm.delta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.index.AbstractIndex;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.ResultSet;
import snu.kdd.synonym.synonymRev.tools.Stat;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StaticFunctions;
import snu.kdd.synonym.synonymRev.tools.WYK_HashMap;
import snu.kdd.synonym.synonymRev.tools.WYK_HashSet;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class JoinDeltaVarIndex extends AbstractIndex {

	protected final ArrayList<ArrayList<WYK_HashMap<QGram, List<Record>>>> idxPD;
	/*
	 * Inverted lists whose key are (pos, delta, q-gram).
	 */

	protected final ArrayList<Set<QGram>> pos2QGramSetMap;
	/*
	 * A map from a position to the set of q-grams in the idxPD.
	 */

	protected final QGramDeltaGenerator qdgen;
	/*
	 * generate the delta-variants of an input q-gram.
	 */
	
	protected final Object2IntOpenHashMap<Record> indexedCountList;
	/*
	 * Keep the number of indexed positions for every string in the indexedSet.
	 */
	
	protected final ObjectArrayList<Record> shortList;
	/*
	 * A list of records in indexedSet whose length is not larger than deltaMax.
	 */

	protected final Query query;
	protected final int indexK;
	protected final int qSize;
	protected final int deltaMax;
	protected final int idxForDist; // 0: lcs, 1: edit
	protected final boolean isSelfJoin;
  
	protected long candQGramCount = 0;
	protected long nCandByPQF = 0;
	protected long nCandByLen = 0;
	protected long candQGramCountTime = 0;
	protected long filterTime = 0;
	protected long verifyTime = 0;
	
	protected final QGram qgram_pad;
	protected final QGram qgramDelta_pad;
	public static boolean useLF = true;
	public static boolean usePQF = true;
	public static boolean useSTPQ = true;
	
	private final IntArrayList firstKPosArr; // does not need to be sorted
	public AlgStat algstat = new AlgStat();

	public JoinDeltaVarIndex( Query query, int indexK, int qSize, int deltaMax, String dist ) {
		/*
		 * methods called in here: insertRecordIntoIdxPD(Record)
		 */
		this.query = query;
		this.indexK = indexK;
		this.qSize = qSize;
		this.deltaMax = deltaMax;
		if ( dist.equals("lcs") ) idxForDist = 0;
		else idxForDist = 1;
		this.isSelfJoin = query.selfJoin;
		this.qdgen = new QGramDeltaGenerator(qSize, deltaMax);

		this.idxPD = new ArrayList<ArrayList<WYK_HashMap<QGram, List<Record>>>>();
		this.pos2QGramSetMap = new ArrayList<Set<QGram>>();
		this.indexedCountList = new Object2IntOpenHashMap<>();
		this.shortList = new ObjectArrayList<>();
		this.firstKPosArr = new IntArrayList();
		for ( int k=0; k<indexK; ++k ) this.firstKPosArr.add(k); // First Top K
	
		int[] tokens = new int[qSize];
		Arrays.fill( tokens, Integer.MAX_VALUE );
		qgram_pad = new QGram( tokens );
		tokens = new int[qSize + deltaMax];
		Arrays.fill( tokens, Integer.MAX_VALUE );
		qgramDelta_pad = new QGram( tokens );
	}
	
	public void build() {
		// build idxPD
		for ( Record recT : query.indexedSet.recordList ) {
			if ( recT.size() <= deltaMax ) shortList.add(recT);
			insertRecordIntoIdxPD(recT);
		}
			
		// build pos2QGramSetMap using idxPD
		for ( int k=0; k<idxPD.size(); ++k ) {
			pos2QGramSetMap.add( new ObjectOpenHashSet<>() );
			ArrayList<WYK_HashMap<QGram, List<Record>>> idxPD_k = idxPD.get(k);
			Set<QGram> qgramSet_k = pos2QGramSetMap.get(k);
			for ( int d=0; d<idxPD_k.size(); ++d ) qgramSet_k.addAll( idxPD_k.get(d).keySet() );
		}
	}

	protected void insertRecordIntoIdxPD( Record recT ) {
		/*
		 * Insert a record in indexedSet (T) into idxPD.
		 * Additionally, update indexedCountList.
		 * 
		 * Supposed to be used in the index with the best top k positions.
		 * Calls getIndexPosition(Record).
		 */
		IntArrayList posList = getIndexPosition(recT);
		if ( posList == null ) return;
		int maxPosition = posList.stream().max(Integer::compare).get().intValue();
		List<List<QGram>> availableQGrams = recT.getSelfQGrams(qSize+deltaMax, maxPosition + 1); // pos -> delta -> qgram
		int indexedCount = 0;

		for ( int actualIndex : posList ) {
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
					++algstat.idxQGramCount;
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

	protected IntArrayList getIndexPosition( Record recT ) {
		return this.firstKPosArr;
	}

	@Override
	public void joinOneRecord( Record recS, ResultSet rslt, Validator checker ) {
		long ts = System.nanoTime();

		// build the available q-grams from recS.
		List<List<Set<QGram>>> availableQGrams = getAvailableQGrams(recS);
		long afterCandQGramTime = System.nanoTime();

		Object2IntOpenHashMap<Record> candidatesCount = getCandidatesCount(recS, availableQGrams);

		Set<Record> candidates = getCandidates(recS, candidatesCount);
		long afterFilterTime = System.nanoTime();

		verify(recS, candidates, checker, rslt);
		long afterEquivTime = System.nanoTime();

		this.candQGramCountTime += afterCandQGramTime - ts;
		this.filterTime += afterFilterTime - afterCandQGramTime;
		this.verifyTime += afterEquivTime - afterFilterTime;
		algstat.candQGramCount = this.candQGramCount;
		algstat.numVerified = checker.checked;
		algstat.candFilterTime = (this.candQGramCountTime + this.filterTime)/1e6;
		algstat.verifyTime = this.verifyTime/1e6;
	} // end joinOneRecord

	protected List<List<Set<QGram>>> getAvailableQGrams( final Record recS ) {
		List<List<Set<QGram>>> availableQGrams = null;
		if ( useSTPQ ) availableQGrams = getVarSTPQ(recS, true);
		else availableQGrams = getVarTPQ(recS, true);
		return availableQGrams;
	}

	public List<List<Set<QGram>>> getVarSTPQ( Record rec, boolean usePruning ) {
		/*
		 * Return the lists of delta-variant positional qgrams in STPQ, where each list is indexed by pos and delta.
		 * If usePruning is true, prune qgrams which do not appear in the index (by referring idxPD and pos12QGramSetMap).
		 * key of the result: (pos, delta)
		 */
		boolean debug = false;
//		if ( rec.getID() == 598 ) debug = true;
		List<List<QGram>> varSTPQ = null;
		if (usePruning) varSTPQ = rec.getQGrams( qSize+deltaMax, idxPD.size() );
		else varSTPQ = rec.getQGrams( qSize+deltaMax );
		if (debug) {
			System.out.println( "varSTPQ:" );
			for ( List<QGram> qgramList : varSTPQ ) {
				for ( QGram qgram : qgramList ) System.out.println( qgram );
			}
		}
		
		List< List<Set<QGram>>> candidatePQGrams = new ArrayList<List<Set<QGram>>>();
		for ( int k=0; k<varSTPQ.size(); ++k ) {
			if ( usePruning && k >= idxPD.size() ) break;
			boolean qgram_pad_appended = false;
			List<Set<QGram>> cand_pos = new ArrayList<Set<QGram>>();
			for ( int d=0; d<=deltaMax; ++d ) cand_pos.add( new WYK_HashSet<QGram>() );
			for ( QGram qgram : varSTPQ.get(k) ) {
				if ( !qgram_pad_appended && qSize+deltaMax > 1 && qgram.qgram[1] == Integer.MAX_VALUE && k < varSTPQ.size()-1 ) {
					varSTPQ.get( k+1 ).add( qgramDelta_pad );
					qgram_pad_appended = true;
				}
//			List<QGram> qgrams = new ArrayList<QGram>();
				if (debug) System.out.println( "qgram: "+qgram );
				if (debug) System.out.println( "qgramDelta: "+qdgen.getQGramDelta( qgram ) );
				for ( Entry<QGram, Integer> entry: qdgen.getQGramDelta( qgram ) ) {
					QGram qgramDelta = entry.getKey();
					int delta_s = entry.getValue();
					if ( usePruning && !pos2QGramSetMap.get(k).contains(qgramDelta) ) continue;
					cand_pos.get(delta_s).add(qgramDelta);
				} // end for (qgramDelta, delta)
//				qgrams.add( qgram );
			} // end for qgram in availableQGrams
			candidatePQGrams.add( cand_pos );
		} // end for k
		return candidatePQGrams;
//		return rec.getQGrams( qSize, maxPosition+1 );
	}
	
	protected List<List<Set<QGram>>> getVarTPQ( Record rec, boolean usePruning ) {
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
				if ( usePruning && k >= idxPD.size() ) break;
				QGram qgram = selfQGramsList.get( k ).get( 0 );
				for ( Entry<QGram, Integer> entry: qdgen.getQGramDelta( qgram ) ) {
					QGram qgramDelta = entry.getKey();
					int delta_t = entry.getValue();
					if ( usePruning && !pos2QGramSetMap.get(k).contains( qgramDelta ) ) continue;
					availableQGrams.get(k).get(delta_t).add( qgramDelta );
				}
			}
		}
		return availableQGrams;
	}

	protected Object2IntOpenHashMap<Record> getCandidatesCount(final Record recS, List<List<Set<QGram>>> availableQGrams ) {
		return getCandidatesCount(recS, availableQGrams, null);
	}

	protected Object2IntOpenHashMap<Record> getCandidatesCount(final Record recS, List<List<Set<QGram>>> availableQGrams, Set<Record> candidates ) {
		Object2IntOpenHashMap<Record> candidatesCount = new Object2IntOpenHashMap<Record>();
		candidatesCount.defaultReturnValue(0);
		int[] rangeS = recS.getTransLengths();
		for ( int k=0; k<idxPD.size(); ++k ) {
//			System.out.println(availableQGrams.size()+", "+k);
			if ( availableQGrams.size() <= k ) break;

			// Given a position
			List<Set<QGram>> cand_qgrams_pos = availableQGrams.get(k);
			List<WYK_HashMap<QGram, List<Record>>> map = idxPD.get(k);
			Set<Record> kthCandidates = new WYK_HashSet<Record>();

			for ( int delta_s=0; delta_s<=deltaMax; ++delta_s ) {
				int delta_t_max = getDeltaTMax(delta_s);
				if ( cand_qgrams_pos.size() <= delta_s ) break;
				this.candQGramCount += cand_qgrams_pos.get( delta_s ).size();
//				System.out.println("AKAJSKDLSD\t"+cand_qgrams_pos.get(delta_s).size() );
				for ( QGram qgram : cand_qgrams_pos.get( delta_s ) ) {
					for ( int delta_t=0; delta_t<=delta_t_max; ++delta_t ) {
						List<Record> recordList = map.get( delta_t ).get( qgram );
						if ( recordList == null ) continue;

						// Perform length filtering.
						for ( Record recT : recordList ) {
							if ( candidates != null && !candidates.contains(recT) ) continue;
							if ( !useLF || StaticFunctions.overlap(rangeS[0]-deltaMax, rangeS[1]+deltaMax, recT.size(), recT.size())) {
//		                        	if ( otherRecord.getID() == 5158 ) System.out.println( qgram+", "+i+", "+delta_s+", "+delta_t );
								kthCandidates.add(recT);
							}
						} // end for otherRecord in recordList
					} // end for delta_t
				} // end for qgram in cand_qgrams_pos
			} // end for delta_s

			for (Record recT : kthCandidates) candidatesCount.addTo( recT, 1 );
//			System.out.println(k+", "+kthCandidates.size());
		} // end for k
		return candidatesCount;
	}

	protected Set<Record> getCandidates(final Record recS, Object2IntOpenHashMap<Record> candidatesCount ) {
		return getCandidates(recS, candidatesCount, null);
	}

	protected Set<Record> getCandidates(final Record recS, Object2IntOpenHashMap<Record> candidatesCount, Set<Record> oldCandidates ) {
		Set<Record> candidates = new WYK_HashSet<Record>(100);
		int[] rangeS = recS.getTransLengths();
		for ( Object2IntMap.Entry<Record> entry : candidatesCount.object2IntEntrySet() ) {
			Record recT = entry.getKey();
			if ( oldCandidates != null && !oldCandidates.contains(recT) ) continue;
			int recordCount = entry.getIntValue();
			// recordCount: number of lists containing the target record given recS
			// indexedCountList.getInt(record): number of pos qgrams which are keys of the target record in the index
//			if ( recS.getID() == 138 && recT.getID() == 0 ) System.out.println( indexedCountList.getInt(recT)+", "+availableQGrams.size()+", "+recordCount );

			if ( !usePQF || ( indexedCountList.getInt(recT) <= recordCount || Math.max(rangeS[0], recT.size())-deltaMax <= recordCount ) ) {
				candidates.add(recT);
			}
		}
		final int thisNCandByPQF = candidates.size();
		nCandByPQF += candidates.size();

		if ( rangeS[0] <= deltaMax ) {
			for ( Record recT : shortList ) {
				if ( oldCandidates != null && !oldCandidates.contains(recT) ) continue;
				candidates.add(recT);
			}
		}
		nCandByLen += candidates.size() - thisNCandByPQF;
		return candidates;
	}

	protected void verify(final Record recS, Set<Record> candidates, Validator checker, ResultSet rslt ) {
		for (Record recT : candidates) {
//		    	try { bw.write( recS.getID()+"\t"+recR.getID()+"\n" ); bw.flush(); }
//		    	catch ( IOException e ) { e.printStackTrace(); }
			if ( rslt.contains(recS, recT) ) continue;
			int compare = checker.isEqual(recS, recT);
			if (compare >= 0) {
				rslt.add(recS, recT);
			}
		}
	}
	
	@Override
	protected void postprocessAfterJoin( StatContainer stat ) {
		stat.add(Stat.INDEX_SIZE, indexSize());
		stat.add(Stat.LEN_INDEX_SIZE, shortList.size());
		stat.add(Stat.CAND_PQGRAM_COUNT, candQGramCount );
		stat.add(Stat.CAND_BY_PQGRAM, nCandByPQF);
		stat.add(Stat.CAND_BY_LEN, nCandByLen);
		stat.add(Stat.CAND_PQGRAM_TIME, candQGramCountTime/1e6);
		stat.add(Stat.FILTER_TIME, filterTime/1e6);
		stat.add(Stat.VERIFY_TIME, verifyTime/1e6);
	}

	protected int nInvList() {
		int n = 0;
		for (int i=0; i<idxPD.size(); i++) {
			ArrayList<WYK_HashMap<QGram, List<Record>>> index_pos = idxPD.get( i );
			for ( WYK_HashMap<QGram, List<Record>> index_pos_delta : index_pos ) {
				n += index_pos_delta.size();
			}
		}
		return n;
	}

	protected int indexSize() {
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

	protected int getDeltaTMax( int delta_s ) {
		if ( this.idxForDist == 0 ) return this.deltaMax - delta_s;
		else return this.deltaMax;
	}
	
	public class AlgStat {
		public long idxQGramCount = 0;
		public long candQGramCount = 0;
		public long numVerified = 0;

		public double candFilterTime = 0;
		public double verifyTime = 0;
	}
}
