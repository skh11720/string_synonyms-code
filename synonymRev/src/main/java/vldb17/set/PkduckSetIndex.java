package vldb17.set;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.order.AbstractGlobalOrder;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.Util;

public class PkduckSetIndex {
	private Int2ObjectOpenHashMap<List<Record>> idx;
	
	/*
	 * Currently, qSize and prefixSize are fixed to 1,
	 * since we are interested in the uni-directional equivalence only.
	 */
	private final AbstractGlobalOrder globalOrder;
	private final double theta;
	
	long indexTime = 0;
	long joinTime = 0;
	
	/**
	 * PkduckSetIndex: build a Pkduck Index
	 * 
	 * @param query
	 * @param stat
	 * @param globalOrder
	 * @param addStat
	 */
	
	public PkduckSetIndex(List<Record> recordList, Query query, double theta, StatContainer stat, AbstractGlobalOrder globalOrder, boolean addStat) {
		
		long startTime = System.nanoTime();
		this.globalOrder = globalOrder;
		this.theta = theta;
		
		idx = new Int2ObjectOpenHashMap<List<Record>>();
		
		long elements = 0;
//		long qGramTime = 0;
//		long indexingTime = 0;
//		long maxlenTime = 0;
//		long recordStartTime, afterQGram, afterIndexing;
		
		// Index records in recordList in the inverted lists.
		for (Record rec : recordList) {
//			recordStartTime = System.currentTimeMillis();
			if (!query.oneSideJoin) {
				throw new RuntimeException("UNIMPLEMENTED CASE");
			}
//			afterQGram = System.currentTimeMillis();
			
			indexRecord( rec );
			elements++;
//			afterIndexing = System.currentTimeMillis();
			
//			qGramTime += afterQGram - recordStartTime;
//			indexingTime += afterIndexing - afterQGram;
		} // end for record in T
		
//		stat.add( "PkduckSetIndex.maxlenTime", maxlenTime/1e6 );
//		stat.add( "PkduckSetIndex.qGramTime", qGramTime );
//		stat.add( "PuduckIndex.indexingTime", indexingTime );
		stat.add( "PkduckSetIndex.size", elements );
		stat.add( "PkduckSetIndex.nList", nInvList());
		
		this.indexTime = System.nanoTime() - startTime;
		Util.printGCStats( stat, "PkduckSetIndex" );
	}
	
	public void writeToFile( String filename ) {
		try {
			BufferedWriter bw = new BufferedWriter( new FileWriter( filename) );
			for ( int token : idx.keySet() ) {
				bw.write( "token: "+token+"\n" );
				for ( Record rec : idx.get( token )) {
					bw.write( rec.getID()+", " );
				}
				bw.write( "\n" );
			}
			bw.close();
		}
		catch (IOException e) { e.printStackTrace(); }
	}
	
	public List<Record> get(int key) {
		try {
			return idx.get( key );
		}
		catch (NullPointerException e) {
			return null;
		}
	}
	
	public Set<Integer> keySet() {
		return idx.keySet();
	}
	
	public int nInvList() {
		return idx.keySet().size();
	}
	
	private void indexRecord(final Record record ) {
		int[] tokens = record.getTokensArray();
		int lenPrefix = (int)Math.floor((1-theta)*record.size())+1;
		int[] keys = new int[lenPrefix];
		
		int keyMax = tokens[0];
		keys[0] = tokens[0];
		for (int i=1; i<tokens.length; i++) {
			if ( globalOrder.compare( keys[0], tokens[i]) > 0 ) keys[0] = tokens[i];
			if ( globalOrder.compare( tokens[i], keyMax ) > 0 ) keyMax = tokens[i];
		}
		
		for ( int j=1; j<lenPrefix; ++j ) {
			keys[j] = keyMax;
			for ( int i=0; i<tokens.length; ++i ) {
				if ( tokens[i] <= keys[j-1] ) continue;
				if ( globalOrder.compare( keys[j], tokens[i] ) > 0 ) keys[j] = tokens[i];
			}
		}

		for ( int key : new IntOpenHashSet(keys) ) {
			if ( idx.get( key ) == null ) idx.put( key, new ObjectArrayList<Record>() );
			idx.get( key ).add( record );
		}
	}
}
