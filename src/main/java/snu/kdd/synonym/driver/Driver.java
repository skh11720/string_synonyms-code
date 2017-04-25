package snu.kdd.synonym.driver;

import java.io.IOException;
import java.util.Date;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.algorithm.CheckQGram;
import snu.kdd.synonym.algorithm.JoinHybridOpt;
import snu.kdd.synonym.algorithm.JoinHybridOpt_Q;
import snu.kdd.synonym.algorithm.JoinHybridThres_Q;
import snu.kdd.synonym.algorithm.JoinMH_QL;
import snu.kdd.synonym.algorithm.JoinMin_Q;
import snu.kdd.synonym.algorithm.JoinNaive1;
import snu.kdd.synonym.algorithm.JoinNaive2;
import snu.kdd.synonym.algorithm.SIJoin;
import snu.kdd.synonym.algorithm.deprecated.JoinMH_QL_OLD;
import snu.kdd.synonym.algorithm.deprecated.JoinMin_Q_OLD;
import snu.kdd.synonym.data.DataInfo;
import snu.kdd.synonym.tools.StatContainer;
import snu.kdd.synonym.tools.StopWatch;
import snu.kdd.synonym.tools.Util;
import tools.DEBUG;

public class Driver {

	private static final Options argOptions;

	static {
		Options options = new Options();
		options.addOption( "rulePath", true, "rule path" );
		options.addOption( "dataOnePath", true, "data one path" );
		options.addOption( "dataTwoPath", true, "data two path" );
		options.addOption( "outputPath", true, "output path" );

		options.addOption( "verbose", false, "verbose" );

		options.addOption( Option.builder( "algorithm" ).argName( "Algorithm" ).numberOfArgs( 1 )
				.desc( "JoinNaive2: \n" + "JoinMH: \n" + "JoinMin: \n" + "JoinHybrid: \n" + "SIJoin: \n" ).build() );

		options.addOption( "check", false, "Check results" );
		options.addOption( "additional", true, "Additional input arguments" );
		argOptions = options;
	}

	private enum AlgorithmName {
		JoinNaive1,
		JoinNaive2,
		JoinMH,
		JoinMH_OLD,
		JoinMin,
		JoinMin_OLD,
		JoinHybridThres,
		JoinHybridOpt,
		SIJoin,
		DebugAlg,
		CheckQGram
	}

	public static CommandLine parseInput( String args[] ) throws ParseException {

		CommandLineParser parser = new DefaultParser();

		CommandLine cmd = null;

		cmd = parser.parse( argOptions, args, false );

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

		DataInfo dataInfo = new DataInfo( dataOnePath, dataTwoPath, rulePath );

		AlgorithmTemplate alg = null;

		StatContainer stat = new StatContainer();

		AlgorithmName algorithm = AlgorithmName.valueOf( cmd.getOptionValue( "algorithm" ) );

		StopWatch totalTime = StopWatch.getWatchStarted( "Result_0_Total_Time" );

		StopWatch initializeTime = null;
		if( DEBUG.ON ) {
			initializeTime = StopWatch.getWatchStarted( "Result_1_Initialize_Time" );
		}

		switch( algorithm ) {
		case JoinNaive1:
			alg = new JoinNaive1( rulePath, dataOnePath, dataTwoPath, outputPath, dataInfo );
			break;
		case JoinNaive2:
			alg = new JoinNaive2( rulePath, dataOnePath, dataTwoPath, outputPath, dataInfo );
			break;
		case JoinMH:
			alg = new JoinMH_QL( rulePath, dataOnePath, dataTwoPath, outputPath, dataInfo );
			break;

		case DebugAlg:
			alg = new JoinHybridOpt_Q( rulePath, dataOnePath, dataTwoPath, outputPath, dataInfo );
			break;
		case JoinMin:
			alg = new JoinMin_Q( rulePath, dataOnePath, dataTwoPath, outputPath, dataInfo );
			break;
		case JoinHybridThres:
			alg = new JoinHybridThres_Q( rulePath, dataOnePath, dataTwoPath, outputPath, dataInfo );
			break;
		case JoinHybridOpt:
			alg = new JoinHybridOpt( rulePath, dataOnePath, dataTwoPath, outputPath, dataInfo );
			break;
		case SIJoin:
			alg = new SIJoin( rulePath, dataOnePath, dataTwoPath, outputPath, dataInfo );
			break;

		case JoinMH_OLD:
			alg = new JoinMH_QL_OLD( rulePath, dataOnePath, dataTwoPath, outputPath, dataInfo );
			break;
		case JoinMin_OLD:
			alg = new JoinMin_Q_OLD( rulePath, dataOnePath, dataTwoPath, outputPath, dataInfo );
			break;

		case CheckQGram:
			alg = new CheckQGram( rulePath, dataOnePath, dataTwoPath, outputPath, dataInfo );
			break;

		default:
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "[OPTIONS]", argOptions, true );
			System.exit( 0 );
			break;
		}

		stat.addPrimary( "Date", "\"" + new Date().toString() + "\"" );
		stat.add( cmd );

		if( dataOnePath.equals( dataTwoPath ) ) {
			alg.setSelfJoin( true );
		}

		if( DEBUG.ON ) {
			initializeTime.stopAndAdd( stat );
		}

		alg.run( cmd.getOptionValue( "additional", "" ).split( " " ), stat );

		totalTime.stop();

		alg.printGCStats();

		stat.addPrimary( totalTime );

		alg.printStat();

		alg.writeJSON( dataInfo, cmd );

		stat.resultWriter( "result/" + alg.getName() + "_" + alg.getVersion() );

		System.err.println( alg.getName() + " finished" );
	}
}
