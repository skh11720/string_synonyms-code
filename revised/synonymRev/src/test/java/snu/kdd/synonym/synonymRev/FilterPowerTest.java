package snu.kdd.synonym.synonymRev;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.commons.cli.ParseException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;

import snu.kdd.synonym.synonymRev.algorithm.JoinMH;
import snu.kdd.synonym.synonymRev.algorithm.JoinMin;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.index.JoinMHIndex;
import snu.kdd.synonym.synonymRev.index.JoinMinIndex;
import snu.kdd.synonym.synonymRev.tools.StatContainer;

public class FilterPowerTest {

	static final int K = 2;
	static final int q = 2;

	@Test
	public void test() throws IOException, ParseException, org.json.simple.parser.ParseException {
		String[] datasetList = {"AOL", "SPROT", "USPS", "SYN_100K"};
		String[] attrList = {"Val_Comparisons", "Val_Length_filtered", "Val_PQGram_filtered"};
		int size = 100000;
		
		PrintWriter writer = new PrintWriter( new BufferedWriter( new FileWriter( "tmp/FilterPowerTest_"+size+".txt" ) ) );

		for ( String dataset : datasetList ) {
			for ( boolean useLF : new boolean[] {true, false} ) {
				for ( boolean usePQF : new boolean[] {true, false} ) {
					// JoinMH
					{
						JoinMHIndex.useLF = useLF;
						JoinMHIndex.usePQF = usePQF;
						Query query = TestUtils.getTestQuery( dataset, size );
						StatContainer stat_joinmh = new StatContainer();
						JoinMH joinmh = new JoinMH( query, stat_joinmh );
						joinmh.run( query, ("-K "+K+" -qSize "+q).split( " " ) );
						System.out.println( stat_joinmh.toJson() );
						JSONParser jparser = new JSONParser();
						JSONObject jobj = (JSONObject) jparser.parse( "{"+stat_joinmh.toJson()+"}" );
						String result = dataset+"\t"+size+"\tJoinMH\t"+K+"\t"+q+"\t"+useLF+"\t"+usePQF;
						for ( String key : attrList ) result += "\t"+jobj.get( key );
						writer.println(result);
					}
					
					// JoinMin
					{
						JoinMinIndex.useLF = useLF;
						JoinMinIndex.usePQF = usePQF;
						Query query = TestUtils.getTestQuery( dataset, size );
						StatContainer stat_joinmh = new StatContainer();
						JoinMin joinmh = new JoinMin( query, stat_joinmh );
						joinmh.run( query, ("-K "+K+" -qSize "+q).split( " " ) );
						System.out.println( stat_joinmh.toJson() );
						JSONParser jparser = new JSONParser();
						JSONObject jobj = (JSONObject) jparser.parse( "{"+stat_joinmh.toJson()+"}" );
						String result = dataset+"\t"+size+"\tJoinMin\t"+K+"\t"+q+"\t"+useLF+"\t"+usePQF;
						for ( String key : attrList ) result += "\t"+jobj.get( key );
						writer.println(result);
					}
				}
			}
		}
		
		writer.flush(); writer.close();
	}

}
