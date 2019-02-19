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
import snu.kdd.synonym.synonymRev.tools.Util;

public class App {
	private static Options argOptions;
	
	public static void main( String args[] ) throws IOException, ParseException {

		CommandLine cmd = parseInput( args );
		Util.printArgsError( cmd );

		Query query = Query.parseQuery( cmd );
		AlgorithmInterface alg = AlgorithmFactory.getAlgorithmInstance(query, cmd);
		alg.run();

		boolean upload = Boolean.parseBoolean( cmd.getOptionValue( "upload" ) );
		if( upload ) {
			alg.writeJSON();
		}

		Util.printLog( alg.getName() + " finished" );
	}
	
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
			options.addOption( "upload", true, "Upload experiments" );

			options.addOption( "additional", true, "Additional input arguments" );

			argOptions = options;
		}

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse( argOptions, args, false );
		return cmd;
	}
}
