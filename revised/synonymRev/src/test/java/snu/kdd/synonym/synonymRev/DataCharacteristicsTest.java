package snu.kdd.synonym.synonymRev;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Map.Entry;

import org.junit.Test;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.data.ACAutomataR;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.QGram;

public class DataCharacteristicsTest {
	
	int K = 1;
	
	@Test
	public void test() throws Exception {
//		String[] datasetList = {"USPS", };
//		String[] datasetList = {"AOL", "SPROT", "USPS", "SYN_100K",};
		String[] datasetList = {
//		 "SYN_D_3.0_10000", "SYN_D_3.5_10000", "SYN_D_4.0_10000", "SYN_D_4.5_10000", "SYN_D_5.0_10000", "SYN_D_5.5_10000", "SYN_D_6.0_10000", "SYN_K_0.0_10000", "SYN_K_0.2_10000", "SYN_K_0.4_10000", "SYN_K_0.6_10000", "SYN_K_0.8_10000", 
//		 "SYN_L_3_10000", "SYN_L_5_10000", "SYN_L_7_10000", "SYN_L_9_10000", "SYN_L_11_10000", "SYN_S_0.0_10000", "SYN_S_-6.0_10000", "SYN_S_-5.0_10000", "SYN_S_-4.0_10000", "SYN_S_-3.0_10000",
//		 "SYN_D_3.5_100000", "SYN_D_4.0_100000", "SYN_D_4.5_100000", "SYN_D_5.0_100000", "SYN_D_5.5_100000", "SYN_D_6.0_100000", "SYN_K_0.0_100000", "SYN_K_0.2_100000", "SYN_K_0.4_100000", "SYN_K_0.6_100000", "SYN_K_0.8_100000", "SYN_K_1.0_100000", "SYN_L_11_100000", "SYN_L_3_100000", "SYN_L_5_100000", "SYN_L_7_100000", "SYN_L_9_100000", "SYN_S_0.0_100000", "SYN_S_-6.0_100000", "SYN_S_-5.0_100000", "SYN_S_-4.0_100000", "SYN_S_-3.0_100000",
//		"SYN2_D_2.5_10000_SELF", "SYN2_D_5.0_10000_SELF", "SYN2_D_10.0_10000_SELF", "SYN2_D_20.0_10000_SELF", "SYN2_D_40.0_10000_SELF", "SYN2_K_0.0_10000_SELF", "SYN2_K_0.2_10000_SELF", "SYN2_K_0.4_10000_SELF", "SYN2_K_0.6_10000_SELF", "SYN2_K_0.8_10000_SELF", "SYN2_K_1.0_10000_SELF", "SYN2_L_3_10000_SELF", "SYN2_L_7_10000_SELF", "SYN2_L_11_10000_SELF", "SYN2_L_15_10000_SELF", "SYN2_S_-4.0_10000_SELF", "SYN2_S_-3.0_10000_SELF", "SYN2_S_-2.0_10000_SELF", "SYN2_S_-1.0_10000_SELF", "SYN2_A_3_10000_SELF", "SYN2_A_6_10000_SELF", "SYN2_A_12_10000_SELF", "SYN2_A_24_10000_SELF",
//		"SYN3_A_1_SELF", "SYN3_A_3_SELF", "SYN3_A_6_SELF", "SYN3_A_12_SELF", "SYN3_A_24_SELF", "SYN3_KR_0.0_SELF", "SYN3_KR_0.2_SELF", "SYN3_KR_0.4_SELF", "SYN3_KR_0.6_SELF", "SYN3_KR_0.8_SELF", "SYN3_KP_0.0_SELF", "SYN3_KP_0.5_SELF", "SYN3_KP_1.0_SELF"
//		"SYN3_A_1_SELF", "SYN3_A_3_SELF", "SYN3_A_6_SELF", "SYN3_A_12_SELF", "SYN3_A_24_SELF",
//		"SYN3_KP_0.0_SELF", "SYN3_KP_0.5_SELF", "SYN3_KP_1.0_SELF"
//		"SYN3_D_200000_L_5_A_1_KAP_0.5_KR_0.0_KP_0.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_1_KAP_0.5_KR_0.0_KP_0.5_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_1_KAP_0.5_KR_0.0_KP_1.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_1_KAP_0.5_KR_0.2_KP_0.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_1_KAP_0.5_KR_0.2_KP_0.5_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_1_KAP_0.5_KR_0.2_KP_1.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_1_KAP_0.5_KR_0.4_KP_0.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_1_KAP_0.5_KR_0.4_KP_0.5_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_1_KAP_0.5_KR_0.4_KP_1.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_1_KAP_0.5_KR_0.6_KP_0.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_1_KAP_0.5_KR_0.6_KP_0.5_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_1_KAP_0.5_KR_0.6_KP_1.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_1_KAP_0.5_KR_0.8_KP_0.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_1_KAP_0.5_KR_0.8_KP_0.5_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_1_KAP_0.5_KR_0.8_KP_1.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_3_KAP_0.5_KR_0.0_KP_0.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_3_KAP_0.5_KR_0.0_KP_0.5_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_3_KAP_0.5_KR_0.0_KP_1.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_3_KAP_0.5_KR_0.2_KP_0.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_3_KAP_0.5_KR_0.2_KP_0.5_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_3_KAP_0.5_KR_0.2_KP_1.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_3_KAP_0.5_KR_0.4_KP_0.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_3_KAP_0.5_KR_0.4_KP_0.5_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_3_KAP_0.5_KR_0.4_KP_1.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_3_KAP_0.5_KR_0.6_KP_0.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_3_KAP_0.5_KR_0.6_KP_0.5_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_3_KAP_0.5_KR_0.6_KP_1.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_3_KAP_0.5_KR_0.8_KP_0.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_3_KAP_0.5_KR_0.8_KP_0.5_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_3_KAP_0.5_KR_0.8_KP_1.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_6_KAP_0.5_KR_0.0_KP_0.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_6_KAP_0.5_KR_0.0_KP_0.5_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_6_KAP_0.5_KR_0.0_KP_1.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_6_KAP_0.5_KR_0.2_KP_0.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_6_KAP_0.5_KR_0.2_KP_0.5_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_6_KAP_0.5_KR_0.2_KP_1.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_6_KAP_0.5_KR_0.4_KP_0.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_6_KAP_0.5_KR_0.4_KP_0.5_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_6_KAP_0.5_KR_0.4_KP_1.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_6_KAP_0.5_KR_0.6_KP_0.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_6_KAP_0.5_KR_0.6_KP_0.5_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_6_KAP_0.5_KR_0.6_KP_1.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_6_KAP_0.5_KR_0.8_KP_0.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_6_KAP_0.5_KR_0.8_KP_0.5_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_6_KAP_0.5_KR_0.8_KP_1.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_12_KAP_0.5_KR_0.0_KP_0.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_12_KAP_0.5_KR_0.0_KP_0.5_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_12_KAP_0.5_KR_0.0_KP_1.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_12_KAP_0.5_KR_0.2_KP_0.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_12_KAP_0.5_KR_0.2_KP_0.5_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_12_KAP_0.5_KR_0.2_KP_1.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_12_KAP_0.5_KR_0.4_KP_0.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_12_KAP_0.5_KR_0.4_KP_0.5_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_12_KAP_0.5_KR_0.4_KP_1.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_12_KAP_0.5_KR_0.6_KP_0.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_12_KAP_0.5_KR_0.6_KP_0.5_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_12_KAP_0.5_KR_0.6_KP_1.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_12_KAP_0.5_KR_0.8_KP_0.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_12_KAP_0.5_KR_0.8_KP_0.5_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_12_KAP_0.5_KR_0.8_KP_1.0_S_1.0e-02_SELF", 
//		"SYN3_D_200000_L_5_A_24_KAP_0.5_KR_0.0_KP_0.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_24_KAP_0.5_KR_0.0_KP_0.5_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_24_KAP_0.5_KR_0.0_KP_1.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_24_KAP_0.5_KR_0.2_KP_0.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_24_KAP_0.5_KR_0.2_KP_0.5_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_24_KAP_0.5_KR_0.2_KP_1.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_24_KAP_0.5_KR_0.4_KP_0.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_24_KAP_0.5_KR_0.4_KP_0.5_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_24_KAP_0.5_KR_0.4_KP_1.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_24_KAP_0.5_KR_0.6_KP_0.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_24_KAP_0.5_KR_0.6_KP_0.5_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_24_KAP_0.5_KR_0.6_KP_1.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_24_KAP_0.5_KR_0.8_KP_0.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_24_KAP_0.5_KR_0.8_KP_0.5_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_24_KAP_0.5_KR_0.8_KP_1.0_S_1.0e-02_SELF"
//		"SYN3_D_200000_L_5_A_5_KAP_0.5_KR_0.0_KP_0.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_5_KAP_0.5_KR_0.0_KP_0.5_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_5_KAP_0.5_KR_0.0_KP_1.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_5_KAP_0.5_KR_0.5_KP_0.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_5_KAP_0.5_KR_0.5_KP_0.5_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_5_KAP_0.5_KR_0.5_KP_1.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_5_KAP_0.5_KR_1.0_KP_0.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_5_KAP_0.5_KR_1.0_KP_0.5_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_5_KAP_0.5_KR_1.0_KP_1.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_10_KAP_0.5_KR_0.0_KP_0.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_10_KAP_0.5_KR_0.0_KP_0.5_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_10_KAP_0.5_KR_0.0_KP_1.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_10_KAP_0.5_KR_0.5_KP_0.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_10_KAP_0.5_KR_0.5_KP_0.5_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_10_KAP_0.5_KR_0.5_KP_1.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_10_KAP_0.5_KR_1.0_KP_0.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_10_KAP_0.5_KR_1.0_KP_0.5_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_10_KAP_0.5_KR_1.0_KP_1.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_15_KAP_0.5_KR_0.0_KP_0.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_15_KAP_0.5_KR_0.0_KP_0.5_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_15_KAP_0.5_KR_0.0_KP_1.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_15_KAP_0.5_KR_0.5_KP_0.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_15_KAP_0.5_KR_0.5_KP_0.5_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_15_KAP_0.5_KR_0.5_KP_1.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_15_KAP_0.5_KR_1.0_KP_0.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_15_KAP_0.5_KR_1.0_KP_0.5_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_15_KAP_0.5_KR_1.0_KP_1.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_20_KAP_0.5_KR_0.0_KP_0.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_20_KAP_0.5_KR_0.0_KP_0.5_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_20_KAP_0.5_KR_0.0_KP_1.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_20_KAP_0.5_KR_0.5_KP_0.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_20_KAP_0.5_KR_0.5_KP_0.5_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_20_KAP_0.5_KR_0.5_KP_1.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_20_KAP_0.5_KR_1.0_KP_0.0_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_20_KAP_0.5_KR_1.0_KP_0.5_S_1.0e-02_SELF", "SYN3_D_200000_L_5_A_20_KAP_0.5_KR_1.0_KP_1.0_S_1.0e-02_SELF",
//		"SYN4_D_25000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF", "SYN4_D_50000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF", "SYN4_D_100000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF", "SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF", "SYN4_D_400000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF",
//		"SYN4_D_25000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF", "SYN4_D_50000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF", "SYN4_D_100000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF", "SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF", "SYN4_D_400000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF", "SYN4_D_200000_L_7_A_3_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF", "SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF", "SYN4_D_200000_L_7_A_12_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF", 
//		"SYN4_D_200000_L_7_A_24_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF", 
//		"SYN4_D_200000_L_7_A_6_LCF_1_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF", "SYN4_D_200000_L_7_A_6_LCF_5_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF", "SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF", "SYN4_D_200000_L_7_A_6_LCF_15_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF", "SYN4_D_200000_L_7_A_6_LCF_20_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF", "SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.00_KR_0.50_KP_0.50_S_1.0e-03_SELF", "SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.30_KR_0.50_KP_0.50_S_1.0e-03_SELF", "SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.60_KR_0.50_KP_0.50_S_1.0e-03_SELF", "SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.90_KR_0.50_KP_0.50_S_1.0e-03_SELF", "SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-04_SELF", "SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF", "SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-02_SELF", "SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-01_SELF", "SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.00_KP_0.50_S_1.0e-03_SELF", "SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.25_KP_0.50_S_1.0e-03_SELF", "SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF", "SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.75_KP_0.50_S_1.0e-03_SELF", "SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_1.00_KP_0.50_S_1.0e-03_SELF", "SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.00_S_1.0e-03_SELF", "SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.25_S_1.0e-03_SELF", "SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-03_SELF", "SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.75_S_1.0e-03_SELF", "SYN4_D_200000_L_7_A_6_LCF_10_KAP_0.50_KR_0.50_KP_1.00_S_1.0e-03_SELF", 
//		"SYN5_D_400000_L_1_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-02_SELF", "SYN5_D_400000_L_1_A_3_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-02_SELF", "SYN5_D_400000_L_1_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-02_SELF", "SYN5_D_400000_L_1_A_9_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-02_SELF", "SYN5_D_400000_L_1_A_12_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-02_SELF", "SYN5_D_400000_L_1_A_6_LCF_1_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-02_SELF", "SYN5_D_400000_L_1_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-02_SELF", "SYN5_D_400000_L_1_A_6_LCF_20_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-02_SELF", "SYN5_D_400000_L_1_A_6_LCF_10_KAP_0.00_KR_0.50_KP_0.50_S_1.0e-02_SELF", "SYN5_D_400000_L_1_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-02_SELF", "SYN5_D_400000_L_1_A_6_LCF_10_KAP_1.00_KR_0.50_KP_0.50_S_1.0e-02_SELF", "SYN5_D_400000_L_1_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-05_SELF", "SYN5_D_400000_L_1_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-02_SELF", "SYN5_D_400000_L_1_A_6_LCF_10_KAP_0.50_KR_0.00_KP_0.50_S_1.0e-02_SELF", "SYN5_D_400000_L_1_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-02_SELF", "SYN5_D_400000_L_1_A_6_LCF_10_KAP_0.50_KR_1.00_KP_0.50_S_1.0e-02_SELF", "SYN5_D_400000_L_1_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.00_S_1.0e-02_SELF", "SYN5_D_400000_L_1_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-02_SELF", "SYN5_D_400000_L_1_A_6_LCF_10_KAP_0.50_KR_0.50_KP_1.00_S_1.0e-02_SELF",
		"SYN5_D_200000_L_1_A_3_LCF_10_KAP_0.50_KR_0.00_KP_0.00_S_1.0e-02_SELF", "SYN5_D_200000_L_1_A_3_LCF_10_KAP_0.50_KR_0.00_KP_0.50_S_1.0e-02_SELF", "SYN5_D_200000_L_1_A_3_LCF_10_KAP_0.50_KR_0.00_KP_1.00_S_1.0e-02_SELF", "SYN5_D_200000_L_1_A_3_LCF_10_KAP_0.50_KR_0.50_KP_0.00_S_1.0e-02_SELF", "SYN5_D_200000_L_1_A_3_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-02_SELF", "SYN5_D_200000_L_1_A_3_LCF_10_KAP_0.50_KR_0.50_KP_1.00_S_1.0e-02_SELF", "SYN5_D_200000_L_1_A_3_LCF_10_KAP_0.50_KR_1.00_KP_0.00_S_1.0e-02_SELF", "SYN5_D_200000_L_1_A_3_LCF_10_KAP_0.50_KR_1.00_KP_0.50_S_1.0e-02_SELF", "SYN5_D_200000_L_1_A_3_LCF_10_KAP_0.50_KR_1.00_KP_1.00_S_1.0e-02_SELF", "SYN5_D_200000_L_1_A_6_LCF_10_KAP_0.50_KR_0.00_KP_0.00_S_1.0e-02_SELF", "SYN5_D_200000_L_1_A_6_LCF_10_KAP_0.50_KR_0.00_KP_0.50_S_1.0e-02_SELF", "SYN5_D_200000_L_1_A_6_LCF_10_KAP_0.50_KR_0.00_KP_1.00_S_1.0e-02_SELF", "SYN5_D_200000_L_1_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.00_S_1.0e-02_SELF", "SYN5_D_200000_L_1_A_6_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-02_SELF", "SYN5_D_200000_L_1_A_6_LCF_10_KAP_0.50_KR_0.50_KP_1.00_S_1.0e-02_SELF", "SYN5_D_200000_L_1_A_6_LCF_10_KAP_0.50_KR_1.00_KP_0.00_S_1.0e-02_SELF", "SYN5_D_200000_L_1_A_6_LCF_10_KAP_0.50_KR_1.00_KP_0.50_S_1.0e-02_SELF", "SYN5_D_200000_L_1_A_6_LCF_10_KAP_0.50_KR_1.00_KP_1.00_S_1.0e-02_SELF", "SYN5_D_200000_L_1_A_9_LCF_10_KAP_0.50_KR_0.00_KP_0.00_S_1.0e-02_SELF", "SYN5_D_200000_L_1_A_9_LCF_10_KAP_0.50_KR_0.00_KP_0.50_S_1.0e-02_SELF", "SYN5_D_200000_L_1_A_9_LCF_10_KAP_0.50_KR_0.00_KP_1.00_S_1.0e-02_SELF", "SYN5_D_200000_L_1_A_9_LCF_10_KAP_0.50_KR_0.50_KP_0.00_S_1.0e-02_SELF", "SYN5_D_200000_L_1_A_9_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-02_SELF", "SYN5_D_200000_L_1_A_9_LCF_10_KAP_0.50_KR_0.50_KP_1.00_S_1.0e-02_SELF", "SYN5_D_200000_L_1_A_9_LCF_10_KAP_0.50_KR_1.00_KP_0.00_S_1.0e-02_SELF", "SYN5_D_200000_L_1_A_9_LCF_10_KAP_0.50_KR_1.00_KP_0.50_S_1.0e-02_SELF", "SYN5_D_200000_L_1_A_9_LCF_10_KAP_0.50_KR_1.00_KP_1.00_S_1.0e-02_SELF", "SYN5_D_200000_L_1_A_12_LCF_10_KAP_0.50_KR_0.00_KP_0.00_S_1.0e-02_SELF", "SYN5_D_200000_L_1_A_12_LCF_10_KAP_0.50_KR_0.00_KP_0.50_S_1.0e-02_SELF", "SYN5_D_200000_L_1_A_12_LCF_10_KAP_0.50_KR_0.00_KP_1.00_S_1.0e-02_SELF", "SYN5_D_200000_L_1_A_12_LCF_10_KAP_0.50_KR_0.50_KP_0.00_S_1.0e-02_SELF", "SYN5_D_200000_L_1_A_12_LCF_10_KAP_0.50_KR_0.50_KP_0.50_S_1.0e-02_SELF", "SYN5_D_200000_L_1_A_12_LCF_10_KAP_0.50_KR_0.50_KP_1.00_S_1.0e-02_SELF", "SYN5_D_200000_L_1_A_12_LCF_10_KAP_0.50_KR_1.00_KP_0.00_S_1.0e-02_SELF", "SYN5_D_200000_L_1_A_12_LCF_10_KAP_0.50_KR_1.00_KP_0.50_S_1.0e-02_SELF", "SYN5_D_200000_L_1_A_12_LCF_10_KAP_0.50_KR_1.00_KP_1.00_S_1.0e-02_SELF",



		};

		// banned list
//		 "SYN_K_1.0_10000", 
//		 "SYN_D_3.0_100000", 

//		String[] datasetList = {"SPROT", "SYN_D_3.0"};
		PrintWriter writer = new PrintWriter( new BufferedWriter( new FileWriter( "tmp/DataCharacteristicsTest.txt" ) ) );
		int[] pos_arr = {10, 20, 30, 40, 50, 60, 70, 80, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100};
		int size = 1000000;
		Object2ObjectOpenHashMap<String, IntArrayList> narMap = new Object2ObjectOpenHashMap<>();
		Object2ObjectOpenHashMap<String, LongArrayList> netMap = new Object2ObjectOpenHashMap<>();
		
		String[] arr_attr = {"dataset", "size", "nRule", "DIC", 
				"LEN_min", "LEN_avg", "LEN_max", "NAR_min", "NAR_avg", "NAR_max", 
				"NET_min", "NET_avg", "NET_max", "NMT_min", "NMT_avg", "NMT_max",
				"NAR_0_POS", "NAR_1_POS", "NAR_2_POS", "NAR_3_POS", "NAR_4_POS", "NAR_5_POS", "NAR_6_POS", "NAR_7_POS", "NAR_8_POS", "NAR_9_POS",
				"NAR_10_PER", "NAR_20_PER", "NAR_30_PER", "NAR_40_PER", "NAR_50_PER", "NAR_60_PER", "NAR_70_PER", "NAR_80_PER", "NAR_90_PER", 
				"NAR_91_PER", "NAR_92_PER", "NAR_93_PER", "NAR_94_PER", "NAR_95_PER", "NAR_96_PER", "NAR_97_PER", "NAR_98_PER", "NAR_99_PER", "NAR_100_PER", 
				"NET_10_PER", "NET_20_PER", "NET_30_PER", "NET_40_PER", "NET_50_PER", "NET_60_PER", "NET_70_PER", "NET_80_PER", "NET_90_PER",
				"NET_91_PER", "NET_92_PER", "NET_93_PER", "NET_94_PER", "NET_95_PER", "NET_96_PER", "NET_97_PER", "NET_98_PER", "NET_99_PER", "NET_100_PER",
		};
		
		for ( String attr : arr_attr ) writer.print( attr+"\t" );
		writer.println();
				

		for ( String dataset : datasetList ) {
			for ( int nRule : new int[] {10000, 100000} ) {
				if ( dataset.equals( "SPROT" ) ) size = 466158;
				try {
					Query query = TestUtils.getTestQuery( dataset, size, nRule );

		//			final ACAutomataR automata = new ACAutomataR( query.ruleSet.get());
					
		//			Object2IntOpenHashMap lhsFreqMap = new Object2IntOpenHashMap<>();
		//			lhsFreqMap.defaultReturnValue(0);
		//			for ( Rule rule : query.ruleSet.get() ) {
		//				lhsFreqMap.addTo( rule.getLeft(), 1 );
		//				System.out.println( rule );
		//			}
		//			System.out.println( "LHS frequency" );
		//			System.out.println( Arrays.toString( lhsFreqMap.values().stream().sorted((x,y)->-Integer.compare(x,y)).mapToInt( Integer::intValue ).limit( 20 ).toArray() ) );
					
					int nTokens = countNumTokens( query );
					int nRules = query.ruleSet.size();
					double[] statLEN = getStatLen( query );
					double[] statNAR = getStatNumApplicableRules( query );
					double[] statNET = getStatEstNuMTrans( query );
					double[] statNMT = getStatNumMultipleTrans( query );

					int[] arrNarPosDist = getStatNumPosApplicableRules( query );
					String narPosDist = "";
					for ( int nar : arrNarPosDist ) narPosDist += nar+"\t";
					narPosDist = narPosDist.trim();

					IntArrayList naList = getNumARulesDistInfo( query );
					narMap.put( dataset, naList );
					String numARulesDistInfo = "";
					for ( int pos : pos_arr ) {
						numARulesDistInfo += naList.getInt( (int)(pos*naList.size()/100.0)-1 )+"\t";
					}
					numARulesDistInfo = numARulesDistInfo.trim();

					LongArrayList estNumTransList = getEstNumTransDistInfo( query );
					netMap.put( dataset, estNumTransList );
					String numEstTransDistInfo = "";
					for ( int pos : pos_arr ) {
						numEstTransDistInfo += estNumTransList.getLong( (int)(pos*estNumTransList.size()/100.0)-1 )+"\t";
					}
					numEstTransDistInfo = numEstTransDistInfo.trim();
					
					String[] arr_attr_vals = {
						dataset,
						Integer.toString( size ),
						Integer.toString( nRules ),
						Integer.toString( nTokens ),
						Double.toString( statLEN[0] ),
						Double.toString( statLEN[1] ),
						Double.toString( statLEN[2] ),
						Double.toString( statNAR[0] ),
						Double.toString( statNAR[1] ),
						Double.toString( statNAR[2] ),
						Double.toString( statNET[0] ),
						Double.toString( statNET[1] ),
						Double.toString( statNET[2] ),
						Double.toString( statNMT[0] ),
						Double.toString( statNMT[1] ),
						Double.toString( statNMT[2] ),
						narPosDist,
						numARulesDistInfo,
						numEstTransDistInfo,
					};
					
					for ( String val : arr_attr_vals ) {
						writer.print(val+"\t");
					}
					writer.println();
					writer.flush();
				}
				catch ( Exception e ) {e.printStackTrace();}
			}
		}
		writer.close();
		
//		PrintWriter ps_dist = new PrintWriter( new BufferedWriter( new FileWriter( "tmp/DataDistributions.txt" ) ) );
//		for ( String dataset : datasetList ) ps_dist.print( dataset+"_NAR\t"+dataset+"_NET\t" );
//		ps_dist.println();
//		for ( int i=0; i<1000000; ++i ) {
//			for ( String dataset : datasetList ) {
//				if ( i < narMap.get( dataset ).size() ) ps_dist.print( narMap.get( dataset ).getInt( i )+"\t" );
//				else ps_dist.print( "\t" );
//				if ( i < netMap.get( dataset ).size() ) ps_dist.print( netMap.get( dataset ).getLong( i )+"\t" );
//				else ps_dist.print( "\t" );
//			}
//			ps_dist.println();
//		}
//		ps_dist.flush();
//		ps_dist.close();
	}
	

