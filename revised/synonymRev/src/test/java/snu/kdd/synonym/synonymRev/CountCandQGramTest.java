package snu.kdd.synonym.synonymRev;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.QGram;

public class CountCandQGramTest {
	String[] datasetList = {"AOL", "SPROT", "USPS", "SYN_100K"};
	int[] sizeList = {10000, 100000};

	@Test
	public void test() throws IOException {
		
		MyPrintStream ps = new MyPrintStream( new BufferedOutputStream( new FileOutputStream( "tmp/CountCandQGramTest.txt" ) ) );

//		int size = 10000;
//		Query query = TestUtils.getTestQuery("SPROT", size);
		for ( int size : sizeList ) {
			for ( String dataset : datasetList ) {
				System.out.println( dataset+'\t'+size );
				Query query = TestUtils.getTestQuery(dataset, size);

				System.out.println( "varying q" );
				int K_fixed = 1;
				for ( int q=1; q<=5; ++q ) {
					compareTPQs( ps, query, dataset, size, q, K_fixed );
				}
				
				System.out.println( "varying K" );
				int q_fixed = 2;
				for ( int K=1; K<=5; ++K ) {
					compareTPQs( ps, query, dataset, size, q_fixed, K );
				}
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
		int[] output = countQGramsInTPQ( query, q, K );
		int nCandQGram = output[0];
		int nNotInTPQ = output[1];
		ps.println( dataset+"\t"+size+"\t"+q+"\t"+K+"\t"+nCandQGram+"\t"+nNotInTPQ+"\t"+String.format( "%.3f", 1.0*nCandQGram/size )+"\t"+String.format( "%.3f",  1.0*nNotInTPQ/size ) );
	}
	
	private int[] countQGramsInTPQ( Query query, int q, int K ) {
		int nCandQGram = 0;
		int nNotInTPQ = 0;
		int size = query.searchedSet.size();
		for ( Record rec : query.searchedSet.recordList ) {
			if ( rec.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) continue;
			List<Set<QGram>> tpq = getTPQ( rec, q, K );
			List<List<QGram>> candQGrams = rec.getQGrams( q, K );
			for ( int k=0; k<K; ++k ) {
				if ( k >= candQGrams.size() ) break;
				nCandQGram += candQGrams.get( k ).size();
				for ( QGram qgram : candQGrams.get( k ) ) {
					if ( !tpq.get( k ).contains( qgram ) ) ++nNotInTPQ;
				}
			}
		}
		return new int[] {nCandQGram, nNotInTPQ};
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
