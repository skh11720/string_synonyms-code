package vldb17.seq;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.tools.WYK_HashMap;
import vldb17.ParamPkduck.GlobalOrder;

public class PkduckIndex {
	private WYK_HashMap<Integer, WYK_HashMap< QGram, List<Record>>> idx;
	
	/*
	 * Currently, qgramSize and prefixSize are fixed to 1,
	 * since we are interested in the uni-directional equivalence only.
	 */
	private final int qgramSize = 1;
	private final GlobalOrder globalOrder;
	private final int prefixSize = 1;
	private final int initCapacity;
	
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
	
	public PkduckIndex(Query query, StatContainer stat, GlobalOrder globalOrder, boolean addStat) {
		
		long startTime = System.nanoTime();
//		this.prefixSize = prefixSize;
		this.globalOrder = globalOrder;
		this.initCapacity = query.indexedSet.size() / 100;
		
		idx = new WYK_HashMap<Integer, WYK_HashMap<QGram, List<Record>>>();
		
		long elements = 0;
		long qGramTime = 0;
		long indexingTime = 0;
		long maxlenTime = 0;
		long recordStartTime, afterQGram, afterIndexing;
		
		// Index records in T in the inverted lists.
		for (Record rec : query.indexedSet.recordList) {
			recordStartTime = System.currentTimeMillis();
			List<List<QGram>> availableQGrams = null;
			if (!query.oneSideJoin) {
				throw new RuntimeException("UNIMPLEMENTED CASE");
			}
			else {
				availableQGrams = rec.getSelfQGrams( qgramSize, rec.size() );
			}
			afterQGram = System.currentTimeMillis();
			
			indexRecord( rec, availableQGrams );
			elements++;
			afterIndexing = System.currentTimeMillis();
			
			qGramTime += afterQGram - recordStartTime;
			indexingTime += afterIndexing - afterQGram;
		} // end for record in T
		
		stat.add(  "PkduckIndex.maxlenTime", maxlenTime/1e6 );
		stat.add( "PkduckIndex.qGramTime", qGramTime );
		stat.add(  "PuduckIndex.indexingTime", indexingTime );
		stat.add( "PkduckIndex.size", elements );
		stat.add( "PkduckIndex.nList", nInvList());
		
		this.indexTime = System.nanoTime() - startTime;
		Util.printGCStats( stat, "PkduckIndex" );
	}
	
	public void writeToFile( String filename ) {
		try {
			BufferedWriter bw = new BufferedWriter( new FileWriter( filename) );
			for (Integer i : idx.keySet()) {
				bw.write(  i + "-th index\n" );
				WYK_HashMap<QGram, List<Record>> invList = idx.get( i );
				if (invList == null) continue;
				for ( QGram qgram : invList.keySet() ) {
					bw.write( "qgram: "+qgram.toString()+"\n" );
					for ( Record rec : invList.get( qgram )) {
						bw.write( rec.getID()+", " );
					}
					bw.write( "\n" );
				}
			}
			bw.close();
		}
		catch (IOException e) { e.printStackTrace(); }
	}
	
	public List<Record> get(int pos, QGram qgram) {
		try {
			return idx.get( pos ).get( qgram );
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
	
	private void indexRecord(final Record record, final List<List<QGram>> availableQGrams ) {
		switch (globalOrder) {
		case PF: {
			if ( idx.get( 0 ) == null ) idx.put( 0, new WYK_HashMap<QGram, List<Record>>() );
//			WYK_HashMap<QGram, List<Record>> invList = idx.get( 0 );
			QGram key = availableQGrams.get( 0 ).get( 0 ); // there is a single qgram at position 0.
			if (idx.get(0).get( key ) == null ) idx.get(0).put(key, new ObjectArrayList<Record>(this.initCapacity));
			idx.get( 0 ).get( key ).add( record );
			break;
		}
			
		case TF: {
			int pos = 0;
			QGram key = availableQGrams.get( 0 ).get( 0 );
			for (int i=1; i<record.size(); i++) {
				QGram qgram = availableQGrams.get( i ).get( 0 );
				if ( compareQGrams( key.qgram, qgram.qgram ) == 1 ) {
					pos = i;
					key = qgram;
				}
			}
			if ( idx.get( pos ) == null ) idx.put( pos, new WYK_HashMap<QGram, List<Record>>() );
			if ( idx.get( pos ).get( key ) == null ) idx.get( pos ).put( key,  new ObjectArrayList<Record>(this.initCapacity) );
			idx.get( pos ).get( key ).add( record );
			break;
		}
			
		default:
			throw new RuntimeException("UNIMPLEMENTED CASE");
		}
	}
	
	public static int compareQGrams(int[] qgram0, int[] qgram1) {
		int len = Math.min( qgram0.length, qgram1.length );
		int res = Integer.MAX_VALUE;
		for (int i=0; i<len; i++) {
			res = Integer.compare( qgram0[i], qgram1[i] );
			if (res != 0) return res;
		}
		if (qgram0.length > len) return 1;
		else if (qgram1.length > len) return -1;
		else return 0;
	}
}