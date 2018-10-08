package snu.kdd.synonym.synonymRev;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;

import org.junit.Test;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntCollection;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;

public class DataCharacteristicsTest {
	
	int K = 1;

	@Test
	public void testPrefixTokenEntropy() throws Exception {
		String[] datasetList = {"AOL", "SPROT", "USPS", "SYN_100K"};
//		String[] datasetList = {"SPROT",};
		PrintWriter writer = new PrintWriter( new BufferedWriter( new FileWriter( "tmp/ValidatorSpeedTest.txt" ) ) );
		int size;
		for ( String dataset : datasetList ) {
			if ( dataset.equals( "SPROT" ) ) size = 466158;
			else size = 1000000;
			Query query = TestUtils.getTestQuery( dataset, size );
			
			Int2IntOpenHashMap counter = new Int2IntOpenHashMap();
			counter.defaultReturnValue( 0 );
			for ( Record rec : query.indexedSet.recordList ) {
				for ( int k=0; k<Math.min( K, rec.size() ); ++k ) counter.addTo( rec.getTokensArray()[k], 1 );
			}
			
			IntCollection countSet = counter.values();
			int countSum = countSet.stream().reduce( (a,b)->a+b ).get();
			
			double entropy = -countSet.stream().mapToDouble( Integer::doubleValue ).map( x -> x/countSum ).map( p -> p * Math.log( p ) ).sum();
			System.out.println( dataset+"\t"+entropy );
		}

	}

}
