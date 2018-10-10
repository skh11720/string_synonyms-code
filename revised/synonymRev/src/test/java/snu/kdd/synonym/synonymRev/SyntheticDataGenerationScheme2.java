package snu.kdd.synonym.synonymRev;

import java.io.IOException;

import org.junit.Test;

import snu.kdd.synonym.synonymRev.data.Generator2;

public class SyntheticDataGenerationScheme2 {

	static final int[] arr_nRec = {10000, 15848, 25118, 39810, 63095, 100000, 158489, 251188, 398107, 630957, 1000000};
	static final int[] arr_nRule = {10000, 30000, 100000};
//	static final int[] arr_nRec = {1000000};
//	static final int[] arr_nRule = {10000, 20000};

	static final int[] arr_DIC = {25000, 50000, 100000, 200000, 400000};
	static final double[] arr_id_DIC = {2.5, 5.0, 10.0, 20.0, 40.0};

	static final double[] arr_KAP = {0, 0.2, 0.4, 0.6, 0.8, 1.0};

	static final int[] arr_LEN = {3, 7, 11, 15};

	static final double[] arr_SEL = {1e-4, 1e-3, 1e-2, 1e-1};
	static final int[] arr_id_SEL = {-4, -3, -2, -1};

	static final int[] arr_NAR = {3, 6, 12, 24};

	static final long seed0 = 0;
	static final long seed1 = 1;
	static final long seed2 = 2;
	static final int maxLhs = 2;
	static final int maxRhs = 2;
	static final boolean selfJoin = true;
	static final String syn_id = "SYN2";
	
	
	static class Default {
//		static final int N_REC = 1_000_000;
//		static final int N_RULE = 100_000;
		static final int DIC = arr_DIC[3];
		static final double KAP = arr_KAP[3];
		static final int LEN = arr_LEN[1];
		static final double SEL = arr_SEL[1];
		static final int NAR = -1;
	}
	
	
	
	@Test
	public void test() throws IOException {
		generateVaryingDIC();
		generateVaryingKAP();
		generateVaryingLEN();
		generateVaryingSEL();
		generateVaryingNAR();
	}
	
	static void generateVaryingDIC() throws IOException {
		for ( int i=0; i<arr_DIC.length; ++i ) {
			int DIC = arr_DIC[i];
			double id_DIC = arr_id_DIC[i];
			String outputPath = String.format( "output/%s_D_%.1f_%s", syn_id, id_DIC, selfJoin? "SELF":"NONSELF");
			System.out.println( outputPath );
			String rulefile = null;
			for ( int nRule : arr_nRule ) rulefile = Generator2.generateRules( DIC, maxLhs, maxRhs, nRule, Default.KAP, seed0, outputPath );
			for ( int nRec : arr_nRec ) Generator2.generateRecords( DIC, Default.LEN, Default.NAR, nRec, Default.KAP, Default.SEL, seed1, outputPath, rulefile );
			if (!selfJoin) for ( int nRec : arr_nRec ) Generator2.generateRecords( DIC, Default.LEN, Default.NAR, nRec, Default.KAP, Default.SEL, seed2, outputPath, rulefile );
		}
	}
	
	static void generateVaryingKAP() throws IOException {
		for ( int i=0; i<arr_KAP.length; ++i ) {
			double KAP = arr_KAP[i];
			String outputPath = String.format( "output/%s_K_%.1f_%s", syn_id, KAP, selfJoin? "SELF":"NONSELF" );
			System.out.println( outputPath );
			String rulefile = null;
			for ( int nRule : arr_nRule ) rulefile = Generator2.generateRules( Default.DIC, maxLhs, maxRhs, nRule, KAP, seed0, outputPath );
			for ( int nRec : arr_nRec ) Generator2.generateRecords( Default.DIC, Default.LEN, Default.NAR, nRec, KAP, Default.SEL, seed1, outputPath, rulefile );
			if (!selfJoin) for ( int nRec : arr_nRec ) Generator2.generateRecords( Default.DIC, Default.LEN, Default.NAR, nRec, KAP, Default.SEL, seed2, outputPath, rulefile );
		}
	}

	static void generateVaryingLEN() throws IOException {
		for ( int i=0; i<arr_LEN.length; ++i ) {
			int LEN = arr_LEN[i];
			String outputPath = String.format( "output/%s_L_%d_%s", syn_id, LEN, selfJoin? "SELF":"NONSELF" );
			System.out.println( outputPath );
			String rulefile = null;
			for ( int nRule : arr_nRule ) rulefile = Generator2.generateRules( Default.DIC, maxLhs, maxRhs, nRule, Default.KAP, seed0, outputPath );
			for ( int nRec : arr_nRec ) Generator2.generateRecords( Default.DIC, LEN, Default.NAR, nRec, Default.KAP, Default.SEL, seed1, outputPath, rulefile );
			if (!selfJoin) for ( int nRec : arr_nRec ) Generator2.generateRecords( Default.DIC, LEN, Default.NAR, nRec, Default.KAP, Default.SEL, seed2, outputPath, rulefile );
		}
	}
	
	static void generateVaryingSEL() throws IOException {
		for ( int i=0; i<arr_SEL.length; ++i ) {
			double SEL = arr_SEL[i];
			double id_SEL = arr_id_SEL[i];
			String outputPath = String.format( "output/%s_S_%.1f_%s", syn_id, id_SEL, selfJoin? "SELF":"NONSELF" );
			System.out.println( outputPath );
			String rulefile = null;
			for ( int nRule : arr_nRule ) rulefile = Generator2.generateRules( Default.DIC, maxLhs, maxRhs, nRule, Default.KAP, seed0, outputPath );
			for ( int nRec : arr_nRec ) Generator2.generateRecords( Default.DIC, Default.LEN, Default.NAR, nRec, Default.KAP, SEL, seed1, outputPath, rulefile );
			if (!selfJoin) for ( int nRec : arr_nRec ) Generator2.generateRecords( Default.DIC, Default.LEN, Default.NAR, nRec, Default.KAP, SEL, seed2, outputPath, rulefile );
		}
	}

	static void generateVaryingNAR() throws IOException {
		for ( int i=0; i<arr_NAR.length; ++i ) {
			int NAR = arr_NAR[i];
			String outputPath = String.format( "output/%s_A_%d_%s", syn_id, NAR, selfJoin? "SELF":"NONSELF" );
			System.out.println( outputPath );
			String rulefile = null;
			for ( int nRule : arr_nRule ) rulefile = Generator2.generateRules( Default.DIC, maxLhs, maxRhs, nRule, Default.KAP, seed0, outputPath );
			for ( int nRec : arr_nRec ) Generator2.generateRecords( Default.DIC, -1, NAR, nRec, Default.KAP, Default.SEL, seed1, outputPath, rulefile );
			if (!selfJoin) for ( int nRec : arr_nRec ) Generator2.generateRecords( Default.DIC, -1, NAR, nRec, Default.KAP, Default.SEL, seed2, outputPath, rulefile );
		}
	}
}
