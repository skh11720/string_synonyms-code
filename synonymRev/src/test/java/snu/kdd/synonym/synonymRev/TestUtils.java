package snu.kdd.synonym.synonymRev;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.Ignore;
import org.junit.Test;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import snu.kdd.synonym.synonymRev.data.ACAutomataR;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.Util;

public class TestUtils {

	public static Query getTestQuery( long size ) throws IOException {
		return getTestQuery("AOL", size );
	}

	public static Query getTestQuery( String name, long size ) throws IOException {
		return getTestQuery(name, size, 0);
	}
	
	public static Query getTestQuery( String name, long size, int nRule ) throws IOException {
		String osName = System.getProperty( "os.name" );
		String prefix = null;
		if ( osName.startsWith( "Windows" ) ) {
			prefix = "D:\\ghsong\\data\\synonyms\\";
//			String dataOnePath = "C:/users/ghsong/data/aol/splitted/aol_"+size+"_data.txt";
//			String dataTwoPath = "C:/users/ghsong/data/aol/splitted/aol_"+size+"_data.txt";
//			String rulePath = "C:/users/ghsong/data/wordnet/rules.noun";
		}
		else if ( osName.startsWith( "Linux" ) ) {
			prefix = "run/data_store/";
//			String dataOnePath = "run/data_store/aol/splitted/aol_"+size+"_data.txt";
//			String dataTwoPath = "run/data_store/aol/splitted/aol_"+size+"_data.txt";
//			String rulePath = "run/data_store/wordnet/rules.noun";
//			String outputPath = "output";
		}
		
		String sep = "\\" + File.separator;
		String dataOnePath, dataTwoPath, rulePath;
		if ( name.equals( "AOL" )) {
			dataOnePath = prefix + String.format( "aol"+sep+"splitted"+sep+"aol_%d_data.txt", size );
			dataTwoPath = prefix + String.format( "aol"+sep+"splitted"+sep+"aol_%d_data.txt", size );
			rulePath = prefix + "wordnet"+sep+"rules.noun";
		}
		else if ( name.equals( "SPROT" ) ) {
			dataOnePath = prefix + String.format( "sprot"+sep+"splitted"+sep+"SPROT_two_%d.txt", size );
			dataTwoPath = prefix + String.format( "sprot"+sep+"splitted"+sep+"SPROT_two_%d.txt", size );
			rulePath = prefix + "sprot"+sep+"rule.txt";
		}
		else if ( name.equals( "USPS" ) ) {
			dataOnePath = prefix + String.format( "JiahengLu"+sep+"splitted"+sep+"USPS_%d.txt", size );
			dataTwoPath = prefix + String.format( "JiahengLu"+sep+"splitted"+sep+"USPS_%d.txt", size );
			rulePath = prefix + "JiahengLu"+sep+"USPS_rule.txt";
		}
		else if ( name.equals( "SYN_100K" ) ) {
			dataOnePath = prefix + String.format( "data"+sep+"1000000_5_%d_1.0_0.0_1.txt", size );
			dataTwoPath = prefix + String.format( "data"+sep+"1000000_5_%d_1.0_0.0_2.txt", size );
			rulePath = prefix + "rule"+sep+"30000_2_2_100000_0.0_0.txt";
		}
		else if ( name.equals( "SYN_300K" ) ) {
			dataOnePath = prefix + String.format( "data"+sep+"1000000_5_%d_1.0_0.0_1.txt", size );
			dataTwoPath = prefix + String.format( "data"+sep+"1000000_5_%d_1.0_0.0_2.txt", size );
			rulePath = prefix + "rule"+sep+"30000_2_2_300000_0.0_0.txt";
		}
		else if ( name.equals( "SYN_1M" ) ) {
			dataOnePath = prefix + String.format( "data"+sep+"1000000_5_%d_1.0_0.0_1.txt", size );
			dataTwoPath = prefix + String.format( "data"+sep+"1000000_5_%d_1.0_0.0_2.txt", size );
			rulePath = prefix + "rule"+sep+"30000_2_2_1000000_0.0_0.txt";
		}
		
		// SYN_FAM4
		else {
			String[] token = name.split( "_" ); // [0:syn_id]_[1:tag]_[2:val]_[3:nRule]_[4:is_self]
			boolean selfJoin = token[token.length-1].equals( "SELF" )? true : false;
			if ( name.equals( "SYN4_D_25000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF" ) ) {
				dataOnePath = prefix + String.format("SYN4"+sep+"SYN4_D_25000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"25000_%s_6_0.5_0.5_1.txt", size );
				dataTwoPath = prefix + String.format("SYN4"+sep+"SYN4_D_25000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"25000_%s_6_0.5_0.5_%d.txt", size, selfJoin? 1:2 );
				rulePath = prefix + String.format("SYN4"+sep+"SYN4_D_25000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"rule"+sep+"25000_%s_0.5_0.txt", nRule );
			}
			else if ( name.equals( "SYN4_D_50000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF" ) ) {
				dataOnePath = prefix + String.format("SYN4"+sep+"SYN4_D_50000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"50000_%s_6_0.5_0.5_1.txt", size );
				dataTwoPath = prefix + String.format("SYN4"+sep+"SYN4_D_50000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"50000_%s_6_0.5_0.5_%d.txt", size, selfJoin? 1:2 );
				rulePath = prefix + String.format("SYN4"+sep+"SYN4_D_50000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"rule"+sep+"50000_%s_0.5_0.txt", nRule );
			}
			else if ( name.equals( "SYN4_D_100000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF" ) ) {
				dataOnePath = prefix + String.format("SYN4"+sep+"SYN4_D_100000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"100000_%s_6_0.5_0.5_1.txt", size );
				dataTwoPath = prefix + String.format("SYN4"+sep+"SYN4_D_100000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"100000_%s_6_0.5_0.5_%d.txt", size, selfJoin? 1:2 );
				rulePath = prefix + String.format("SYN4"+sep+"SYN4_D_100000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"rule"+sep+"100000_%s_0.5_0.txt", nRule );
			}
			else if ( name.equals( "SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF" ) ) {
				dataOnePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_6_0.5_0.5_1.txt", size );
				dataTwoPath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_6_0.5_0.5_%d.txt", size, selfJoin? 1:2 );
				rulePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"rule"+sep+"200000_%s_0.5_0.txt", nRule );
			}
			else if ( name.equals( "SYN4_D_400000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF" ) ) {
				dataOnePath = prefix + String.format("SYN4"+sep+"SYN4_D_400000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"400000_%s_6_0.5_0.5_1.txt", size );
				dataTwoPath = prefix + String.format("SYN4"+sep+"SYN4_D_400000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"400000_%s_6_0.5_0.5_%d.txt", size, selfJoin? 1:2 );
				rulePath = prefix + String.format("SYN4"+sep+"SYN4_D_400000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"rule"+sep+"400000_%s_0.5_0.txt", nRule );
			}
			else if ( name.equals( "SYN4_D_200000_L_7_A_3_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF" ) ) {
				dataOnePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_3_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_3_0.5_0.5_1.txt", size );
				dataTwoPath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_3_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_3_0.5_0.5_%d.txt", size, selfJoin? 1:2 );
				rulePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_3_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"rule"+sep+"200000_%s_0.5_0.txt", nRule );
			}
			else if ( name.equals( "SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF" ) ) {
				dataOnePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_6_0.5_0.5_1.txt", size );
				dataTwoPath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_6_0.5_0.5_%d.txt", size, selfJoin? 1:2 );
				rulePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"rule"+sep+"200000_%s_0.5_0.txt", nRule );
			}
			else if ( name.equals( "SYN4_D_200000_L_7_A_12_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF" ) ) {
				dataOnePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_12_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_12_0.5_0.5_1.txt", size );
				dataTwoPath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_12_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_12_0.5_0.5_%d.txt", size, selfJoin? 1:2 );
				rulePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_12_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"rule"+sep+"200000_%s_0.5_0.txt", nRule );
			}
			else if ( name.equals( "SYN4_D_200000_L_7_A_24_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF" ) ) {
				dataOnePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_24_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_24_0.5_0.5_1.txt", size );
				dataTwoPath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_24_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_24_0.5_0.5_%d.txt", size, selfJoin? 1:2 );
				rulePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_24_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"rule"+sep+"200000_%s_0.5_0.txt", nRule );
			}
			else if ( name.equals( "SYN4_D_200000_L_7_A_6_LCF_1_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF" ) ) {
				dataOnePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_1_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_6_0.5_0.5_1.txt", size );
				dataTwoPath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_1_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_6_0.5_0.5_%d.txt", size, selfJoin? 1:2 );
				rulePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_1_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"rule"+sep+"200000_%s_0.5_0.txt", nRule );
			}
			else if ( name.equals( "SYN4_D_200000_L_7_A_6_LCF_5_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF" ) ) {
				dataOnePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_5_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_6_0.5_0.5_1.txt", size );
				dataTwoPath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_5_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_6_0.5_0.5_%d.txt", size, selfJoin? 1:2 );
				rulePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_5_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"rule"+sep+"200000_%s_0.5_0.txt", nRule );
			}
			else if ( name.equals( "SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF" ) ) {
				dataOnePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_6_0.5_0.5_1.txt", size );
				dataTwoPath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_6_0.5_0.5_%d.txt", size, selfJoin? 1:2 );
				rulePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"rule"+sep+"200000_%s_0.5_0.txt", nRule );
			}
			else if ( name.equals( "SYN4_D_200000_L_7_A_6_LCF_15_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF" ) ) {
				dataOnePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_15_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_6_0.5_0.5_1.txt", size );
				dataTwoPath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_15_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_6_0.5_0.5_%d.txt", size, selfJoin? 1:2 );
				rulePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_15_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"rule"+sep+"200000_%s_0.5_0.txt", nRule );
			}
			else if ( name.equals( "SYN4_D_200000_L_7_A_6_LCF_20_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF" ) ) {
				dataOnePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_20_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_6_0.5_0.5_1.txt", size );
				dataTwoPath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_20_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_6_0.5_0.5_%d.txt", size, selfJoin? 1:2 );
				rulePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_20_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"rule"+sep+"200000_%s_0.5_0.txt", nRule );
			}
			else if ( name.equals( "SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.00_KR_0.50_KP_0.50_S_1.0e-03_SELF" ) ) {
				dataOnePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.00_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_6_0.0_0.5_1.txt", size );
				dataTwoPath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.00_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_6_0.0_0.5_%d.txt", size, selfJoin? 1:2 );
				rulePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.00_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"rule"+sep+"200000_%s_0.5_0.txt", nRule );
			}
			else if ( name.equals( "SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.30_KR_0.50_KP_0.50_S_1.0e-03_SELF" ) ) {
				dataOnePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.30_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_6_0.3_0.5_1.txt", size );
				dataTwoPath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.30_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_6_0.3_0.5_%d.txt", size, selfJoin? 1:2 );
				rulePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.30_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"rule"+sep+"200000_%s_0.5_0.txt", nRule );
			}
			else if ( name.equals( "SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.60_KR_0.50_KP_0.50_S_1.0e-03_SELF" ) ) {
				dataOnePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.60_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_6_0.6_0.5_1.txt", size );
				dataTwoPath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.60_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_6_0.6_0.5_%d.txt", size, selfJoin? 1:2 );
				rulePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.60_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"rule"+sep+"200000_%s_0.5_0.txt", nRule );
			}
			else if ( name.equals( "SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.90_KR_0.50_KP_0.50_S_1.0e-03_SELF" ) ) {
				dataOnePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.90_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_6_0.9_0.5_1.txt", size );
				dataTwoPath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.90_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_6_0.9_0.5_%d.txt", size, selfJoin? 1:2 );
				rulePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.90_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"rule"+sep+"200000_%s_0.5_0.txt", nRule );
			}
			else if ( name.equals( "SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-04_SELF" ) ) {
				dataOnePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-04_SELF"+sep+"data"+sep+"200000_%s_6_0.5_0.5_1.txt", size );
				dataTwoPath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-04_SELF"+sep+"data"+sep+"200000_%s_6_0.5_0.5_%d.txt", size, selfJoin? 1:2 );
				rulePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-04_SELF"+sep+"rule"+sep+"200000_%s_0.5_0.txt", nRule );
			}
			else if ( name.equals( "SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF" ) ) {
				dataOnePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_6_0.5_0.5_1.txt", size );
				dataTwoPath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_6_0.5_0.5_%d.txt", size, selfJoin? 1:2 );
				rulePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"rule"+sep+"200000_%s_0.5_0.txt", nRule );
			}
			else if ( name.equals( "SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-02_SELF" ) ) {
				dataOnePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-02_SELF"+sep+"data"+sep+"200000_%s_6_0.5_0.5_1.txt", size );
				dataTwoPath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-02_SELF"+sep+"data"+sep+"200000_%s_6_0.5_0.5_%d.txt", size, selfJoin? 1:2 );
				rulePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-02_SELF"+sep+"rule"+sep+"200000_%s_0.5_0.txt", nRule );
			}
			else if ( name.equals( "SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-01_SELF" ) ) {
				dataOnePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-01_SELF"+sep+"data"+sep+"200000_%s_6_0.5_0.5_1.txt", size );
				dataTwoPath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-01_SELF"+sep+"data"+sep+"200000_%s_6_0.5_0.5_%d.txt", size, selfJoin? 1:2 );
				rulePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-01_SELF"+sep+"rule"+sep+"200000_%s_0.5_0.txt", nRule );
			}
			else if ( name.equals( "SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.00_KP_0.50_S_1.0e-03_SELF" ) ) {
				dataOnePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.00_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_6_0.5_0.5_1.txt", size );
				dataTwoPath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.00_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_6_0.5_0.5_%d.txt", size, selfJoin? 1:2 );
				rulePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.00_KP_0.50_S_1.0e-03_SELF"+sep+"rule"+sep+"200000_%s_0.0_0.txt", nRule );
			}
			else if ( name.equals( "SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.25_KP_0.50_S_1.0e-03_SELF" ) ) {
				dataOnePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.25_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_6_0.5_0.5_1.txt", size );
				dataTwoPath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.25_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_6_0.5_0.5_%d.txt", size, selfJoin? 1:2 );
				rulePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.25_KP_0.50_S_1.0e-03_SELF"+sep+"rule"+sep+"200000_%s_0.25_0.txt", nRule );
			}
			else if ( name.equals( "SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF" ) ) {
				dataOnePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_6_0.5_0.5_1.txt", size );
				dataTwoPath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_6_0.5_0.5_%d.txt", size, selfJoin? 1:2 );
				rulePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"rule"+sep+"200000_%s_0.5_0.txt", nRule );
			}
			else if ( name.equals( "SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.75_KP_0.50_S_1.0e-03_SELF" ) ) {
				dataOnePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.75_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_6_0.5_0.5_1.txt", size );
				dataTwoPath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.75_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_6_0.5_0.5_%d.txt", size, selfJoin? 1:2 );
				rulePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.75_KP_0.50_S_1.0e-03_SELF"+sep+"rule"+sep+"200000_%s_0.75_0.txt", nRule );
			}
			else if ( name.equals( "SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_1.00_KP_0.50_S_1.0e-03_SELF" ) ) {
				dataOnePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_1.00_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_6_0.5_0.5_1.txt", size );
				dataTwoPath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_1.00_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_6_0.5_0.5_%d.txt", size, selfJoin? 1:2 );
				rulePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_1.00_KP_0.50_S_1.0e-03_SELF"+sep+"rule"+sep+"200000_%s_1.0_0.txt", nRule );
			}
			else if ( name.equals( "SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.00_S_1.0e-03_SELF" ) ) {
				dataOnePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.00_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_6_0.5_0.0_1.txt", size );
				dataTwoPath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.00_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_6_0.5_0.0_%d.txt", size, selfJoin? 1:2 );
				rulePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.00_S_1.0e-03_SELF"+sep+"rule"+sep+"200000_%s_0.5_0.txt", nRule );
			}
			else if ( name.equals( "SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.25_S_1.0e-03_SELF" ) ) {
				dataOnePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.25_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_6_0.5_0.25_1.txt", size );
				dataTwoPath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.25_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_6_0.5_0.25_%d.txt", size, selfJoin? 1:2 );
				rulePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.25_S_1.0e-03_SELF"+sep+"rule"+sep+"200000_%s_0.5_0.txt", nRule );
			}
			else if ( name.equals( "SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF" ) ) {
				dataOnePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_6_0.5_0.5_1.txt", size );
				dataTwoPath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_6_0.5_0.5_%d.txt", size, selfJoin? 1:2 );
				rulePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF"+sep+"rule"+sep+"200000_%s_0.5_0.txt", nRule );
			}
			else if ( name.equals( "SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.75_S_1.0e-03_SELF" ) ) {
				dataOnePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.75_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_6_0.5_0.75_1.txt", size );
				dataTwoPath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.75_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_6_0.5_0.75_%d.txt", size, selfJoin? 1:2 );
				rulePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.75_S_1.0e-03_SELF"+sep+"rule"+sep+"200000_%s_0.5_0.txt", nRule );
			}
			else if ( name.equals( "SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_1.00_S_1.0e-03_SELF" ) ) {
				dataOnePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_1.00_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_6_0.5_1.0_1.txt", size );
				dataTwoPath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_1.00_S_1.0e-03_SELF"+sep+"data"+sep+"200000_%s_6_0.5_1.0_%d.txt", size, selfJoin? 1:2 );
				rulePath = prefix + String.format("SYN4"+sep+"SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_1.00_S_1.0e-03_SELF"+sep+"rule"+sep+"200000_%s_0.5_0.txt", nRule );
			}

			else throw new RuntimeException();
		}

		String outputPath = "output";
		boolean oneSideJoin = true;
		Query query = new Query(rulePath, dataOnePath, dataTwoPath, oneSideJoin, outputPath);

		final ACAutomataR automata = new ACAutomataR( query.ruleSet.get());
		for ( Record record : query.searchedSet.recordList ) {
			record.preprocessRules( automata );
			record.preprocessSuffixApplicableRules();
			record.preprocessTransformLength();
			record.preprocessTransformLength();
			record.preprocessEstimatedRecords();
		}

		if ( !query.selfJoin ) {
			for ( Record record : query.indexedSet.recordList ) {
				record.preprocessRules( automata );
				record.preprocessSuffixApplicableRules();
				record.preprocessTransformLength();
				record.preprocessTransformLength();
				record.preprocessEstimatedRecords();
			}
		}
		return query;
	}

	public static void inspect_record( final Record record, final Query query ) {
		//System.out.println("record: "+record.toString(query.tokenIndex));
		System.out.println("record ("+record.getID()+"): "+Arrays.toString(record.getTokensArray()) );
		System.out.println( "num applicable rules: "+record.getNumApplicableRules() );
		System.out.println( "range of transform length: "+record.getTransLengths()[0]+", "+record.getTransLengths()[1] );

		System.out.println( "applicable rules: " );
		for (int pos=0; pos<record.size(); pos++ ) {
			for (final Rule rule : record.getSuffixApplicableRules( pos )) {
				//System.out.println("\t("+rule.toOriginalString(query.tokenIndex)+", "+pos+")");
				System.out.println("\t("+rule.toString()+", "+pos+")");
			}
		}

		System.out.println( "transformed strings: " );
		final List<Record> expanded = record.expandAll();
		for( final Record exp : expanded ) {
			System.out.println( "\t"+Arrays.toString( exp.getTokensArray() ) );
		}
		
//		System.out.println( "positional q-grams: " );
//		List<List<QGram>> qgrams_self = record.getSelfQGrams( q, record.getTokenCount() );
//		for (int i=0; i<qgrams_self.size(); i++) {
//			for (final QGram qgram : qgrams_self.get(i)) {
//				//System.out.println( "\t["+qgram.toString( query.tokenIndex )+", "+i+"]" );
//				System.out.println( "\t["+qgram.toString()+", "+i+"]" );
//			}
//		}
//		
//		System.out.println( "positional q-grams in a transformed string: " );
//		List<List<QGram>> qgrams = record.getQGrams(q);
//		for (int i=0; i<qgrams.size(); i++) {
//			for (final QGram qgram : qgrams.get(i)) {
//				//System.out.println( "\t["+qgram.toString( query.tokenIndex )+", "+i+"]" );
//				System.out.println( "\t["+qgram.toString()+", "+i+"]" );
//			}
//		}
	}
	
	@Test
	public void testLCS() {
		Random random = new Random(0);
		final int nTest = 1000;
		final int lz_max = 10;
		final int l_max = 10;
		final int nV = 10;
		
		for ( int itr=0; itr<nTest; ++itr ) {
			int error = -1;
			int lz = random.nextInt( lz_max+1 );
			int lx = lz + random.nextInt( l_max+1 );
			int ly = lz + random.nextInt( l_max+1 );
			int[] z = new int[lz];
			int[] x = new int[lx];
			int[] y = new int[ly];
			for ( int i=0; i<lz; ++i ) z[i] = random.nextInt( nV );
			IntOpenHashSet idxSet = new IntOpenHashSet();
			idxSet.clear();
			while ( idxSet.size() < lz ) idxSet.add( random.nextInt( lx ) );
			for ( int i=0, k=0; i<lx; ++i ) {
				if ( idxSet.contains( i ) ) x[i] = z[k++];
				else x[i] = error--;
			}
			idxSet.clear();
			while ( idxSet.size() < lz ) idxSet.add( random.nextInt( ly ) );
			for ( int j=0, k=0; j<ly; ++j ) {
				if ( idxSet.contains( j ) ) y[j] = z[k++];
				else y[j] = error--;
			}
//			System.out.println( "x: "+Arrays.toString(x) );
//			System.out.println( "y: "+Arrays.toString(y) );
//			System.out.println( "z: "+Arrays.toString(z) );
			assertEquals( lz, Util.lcs( x, y ) );
		}
	}
	
	// 700 ms
	@Ignore
	public void testGetCombinations() {
		// check the output
		for ( IntArrayList comb : Util.getCombinations( 7, 3 ) )
			System.out.println( comb );
		
		// measure the execution time
		final int nTest = 1000;
		final int n_max = 10;

		for ( int itr=0; itr<nTest; ++itr ) {
			for ( int n=0; n<=n_max; ++n ) {
				for ( int k=0; k<=n; ++k ) {
					Util.getCombinations( n, k ).size();
				}
			}
		}
	}

	@Ignore
	public void testGetCombinationsAll() {
		// check the output
		for ( IntArrayList comb : Util.getCombinationsAll( 7, 3 ) )
			System.out.println( comb );
	}
	
	public static void main(String[] args) throws IOException {
		Query query  =getTestQuery("AOL", 10000);
		inspect_record( query.searchedSet.getRecord(3235), query );
	}
}
