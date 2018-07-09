package snu.kdd.synonym.synonymRev;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import org.junit.BeforeClass;
import org.junit.Test;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import snu.kdd.synonym.synonymRev.algorithm.delta.JoinNaiveDelta2;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.Util;

public class JoinNaiveDelta2Test {
	static Query query;
	
	@BeforeClass
	public static void getQuery() throws IOException {
		query = TestUtils.getTestQuery( 1000 );
	}

//	@Test
//	public void testSortIndexedRecords() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException, NoSuchFieldException {
//		JoinNaiveDelta2 alg = new JoinNaiveDelta2(query, new StatContainer());
//		Method method = JoinNaiveDelta2.class.getDeclaredMethod( "sortIndexedRecords", List.class);
//		method.setAccessible( true );
//		method.invoke( alg, query.indexedSet.recordList );
//		for ( int i=0; i<query.indexedSet.size(); ++i )
//			System.out.println( ""+i+": "+Arrays.toString( query.indexedSet.getRecord( i ).getTokensArray() ) );
//	}
	
	@Test
	public void test() {
		Random rand = new Random( 0 );
		int deltaMax = 1;
		for ( int i=0; i<10000; ++i ) {
			Record x = query.searchedSet.getRecord( rand.nextInt( 1000 ) );
			Record y = query.searchedSet.getRecord( rand.nextInt( 1000 ) );
			int[] yTokens = y.getTokensArray();
			
			for ( Record x_exp : x.expandAll() ) {
				int[] x_expTokens = x_exp.getTokensArray();
//				System.out.println( "x_exp: "+Arrays.toString( x_expTokens ) );
//				System.out.println( "y: "+Arrays.toString( yTokens ) );
				int len_lcs = Util.lcs( x_expTokens, yTokens );
				int d_lcs1 = x_exp.size() + y.size() - 2 * len_lcs;
//				System.out.println( "d_LCS1: "+ d_lcs1 );
				int d_lcs2 = Util.lcs( x_expTokens, yTokens, deltaMax, 0, 0, -1, -1 );
//				System.out.println( "d_LCS2: "+ d_lcs2 );
				if ( d_lcs2 > deltaMax) assertTrue( d_lcs1 > deltaMax );
				else assertEquals( d_lcs1, d_lcs2 );
			}
		}
	}
}
