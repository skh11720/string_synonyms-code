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
import vldb17.PkduckIndex.GlobalOrder;

public class ParamPkduck extends Param {
	private static final Options argOptions;

	static {
		Options options = new Options();
		options.addOption( "globalOrder", true, "Global order of pos q-grams" );
		options.addOption( "verify", true, "Verification method" );

		argOptions = options;
	}

	public static ParamPkduck parseArgs( String[] args, StatContainer stat, Query query ) throws IOException, ParseException {
		CommandLineParser parser = new DefaultParser();
		ParamPkduck param = new ParamPkduck();

		CommandLine cmd = parser.parse( argOptions, args );

		stat.add( cmd );

		if( cmd.hasOption( "globalOrder" ) ) {
			param.globalOrder = GlobalOrder.valueOf( cmd.getOptionValue( "globalOrder" ) );
			if (param.globalOrder == null) {
				throw new RuntimeException( "globalOrder cannot be null");
			}
		}

		if( cmd.hasOption( "verify" ) ) {
			Set<String> possibleValues = new ObjectOpenHashSet<String>( new String[] {"naive", "greedy"} );
			param.verifier = cmd.getOptionValue( "verify" );
			if ( !possibleValues.contains( param.verifier ) )
				throw new RuntimeException("unexpected value for option -verify: "+param.verifier);
		}
		else {
			throw new RuntimeException("");
		}

		return param;
	}

	public GlobalOrder globalOrder = null;
	public String verifier = null;
}
