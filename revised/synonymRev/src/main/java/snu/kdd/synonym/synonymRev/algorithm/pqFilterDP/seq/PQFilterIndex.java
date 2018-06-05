package snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.seq;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.order.AbstractGlobalOrder;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.PosQGram;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.Util;

public class PQFilterIndex extends AbstractPQFilterIndex {

	protected Int2ObjectOpenHashMap<Map<QGram, List<Record>>> idx;
	protected Object2IntOpenHashMap<Record> indexedCountList;

	protected final PosQGramComparator comparator = new PosQGramComparator();
	protected final AbstractGlobalOrder globalOrder;
	protected final int indexK;
	protected final int qgramSize;

//	protected int qgramCount = 0;
	protected long indexTime = 0;
	
	public PQFilterIndex( int indexK, int qgramSize, Iterable<Record> indexedSet, Query query, AbstractGlobalOrder globalOrder, StatContainer stat ) {
		this.indexK = indexK;
		this.qgramSize = qgramSize;
		this.globalOrder = globalOrder;

		idx = new Int2ObjectOpenHashMap<Map<QGram, List<Record>>>();
		indexedCountList = new Object2IntOpenHashMap<Record>();
		
		long qGramTime = 0;
		long indexingTime = 0;
		long elements = 0;
		for ( Record rec : indexedSet ) {
//			boolean debug = false;
//			if( rec.getID()==20060) debug = true;
			long ts = System.currentTimeMillis();
			List<List<QGram>> availableQGrams = rec.getSelfQGrams( qgramSize, rec.size() );
			ObjectArrayList<PosQGram> pqgrams = new ObjectArrayList<PosQGram>();
			for ( int i=0; i<availableQGrams.size(); ++i ) {
				for ( QGram qgram : availableQGrams.get( i ) ) pqgrams.add( new PosQGram( qgram, i ) );
			}
			pqgrams.sort(comparator);
			long afterQGramTime = System.currentTimeMillis();
//			if (debug) System.out.println( "pqgrams: "+pqgrams.toString() );

			int indexedCount = Math.min( indexK, pqgrams.size() );
			indexedCountList.put( rec, indexedCount );
//			if (debug) System.out.println( "indexedCount: "+indexedCount );

			for ( int k=0; k<indexedCount; ++k ) {
				PosQGram pqgram = pqgrams.get( k );
				QGram qgram = pqgram.qgram;
				int pos = pqgram.pos;
				if ( !idx.containsKey( pos ) ) idx.put( pos, new Object2ObjectOpenHashMap<QGram, List<Record>>() );
				Map<QGram, List<Record>> map = idx.get( pos );
				if ( !map.containsKey( qgram ) ) map.put( qgram, new ObjectArrayList<Record>() );
				map.get( qgram ).add( rec );
//				if (debug) System.out.println( "index record "+rec.getID()+": "+Arrays.toString( rec.getTokensArray() )+" with key "+qgram.toString()+", "+pos );
				++elements;
			}
			long afterIndexingTime = System.currentTimeMillis();
			qGramTime += afterQGramTime - ts;
			indexingTime += afterIndexingTime - afterQGramTime;
		}
		buildInvertedIndex();

		stat.add("Result_3_1_1_qGramTime", qGramTime);
		stat.add("Result_3_1_2_indexingTime", indexingTime);
		stat.add("Stat_Index_Size", elements);
		stat.add( "nList", nInvList());

		Util.printGCStats(stat, "Stat_Index");
	}
	
	public Map<QGram, List<Record>> get( int pos ) {
		return idx.get( pos );
	}
	
	public int getIndexedCount( Record rec ) {
		return indexedCountList.getInt( rec );
	}
	
	public Iterable<Integer> getPosSet() {
		return idx.keySet();
	}

	public int nInvList() {
		int n = 0;
		for (int i : getPosSet() ) {
			n += idx.get( i ).size();
		}
		return n;
	}

	public void writeToFile() {
		try { 
			BufferedWriter bw = new BufferedWriter( new FileWriter( "tmp/PQFilterIndex.txt" ) ); 
			for ( int pos : idx.keySet() ) {
				bw.write( "pos: "+pos+"\n" );
				for ( Entry<QGram, List<Record>> entry : idx.get( pos ).entrySet() ) {
					bw.write( "qgram: "+entry.getKey()+"\n" );
					for ( Record rec : entry.getValue() ) {
						bw.write( rec.getID()+": "+Arrays.toString( rec.getTokensArray() ) + "\n");
					}
				}
			}
			bw.flush();
			bw.close();
			
			bw = new BufferedWriter( new FileWriter( "tmp/PQFilterInvIndex.txt") );
			for ( Entry<IntegerPair, Set<QGram>> entry : invIndex.entrySet() ) {
				bw.write( entry.getKey().toString()+" : "+entry.getValue().toString()+"\n" );
			}
			bw.flush();
			bw.close();
		}
		catch (IOException e ) { e.printStackTrace(); }
	}
	
	private class PosQGramComparator implements Comparator<PosQGram> {

		@Override
		public int compare( PosQGram o1, PosQGram o2 ) {
			int n1 = globalOrder.getOrder( o1 );
			int n2 = globalOrder.getOrder( o2 );
			return Integer.compare( n1, n2 );
		}
	}
}