	int countNumTokens( Query query ) {
		IntOpenHashSet tokenSet = new IntOpenHashSet();
		for ( Record rec : query.searchedSet.recordList ) tokenSet.addAll( rec.getTokens() );
		if ( !query.selfJoin ) for ( Record rec : query.indexedSet.recordList ) tokenSet.addAll( rec.getTokens() );
		return tokenSet.size();
	}
	
	// returned value: double[3], [0]: min, [1]: avg, [2]: max

	double[] getStatLen( Query query ) {
		double[] output = new double[3];
		output[0] = Double.MAX_VALUE;
		for ( Record rec : query.searchedSet.recordList ) {
			int len = rec.size();
			output[0] = Math.min( output[0], len );
			output[1] += len;
			output[2] = Math.max( output[2], len );
		}
		output[1] /= query.searchedSet.size();
		return output;
	}
	
	double[] getStatNumApplicableRules( Query query ) {
		double[] output = new double[3];
		output[0] = Double.MAX_VALUE;
		for ( Record rec : query.searchedSet.recordList ) {
			int nar = rec.getNumApplicableRules();
			output[0] = Math.min( output[0], nar );
			output[1] += nar;
			output[2] = Math.max( output[2], nar );
		}
		output[1] /= query.searchedSet.size();
		return output;
	}

