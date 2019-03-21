package snu.kdd.synonym.synonymRev.algorithm.delta;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.index.AbstractIndex;
import snu.kdd.synonym.synonymRev.tools.ResultSet;
import snu.kdd.synonym.synonymRev.tools.Stat;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class JoinDeltaNaiveIndex extends AbstractIndex {

	private HashTable idx;
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
		
		idx = new HashTable(query.indexedSet.size());
		for ( Record recT : query.indexedSet.recordList ) {
			List<IntArrayList> combList = Util.getCombinationsAll( recT.size(), deltaMax ); // indexes whose elements will be deleted
			for ( IntArrayList idxListNotIn : combList ) {
				idx.put(recT.getID(), recT.getTokensArray(), idxListNotIn);
			}
		}
	}

	@Override
	protected void joinOneRecord( Record recS, ResultSet rslt, Validator checker ) {
		for ( Record exp : recS.expandAll() ) {
//			System.out.println("exp: "+exp);
			Set<Integer> candidates;
			if ( idxForDist == 0 ) candidates = getCandidateWithLCS(exp);
			else candidates = getCandidateWithEdit(exp);

			for ( int recTID : candidates ) {
				rslt.add(recS, recTID);
			}
		} // end for exp
	}
	
	protected Set<Integer> getCandidateWithLCS( Record exp ) {
		Set<Integer> candidates = new IntOpenHashSet();
		List<List<IntArrayList>> combListDelta = Util.getCombinationsAllByDelta( exp.size(), deltaMax );
		for ( int d_s=0; d_s<=deltaMax; ++d_s ) {
			for ( IntArrayList idxListNotIn : combListDelta.get(d_s) ) {
				for ( int rid : idx.get(0, deltaMax-d_s, exp.getTokensArray(), idxListNotIn) ) {
					candidates.add(rid);
				}
			}
		}
		return candidates;
	}
	
	protected Set<Integer> getCandidateWithEdit( Record exp ) {
		Set<Integer> candidates = new IntOpenHashSet();
		List<IntArrayList> combList = Util.getCombinationsAll( exp.size(), deltaMax );
		for ( IntArrayList idxListNotIn : combList ) {
			for ( int rid : idx.get(0, deltaMax, exp.getTokensArray(), idxListNotIn) ) {
				candidates.add(rid);
			}
		}
		return candidates;
	}

	@Override
	protected void postprocessAfterJoin(StatContainer stat) {
		int size = idx.size();
		int nList = idx.numEntries();
		stat.add(Stat.INDEX_SIZE, size );
		stat.add(Stat.AVG_LIST_LENGTH, size/nList);
	}
	
	protected static int getIntKey( int[] arr, IntArrayList idxListNotIn ) {
		int key = 0;
		for ( int i=0, j=0; i<arr.length; ++i ) {
			if ( j < idxListNotIn.size() && i == idxListNotIn.getInt(j) ) ++j;
			else key = ((key + arr[i]) << 32) % Util.bigprime;
		}
		return key;
	}
	
    protected static Record getRecordKey( int[] arr, IntArrayList idxListNotIn ) {
        int[] token = Util.getSubsequenceNotIn(arr, idxListNotIn);
        if ( token == null ) return Record.EMPTY_RECORD;
        else return new Record(token);
    }
    
    private final class HashTable {
    	private final Int2ObjectOpenHashMap<Int2ObjectOpenHashMap<HashTableEntry>> table;
    	
    	public HashTable( int initCapacity ) {
    		table = new Int2ObjectOpenHashMap<>();
    		for ( int d=0; d<=deltaMax; ++d ) table.put(d, new Int2ObjectOpenHashMap<>(initCapacity*(d+1)));
		}
    	
    	public void put( int rid, int[] arr, IntArrayList idxListNotIn ) {
    		int d = idxListNotIn.size();
    		int intKey = getIntKey(arr, idxListNotIn);
    		Int2ObjectOpenHashMap<HashTableEntry> table_d = table.get(d);
    		if (!table_d.containsKey(intKey)) table_d.put(intKey, new HashTableEntry());
    		table_d.get(intKey).put(rid, arr, idxListNotIn);
    	}
    	
    	public Iterable<Integer> get( int d_min, int d_max, int[] arr, IntArrayList idxListNotIn ) {
    		Set<Integer> outputSet = new IntOpenHashSet();
    		int intKey = getIntKey(arr, idxListNotIn);
    		for ( int d=d_min; d<=d_max; ++d ) {
    			if (table.get(d).containsKey(intKey)) {
    				HashTableEntry entry = table.get(d).get(intKey);
    				for ( int rid : entry.get(arr, idxListNotIn) ) outputSet.add(rid);
    			}
    		}
    		return outputSet;
    	}
    	
    	public int size() {
    		int n = 0;
    		for ( int d=0; d<=deltaMax; ++d ) {
    			for ( HashTableEntry entry : table.get(d).values()) n += entry.size();
    		}
    		return n;
    	}
    	
    	public int numEntries() {
    		int n = 0;
    		for ( int d=0; d<=deltaMax; ++d ) {
    			n += table.get(d).size();
    		}
    		return n;
    	}
    }

    private final class HashTableEntry {
    	private IntArrayList firstList;
    	private final Object2ObjectOpenHashMap<Record, IntArrayList> recordMap;
    	
    	public HashTableEntry() {
    		recordMap = new Object2ObjectOpenHashMap<>();
		}
    	
    	public void put( int rid, int[] arr, IntArrayList idxListNotIn ) {
    		Record recordKey = getRecordKey(arr, idxListNotIn);
    		if (!recordMap.containsKey(recordKey)) {
    			recordMap.put(recordKey, new IntArrayList());
    			if ( firstList == null ) firstList = recordMap.get(recordKey);
    		}
    		recordMap.get(recordKey).add(rid);
    	}
    	
    	public Iterable<Integer> get( int[] arr, IntArrayList idxListNotIn ) {
    		if (recordMap.size() == 0 ) return Collections.emptyList();
//    		if (recordMap.size() == 1) return firstList;
    		Record recordKey = getRecordKey(arr, idxListNotIn);
    		if (recordMap.containsKey(recordKey)) return recordMap.get(recordKey);
    		else return Collections.emptyList();
    	}
    	
    	public int size() {
    		int n = 0;
    		for ( IntArrayList list : recordMap.values() ) n += list.size();
    		return n;
    	}
    }
    
}
