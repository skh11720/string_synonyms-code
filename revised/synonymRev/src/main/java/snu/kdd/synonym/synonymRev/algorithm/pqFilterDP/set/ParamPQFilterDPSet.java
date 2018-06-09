package snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.set;

import java.io.IOException;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.tools.Param;
import snu.kdd.synonym.synonymRev.tools.StatContainer;

public class ParamPQFilterDPSet extends Param {
	private static final Options argOptions;

	static {
		Options options = new Options();
		options.addOption( "K", true, "Number of indexing for a record" );
		options.addOption( "verify", true, "Verification" );
		options.addOption( "rc", true, "Rule compression" );

		argOptions = options;
	}

	public static ParamPQFilterDPSet parseArgs( String[] args, StatContainer stat, Query query ) throws IOException, ParseException {
		CommandLineParser parser = new DefaultParser();
		ParamPQFilterDPSet param = new ParamPQFilterDPSet();

		CommandLine cmd = parser.parse( argOptions, args );

		stat.add( cmd );
		
		if ( cmd.hasOption( "K" ) ) {
			param.K = Integer.parseInt( cmd.getOptionValue( "K" ) );
			if ( param.K < 0 ) throw new RuntimeException("K must be larger than 0, not "+param.K);
		}

		if( cmd.hasOption( "verify" ) ) {
			param.verifier = cmd.getOptionValue( "verify" );
			Set<String> possibleValues = new ObjectOpenHashSet<String>( new String[] {"TD", "MIT_GR"} );
			if ( param.verifier.startsWith( "GR" ) ) {
				param.beamWidth = Integer.parseInt( param.verifier.substring( 2 ) );
			}
			else if ( !possibleValues.contains( param.verifier ) )
				throw new RuntimeException("unexpected value for option -verify: "+param.verifier);
		}
		else throw new RuntimeException("verify is not specified.");
		
		if ( cmd.hasOption( "rc" ) ) {
			param.ruleComp = Boolean.getBoolean( cmd.getOptionValue( "rc" ) );
		}
		return param;
	}
	
	public String verifier;
	public int beamWidth;
	public int K;
	public boolean ruleComp;
}
