package snu.kdd.synonym.synonymRev.algorithm.delta;

import java.util.List;
import java.util.Set;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.index.AbstractIndex;
import snu.kdd.synonym.synonymRev.tools.ResultSet;
import snu.kdd.synonym.synonymRev.tools.Stat;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class JoinDeltaNaiveIndex extends AbstractIndex {

	protected final ObjectArrayList<Object2ObjectOpenHashMap<Record, List<Integer>>> idx; 
	/*
	 * idx is a list of hash tables. idx[d] is a hash table for d-variants of strings in the indexedSet.
	 * The range of d is 0~deltaMax.
	 */
	protected final int deltaMax;
	protected final int idxForDist; // 0: lcs, 1: edit
	protected final boolean isSelfJoin;
	
	public JoinDeltaNaiveIndex( int deltaMax, String dist, Query query ) {
		this.deltaMax = deltaMax;
		this.isSelfJoin = query.selfJoin;
		if ( dist.equals("lcs") ) idxForDist = 0;
		else idxForDist = 1;
		idx = new ObjectArrayList<>();
		for ( int d=0; d<=deltaMax; ++d ) idx.add(new Object2ObjectOpenHashMap<>());
		
		for ( Record recT : query.indexedSet.recordList ) {
			List<IntArrayList> combList = Util.getCombinationsAll( recT.size(), deltaMax ); // indexes whose elements will be deleted
			for ( IntArrayList idxList : combList ) {
				Record strAfterDeleted = getKey(recT.getTokensArray(), idxList);
				int d = idxList.size();
				if ( !idx.get(d).containsKey(strAfterDeleted) ) idx.get(d).put(strAfterDeleted, new IntArrayList() );
				idx.get(d).get(strAfterDeleted).add(recT.getID());
//				System.out.println(d+", "+key+", "+recT);
			}
		}
	}

	@Override
	protected void joinOneRecord( Record recS, ResultSet rslt, Validator checker ) {
		Set<Integer> matched = new IntOpenHashSet();
		for ( Record exp : recS.expandAll() ) {
//			System.out.println("exp: "+exp);
			Set<Integer> candidates;
			if ( idxForDist == 0 ) candidates = getCandidateWithLCS(exp);
			else candidates = getCandidateWithEdit(exp);

			for ( int recTID : candidates ) {
				if ( matched.contains(recTID) ) continue;
				rslt.add(recS, recTID);
				matched.add(recTID);
			}
		} // end for exp
	}
	
	protected Set<Integer> getCandidateWithLCS( Record exp ) {
		Set<Integer> candidates = new IntOpenHashSet();
		List<List<IntArrayList>> combListDelta = Util.getCombinationsAllByDelta( exp.size(), deltaMax );
		for ( int d_s=0; d_s<=deltaMax; ++d_s ) {
			for ( IntArrayList idxList : combListDelta.get(d_s) ) {
				Record key = getKey(exp.getTokensArray(), idxList);
				for ( int d_t=0; d_t<=deltaMax-d_s; ++d_t ) {
					if ( idx.get(d_t).containsKey(key) ) {
						for ( int recId : idx.get(d_t).get(key) ) {
							candidates.add(recId);
						}
					}
				}
			}
		}
		return candidates;
	}
	
	protected Set<Integer> getCandidateWithEdit( Record exp ) {
		Set<Integer> candidates = new IntOpenHashSet();
		List<IntArrayList> combList = Util.getCombinationsAll( exp.size(), deltaMax );
		for ( IntArrayList idxList : combList ) {
			Record key = getKey(exp.getTokensArray(), idxList);
			for ( int d_t=0; d_t<=deltaMax; ++d_t ) {
				if ( idx.get(d_t).containsKey(key) ) {
					for ( int recId : idx.get(d_t).get(key) ) {
						candidates.add(recId);
					}
				}
			}
		}
		return candidates;
	}

	@Override
	protected void postprocessAfterJoin(StatContainer stat) {
		int size = 0;
		int nList = 0;
		for ( int d=0; d<=deltaMax; ++d ) {
			Object2ObjectOpenHashMap<Record, List<Integer>> map = idx.get(d);
			for ( List<Integer> list : map.values() ) size += list.size();
			nList += map.size();
		}
		stat.add(Stat.INDEX_SIZE, size );
		stat.add(Stat.AVG_LIST_LENGTH, size/nList);
	}
	
	protected static Record getKey( int[] arr, IntArrayList idxList ) {
		/*
		 * Return the key value of the subsequence of arr WITHOUT indexes in idxList.
		 * (i.e., idxList represents the positions of elements in arr to be deleted.)
		 * This function is similar to the hash function of Record class.
		 */
		int[] token = Util.getSubsequenceNotIn(arr, idxList);
		if ( token == null ) return Record.EMPTY_RECORD;
		else return new Record(token);
	}
}
