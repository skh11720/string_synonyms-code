package vldb17.set;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.order.AbstractGlobalOrder.Ordering;
import snu.kdd.synonym.synonymRev.order.QGramGlobalOrder;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.tools.WYK_HashMap;

public class PkduckSetIndex {
	private WYK_HashMap< QGram, List<Record>> idx;
	
	/*
	 * Currently, qgramSize and prefixSize are fixed to 1,
	 * since we are interested in the uni-directional equivalence only.
	 */
	private final int qgramSize = 1;
	private final QGramGlobalOrder globalOrder;
	private final int initCapacity;
	
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
	
	public PkduckSetIndex(List<Record> recordList, Query query, StatContainer stat, QGramGlobalOrder globalOrder, boolean addStat) {
		
		long startTime = System.nanoTime();
		this.globalOrder = globalOrder;
		this.initCapacity = recordList.size() / 100;
		
		idx = new WYK_HashMap<QGram, List<Record>>();
		
		long elements = 0;
//		long qGramTime = 0;
//		long indexingTime = 0;
//		long maxlenTime = 0;
//		long recordStartTime, afterQGram, afterIndexing;
		
		// Index records in recordList in the inverted lists.
		for (Record rec : recordList) {
//			recordStartTime = System.currentTimeMillis();
			List<List<QGram>> availableQGrams = null;
			if (!query.oneSideJoin) {
				throw new RuntimeException("UNIMPLEMENTED CASE");
			}
			else {
				availableQGrams = rec.getSelfQGrams( qgramSize, rec.size() );
			}
//			afterQGram = System.currentTimeMillis();
			
			indexRecord( rec, availableQGrams );
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
			for ( QGram qgram : idx.keySet() ) {
				bw.write( "qgram: "+qgram.toString()+"\n" );
				for ( Record rec : idx.get( qgram )) {
					bw.write( rec.getID()+", " );
				}
				bw.write( "\n" );
			}
			bw.close();
		}
		catch (IOException e) { e.printStackTrace(); }
	}
	
	public List<Record> get(QGram qgram) {
		try {
			return idx.get( qgram );
		}
		catch (NullPointerException e) {
			return null;
		}
	}
	
	public Set<QGram> keySet() {
		return idx.keySet();
	}
	
	public int nInvList() {
		return idx.keySet().size();
	}
	
	private void indexRecord(final Record record, final List<List<QGram>> availableQGrams ) {
		
		Ordering order = globalOrder.getMode();

		if ( order != Ordering.TF && order != Ordering.FF ) 
			throw new RuntimeException("PositionFirst is disabled in JoinPkduckSet.");
			
		QGram key = availableQGrams.get( 0 ).get( 0 );
		for (int i=0; i<record.size(); i++) {
			QGram qgram = availableQGrams.get( i ).get( 0 );
			if ( globalOrder.compare( key, qgram) == 1 ) key = qgram;
		}
		if ( idx.get( key ) == null ) idx.put( key, new ObjectArrayList<Record>(this.initCapacity) );
		idx.get( key ).add( record );
	}
}
