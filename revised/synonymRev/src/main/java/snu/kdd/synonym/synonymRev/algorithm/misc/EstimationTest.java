package snu.kdd.synonym.synonymRev.algorithm.misc;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.estimation.SampleEstimate;
import snu.kdd.synonym.synonymRev.index.JoinMHIndex;
import snu.kdd.synonym.synonymRev.index.JoinMinIndex;
import snu.kdd.synonym.synonymRev.index.NaiveIndex;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.Param;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class EstimationTest extends AlgorithmTemplate {

	public EstimationTest( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}

	public Validator checker;
	SampleEstimate estimate;
	private int qSize = 0;
	private int indexK = 0;
	private double sampleRatio = 0;
	private int joinThreshold = 1;
	private boolean joinMinRequired = true;
	private boolean joinMHRequired = true;

	NaiveIndex naiveIndex;
	JoinMinIndex joinMinIdx;
	JoinMHIndex joinMHIdx;

	private long maxSearchedEstNumRecords = 0;
	private long maxIndexedEstNumRecords = 0;

	public static BufferedWriter bw = null;
	public int[] range = null;

	public static BufferedWriter getWriter() {
		if( bw == null ) {
			try {
				bw = new BufferedWriter( new FileWriter( "Estimation_DEBUG.txt" ) );
			}
			catch( IOException e ) {
				e.printStackTrace();
			}
		}
		return bw;
	}

	public static void closeWriter() {
		if( bw != null ) {
			try {
				bw.close();
			}
			catch( IOException e ) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void preprocess() {
		super.preprocess();

		for( Record rec : query.indexedSet.get() ) {
			rec.preprocessSuffixApplicableRules();
			if( maxIndexedEstNumRecords < rec.getEstNumTransformed() ) {
				maxIndexedEstNumRecords = rec.getEstNumTransformed();
			}
		}
		if( !query.selfJoin ) {
			for( Record rec : query.searchedSet.get() ) {
				rec.preprocessSuffixApplicableRules();
				if( maxSearchedEstNumRecords < rec.getEstNumTransformed() ) {
					maxSearchedEstNumRecords = rec.getEstNumTransformed();
				}
			}
		}
		else {
			maxSearchedEstNumRecords = maxIndexedEstNumRecords;
		}
	}

	@SuppressWarnings( "unused" )
	@Override
	public void run( Query query, String[] args ) throws IOException, ParseException {

		// USPS
		int[] USPSRange = { 3, 4, 6, 8, 9, 10, 12, 14, 16, 18, 20, 21, 24, 27, 28, 30, 36, 40 };

		// SPROT
		int[] SPROTRange = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 20, 21, 22, 24, 25, 26, 27, 28, 29,
				30, 32, 34, 35, 36, 40, 41, 42, 44, 45, 48, 49, 51, 52, 54, 55, 56, 63, 64, 65, 70, 72, 75, 80, 81, 84, 96, 98,
				103, 104, 117, 120, 125, 135, 144, 147, 150, 151, 168, 180, 192, 216, 221, 246, 256, 289, 290, 294, 297, 300, 384,
				490, 576, 580, 735, 1056, 1215, 1470, 1536, 1640, 1681, 1963, 2058, 8232, 16198, 68921 };

		// AOL
		int[] AOLRange = { 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29,
				30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 54, 55, 56, 57, 58,
				59, 60, 62, 63, 64, 65, 66, 67, 68, 69, 70, 72, 75, 76, 77, 78, 79, 80, 81, 82, 84, 85, 86, 87, 88, 90, 91, 92,
				93, 95, 96, 97, 98, 99, 100, 102, 104, 105, 106, 108, 110, 112, 113, 114, 115, 116, 117, 119, 120, 121, 124, 125,
				126, 127, 128, 130, 132, 133, 135, 136, 138, 140, 141, 143, 144, 147, 150, 151, 152, 153, 154, 155, 156, 160, 161,
				162, 163, 165, 168, 169, 170, 171, 174, 175, 176, 179, 180, 182, 184, 186, 187, 189, 192, 195, 196, 198, 199, 200,
				204, 207, 208, 209, 210, 215, 216, 217, 220, 221, 224, 225, 226, 228, 230, 231, 232, 234, 238, 240, 241, 242, 243,
				245, 246, 247, 248, 250, 252, 255, 256, 260, 261, 264, 266, 270, 272, 273, 276, 279, 280, 282, 285, 286, 287, 288,
				289, 294, 295, 297, 300, 304, 306, 308, 310, 312, 315, 320, 322, 324, 325, 328, 329, 330, 333, 336, 338, 340, 342,
				343, 344, 345, 348, 350, 352, 357, 360, 363, 364, 366, 368, 372, 374, 375, 378, 380, 384, 385, 390, 391, 392, 396,
				400, 403, 405, 408, 414, 416, 418, 420, 425, 430, 432, 434, 437, 440, 441, 448, 450, 455, 456, 460, 462, 465, 468,
				476, 480, 486, 490, 492, 495, 496, 500, 504, 506, 508, 510, 512, 520, 522, 525, 528, 539, 540, 544, 546, 550, 552,
				560, 561, 567, 568, 570, 575, 576, 580, 585, 588, 594, 595, 600, 605, 612, 616, 620, 621, 624, 627, 629, 630, 632,
				636, 637, 640, 644, 648, 650, 651, 657, 660, 665, 672, 675, 680, 682, 686, 688, 693, 700, 702, 704, 714, 720, 726,
				728, 732, 735, 736, 744, 756, 765, 768, 770, 775, 780, 781, 784, 791, 792, 800, 810, 812, 816, 819, 820, 828, 833,
				836, 840, 850, 855, 860, 864, 868, 880, 882, 884, 891, 895, 896, 900, 912, 924, 928, 930, 933, 935, 936, 945, 949,
				960, 968, 969, 970, 972, 980, 990, 992, 1008, 1012, 1020, 1029, 1040, 1050, 1056, 1064, 1078, 1080, 1085, 1089,
				1090, 1092, 1100, 1104, 1116, 1120, 1122, 1134, 1140, 1144, 1150, 1152, 1160, 1164, 1170, 1175, 1176, 1184, 1188,
				1190, 1200, 1210, 1215, 1216, 1225, 1232, 1236, 1248, 1254, 1260, 1274, 1276, 1280, 1287, 1288, 1295, 1296, 1305,
				1309, 1320, 1323, 1334, 1344, 1365, 1368, 1372, 1380, 1386, 1400, 1403, 1404, 1428, 1430, 1440, 1444, 1452, 1455,
				1456, 1458, 1470, 1479, 1482, 1488, 1496, 1500, 1512, 1518, 1520, 1530, 1531, 1536, 1539, 1540, 1548, 1550, 1560,
				1568, 1575, 1584, 1600, 1610, 1617, 1620, 1624, 1632, 1650, 1656, 1664, 1666, 1674, 1680, 1694, 1701, 1710, 1716,
				1724, 1728, 1750, 1760, 1764, 1782, 1792, 1800, 1804, 1815, 1840, 1848, 1862, 1872, 1890, 1896, 1904, 1908, 1911,
				1920, 1925, 1936, 1944, 1950, 1960, 1984, 2000, 2002, 2016, 2040, 2046, 2058, 2070, 2079, 2088, 2100, 2108, 2112,
				2145, 2148, 2160, 2170, 2178, 2184, 2210, 2220, 2232, 2240, 2268, 2292, 2295, 2304, 2310, 2340, 2352, 2376, 2380,
				2394, 2400, 2420, 2425, 2430, 2432, 2448, 2450, 2464, 2484, 2496, 2520, 2560, 2576, 2592, 2600, 2610, 2616, 2618,
				2628, 2640, 2646, 2652, 2668, 2673, 2688, 2700, 2728, 2736, 2744, 2760, 2772, 2790, 2793, 2800, 2816, 2835, 2856,
				2860, 2880, 2898, 2900, 2926, 2940, 2970, 2976, 2992, 3000, 3024, 3042, 3072, 3080, 3105, 3120, 3150, 3168, 3176,
				3185, 3200, 3220, 3240, 3264, 3270, 3276, 3300, 3312, 3315, 3360, 3388, 3400, 3402, 3420, 3456, 3458, 3468, 3472,
				3510, 3528, 3564, 3570, 3584, 3596, 3600, 3627, 3640, 3672, 3696, 3735, 3744, 3780, 3816, 3822, 3825, 3840, 3876,
				3888, 3906, 3920, 3969, 4032, 4048, 4050, 4068, 4080, 4116, 4140, 4160, 4200, 4235, 4312, 4320, 4368, 4410, 4420,
				4464, 4480, 4488, 4536, 4560, 4564, 4576, 4592, 4608, 4620, 4640, 4680, 4704, 4725, 4732, 4752, 4785, 4800, 4816,
				4840, 4992, 5040, 5100, 5120, 5148, 5152, 5208, 5239, 5250, 5280, 5292, 5400, 5472, 5544, 5580, 5600, 5616, 5670,
				5712, 5750, 5760, 5850, 5880, 6048, 6060, 6072, 6160, 6240, 6256, 6272, 6300, 6336, 6400, 6426, 6440, 6528, 6552,
				6578, 6624, 6720, 6762, 6804, 6897, 6912, 6936, 7000, 7056, 7068, 7280, 7350, 7392, 7480, 7560, 7595, 7616, 7821,
				7938, 7980, 8064, 8160, 8190, 8250, 8280, 8288, 8316, 8415, 8450, 8500, 8580, 8640, 8694, 8832, 8840, 8883, 8960,
				9120, 9152, 9177, 9180, 9216, 9240, 9300, 9408, 9504, 9720, 9744, 9800, 10164, 10240, 10368, 10568, 10584, 10692,
				10752, 10800, 10920, 10976, 11340, 11466, 11520, 11550, 11707, 11880, 12000, 12096, 12285, 12600, 12775, 12870,
				12936, 12960, 13005, 13056, 13104, 13392, 13572, 13650, 13824, 13860, 14080, 14112, 14168, 14280, 14553, 14700,
				14720, 14850, 15120, 15190, 15360, 15500, 15552, 15624, 15840, 16128, 16170, 16464, 16605, 16632, 16704, 16770,
				16807, 17160, 17640, 17710, 17745, 17940, 18032, 18040, 18144, 18200, 18240, 18480, 18564, 18816, 19040, 19152,
				19208, 19278, 19380, 19440, 19584, 20800, 21000, 21060, 21168, 21560, 21750, 22018, 22176, 22320, 22680, 22932,
				23040, 24000, 24180, 24288, 24304, 25200, 25375, 25600, 25704, 25872, 26350, 27216, 27900, 28776, 28875, 29700,
				29952, 30720, 31212, 31500, 31752, 31980, 32256, 32400, 33432, 33600, 33759, 34020, 34200, 35190, 35728, 36610,
				37232, 39690, 40176, 40320, 40572, 41400, 44352, 45360, 47880, 48384, 48600, 50544, 51840, 52080, 52920, 55692,
				56000, 57600, 57720, 58464, 60016, 60690, 63630, 64260, 65688, 66528, 67200, 71400, 71680, 71890, 75264, 75600,
				78624, 80325, 81144, 81312, 85680, 86400, 86940, 90720, 104040 };

		// Synthetic
		int[] SyntheticRange = { 1, 2, 3, 4, 6, 8, 9, 12, 16, 18, 24, 54 };

		range = USPSRange;

		// runMinNaive( query, args );
		runMHNaive( query, args );
	}

	public void runMHNaive( Query query, String[] args ) throws IOException, ParseException {
		Param params = Param.parseArgs( args, stat, query );
		// Setup parameters
		checker = params.validator;
		qSize = params.qgramSize;
		indexK = params.indexK;
		sampleRatio = params.sampleRatio;

		StopWatch stepTime = StopWatch.getWatchStarted( "Result_2_Preprocess_Total_Time" );
		preprocess();
		stepTime.stopAndAdd( stat );
		// Retrieve statistics

		stepTime.resetAndStart( "Result_3_Run_Time" );
		// Estimate constants

		Collection<IntegerPair> rslt = joinMHNaive();
		stepTime.stopAndAdd( stat );
		stat.addMemory( "Mem_4_Joined" );

		stepTime.resetAndStart( "Result_4_Write_Time" );
		writeResult( rslt );
		stepTime.stopAndAdd( stat );

		for( int idx : this.range ) {
			actualJoinMHNaiveThreshold( idx );
		}

		closeWriter();
	}

	public void runMinNaive( Query query, String[] args ) throws IOException, ParseException {
		Param params = Param.parseArgs( args, stat, query );
		// Setup parameters
		checker = params.validator;
		qSize = params.qgramSize;
		indexK = params.indexK;
		sampleRatio = params.sampleRatio;

		StopWatch stepTime = StopWatch.getWatchStarted( "Result_2_Preprocess_Total_Time" );
		preprocess();
		stepTime.stopAndAdd( stat );
		// Retrieve statistics

		stepTime.resetAndStart( "Result_3_Run_Time" );
		// Estimate constants

		Collection<IntegerPair> rslt = joinMinNaive();
		stepTime.stopAndAdd( stat );
		stat.addMemory( "Mem_4_Joined" );

		stepTime.resetAndStart( "Result_4_Write_Time" );
		writeResult( rslt );
		stepTime.stopAndAdd( stat );

		for( int idx : this.range ) {
			actualJoinMinNaiveThreshold( idx );
		}

		rslt = joinMinNaive();

		closeWriter();
	}

	private void buildJoinMHIndex( int threshold ) {
		// Build an index
		int[] index = new int[ indexK ];
		for( int i = 0; i < indexK; i++ ) {
			index[ i ] = i;
		}
		joinMHIdx = new JoinMHIndex( indexK, qSize, query.indexedSet.get(), query, stat, index, true, true, threshold );
	}

	private void buildJoinMinIndex( boolean writeResult, int threshold ) {
		// Build an index
		joinMinIdx = new JoinMinIndex( indexK, qSize, stat, query, threshold, writeResult );
	}

	private void buildNaiveIndex( boolean writeResult, int joinThreshold ) {
		naiveIndex = NaiveIndex.buildIndex( joinThreshold / 2, stat, joinThreshold, writeResult, query );
	}

	private void findJoinMHConstants( double sampleratio ) {
		// Sample
		estimate = new SampleEstimate( query, sampleratio, query.selfJoin );
		estimate.estimateJoinMHNaiveWithSample( stat, checker, indexK, qSize );
	}

	private void findJoinMinConstants( double sampleratio ) {
		// Sample
		estimate = new SampleEstimate( query, sampleratio, query.selfJoin );
		estimate.estimateJoinMinNaiveWithSample( stat, checker, indexK, qSize );
	}

	private ArrayList<IntegerPair> joinMHNaive() {

		StopWatch buildTime = StopWatch.getWatchStarted( "Result_3_1_Index_Building_Time" );
		findJoinMHConstants( sampleRatio );

		joinThreshold = estimate.findThetaJoinMHNaive( qSize, indexK, stat, maxIndexedEstNumRecords, maxSearchedEstNumRecords,
				query.oneSideJoin );

		if( Long.max( maxSearchedEstNumRecords, maxIndexedEstNumRecords ) <= joinThreshold ) {
			joinMHRequired = false;
		}

		Util.printLog( "Selected Threshold: " + joinThreshold );

		if( joinMHRequired ) {
			buildJoinMHIndex( joinThreshold );
		}
		int joinMHResultSize = 0;

		buildTime.stopQuiet();
		StopWatch joinTime = StopWatch.getWatchStarted( "Result_3_2_Join_Time" );
		ArrayList<IntegerPair> rslt = new ArrayList<IntegerPair>();

		if( joinMHRequired ) {
			if( query.oneSideJoin ) {
				for( Record s : query.searchedSet.get() ) {
					// System.out.println( "test " + s + " " + s.getEstNumRecords() );
					if( s.getEstNumTransformed() > joinThreshold ) {
						joinMHIdx.joinOneRecordThres( indexK, s, rslt, checker, joinThreshold, query.oneSideJoin, indexK - 1 );
					}
				}
			}
			else {
				for( Record s : query.searchedSet.get() ) {
					joinMHIdx.joinOneRecordThres( indexK, s, rslt, checker, joinThreshold, query.oneSideJoin, indexK - 1 );
				}
			}

			joinMHResultSize = rslt.size();
			stat.add( "Join_MH_Result", joinMHResultSize );
			stat.add( "Stat_Equiv_Comparison", joinMHIdx.equivComparisons );
		}
		joinTime.stopQuiet();

		buildTime.start();
		buildNaiveIndex( true, joinThreshold );
		buildTime.stopAndAdd( stat );

		if( DEBUG.JoinMHNaiveON ) {
			stat.add( "Const_Alpha_Actual", String.format( "%.2f", naiveIndex.alpha ) );
			stat.add( "Const_Alpha_IndexTime_Actual", String.format( "%.2f", naiveIndex.indexTime ) );
			stat.add( "Const_Alpha_ExpLength_Actual", String.format( "%.2f", naiveIndex.totalExpLength ) );
		}

		joinTime.start();
		@SuppressWarnings( "unused" )
		int naiveSearch = 0;
		long starttime = System.nanoTime();
		for( Record s : query.searchedSet.get() ) {
			if( s.getEstNumTransformed() > joinThreshold ) {
				continue;
			}
			else {
				naiveIndex.joinOneRecord( s, rslt );
				naiveSearch++;
			}
		}
		double joinNanoTime = System.nanoTime() - starttime;

		stat.add( "Join_Naive_Result", rslt.size() - joinMHResultSize );
		joinTime.stopAndAdd( stat );

		if( DEBUG.JoinMHNaiveON ) {
			stat.add( "Const_Beta_Actual", String.format( "%.2f", joinNanoTime / naiveIndex.totalExp ) );
			stat.add( "Const_Beta_JoinTime_Actual", String.format( "%.2f", joinTime ) );
			stat.add( "Const_Beta_TotalExp_Actual", String.format( "%.2f", naiveIndex.totalExp ) );

			stat.add( "Stat_Naive search count", naiveSearch );
		}
		buildTime.stopAndAdd( stat );
		return rslt;
	}

	private ArrayList<IntegerPair> joinMinNaive() {
		StopWatch buildTime = StopWatch.getWatchStarted( "Result_3_1_Index_Building_Time" );
		findJoinMinConstants( sampleRatio );

		joinThreshold = estimate.findThetaJoinMinNaive( qSize, stat, maxIndexedEstNumRecords, maxSearchedEstNumRecords,
				query.oneSideJoin );

		if( Long.max( maxSearchedEstNumRecords, maxIndexedEstNumRecords ) <= joinThreshold ) {
			joinMinRequired = false;
		}

		Util.printLog( "Selected Threshold: " + joinThreshold );

		StopWatch stepTime = StopWatch.getWatchStarted( "Result_7_0_JoinMin_Index_Build_Time" );

		buildTime.start();
		if( joinMinRequired ) {
			buildJoinMinIndex( true, joinThreshold );
		}
		int joinMinResultSize = 0;
		if( DEBUG.JoinMinNaiveON ) {
			if( joinMinRequired ) {
				stat.add( "Const_Gamma_Actual", String.format( "%.2f", joinMinIdx.gamma ) );
				stat.add( "Const_Gamma_SearchedSigCount_Actual", joinMinIdx.searchedTotalSigCount );
				stat.add( "Const_Gamma_CountTime_Actual", String.format( "%.2f", joinMinIdx.countTime ) );

				stat.add( "Const_Delta_Actual", String.format( "%.2f", joinMinIdx.delta ) );
				stat.add( "Const_Delta_IndexedSigCount_Actual", joinMinIdx.indexedTotalSigCount );
				stat.add( "Const_Delta_IndexTime_Actual", String.format( "%.2f", joinMinIdx.indexTime ) );
			}
			stepTime.stopAndAdd( stat );
			stepTime.resetAndStart( "Result_7_1_SearchEquiv_JoinMin_Time" );
		}
		buildTime.stopQuiet();
		StopWatch joinTime = StopWatch.getWatchStarted( "Result_3_2_Join_Time" );
		ArrayList<IntegerPair> rslt = new ArrayList<IntegerPair>();
		long joinstart = System.nanoTime();
		if( joinMinRequired ) {
			if( query.oneSideJoin ) {
				for( Record s : query.searchedSet.get() ) {
					// System.out.println( "test " + s + " " + s.getEstNumRecords() );
					if( s.getEstNumTransformed() > joinThreshold ) {
						joinMinIdx.joinRecordMaxKThres( indexK, s, rslt, true, null, checker, joinThreshold, query.oneSideJoin );
					}
				}
			}
			else {
				for( Record s : query.searchedSet.get() ) {
					joinMinIdx.joinRecordMaxKThres( indexK, s, rslt, true, null, checker, joinThreshold, query.oneSideJoin );
				}
			}

			joinMinResultSize = rslt.size();
			stat.add( "Join_Min_Result", joinMinResultSize );
			stat.add( "Stat_Equiv_Comparison", joinMinIdx.equivComparisons );
		}
		double joinminJointime = System.nanoTime() - joinstart;
		joinTime.stopQuiet();

		if( DEBUG.JoinMinNaiveON ) {
			Util.printLog( "After JoinMin Result: " + rslt.size() );
			stat.add( "Const_Epsilon_JoinTime_Actual", String.format( "%.2f", joinminJointime ) );
			if( joinMinRequired ) {
				stat.add( "Const_Epsilon_Predict_Actual", joinMinIdx.predictCount );
				stat.add( "Const_Epsilon_Actual", String.format( "%.2f", joinminJointime / joinMinIdx.predictCount ) );

				stat.add( "Const_EpsilonPrime_Actual", String.format( "%.2f", joinminJointime / joinMinIdx.comparisonCount ) );
				stat.add( "Const_EpsilonPrime_Comparison_Actual", joinMinIdx.comparisonCount );
			}
			stepTime.stopAndAdd( stat );
			stepTime.resetAndStart( "Result_7_2_Naive Index Building Time" );
		}

		buildTime.start();
		buildNaiveIndex( true, joinThreshold );
		buildTime.stopAndAdd( stat );

		if( DEBUG.JoinMinNaiveON ) {
			stat.add( "Const_Alpha_Actual", String.format( "%.2f", naiveIndex.alpha ) );
			stat.add( "Const_Alpha_IndexTime_Actual", String.format( "%.2f", naiveIndex.indexTime ) );
			stat.add( "Const_Alpha_ExpLength_Actual", String.format( "%.2f", naiveIndex.totalExpLength ) );

			stepTime.stopAndAdd( stat );
			stepTime.resetAndStart( "Result_7_3_SearchEquiv Naive Time" );
		}

		joinTime.start();
		@SuppressWarnings( "unused" )
		int naiveSearch = 0;
		long starttime = System.nanoTime();
		for( Record s : query.searchedSet.get() ) {
			if( s.getEstNumTransformed() > joinThreshold ) {
				continue;
			}
			else {
				naiveIndex.joinOneRecord( s, rslt );
				naiveSearch++;
			}
		}
		double joinNanoTime = System.nanoTime() - starttime;

		stat.add( "Join_Naive_Result", rslt.size() - joinMinResultSize );
		joinTime.stopAndAdd( stat );

		if( DEBUG.JoinMinNaiveON ) {
			stat.add( "Const_Beta_Actual", String.format( "%.2f", joinNanoTime / naiveIndex.totalExp ) );
			stat.add( "Const_Beta_JoinTime_Actual", String.format( "%.2f", joinTime ) );
			stat.add( "Const_Beta_TotalExp_Actual", String.format( "%.2f", naiveIndex.totalExp ) );

			stat.add( "Stat_Naive search count", naiveSearch );
			stepTime.stopAndAdd( stat );
		}
		buildTime.stopAndAdd( stat );
		return rslt;
	}

	private ArrayList<IntegerPair> actualJoinMHNaiveThreshold( int joinThreshold ) {

		StopWatch stepTime = StopWatch.getWatchStarted( "Result_7_0_JoinMin_Index_Build_Time" );
		BufferedWriter bwEstimation = null;
		if( DEBUG.PrintEstimationON ) {
			bwEstimation = getWriter();
			try {
				bwEstimation.write( "Threshold " + joinThreshold + "\n" );
			}
			catch( IOException e ) {
				e.printStackTrace();
			}
		}

		if( joinMHRequired ) {
			buildJoinMHIndex( joinThreshold );
		}
		if( DEBUG.JoinMHNaiveON ) {
			if( joinMHRequired ) {
				// stat.add( "Const_Gamma_Actual", String.format( "%.2f", joinMHIndex.gamma ) );
				// stat.add( "Const_Gamma_SearchedSigCount_Actual", joinMHIndex.searchedTotalSigCount );
				// stat.add( "Const_Gamma_CountTime_Actual", String.format( "%.2f", joinMHIndex.countTime ) );
				//
				// stat.add( "Const_Delta_Actual", String.format( "%.2f", joinMHIndex.delta ) );
				// stat.add( "Const_Delta_IndexedSigCount_Actual", joinMHIndex.indexedTotalSigCount );
				// stat.add( "Const_Delta_IndexTime_Actual", String.format( "%.2f", joinMHIndex.indexTime ) );
			}
			stepTime.stopAndAdd( stat );
			stepTime.resetAndStart( "Result_7_1_SearchEquiv_JoinMin_Time" );
		}

		ArrayList<IntegerPair> rslt = new ArrayList<IntegerPair>();
		long joinstart = System.nanoTime();
		if( joinMHRequired ) {
			if( query.oneSideJoin ) {
				for( Record s : query.searchedSet.get() ) {
					// System.out.println( "test " + s + " " + s.getEstNumRecords() );
					if( s.getEstNumTransformed() > joinThreshold ) {
						joinMHIdx.joinOneRecordThres( indexK, s, rslt, checker, joinThreshold, query.oneSideJoin, indexK - 1 );
					}
				}
			}
			else {
				for( Record s : query.searchedSet.get() ) {
					joinMHIdx.joinOneRecordThres( indexK, s, rslt, checker, joinThreshold, query.oneSideJoin, indexK - 1 );
				}
			}
			// stat.add( "Join_Min_Result", joinMinResultSize );
			// stat.add( "Stat_Equiv_Comparison", joinMHIndex.equivComparisons );
		}

		double joinmhJointime = System.nanoTime() - joinstart;

		if( DEBUG.PrintEstimationON ) {
			try {
				if( joinMHRequired ) {
					bwEstimation.write( "[Theta] " + joinmhJointime / (double) joinMHIdx.predictCount );
					bwEstimation.write( " JoinTime " + joinmhJointime );
					bwEstimation.write( " PredictedCount " + joinMHIdx.predictCount );
					bwEstimation.write( " ActualCount " + joinMHIdx.equivComparisons + "\n" );
				}
			}
			catch( Exception e ) {
				e.printStackTrace();
			}
		}

		if( DEBUG.JoinMHNaiveON ) {
			Util.printLog( "After JoinMin Result: " + rslt.size() );
			stat.add( "Const_Epsilon_JoinTime_Actual", String.format( "%.2f", joinmhJointime ) );
			if( joinMHRequired ) {
				// stat.add( "Const_Epsilon_Predict_Actual", joinMHIndex.predictCount );
				// stat.add( "Const_Epsilon_Actual", String.format( "%.2f", joinminJointime / joinMHIndex.predictCount ) );
				//
				// stat.add( "Const_EpsilonPrime_Actual", String.format( "%.2f", joinminJointime / joinMHIndex.comparisonCount ) );
				// stat.add( "Const_EpsilonPrime_Comparison_Actual", joinMHIndex.comparisonCount );
			}
			stepTime.stopAndAdd( stat );
			stepTime.resetAndStart( "Result_7_2_Naive Index Building Time" );
		}

		buildNaiveIndex( false, joinThreshold );

		if( DEBUG.JoinMHNaiveON ) {
			stat.add( "Const_Alpha_Actual", String.format( "%.2f", naiveIndex.alpha ) );
			stat.add( "Const_Alpha_IndexTime_Actual", String.format( "%.2f", naiveIndex.indexTime ) );
			stat.add( "Const_Alpha_ExpLength_Actual", String.format( "%.2f", naiveIndex.totalExpLength ) );

			stepTime.stopAndAdd( stat );
			stepTime.resetAndStart( "Result_7_3_SearchEquiv Naive Time" );
		}

		@SuppressWarnings( "unused" )
		int naiveSearch = 0;
		long starttime = System.nanoTime();
		for( Record s : query.searchedSet.get() ) {
			if( s.getEstNumTransformed() > joinThreshold ) {
				continue;
			}
			else {
				naiveIndex.joinOneRecord( s, rslt );
				naiveSearch++;
			}
		}

		double joinNanoTime = System.nanoTime() - starttime;

		if( DEBUG.PrintEstimationON ) {
			try {
				bwEstimation.write( "[Beta] " + joinNanoTime / (double) naiveIndex.totalExp );
				bwEstimation.write( " JoinTime " + joinNanoTime );
				bwEstimation.write( " TotalExp " + naiveIndex.totalExp + "\n" );
			}
			catch( Exception e ) {
				e.printStackTrace();
			}
		}

		if( DEBUG.JoinMHNaiveON ) {
			stat.add( "Const_Beta_Actual", String.format( "%.2f", joinNanoTime / naiveIndex.totalExp ) );
			stat.add( "Const_Beta_TotalExp_Actual", String.format( "%.2f", naiveIndex.totalExp ) );

			stat.add( "Stat_Naive search count", naiveSearch );
			stepTime.stopAndAdd( stat );
		}

		if( DEBUG.PrintEstimationON ) {
			bwEstimation = getWriter();
			try {
				bwEstimation.write( "\n" );
			}
			catch( IOException e ) {
				e.printStackTrace();
			}
		}
		return rslt;
	}

	private ArrayList<IntegerPair> actualJoinMinNaiveThreshold( int joinThreshold ) {
		System.out.println( "Threshold: " + joinThreshold );
		BufferedWriter bwEstimation = null;
		if( DEBUG.PrintEstimationON ) {
			bwEstimation = getWriter();
			try {
				bwEstimation.write( "Threshold " + joinThreshold + "\n" );
			}
			catch( IOException e ) {
				e.printStackTrace();
			}
		}

		long startTime = System.nanoTime();

		boolean joinMinRequired = true;
		if( Long.max( maxSearchedEstNumRecords, maxIndexedEstNumRecords ) <= joinThreshold ) {
			joinMinRequired = false;
		}

		if( joinMinRequired ) {
			buildJoinMinIndex( false, joinThreshold );
		}
		int joinMinResultSize = 0;

		long joinMinBuildTime = System.nanoTime();
		System.out.println( "Threshold " + joinThreshold + " joinMin Index " + ( joinMinBuildTime - startTime ) );

		ArrayList<IntegerPair> rslt = new ArrayList<IntegerPair>();

		if( joinMinRequired ) {
			if( query.oneSideJoin ) {
				for( Record s : query.searchedSet.get() ) {
					// System.out.println( "test " + s + " " + s.getEstNumRecords() );
					if( s.getEstNumTransformed() > joinThreshold ) {
						joinMinIdx.joinRecordMaxKThres( indexK, s, rslt, false, null, checker, joinThreshold, query.oneSideJoin );
					}
				}
			}
			else {
				for( Record s : query.searchedSet.get() ) {
					joinMinIdx.joinRecordMaxKThres( indexK, s, rslt, false, null, checker, joinThreshold, query.oneSideJoin );
				}
			}

			joinMinResultSize = rslt.size();
			stat.add( "Join_Min_Result", joinMinResultSize );
			stat.add( "Stat_Equiv_Comparison", joinMinIdx.equivComparisons );
		}
		long joinMinJoinTime = System.nanoTime();

		if( joinMinRequired ) {
			if( DEBUG.PrintEstimationON ) {
				try {
					bwEstimation
							.write( "[Epsilon] " + ( joinMinJoinTime - joinMinBuildTime ) / (double) joinMinIdx.predictCount );
					bwEstimation.write( " JoinTime " + ( joinMinJoinTime - joinMinBuildTime ) );
					bwEstimation.write( " PredictedCount " + joinMinIdx.predictCount );
					bwEstimation.write( " ActualCount " + joinMinIdx.comparisonCount + "\n" );
				}
				catch( Exception e ) {
					e.printStackTrace();
				}
			}
			System.out.println( "[Epsilon] " + ( joinMinJoinTime - joinMinBuildTime ) / (double) joinMinIdx.predictCount );
			System.out.println( "[Epsilon] JoinTime " + ( joinMinJoinTime - joinMinBuildTime ) );
			System.out.println( "[Epsilon] PredictedCount " + joinMinIdx.predictCount );
			System.out.println( "[Epsilon] ActualCount " + joinMinIdx.comparisonCount );

			System.out.println( "Threshold " + joinThreshold + " joinMin Join " + ( joinMinJoinTime - joinMinBuildTime ) );
		}

		buildNaiveIndex( false, joinThreshold );
		long naiveBuildTime = System.nanoTime();
		System.out.println( "Threshold " + joinThreshold + " naive Index " + ( naiveBuildTime - joinMinJoinTime ) );

		int naiveSearch = 0;
		for( Record s : query.searchedSet.get() ) {
			if( s.getEstNumTransformed() > joinThreshold ) {
				continue;
			}
			else {
				naiveIndex.joinOneRecord( s, rslt );
				naiveSearch++;
			}
		}
		double naiveJoinTime = System.nanoTime();

		if( DEBUG.PrintEstimationON ) {
			try {
				bwEstimation.write( "[Beta] " + ( naiveJoinTime - naiveBuildTime ) / (double) naiveIndex.totalExp );
				bwEstimation.write( " JoinTime " + ( naiveJoinTime - naiveBuildTime ) );
				bwEstimation.write( " TotalExp " + naiveIndex.totalExp + "\n" );
			}
			catch( Exception e ) {
				e.printStackTrace();
			}
		}

		System.out.println( "[Beta] " + ( naiveJoinTime - naiveBuildTime ) / (double) naiveIndex.totalExp );
		System.out.println( "[Beta] JoinTime " + ( naiveJoinTime - naiveBuildTime ) );
		System.out.println( "[Beta] TotalExp " + naiveIndex.totalExp );

		System.out.println( "Naive Search " + naiveSearch );

		System.out.println( "Threshold " + joinThreshold + " naive Join " + ( naiveJoinTime - naiveBuildTime ) );
		System.out.println( "Total Time " + ( naiveJoinTime - startTime ) );

		System.out.println();

		if( DEBUG.PrintEstimationON ) {
			bwEstimation = getWriter();
			try {
				bwEstimation.write( "\n" );
			}
			catch( IOException e ) {
				e.printStackTrace();
			}
		}

		return rslt;
	}

	@Override
	public String getName() {
		return "EstimationTest";
	}

	@Override
	public String getVersion() {
		return "1.0";
	}
}
