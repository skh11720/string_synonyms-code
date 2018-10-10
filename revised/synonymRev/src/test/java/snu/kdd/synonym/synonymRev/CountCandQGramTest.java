package snu.kdd.synonym.synonymRev;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.QGram;

public class CountCandQGramTest {
	String[] datasetList = {"AOL", "SPROT", "USPS", "SYN_100K"};
	int[] sizeList = {10000, 100000};
//	String[] datasetList = {"SPROT"};
//	int[] sizeList = {10000};
	
	@Test
	public void test() throws IOException {
		String dataset = "SYN_1M";
		int size = 100000;
		Query query = TestUtils.getTestQuery(dataset, size);
		Int2IntOpenHashMap counter = new Int2IntOpenHashMap();
		counter.defaultReturnValue( 0 );
		for ( Record rec : query.searchedSet.recordList ) {
			for ( int k=0; k<1; ++k ) {
				for ( Rule rule : rec.getSuffixApplicableRules( k ) ) counter.addTo( rule.leftSize(), 1 );
			}
		}
		System.out.println( "|LHS| histogram:"+counter );
		
		counter.clear();
		for ( Record rec : query.searchedSet.recordList ) {
			for ( int k=0; k<1; ++k ) {
				for ( Rule rule : rec.getSuffixApplicableRules( k ) ) counter.addTo( rule.rightSize(), 1 );
			}
		}
		System.out.println( "|RHS| histogram:"+counter );
		
		
		counter.clear();
		for ( Record rec : query.searchedSet.recordList ) {
			counter.addTo( rec.size(), 1 );
		}
		System.out.println( "|s| histogram:"+counter );
		
		counter.clear();
		for ( Record rec : query.indexedSet.recordList ) {
			counter.addTo( rec.size(), 1 );
		}
		System.out.println( "|t| histogram:"+counter );
	}

	@Ignore
	public void testCountCandQGrams() throws IOException {
		
		MyPrintStream ps = new MyPrintStream( new BufferedOutputStream( new FileOutputStream( "tmp/CountCandQGramTest.txt" ) ) );

		int[] sizeList = {100000};
		String[] datasetList = {"SYN_300K"};
		for ( int size : sizeList ) {
			for ( String dataset : datasetList ) {
				System.out.println( dataset+'\t'+size );
				Query query = TestUtils.getTestQuery(dataset, size);

//				System.out.println( "varying q" );
//				int K_fixed = 1;
//				for ( int q=1; q<=5; ++q ) {
//					compareTPQs( ps, query, dataset, size, q, K_fixed );
//				}
				
				System.out.println( "varying K" );
				final int q = 2;
				final int K = 10;
				compareTPQs( ps, query, dataset, size, q, K );
			}
		}
		ps.flush(); ps.close();
		
	}
	
	private List<Set<QGram>> getTPQ( Record rec, int q, int K ) {
		List<Set<QGram>> tpq = new ObjectArrayList<>();
		for ( int k=0; k<K; ++k ) tpq.add( new ObjectOpenHashSet<QGram>() );
		for ( Record exp : rec.expandAll() ) {
			List<List<QGram>> exp_pq = exp.getSelfQGrams( q, K );
			for ( int k=0; k<K; ++k ) {
				if ( k >= exp_pq.size() ) break;
				tpq.get( k ).addAll( exp_pq.get( k ) );
			}
		}
		return tpq;
	}
	
	private void compareTPQs( PrintStream ps, Query query, String dataset, int size, int q, int K ) {
		int[][] output = countQGramsInTPQ( query, q, K );
		int[] nCandQGram = output[0];
		int[] nInTPQ = output[1];
		int[] nNotInTPQ = output[2];
		for ( int k=0; k<K; ++k ) {
			ps.println( dataset+"\t"+size+"\t"+q+"\t"+k+"\t"+nCandQGram[k]+"\t"+nInTPQ[k]+"\t"+nNotInTPQ[k]
					+"\t"+String.format( "%.3f", 1.0*nCandQGram[k]/size )+"\t"+String.format( "%.3f",  1.0*nNotInTPQ[k]/size ) );
		}
	}
	
	private int[][] countQGramsInTPQ( Query query, int q, int K ) {
		int[] nCandQGram = new int[K];
		int[] nInTPQ = new int[K];
		int[] nNotInTPQ = new int[K];
		Arrays.fill( nCandQGram, 0 );
		Arrays.fill( nInTPQ, 0 );
		Arrays.fill( nNotInTPQ, 0 );
		int size = query.searchedSet.size();
		for ( Record rec : query.searchedSet.recordList ) {
			if ( rec.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) continue;
			List<Set<QGram>> tpq = getTPQ( rec, q, K );
			List<List<QGram>> candQGrams = rec.getQGrams( q, K );
			for ( int k=0; k<K; ++k ) {
				if ( k >= candQGrams.size() ) break;
				Set<QGram> candQGramsK = new ObjectOpenHashSet<>( candQGrams.get( k ));
				nCandQGram[k] += candQGramsK.size();
				nInTPQ[k] += tpq.get( k ).size();
				for ( QGram qgram : candQGrams.get( k ) ) {
					if ( !tpq.get( k ).contains( qgram ) ) ++nNotInTPQ[k];
				}
			}
		}

		return new int[][] {nCandQGram, nInTPQ, nNotInTPQ};
	}
	
	class MyPrintStream extends PrintStream {
		
		public MyPrintStream( OutputStream out ) {
			super( out );
			// TODO Auto-generated constructor stub
		}
		
		@Override
		public void println( String x ) {
			super.println( x );
			System.out.println( x );
		}
	}
}
