package snu.kdd.synonym.synonymRev.tools;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.synonymRev.validator.TopDownHashSetSinglePath_DS;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class Param {
	private static final Options argOptions;

	static {
		Options options = new Options();
		options.addOption( "K", true, "Represents a number of indexes" );
		options.addOption( "qSize", true, "Q gram size" );

		argOptions = options;
	}

	public static Param parseArgs( String[] args, StatContainer stat ) throws IOException, ParseException {
		CommandLineParser parser = new DefaultParser();
		Param param = new Param();

		CommandLine cmd = parser.parse( argOptions, args );

		stat.add( cmd );

		if( cmd.hasOption( "n" ) ) {
			param.indexK = Integer.parseInt( cmd.getOptionValue( "K" ) );
		}

		if( cmd.hasOption( "qSize" ) ) {
			param.qgramSize = Integer.parseInt( cmd.getOptionValue( "qSize" ) );
		}

		param.validator = new TopDownHashSetSinglePath_DS();

		return param;
	}

	public int indexK = 2;
	public int qgramSize = 2;
	public Validator validator;
}
