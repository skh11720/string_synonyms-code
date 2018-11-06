package vldb17.seq;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.order.AbstractGlobalOrder;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.tools.WYK_HashMap;

public class PkduckIndex {
	private WYK_HashMap<Integer, WYK_HashMap<Integer, List<Record>>> idx;
	
	/*
	 * Currently, qgramSize and prefixSize are fixed to 1,
	 * since we are interested in the uni-directional equivalence only.
	 */
	private final AbstractGlobalOrder globalOrder;
	
	long indexTime = 0;
	long joinTime = 0;
	
	/**
	 * PkduckIndex: build a Pkduck Index
	 * 
	 * @param query
	 * @param stat
	 * @param globalOrder
	 * @param addStat
	 */
	
	public PkduckIndex(Query query, StatContainer stat, AbstractGlobalOrder globalOrder, boolean addStat) {
		
		long startTime = System.nanoTime();
//		this.prefixSize = prefixSize;
		this.globalOrder = globalOrder;
		
		idx = new WYK_HashMap<Integer, WYK_HashMap<Integer, List<Record>>>();
		
		long elements = 0;
		long indexingTime = 0;
		long maxlenTime = 0;
		long recordStartTime, afterIndexing;
		
		// Index records in T in the inverted lists.
		for (Record rec : query.indexedSet.recordList) {
			recordStartTime = System.currentTimeMillis();
			if (!query.oneSideJoin) {
				throw new RuntimeException("UNIMPLEMENTED CASE");
			}
			indexRecord( rec );
			elements++;
			afterIndexing = System.currentTimeMillis();
			
			indexingTime += afterIndexing - recordStartTime;
		} // end for record in T
		
		stat.add(  "PkduckIndex.maxlenTime", maxlenTime/1e6 );
		stat.add(  "PuduckIndex.indexingTime", indexingTime );
		stat.add( "PkduckIndex.size", elements );
		stat.add( "PkduckIndex.nList", nInvList());
		
		this.indexTime = System.nanoTime() - startTime;
		Util.printGCStats( stat, "PkduckIndex" );
	}
	
	public void writeToFile() {
		try {
			String filename = "tmp/PkduckIndex.txt";
			BufferedWriter bw = new BufferedWriter( new FileWriter( filename) );
			for (Integer i : idx.keySet()) {
				bw.write(  i + "-th index\n" );
				WYK_HashMap<Integer, List<Record>> invList = idx.get( i );
				if (invList == null) continue;
				for ( int token : invList.keySet() ) {
					bw.write( "token: "+token+"\n" );
					for ( Record rec : invList.get( token )) {
						bw.write( rec.getID()+", " );
					}
					bw.write( "\n" );
				}
			}
			bw.close();
		}
		catch (IOException e) { e.printStackTrace(); }
	}
	
	public List<Record> get(int pos, int token ) {
		try {
			return idx.get( pos ).get( token );
		}
		catch (NullPointerException e) {
			return null;
		}
	}
	
	public Set<Integer> keySet() {
		return idx.keySet();
	}
	
	public int nInvList() {
		int nList = 0;
		for (int pos : idx.keySet()) {
			nList += idx.get( pos ).size();
		}
		return nList;
	}
	
	private void indexRecord(final Record record ) {
		int[] tokens = record.getTokensArray();
		int pos=0;
		int key = tokens[pos];
		for ( int i=1; i<record.size(); i++ ) {
			if ( globalOrder.compare( key,  pos, tokens[i], i ) > 0 ) {
				pos = i;
				key = tokens[i];
			}
		}
		if ( idx.get( pos ) == null ) idx.put( pos, new WYK_HashMap<Integer, List<Record>>() );
		if ( idx.get( pos ).get( key ) == null ) idx.get( pos ).put( key,  new ObjectArrayList<Record>() );
		idx.get( pos ).get( key ).add( record );
	}
}
