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
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.DEBUG;

public class DataCharacteristicsTest {
	
	int K = 1;

	@Test
	public void testPrefixTokenEntropy() throws Exception {
//		String[] datasetList = {"AOL", "SPROT", "USPS", "SYN_100K",
//		 "SYN_D_2.0", "SYN_D_2.5", "SYN_D_3.0", "SYN_D_3.5", "SYN_D_4.0", "SYN_D_4.5", "SYN_D_5.0", "SYN_D_5.5", "SYN_D_6.0", "SYN_K_0.0", "SYN_K_0.2", "SYN_K_0.4", "SYN_K_0.6", "SYN_K_0.8", "SYN_K_1.0", "SYN_L_11", "SYN_L_3", "SYN_L_5", "SYN_L_7", "SYN_L_9", "SYN_S_0.0", "SYN_S_-6.0", "SYN_S_-5.0", "SYN_S_-4.0", "SYN_S_-3.0"};
		String[] datasetList = {"SPROT", "SYN_D_3.0"};
		PrintWriter writer = new PrintWriter( new BufferedWriter( new FileWriter( "tmp/DataCharacteristicsTest.txt" ) ) );
		int[] pos_arr = {10, 20, 30, 40, 50, 60, 70, 80, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100};
		int size;
		Object2ObjectOpenHashMap<String, IntArrayList> narMap = new Object2ObjectOpenHashMap<>();
		Object2ObjectOpenHashMap<String, LongArrayList> netMap = new Object2ObjectOpenHashMap<>();

		for ( String dataset : datasetList ) {
			if ( dataset.equals( "SPROT" ) ) size = 466158;
			else size = 1000000;
			Query query = TestUtils.getTestQuery( dataset, size );
			
			int nTokens = countNumTokens( query );
			double avgNumRules = getAvgNumApplicableRules( query );

			IntArrayList naList = getNumARulesDistInfo( query );
			narMap.put( dataset, naList );
			String numARulesDistInfo = "";
			for ( int pos : pos_arr ) {
				numARulesDistInfo += naList.getInt( (int)(pos*naList.size()/100.0)-1 )+" ";
			}

			LongArrayList estNumTransList = getEstNumTransDistInfo( query );
			netMap.put( dataset, estNumTransList );
			String numEstTransDistInfo = "";
			for ( int pos : pos_arr ) {
				numEstTransDistInfo += estNumTransList.getLong( (int)(pos*estNumTransList.size()/100.0)-1 )+" ";
			}
			writer.println(dataset+"\t"+nTokens+"\t"+avgNumRules+"\t"+numARulesDistInfo+"\t"+numEstTransDistInfo);
			System.out.println(dataset+"\t"+nTokens+"\t"+avgNumRules+"\t"+numARulesDistInfo+"\t"+numEstTransDistInfo);
		}
		writer.flush();
		writer.close();
		
		PrintWriter ps_dist = new PrintWriter( new BufferedWriter( new FileWriter( "tmp/DataDistributions.txt" ) ) );
		for ( String dataset : datasetList ) ps_dist.print( dataset+"_NAR\t"+dataset+"_NET\t" );
		ps_dist.println();
		for ( int i=0; i<1000000; ++i ) {
			for ( String dataset : datasetList ) {
				if ( i < narMap.get( dataset ).size() ) ps_dist.print( narMap.get( dataset ).getInt( i )+"\t" );
				else ps_dist.print( "\t" );
				if ( i < netMap.get( dataset ).size() ) ps_dist.print( netMap.get( dataset ).getLong( i )+"\t" );
				else ps_dist.print( "\t" );
			}
			ps_dist.println();
		}
		ps_dist.flush();
		ps_dist.close();
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
	
	IntArrayList getNumARulesDistInfo( Query query ) {
		IntArrayList naList = new IntArrayList();
		for ( Record rec : query.searchedSet.recordList ) {
			if ( rec.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) continue;
			naList.add( rec.getNumApplicableRules() );
		}
		naList.sort( Integer::compare );
		// [10, 20, ..., 90, 91, 92, ..., 100]
		return naList;
	}
	
	LongArrayList getEstNumTransDistInfo( Query query ) {
		LongArrayList estNumTransList = new LongArrayList();
		for ( Record rec : query.searchedSet.recordList ) {
			if ( rec.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) continue;
			estNumTransList.add( rec.getEstNumTransformed() );
		}
		estNumTransList.sort( Long::compare );
		// [10, 20, ..., 90, 91, 92, ..., 100]
		return estNumTransList;
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
