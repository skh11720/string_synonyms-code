package snu.kdd.synonym.driver;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import tools.Util;

public class Driver {
	public static CommandLine parseInput( String args[] ) {
		Options options = new Options();
		options.addOption( "filePath", true, "file path" );
		options.addOption( "v", false, "verbose" );

		options.addOption( "baseline", false, "Debug baseline algorithm" );
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

	public static void main( String args[] ) {
		CommandLine cmd = parseInput( args );

		Util.printArgsError( cmd );
	}
}
