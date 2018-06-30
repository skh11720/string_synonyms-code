package snu.kdd.synonym.synonymRev;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.algorithm.delta.DeltaValidator;
import snu.kdd.synonym.synonymRev.algorithm.delta.JoinMHDeltaIndex;
import snu.kdd.synonym.synonymRev.algorithm.delta.JoinMinDeltaIndex;
import snu.kdd.synonym.synonymRev.algorithm.delta.NaiveDeltaIndex;
import snu.kdd.synonym.synonymRev.data.Dataset;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.index.JoinMHIndex;
import snu.kdd.synonym.synonymRev.index.JoinMinIndex;
import snu.kdd.synonym.synonymRev.index.NaiveIndex;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.validator.TopDownOneSide;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class TestForJoinHybrid {
	public static Query query;
	public static final int N = 1000; // number of records in searchedSet
	public static final int M = 10; // repeat join one record
	public static final int threshold = (int) 1e4;
	
	public static void main( String[] args ) throws IOException, NoSuchMethodException, SecurityException {
		query = TestUtils.getTestQuery("AOL", 100000);
		Random random = new Random(0);
		StatContainer stat = new StatContainer();
		double estTransformed = 0.0;
		for( Record rec : query.indexedSet.get() ) {
			estTransformed += rec.getEstNumTransformed();
		}
		double avgTransformed = estTransformed / query.indexedSet.size();
		int deltaMax = 1;
		int indexK = 1;
		int[] indexPosition = {0};
		int qgramSize = 2;
		TopDownOneSide val0 = new TopDownOneSide();
		DeltaValidator val1 = new DeltaValidator( deltaMax );
		

		// build indexes
		NaiveIndexWrapper naiveIndex = new NaiveIndexWrapper( query.indexedSet, query, stat, false, -1, avgTransformed );
		NaiveDeltaIndexWrapper naiveDeltaIndex = new NaiveDeltaIndexWrapper( query.indexedSet, query, stat, true, deltaMax, -1, avgTransformed );

		JoinMHIndexWrapper joinMHIndex = new JoinMHIndexWrapper( indexK, qgramSize, query.indexedSet.recordList, query, stat, indexPosition, true, true, -1 );
		joinMHIndex.set( val0 );
		JoinMinIndexWrapper joinMinIndex = new JoinMinIndexWrapper( indexK, qgramSize, stat, query, -1, true );
		joinMinIndex.set( val0, indexK );
		
		JoinMHDeltaIndexWrapper joinMHDeltaIndex = new JoinMHDeltaIndexWrapper( indexK, qgramSize, deltaMax, query.indexedSet.recordList, query, stat, indexPosition, true, true, -1 );
		joinMHDeltaIndex.set( val1 );
		JoinMinDeltaIndexWrapper joinMinDeltaIndex = new JoinMinDeltaIndexWrapper( indexK, qgramSize, deltaMax, stat, query, -1, true );
		joinMinDeltaIndex.set( val1, indexK );
		
		Set<IntegerPair> rslt;
		
		System.out.println( "Randomly select N records" );
		List<Record> recSList = new ObjectArrayList<>();
		IntOpenHashSet idxSet = new IntOpenHashSet();
		while ( idxSet.size() < N ) {
			int idx = random.nextInt( query.searchedSet.size() );
			if ( query.searchedSet.getRecord( idx ).getEstNumTransformed() > threshold ) continue;
			idxSet.add( idx );
		}

		System.out.println( "Sort records in searchedSet" );
		// Sort records in searchedSet in increasing order of #transformations.
		for ( int i : idxSet ) {
			Record rec = query.searchedSet.getRecord( i );
			recSList.add( rec );
		}
		Comparator<Record> cmp = new Comparator<Record>() {
			@Override
			public int compare( Record o1, Record o2 ) {
				long est1 = o1.expandAll().size();
				long est2 = o2.expandAll().size();
				return Long.compare( est1, est2 );
			}
		};
		Collections.sort( recSList, cmp );
		
		
		// prepare join test
		
		// join, delta = 0
		IndexInterface[] indexList = {naiveIndex, joinMHIndex, joinMinIndex, naiveDeltaIndex, joinMHDeltaIndex, joinMinDeltaIndex};
		double[][] timeMat = new double[indexList.length][];
		for ( int i=0; i<indexList.length; ++i ) {
			IndexInterface index = indexList[i];
			timeMat[i] = join(index, recSList);
		}
		
		// write the results
		BufferedWriter bw = new BufferedWriter( new FileWriter( "tmp/hybridTest.txt" ) );
		for ( int j=0; j<recSList.size(); ++j ) {
			Record recS = recSList.get( j );
			int nExp = recS.expandAll().size();
			bw.write( ""+nExp );
			for ( int i=0; i<indexList.length; ++i ) {
				bw.write( "\t"+timeMat[i][j] );
			}
			bw.write( "\n" );
		}
		bw.flush();
		bw.close();
	}
	
	public static double[] join( IndexInterface index, List<Record> recSList ) {
		Set<IntegerPair> rslt = new ObjectOpenHashSet<>();
		double[] timeList = new double[recSList.size()];
		for ( int i=0; i<recSList.size(); ++i ) {
			Record recS = recSList.get( i );
			int nExp = recS.expandAll().size();
			long t = -1;
			try { 
				t = System.nanoTime();
				for ( int j=0; j<M; ++j ) index.joinOneRecord( recS, rslt );
				t = (System.nanoTime() - t)/M;
			}
			catch ( OutOfMemoryError e ) { continue; }
//			System.out.println( "(nExp, t) = "+nExp+String.format( ", %.3f", t/1e3 ) );
			timeList[i] = t/1e3;
		}
		System.out.println( index.getName()+": "+rslt.size() );
		return timeList;
	}
}

