package vldb17;

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

public class ParamPkduck extends Param {
	private static final Options argOptions;

	static {
		Options options = new Options();
		options.addOption( "ord", true, "Global order of pos q-grams" );
		options.addOption( "verify", true, "Verification method" );
		options.addOption( "rc", true, "Use rule compression" );

		argOptions = options;
	}

	public static ParamPkduck parseArgs( String[] args, StatContainer stat, Query query ) throws IOException, ParseException {
		CommandLineParser parser = new DefaultParser();
		ParamPkduck param = new ParamPkduck();

		CommandLine cmd = parser.parse( argOptions, args );

		stat.add( cmd );

		if( cmd.hasOption( "ord" ) ) {
			Set<String> possibleValues = new ObjectOpenHashSet<String>( new String[] {"PF", "TF", "FF"} );
			param.globalOrder = cmd.getOptionValue( "ord" );
			if ( !possibleValues.contains( param.globalOrder ) )
				throw new RuntimeException( "unexpected value for option -ord: "+param.globalOrder );
		}

		if( cmd.hasOption( "verify" ) ) {
			Set<String> possibleValues = new ObjectOpenHashSet<String>( new String[] {"naive", "greedy"} );
			param.verifier = cmd.getOptionValue( "verify" );
			if ( !possibleValues.contains( param.verifier ) )
				throw new RuntimeException("unexpected value for option -verify: "+param.verifier);
		}
		
		if ( cmd.hasOption( "rc" ) ) {
			param.useRuleComp = Boolean.valueOf( cmd.getOptionValue( "rc" ) );
			if ( param.useRuleComp == null )
				throw new RuntimeException("unexpected value for option -rc: "+param.useRuleComp);
		}
		else throw new RuntimeException("the vaule for option -rc is not specified.");

		return param;
	}

	public String globalOrder = null;
	public String verifier = null;
	public Boolean useRuleComp = null;
}
