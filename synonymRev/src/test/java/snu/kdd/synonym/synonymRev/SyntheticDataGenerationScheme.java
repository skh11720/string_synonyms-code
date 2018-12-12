package snu.kdd.synonym.synonymRev;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Ignore;
import org.junit.Test;

import snu.kdd.synonym.synonymRev.data.NewGenerator;

/*
 * Use the NewGenerator.
 */

public class SyntheticDataGenerationScheme {

//	static final int[] arr_nRecord = {10000, 31622, 100000};
////	static final int[] arr_nRecord = {10000, 31622, 100000, 316227, 1_000_000, 3_162_277, 10_000_000};
//	static final int nRecordMax = arr_nRecord[arr_nRecord.length-1];
//
////	static final int[] arr_nRule = {10000, 30000, 100000};
//	static final int[] arr_nRule = {100000};
//	static final int nRuleMax = arr_nRule[arr_nRule.length-1];
//
//	static final int[] arr_nToken = {100000, 500000};
//
//	static final int[] arr_ANR = {2, 4, 6, 8, 10};
//	static final double[] arr_skewD = {0.0, 1.0};
//	static final double[] arr_SEL = {0, 1e-1};
//
	static final boolean selfJoin = true;
//	static final String syn_id = "SYN07";
	static final String parentPath = "run/data_store/";
//	static final String parentPath = "./";
	
	
	@Test
	public void generate() throws IOException, ParseException {
		JSONParser parser = new JSONParser();
		JSONObject jobj = (JSONObject)parser.parse( new FileReader("SYN_catalog.json"));
		JSONArray jarr;
		
		jarr = (JSONArray) jobj.get("nRecord");
		int[] arr_nRecord = new int[jarr.size()];
		for ( int i=0; i<jarr.size(); ++i ) arr_nRecord[i] = ((Long)jarr.get(i)).intValue();

		jarr = (JSONArray) jobj.get("nRule");
		int[] arr_nRule= new int[jarr.size()];
		for ( int i=0; i<jarr.size(); ++i ) arr_nRule[i] = ((Long)jarr.get(i)).intValue();

		jarr = (JSONArray) jobj.get("nToken");
		int[] arr_nToken= new int[jarr.size()];
		for ( int i=0; i<jarr.size(); ++i ) arr_nToken[i] = ((Long)jarr.get(i)).intValue();

		jarr = (JSONArray) jobj.get("ANR");
		int[] arr_ANR= new int[jarr.size()];
		for ( int i=0; i<jarr.size(); ++i ) arr_ANR[i] = ((Long)jarr.get(i)).intValue();

		jarr = (JSONArray) jobj.get("skewD");
		double[] arr_skewD = new double[jarr.size()];
		for ( int i=0; i<jarr.size(); ++i ) arr_skewD[i] = ((Double)jarr.get(i)).doubleValue();

		jarr = (JSONArray) jobj.get("SEL");
		double[] arr_SEL= new double[jarr.size()];
		for ( int i=0; i<jarr.size(); ++i ) arr_SEL[i] = ((Double)jarr.get(i)).doubleValue();
		
		int nRecordMax = arr_nRecord[arr_nRecord.length-1];
		String syn_id = (String)jobj.get("ID");
		
		
		NewGenerator gen = new NewGenerator(0);
		System.out.println("Number of datasets to be generated: "+arr_nToken.length*arr_skewD.length*arr_nRule.length*arr_ANR.length*arr_SEL.length);
		
		for ( int nToken : arr_nToken ) {
			for ( double skewD : arr_skewD ) {
				String datadir = parentPath + syn_id + String.format("_D%d_K%.2f", nToken, skewD );
				System.out.println("Build dict for "+datadir);
				gen.buildDict(nToken, skewD);
				for ( int nRule : arr_nRule ) {
					System.out.println("Generate "+nRule+" rules");
					gen.generateRules(datadir, nRule);
					for ( int ANR : arr_ANR ) {
						for ( double SEL : arr_SEL ) {
							System.out.println("Generate "+nRecordMax+" records with ANR="+ANR+" and SEL="+SEL);
							String recordPath = gen.generateRecords(datadir, nRecordMax, ANR, SEL);
							String[] token = recordPath.split("/");
							String recordPathTemplate = token[token.length-1].replaceFirst("_N"+nRecordMax, "_N%s");
							for ( int nRecord : arr_nRecord ) {
								if ( nRecord == nRecordMax ) continue;
								BufferedReader reader = new BufferedReader( new FileReader( recordPath ) );
								BufferedWriter writer = new BufferedWriter( new FileWriter( datadir+"/data/"+String.format( recordPathTemplate, nRecord ) ) );
								for ( int i=0; i<nRecord; ++i ) writer.write( reader.readLine()+'\n' );
								writer.close(); reader.close();
							}
						}
					}
				}
			}
		}
	}
	
	@Ignore
	public void testReadCatalog() throws FileNotFoundException, IOException, ParseException {
		JSONParser parser = new JSONParser();
		JSONObject jobj = (JSONObject)parser.parse( new FileReader("SYN_catalog.json"));
		JSONArray jarr;
		
		jarr = (JSONArray) jobj.get("nRecord");
		int[] arr_nRecord = new int[jarr.size()];
		for ( int i=0; i<jarr.size(); ++i ) arr_nRecord[i] = ((Long)jarr.get(i)).intValue();

		jarr = (JSONArray) jobj.get("nRule");
		int[] arr_nRule= new int[jarr.size()];
		for ( int i=0; i<jarr.size(); ++i ) arr_nRule[i] = ((Long)jarr.get(i)).intValue();

		jarr = (JSONArray) jobj.get("nToken");
		int[] arr_nToken= new int[jarr.size()];
		for ( int i=0; i<jarr.size(); ++i ) arr_nToken[i] = ((Long)jarr.get(i)).intValue();

		jarr = (JSONArray) jobj.get("ANR");
		int[] arr_ANR= new int[jarr.size()];
		for ( int i=0; i<jarr.size(); ++i ) arr_ANR[i] = ((Long)jarr.get(i)).intValue();

		jarr = (JSONArray) jobj.get("skewD");
		double[] arr_skewD = new double[jarr.size()];
		for ( int i=0; i<jarr.size(); ++i ) arr_skewD[i] = ((Double)jarr.get(i)).doubleValue();

		jarr = (JSONArray) jobj.get("SEL");
		double[] arr_SEL= new double[jarr.size()];
		for ( int i=0; i<jarr.size(); ++i ) arr_SEL[i] = ((Double)jarr.get(i)).doubleValue();
		
		System.out.println(Arrays.toString(arr_nRecord));
		System.out.println(Arrays.toString(arr_nRule));
		System.out.println(Arrays.toString(arr_nToken));
		System.out.println(Arrays.toString(arr_ANR));
		System.out.println(Arrays.toString(arr_skewD));
		System.out.println(Arrays.toString(arr_SEL));
	}
}
