package snu.kdd.synonym.synonymRev;

import java.io.IOException;
import java.util.Date;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate.AlgorithmName;
import snu.kdd.synonym.synonymRev.algorithm.JoinBK;
import snu.kdd.synonym.synonymRev.algorithm.JoinBK_Split;
import snu.kdd.synonym.synonymRev.algorithm.JoinMH;
import snu.kdd.synonym.synonymRev.algorithm.JoinMH_Split;
import snu.kdd.synonym.synonymRev.algorithm.JoinMin;
import snu.kdd.synonym.synonymRev.algorithm.JoinNaive;
import snu.kdd.synonym.synonymRev.algorithm.JoinNaive_Split;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.tools.Util;

public class App {
	private static Options argOptions;

	public static CommandLine parseInput( String args[] ) throws ParseException {
		if( argOptions == null ) {
			Options options = new Options();
			options.addOption( "rulePath", true, "rule path" );
			options.addOption( "dataOnePath", true, "data one path" );
			options.addOption( "dataTwoPath", true, "data two path" );
			options.addOption( "outputPath", true, "output path" );
			options.addOption( "oneSideJoin", true, "One side join" );
			options.addOption( "algorithm", true, "Algorithm" );
			options.addOption( "split", false, "Split datasets" );

			options.addOption( "additional", true, "Additional input arguments" );

			argOptions = options;
		}

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse( argOptions, args, false );
		return cmd;
	}

	public static void main( String args[] ) throws IOException, ParseException {

		CommandLine cmd = null;
		try {
			cmd = parseInput( args );
		}
		catch( ParseException e ) {
			e.printStackTrace();
			throw e;
		}

		Util.printArgsError( cmd );

		String rulePath = cmd.getOptionValue( "rulePath" );
		String dataOnePath = cmd.getOptionValue( "dataOnePath" );
		String dataTwoPath = cmd.getOptionValue( "dataTwoPath" );
		String outputPath = cmd.getOptionValue( "outputPath" );
		Boolean oneSideJoin = Boolean.parseBoolean( cmd.getOptionValue( "oneSideJoin" ) );

		StopWatch totalTime = StopWatch.getWatchStarted( "Result_0_Total_Time" );
		StopWatch initializeTime = StopWatch.getWatchStarted( "Result_1_Initialize_Time" );

		Query query = new Query( rulePath, dataOnePath, dataTwoPath, oneSideJoin, outputPath );

		AlgorithmTemplate alg = null;
		AlgorithmName algorithmName = AlgorithmName.valueOf( cmd.getOptionValue( "algorithm" ) );
		StatContainer stat = new StatContainer();

		boolean split = cmd.hasOption( "split" );

		switch( algorithmName ) {
		case JoinNaive:
			if( split ) {
				alg = new JoinNaive_Split( query, stat );
			}
			else {
				alg = new JoinNaive( query, stat );
			}
			break;
		case JoinMH:
			if( split ) {
				alg = new JoinMH_Split( query, stat );
			}
			else {
				alg = new JoinMH( query, stat );
			}
			break;
		case JoinBK:
			if( split ) {
				alg = new JoinBK_Split( query, stat );
			}
			else {
				alg = new JoinBK( query, stat );
			}
			break;
		case JoinMin:
			alg = new JoinMin( query, stat );
			break;
		default:
			Util.printLog( "Invalid algorithm " + algorithmName );
			System.exit( 0 );
			break;
		}

		stat.addPrimary( "Date", "\"" + new Date().toString() + "\"" );
		stat.add( cmd );

		initializeTime.stopAndAdd( stat );

		String additionalOptions = cmd.getOptionValue( "additional", "" );

		if( additionalOptions != null ) {
			String additionalArgs[] = additionalOptions.split( " " );
			alg.run( query, additionalArgs );
		}
		else {
			alg.run( query, null );
		}

		totalTime.stop();
		Util.printGCStats( stat );

		stat.addPrimary( totalTime );
		alg.printStat();

		stat.resultWriter( "result/" + alg.getName() + "_" + alg.getVersion() );
		Util.printLog( alg.getName() + " finished" );
	}
}