	int[] getStatNumPosApplicableRules( Query query ) {
		int[] narPosDist = new int[10];
		for ( Record rec : query.searchedSet.recordList ) {
			for ( int k=0; k<rec.size(); ++k ) {
				for ( Rule rule : rec.getApplicableRules( k ) ) {
					for ( int i=0; i<rule.leftSize(); ++i ) {
						if ( k+i >= 10 ) break;
						++narPosDist[k+i];
					}
				}
			}
		}
		return narPosDist;
	}
	
	double[] getStatEstNuMTrans( Query query ) {
		double[] output = new double[3];
		output[0] = Double.MAX_VALUE;
		for ( Record rec : query.searchedSet.recordList ) {
			if ( rec.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) continue;
			long net = rec.getEstNumTransformed();
			output[0] = Math.min( output[0], net );
			output[1] += net;
			output[2] = Math.max( output[2], net );
		}
		output[1] /= query.searchedSet.size();
		return output;
	}
	
	double[] getStatNumMultipleTrans( Query query ) {
		/*
		 * Two choices:
		 * (1) scan the set of all the rules in R (chosen)
		 * (2) scan the set of all the applicable rules of every s in S
		 */
		double[] output = new double[3];
		output[0] = Double.MAX_VALUE;
		Object2IntOpenHashMap<int[]> counter = new Object2IntOpenHashMap<>();
		for ( Rule rule :  query.ruleSet.get() ) {
			counter.addTo( rule.getLeft(), 1 );
		}
		for ( Entry<int[], Integer> entry : counter.entrySet() ) {
			int[] key = entry.getKey();
			int count = entry.getValue();
			output[0] = Math.min( output[0], count );
			output[1] += count;
			output[2] = Math.max( output[2], count );
		}
		output[1] /= query.ruleSet.size();
		return output;
	}
	
