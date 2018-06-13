package snu.kdd.synonym.synonymRev;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.seq.AbstractPosQGramFilterDP;
import snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.seq.PosQGramFilterDP;
import snu.kdd.synonym.synonymRev.data.ACAutomataR;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;

@Ignore
public class PosQGramFilterDPTest {
	
	private static Query query;
	private static Record record;
	private static int q;
	private PosQGramFilterDP target;
	
	@BeforeClass
	public static void initialize() throws IOException {
		String osName = System.getProperty( "os.name" );
		if ( osName.startsWith( "Windows" ) ) {
			final String dataOnePath = "D:\\ghsong\\data\\aol\\splitted\\aol_10000_data.txt";
			final String dataTwoPath = "D:\\ghsong\\data\\aol\\splitted\\aol_10000_data.txt";
			final String rulePath = "D:\\ghsong\\data\\wordnet\\rules.noun";
			final String outputPath = "output";
			final Boolean oneSideJoin = true;
			query = new Query(rulePath, dataOnePath, dataTwoPath, oneSideJoin, outputPath);
		}
		else if ( osName.startsWith( "Linux" ) ) {
			final String dataOnePath = "run/data_store/aol/splitted/aol_10000_data.txt";
			final String dataTwoPath = "run/data_store/aol/splitted/aol_10000_data.txt";
			final String rulePath = "run/data_store/wordnet/rules.noun";
			final String outputPath = "output";
			final Boolean oneSideJoin = true;
			query = new Query(rulePath, dataOnePath, dataTwoPath, oneSideJoin, outputPath);
		}
		
		// read
//		ObjectInputStream ois = new ObjectInputStream( new FileInputStream( "tmp/test_query.obj" ) );
//		SerializableQuery serialQuery = (SerializableQuery) ois.readObject();
//		ois.close();
//		serialQuery.query = query;

		// write
//		SerializableQuery serialQuery = new SerializableQuery();
//		query = serialQuery.query;
//		ObjectOutputStream oos = new ObjectOutputStream( new FileOutputStream( "tmp/test_query.obj" ) );
//		oos.writeObject( serialQuery );
//		oos.close();

		q = 1;
		record = query.searchedSet.getRecord( 0 );
		final ACAutomataR automata = new ACAutomataR( query.ruleSet.get());
		record.preprocessRules( automata );
		record.preprocessSuffixApplicableRules();
		record.preprocessTransformLength();
	}
	
	@Before
	public void beforeTest() {
		target = new PosQGramFilterDP( record, q );
	}
	
	@Test
	public void testExistence() {
//		fail( "Not yet implemented" );
	}

