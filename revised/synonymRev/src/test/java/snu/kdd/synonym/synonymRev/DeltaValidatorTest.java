package snu.kdd.synonym.synonymRev;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import snu.kdd.synonym.synonymRev.algorithm.delta.DeltaValidator;
import snu.kdd.synonym.synonymRev.data.ACAutomataR;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.validator.NaiveOneSideDelta;

public class DeltaValidatorTest {
	
	private static Query query;
	private static ACAutomataR automata;
	private static Random random = new Random();
//	private static NaiveOneSide naiveValidator = new NaiveOneSide();
	private static int replace = -1001;
	
	@BeforeClass
	public static void initialize() throws IOException {
		query = TestUtils.getTestQuery(10000);
		automata = new ACAutomataR( query.ruleSet.get());
	}
	
	@Test
	public void testIsEqualsFixed() throws IOException {
		for ( int seed=0; seed<1; ++seed ) {
//			System.out.println( "seed: "+seed );
			random = new Random(seed);
			query = TestUtils.getTestQuery(10000);
		
			int[][] ridPairArray = { 
					{1209, 1210}, {1901, 1902}, {3740, 4384}, {3490, 3418}, 
					{3537, 3539}, {3539, 3537}, {3600, 3601}, {3601, 3600}, 
					{3644, 3645}, {3645, 3644}, {3734, 4383}, {3740, 4384}, 
					{3822, 4391}, {3834, 4393}, {3845, 4009}, {3847, 4394}, 
					{3868, 4396}, {3921, 3922}, {3922, 3921}, {3936, 4484},
					{386, 436}, {321, 922}, {322, 391}, {396, 484},
			};

			int deltaMax = 4;
			
			for ( int k=0; k<ridPairArray.length; ++k ) {
				int i = ridPairArray[k][0];
				int j = ridPairArray[k][1];
				Record x = query.searchedSet.getRecord( i );
				Record y = query.searchedSet.getRecord( j );
//				SampleDataTest.inspect_record( x, query, 1 );
				long n_est_expand = x.getEstNumTransformed();
//				System.out.println( i+", "+j );
//				System.out.println( "n_est_expand: "+n_est_expand );

				int errorMax = 3;
				for ( int nError=0; nError<=errorMax; ++nError ) {
					if ( nError > 0 ) {
						if ( random.nextInt(2) == 0 ) {
							x = insertError( x );
						}
						else y = insertError( y );
					}
//					System.out.println( "x: "+Arrays.toString( x.getTokensArray() ) );
//					System.out.println( "y: "+Arrays.toString( y.getTokensArray() ) );
					for ( int delta=0; delta<=deltaMax; ++delta ) {
						NaiveOneSideDelta naiveValidator = new NaiveOneSideDelta( delta );
						DeltaValidator deltaValidator = new DeltaValidator( delta );
						int naiveOutput = naiveValidator.isEqual( x, y );
						int deltaOutput = deltaValidator.isEqual( x, y );
//						System.out.println( delta+"-naive, delta: "+naiveOutput+", "+deltaOutput );
						assertEquals( naiveOutput, deltaOutput );
//						if ( delta >= nError ) assertEquals( 1, deltaOutput );
//						else assertEquals( -1, deltaOutput );
					} // end for ridPairArray
				} // end for nError
			} // end for delta
		} // end for seed
	}
	
	private Record insertError( Record rec ) {
		int i_rep = random.nextInt( rec.size() );
		int[] arr = rec.getTokensArray();
		int[] newArr = new int[rec.size()+1];
		for ( int i=0; i<=rec.size(); ++i ) {
			if ( i < i_rep ) newArr[i] = arr[i];
			else if ( i > i_rep ) newArr[i] = arr[i-1];
			else newArr[i] = replace--;
		}
		rec = new Record(newArr);
		rec.preprocessRules( automata );
		rec.preprocessSuffixApplicableRules();
		rec.preprocessTransformLength();
		rec.preprocessTransformLength();
		rec.preprocessEstimatedRecords();
		return rec;
	}
	
	@Ignore
	public void testIsSuffixWithErrors() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		DeltaValidator target = new DeltaValidator( 3 );
		Method method = DeltaValidator.class.getDeclaredMethod( "isSuffixWithErrors", int[].class, int[].class, int.class );
		method.setAccessible( true );
//		Random random = new Random(0);
//		int nTest = 10;
//		int nv = 2; // size of vocabulary

