//package snu.kdd.synonym.synonymRev;
//
//import java.io.BufferedWriter;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.io.PrintWriter;
//import java.util.List;
//
//import org.junit.Test;
//
//import it.unimi.dsi.fastutil.objects.ObjectArrayList;
//import snu.kdd.synonym.synonymRev.data.Generator2;
//
//public class SyntheticDataGenerationScheme4 {
//
////	static final int[] arr_nRec = {10000, 15848};
////	static final int[] arr_nRule = {10000, 30000, 100000};
//	static final int[] arr_nRec = {1000000};
//	static final int[] arr_nRule = {10000, 100000};
//
//	static final int[] arr_DIC = {25000, 50000, 100000, 200000, 400000};
//	static final int nRecMax = arr_nRec[arr_nRec.length-1];
//	static final int nRuleMax = arr_nRule[arr_nRule.length-1];
////	static final double[] arr_id_DIC = {2.5, 5.0, 10.0, 20.0, 40.0};
//
//
////	static final int[] arr_LEN = {3, 7, 11, 15};
//
////	static final int[] arr_id_SEL = {-4, -3, -2, -1};
//
//	static final int[] arr_NAR = {3, 6, 12, 24};
//	static final int[] arr_LCF = {1, 5, 10, 15, 20};
//
//	static final double[] arr_KAP = {0, 0.3, 0.6, 0.9};
//	static final double[] arr_SEL = {1e-4, 1e-3, 1e-2, 1e-1};
//	static final double[] arr_skewR = {0.0, 0.25, 0.5, 0.75, 1.0};
//	static final double[] arr_skewP = {0.0, 0.25, 0.5, 0.75, 1.0};
//
//	static final long seed0 = 0;
//	static final long seed1 = 1;
//	static final long seed2 = 2;
//	static final int maxLhs = 2;
//	static final int maxRhs = 2;
//	static final boolean selfJoin = true;
//	static final String syn_id = "SYN4";
//	static final String parentPath = "D:/ghsong/data/synonyms/"+syn_id;
//	
//	
//	static class Default {
//		static final int DIC = 200000;
//		static final int LEN = 7;
//		static final int NAR = 6;
//		static final int LCF = 10;
//		static final double KAP = 0.5;
//		static final double SEL = 1e-3;
//		static final double skewR = 0.5;
//		static final double skewP = 0.5;
//	}
//	
//	static int DIC;
//	static int LEN;
//	static int NAR;
//	static int LCF; // LHS candidate factor
//	static double KAP;
//	static double SEL;
//	static double skewR;
//	static double skewP;
//	
//	static void resetToDefault() {
//		DIC = Default.DIC;
//		LEN = Default.LEN;
//		NAR = Default.NAR;
//		LCF = Default.LCF;
//		KAP = Default.KAP;
//		SEL = Default.SEL;
//		skewR = Default.skewR;
//		skewP = Default.skewP;
//	}
//	
//	static List<String> dname_list = new ObjectArrayList<>();
//	
//	// loggers
//	static PrintWriter ps_stat = null; // for stat computation
//	static PrintWriter ps_run = null; // for writing execution scripts
//	static PrintWriter ps_res = null; // for plotting 
//	static {
//		try {
//			ps_stat = new PrintWriter( new BufferedWriter( new FileWriter( "tmp/syngen2_stat.txt" ) ) );
//			ps_run = new PrintWriter( new BufferedWriter( new FileWriter( "tmp/syngen2_run.txt" ) ) );
//			ps_res = new PrintWriter( new BufferedWriter( new FileWriter( "tmp/syngen2_res.txt" ) ) );
//		}
//		catch( IOException e ) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
//	
//	static void flush_all() {
//		ps_stat.flush();
//		ps_run.flush();
//		ps_res.flush();
//	}
//	
//	
//	@Test
//	public void test() throws IOException {
////		generateVaryingAll();
//		generateVaryingDIC();
////		generateVaryingNAR();
////		generateVaryingLCF();
////		generateVaryingKAP();
////		generateVaryingSEL();
////		generateVaryingSkewR();
////		generateVaryingSkewP();
//		
//		ps_run.print( "dname_list = [" );
//		for ( String dname : dname_list ) ps_run.print( "\""+dname+"\", " );
//		ps_run.println( "]" );
//		flush_all();
//	}
//	
//	static void generateDataset( int DIC, int LEN, int NAR, int LCF, double KAP, double SEL, double skewR, double skewP ) throws IOException {
//		String outputPath = String.format( "%s_D_%d_L_%d_A_%d_LCF_%d_KAP_%.2f_KR_%.2f_KP_%.2f_S_%.1e_%s", syn_id, DIC, LEN, NAR, LCF, KAP, skewR, skewP, SEL, selfJoin? "SELF":"NONSELF");
//		System.out.println( "Generate "+outputPath );
////		System.out.println( DIC+", "+LEN+", "+NAR+", "+KAP+", "+SEL+", "+skewR+", "+skewP+", "+outputPath );
////		System.out.println( outputPath );
//		String rulePath = null;
//		String recordPath = null;
//		Generator2 gen = new Generator2( DIC, KAP, seed0 );
////	public static String generateRules( Generator2 gen, int nToken, int nRule, int avgNMR, double skewZ, double skewR, long seed, String outputPath ) throws IOException {
//		for ( int nRule : arr_nRule ) rulePath = Generator2.generateRules( gen, DIC, nRule, LCF, SEL, skewR, seed0, parentPath+"/"+outputPath );
//		for ( int nRec : arr_nRec ) recordPath = Generator2.generateRecords( gen, DIC, LEN, NAR, nRec, KAP, skewP, SEL, seed1, parentPath+"/"+outputPath, rulePath );
//		if (!selfJoin) for ( int nRec : arr_nRec ) Generator2.generateRecords( gen, DIC, LEN, NAR, nRec, KAP, skewP, SEL, seed1, parentPath+"/"+outputPath, rulePath );
//		
////		System.out.println( outputPath );
////		System.out.println( rulePath );
////		System.out.println( recordPath );
//		String[] token = recordPath.split( "/" );
//		String recordPathTemplate = token[token.length-1].replaceFirst( "_"+arr_nRec[arr_nRec.length-1]+"_", "_%s_" );
//		token = rulePath.split( "/" );
//		String rulePathTemplate = token[token.length-1].replaceFirst( "_"+arr_nRule[arr_nRule.length-1]+"_", "_%s_" );
//		String dname = outputPath;
////		token = dname.split( "_" );
////		dname = token[0] +"_"+token[1]+"_"+token[2]+"_"+Integer.toString( arr_nRule[arr_nRule.length-1] )+"_"+token[3];
//		
//		ps_stat.println( 
//				"else if ( name.equals( \"" + dname + "\" ) ) {\n" +
//				String.format( "\tdataOnePath = prefix + String.format(\"%s\"+sep+\"%s\"+sep+\"data\"+sep+\"%s\", size );\n", syn_id, dname, recordPathTemplate ) +
//				String.format( "\tdataTwoPath = prefix + String.format(\"%s\"+sep+\"%s\"+sep+\"data\"+sep+\"%s\", size, selfJoin? 1:2 );\n", syn_id, dname, recordPathTemplate.replace("_1.txt", "_%d.txt") ) +
//				String.format( "\trulePath = prefix + String.format(\"%s\"+sep+\"%s\"+sep+\"rule\"+sep+\"%s\", nRule );\n", syn_id, dname, rulePathTemplate ) +
//				"}"
//				);
//		dname_list.add( dname );
////		else if ( name.equals( "SYN4_D_200000_L_7_A_3_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF" ) ) {
////			dataOnePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_3_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_3_0.5_0.5_1.txt", size );
////			dataTwoPath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_3_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_3_0.5_0.5_%d.txt", size, selfJoin? 1:2 );
////			rulePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_3_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"rule"+sep+"200000_%s_0.5_0.txt", nRule );
////		}
//		ps_run.println(
//				"dict_path[\'"+dname+"\'] = \'" + recordPath.replaceAll( "_"+nRecMax+"_", "_%%s_" ).replaceAll( "_1.txt", "_%%s.txt" ) + "\'\n" +
//				"dict_path[\'"+dname+"\'] = \'" + rulePath.replaceAll( "_"+nRuleMax+"_", "_%%s" ) + "\'"
//				);
//		
//		flush_all();
//	}
//
//	static void generateVaryingDIC() throws IOException {
//		resetToDefault();
//		for ( int i=0; i<arr_DIC.length; ++i ) {
//			int DIC = arr_DIC[i];
//			generateDataset( DIC, LEN, NAR, LCF, KAP, SEL, skewR, skewP );
//		}
//	}
//	
//	static void generateVaryingNAR() throws IOException {
//		resetToDefault();
//		for ( int i=0; i<arr_NAR.length; ++i ) {
//			int NAR = arr_NAR[i];
//			generateDataset( DIC, LEN, NAR, LCF, KAP, SEL, skewR, skewP );
//		}
//	}
//
//	static void generateVaryingLCF() throws IOException {
//		resetToDefault();
//		for ( int i=0; i<arr_LCF.length; ++i ) {
//			int LCF = arr_LCF[i];
//			generateDataset( DIC, LEN, NAR, LCF, KAP, SEL, skewR, skewP );
//		}
//	}
//
//	static void generateVaryingKAP() throws IOException {
//		resetToDefault();
//		for ( int i=0; i<arr_KAP.length; ++i ) {
//			double KAP = arr_KAP[i];
//			generateDataset( DIC, LEN, NAR, LCF, KAP, SEL, skewR, skewP) ;
//		}
//	}
//
//	static void generateVaryingSEL() throws IOException {
//		resetToDefault();
//		for ( int i=0; i<arr_SEL.length; ++i ) {
//			double SEL = arr_SEL[i];
//			generateDataset( DIC, LEN, NAR, LCF, KAP, SEL, skewR, skewP) ;
//		}
//	}
//
//	static void generateVaryingSkewR() throws IOException {
//		resetToDefault();
//		for ( int i=0; i<arr_skewR.length; ++i ) {
//			double skewR = arr_skewR[i];
//			generateDataset( DIC, LEN, NAR, LCF, KAP, SEL, skewR, skewP) ;
//		}
//	}
//
//	static void generateVaryingSkewP() throws IOException {
//		resetToDefault();
//		for ( int i=0; i<arr_skewP.length; ++i ) {
//			double skewP = arr_skewP[i];
//			generateDataset( DIC, LEN, NAR, LCF, KAP, SEL, skewR, skewP );
//		}
//	}
//
//	static void generateVaryingAll() throws IOException {
//		resetToDefault();
//		for ( int i0=0; i0<arr_LCF.length; ++i0 ) {
//			int NAR = arr_LCF[i0];
//			for ( int i1=0; i1<arr_skewR.length; ++i1 ) {
//				double skewR = arr_skewR[i1];
//				for ( int i2=0; i2<arr_skewP.length; ++i2 ) {
//					double skewP = arr_skewP[i2];
//					for ( int i3=0; i3<arr_KAP.length; ++i3 ) { 
//						double KAP = arr_KAP[i3];
//						generateDataset( DIC, LEN, NAR, LCF, KAP, SEL, skewR, skewP );
//					}
//				}
//			}
//		}
//	}
//}
