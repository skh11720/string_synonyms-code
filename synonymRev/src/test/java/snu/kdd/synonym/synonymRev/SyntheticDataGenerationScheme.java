package snu.kdd.synonym.synonymRev;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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

	static final boolean selfJoin = true;
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
		String dataHome = (String)jobj.get("data_home");
		String syn_id = (String)jobj.get("ID");
		String syn_suffix = (String)jobj.get("ID_suffix");
		String dataHomeTmpl = dataHome+syn_id+syn_suffix;
		String rulePathTmpl = (String)jobj.get("rule_path");
		String dataPathTmpl = (String)jobj.get("data_path");
		
		
		NewGenerator gen = new NewGenerator( dataHomeTmpl, rulePathTmpl, dataPathTmpl, 0L );
		System.out.println("Number of datasets to be generated: "+arr_nToken.length*arr_skewD.length*arr_nRule.length*arr_ANR.length*arr_SEL.length);
		
		for ( int nToken : arr_nToken ) {
			for ( double skewD : arr_skewD ) {
				System.out.println("Build dict with nToken="+nToken+", skewD="+skewD);
				gen.buildDict(nToken, skewD);
				for ( int nRule : arr_nRule ) {
					System.out.println("Generate "+nRule+" rules");
					gen.generateRules(nRule);
					for ( int ANR : arr_ANR ) {
						for ( double SEL : arr_SEL ) {
							System.out.println("Generate "+nRecordMax+" records with ANR="+ANR+" and SEL="+SEL);
							String recordPath = gen.generateRecords(nRecordMax, ANR, SEL);
							String recordPathTemplate = recordPath.replaceFirst("_N"+nRecordMax, "_N%s");
							for ( int nRecord : arr_nRecord ) {
								if ( nRecord == nRecordMax ) continue;
								BufferedReader reader = new BufferedReader( new FileReader( recordPath ) );
								BufferedWriter writer = new BufferedWriter( new FileWriter( String.format( recordPathTemplate, nRecord ) ) );
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
