package passjoin;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.tools.WYK_HashSet;

public class PassJoinIndex {
	
	private final int D, N, PN;
	private final List<Record> dict;
	private int MaxDictLen = 0;
	private int MinDictLen = Integer.MAX_VALUE;
	
	List<PIndex>[][] partIndex;
	Long2ObjectMap<IntArrayList>[][] invLists;
	private int[][] partPos;
	private int[][] partLen;
	private int[] dist;
	
	
	public PassJoinIndex( List<Record> indexedList, int deltaMax) {
		dict = indexedList;
		D = deltaMax;
		N = indexedList.size();
		PN = D+1;
	}
	
	public Set<IntegerPair> run() {
		sort();
		init();
		prepare();
		return join();
	}
	
	private void sort() {
		Comparator<Record> comp = new Comparator<Record>() {
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

		Collections.sort( dict, comp );
		MinDictLen = dict.get( 0 ).size();
		MaxDictLen = dict.get( N-1 ).size();
	}
	
	private void init() {
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
		
		Arrays.fill( dist, N );
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
		for (int id = 0; id < N; id++) {
			if ( clen == dict.get( id ).size() ) continue;
			for (int lp = clen + 1; lp <= dict.get( id ).size(); lp++) dist[lp] = id;
			clen = dict.get( id ).size();
		}

		clen = 0;
		for (int id = 0; id < N; id++) {
			if (clen == dict.get( id ).size()) continue;
			clen = dict.get( id ).size();

			for (int pid = 0; pid < PN; pid++)
			{
				for (int len = Math.max(clen - D, MinDictLen); len <= clen; len++)
				{
					if (dist[len] == dist[len + 1]) continue;
					
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
	
	private Set<IntegerPair> join() {
		boolean debug = false;
		Set<IntegerPair> rslt = new WYK_HashSet<IntegerPair>();
		long candNum = 0;
		long realNum = 0;
		for (int id = 0; id < N; id++) {
			IntOpenHashSet checked_ids = new IntOpenHashSet();
			IntOpenHashSet answer_ids = new IntOpenHashSet();
			int[] y = dict.get( id ).getTokensArray();
			int clen = dict.get( id ).size();
			if (debug) System.out.println( "y: "+dict.get( id ).getID()+", "+Arrays.toString( y ) );
			for (int partId = 0; partId < PN; partId++) {
				for (int lp = 0; lp < partIndex[partId][clen].size(); lp++) {
					int stPos = partIndex[partId][clen].get( lp ).stPos;
					int Lo = partIndex[partId][clen].get( lp ).Lo;
					int pLen = partIndex[partId][clen].get( lp ).partLen;
					int len = partIndex[partId][clen].get( lp ).len;

					long hash_value = DJB_hash(dict.get( id ).getTokensArray(), stPos, pLen);
					if ( !invLists[partId][len].containsKey( hash_value ) ) continue;
					for (int cand : invLists[partId][len].get( hash_value ) ) {
						if ( !checked_ids.contains( cand ) ) {
							++candNum;
							int[] x = dict.get( cand ).getTokensArray();
							if (debug) System.out.println( "x: "+dict.get( cand ).getID()+", "+Arrays.toString( x ) );
							if (partId == D) checked_ids.add(cand);
							if (partId == 0 || Util.lcs(x, y, partId, 0, 0, Lo, stPos) <= partId) {
								if (partId == 0) checked_ids.add(cand);
								if (partId == D || Util.lcs(x, y, D - partId, Lo + pLen, stPos + pLen, -1, -1) <= D - partId) {
									if (Util.lcs(x, y, D, 0, 0, -1, -1) <= D) {
										checked_ids.add(cand);
										answer_ids.add( cand );
										++realNum;
									}
								}
							}
						}
					}
				}
			} // end for partId

			// incrementally build the index
			for (int partId = 0; partId < PN; partId++) {
				int pLen = partLen[partId][clen];
				int stPos = partPos[partId][clen];
				long hash = DJB_hash(dict.get( id ).getTokensArray(), stPos, pLen);
				if ( !invLists[partId][clen].containsKey( hash ) ) invLists[partId][clen].put( hash, new IntArrayList() );
				invLists[partId][clen].get( hash ).add( id );
				if (debug) System.out.println( "insert "+ id +" into invLists with"
						+ "\tpartId: "+partId
						+ "\tclen: "+clen
						+ "\tstPos: "+stPos
						+ "\tpLen: "+pLen
						+ "\tstr: "+Arrays.toString( Arrays.copyOfRange( dict.get( id ).getTokensArray(), stPos, stPos+pLen ) ) );
			}
			
			// output the results
			for ( int answer_id : answer_ids ) {
				int id1 = dict.get( id ).getID();
				int id2 = dict.get( answer_id ).getID();
				rslt.add( new IntegerPair(id1, id2) );
				if (debug) System.out.println( "output: "+id1+", "+id2 );
			}
		} // end for id

		if (debug) System.out.println( "candNum: "+candNum );
		if (debug) System.out.println( "realNum: "+realNum );
		return rslt;
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
