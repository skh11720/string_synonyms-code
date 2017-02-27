package snu.kdd.synonym.driver;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.algorithm.JoinD2GramNoIntvlTree;
import snu.kdd.synonym.algorithm.JoinH2GramNoIntvlTree;
import snu.kdd.synonym.algorithm.JoinNaive2;
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

		options.addOption( Option.builder( "algorithm" ).argName( "Algorithm" )
				.desc( "JoinNaive2: \n" + "H2GramNoIntvlTree: \n" + "D2GramNoIntvlTree: \n" ).build() );

		options.addOption( "check", false, "Check results" );
		options.addOption( "additional", true, "Additional input arguments" );
		argOptions = options;
	}

	private enum AlgorithmName {
		JoinNaive2, H2GramNoIntvlTree, D2GramNoIntvlTree

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

		AlgorithmName algorithm = AlgorithmName.valueOf( cmd.getOptionValue( "v" ) );

		switch( algorithm ) {
		case JoinNaive2:
			alg = new JoinNaive2( rulePath, dataOnePath, dataTwoPath, outputPath );
			break;
		case H2GramNoIntvlTree:
			alg = new JoinH2GramNoIntvlTree( rulePath, dataOnePath, dataTwoPath, outputPath );
			break;
		case D2GramNoIntvlTree:
			alg = new JoinD2GramNoIntvlTree( rulePath, dataOnePath, dataTwoPath, outputPath );
			break;
		default:
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "[OPTIONS]", argOptions, true );
			System.exit( 0 );
			break;
		}

		StopWatch totalTime = StopWatch.getWatchStarted( "Total Time" );
		alg.run( cmd.getOptionValue( "additional", "" ).split( " " ), stat );
		totalTime.stop();

		stat.add( totalTime );

		alg.printStat();
	}
}
