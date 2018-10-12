package snu.kdd.synonym.synonymRev;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;

import org.junit.Test;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.DEBUG;

public class DataCharacteristicsTest {
	
	int K = 1;

	@Test
	public void testPrefixTokenEntropy() throws Exception {
//		String[] datasetList = {"AOL", "SPROT", "USPS", "SYN_100K", "SYN_D_0.2"};
		String[] datasetList = {"SYN_D_3.0",};
		PrintWriter writer = new PrintWriter( new BufferedWriter( new FileWriter( "tmp/DataCharacteristicsTest.txt" ) ) );
		int size;
		for ( String dataset : datasetList ) {
			if ( dataset.equals( "SPROT" ) ) size = 466158;
			else size = 1000000;
			Query query = TestUtils.getTestQuery( dataset, size );
			
			int nTokens = countNumTokens( query );
			double avgNumRules = getAvgNumApplicableRules( query );
			String numARulesDistInfo = getNumARulesDistInfo( query );
			String numEstTransDistInfo = getEstNumTransDistInfo( query );
			writer.println(dataset+"\t"+nTokens+"\t"+avgNumRules+"\t"+numARulesDistInfo+"\t"+numEstTransDistInfo);
			System.out.println(dataset+"\t"+nTokens+"\t"+avgNumRules+"\t"+numARulesDistInfo+"\t"+numEstTransDistInfo);
		}
		
		writer.flush();
		writer.close();
	}

	int countNumTokens( Query query ) {
		IntOpenHashSet tokenSet = new IntOpenHashSet();
		for ( Record rec : query.searchedSet.recordList ) tokenSet.addAll( rec.getTokens() );
		if ( !query.selfJoin ) for ( Record rec : query.indexedSet.recordList ) tokenSet.addAll( rec.getTokens() );
		return tokenSet.size();
	}
	
	double getAvgNumApplicableRules( Query query ) {
		double sumNumRules = 0;
		for ( Record rec : query.searchedSet.recordList ) sumNumRules += rec.getNumApplicableRules();
		return sumNumRules/query.searchedSet.size();
	}
	
	Int2IntOpenHashMap getHistAvgNumApplicableRules( Query query ) {
		Int2IntOpenHashMap hist = new Int2IntOpenHashMap();
		hist.defaultReturnValue( 0 );
		for ( Record rec : query.searchedSet.recordList ) {
			if ( rec.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) continue;
			hist.addTo( rec.getNumApplicableRules(), 1 );
		}
		return hist;
	}
	
	String getNumARulesDistInfo( Query query ) {
		IntArrayList naList = new IntArrayList();
		for ( Record rec : query.searchedSet.recordList ) {
			if ( rec.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) continue;
			naList.add( rec.getNumApplicableRules() );
		}
		naList.sort( Integer::compare );
		// [10, 20, ..., 90, 91, 92, ..., 100]
		String output = "";
		int[] pos_arr = {10, 20, 30, 40, 50, 60, 70, 80, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100};
		for ( int pos : pos_arr ) {
			output += naList.getInt( (int)(pos*naList.size()/100.0)-1 )+" ";
		}
		return output;
	}
	
	String getEstNumTransDistInfo( Query query ) {
		LongArrayList estNumTransList = new LongArrayList();
		for ( Record rec : query.searchedSet.recordList ) {
			if ( rec.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) continue;
			estNumTransList.add( rec.getEstNumTransformed() );
		}
		estNumTransList.sort( Long::compare );
		// [10, 20, ..., 90, 91, 92, ..., 100]
		String output = "";
		int[] pos_arr = {10, 20, 30, 40, 50, 60, 70, 80, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100};
		for ( int pos : pos_arr ) {
			output += estNumTransList.getLong( (int)(pos*estNumTransList.size()/100.0)-1 )+" ";
		}
		return output;
	}
	
	double getEntropyOfTableT( Query query ) {
		Int2IntOpenHashMap counter = new Int2IntOpenHashMap();
		counter.defaultReturnValue( 0 );
		for ( Record rec : query.indexedSet.recordList ) {
			for ( int k=0; k<Math.min( K, rec.size() ); ++k ) counter.addTo( rec.getTokensArray()[k], 1 );
		}
		
		IntCollection countSet = counter.values();
		int countSum = countSet.stream().reduce( (a,b)->a+b ).get();
		
		double entropy = -countSet.stream().mapToDouble( Integer::doubleValue ).map( x -> x/countSum ).map( p -> p * Math.log( p ) ).sum();
		return entropy;
	}
}
