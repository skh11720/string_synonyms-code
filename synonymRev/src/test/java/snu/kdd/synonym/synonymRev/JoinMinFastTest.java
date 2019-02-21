package snu.kdd.synonym.synonymRev;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

import org.apache.commons.cli.ParseException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import snu.kdd.synonym.synonymRev.algorithm.JoinMinFast;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.tools.StatContainer;

public class JoinMinFastTest {
	
	static final int K = 2;
	static final int q = 2;

	@Test
	public void test() throws IOException, ParseException, org.json.simple.parser.ParseException {
		PrintWriter writer = new PrintWriter( new BufferedWriter( new FileWriter( "tmp/JoinMinFastTest.txt" ) ) );
		String[] datasetList = {"AOL", "SPROT", "USPS", "SYN_100K"};
		int size = 100000;
		double[] sampleRatioList = {0.01, 0.02, 0.05, 0.1, 0.2, 0.5, 1.0};

		for ( String dataset : datasetList ) {
			for ( double sampleRatio : sampleRatioList ) {
				Query query = TestUtils.getTestQuery( dataset, size );
				JoinMinFast alg = new JoinMinFast( query, String.format( "-K %d -qSize %d -sample %.4f", K, q, sampleRatio ).split( " " ) );
				alg.run();
				JSONParser jparser = new JSONParser();
				alg.getStat().printResult();
				JSONObject jobj = (JSONObject) jparser.parse( "{"+alg.getStat().toJson()+"}" );
//				String result = dataset+"\t"+size+"\tJoinMH\t"+K+"\t"+q+"\t"+sampleRatio;
				String strPosDist = (String) jobj.get( "posDistribution" );
				System.out.println( strPosDist );
		//		{0=>3971, 2=>1492, 6=>108, 4=>836, 12=>4, 13=>4, 8=>35, 9=>9, 11=>6, 10=>5, 1=>2391, 3=>804, 7=>61, 5=>268, 16=>1, 19=>1, 21=>4}
				
				StringBuffer sb = new StringBuffer( strPosDist );
				sb.deleteCharAt( 0 );
				sb.deleteCharAt( sb.length()-1 );
				String[] tokenList = sb.toString().split( ", " );
				Int2IntOpenHashMap hist = new Int2IntOpenHashMap();
				for ( int i=0; i<tokenList.length; ++i ) {
					String token = tokenList[i];
					int[] kvPair = Arrays.stream( token.split( "=>" ) ).mapToInt( Integer::parseInt ).toArray();
					hist.put( kvPair[0], kvPair[1] );
				}
				int keyMax = hist.keySet().stream().max( Integer::compare ).get();
				writer.print( dataset+"\t"+size+"\t"+sampleRatio );
				for ( int key=0; key<=keyMax; ++key ) {
					if ( hist.keySet().contains( key ) ) writer.print( "\t"+hist.get( key ) );
					else writer.print( "\t0" );
				}
				writer.println();
			}
		}


		writer.flush(); writer.close();
	}
}
