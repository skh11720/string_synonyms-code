package snu.kdd.synonym.synonymRev;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.ArrayUtils;
import org.junit.Ignore;
import org.junit.Test;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;

public class TokenFrequencyTest {
	String[] datasetList = {"AOL", "SPROT", "USPS", "SYN_100K"};
	int[] sizeList = {1000000, 466158, 1000000, 1000000};

	@Test
	public void test() throws IOException {
		PrintStream ps = new PrintStream( new BufferedOutputStream( new FileOutputStream( "tmp/TokenFrequencyTest.txt" ) ) );
		int[][] sortedCountList = new int[4][];
		for ( int i=0; i<datasetList.length; ++i ) {
			String dataset = datasetList[i];
			int size = sizeList[i];
			System.out.println( dataset+'\t'+size );
			Query query = TestUtils.getTestQuery(dataset, size);
			
//			Int2IntOpenHashMap counterS = countTokensInRecords( query.searchedSet.recordList );
			Int2IntOpenHashMap counterT = countTokensInRecords( query.indexedSet.recordList );
//			System.out.println( "nTokens in S: "+counterS.size() );
			System.out.println( "nTokens in T: "+counterT.size() );
			sortedCountList[i] = counterT.values().stream().sorted().mapToInt( Integer::intValue ).toArray();
		}
		
		int m = Arrays.stream( sortedCountList ).map( Array::getLength ).max( Integer::compare ).get().intValue();
		ps.println( "AOL\tSPROT\tUSPS\tSYN_100K" );
		for ( int i=0; i<m; ++i ) {
			for ( int j=0; j<4; ++ j ) {
				if ( i < sortedCountList[j].length ) ps.print( sortedCountList[j][i]+"\t" );
				else ps.print( "-\t" );
				if ( i < 5 ) {
					System.out.print( sortedCountList[j][i]+"\t" );
					System.out.println( "i: "+i );
				}
			}
			ps.println();
			System.out.println();
		}
		ps.flush(); ps.close();
	}
	
	public void test2() {
		int[] arr = {6, 12, 2, 3, 8};
		IntOpenHashSet set = new IntOpenHashSet( arr );
		int[] a = set.stream().sorted().mapToInt( Integer::intValue ).toArray();
		System.out.println( Arrays.toString( a ) );
	}
	
	
	
	
	private Int2IntOpenHashMap countTokensInRecords( List<Record> recordList ) {
		Int2IntOpenHashMap counter = new Int2IntOpenHashMap();
		counter.defaultReturnValue( 0 );
		for ( Record rec : recordList ) {
			for ( int token : rec.getTokens() ) counter.addTo( token, 1 );
		}
		return counter;
	}
}
