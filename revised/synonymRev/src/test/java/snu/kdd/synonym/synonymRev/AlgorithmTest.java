package snu.kdd.synonym.synonymRev;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.lang.reflect.Method;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import snu.kdd.synonym.synonymRev.algorithm.AlgorithmInterface;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.tools.StatContainer;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AlgorithmTest {
	
//	private static Query query;
	private static String[] args = {"-algorithm", "", "-oneSideJoin", "True", "-additional", ""};
	private static boolean isSelfJoin = false;
	
	// answer values
	private static final int[] ANS_SEQ_SELF_DELTA = new int[]{1014, 1190, 2447};
	private static final int[] ANS_SEQ_NONSELF_DELTA = new int[]{4, 149, 3281};
	private static final int[] ANS_SET_SELF_DELTA = new int[] {1028};
	private static final int[] ANS_SET_NONSELF_DELTA = new int[] {7};
	
	public static Query getSelfJoinQuery() throws ParseException, IOException {
		String osName = System.getProperty( "os.name" );
		final String dataOnePath, dataTwoPath, rulePath;
		if ( osName.startsWith( "Windows" ) ) {
			dataOnePath = "C:\\users\\ghsong\\data\\aol\\splitted\\aol_1000_data.txt";
			dataTwoPath = "C:\\users\\ghsong\\data\\aol\\splitted\\aol_1000_data.txt";
			rulePath = "C:\\users\\ghsong\\data\\wordnet\\rules.noun";
		}
		else if ( osName.startsWith( "Linux" ) ) {
			dataOnePath = "run/data_store/aol/splitted/aol_1000_data.txt";
			dataTwoPath = "run/data_store/aol/splitted/aol_1000_data.txt";
			rulePath = "run/data_store/wordnet/rules.noun";
		}
		else dataOnePath = dataTwoPath = rulePath = null;
		String[] args = ("-dataOnePath " + dataOnePath + " " + 
				"-dataTwoPath " + dataTwoPath + " " +
				"-rulePath " + rulePath + " " +
				"-outputPath output -algorithm * -oneSideJoin True -additional *").split( " ", 14 );
		
		CommandLine cmd = App.parseInput( args );
		Query query = App.getQuery( cmd );
		return query;
	}
	
	public static Query get2WayJoinQuery() throws IOException, ParseException {
		String osName = System.getProperty( "os.name" );
		final String dataOnePath, dataTwoPath, rulePath;
		if ( osName.startsWith( "Windows" ) ) {
			dataOnePath = "C:\\users\\ghsong\\data\\data\\1000000_5_10000_1.0_0.0_1.txt";
			dataTwoPath = "C:\\users\\ghsong\\data\\data\\1000000_5_10000_1.0_0.0_2.txt";
			rulePath = "C:\\users\\ghsong\\data\\rule\\30000_2_2_10000_0.0_0.txt";
		}
		else if ( osName.startsWith( "Linux" ) ) {
			dataOnePath = "run/data_store/data/1000000_5_10000_1.0_0.0_1.txt";
			dataTwoPath = "run/data_store/data/1000000_5_10000_1.0_0.0_2.txt";
			rulePath = "run/data_store/rule/30000_2_2_10000_0.0_0.txt";
		}
		else dataOnePath = dataTwoPath = rulePath = null;
		String[] args = ("-dataOnePath " + dataOnePath + " " + 
				"-dataTwoPath " + dataTwoPath + " " +
				"-rulePath " + rulePath + " " +
				"-outputPath output -algorithm * -oneSideJoin True -additional *").split( " ", 14 );
		
		CommandLine cmd = App.parseInput( args );
		Query query = App.getQuery( cmd );
		return query;
	}
	
	private static int[] getRepeatedArray( int[] arr, int nRepeat ) {
		/*
		 * Return the concatenation of nRepeat number of arrs.
		 */
		int[] out = new int[nRepeat*arr.length];
		for ( int i=0; i<arr.length; ++i ) {
			for ( int j=0; j<nRepeat; ++j ) out[i+arr.length*j] = arr[i];
		}
		return out;
	}
	
//	private static void runAlgorithm( String param ) throws ParseException, IOException {
//		args[5] = param;
//		CommandLine cmd = App.parseInput( args );
//		AlgorithmTemplate alg = App.getAlgorithm( query, stat, cmd );
//		alg.writeResult = false;
//		App.run( alg, query, cmd );
//		assertEquals( 1014, alg.rsltSize );
//	}

	private static void runAlgorithm( String param, int answer, boolean isSelfJoin ) throws ParseException, IOException {
		Record.initStatic();
		Rule.initStatic();
		Query query = null;
		if ( isSelfJoin ) query = AlgorithmTest.getSelfJoinQuery();
		else query = AlgorithmTest.get2WayJoinQuery();
		StatContainer stat = new StatContainer();
		args[5] = param;
		CommandLine cmd = App.parseInput( args );
		AlgorithmInterface alg = (AlgorithmInterface)App.getAlgorithm( query, stat, cmd );
		alg.setWriteResult( false );
		System.out.println( alg.getName()+", "+param );
		App.run( alg, query, cmd );
		assertEquals( answer, alg.getResult().size() );
	}
	
	@Test
	public void testSelected() throws ParseException, IOException {
		
		boolean[] flags = {true, false};
		for ( boolean flag : flags ) {
//			isSelfJoin = flag;
//			testJoinNaiveDelta();
//			testJoinNaiveDelta2();
			testJoinMHDelta();
			testJoinMHStrongDelta();
//			testJoinMHDeltaDP();
			testJoinMinDelta();
			testJoinMinStrongDelta();
//			testJoinMinDeltaDP();
//			testJoinHybridAllDelta();

//			testJoinNaive();
//			testJoinMH();
//			testJoinMHNaive();
//			testJoinMHNaiveThres();
//			testJoinMin();
//			testJoinMinNaive();
//			testJoinMinNaiveThres();
//			testJoinHybridAll();
//			testJoinPkduck();
			
//			testJoinMHDP();
//			testJoinMHNaiveDP();
//			testJoinMHNaiveThresDP();
//			testJoinMinDP();
//			testJoinMinNaiveDP();
//			testJoinMinNaiveThresDP();
			
//			testJoinSetNaive();
//			testJoinPQFilterDPSet();
//			testJoinPkduckSet();
		}

//		testJoinMH();
//		testJoinMHNaive();
//		testJoinMHNaiveThres();
//		testJoinMHDelta();
//		testJoinMHNaiveDelta();
//		testJoinMHNaiveThresDelta();
//		testJoinMHDeltaDP();
//
//		testJoinMin();
//		testJoinMinNaive();
//		testJoinMinNaiveThres();
//		testJoinMinDelta();
//		testJoinMinNaiveDelta();
//		testJoinMinNaiveThresDelta();
//		testJoinMinDeltaDP();
	}
	
	
	
	
	/**********************************
	 *  SEQUENCE BASED JOIN ALGORITHMS
	 **********************************/

	@Ignore
	public void testJoinNaive() throws IOException, ParseException {
		args[1] = "JoinNaive";
		String[] param_list = {
				"\"-1\"",
		};
		int answer;
		if ( isSelfJoin ) answer = ANS_SEQ_SELF_DELTA[0];
		else answer = ANS_SEQ_NONSELF_DELTA[0];
		for ( String param : param_list ) runAlgorithm( param, answer, isSelfJoin );
	}

	@Ignore
	public void testJoinMH() throws IOException, ParseException {
		args[1] = "JoinMH";
		String[] param_list = {
				"\"-K 1 -qSize 1\"",
				"\"-K 1 -qSize 2\"",
				"\"-K 2 -qSize 1\""
		};
		int answer;
		if ( isSelfJoin ) answer = ANS_SEQ_SELF_DELTA[0];
		else answer = ANS_SEQ_NONSELF_DELTA[0];
		for ( String param : param_list ) runAlgorithm( param, answer, isSelfJoin );
	}

	@Ignore
	public void testJoinMHNaive() throws IOException, ParseException {
		args[1] = "JoinMHNaive";
		String[] param_list = {
				"\"-K 1 -qSize 1 -sample 0.01\"",
				"\"-K 1 -qSize 2 -sample 0.01\"",
				"\"-K 2 -qSize 1 -sample 0.01\""
		};
		int answer;
		if ( isSelfJoin ) answer = ANS_SEQ_SELF_DELTA[0];
		else answer = ANS_SEQ_NONSELF_DELTA[0];
		for ( String param : param_list ) runAlgorithm( param, answer, isSelfJoin );
	}

	@Ignore
	public void testJoinMHNaiveThres() throws IOException, ParseException {
		args[1] = "JoinMHNaiveThres";
		String[] param_list = {
				"\"-K 1 -qSize 1 -t 300\"",
				"\"-K 1 -qSize 2 -t 300\"",
				"\"-K 2 -qSize 1 -t 300\""
		};
		int answer;
		if ( isSelfJoin ) answer = ANS_SEQ_SELF_DELTA[0];
		else answer = ANS_SEQ_NONSELF_DELTA[0];
		for ( String param : param_list ) runAlgorithm( param, answer, isSelfJoin );
	}
	
	@Ignore
	public void testJoinMin() throws ParseException, IOException {
		args[1] = "JoinMin";
		String[] param_list = {
				"\"-K 1 -qSize 1\"",
				"\"-K 1 -qSize 2\"",
				"\"-K 2 -qSize 1\""
		};
		int answer;
		if ( isSelfJoin ) answer = ANS_SEQ_SELF_DELTA[0];
		else answer = ANS_SEQ_NONSELF_DELTA[0];
		for ( String param : param_list ) runAlgorithm( param, answer, isSelfJoin );
	}
	
	@Ignore
	public void testJoinMinNaive() throws ParseException, IOException {
		args[1] = "JoinMinNaive";
		String[] param_list = {
				"\"-K 1 -qSize 1 -sample 0.01\"",
				"\"-K 1 -qSize 2 -sample 0.01\"",
				"\"-K 2 -qSize 1 -sample 0.01\""
		};
		int answer;
		if ( isSelfJoin ) answer = ANS_SEQ_SELF_DELTA[0];
		else answer = ANS_SEQ_NONSELF_DELTA[0];
		for ( String param : param_list ) runAlgorithm( param, answer, isSelfJoin );
	}
	
	@Ignore
	public void testJoinMinNaiveThres() throws ParseException, IOException {
		args[1] = "JoinMinNaiveThres";
		String[] param_list = {
				"\"-K 1 -qSize 1 -t 300\"",
				"\"-K 1 -qSize 2 -t 300\"",
				"\"-K 2 -qSize 1 -t 300\""
		};
		int answer;
		if ( isSelfJoin ) answer = ANS_SEQ_SELF_DELTA[0];
		else answer = ANS_SEQ_NONSELF_DELTA[0];
		for ( String param : param_list ) runAlgorithm( param, answer, isSelfJoin );
	}
	
	public void testJoinHybridAll() throws ParseException, IOException {
		args[1] = "JoinHybridAll";
		String[] param_list = {
				"\"-K 1 -qSize 1 -sample 0.01\"",
				"\"-K 1 -qSize 2 -sample 0.01\"",
				"\"-K 2 -qSize 1 -sample 0.01\"",
				"\"-K 2 -qSize 2 -sample 0.01\"",
		};
		int answer;
		if ( isSelfJoin ) answer = ANS_SEQ_SELF_DELTA[0];
		else answer = ANS_SEQ_NONSELF_DELTA[0];
		for ( String param : param_list ) runAlgorithm( param, answer, isSelfJoin );
	}
	
	@Ignore
	public void testJoinPkduck() throws ParseException, IOException {
		args[1] = "JoinPkduck";
		String[] param_list = {
				"\"-ord FF -verify naive -rc false\"",
				"\"-ord FF -verify naive -rc true\"",
				"\"-ord FF -verify greedy -rc false\"",
				"\"-ord FF -verify greedy -rc true\""
		};
		int answer;
		if ( isSelfJoin ) answer = ANS_SEQ_SELF_DELTA[0];
		else answer = ANS_SEQ_NONSELF_DELTA[0];
		int[] answer_list;
		if ( isSelfJoin ) answer_list = new int[]{answer, answer, answer-1, answer-1};
		else answer_list = new int[] {answer, answer, answer, answer};
		for ( int i=0; i<param_list.length; ++i ) {
			String param = param_list[i];
			runAlgorithm( param, answer_list[i] , isSelfJoin );
		}
	}
	
	
	
	
	/*************************************************
	 *  SEQUENCE BASED JOIN ALGORITHMS, DP EXTENSIONS
	 *************************************************/

	@Ignore
	public void testJoinMHDP() throws IOException, ParseException {
		args[1] = "JoinMHDP";
		String[] param_list = {
				"\"-K 1 -qSize 1\"",
				"\"-K 1 -qSize 2\"",
				"\"-K 2 -qSize 1\""
		};
		int answer;
		if ( isSelfJoin ) answer = ANS_SEQ_SELF_DELTA[0];
		else answer = ANS_SEQ_NONSELF_DELTA[0];
		for ( String param : param_list ) runAlgorithm( param, answer, isSelfJoin );
	}

	@Ignore
	public void testJoinMHNaiveDP() throws IOException, ParseException {
		args[1] = "JoinMHNaiveDP";
		String[] param_list = {
				"\"-K 1 -qSize 1 -sample 0.01\"",
				"\"-K 1 -qSize 2 -sample 0.01\"",
				"\"-K 2 -qSize 1 -sample 0.01\""
		};
		int answer;
		if ( isSelfJoin ) answer = ANS_SEQ_SELF_DELTA[0];
		else answer = ANS_SEQ_NONSELF_DELTA[0];
		for ( String param : param_list ) runAlgorithm( param, answer, isSelfJoin );
	}

	@Ignore
	public void testJoinMHNaiveThresDP() throws IOException, ParseException {
		args[1] = "JoinMHNaiveThresDP";
		String[] param_list = {
				"\"-K 1 -qSize 1 -t 300\"",
				"\"-K 1 -qSize 2 -t 300\"",
				"\"-K 2 -qSize 1 -t 300\""
		};
		int answer;
		if ( isSelfJoin ) answer = ANS_SEQ_SELF_DELTA[0];
		else answer = ANS_SEQ_NONSELF_DELTA[0];
		for ( String param : param_list ) runAlgorithm( param, answer, isSelfJoin );
	}
	
	@Ignore
	public void testJoinMinDP() throws ParseException, IOException {
		args[1] = "JoinMinDP";
		String[] param_list = {
				"\"-K 1 -qSize 1 -mode dp1\"",
				"\"-K 1 -qSize 2 -mode dp1\"",
				"\"-K 2 -qSize 1 -mode dp1\""
		};
		int answer;
		if ( isSelfJoin ) answer = ANS_SEQ_SELF_DELTA[0];
		else answer = ANS_SEQ_NONSELF_DELTA[0];
		for ( String param : param_list ) runAlgorithm( param, answer, isSelfJoin );
	}
	
	@Ignore
	public void testJoinMinNaiveDP() throws ParseException, IOException {
		args[1] = "JoinMinNaiveDP";
		String[] param_list = {
				"\"-K 1 -qSize 1 -sample 0.01\"",
				"\"-K 1 -qSize 2 -sample 0.01\"",
				"\"-K 2 -qSize 1 -sample 0.01\""
		};
		int answer;
		if ( isSelfJoin ) answer = ANS_SEQ_SELF_DELTA[0];
		else answer = ANS_SEQ_NONSELF_DELTA[0];
		for ( String param : param_list ) runAlgorithm( param, answer, isSelfJoin );
	}
	
	@Ignore
	public void testJoinMinNaiveThresDP() throws ParseException, IOException {
		args[1] = "JoinMinNaiveThresDP";
		String[] param_list = {
				"\"-K 1 -qSize 1 -t 300\"",
				"\"-K 1 -qSize 2 -t 300\"",
				"\"-K 2 -qSize 1 -t 300\""
		};
		int answer;
		if ( isSelfJoin ) answer = ANS_SEQ_SELF_DELTA[0];
		else answer = ANS_SEQ_NONSELF_DELTA[0];
		for ( String param : param_list ) runAlgorithm( param, answer, isSelfJoin );
	}
	
	
	
	
	
	
	/****************************************************
	 *  SEQUENCE BASED JOIN ALGORITHMS, DELTA EXTENSIONS
	 ****************************************************/
	
	@Ignore
	public void testJoinNaiveDelta() throws ParseException, IOException {
		args[1] = "JoinNaiveDelta";
		String[] param_list = {
				"\"-delta 0\"",
				"\"-delta 1\"",
				"\"-delta 2\""
		};
		int[] answer_list;
		if ( isSelfJoin ) answer_list = ANS_SEQ_SELF_DELTA;
		else answer_list = ANS_SEQ_NONSELF_DELTA;
		for ( int i=0; i<param_list.length; ++i ) {
			String param = param_list[i];
			runAlgorithm( param, answer_list[i] , isSelfJoin );
		}
	}

	public void testJoinNaiveDelta2() throws ParseException, IOException {
		args[1] = "JoinNaiveDelta2";
		String[] param_list = {
				"\"-delta 0\"",
				"\"-delta 1\"",
				"\"-delta 2\""
		};
		int[] answer_list;
		if ( isSelfJoin ) answer_list = ANS_SEQ_SELF_DELTA;
		else answer_list = ANS_SEQ_NONSELF_DELTA;
		for ( int i=0; i<param_list.length; ++i ) {
			String param = param_list[i];
			runAlgorithm( param, answer_list[i] , isSelfJoin );
		}
	}
	
	@Ignore
	public void testJoinMHDelta() throws ParseException, IOException {
		args[1] = "JoinMHDelta";
		String[] param_list = {
				"\"-K 1 -qSize 1 -delta 0\"",
				"\"-K 1 -qSize 1 -delta 1\"",
				"\"-K 1 -qSize 1 -delta 2\"",

				"\"-K 1 -qSize 2 -delta 0\"",
				"\"-K 1 -qSize 2 -delta 1\"",
				"\"-K 1 -qSize 2 -delta 2\"",

				"\"-K 2 -qSize 1 -delta 0\"",
				"\"-K 2 -qSize 1 -delta 1\"",
				"\"-K 2 -qSize 1 -delta 2\"",
		};
		int[] answer_list;
		if ( isSelfJoin ) answer_list = getRepeatedArray( ANS_SEQ_SELF_DELTA, 3 );
		else answer_list = getRepeatedArray( ANS_SEQ_NONSELF_DELTA, 3 );
		for ( int i=0; i<param_list.length; ++i ) {
			String param = param_list[i];
			runAlgorithm( param, answer_list[i] , isSelfJoin );
		}
	}

	@Ignore
	public void testJoinMHStrongDelta() throws ParseException, IOException {
		args[1] = "JoinMHStrongDelta";
		String[] param_list = {
				"\"-K 1 -qSize 1 -delta 0\"",
				"\"-K 1 -qSize 1 -delta 1\"",
				"\"-K 1 -qSize 1 -delta 2\"",

				"\"-K 1 -qSize 2 -delta 0\"",
				"\"-K 1 -qSize 2 -delta 1\"",
				"\"-K 1 -qSize 2 -delta 2\"",

				"\"-K 2 -qSize 1 -delta 0\"",
				"\"-K 2 -qSize 1 -delta 1\"",
				"\"-K 2 -qSize 1 -delta 2\"",
		};
		int[] answer_list;
		if ( isSelfJoin ) answer_list = getRepeatedArray( ANS_SEQ_SELF_DELTA, 3 );
		else answer_list = getRepeatedArray( ANS_SEQ_NONSELF_DELTA, 3 );
		for ( int i=0; i<param_list.length; ++i ) {
			String param = param_list[i];
			runAlgorithm( param, answer_list[i] , isSelfJoin );
		}
	}

	@Ignore
	public void testJoinMHNaiveDelta() throws ParseException, IOException {
		args[1] = "JoinMHNaiveDelta";
		String[] param_list = {
				"\"-K 1 -qSize 1 -sample 0.01 -delta 0\"",
				"\"-K 1 -qSize 1 -sample 0.01 -delta 1\"",
				"\"-K 1 -qSize 1 -sample 0.01 -delta 2\"",

				"\"-K 1 -qSize 2 -sample 0.01 -delta 0\"",
				"\"-K 1 -qSize 2 -sample 0.01 -delta 1\"",
				"\"-K 1 -qSize 2 -sample 0.01 -delta 2\"",

				"\"-K 2 -qSize 1 -sample 0.01 -delta 0\"",
				"\"-K 2 -qSize 1 -sample 0.01 -delta 1\"",
				"\"-K 2 -qSize 1 -sample 0.01 -delta 2\"",
		};
		int[] answer_list;
		if ( isSelfJoin ) answer_list = getRepeatedArray( ANS_SEQ_SELF_DELTA, 3 );
		else answer_list = getRepeatedArray( ANS_SEQ_NONSELF_DELTA, 3 );
		for ( int i=0; i<param_list.length; ++i ) {
			String param = param_list[i];
			runAlgorithm( param, answer_list[i] , isSelfJoin );
		}
	}

	@Ignore
	public void testJoinMHNaiveThresDelta() throws ParseException, IOException {
		args[1] = "JoinMHNaiveThresDelta";
		String[] param_list = {
				"\"-K 1 -qSize 1 -t 300 -delta 0\"",
				"\"-K 1 -qSize 1 -t 300 -delta 1\"",
				"\"-K 1 -qSize 1 -t 300 -delta 2\"",

				"\"-K 1 -qSize 2 -t 300 -delta 0\"",
				"\"-K 1 -qSize 2 -t 300 -delta 1\"",
				"\"-K 1 -qSize 2 -t 300 -delta 2\"",

				"\"-K 2 -qSize 1 -t 300 -delta 0\"",
				"\"-K 2 -qSize 1 -t 300 -delta 1\"",
				"\"-K 2 -qSize 1 -t 300 -delta 2\"",
		};
		int[] answer_list;
		if ( isSelfJoin ) answer_list = getRepeatedArray( ANS_SEQ_SELF_DELTA, 3 );
		else answer_list = getRepeatedArray( ANS_SEQ_NONSELF_DELTA, 3 );
		for ( int i=0; i<param_list.length; ++i ) {
			String param = param_list[i];
			runAlgorithm( param, answer_list[i] , isSelfJoin );
		}
	}

	@Ignore
	public void testJoinMHDeltaDP() throws ParseException, IOException {
		args[1] = "JoinMHDeltaDP";
		String[] param_list = {
				"\"-K 1 -qSize 1 -delta 0\"",
				"\"-K 1 -qSize 1 -delta 1\"",
				"\"-K 1 -qSize 1 -delta 2\"",

				"\"-K 1 -qSize 2 -delta 0\"",
				"\"-K 1 -qSize 2 -delta 1\"",
				"\"-K 1 -qSize 2 -delta 2\"",

				"\"-K 2 -qSize 1 -delta 0\"",
				"\"-K 2 -qSize 1 -delta 1\"",
				"\"-K 2 -qSize 1 -delta 2\"",
		};
		int[] answer_list;
		if ( isSelfJoin ) answer_list = getRepeatedArray( ANS_SEQ_SELF_DELTA, 3 );
		else answer_list = getRepeatedArray( ANS_SEQ_NONSELF_DELTA, 3 );
		for ( int i=0; i<param_list.length; ++i ) {
			String param = param_list[i];
			runAlgorithm( param, answer_list[i] , isSelfJoin );
		}
	}
	
	@Ignore
	public void testJoinMinDelta() throws ParseException, IOException {
		args[1] = "JoinMinDelta";
		String[] param_list = {
				"\"-K 1 -qSize 1 -delta 0\"",
				"\"-K 1 -qSize 1 -delta 1\"",
				"\"-K 1 -qSize 1 -delta 2\"",

				"\"-K 1 -qSize 2 -delta 0\"",
				"\"-K 1 -qSize 2 -delta 1\"",
				"\"-K 1 -qSize 2 -delta 2\"",

				"\"-K 2 -qSize 1 -delta 0\"",
				"\"-K 2 -qSize 1 -delta 1\"",
				"\"-K 2 -qSize 1 -delta 2\"",
		};
		int[] answer_list;
		if ( isSelfJoin ) answer_list = getRepeatedArray( ANS_SEQ_SELF_DELTA, 3 );
		else answer_list = getRepeatedArray( ANS_SEQ_NONSELF_DELTA, 3 );
		for ( int i=0; i<param_list.length; ++i ) {
			String param = param_list[i];
			runAlgorithm( param, answer_list[i] , isSelfJoin );
		}
	}
	
	@Ignore
	public void testJoinMinStrongDelta() throws ParseException, IOException {
		args[1] = "JoinMinStrongDelta";
		String[] param_list = {
				"\"-K 1 -qSize 1 -delta 0\"",
				"\"-K 1 -qSize 1 -delta 1\"",
				"\"-K 1 -qSize 1 -delta 2\"",

				"\"-K 1 -qSize 2 -delta 0\"",
				"\"-K 1 -qSize 2 -delta 1\"",
				"\"-K 1 -qSize 2 -delta 2\"",

				"\"-K 2 -qSize 1 -delta 0\"",
				"\"-K 2 -qSize 1 -delta 1\"",
				"\"-K 2 -qSize 1 -delta 2\"",
		};
		int[] answer_list;
		if ( isSelfJoin ) answer_list = getRepeatedArray( ANS_SEQ_SELF_DELTA, 3 );
		else answer_list = getRepeatedArray( ANS_SEQ_NONSELF_DELTA, 3 );
		for ( int i=0; i<param_list.length; ++i ) {
			String param = param_list[i];
			runAlgorithm( param, answer_list[i] , isSelfJoin );
		}
	}
	
	@Ignore
	public void testJoinMinNaiveDelta() throws ParseException, IOException {
		args[1] = "JoinMinNaiveDelta";
		String[] param_list = {
				"\"-K 1 -qSize 1 -sample 0.01 -delta 0\"",
				"\"-K 1 -qSize 1 -sample 0.01 -delta 1\"",
				"\"-K 1 -qSize 1 -sample 0.01 -delta 2\"",

				"\"-K 1 -qSize 2 -sample 0.01 -delta 0\"",
				"\"-K 1 -qSize 2 -sample 0.01 -delta 1\"",
				"\"-K 1 -qSize 2 -sample 0.01 -delta 2\"",

				"\"-K 2 -qSize 1 -sample 0.01 -delta 0\"",
				"\"-K 2 -qSize 1 -sample 0.01 -delta 1\"",
				"\"-K 2 -qSize 1 -sample 0.01 -delta 2\"",
		};
		int[] answer_list;
		if ( isSelfJoin ) answer_list = getRepeatedArray( ANS_SEQ_SELF_DELTA, 3 );
		else answer_list = getRepeatedArray( ANS_SEQ_NONSELF_DELTA, 3 );
		for ( int i=0; i<param_list.length; ++i ) {
			String param = param_list[i];
			runAlgorithm( param, answer_list[i] , isSelfJoin );
		}
	}

	@Ignore
	public void testJoinMinNaiveThresDelta() throws ParseException, IOException {
		args[1] = "JoinMinNaiveThresDelta";
		String[] param_list = {
				"\"-K 1 -qSize 1 -t 300 -delta 0\"",
				"\"-K 1 -qSize 1 -t 300 -delta 1\"",
				"\"-K 1 -qSize 1 -t 300 -delta 2\"",

				"\"-K 1 -qSize 2 -t 300 -delta 0\"",
				"\"-K 1 -qSize 2 -t 300 -delta 1\"",
				"\"-K 1 -qSize 2 -t 300 -delta 2\"",

				"\"-K 2 -qSize 1 -t 300 -delta 0\"",
				"\"-K 2 -qSize 1 -t 300 -delta 1\"",
				"\"-K 2 -qSize 1 -t 300 -delta 2\"",
		};
		int[] answer_list;
		if ( isSelfJoin ) answer_list = getRepeatedArray( ANS_SEQ_SELF_DELTA, 3 );
		else answer_list = getRepeatedArray( ANS_SEQ_NONSELF_DELTA, 3 );
		for ( int i=0; i<param_list.length; ++i ) {
			String param = param_list[i];
			runAlgorithm( param, answer_list[i] , isSelfJoin );
		}
	}
	
	@Ignore
	public void testJoinMinDeltaDP() throws ParseException, IOException {
		args[1] = "JoinMinDeltaDP";
		String[] param_list = {
				"\"-K 1 -qSize 1 -delta 0\"",
				"\"-K 1 -qSize 1 -delta 1\"",
				"\"-K 1 -qSize 1 -delta 2\"",

				"\"-K 1 -qSize 2 -delta 0\"",
				"\"-K 1 -qSize 2 -delta 1\"",
				"\"-K 1 -qSize 2 -delta 2\"",

				"\"-K 2 -qSize 1 -delta 0\"",
				"\"-K 2 -qSize 1 -delta 1\"",
				"\"-K 2 -qSize 1 -delta 2\"",
		};
		int[] answer_list;
		if ( isSelfJoin ) answer_list = getRepeatedArray( ANS_SEQ_SELF_DELTA, 3 );
		else answer_list = getRepeatedArray( ANS_SEQ_NONSELF_DELTA, 3 );
		for ( int i=0; i<param_list.length; ++i ) {
			String param = param_list[i];
			runAlgorithm( param, answer_list[i] , isSelfJoin );
		}
	}
	
	@Ignore
	public void testJoinHybridAllDelta() throws ParseException, IOException {
		args[1] = "JoinHybridAllDelta";
		String[] param_list = {
				"\"-K 1 -qSize 1 -sample 0.01 -delta 0\"",
				"\"-K 1 -qSize 1 -sample 0.01 -delta 1\"",
				"\"-K 1 -qSize 1 -sample 0.01 -delta 2\"",

				"\"-K 1 -qSize 2 -sample 0.01 -delta 0\"",
				"\"-K 1 -qSize 2 -sample 0.01 -delta 1\"",
				"\"-K 1 -qSize 2 -sample 0.01 -delta 2\"",

				"\"-K 2 -qSize 1 -sample 0.01 -delta 0\"",
				"\"-K 2 -qSize 1 -sample 0.01 -delta 1\"",
				"\"-K 2 -qSize 1 -sample 0.01 -delta 2\"",
		};
		int[] answer_list;
		if ( isSelfJoin ) answer_list = getRepeatedArray( ANS_SEQ_SELF_DELTA, 3 );
		else answer_list = getRepeatedArray( ANS_SEQ_NONSELF_DELTA, 3 );
		for ( int i=0; i<param_list.length; ++i ) {
			String param = param_list[i];
			runAlgorithm( param, answer_list[i] , isSelfJoin );
		}
	}
	
	
	
	
	
	/********************************
	 *  SET BASED JOIN ALGORITHMS
	 ********************************/

	@Ignore
	public void testJoinSetNaive() throws IOException, ParseException {
		args[1] = "JoinSetNaive";
		String[] param_list = {
				"\"-1\"",
		};
		int answer;
		if ( isSelfJoin ) answer = ANS_SET_SELF_DELTA[0];
		else answer = ANS_SET_NONSELF_DELTA[0];
		for ( String param : param_list ) runAlgorithm( param, answer, isSelfJoin );
	}

	@Ignore
	public void testJoinPQFilterDPSet() throws ParseException, IOException {
		args[1] = "JoinPQFilterDPSet";
		String[] param_list = {
				"\"-K 1 -verify TD\"",
				"\"-K 1 -verify GR1\"",
				"\"-K 1 -verify GR3\"",
				"\"-K 1 -verify MIT_GR\"",
				"\"-K 2 -verify TD\"",
				"\"-K 2 -verify GR1\"",
				"\"-K 2 -verify GR3\"",
				"\"-K 2 -verify MIT_GR\""
		};
		int answer;
		if ( isSelfJoin ) answer = ANS_SET_SELF_DELTA[0];
		else answer = ANS_SET_NONSELF_DELTA[0];
		for ( String param : param_list ) runAlgorithm( param, answer, isSelfJoin );
	}
	
	@Ignore
	public void testJoinPkduckSet() throws ParseException, IOException {
		args[1] = "JoinPkduckSet";
		String[] param_list = {
				"\"-ord FF -verify naive -rc false\"",
				"\"-ord FF -verify naive -rc true\"",
				"\"-ord FF -verify greedy -rc false\"",
				"\"-ord FF -verify greedy -rc true\""
		};
		int answer;
		if ( isSelfJoin ) answer = ANS_SET_SELF_DELTA[0];
		else answer = ANS_SET_NONSELF_DELTA[0];
		for ( String param : param_list ) runAlgorithm( param, answer, isSelfJoin );
	}
}
