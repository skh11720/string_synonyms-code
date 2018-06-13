package snu.kdd.synonym.synonymRev;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.internal.runners.TestMethod;
import org.junit.runners.MethodSorters;

import junit.framework.TestSuite;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.tools.StatContainer;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AlgorithmTest {
	
	private static StatContainer stat = new StatContainer();
	private static Query query;
	private static String[] args = {"-algorithm", "", "-oneSideJoin", "True", "-additional", ""};
	private static final int answer = 10144;
	
	@BeforeClass
	public static void initialize() throws ParseException, IOException {
		String[] args = ("-dataOnePath D:\\ghsong\\data\\aol\\splitted\\aol_10000_data.txt " + 
				"-dataTwoPath D:\\ghsong\\data\\aol\\splitted\\aol_10000_data.txt " + 
				"-rulePath D:\\ghsong\\data\\wordnet\\rules.noun " + 
				"-outputPath output -algorithm * -oneSideJoin True -additional *").split( " ", 14 );
		
		CommandLine cmd = App.parseInput( args );
		query = App.getQuery( cmd );
	}
	
	private static void runAlgorithm( String param ) throws ParseException, IOException {
		args[5] = param;
		CommandLine cmd = App.parseInput( args );
		AlgorithmTemplate alg = App.getAlgorithm( query, stat, cmd );
		alg.writeResult = false;
		App.run( alg, query, cmd );
		assertEquals( answer, alg.rsltSize );
	}

	@Ignore
	public void testJoinMH() throws IOException, ParseException {
		args[1] = "JoinMH";
		String[] param_list = {
				"\"-K 1 -qSize 1\"",
				"\"-K 1 -qSize 2\"",
				"\"-K 2 -qSize 1\""
		};
		for ( String param : param_list ) runAlgorithm( param );
	}

	@Ignore
	public void testJoinMHNaive() throws IOException, ParseException {
		args[1] = "JoinMHNaive";
		String[] param_list = {
				"\"-K 1 -qSize 1 -sample 0.01\"",
				"\"-K 1 -qSize 2 -sample 0.01\"",
				"\"-K 2 -qSize 1 -sample 0.01\""
		};
		for ( String param : param_list ) runAlgorithm( param );
	}

	@Ignore
	public void testJoinMHNaiveThres() throws IOException, ParseException {
		args[1] = "JoinMHNaiveThres";
		String[] param_list = {
				"\"-K 1 -qSize 1 -t 300\"",
				"\"-K 1 -qSize 2 -t 300\"",
				"\"-K 2 -qSize 1 -t 300\""
		};
		for ( String param : param_list ) runAlgorithm( param );
	}
	
	@Ignore
	public void testJoinMin() throws ParseException, IOException {
		args[1] = "JoinMin";
		String[] param_list = {
				"\"-K 1 -qSize 1\"",
				"\"-K 1 -qSize 2\"",
				"\"-K 2 -qSize 1\""
		};
		for ( String param : param_list ) runAlgorithm( param );
	}
	
	@Ignore
	public void testJoinMinNaive() throws ParseException, IOException {
		args[1] = "JoinMinNaive";
		String[] param_list = {
				"\"-K 1 -qSize 1 -sample 0.01\"",
				"\"-K 1 -qSize 2 -sample 0.01\"",
				"\"-K 2 -qSize 1 -sample 0.01\""
		};
		for ( String param : param_list ) runAlgorithm( param );
	}
	
	@Ignore
	public void testJoinMinNaiveThres() throws ParseException, IOException {
		args[1] = "JoinMinNaiveThres";
		String[] param_list = {
				"\"-K 1 -qSize 1 -t 300\"",
				"\"-K 1 -qSize 2 -t 300\"",
				"\"-K 2 -qSize 1 -t 300\""
		};
		for ( String param : param_list ) runAlgorithm( param );
	}

	@Test
	public void testJoinMHDP() throws IOException, ParseException {
		args[1] = "JoinMHDP";
		String[] param_list = {
				"\"-K 1 -qSize 1 -mode dp1 -index FF\"",
				"\"-K 1 -qSize 2 -mode dp1 -index FF\"",
				"\"-K 2 -qSize 1 -mode dp1 -index FF\""
		};
		for ( String param : param_list ) runAlgorithm( param );
	}

	@Ignore
	public void testJoinMHNaiveDP() throws IOException, ParseException {
		args[1] = "JoinMHNaiveDP";
		String[] param_list = {
				"\"-K 1 -qSize 1 -sample 0.01\"",
				"\"-K 1 -qSize 2 -sample 0.01\"",
				"\"-K 2 -qSize 1 -sample 0.01\""
		};
		for ( String param : param_list ) runAlgorithm( param );
	}

	@Ignore
	public void testJoinMHNaiveThresDP() throws IOException, ParseException {
		args[1] = "JoinMHNaiveThresDP";
		String[] param_list = {
				"\"-K 1 -qSize 1 -t 300\"",
				"\"-K 1 -qSize 2 -t 300\"",
				"\"-K 2 -qSize 1 -t 300\""
		};
		for ( String param : param_list ) runAlgorithm( param );
	}
	
	@Ignore
	public void testJoinMinDP() throws ParseException, IOException {
		args[1] = "JoinMinDP";
		String[] param_list = {
				"\"-K 1 -qSize 1 -mode dp1\"",
				"\"-K 1 -qSize 2 -mode dp1\"",
				"\"-K 2 -qSize 1 -mode dp1\""
		};
		for ( String param : param_list ) runAlgorithm( param );
	}
	
	@Ignore
	public void testJoinMinNaiveDP() throws ParseException, IOException {
		args[1] = "JoinMinNaiveDP";
		String[] param_list = {
				"\"-K 1 -qSize 1 -sample 0.01\"",
				"\"-K 1 -qSize 2 -sample 0.01\"",
				"\"-K 2 -qSize 1 -sample 0.01\""
		};
		for ( String param : param_list ) runAlgorithm( param );
	}
	
	@Ignore
	public void testJoinMinNaiveThresDP() throws ParseException, IOException {
		args[1] = "JoinMinNaiveThresDP";
		String[] param_list = {
				"\"-K 1 -qSize 1 -t 300\"",
				"\"-K 1 -qSize 2 -t 300\"",
				"\"-K 2 -qSize 1 -t 300\""
		};
		for ( String param : param_list ) runAlgorithm( param );
	}
}
