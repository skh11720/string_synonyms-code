package snu.kdd.synonym.synonymRev.algorithm.delta;

import java.util.List;
import java.util.Set;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.algorithm.AbstractAlgorithm;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.index.AbstractIndex;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class DeltaHashIndex extends AbstractIndex {

	protected final ObjectArrayList<Int2ObjectOpenHashMap<List<Record>>> idx; 
	/*
	 * idx is a list of hash tables. idx[d] is a hash table for d-variants of strings in the indexedSet.
	 * The range of d is 0~deltaMax.
	 */
	protected final int deltaMax;
	protected final int idxForDist; // 0: lcs, 1: edit
	protected final boolean isSelfJoin;
	
	public DeltaHashIndex( int deltaMax, String dist, Query query, StatContainer stat ) {
		this.deltaMax = deltaMax;
		this.isSelfJoin = query.selfJoin;
		if ( dist.equals("lcs") ) idxForDist = 0;
		else idxForDist = 1;
		idx = new ObjectArrayList<>();
		for ( int d=0; d<=deltaMax; ++d ) idx.add(new Int2ObjectOpenHashMap<>());
		int size = 0;
		
		for ( Record recT : query.indexedSet.recordList ) {
			List<IntArrayList> combList = Util.getCombinationsAll( recT.size(), deltaMax ); // indexes whose elements will be deleted
			for ( IntArrayList idxList : combList ) {
				int key = getKey(recT.getTokensArray(), idxList);
				int d = idxList.size();
				if ( !idx.get(d).containsKey(key) ) idx.get(d).put(key, new ObjectArrayList<Record>() );
				idx.get(d).get(key).add(recT);
//				System.out.println(d+", "+key+", "+recT);
				++size;
			}
		}
		
		// check hash collision
		int n_list = 0;
		for ( Int2ObjectOpenHashMap<List<Record>> map : idx ) n_list += map.size();
		stat.add("Stat_Index_Size", size );
		stat.add("Stat_Index_nList", n_list );
	}

	@Override
	protected void joinOneRecord( Record recS, Set<IntegerPair> rslt, Validator checker ) {
		Set<Record> matched = new ObjectOpenHashSet<>();
		for ( Record exp : recS.expandAll() ) {
//			System.out.println("exp: "+exp);
			Set<Record> candidates;
			if ( idxForDist == 0 ) candidates = getCandidateWithLCS(exp);
			else candidates = getCandidateWithEdit(exp);

			for ( Record recT : candidates ) {
				if ( matched.contains(recT) ) continue;
				++checker.checked;
				if ( exp.equals(recT) ) matched.add(recT);
				else if ( ((AbstractDeltaValidator)checker).distGivenThres.eval(exp.getTokensArray(), recT.getTokensArray(), deltaMax) <= deltaMax ) {
					matched.add(recT);
				}
			}
		} // end for exp
		
		for ( Record recT : matched ) AbstractAlgorithm.addSeqResult(recS, recT, rslt, isSelfJoin);
	}
	
	protected Set<Record> getCandidateWithLCS( Record exp ) {
		Set<Record> candidates = new ObjectOpenHashSet<>();
		List<List<IntArrayList>> combListDelta = Util.getCombinationsAllByDelta( exp.size(), deltaMax );
		for ( int d_s=0; d_s<=deltaMax; ++d_s ) {
			for ( IntArrayList idxList : combListDelta.get(d_s) ) {
				int key = getKey(exp.getTokensArray(), idxList);
				for ( int d_t=0; d_t<=deltaMax-d_s; ++d_t ) {
					if ( idx.get(d_t).containsKey(key) ) {
						for ( Record recT : idx.get(d_t).get(key) ) {
							candidates.add(recT);
						}
					}
				}
			}
		}
		return candidates;
	}
	
	protected Set<Record> getCandidateWithEdit( Record exp ) {
		Set<Record> candidates = new ObjectOpenHashSet<>();
		List<IntArrayList> combList = Util.getCombinationsAll( exp.size(), deltaMax );
		for ( IntArrayList idxList : combList ) {
			int key = getKey(exp.getTokensArray(), idxList);
			for ( int d_t=0; d_t<=deltaMax; ++d_t ) {
				if ( idx.get(d_t).containsKey(key) ) {
					for ( Record recT : idx.get(d_t).get(key) ) {
						candidates.add(recT);
					}
				}
			}
		}
		return candidates;
	}

	@Override
	protected void postprocessAfterJoin(StatContainer stat) {
	}
	
	protected static int getKey( int[] arr, IntArrayList idxList ) {
		/*
		 * Return the key value of the subsequence of arr WITHOUT indexes in idxList.
		 * (i.e., idxList represents the positions of elements in arr to be deleted.)
		 * This function is similar to the hash function of Record class.
		 */
		int key = 0;
		int j = 0;
		int idx = idxList.size() > 0 ? idxList.getInt(j) : -1;
		for ( int i=0; i<arr.length; ++i ) {
			if ( idx == i ) {
				if ( j < idxList.size()-1 ) idx = idxList.getInt(++j);
				else idx = -1;
			}
			else {
				key = ( key << 3 ) + arr[i];
				key %= Util.bigprime;
			}
		}
//		key %= Integer.MAX_VALUE; // is this line necessary?
		return key;
	}
}
