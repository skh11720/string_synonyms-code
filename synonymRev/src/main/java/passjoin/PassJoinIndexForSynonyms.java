package passjoin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.synonym.synonymRev.algorithm.AbstractAlgorithm;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.index.AbstractIndex;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.ResultSet;
import snu.kdd.synonym.synonymRev.tools.Stat;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class PassJoinIndexForSynonyms extends AbstractIndex {
	/*
	 * Suppose a 2-way join with S (searchedList) and T (indexedList).
	 * partIndex[pid][clen]: info of candidate substrings of s in S where pid is the partition id and clen is the length of s.
	 * dist[lp]: the position of the first record t in the sorted T whose length is not less than lp.
	 * 
	 */
	
	private final int D, PN;
	private final List<Record> searchedList, indexedList;
	private int MaxDictLen = 0;
	private int MinDictLen = Integer.MAX_VALUE;
	
	List<PIndex>[][] partIndex;
	Long2ObjectMap<IntArrayList>[][] invLists;
	private int[][] partPos;
	private int[][] partLen;
	private int[] dist;
	
	private final Query query; // for debugging
	private final boolean isSelfJoin;
	public long candNum = 0;
	public long realNum = 0;
	private Comparator<Record> comp;
	private StatContainer stat;
	
	// values required to estimate coefficients
	public long indexTime = 0;
	public long expandTime = 0;
	public long joinTime = 0;
	public long verifyTime = 0;
	public long sumLenS = 0;
	public long sumLenT = 0;
	public long expCount = 0;
	public long verifyCount = 0;
	public double verifyCost = 0;
	/*
	 * veriifyCost is computed as the sum of len(searched record) * (#verified indexed records/#indexed records).
	 * then, coeff3 is computed as (verify time/#indexed records)/veriifyCost.
	 */
	// substring enumeration time = joinTime - verifyTime
	
	public double coeff1; // indexing time / sum of len(indexed records)
	public double coeff2; // substring enumeration time / sum of len(searched record)
	public double coeff3; // verification time / sum of (len(searched record) * #verified indexed records))
	
	public PassJoinIndexForSynonyms( Query query, int deltaMax, String distFunc, StatContainer stat ) {
		indexedList = new ObjectArrayList<>(query.indexedSet.recordList);
		searchedList = new ObjectArrayList<>(query.searchedSet.recordList);
//		indexedList = query.indexedSet.recordList;
//		searchedList = query.searchedSet.recordList;
		D = deltaMax;
//		N = indexedList.size();
		PN = D+1;
		this.query = query;
		this.isSelfJoin = query.selfJoin;
		this.stat = stat;

		/*
		 * Since the running time is small, the print messages are omitted.
		 */
		boolean debug = false;
		long ts = System.nanoTime();
		sort();
		init();
		prepare();
		buildIndex();
		long afterBuildIndex= System.nanoTime();
		indexTime = afterBuildIndex - ts;

		comp = new Comparator<Record>() {
			@Override
			public int compare( Record rec1, Record rec2 ) {
				if ( rec1.size() < rec2.size() ) return -1;
				else if ( rec1.size() > rec2.size() ) return 1;
				else {
					int[] arr1 = rec1.getTokensArray();
					int[] arr2 = rec2.getTokensArray();
					for ( int i=0; i<rec1.size(); ++i ) {
						if ( arr1[i] < arr2[i] ) return -1;
						else if ( arr1[i] > arr2[i] ) return 1;
					}
					return 0;
				}
			}
		};
	}
	
	private void sort() {
		Collections.sort( indexedList, comp );
	}
	
	private void init() {
		for ( Record recS : searchedList ) {
			MinDictLen = Math.min( MinDictLen, recS.getMinTransLength() );
			MaxDictLen = Math.max( MaxDictLen, recS.getMaxTransLength() );
		}
		for ( Record recT : indexedList ) {
			MinDictLen = Math.min( MinDictLen, recT.size() );
			MaxDictLen = Math.max( MaxDictLen, recT.size() );
		}
		partLen = new int[PN][MaxDictLen+1];
		partPos = new int[PN+1][MaxDictLen+1];
		partIndex = new ObjectArrayList[PN][MaxDictLen+1];
		for ( int i=0; i<PN; ++i ) {
			for ( int j=0; j<MaxDictLen+1; ++j ) partIndex[i][j] = new ObjectArrayList<PIndex>();
		}
		invLists = new Long2ObjectOpenHashMap[PN][MaxDictLen+1];
		for ( int i=0; i<PN; ++i ) {
			for ( int j=0; j<MaxDictLen+1; ++j ) invLists[i][j] = new Long2ObjectOpenHashMap<IntArrayList>();
		}
		dist = new int[MaxDictLen+2];
		
		Arrays.fill( dist, indexedList.size() );
		for ( int len=MinDictLen; len<=MaxDictLen; ++len ) {
			partPos[0][len] = 0;
			partLen[0][len] = len/PN;
			partPos[PN][len] = len;
		}
		
		for ( int pid=1; pid<PN; ++pid ) {
			for ( int len = MinDictLen; len<=MaxDictLen; ++len ) {
				partPos[pid][len] = partPos[pid-1][len] + partLen[pid-1][len];
				if ( pid == (PN - len % PN)) partLen[pid][len] = partLen[pid-1][len]+1;
				else partLen[pid][len] = partLen[pid-1][len];
			}
		}
	}
	
	private void prepare() {
		int clen = 0;	
		// indexedList must be sorted
//		for (int id = 0; id < indexedList.size(); id++) {
//			if ( clen == indexedList.get( id ).size() ) continue;
//			for (int lp = clen + 1; lp <= indexedList.get( id ).size(); lp++) dist[lp] = id;
//			clen = indexedList.get( id ).size();
//		}

//		clen = 0;
//		for (int id = 0; id < indexedList.size(); id++) {
//			if (clen == indexedList.get( id ).size()) continue;
//			clen = indexedList.get( id ).size();
		for ( clen=MinDictLen; clen<=MaxDictLen; ++clen) {
//			System.out.println( "prepare, clen: "+clen );

			for (int pid = 0; pid < PN; pid++)
			{
				for (int len = Math.max(clen - D, MinDictLen); len <= Math.min( clen + D, MaxDictLen ); len++)
				{
//					if (dist[len] == dist[len + 1]) continue;
					
					int stPos_start = Math.max( 0, partPos[pid][len] - pid );
					stPos_start = Math.max( stPos_start, partPos[pid][len] + (clen - len) - (D - pid) );
					int stPos_end = Math.min(clen - partLen[pid][len], partPos[pid][len] + pid );
					stPos_end = Math.min( stPos_end, partPos[pid][len] + (clen - len) + (D - pid) );

					for (int stPos = stPos_start; stPos <=stPos_end; ++stPos ) {
						partIndex[pid][clen].add( new PIndex(stPos, partPos[pid][len], partLen[pid][len], len) );
					}
				}
			}
		}
	}
	
	private void buildIndex() {
		for (int id = 0; id < indexedList.size(); id++) {
			int clen = indexedList.get( id ).size();
			sumLenT += clen;
			for (int partId = 0; partId < PN; partId++) {
				int pLen = partLen[partId][clen];
				int stPos = partPos[partId][clen];
				long hash = DJB_hash(indexedList.get( id ).getTokensArray(), stPos, pLen);
				if ( !invLists[partId][clen].containsKey( hash ) ) invLists[partId][clen].put( hash, new IntArrayList() );
				invLists[partId][clen].get( hash ).add( id );
			}
		}	
	}
	
	@Override
	public void postprocessAfterJoin( StatContainer stat ) {
		stat.add( Stat.NUM_VERIFY, candNum );

		// compute coefficients
		coeff1 = indexTime/sumLenT;
		coeff2 = (joinTime - verifyTime)/sumLenS;
		coeff3 = verifyTime/verifyCost;
		stat.add( "Stat_Index_indexTime", indexTime/1e6 );
		stat.add( "Stat_Index_substringTime", (joinTime - verifyTime)/1e6);
		stat.add( "Stat_Index_verifyTime", verifyTime/1e6 );
		stat.add( "Stat_Index_sumLenT", sumLenT );
		stat.add( "Stat_Index_sumLenS", sumLenS );
		stat.add( "Stat_Index_veriifyCost", verifyCost );
		stat.add( "Stat_Index_Coeff1", coeff1 );
		stat.add( "Stat_Index_Coeff2", coeff2 );
		stat.add( "Stat_Index_Coeff3", coeff3 );
	}
	
	@Override
	protected void joinOneRecord(Record recS, ResultSet rslt, Validator checker) {
		boolean debug = false;
//			if ( searchedList.get( id ).getID() < 10 ) debug = true;
//			if ( searchedList.get( id ).getID() == 677 ) debug = true;
//			if ( searchedList.get( id ).getID() == 681 ) debug = true;
		if (debug) System.out.println( "searched ID: "+recS.getID() );

		IntOpenHashSet answer_ids = new IntOpenHashSet();
//			Int2ObjectOpenHashMap<List<Record>> len2rec = new Int2ObjectOpenHashMap<>();
//			for ( Record exp : searchedList.get( id ).expandAll() ) {
//				int len = exp.size();
//				if ( !len2rec.containsKey( len ) ) len2rec.put( len, new ObjectArrayList<Record>() );
//				len2rec.get( len ).add( exp );
//			}
		
		long ts = System.nanoTime();
		if ( DEBUG.EstTooManyThreshold < recS.getEstNumTransformed() ) return;
		ArrayList<Record> expand_list = recS.expandAll();
		expCount += expand_list.size();
		long t_afterExpand = System.nanoTime();
		expandTime += t_afterExpand - ts;
//		Collections.sort( expand_list, comp ); sorting does not improve the performance.

		for ( Record exp : expand_list ) {
			IntOpenHashSet checked_ids = new IntOpenHashSet();
			int[] y = exp.getTokensArray();
			int clen = exp.size();
			int exp_checked = 0;
			sumLenS += clen;
			if (debug) System.out.println( "y_exp: "+exp.getID()+", "+Arrays.toString( y ) );
			for (int partId = 0; partId < PN; partId++) {
				if (debug) System.out.println( "partIndex["+partId+"]["+clen+"].size: "+partIndex[partId][clen].size() );
				for (int lp = 0; lp < partIndex[partId][clen].size(); lp++) {
					int stPos = partIndex[partId][clen].get( lp ).stPos;
					int Lo = partIndex[partId][clen].get( lp ).Lo;
					int pLen = partIndex[partId][clen].get( lp ).partLen;
					int len = partIndex[partId][clen].get( lp ).len;

					long hash_value = DJB_hash(y, stPos, pLen);
//						if (debug) System.out.println( "substring: "+Arrays.toString( Arrays.copyOfRange( y, stPos, stPos+pLen ) ) );
//						if (debug) System.out.println( "exists invLists: "+invLists[partId][len].containsKey( hash_value ) );
					if ( !invLists[partId][len].containsKey( hash_value ) ) continue;
					long ts_verify = System.nanoTime();
					for (int cand : invLists[partId][len].get( hash_value ) ) {
//							if (debug) System.out.println( "cand, checked: "+indexedList.get( cand ).getID()+", "+checked_ids.contains( cand ) );
						if ( !checked_ids.contains( cand ) ) {
							++candNum;
							++exp_checked;
//								if ( searchedList.get( id ).getID() == 440 && indexedList.get( cand ).getID() == 518 ) debug = true;
//								if ( searchedList.get( id ).getID() == 681 && indexedList.get( cand ).getID() == 478 ) debug = true;
							Record candRecord = indexedList.get(cand);
							int[] x = candRecord.getTokensArray();
//								if (debug) System.out.println( "y: "+searchedList.get( id ).getID()+", "+Arrays.toString( y ) );
//								if (debug) System.out.println( "x: "+indexedList.get( cand ).getID()+", "+Arrays.toString( x ) );
//								if (debug) System.out.println( "pardId: "+partId );
//								if (debug) System.out.println( "edit(x[0:0+Lo], y[0:0+stPos]): "+ Util.edit(x, y, partId, 0, 0, Lo, stPos) );
//								if (debug) System.out.println( "edit(x[Lo+pLen:], y[stPos+pLen:]): "+ Util.edit(x, y, D - partId, Lo + pLen, stPos + pLen, -1, -1) );
//								if (debug) System.out.println( "edit(x, y): "+ Util.edit(x, y, D, 0, 0, -1, -1) );
							if (partId == D) checked_ids.add(cand);
							if (partId == 0 || ((PassJoinValidator)checker).partialDistGivenThres.eval(x, y, partId, 0, 0, Lo, stPos) <= partId) {
								if (partId == 0) checked_ids.add(cand);
								if (partId == D || ((PassJoinValidator)checker).partialDistGivenThres.eval(x, y, D - partId, Lo + pLen, stPos + pLen, -1, -1) <= D - partId) {
//										if (debug) System.out.println( "d_edit: "+Util.edit(x, y, D, 0, 0, -1, -1) );
									if ( checker.isEqual( candRecord, exp ) >= 0 ) {
//									if (Util.edit(x, y, D, 0, 0, -1, -1) <= D) {
										checked_ids.add(cand);
										answer_ids.add( cand );
										++realNum;
									}
								}
							}
						}
					}
//					verifyTime += System.nanoTime() - ts_verify;
				} // end for lp
			} // end for partId
			verifyCost += 1.0 * clen * exp_checked;
			verifyCount += exp_checked;
		} // end for Record exp

		// output the results
		for ( int answer_id : answer_ids ) {
			Record recT = indexedList.get( answer_id );
			rslt.add(recS, recT);;
//				if (debug) System.out.println( rslt.size()+ " output: "+rec1.getID()+", "+rec2.getID() );
		}
		
		verifyTime += System.nanoTime() - t_afterExpand;
	}

	private long DJB_hash( int[] str, int start, int len) {
		long hash = 5381;

		for (int k = 0; k < len; k++)
		{
			hash += (hash << 5) + str[start+k];
		}

		return (hash & 0x7FFFFFFF);
	}

	private class PIndex {
		final int stPos;
		final int Lo;
		final int partLen;
		final int len;
		
		public PIndex( int _s, int _o, int _p, int _l ) {
			stPos = _s;
			Lo = _o;
			partLen = _p;
			len = _l;
		}
	}
}
