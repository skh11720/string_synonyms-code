package snu.kdd.synonym.synonymRev;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;

import snu.kdd.synonym.synonymRev.data.Generator2;

public class SyntheticDataGenerationScheme2 {

	static final int[] arr_nRec = {10000, 15848, 25118, 39810, 63095, 100000, 158489, 251188, 398107, 630957, 1000000};
//	static final int[] arr_nRule = {10000, 30000, 100000};
//	static final int[] arr_nRec = {1000000};
	static final int[] arr_nRule = {10000, 100000};

	static final int[] arr_DIC = {25000, 50000, 100000, 200000, 400000};
	static final double[] arr_id_DIC = {2.5, 5.0, 10.0, 20.0, 40.0};

	static final double[] arr_KAP = {0, 0.3, 0.6, 0.9};

//	static final int[] arr_LEN = {3, 7, 11, 15};

//	static final double[] arr_SEL = {1e-4, 1e-3, 1e-2, 1e-1};
//	static final int[] arr_id_SEL = {-4, -3, -2, -1};

	static final int[] arr_NAR = {1, 3, 6, 12, 24};
	static final double[] arr_skewR = {0.0, 0.2, 0.4, 0.6, 0.8};
	static final double[] arr_skewP = {0.0, 0.5, 1.0};

	static final long seed0 = 0;
	static final long seed1 = 1;
	static final long seed2 = 2;
	static final int maxLhs = 2;
	static final int maxRhs = 2;
	static final boolean selfJoin = true;
	static final String syn_id = "SYN3";
	static final String parentPath = "D:/ghsong/data/synonyms/"+syn_id;
	
	
	static class Default {
		static final int DIC = arr_DIC[3];
		static final int LEN = 5;
		static final int NAR = arr_NAR[2];
		static final double KAP = 0.5;
		static final double SEL = 1e-2;
		static final double skewR = arr_skewR[3];
		static final double skewP = arr_skewP[1];
	}
	
	static int DIC;
	static int LEN;
	static int NAR;
	static double KAP;
	static double SEL;
	static double skewR;
	static double skewP;
	
	static void resetToDefault() {
		DIC = Default.DIC;
		LEN = Default.LEN;
		NAR = Default.NAR;
		KAP = Default.KAP;
		SEL = Default.SEL;
		skewR = Default.skewR;
		skewP = Default.skewP;
	}
	
	
	
	@Test
	public void test() throws IOException {
		generateVaryingAll();
//		generateVaryingNAR();
//		generateVaryingSkewR();
//		generateVaryingSkewP();
	}
	
	static void generateDataset( int DIC, int LEN, int NAR, double KAP, double SEL, double skewR, double skewP ) throws IOException {
		String outputPath = String.format( "%s_D_%d_L_%d_A_%d_KAP_%.1f_KR_%.1f_KP_%.1f_S_%.1e_%s", syn_id, DIC, LEN, NAR, KAP, skewR, skewP, SEL, selfJoin? "SELF":"NONSELF");
//		System.out.println( DIC+", "+LEN+", "+NAR+", "+KAP+", "+SEL+", "+skewR+", "+skewP+", "+outputPath );
//		System.out.println( outputPath );
		String rulePath = null;
		String recordPath = null;
		Generator2 gen = new Generator2( DIC, KAP, seed0 );
//	public static String generateRules( Generator2 gen, int nToken, int nRule, int avgNMR, double skewZ, double skewR, long seed, String outputPath ) throws IOException {
		for ( int nRule : arr_nRule ) rulePath = Generator2.generateRules( gen, DIC, nRule, NAR, SEL, skewR, seed0, parentPath+"/"+outputPath );
		for ( int nRec : arr_nRec ) recordPath = Generator2.generateRecords( gen, DIC, LEN, NAR, nRec, KAP, skewP, SEL, seed1, parentPath+"/"+outputPath, rulePath );
		if (!selfJoin) for ( int nRec : arr_nRec ) Generator2.generateRecords( gen, DIC, LEN, NAR, nRec, KAP, skewP, SEL, seed1, parentPath+"/"+outputPath, rulePath );
		
//		System.out.println( outputPath );
//		System.out.println( rulePath );
//		System.out.println( recordPath );
		String[] token = recordPath.split( "/" );
		String recordPathTemplate = token[token.length-1].replaceFirst( Integer.toString( arr_nRec[arr_nRec.length-1]), "%s" );
		token = rulePath.split( "/" );
		String rulePathTemplate = token[token.length-1].replaceFirst( Integer.toString( arr_nRule[arr_nRule.length-1] ), "%s" );
		String dname = outputPath;
//		token = dname.split( "_" );
//		dname = token[0] +"_"+token[1]+"_"+token[2]+"_"+Integer.toString( arr_nRule[arr_nRule.length-1] )+"_"+token[3];
		
		System.out.println( 
				"else if ( name.equals( \"" + dname + "\" ) ) {\n" +
				String.format( "\tdataOnePath = prefix + String.format(\"%s\"+sep+\"%s\"+sep+\"data\"+sep+\"%s\", size );\n", syn_id, dname, recordPathTemplate ) +
				String.format( "\tdataTwoPath = prefix + String.format(\"%s\"+sep+\"%s\"+sep+\"data\"+sep+\"%s\", size, selfJoin? 1:2 );\n", syn_id, dname, recordPathTemplate.replace("_1.txt", "_%d.txt") ) +
				String.format( "\trulePath = prefix + String.format(\"%s\"+sep+\"%s\"+sep+\"rule\"+sep+\"%s\", nRule );\n", syn_id, dname, rulePathTemplate ) +
				"}"
				);
	}
	
	static void generateVaryingNAR() throws IOException {
		resetToDefault();
		for ( int i=0; i<arr_NAR.length; ++i ) {
			int NAR = arr_NAR[i];
			generateDataset( DIC, LEN, NAR, KAP, SEL, skewR, skewP );
		}
	}

	static void generateVaryingSkewR() throws IOException {
		resetToDefault();
		for ( int i=0; i<arr_skewR.length; ++i ) {
			double skewR = arr_skewR[i];
			generateDataset( DIC, LEN, NAR, KAP, SEL, skewR, skewP) ;
		}
	}

	static void generateVaryingSkewP() throws IOException {
		resetToDefault();
		for ( int i=0; i<arr_skewP.length; ++i ) {
			double skewP = arr_skewP[i];
			generateDataset( DIC, LEN, NAR, KAP, SEL, skewR, skewP );
		}
	}

	static void generateVaryingAll() throws IOException {
		resetToDefault();
		for ( int i0=0; i0<arr_NAR.length; ++i0 ) {
			int NAR = arr_NAR[i0];
//			for ( int i1=0; i1<arr_skewR.length; ++i1 ) {
//				double skewR = arr_skewR[i1];
				for ( int i2=0; i2<arr_skewP.length; ++i2 ) {
					double skewP = arr_skewP[i2];
//					for ( int i3=0; i3<arr_KAP.length; ++i3 ) { 
//						double KAP = arr_KAP[i3];
						generateDataset( DIC, LEN, NAR, KAP, SEL, skewR, skewP );
//					}
				}
//			}
		}
	}
}
