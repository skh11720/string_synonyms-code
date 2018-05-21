package snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.seq;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.tools.Param;
import snu.kdd.synonym.synonymRev.tools.StatContainer;

public class ParamPQFilterDP extends Param {
	private static final Options argOptions;

	static {
		Options options = new Options();
		options.addOption( "K", true, "Represents a number of indexes" );
		options.addOption( "qSize", true, "Q gram size" );
		options.addOption( "useLF", true, "use length filtering (default: true)" );
		options.addOption( "recurse", true, "the way of recursion in DP (defualt: BU" );
		options.addOption( "mode", true, "mode" );

		argOptions = options;
	}

	public static ParamPQFilterDP parseArgs( String[] args, StatContainer stat, Query query ) throws IOException, ParseException {
		CommandLineParser parser = new DefaultParser();
		ParamPQFilterDP param = new ParamPQFilterDP();

		CommandLine cmd = parser.parse( argOptions, args );

		stat.add( cmd );

		if( cmd.hasOption( "K" ) ) {
			param.indexK = Integer.parseInt( cmd.getOptionValue( "K" ) );
		}

		if( cmd.hasOption( "qSize" ) ) {
			param.qgramSize = Integer.parseInt( cmd.getOptionValue( "qSize" ) );
		}
		
		if ( cmd.hasOption( "useLF" ) ) {
			param.useLF = Boolean.parseBoolean( cmd.getOptionValue( "useLF" ) );
		}
		else param.useLF = true;
		
//		if ( cmd.hasOption( "recurse" ) ) {
//			String val = cmd.getOptionValue( "recurse" );
//			if ( val.equals( "TD" )) param.useTopDown = true;
//			else if (val.equals( "BU" )) param.useTopDown = false;
//			else throw new RuntimeException("invalid argument for option recurse: "+val);
//		}
//		else throw new RuntimeException("the way of recursion has to be specified.");

		if( cmd.hasOption( "mode" ) ) {
			param.mode = cmd.getOptionValue( "mode" );
//			if ( param.mode.equals( "naive" ) );
			if ( param.mode.equals("dp1") );
//			else if ( param.mode.equals("dp2") );
			else if ( param.mode.equals("dp3") );
			else throw new RuntimeException("Unexpected mode: "+param.mode );
		}
		else throw new RuntimeException("mode is not specified");
		
		return param;
	}
	
	public Boolean useLF;
	public Boolean useTopDown = false;
	public String mode;
}
