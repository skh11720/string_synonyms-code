package vldb17;

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
	private int len_max_S = 0;
	
	long indexTime = 0;
	long joinTime = 0;
	
	public enum GlobalOrder {
		PositionFirst,
		TokenIndexFirst,
	}
	
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
		
		// find the maximum length of records in S.
		for (Record rec : query.searchedSet.recordList) len_max_S = Math.max( len_max_S, rec.size() );
		maxlenTime = System.nanoTime() - startTime;
		
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
	
	public Boolean isInSigU( Record rec, QGram target_qgram, int k ) {
		/*
		 * Compute g[o][i][l] for o=0,1, i=0~|rec|, l=0~max(|recS|).
		 * g[1][i][l] is X_l in the MIT paper.
		 */
//		System.out.println( "PkduckIndex.isInSigU, "+target_qgram+", "+k );
		
		// initialize g.
		int[][][] g = new int[2][rec.size()+1][len_max_S+1];
		for (int o=0; o<2; o++) {
			for (int i=0; i<=rec.size(); i++ ) {
				Arrays.fill( g[o][i], Integer.MAX_VALUE/2 ); // divide by 2 to prevent overflow
			}
		}
		g[0][0][0] = 0;
		List<List<QGram>> availableQgrams = rec.getSelfQGrams( 1, rec.size() );

		// compute g[0][i][l].
		for (int i=1; i<=rec.size(); i++) {
			QGram current_qgram = availableQgrams.get( i-1 ).get( 0 );
			for (int l=1; l<=len_max_S; l++) {
				int comp = comparePosQGrams( current_qgram.qgram, i-1, target_qgram.qgram, k );
//				System.out.println( "comp: "+comp );
//				System.out.println( "g[0]["+i+"]["+l+"]: "+g[0][i][l] );
				if ( comp != 0 ) g[0][i][l] = Math.min( g[0][i][l], g[0][i-1][l-1] + (comp==-1?1:0) );
//				System.out.println( "g[0]["+(i-1)+"]["+(l-1)+"]: "+g[0][i-1][l-1] );
//				System.out.println( "g[0]["+i+"]["+l+"]: "+g[0][i][l] );
				for (Rule rule : rec.getSuffixApplicableRules( i-1 )) {
//					System.out.println( rule );
					int[] rhs = rule.getRight();
					int num_smaller = 0;
					Boolean isValid = true;
					for (int j=0; j<rhs.length; j++) {
						// check whether the rule does not generate [target_token, k].
						isValid &= !(target_qgram.equals( Arrays.copyOfRange( rhs, j, j+1 ) ) && l-rhs.length+j == k); 
						num_smaller += comparePosQGrams( Arrays.copyOfRange( rhs, j, j+1 ), l-rhs.length+j, target_qgram.qgram, k )==-1?1:0;
					}
//					System.out.println( "isValid: "+isValid );
//					System.out.println( "num_smaller: "+num_smaller );
					if (isValid && i-rule.leftSize() >= 0 && l-rule.rightSize() >= 0) 
						g[0][i][l] = Math.min( g[0][i][l], g[0][i-rule.leftSize()][l-rule.rightSize()] + num_smaller );
				}
//				System.out.println( "g[0]["+i+"]["+l+"]: "+g[0][i][l] );
			}
		}
//		System.out.println(Arrays.deepToString(g[0]).replaceAll( "],", "]\n" ));
		
		// compute g[1][i][l].
		for (int i=1; i<=rec.size(); i++ ) {
			QGram current_qgram = availableQgrams.get( i-1 ).get( 0 );
			for (int l=1; l<=len_max_S; l++) {
				int comp = comparePosQGrams( current_qgram.qgram, i-1, target_qgram.qgram, k );
//				System.out.println( "comp: "+comp );
				if ( comp != 0 ) g[1][i][l] = Math.min( g[1][i][l], g[1][i-1][l-1] + (comp<0?1:0) );
				else g[1][i][l] = Math.min( g[1][i][l], g[0][i-1][l-1] );
//				System.out.println( "g[1]["+i+"]["+l+"]: "+g[1][i][l] );
				for (Rule rule : rec.getSuffixApplicableRules( i-1 )) {
//					System.out.println( rule );
					int[] rhs = rule.getRight();
					int num_smaller = 0;
					Boolean isValid = false;
					for (int j=0; j<rhs.length; j++) {
						// check whether the rule generates [target_token, k].
						isValid |= target_qgram.equals( Arrays.copyOfRange( rhs, j, j+1 ) ) && l-rhs.length+j == k;
						num_smaller += comparePosQGrams( Arrays.copyOfRange( rhs, j, j+1 ), l-rhs.length+j, target_qgram.qgram, k )==-1?1:0;
					}
//					System.out.println( "isValid: "+isValid );
//					System.out.println( "num_smaller: "+num_smaller );
					if ( i-rule.leftSize() >= 0 && l-rule.rightSize() >= 0) {
						g[1][i][l] = Math.min( g[1][i][l], g[1][i-rule.leftSize()][l-rule.rightSize()] + num_smaller );
						if (isValid) g[1][i][l] = Math.min( g[1][i][l], g[0][i-rule.leftSize()][l-rule.rightSize()] + num_smaller );
					}
				}
//				System.out.println( "g[1]["+i+"]["+l+"]: "+g[1][i][l] );
			}
		}
//		System.out.println(Arrays.deepToString(g[1]).replaceAll( "],", "]\n" ));

		Boolean res = false;
		for (int l=1; l<=len_max_S; l++) res |= (g[1][rec.size()][l] == 0);
		return res;
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
		case PositionFirst: {
			if ( idx.get( 0 ) == null ) idx.put( 0, new WYK_HashMap<QGram, List<Record>>() );
//			WYK_HashMap<QGram, List<Record>> invList = idx.get( 0 );
			QGram key = availableQGrams.get( 0 ).get( 0 ); // there is a single qgram at position 0.
			if (idx.get(0).get( key ) == null ) idx.get(0).put(key, new ObjectArrayList<Record>(this.initCapacity));
			idx.get( 0 ).get( key ).add( record );
			break;
		}
			
		case TokenIndexFirst: {
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
	
	private int comparePosQGrams(int[] qgram0, int pos0, int[] qgram1, int pos1 ) {
		int res = Integer.MAX_VALUE;
		switch (globalOrder) {
		case PositionFirst:
			res = Integer.compare( pos0, pos1 );
			if (res != 0 ) return res;
			else res = compareQGrams( qgram0, qgram1 );
			break;

		case TokenIndexFirst:
			res = compareQGrams( qgram0, qgram1 );
			if (res != 0 ) return res;
			else res = Integer.compare( pos0, pos1 );
			break;

		default:
			throw new RuntimeException("UNIMPLEMENTED CASE");
		}
		assert res != Integer.MAX_VALUE;
		return res;
	}
	
	private int compareQGrams(int[] qgram0, int[] qgram1) {
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