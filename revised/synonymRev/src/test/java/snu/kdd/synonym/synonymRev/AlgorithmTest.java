package snu.kdd.synonym.synonymRev;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.tools.StatContainer;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AlgorithmTest {
	
//	private static Query query;
	private static String[] args = {"-algorithm", "", "-oneSideJoin", "True", "-additional", ""};
	
	public static Query getQuery() throws ParseException, IOException {
		String osName = System.getProperty( "os.name" );
		final String dataOnePath, dataTwoPath, rulePath;
		if ( osName.startsWith( "Windows" ) ) {
			dataOnePath = "D:\\ghsong\\data\\aol\\splitted\\aol_1000_data.txt";
			dataTwoPath = "D:\\ghsong\\data\\aol\\splitted\\aol_1000_data.txt";
			rulePath = "D:\\ghsong\\data\\wordnet\\rules.noun";
		}
		else if ( osName.startsWith( "Linux" ) ) {
			dataOnePath = "run/data_store/aol/splitted/aol_10000_data.txt";
			dataTwoPath = "run/data_store/aol/splitted/aol_10000_data.txt";
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
	
//	private static void runAlgorithm( String param ) throws ParseException, IOException {
//		args[5] = param;
//		CommandLine cmd = App.parseInput( args );
//		AlgorithmTemplate alg = App.getAlgorithm( query, stat, cmd );
//		alg.writeResult = false;
//		App.run( alg, query, cmd );
//		assertEquals( 1014, alg.rsltSize );
//	}

	private static void runAlgorithm( String param, int answer ) throws ParseException, IOException {
		Record.initStatic();
		Rule.initStatic();
		Query query = AlgorithmTest.getQuery();
		System.out.println( "runAlgorithms, before run" );
		System.out.println( "searchedSet" );
		for ( int i=0; i<10; ++i ) System.out.println( query.searchedSet.getRecord( i ) );
		StatContainer stat = new StatContainer();
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
		for ( String param : param_list ) runAlgorithm( param, 1014 );
	}

	@Ignore
	public void testJoinMHNaive() throws IOException, ParseException {
		args[1] = "JoinMHNaive";
		String[] param_list = {
				"\"-K 1 -qSize 1 -sample 0.01\"",
				"\"-K 1 -qSize 2 -sample 0.01\"",
				"\"-K 2 -qSize 1 -sample 0.01\""
		};
		for ( String param : param_list ) runAlgorithm( param, 1014 );
	}

	@Ignore
	public void testJoinMHNaiveThres() throws IOException, ParseException {
		args[1] = "JoinMHNaiveThres";
		String[] param_list = {
				"\"-K 1 -qSize 1 -t 300\"",
				"\"-K 1 -qSize 2 -t 300\"",
				"\"-K 2 -qSize 1 -t 300\""
		};
		for ( String param : param_list ) runAlgorithm( param, 1014 );
	}
	
	@Ignore
	public void testJoinMin() throws ParseException, IOException {
		args[1] = "JoinMin";
		String[] param_list = {
				"\"-K 1 -qSize 1\"",
				"\"-K 1 -qSize 2\"",
				"\"-K 2 -qSize 1\""
		};
		for ( String param : param_list ) runAlgorithm( param, 1014 );
	}
	
	@Ignore
	public void testJoinMinNaive() throws ParseException, IOException {
		args[1] = "JoinMinNaive";
		String[] param_list = {
				"\"-K 1 -qSize 1 -sample 0.01\"",
				"\"-K 1 -qSize 2 -sample 0.01\"",
				"\"-K 2 -qSize 1 -sample 0.01\""
		};
		for ( String param : param_list ) runAlgorithm( param, 1014 );
	}
	
	@Ignore
	public void testJoinMinNaiveThres() throws ParseException, IOException {
		args[1] = "JoinMinNaiveThres";
		String[] param_list = {
				"\"-K 1 -qSize 1 -t 300\"",
				"\"-K 1 -qSize 2 -t 300\"",
				"\"-K 2 -qSize 1 -t 300\""
		};
		for ( String param : param_list ) runAlgorithm( param, 1014 );
	}

	@Ignore
	public void testJoinMHDP() throws IOException, ParseException {
		args[1] = "JoinMHDP";
		String[] param_list = {
				"\"-K 1 -qSize 1 -mode dp1 -index FF\"",
				"\"-K 1 -qSize 2 -mode dp1 -index FF\"",
				"\"-K 2 -qSize 1 -mode dp1 -index FF\""
		};
		for ( String param : param_list ) runAlgorithm( param, 1014 );
	}

	@Ignore
	public void testJoinMHNaiveDP() throws IOException, ParseException {
		args[1] = "JoinMHNaiveDP";
		String[] param_list = {
				"\"-K 1 -qSize 1 -sample 0.01\"",
				"\"-K 1 -qSize 2 -sample 0.01\"",
				"\"-K 2 -qSize 1 -sample 0.01\""
		};
		for ( String param : param_list ) runAlgorithm( param, 1014 );
	}

	@Ignore
	public void testJoinMHNaiveThresDP() throws IOException, ParseException {
		args[1] = "JoinMHNaiveThresDP";
		String[] param_list = {
				"\"-K 1 -qSize 1 -t 300\"",
				"\"-K 1 -qSize 2 -t 300\"",
				"\"-K 2 -qSize 1 -t 300\""
		};
		for ( String param : param_list ) runAlgorithm( param, 1014 );
	}
	
	@Ignore
	public void testJoinMinDP() throws ParseException, IOException {
		args[1] = "JoinMinDP";
		String[] param_list = {
				"\"-K 1 -qSize 1 -mode dp1\"",
				"\"-K 1 -qSize 2 -mode dp1\"",
				"\"-K 2 -qSize 1 -mode dp1\""
		};
		for ( String param : param_list ) runAlgorithm( param, 1014 );
	}
	
	@Ignore
	public void testJoinMinNaiveDP() throws ParseException, IOException {
		args[1] = "JoinMinNaiveDP";
		String[] param_list = {
				"\"-K 1 -qSize 1 -sample 0.01\"",
				"\"-K 1 -qSize 2 -sample 0.01\"",
				"\"-K 2 -qSize 1 -sample 0.01\""
		};
		for ( String param : param_list ) runAlgorithm( param, 1014 );
	}
	
	@Ignore
	public void testJoinMinNaiveThresDP() throws ParseException, IOException {
		args[1] = "JoinMinNaiveThresDP";
		String[] param_list = {
				"\"-K 1 -qSize 1 -t 300\"",
				"\"-K 1 -qSize 2 -t 300\"",
				"\"-K 2 -qSize 1 -t 300\""
		};
		for ( String param : param_list ) runAlgorithm( param, 1014 );
	}
	
	@Test
	public void testJoinPkduck() throws ParseException, IOException {
		args[1] = "JoinPkduck";
		String[] param_list = {
				"\"-ord FF -verify naive -rc false\"",
				"\"-ord FF -verify naive -rc true\""
		};
		for ( String param : param_list ) runAlgorithm( param, 1014 );
	}
}
