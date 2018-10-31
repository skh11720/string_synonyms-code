package snu.kdd.synonym.synonymRev;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import snu.kdd.synonym.synonymRev.algorithm.delta.DeltaValidatorTopDown;
import snu.kdd.synonym.synonymRev.algorithm.delta.SampleEstimateDelta;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.tools.StatContainer;

public class SampleEstimateDeltaTest {
	
	private static Query query;
	private final int deltaMax = 2;
	private final int qgramSize = 2;
	private final int indexK = 2;
	
	@BeforeClass
	public static void initialize() throws IOException {
		query = TestUtils.getTestQuery( "AOL", 63095 );
	}
	
	@Test
	public void test() throws IOException {
		inspect( 10, "estimationTest_10", false );
		inspect( 10, "estimationTest_10_stratified", true );
		inspect( 20, "estimationTest_20", false );
		inspect( 20, "estimationTest_20_stratified", true );
	}

	public void inspect( int N, String outputName, boolean stratified ) throws IOException {
		BufferedWriter bw = new BufferedWriter( new FileWriter( "tmp/"+outputName+".txt" ) );
		DeltaValidatorTopDown checker = new DeltaValidatorTopDown( deltaMax );
		StatContainer stat = new StatContainer();
//		int N = 20;

		String[][] keyList = new String[3][];
		keyList[0] = new String[]{"Naive_Coeff_1", "Naive_Coeff_2", "Naive_Coeff_3", "Naive_Term_1", "Naive_Term_2", "Naive_Term_3", "Naive_Est_Time", "Naive_Join_Time"};
		keyList[1] = new String[]{"MH_Coeff_1", "MH_Coeff_2", "MH_Coeff_3", "MH_Term_1", "MH_Term_2", "MH_Term_3", "MH_Est_Time", "MH_Join_Time"};
		keyList[2] = new String[]{"Min_Coeff_1", "Min_Coeff_2", "Min_Coeff_3", "Min_Term_1", "Min_Term_2", "Min_Term_3", "Min_Est_Time", "Min_Join_Time"};
		
		bw.write( "ratio\t" );
		for ( int j=0; j<3; ++j ) {
			for ( String key : keyList[j] ) bw.write( key+"\t" );
		}
		bw.write( "\n" );


		Object2DoubleMap<String>[][] output = new Object2DoubleMap[N][];
		for ( int i=0; i<N; ++i ) {
			double sampleratio = 0.01*(i+1);
			bw.write( String.format( "%.2f", sampleratio )+"\t" );
			SampleEstimateDelta est = new SampleEstimateDelta( query, deltaMax, sampleratio, query.selfJoin, stratified );
			output[i] = new Object2DoubleMap[3];
			output[i][0] = est.estimateJoinNaiveDelta( stat );
			output[i][1] = est.estimateJoinMHDelta( stat, checker, indexK, qgramSize );
			output[i][2] = est.estimateJoinMinDelta( stat, checker, indexK, qgramSize );
			
			for ( int j=0; j<3; ++j ) {
				for ( String key : keyList[j] ) bw.write( output[i][j].get( key )+"\t" );
			}
			bw.write( "\n" );
			bw.flush();
		}
		bw.write( "\n" );
		
		for ( int j=0; j<3; ++j ) {
			for ( int i0=0; i0<N; ++i0 ) {
				double coeff1 = output[i0][j].get( keyList[j][0] );
				double coeff2 = output[i0][j].get( keyList[j][1] );
				double coeff3 = output[i0][j].get( keyList[j][2] );
//				System.out.println( coeff1+"\t"+coeff2+"\t"+coeff3 );
				
				// est time by the coefficients
				double[] estTime = new double[N];
				for ( int i1=0; i1<N; ++i1 ) {
					double term1 = output[i1][j].get( keyList[j][3] );
					double term2 = output[i1][j].get( keyList[j][4] );
					double term3 = output[i1][j].get( keyList[j][5] );
					estTime[i1] = coeff1 * term1 + coeff2 * term2 + coeff3 * term3;
					bw.write( estTime[i1]+"\t" );
//				System.out.println( term1+"\t"+term2+"\t"+term3+"\t"+estTime[i1] );
				}
				bw.write( "\t" );
				
				// est time - join time
				for ( int i1=0; i1<N; ++i1 ) {
					bw.write( (estTime[i1] - output[i1][j].get( keyList[j][7] ) )+"\t" );
				}
				bw.write( "\n" );
			}
			bw.write( "\n" );
		}
		
		bw.flush(); bw.close();
	}
}
