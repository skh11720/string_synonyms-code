package snu.kdd.synonym.synonymRev;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmFactory;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmInterface;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AlgorithmTest {


	@Test
	public void testSelected() throws ParseException, IOException {
		
		boolean[] flags = {true, false};
		for ( boolean flag : flags ) {
			isSelfJoin = flag;
			
			testJoinNaive();
			testJoinMH();
			testJoinMin();
			testJoinMinFast();
			testJoinHybridAll();
			testJoinPkduck();
			testPassJoin();
			
			testJoinSetNaive();
			testJoinPkduckSet();
			testJoinBKPSet();
			
			testJoinDeltaNaive();
			testJoinDeltaSimple();
			testJoinDeltaVar();
			testJoinDeltaVarBK();
		}
	}

	@Ignore
	public void testSingleCase() throws IOException, ParseException {
		args[1] = "JoinDeltaVar";
		int K = 1;
		int q = 2;
		int d = 1;
		String dist = "lcs";
		boolean isSelfJoin = false;
		String pstr = String.format("\"-K %d -qSize %d -delta %d -dist %s\"", K, q, d, dist);
		int answer;
		if ( isSelfJoin ) answer = ANS_SEQ_SELF_DELTA.get(dist)[d];
		else answer = ANS_SEQ_NONSELF_DELTA.get(dist)[d];
		runAlgorithm( pstr, answer, isSelfJoin );
	}
	
	public static String[] args = {"-algorithm", "", "-oneSideJoin", "True", "-additional", ""};
	public static boolean isSelfJoin = false;
	
	private static final Map<String, int[]> ANS_SEQ_SELF_DELTA = new Object2ObjectOpenHashMap<>();
	static {
		final int[] ANS_SEQ_SELF_DELTA_EDIT = new int[]{1014, 2126, 33711};
		final int[] ANS_SEQ_SELF_DELTA_LCS = new int[]{1014, 1190, 2447};
		ANS_SEQ_SELF_DELTA.put("edit", ANS_SEQ_SELF_DELTA_EDIT);
		ANS_SEQ_SELF_DELTA.put("lcs", ANS_SEQ_SELF_DELTA_LCS);
	}

	private static final Map<String, int[]> ANS_SEQ_NONSELF_DELTA = new Object2ObjectOpenHashMap<>();
	static {
		final int[] ANS_SEQ_NONSELF_DELTA_EDIT = new int[]{105, 3938, 49322};
		final int[] ANS_SEQ_NONSELF_DELTA_LCS = new int[]{105, 136, 4174};
		ANS_SEQ_NONSELF_DELTA.put("edit", ANS_SEQ_NONSELF_DELTA_EDIT);
		ANS_SEQ_NONSELF_DELTA.put("lcs", ANS_SEQ_NONSELF_DELTA_LCS);
	}

	private static final int[] ANS_SET_SELF_DELTA = new int[] {1028}; 
	private static final int[] ANS_SET_NONSELF_DELTA = new int[] {105};
	private static final String[] dist_list = new String[] {"edit", "lcs"};

	
	
	public static Query getSelfJoinQuery() throws ParseException, IOException {
		String osName = System.getProperty( "os.name" );
		final String dataOnePath, dataTwoPath, rulePath;
		if ( osName.startsWith( "Windows" ) ) {
			dataOnePath = "D:\\ghsong\\data\\synonyms\\aol\\splitted\\aol_1000_data.txt";
			dataTwoPath = "D:\\ghsong\\data\\synonyms\\aol\\splitted\\aol_1000_data.txt";
			rulePath = "D:\\ghsong\\data\\synonyms\\wordnet\\rules.noun";
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
		Query query = Query.parseQuery( cmd );
		return query;
	}
	
	public static Query get2WayJoinQuery() throws IOException, ParseException {
		String osName = System.getProperty( "os.name" );
		final String dataOnePath, dataTwoPath, rulePath;
		if ( osName.startsWith( "Windows" ) ) {
			dataOnePath = "D:\\ghsong\\data\\synonyms\\data\\1000000_5_1000_1.0_0.0_1.txt";
			dataTwoPath = "D:\\ghsong\\data\\synonyms\\data\\1000000_5_1000_1.0_0.0_2.txt";
			rulePath = "D:\\ghsong\\data\\synonyms\\rule\\30000_2_2_10000_0.0_0.txt";
		}
		else if ( osName.startsWith( "Linux" ) ) {
			dataOnePath = "run/data_store/data/1000000_5_1000_1.0_0.0_1.txt";
			dataTwoPath = "run/data_store/data/1000000_5_1000_1.0_0.0_2.txt";
			rulePath = "run/data_store/rule/30000_2_2_10000_0.0_0.txt";
		}
		else dataOnePath = dataTwoPath = rulePath = null;
		String[] args = ("-dataOnePath " + dataOnePath + " " + 
				"-dataTwoPath " + dataTwoPath + " " +
				"-rulePath " + rulePath + " " +
				"-outputPath output -algorithm * -oneSideJoin True -additional *").split( " ", 14 );
		
		CommandLine cmd = App.parseInput( args );
		Query query = Query.parseQuery( cmd );
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
	
	private static void runAlgorithm( String param, int answer, boolean isSelfJoin ) throws ParseException, IOException {
		Record.initStatic();
		Rule.initStatic();
		Query query = null;
		if ( isSelfJoin ) query = AlgorithmTest.getSelfJoinQuery();
		else query = TestUtils.getTestQuery("UNIV_1_2", -1);
		args[5] = param;
		CommandLine cmd = App.parseInput( args );
		AlgorithmInterface alg = AlgorithmFactory.getAlgorithmInstance(cmd, query.selfJoin);
		alg.setWriteResult( true );
		System.out.println( alg.getName()+", "+param );
		alg.run(query);
		System.out.println( "Result size: "+alg.getResult().size() );
		assertEquals( answer, alg.getResult().size() );
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
		if ( isSelfJoin ) answer = ANS_SEQ_SELF_DELTA.get("edit")[0];
		else answer = ANS_SEQ_NONSELF_DELTA.get("edit")[0];
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
		if ( isSelfJoin ) answer = ANS_SEQ_SELF_DELTA.get("edit")[0];
		else answer = ANS_SEQ_NONSELF_DELTA.get("edit")[0];
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
		if ( isSelfJoin ) answer = ANS_SEQ_SELF_DELTA.get("edit")[0];
		else answer = ANS_SEQ_NONSELF_DELTA.get("edit")[0];
		for ( String param : param_list ) runAlgorithm( param, answer, isSelfJoin );
	}

	@Ignore
	public void testJoinMinFast() throws ParseException, IOException {
		args[1] = "JoinMinFast";
		String[] param_list = {
				"\"-K 1 -qSize 1 -sampleB 0.01\"",
				"\"-K 1 -qSize 2 -sampleB 0.05\"",
				"\"-K 2 -qSize 1 -sampleB 0.1\""
		};
		int answer;
		if ( isSelfJoin ) answer = ANS_SEQ_SELF_DELTA.get("edit")[0];
		else answer = ANS_SEQ_NONSELF_DELTA.get("edit")[0];
		for ( String param : param_list ) runAlgorithm( param, answer, isSelfJoin );
	}
	
	@Ignore
	public void testJoinHybridAll() throws ParseException, IOException {
		args[1] = "JoinHybridAll";
		String[] param_list = {
				"\"-K 1 -qSize 1 -sampleH 0.01 -sampleB 0.01\"",
				"\"-K 1 -qSize 2 -sampleH 0.01 -sampleB 0.01\"",
				"\"-K 2 -qSize 1 -sampleH 0.01 -sampleB 0.01\"",
				"\"-K 2 -qSize 2 -sampleH 0.01 -sampleB 0.01\"",
		};
		int answer;
		if ( isSelfJoin ) answer = ANS_SEQ_SELF_DELTA.get("edit")[0];
		else answer = ANS_SEQ_NONSELF_DELTA.get("edit")[0];
		for ( String param : param_list ) runAlgorithm( param, answer, isSelfJoin );
	}
	
	@Ignore
	public void testJoinPkduck() throws ParseException, IOException {
		args[1] = "JoinPkduck";
		String[] param_list = {
				"\"-ord FF -verify naive -rc false -lf true\"",
				"\"-ord FF -verify naive -rc true -lf true\"",
				"\"-ord FF -verify greedy -rc false -lf true\"",
				"\"-ord FF -verify greedy -rc true -lf true\"",
				"\"-ord FF -verify naive -rc false -lf false\"",
				"\"-ord FF -verify naive -rc true -lf false\"",
				"\"-ord FF -verify greedy -rc false -lf false\"",
				"\"-ord FF -verify greedy -rc true -lf false\"",
		};
		int answer;
		if ( isSelfJoin ) answer = ANS_SEQ_SELF_DELTA.get("edit")[0];
		else answer = ANS_SEQ_NONSELF_DELTA.get("edit")[0];
		int[] answer_list;
		if ( isSelfJoin ) answer_list = new int[]{answer, answer, answer-1, answer-1, answer, answer, answer-1, answer-1 };
		else answer_list = new int[] {answer, answer, answer, answer, answer, answer, answer, answer};
		for ( int i=0; i<param_list.length; ++i ) {
			String param = param_list[i];
			runAlgorithm( param, answer_list[i] , isSelfJoin );
		}
	}

	@Ignore
	public void testPassJoin() throws ParseException, IOException {
		args[1] = "PassJoin";
		String[] param_list = {
				"\"-delta 0\""
		};
		int answer;
		if ( isSelfJoin ) answer = ANS_SEQ_SELF_DELTA.get("edit")[0];
		else answer = ANS_SEQ_NONSELF_DELTA.get("edit")[0];
		for ( String param : param_list ) runAlgorithm( param, answer, isSelfJoin );
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
	public void testJoinPkduckSet() throws ParseException, IOException {
		args[1] = "JoinPkduckSet";
		String[] param_list = {
				"\"-ord FF -verify naive -rc false -lf true\"",
				"\"-ord FF -verify naive -rc true -lf true\"",
				"\"-ord FF -verify greedy -rc false -lf true\"",
				"\"-ord FF -verify greedy -rc true -lf true\"",
				"\"-ord FF -verify naive -rc false -lf false\"",
				"\"-ord FF -verify naive -rc true -lf false\"",
				"\"-ord FF -verify greedy -rc false -lf false\"",
				"\"-ord FF -verify greedy -rc true -lf false\"",
		};
		int answer;
		if ( isSelfJoin ) answer = ANS_SET_SELF_DELTA[0];
		else answer = ANS_SET_NONSELF_DELTA[0];
		for ( String param : param_list ) runAlgorithm( param, answer, isSelfJoin );
	}

	@Ignore
	public void testJoinBKPSet() throws ParseException, IOException {
		args[1] = "JoinBKPSet";
		String[] param_list = {
				"\"-K 1 -verify TD\"",
				"\"-K 2 -verify TD\"",
				"\"-K 1 -verify GR1\"",
				"\"-K 2 -verify GR1\"",
		};
		int answer;
		if ( isSelfJoin ) answer = ANS_SET_SELF_DELTA[0];
		else answer = ANS_SET_NONSELF_DELTA[0];
		for ( String param : param_list ) runAlgorithm( param, answer, isSelfJoin );
	}
	
	
	
	/********************************
	 *  DELTA JOIN ALGORITHMS
	 ********************************/
	
	@Ignore
	public void testJoinDeltaNaive() throws IOException, ParseException {
		args[1] = "JoinDeltaNaive";
		for ( String dist : dist_list ) {
			String[] param_list = {
					"\"-delta 0 -dist "+dist+"\"",
					"\"-delta 1 -dist "+dist+"\"",
					"\"-delta 2 -dist "+dist+"\"",
			};
			int answer;
			for ( int d=0; d<param_list.length; ++d ) {
				if ( isSelfJoin ) answer = ANS_SEQ_SELF_DELTA.get(dist)[d];
				else answer = ANS_SEQ_NONSELF_DELTA.get(dist)[d];
				runAlgorithm( param_list[d], answer, isSelfJoin );
			}
		}
	}

	@Ignore
	public void testJoinDeltaSimple() throws IOException, ParseException {
		args[1] = "JoinDeltaSimple";
		for ( String dist : dist_list ) {
			String[][] param_list = new String[3][];
			for ( int d=0; d<3; ++d ) {
				List<String> pstr_list = new ArrayList<>();
				for ( int q=1; q<=3; ++q ) {
					pstr_list.add( String.format("\"-qSize %d -delta %d -dist %s\"", q, d, dist ) );
				}
				param_list[d] = new String[pstr_list.size()];
				pstr_list.toArray( param_list[d] );
			}

			int answer;
			for ( int d=0; d<param_list.length; ++d ) {
				for ( int i=0; i<param_list[d].length; ++i ) {
					if ( isSelfJoin ) answer = ANS_SEQ_SELF_DELTA.get(dist)[d];
					else answer = ANS_SEQ_NONSELF_DELTA.get(dist)[d];
					runAlgorithm( param_list[d][i], answer, isSelfJoin );
				}
			}
		}
	}

	@Ignore
	public void testJoinDeltaVar() throws IOException, ParseException {
		args[1] = "JoinDeltaVar";
		for ( String dist : dist_list ) {
			String[][] param_list = new String[3][];
			for ( int d=0; d<3; ++d ) {
				List<String> pstr_list = new ArrayList<>();
				for ( int k=1; k<=3; ++k ) {
					for ( int q=1; q<=3; ++q ) {
						pstr_list.add( String.format("\"-K %d -qSize %d -delta %d -dist %s\"", k, q, d, dist ) );
					}
				}
				param_list[d] = new String[pstr_list.size()];
				pstr_list.toArray( param_list[d] );
			}

			int answer;
			for ( int d=0; d<param_list.length; ++d ) {
				for ( int i=0; i<param_list[d].length; ++i ) {
					if ( isSelfJoin ) answer = ANS_SEQ_SELF_DELTA.get(dist)[d];
					else answer = ANS_SEQ_NONSELF_DELTA.get(dist)[d];
					runAlgorithm( param_list[d][i], answer, isSelfJoin );
				}
			}
		}
	}

	@Ignore
	public void testJoinDeltaVarBK() throws IOException, ParseException {
		args[1] = "JoinDeltaVarBK";
		for ( String dist : dist_list ) {
			String[][] param_list = new String[3][];
			for ( int d=0; d<3; ++d ) {
				List<String> pstr_list = new ArrayList<>();
				for ( int k=1; k<=3; ++k ) {
					for ( int q=1; q<=3; ++q ) {
						pstr_list.add( String.format("\"-K %d -qSize %d -delta %d -dist %s -sampleB 0.01\"", k, q, d, dist ) );
					}
				}
				param_list[d] = new String[pstr_list.size()];
				pstr_list.toArray( param_list[d] );
			}

			int answer;
			for ( int d=0; d<param_list.length; ++d ) {
				for ( int i=0; i<param_list[d].length; ++i ) {
					if ( isSelfJoin ) answer = ANS_SEQ_SELF_DELTA.get(dist)[d];
					else answer = ANS_SEQ_NONSELF_DELTA.get(dist)[d];
					runAlgorithm( param_list[d][i], answer, isSelfJoin );
				}
			}
		}
	}
}
