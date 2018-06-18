package snu.kdd.synonym.synonymRev;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import org.junit.Ignore;
import org.junit.Test;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import snu.kdd.synonym.synonymRev.data.ACAutomataR;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.Util;

public class TestUtils {

	public static Query getTestQuery() throws IOException {
		String osName = System.getProperty( "os.name" );
		Query query = null;
		if ( osName.startsWith( "Windows" ) ) {
			final String dataOnePath = "D:\\ghsong\\data\\aol\\splitted\\aol_10000_data.txt";
			final String dataTwoPath = "D:\\ghsong\\data\\aol\\splitted\\aol_10000_data.txt";
			final String rulePath = "D:\\ghsong\\data\\wordnet\\rules.noun";
			final String outputPath = "output";
			final Boolean oneSideJoin = true;
			query = new Query(rulePath, dataOnePath, dataTwoPath, oneSideJoin, outputPath);
		}
		else if ( osName.startsWith( "Linux" ) ) {
			final String dataOnePath = "run/data_store/aol/splitted/aol_1000_data.txt";
			final String dataTwoPath = "run/data_store/aol/splitted/aol_1000_data.txt";
			final String rulePath = "run/data_store/wordnet/rules.noun";
			final String outputPath = "output";
			final Boolean oneSideJoin = true;
			query = new Query(rulePath, dataOnePath, dataTwoPath, oneSideJoin, outputPath);
		}

//		record = query.searchedSet.getRecord( 0 );
		final ACAutomataR automata = new ACAutomataR( query.ruleSet.get());
		for ( Record record : query.searchedSet.recordList ) {
			record.preprocessRules( automata );
			record.preprocessSuffixApplicableRules();
			record.preprocessTransformLength();
			record.preprocessTransformLength();
			record.preprocessEstimatedRecords();
		}
		return query;
	}
	
	@Ignore
	public void testLCS() {
		Random random = new Random(0);
		final int nTest = 1000;
		final int lz_max = 10;
		final int l_max = 10;
		final int nV = 10;
		
		for ( int itr=0; itr<nTest; ++itr ) {
			int error = -1;
			int lz = random.nextInt( lz_max+1 );
			int lx = lz + random.nextInt( l_max+1 );
			int ly = lz + random.nextInt( l_max+1 );
			int[] z = new int[lz];
			int[] x = new int[lx];
			int[] y = new int[ly];
			for ( int i=0; i<lz; ++i ) z[i] = random.nextInt( nV );
			IntOpenHashSet idxSet = new IntOpenHashSet();
			idxSet.clear();
			while ( idxSet.size() < lz ) idxSet.add( random.nextInt( lx ) );
			for ( int i=0, k=0; i<lx; ++i ) {
				if ( idxSet.contains( i ) ) x[i] = z[k++];
				else x[i] = error--;
			}
			idxSet.clear();
			while ( idxSet.size() < lz ) idxSet.add( random.nextInt( ly ) );
			for ( int j=0, k=0; j<ly; ++j ) {
				if ( idxSet.contains( j ) ) y[j] = z[k++];
				else y[j] = error--;
			}
			System.out.println( "x: "+Arrays.toString(x) );
			System.out.println( "y: "+Arrays.toString(y) );
			System.out.println( "z: "+Arrays.toString(z) );
			assertEquals( lz, Util.lcs( x, y ) );
		}
	}
	
	// 700 ms
	@Ignore
	public void testGetCombinations() {
		final int nTest = 1000;
		final int n_max = 10;

		for ( int itr=0; itr<nTest; ++itr ) {
			for ( int n=0; n<=n_max; ++n ) {
				for ( int k=0; k<=n; ++k ) {
					Util.getCombinations( n, k ).size();
				}
			}
		}
	}
}
