package snu.kdd.synonym.driver;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.algorithm.JoinH2GramNoIntvlTree;
import snu.kdd.synonym.tools.StopWatch;
import snu.kdd.synonym.tools.Util;
import tools.Algorithm;

public class Driver {
	public static CommandLine parseInput( String args[] ) {
		Options options = new Options();
		options.addOption( "rulePath", true, "rule path" );
		options.addOption( "dataOnePath", true, "data one path" );
		options.addOption( "dataTwoPath", true, "data two path" );

		options.addOption( "verbose", false, "verbose" );

		options.addOption( "baseline", false, "Baseline algorithm" );
		options.addOption( "H2GramNoIntvlTree", false, "JoinH2GramNoIntvlTree algorithm" );
		options.addOption( "hybrid", false, "Hybrid algorithm" );

		options.addOption( "check", false, "Check results" );

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;

		try {
			cmd = parser.parse( options, args );
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

		Algorithm alg = null;

		if( cmd.hasOption( "H2GramNoIntvlTree" ) ) {
			alg = new JoinH2GramNoIntvlTree( rulePath, dataOnePath, dataTwoPath );
		}

		StopWatch totalTime = StopWatch.getWatchStarted( "Total Time" );
		alg.run( cmd.getArgs() );
		totalTime.stop();
	}
}
