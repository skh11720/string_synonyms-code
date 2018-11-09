package snu.kdd.synonym.synonymRev.algorithm.delta;

import java.util.List;
import java.util.Set;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.index.AbstractIndex;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class DeltaHashIndex extends AbstractIndex {

	protected final ObjectArrayList<Object2ObjectOpenHashMap<int[], List<Record>>> idx; 
	/*
	 * idx is a list of hash tables. idx[d] is a hash table for d'-variants of strings ( 0 <= d' <= d)in the indexedSet.
	 * The range of d is 0~deltaMax.
	 */
	protected final int deltaMax;
	protected final boolean isSelfJoin;
	protected int n_verified = 0;
	
	public DeltaHashIndex( int deltaMax, Query query, StatContainer stat ) {
		this.deltaMax = deltaMax;
		this.isSelfJoin = query.selfJoin;
		idx = new ObjectArrayList<>();
		for ( int d=0; d<=deltaMax; ++d ) idx.add(new Object2ObjectOpenHashMap<>());
		int size = 0;
		
		for ( Record recT : query.indexedSet.recordList ) {
			int[] arrT = recT.getTokensArray();
			for ( int d=0; d<=deltaMax; ++d ) {
				List<IntArrayList> combList = Util.getCombinationsAll( recT.size(), d );
				for ( IntArrayList idxList : combList ) {
					int[] key = Util.getSubsequence( arrT, idxList );
					if ( !idx.get(d).containsKey(key) ) idx.get(d).put(key, new ObjectArrayList<Record>() );
					idx.get(d).get(key).add(recT);
					++size;
				}
			}
		}
		
		stat.add("Stat_Table_Size", size );
	}

	@Override
	protected void joinOneRecord( Record recS, Set<IntegerPair> rslt, Validator checker ) {
		Set<Record> matched = new ObjectOpenHashSet<>();
		for ( Record exp : recS.expandAll() ) {
			Set<Record> candidates = new ObjectOpenHashSet<>();
			int[] arrExp = exp.getTokensArray();
			for ( int d=0; d<=deltaMax; ++d ) {
				List<IntArrayList> combList = Util.getCombinations( exp.size(), d );
				for ( IntArrayList idxList : combList ) {
					int[] key = Util.getSubsequence( arrExp, idxList );
					if ( idx.get(deltaMax-d).containsKey(key) ) candidates.addAll( idx.get(deltaMax-d).get(key) );
				} // end for idxList
			} // end for d

			candidates.removeAll(matched);
			n_verified += candidates.size();
			for ( Record recT : candidates ) {
//				if ( Util.edit( exp.getTokensArray(), recT.getTokensArray() ) <= deltaMax ) {
				if ( Util.edit( exp.getTokensArray(), recT.getTokensArray(), deltaMax, 0, 0, exp.size(), recT.size() ) <= deltaMax ) {
					AlgorithmTemplate.addSeqResult(recS, recT, rslt, isSelfJoin);
					matched.add(recT);
				}
			}
		} // end for exp
	}

	@Override
	protected void postprocessAfterJoin(StatContainer stat) {
		stat.add("Val_Comparisons", n_verified );
	}
}