		int[][] pat_list = {
				{0, 1},
				{1, 0},
				{0, 0, 1, 0},
				{1, 0, 0, 0},
				{0, 1, 0},
				{0, 0, 1, 0, 1},
				{0, 1, 0, 1},
				{1, 1, 0, 0, 0},
				{1, 0, 0, 1},
				{1, 1},
		};
		int[][] seq_list = {
				{1, 0, 1, 0, 1, 1, 0, 0, 0, 1},
				{1, 0, 0, 0, 0, 0},
				{1, 1},
				{1, 1, 1, 1, 0, 1, 1, 1, 1},
				{1, 1, 1, 1},
				{1, 1, 0, 0, 0, 0, 0, 0, 1},
				{1, 1, 1},
				{0, 0, 1, 0, 0, 0, 1, 0, 1},
				{0, 1, 1, 0, 1, 0, 1},
				{1, 0, 1, 0, 0, 0, 1},
		};
		int[] answer_list = {0, 1, 3, 3, 2, 1, 2, 4, 1, 1};

		for ( int itr=0; itr<10; ++itr ) {
//			System.out.println( "itr: "+itr );
//			int patLen = random.nextInt( 5 ) + 2;
//			int seqLen = random.nextInt( 10 ) + 2;
//			int[] pat = random.ints( patLen, 0, nv ).toArray();
//			int[] seq = random.ints( seqLen, 0, nv ).toArray();
			int[] pat = pat_list[itr];
			int[] seq = seq_list[itr];
			
//			System.out.println( "pat: "+Arrays.toString( pat ) );
//			System.out.println( "seq: "+Arrays.toString( seq) );
			assertTrue( answer_list[itr] == (int)(method.invoke( target, pat, seq, seq.length )) );
		}
	}
	
	@Ignore
	public void testLcsDist() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Method method = DeltaValidator.class.getDeclaredMethod( "lcsDist", int[].class, int[].class, int.class );
		DeltaValidator target = new DeltaValidator( 3 );
		method.setAccessible( true );

		int[][] pat_list = {
				{0, 1},
				{1, 0},
				{0, 0, 1, 0},
				{1, 0, 0, 0},
				{0, 1, 0},
				{0, 0, 1, 0, 1},
				{0, 1, 0, 1},
				{1, 1, 0, 0, 0},
				{1, 0, 0, 1},
				{1, 1},
		};
		int[][] seq_list = {
				{1, 0, 1, 0, 1, 1, 0, 0, 0, 1},
				{1, 0, 0, 0, 0, 0},
				{1, 1},
				{1, 1, 1, 1, 0, 1, 1, 1, 1},
				{1, 1, 1, 1},
				{1, 1, 0, 0, 0, 0, 0, 0, 1},
				{1, 1, 1},
				{0, 0, 1, 0, 0, 0, 1, 0, 1},
				{0, 1, 1, 0, 1, 0, 1},
				{1, 0, 1, 0, 0, 0, 1},
		};
		int[][] answer_list = {
				{8, 7, 6, 5, 4, 3, 2, 1, 0, 1, 2},
				{4, 5, 4, 3, 2, 1, 2},
				{4, 3, 4},
				{9, 8, 7, 6, 7, 6, 5, 4, 3, 4},
				{5, 4, 3, 2, 3},
				{6, 5, 4, 3, 2, 1, 2, 3, 4, 5},
				{3, 2, 3, 4},
				{6, 5, 4, 5, 4, 5, 4, 5, 4, 5},
				{3, 2, 1, 2, 1, 2, 3, 4},
				{5, 4, 3, 4, 3, 2, 1, 2}
		};

		for ( int itr=0; itr<10; ++itr ) {
//			System.out.println( "itr: "+itr );
//			int patLen = random.nextInt( 5 ) + 2;
//			int seqLen = random.nextInt( 10 ) + 2;
//			int[] pat = random.ints( patLen, 0, nv ).toArray();
//			int[] seq = random.ints( seqLen, 0, nv ).toArray();
			int[] pat = pat_list[itr];
			int[] seq = seq_list[itr];
			
//			System.out.println( "pat: "+Arrays.toString( pat ) );
//			System.out.println( "seq: "+Arrays.toString( seq) );
			int[] D = (int[])(method.invoke( target, pat, seq, seq.length ));
			assertTrue( Arrays.equals( D, answer_list[itr] ) );
		}
	}
	
	@Ignore
	public void testIsEquals() {
		int deltaMaxMax = 2;
		for ( int deltaMax=0; deltaMax<=deltaMaxMax; ++deltaMax ) {
			DeltaValidator deltaValidator = new DeltaValidator( deltaMax );
			int nTest = 5;
			long t = 0;
			for ( int idx_test=0; idx_test<nTest; ++idx_test ) {
				long ts = System.currentTimeMillis();
				int n = 300;
				for ( int i=0; i<n; ++i ) {
					Record x = query.searchedSet.getRecord( i );
					for ( int j=0; j<n; ++j ) {
						Record y = query.searchedSet.getRecord( j );
						deltaValidator.isEqual( x, y );
					}
				}
				t += System.currentTimeMillis() - ts;
			}
			System.out.println( "deltaMax="+deltaMax+", time: "+t/nTest+" ms" );
		}
	}
}
