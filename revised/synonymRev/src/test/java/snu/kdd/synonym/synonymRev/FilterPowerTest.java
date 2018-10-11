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
import snu.kdd.synonym.synonymRev.algorithm.JoinMinFast;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.index.JoinMHIndex;
import snu.kdd.synonym.synonymRev.index.JoinMinFastIndex;
import snu.kdd.synonym.synonymRev.index.JoinMinIndex;
import snu.kdd.synonym.synonymRev.tools.StatContainer;

public class FilterPowerTest {

	static final int K = 1;
	static final int q = 2;

	@Test
	public void test() throws IOException, ParseException, org.json.simple.parser.ParseException {
		String[] datasetList = {"AOL", "SPROT", "USPS"};
		String[] attrList = {"Val_Comparisons", "Val_Length_filtered", "Val_PQGram_filtered"};
		int size = 1000;
		
		PrintWriter writer = new PrintWriter( new BufferedWriter( new FileWriter( "tmp/FilterPowerTest_"+size+".txt" ) ) );
		
		boolean[][] settings = { // LF, PQF, STPQ
				{false, false, false}, // use nothing
				{true, false, false}, // use LF only
				{false, true, false}, // use PQF only, use TPQ
				{false, true, true}, // use PQF only, use STPQ
				{true, true, false}, // use both, use TPQ
				{true, true, true}, // use both, use STPQ
		};

		for ( String dataset : datasetList ) {
			for ( boolean[] setting : settings ) {
				boolean useLF = setting[0];
				boolean usePQF = setting[1];
				boolean useSTPQ = setting[2];

				// JoinMH
				{
					Query query = TestUtils.getTestQuery( dataset, size );
					StatContainer stat_joinmh = new StatContainer();
					JoinMH joinmh = new JoinMH( query, stat_joinmh );
					joinmh.run( query, ("-K "+K+" -qSize "+q+" -useLF "+useLF+" -usePQF "+usePQF+" -useSTPQ "+useSTPQ).split( " " ) );
					System.out.println( stat_joinmh.toJson() );
					JSONParser jparser = new JSONParser();
					JSONObject jobj = (JSONObject) jparser.parse( "{"+stat_joinmh.toJson()+"}" );
					String result = dataset+"\t"+size+"\tJoinMH\t"+K+"\t"+q+"\t"+useLF+"\t"+usePQF+"\t"+useSTPQ;
					for ( String key : attrList ) result += "\t"+jobj.get( key );
					writer.println(result);
				}
				
				// JoinMin
				{
					Query query = TestUtils.getTestQuery( dataset, size );
					StatContainer stat_joinmh = new StatContainer();
					JoinMin joinmh = new JoinMin( query, stat_joinmh );
					joinmh.run( query, ("-K "+K+" -qSize "+q+" -useLF "+useLF+" -usePQF "+usePQF+" -useSTPQ "+useSTPQ).split( " " ) );
					System.out.println( stat_joinmh.toJson() );
					JSONParser jparser = new JSONParser();
					JSONObject jobj = (JSONObject) jparser.parse( "{"+stat_joinmh.toJson()+"}" );
					String result = dataset+"\t"+size+"\tJoinMin\t"+K+"\t"+q+"\t"+useLF+"\t"+usePQF+"\t"+useSTPQ;
					for ( String key : attrList ) result += "\t"+jobj.get( key );
					writer.println(result);
				}

				// JoinMinFast
				{
					Query query = TestUtils.getTestQuery( dataset, size );
					StatContainer stat_joinmhfast = new StatContainer();
					JoinMinFast joinmhfast = new JoinMinFast( query, stat_joinmhfast );
					joinmhfast.run( query, ("-K "+K+" -qSize "+q+" -sample 0.01 -useLF "+useLF+" -usePQF "+usePQF+" -useSTPQ "+useSTPQ).split( " " ) );
					System.out.println( stat_joinmhfast.toJson() );
					JSONParser jparser = new JSONParser();
					JSONObject jobj = (JSONObject) jparser.parse( "{"+stat_joinmhfast.toJson()+"}" );
					String result = dataset+"\t"+size+"\tJoinMinFast\t"+K+"\t"+q+"\t"+useLF+"\t"+usePQF+"\t"+useSTPQ;
					for ( String key : attrList ) result += "\t"+jobj.get( key );
					writer.println(result);
				}
			}
		}
		
		writer.flush(); writer.close();
	}

}
