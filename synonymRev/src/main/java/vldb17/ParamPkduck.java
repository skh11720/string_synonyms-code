package vldb17;

import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.tools.AbstractParam;

public class ParamPkduck extends AbstractParam {
	private static final Options argOptions;

	static {
		Options options = new Options();
		options.addOption( "ord", true, "Global order of pos q-grams" );
		options.addOption( "verify", true, "Verification method" );
		options.addOption( "rc", true, "Use rule compression" );
		options.addOption( "lf", true, "Use the length filter" );
		options.addOption( "theta", true, "similarity threshold" );

		argOptions = options;
	}

	public ParamPkduck( String[] args ) throws ParseException {
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse( argOptions, args );

		if( cmd.hasOption( "ord" ) ) {
			Set<String> possibleValues = new ObjectOpenHashSet<String>( new String[] {"PF", "TF", "FF"} );
			String globalOrder = cmd.getOptionValue( "ord" );
			if ( !possibleValues.contains( globalOrder ) )
				throw new RuntimeException( "unexpected value for option -ord: "+globalOrder );
			mapParamS.put("ord", globalOrder);
		}

		if( cmd.hasOption( "verify" ) ) {
			Set<String> possibleValues = new ObjectOpenHashSet<String>( new String[] {"naive", "greedy", "TD"} );
			String verifier = cmd.getOptionValue( "verify" );
			if ( !possibleValues.contains( verifier ) )
				throw new RuntimeException("unexpected value for option -verify: "+verifier);
			mapParamS.put("verify", verifier);
		}
		
		if ( cmd.hasOption( "theta" ) ) {
			double theta = Double.parseDouble( cmd.getOptionValue( "theta" ) );
			mapParamD.put("theta", theta);
		}

		boolean rc = false;
		if ( cmd.hasOption( "rc" ) ) rc = Boolean.parseBoolean( cmd.getOptionValue( "rc" ) );
		mapParamB.put("rc", rc);
		
		boolean useLF = true;
		if ( cmd.hasOption( "lf" ) ) useLF = Boolean.valueOf( cmd.getOptionValue( "lf" ) );
		mapParamB.put("useLF", useLF);
	}
}
