package snu.kdd.synonym.driver;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.algorithm.JoinHybrid;
import snu.kdd.synonym.algorithm.JoinMH;
import snu.kdd.synonym.algorithm.JoinMin;
import snu.kdd.synonym.algorithm.JoinNaive1;
import snu.kdd.synonym.algorithm.JoinNaive2;
import snu.kdd.synonym.algorithm.SIJoin;
import snu.kdd.synonym.tools.StatContainer;
import snu.kdd.synonym.tools.StopWatch;
import snu.kdd.synonym.tools.Util;

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
		JoinNaive1, JoinNaive2, JoinMH, JoinMin, JoinHybrid, SIJoin
	}

	public static CommandLine parseInput( String args[] ) {

		CommandLineParser parser = new DefaultParser();

		CommandLine cmd = null;

		try {
			cmd = parser.parse( argOptions, args, false );
		}
		catch( ParseException e ) {
			e.printStackTrace();
			System.exit( 1 );
		}

		return cmd;
	}

	public static void main( String args[] ) throws IOException {
		CommandLine cmd = parseInput( args );

		Util.printArgsError( cmd );

		String rulePath = cmd.getOptionValue( "rulePath" );
		String dataOnePath = cmd.getOptionValue( "dataOnePath" );
		String dataTwoPath = cmd.getOptionValue( "dataTwoPath" );
		String outputPath = cmd.getOptionValue( "outputPath" );

		AlgorithmTemplate alg = null;

		StatContainer stat = new StatContainer();

		AlgorithmName algorithm = AlgorithmName.valueOf( cmd.getOptionValue( "algorithm" ) );

		StopWatch totalTime = StopWatch.getWatchStarted( "Total Time" );
		StopWatch initializeTime = StopWatch.getWatchStarted( "Initialize Time" );
		switch( algorithm ) {
		case JoinNaive1:
			alg = new JoinNaive1( rulePath, dataOnePath, dataTwoPath, outputPath );
			break;
		case JoinNaive2:
			alg = new JoinNaive2( rulePath, dataOnePath, dataTwoPath, outputPath );
			break;
		case JoinMH:
			alg = new JoinMH( rulePath, dataOnePath, dataTwoPath, outputPath );
			break;
		case JoinMin:
			alg = new JoinMin( rulePath, dataOnePath, dataTwoPath, outputPath );
			break;
		case JoinHybrid:
			alg = new JoinHybrid( rulePath, dataOnePath, dataTwoPath, outputPath );
			break;
		case SIJoin:
			alg = new SIJoin( rulePath, dataOnePath, dataTwoPath, outputPath );
			break;
		default:
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "[OPTIONS]", argOptions, true );
			System.exit( 0 );
			break;
		}
		initializeTime.stop();

		stat.add( cmd );

		alg.run( cmd.getOptionValue( "additional", "" ).split( " " ), stat );
		totalTime.stop();

		stat.add( initializeTime );
		stat.add( totalTime );

		alg.printStat();

		BufferedWriter bw_json = new BufferedWriter( new FileWriter( "json/" + alg.getName() + "_"
				+ new java.text.SimpleDateFormat( "yyyyMMdd_HHmmss_z" ).format( new java.util.Date() ) + ".txt", true ) );
		bw_json.write( stat.toJson() );
		bw_json.close();

		stat.resultWriter( "result/" + alg.getName() + "_" + alg.getVersion() );

		System.err.println( alg.getName() + " finished" );
	}
}
