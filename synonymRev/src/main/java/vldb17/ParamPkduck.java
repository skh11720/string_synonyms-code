package vldb17;

import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public class ParamPkduck {
	private static final Options argOptions;

	static {
		Options options = new Options();
		options.addOption( "ord", true, "Global order of pos q-grams" );
		options.addOption( "verify", true, "Verification method" );
		options.addOption( "rc", true, "Use rule compression" );
		options.addOption( "lf", true, "Use the length filter" );

		argOptions = options;
	}

	public ParamPkduck( String[] args ) throws ParseException {
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse( argOptions, args );

		if( cmd.hasOption( "ord" ) ) {
			Set<String> possibleValues = new ObjectOpenHashSet<String>( new String[] {"PF", "TF", "FF"} );
			globalOrder = cmd.getOptionValue( "ord" );
			if ( !possibleValues.contains( globalOrder ) )
				throw new RuntimeException( "unexpected value for option -ord: "+globalOrder );
		}

		if( cmd.hasOption( "verify" ) ) {
			Set<String> possibleValues = new ObjectOpenHashSet<String>( new String[] {"naive", "greedy", "TD"} );
			verifier = cmd.getOptionValue( "verify" );
			if ( !possibleValues.contains( verifier ) )
				throw new RuntimeException("unexpected value for option -verify: "+verifier);
		}
		
		if ( cmd.hasOption( "rc" ) ) {
			useRuleComp = Boolean.valueOf( cmd.getOptionValue( "rc" ) );
			if ( useRuleComp == null )
				throw new RuntimeException("unexpected value for option -rc: "+useRuleComp);
		}
		else throw new RuntimeException("the vaule for option -rc is not specified.");
		
		if ( cmd.hasOption( "lf" ) ) {
			useLF = Boolean.valueOf( cmd.getOptionValue( "lf" ) );
			if ( useLF == null )
				throw new RuntimeException("unexpected value for option -lf: "+useLF );
		}
		else throw new RuntimeException("the vaule for option -lf is not specified.");
	}

	public String globalOrder = null;
	public String verifier = null;
	public Boolean useRuleComp = null;
	public Boolean useLF = null;
}
