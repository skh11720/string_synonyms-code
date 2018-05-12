package snu.kdd.synonym.synonymRev.algorithm.pqFilterDP;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.tools.Param;
import snu.kdd.synonym.synonymRev.tools.StatContainer;

public class ParamPQFilterDPSet extends Param {
	private static final Options argOptions;

	static {
		Options options = new Options();
		options.addOption( "verify", true, "Verification" );

		argOptions = options;
	}

	public static ParamPQFilterDPSet parseArgs( String[] args, StatContainer stat, Query query ) throws IOException, ParseException {
		CommandLineParser parser = new DefaultParser();
		ParamPQFilterDPSet param = new ParamPQFilterDPSet();

		CommandLine cmd = parser.parse( argOptions, args );

		stat.add( cmd );

		if( cmd.hasOption( "verify" ) ) {
			param.verifier = cmd.getOptionValue( "verify" );
		}
		else throw new RuntimeException("verify is not specified.");
		return param;
	}
	
	public String verifier;
}