	@Test
	public void testIsPrefixOf() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Method method = AbstractPosQGramFilterDP.class.getDeclaredMethod( "isPrefixOf", int[].class, int.class, int.class, int[].class );
		method.setAccessible( true );
		Random random = new Random(0);
		int nTest = 10000;
		int nv = 3; // size of vocabulary
		for ( int itr=0; itr<nTest; itr++ ) {
//			System.out.println( "itr: "+itr );
			int patLen = random.nextInt( 5 ) + 1;
			int seqLen = random.nextInt( 15 ) + 6;
			int[] pat = random.ints( patLen, 0, nv ).toArray();
			int[] seq = random.ints( seqLen, 0, nv ).toArray();
			
			// true answer: trueAnswer[i][j] is true if pat[i:j] is a prefix of seq. (both inclusive)
			boolean[][] trueAnswer = new boolean[patLen][patLen];
			for ( int i=0; i<patLen-1; i++ ) {
				for ( int j=i; j<patLen; j++ ) {
					if ( i == j) trueAnswer[i][j] = (pat[i] == seq[j-i]); 
					else trueAnswer[i][j] = trueAnswer[i][j-1] && ( pat[j] == seq[j-i] );
				}
			}

			// answer by AbstractPosQGramFilterDP.isPrefixOf
			for ( int i=0; i<patLen-1; i++ ) {
				for (int j=i; j<patLen; j++ ) {
					assertTrue( trueAnswer[i][j] == (boolean)(method.invoke( target, pat, i, j+1, seq )) );
				}
			}
			
			// answer by this.isSubArrayOf
		}
	}

	@Test
	public void testIsSuffixOf() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Method method = AbstractPosQGramFilterDP.class.getDeclaredMethod( "isSuffixOf", int[].class, int.class, int.class, int[].class );
		method.setAccessible( true );
		Random random = new Random(0);
		int nTest = 10000;
		int nv = 3; // size of vocabulary
		for ( int itr=0; itr<nTest; itr++ ) {
//			System.out.println( "itr: "+itr );
			int patLen = random.nextInt( 5 ) + 1;
			int seqLen = random.nextInt( 15 ) + 6;
			int[] pat = random.ints( patLen, 0, nv ).toArray();
			int[] seq = random.ints( seqLen, 0, nv ).toArray();
			
			// true answer: trueAnswer[i][j] is true if pat[i:j] is a suffix of seq. (both inclusive)
			boolean[][] trueAnswer = new boolean[patLen][patLen];
//			System.out.println( "pat: "+Arrays.toString( pat ) );
//			System.out.println( "seq: "+Arrays.toString( seq ) );
			for ( int j=patLen-1; j>=0; --j ) {
				for ( int i=j; i>=0; --i ) {
//					System.out.println( ""+i+", "+j );
					if ( i == j ) trueAnswer[i][j] = (pat[i] == seq[seq.length+i-j-1]); 
					else trueAnswer[i][j] = trueAnswer[i+1][j] && ( pat[i] == seq[seq.length+i-j-1] );
				}
			}

			// answer by AbstractPosQGramFilterDP.isSuffixOf
			for ( int i=0; i<patLen-1; i++ ) {
				for (int j=i; j<patLen; j++ ) {
					assertTrue( trueAnswer[i][j] == (boolean)(method.invoke( target, pat, i, j+1, seq )) );
				}
			}
		}
	}

	@Test
	public void testIsSubstringOf() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Method method = AbstractPosQGramFilterDP.class.getDeclaredMethod( "isSubstringOf", int[].class, int.class, int[].class );
		method.setAccessible( true );
		Random random = new Random(0);
		int nTest = 1000;
		int nv = 3; // size of vocabulary
		for ( int itr=0; itr<nTest; itr++ ) {
//			System.out.println( "itr: "+itr );
			int patLen = random.nextInt( 5 ) + 1;
			int seqLen = random.nextInt( 15 ) + 26;
			int[] pat = random.ints( patLen, 0, nv ).toArray();
			int[] seq = random.ints( seqLen, 0, nv ).toArray();
			int[] failure = target.prepare_search( pat );
//			System.out.println( Arrays.toString( pat ) );
//			System.out.println( Arrays.toString( seq ) );
			
			
			for ( int i=1; i<=patLen; ++i ) { // search pat[0:i], right exclusive
				// true answer
				IntOpenHashSet posSet0 = new IntOpenHashSet(); // set of finishing indices (exclusive)
				for ( int j=0; j<seqLen; ++j ) {
					boolean match = true;
					int k;
					for ( k=0; k<i && j+k<seqLen && match; ++k ) match &= pat[k] == seq[j+k];
					if ( k == i && match ) posSet0.add( j+i );
				}

				// answer by AbstractPosQGramFilterDP.isSubstringOf; may be failed if a pattern appears multiple times in a sequence.
//				int ansByMethod = (int)(method.invoke( target, pat, i, seq ));
//				System.out.println( "ansByMethod: "+ansByMethod );
				
				// answer by this.isPrefixSubArrayOf
				IntArrayList posSet1 = target.isPrefixSubArrayOf( pat, failure, i, seq );
				assertTrue( posSet0.equals( new IntOpenHashSet(posSet1) ) );
			}
		}
	}
}
