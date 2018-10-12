package snu.kdd.synonym.synonymRev;

import java.io.IOException;

import org.junit.Test;

import snu.kdd.synonym.synonymRev.data.Generator;

public class SyntheticDataGenerationScheme {

	static final int[] arr_nRec = {10000, 15848, 25118, 39810, 63095, 100000, 158489, 251188, 398107, 630957, 1000000};
	static final int[] arr_nRule = {10000, 30000, 100000, 173200, 300000, 519600, 1000000};
//	static final int[] arr_nRec = {10000, 100000, 1000000};
//	static final int[] arr_nRule = {10000, 100000, 1000000};

	static final int[] arr_DIC = {100, 316, 1000, 3162, 10000, 31622, 100000, 316227, 1000000};
	static final double[] arr_id_DIC = {2, 2.5, 3, 3.5, 4, 4.5, 5, 5.5, 6};

	static final double[] arr_KAP = {0, 0.2, 0.4, 0.6, 0.8, 1.0};

	static final int[] arr_LEN = {3, 5, 7, 9, 11};

	static final double[] arr_SEL = {0, 1e-6, 1e-5, 1e-4, 1e-3};
	static final int[] arr_id_SEL = {0, -6, -5, -4, -3};

	static final long seed0 = 0;
	static final long seed1 = 1;
	static final long seed2 = 2;
	static final int maxLhs = 2;
	static final int maxRhs = 2;
	
	
	static class Default {
//		static final int N_REC = 1_000_000;
//		static final int N_RULE = 100_000;
		static final int DIC = 10_000;
		static final double KAP = 0.6;
		static final int LEN = 5;
		static final double SEL = 1e-5;
	}
	
	
	
	@Test
	public void test() throws IOException {
		generateVaryingDic();
		generateVaryingKAP();
		generateVaryingLEN();
		generateVaryingSEL();
	}
	
	static void generateVaryingDic() throws IOException {
		for ( int i=0; i<arr_DIC.length; ++i ) {
			int DIC = arr_DIC[i];
			double id_DIC = arr_id_DIC[i];
			String outputPath = String.format( "output/SYN_D_%.1f", id_DIC );
			System.out.println( outputPath );
			String rulefile = null;
			for ( int nRule : arr_nRule ) rulefile = Generator.generateRules( DIC, maxLhs, maxRhs, nRule, Default.KAP, seed0, outputPath );
			for ( int nRec : arr_nRec ) Generator.generateRecords( DIC, Default.LEN, nRec, Default.KAP, Default.SEL, seed1, outputPath, rulefile );
			for ( int nRec : arr_nRec ) Generator.generateRecords( DIC, Default.LEN, nRec, Default.KAP, Default.SEL, seed2, outputPath, rulefile );
		}
	}
	
	static void generateVaryingKAP() throws IOException {
		for ( int i=0; i<arr_KAP.length; ++i ) {
			double KAP = arr_KAP[i];
			String outputPath = String.format( "output/SYN_K_%.1f", KAP );
			System.out.println( outputPath );
			String rulefile = null;
			for ( int nRule : arr_nRule ) rulefile = Generator.generateRules( Default.DIC, maxLhs, maxRhs, nRule, KAP, seed0, outputPath );
			for ( int nRec : arr_nRec ) Generator.generateRecords( Default.DIC, Default.LEN, nRec, KAP, Default.SEL, seed1, outputPath, rulefile );
			for ( int nRec : arr_nRec ) Generator.generateRecords( Default.DIC, Default.LEN, nRec, KAP, Default.SEL, seed2, outputPath, rulefile );
		}
	}

	static void generateVaryingLEN() throws IOException {
		for ( int i=0; i<arr_LEN.length; ++i ) {
			int LEN = arr_LEN[i];
			String outputPath = String.format( "output/SYN_L_%d", LEN );
			System.out.println( outputPath );
			String rulefile = null;
			for ( int nRule : arr_nRule ) rulefile = Generator.generateRules( Default.DIC, maxLhs, maxRhs, nRule, Default.KAP, seed0, outputPath );
			for ( int nRec : arr_nRec ) Generator.generateRecords( Default.DIC, LEN, nRec, Default.KAP, Default.SEL, seed1, outputPath, rulefile );
			for ( int nRec : arr_nRec ) Generator.generateRecords( Default.DIC, LEN, nRec, Default.KAP, Default.SEL, seed2, outputPath, rulefile );
		}
	}
	
	static void generateVaryingSEL() throws IOException {
		for ( int i=0; i<arr_SEL.length; ++i ) {
			double SEL = arr_SEL[i];
			double id_SEL = arr_id_SEL[i];
			String outputPath = String.format( "output/SYN_S_%.1f", id_SEL );
			System.out.println( outputPath );
			String rulefile = null;
			for ( int nRule : arr_nRule ) rulefile = Generator.generateRules( Default.DIC, maxLhs, maxRhs, nRule, Default.KAP, seed0, outputPath );
			for ( int nRec : arr_nRec ) Generator.generateRecords( Default.DIC, Default.LEN, nRec, Default.KAP, SEL, seed1, outputPath, rulefile );
			for ( int nRec : arr_nRec ) Generator.generateRecords( Default.DIC, Default.LEN, nRec, Default.KAP, SEL, seed2, outputPath, rulefile );
		}
	}
}