interface IndexInterface {
	void joinOneRecord( Record rec, Set<IntegerPair> rslt );
	String getName();
}

class NaiveIndexWrapper extends NaiveIndex implements IndexInterface {
	public NaiveIndexWrapper( Dataset indexedSet, Query query, StatContainer stat, boolean addStat, long threshold, double avgTransformed ) {
		super( indexedSet, query, stat, addStat, threshold, avgTransformed );
	}

	public String getName() { return "NaiveIndex"; }
}

class JoinMHIndexWrapper extends JoinMHIndex implements IndexInterface {
	Validator val;

	public JoinMHIndexWrapper( int indexK, int qgramSize, Iterable<Record> indexedSet, Query query, StatContainer stat,
			int[] indexPosition, boolean addStat, boolean useIndexCount, int threshold ) {
		super( indexK, qgramSize, indexedSet, query, stat, indexPosition, addStat, useIndexCount, threshold );
	}
	
	public void set( Validator val ) { this.val = val; }

	@Override
	public void joinOneRecord( Record rec, Set<IntegerPair> rslt ) {
		joinOneRecordThres( rec, rslt, val, -1, true );
	}

	public String getName() { return "JoinMHIndex"; }
}

class JoinMinIndexWrapper extends JoinMinIndex implements IndexInterface {
	int indexK;
	Validator val;
	public JoinMinIndexWrapper( int nIndex, int qSize, StatContainer stat, Query query, int threshold, boolean writeResult ) {
		super( nIndex, qSize, stat, query, threshold, writeResult );
	}

	public void set( Validator val, int indexK ) { 
		this.val = val; 
		this.indexK = indexK;
	}

	@Override
	public void joinOneRecord( Record rec, Set<IntegerPair> rslt ) {
		joinRecordMaxK( indexK, rec, rslt, false, null, val, true );
	}

	public String getName() { return "JoinMinIndex"; }
}

class NaiveDeltaIndexWrapper extends NaiveDeltaIndex implements IndexInterface {

	public NaiveDeltaIndexWrapper( Dataset indexedSet, Query query, StatContainer stat, boolean addStat, int deltaMax,
			long threshold, double avgTransformed ) {
		super( indexedSet, query, stat, addStat, deltaMax, threshold, avgTransformed );
	}
	
	@Override
	public String getName() { return "NaiveDeltaIndex"; }
}

class JoinMHDeltaIndexWrapper extends JoinMHDeltaIndex implements IndexInterface {
	Validator val;

	public JoinMHDeltaIndexWrapper( int indexK, int qgramSize, int deltaMax, Iterable<Record> indexedSet, Query query,
			StatContainer stat, int[] indexPosition, boolean addStat, boolean useIndexCount, int threshold ) {
		super( indexK, qgramSize, deltaMax, indexedSet, query, stat, indexPosition, addStat, useIndexCount, threshold );
	}
	
	public void set( Validator val ) {
		this.val = val;
	}

	@Override
	public void joinOneRecord( Record rec, Set<IntegerPair> rslt ) {
		joinOneRecordThres( rec, rslt, val, -1, true );
	}

	@Override
	public String getName() { return "JoinMHDeltaIndex"; }
}

class JoinMinDeltaIndexWrapper extends JoinMinDeltaIndex implements IndexInterface {
	
	int indexK;
	Validator val;

	public JoinMinDeltaIndexWrapper( int nIndex, int qSize, int deltaMax, StatContainer stat, Query query, int threshold,
			boolean writeResult ) {
		super( nIndex, qSize, deltaMax, stat, query, threshold, writeResult );
	}
	
	public void set( Validator val, int indexK ) {
		this.indexK = indexK;
		this.val = val;
	}

	@Override
	public void joinOneRecord( Record rec, Set<IntegerPair> rslt ) {
		joinRecordMaxKThres( indexK, rec, rslt, false, null, val, -1, true );
	}

	@Override
	public String getName() { return "JoinMinDeltaIndex"; }
}