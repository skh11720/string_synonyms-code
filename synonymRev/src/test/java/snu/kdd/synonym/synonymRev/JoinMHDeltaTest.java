package snu.kdd.synonym.synonymRev;

import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import snu.kdd.synonym.synonymRev.algorithm.delta.JoinMHDeltaIndex;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.StatContainer;

public class JoinMHDeltaTest {
	
	private static Query query;
	private static JoinMHDeltaIndex index;
	@BeforeClass
	public static void initialize() throws IOException {
		query = TestUtils.getTestQuery(10000);
		int indexK = 1;
		int qgramSize = 2;
		int deltaMax = 1;
		int[] indexPosition = {0};
		index = new JoinMHDeltaIndex(indexK, qgramSize, deltaMax, query.indexedSet.get(), query, new StatContainer(), indexPosition, false, true, 0);
	}

	@Ignore	
	public void testGetCandidatePQGrams() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Method method = JoinMHDeltaIndex.class.getDeclaredMethod( "getCandidatePQGrams", Record.class );
		method.setAccessible( true );
		
		Record rec;
		List<List<List<QGram>>> cand_pqgrams;
		
		rec = query.searchedSet.getRecord( 677 );
		System.out.println( "rec "+rec.getID()+": "+rec+", "+Arrays.toString( rec.getTokensArray() ) );
		cand_pqgrams = (List<List<List<QGram>>>) method.invoke( index, rec );
		System.out.println( cand_pqgrams );
//
//		rec = query.searchedSet.getRecord( 297 );
//		System.out.println( "rec "+rec.getID()+": "+Arrays.toString( rec.getTokensArray() ) );
//		cand_pqgrams = (List<List<List<QGram>>>) method.invoke( index, rec );
//		System.out.println( cand_pqgrams );
//
//		rec = query.searchedSet.getRecord( 351 );
//		System.out.println( "rec "+rec.getID()+": "+Arrays.toString( rec.getTokensArray() ) );
//		cand_pqgrams = (List<List<List<QGram>>>) method.invoke( index, rec );
//		System.out.println( cand_pqgrams );
	}

}
