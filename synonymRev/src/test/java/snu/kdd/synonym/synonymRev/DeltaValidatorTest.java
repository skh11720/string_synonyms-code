package snu.kdd.synonym.synonymRev;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Random;

import org.junit.Test;

import snu.kdd.synonym.synonymRev.algorithm.delta.DeltaValidatorNaive;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class DeltaValidatorTest {
	
	/*
	 * Test for the DeltaValidators and their subroutines.
	 * The validators support the edit distance.
	 */
	
	@Test
	public void testEditDistance() {
		int[] x, y;
		
		x = new int[]{1,1,1,1,1};
		y = new int[]{1,1,1,1,1};
		assertEquals( 0, Util.edit( x, y ) );
		
		x = new int[] {1,2,3,4,5};
		y = new int[] {1,2,3,4,5};
		assertEquals( 0, Util.edit( x, y ) );
		
		x = new int[] {1,2,3,4,5};
		y = new int[] {5,4,3,2,1};
		assertEquals( 4, Util.edit( x, y ) );

		x = new int[] {5,4,1,2,3};
		y = new int[] {5,4,3,2,1};
		assertEquals( 2, Util.edit( x, y ) );

		x = new int[] {3,4,5,2,1};
		y = new int[] {5,4,3,2,1};
		assertEquals( 2, Util.edit( x, y ) );

		x = new int[] {1,2,3,4,5,5,5,5,5,5};
		y = new int[] {1,2,3,4,5};
		assertEquals( 5, Util.edit( x, y ) );

		x = new int[] {1,1,1,1,1,1,2,3,4,5};
		y = new int[] {1,2,3,4,5};
		assertEquals( 5, Util.edit( x, y ) );
	}
	
	@Test
	public void testDeltaValidators() throws IOException {
		String name = "SPROT";
		int size = 100000;
		int deltaMax = 3; // TODO: vary deltaMax
		Query query = TestUtils.getTestQuery( name, size );
		Validator checker0 = new DeltaValidatorNaive( deltaMax );
		
		int nPairs = 1000000;
		Random rand = new Random(0);
		for ( int i=0; i<nPairs; ++i ) {
			Record recS = query.searchedSet.recordList.get( rand.nextInt( query.searchedSet.size() ) );
			Record recT = query.indexedSet.recordList.get( rand.nextInt( query.indexedSet.size() ) );
			int answer0 = checker0.isEqual(recS, recT);
			if ( answer0 >= 0 ) {
				System.out.println( recS.getID() +", "+ recT.getID() +"\t"+ recS + "\t"+recT );
			}
		}
	}
}
