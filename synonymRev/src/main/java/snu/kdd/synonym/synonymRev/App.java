package snu.kdd.synonym.synonymRev;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.synonymRev.algorithm.AlgorithmFactory;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmInterface;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.tools.AlgorithmResultQualityEvaluator;
import snu.kdd.synonym.synonymRev.tools.Util;

public class App {
	private static Options argOptions;
	private static boolean uploadOn;
	private static String groundPath = null;
	
	static {
		argOptions = new Options();
		argOptions.addOption( "rulePath", true, "rule path" );
		argOptions.addOption( "dataOnePath", true, "data one path" );
		argOptions.addOption( "dataTwoPath", true, "data two path" );
		argOptions.addOption( "groundPath", true, "groundtruth path" );
		argOptions.addOption( "outputPath", true, "output path" );
		argOptions.addOption( "oneSideJoin", true, "One side join" );
		argOptions.addOption( "algorithm", true, "Algorithm" );
		argOptions.addOption( "upload", true, "Upload experiments" );
		argOptions.addOption( "additional", true, "Additional input arguments" );
	}
	
	public static void main( String args[] ) throws IOException, ParseException {
		CommandLine cmd = parseInput( args );
		Query query = Query.parseQuery( cmd );
		AlgorithmInterface alg = AlgorithmFactory.getAlgorithmInstance(cmd);
		alg.run(query);
		AlgorithmResultQualityEvaluator.evaluate(alg, query, groundPath);

		if (uploadOn) alg.writeJSON();
		printStat(alg);
		Util.printLog( alg.getName() + " finished" );
	}
	
	public static CommandLine parseInput( String args[] ) throws ParseException {
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse( argOptions, args, false );
		Util.printArgsError( cmd );
		uploadOn = Boolean.parseBoolean( cmd.getOptionValue( "upload" ) );
		groundPath = cmd.getOptionValue( "groundPath" );
		return cmd;
	}

	public static void printStat( AlgorithmInterface alg ) {
		System.out.println( "=============[" + alg.getName() + " stats" + "]=============" );
		alg.getStat().printResult();
		System.out.println(
				"==============" + new String( new char[ alg.getName().length() ] ).replace( "\0", "=" ) + "====================" );
	}
}