	Int2IntOpenHashMap getHistAvgNumApplicableRules( Query query ) {
		Int2IntOpenHashMap hist = new Int2IntOpenHashMap();
		hist.defaultReturnValue( 0 );
		for ( Record rec : query.searchedSet.recordList ) {
			if ( rec.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) continue;
			hist.addTo( rec.getNumApplicableRules(), 1 );
		}
		return hist;
	}
	
	IntArrayList getNumARulesDistInfo( Query query ) {
		IntArrayList naList = new IntArrayList();
		for ( Record rec : query.searchedSet.recordList ) {
			if ( rec.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) continue;
			naList.add( rec.getNumApplicableRules() );
		}
		naList.sort( Integer::compare );
		// [10, 20, ..., 90, 91, 92, ..., 100]
		return naList;
	}
	
	LongArrayList getEstNumTransDistInfo( Query query ) {
		LongArrayList estNumTransList = new LongArrayList();
		for ( Record rec : query.searchedSet.recordList ) {
			if ( rec.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) continue;
			estNumTransList.add( rec.getEstNumTransformed() );
		}
		estNumTransList.sort( Long::compare );
		// [10, 20, ..., 90, 91, 92, ..., 100]
		return estNumTransList;
	}
	
	double getEntropyOfTableT( Query query ) {
		Int2IntOpenHashMap counter = new Int2IntOpenHashMap();
		counter.defaultReturnValue( 0 );
		for ( Record rec : query.indexedSet.recordList ) {
			for ( int k=0; k<Math.min( K, rec.size() ); ++k ) counter.addTo( rec.getTokensArray()[k], 1 );
		}
		
		IntCollection countSet = counter.values();
		int countSum = countSet.stream().reduce( (a,b)->a+b ).get();
		
		double entropy = -countSet.stream().mapToDouble( Integer::doubleValue ).map( x -> x/countSum ).map( p -> p * Math.log( p ) ).sum();
		return entropy;
	}
}
