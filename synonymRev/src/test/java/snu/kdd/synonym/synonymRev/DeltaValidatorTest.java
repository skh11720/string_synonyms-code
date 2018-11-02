package snu.kdd.synonym.synonymRev;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import org.junit.Ignore;
import org.junit.Test;

import snu.kdd.synonym.synonymRev.algorithm.delta.DeltaValidatorDP;
import snu.kdd.synonym.synonymRev.algorithm.delta.DeltaValidatorDPTopDown;
import snu.kdd.synonym.synonymRev.algorithm.delta.DeltaValidatorNaive;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class DeltaValidatorTest {
	
	/*
	 * Test for the DeltaValidators and their subroutines.
	 * The validators support the edit distance.
	 */
	
	@Ignore
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
	
	@Ignore
	public void testDeltaValidators() throws IOException {
		String name = "AOL";
		int size = 100000;
		int deltaMax = 3; // TODO: vary deltaMax
		Query query = TestUtils.getTestQuery( name, size );
		Validator checker0 = new DeltaValidatorNaive( deltaMax );
		Validator checker1 = new DeltaValidatorDP(deltaMax);
		Validator checker2 = new DeltaValidatorDPTopDown(deltaMax);
		Validator[] checker_list = {checker0, checker1, checker2};
		long[] ts_list = new long[checker_list.length];
		int[] answer_list = new int[checker_list.length];
		
		int nPairs = 1000000;
		Random rand = new Random(0);
		long ts;
		for ( int i=0; i<nPairs; ++i ) {
			Record recS = query.searchedSet.recordList.get( rand.nextInt( query.searchedSet.size() ) );
			Record recT = query.indexedSet.recordList.get( rand.nextInt( query.indexedSet.size() ) );
			if ( recS.getEstNumTransformed() > 10000 ) continue;

			System.out.print((i+1)+"\t"+recS.getID()+"\t"+recT.getID());
			for ( int j=0; j<checker_list.length; ++j ) {
				ts = System.nanoTime();
				answer_list[j] = checker_list[j].isEqual(recS, recT);
				ts_list[j] += System.nanoTime() - ts;
				System.out.print("\t"+answer_list[j]);
				if ( j > 0 ) assertEquals(answer_list[0], answer_list[j]);
			}
			System.out.println();
		}
		
		System.out.println("Execution times");
		for ( int j=0; j<checker_list.length; ++j ) {
			System.out.println(checker_list[j].getName()+": "+ts_list[j]/1e6);
		}
	}

	@Test
	public void testTimeDeltaValidators() throws IOException {
		String name = "AOL";
		int size = 100000;
		int deltaMax = 3; // TODO: vary deltaMax
		Query query = TestUtils.getTestQuery( name, size );
		Validator checker1 = new DeltaValidatorDP(deltaMax);
		Validator checker2 = new DeltaValidatorDPTopDown(deltaMax);
		Validator[] checker_list = {checker1, checker2};
		long[] ts_list = new long[checker_list.length];
		int[] answer_list = new int[checker_list.length];
		
		int nPairs = 1000000;
		Random rand = new Random(0);
		long ts;
		for ( int i=0; i<nPairs; ++i ) {
			Record recS = query.searchedSet.recordList.get( rand.nextInt( query.searchedSet.size() ) );
			Record recT = query.indexedSet.recordList.get( rand.nextInt( query.indexedSet.size() ) );
			if ( recS.getEstNumTransformed() > 10000 ) continue;

			if ( (i+1) % 1000 == 0 ) System.out.print((i+1)+"\t"+recS.getID()+"\t"+recT.getID());
			for ( int j=0; j<checker_list.length; ++j ) {
				ts = System.nanoTime();
				answer_list[j] = checker_list[j].isEqual(recS, recT);
				ts_list[j] += System.nanoTime() - ts;
				if ( (i+1) % 1000 == 0 ) System.out.print("\t"+answer_list[j]);
			}
			if ( (i+1) % 1000 == 0 ) System.out.println();
		}
		
		System.out.println("Execution times");
		for ( int j=0; j<checker_list.length; ++j ) {
			System.out.println(checker_list[j].getName()+": "+ts_list[j]/1e6);
		}
	}
	
	public void testDeltaValidatorDP( Record x, Record y, int deltaMax ) throws IOException {
		System.out.println("x: "+Arrays.toString(x.getTokensArray()));
		System.out.println("y: "+Arrays.toString(y.getTokensArray()));
		
		
		int Lx = 5, Ly = 5;
		boolean[][][] M = new boolean[deltaMax+1][Lx+1][Ly+1]; 
		
		// enlarge the array M if necessary
		int lx = x.size();
		int ly = y.size();
		if ( lx > Lx || ly > Ly ) {
			Lx = Math.max( lx, Lx );
			Ly = Math.max( ly, Ly );
			M = new boolean[deltaMax+1][Lx+1][Ly+1];
		}

		// initialize M
		int[][] L = x.getTransLengthsAll();
		for ( int d=0; d<=deltaMax; ++d ) {
			M[d][0][0] = true;
			for ( int i=1; i<=lx; ++i ) M[d][i][0] = (L[i-1][0] <= d);
			for ( int j=1; j<=ly; ++j ) M[d][0][j] = (j <= d);
			for ( int i=1; i<=lx; ++i ) {
				for ( int j=1; j<=ly; ++j ) M[d][i][j] = false;
			}
		}
		
		for ( int i=1; i<=lx; ++i ) {
			for ( Rule rule : x.getSuffixApplicableRules(i-1) ) {
				int[] rhs = rule.getRight();
				int[][] D = new int[ly+1][];
				/*
				 * D[j0][j1] is the edit distance between rhs and y[j0:j1].
				 * 0 <= j0 <= j1 and 1 <= j1 <= |t|.
				 */
				if ( i - rule.leftSize() < 0 ) continue;
				for ( int j0=0; j0<=ly; ++j0 ) {
					D[j0] = Util.edit_all( rhs, y.getTokensArray(), j0 ); // D[j0][j0], D[j0][j0+1] ... are valid values
				}
				// given the current rule, find j0 which satisfies the condition in the recurrence equation for every 1 <= j <= |y|.
				for ( int j=1; j<=ly; ++j ) {
					for ( int d=0; d<=deltaMax; ++d ) {
						for ( int j0=0; j0<=j; ++j0 ) {
							if ( D[j0][j] > d ) continue;
							M[d][i][j] |= M[ d - D[j0][j] ][i - rule.leftSize()][j0];
						}
					}
				}
				// end updating M[d][i][j] for i and rule
				System.out.println(i+", "+rule);
				Util.print3dArray(M);
			}
		}
		// end computing M
		System.out.println("deltaMax, lx, ly: "+deltaMax+", "+lx+", "+ly);
		System.out.println("M[deltaMax][lx][ly]: "+M[deltaMax][lx][ly]);

	}
}
