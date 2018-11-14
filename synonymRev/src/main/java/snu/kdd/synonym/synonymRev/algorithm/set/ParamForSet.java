package snu.kdd.synonym.synonymRev.algorithm.set;

import java.io.IOException;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.tools.AbstractParam;

public class ParamForSet extends AbstractParam {

	private static final Options argOptions;

    static {
        Options options = new Options();
        options.addOption( "K", true, "Number of indexing for a record" );
        options.addOption( "verify", true, "Verification" );
        options.addOption( "rc", true, "Rule compression" );

        argOptions = options;
    }

    public ParamForSet(String[] args) throws IOException, ParseException {
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse( argOptions, args );
		
		if ( cmd.hasOption( "K" ) ) {
			int indexK = Integer.parseInt( cmd.getOptionValue( "K" ) );
			if ( indexK < 0 ) throw new RuntimeException("K must be larger than 0, not "+indexK);
			mapParamI.put("indexK", indexK);
		}

		if( cmd.hasOption( "verify" ) ) {
			String verifier = cmd.getOptionValue( "verify" );
			Set<String> possibleValues = new ObjectOpenHashSet<String>( new String[] {"TD", "MIT_GR"} );
			if ( verifier.startsWith( "GR" ) ) {
				int beamWidth = Integer.parseInt( verifier.substring( 2 ) );
				mapParamI.put("beamWidth", beamWidth);
			}
			else if ( !possibleValues.contains( verifier ) )
				throw new RuntimeException("unexpected value for option -verify: "+verifier);
			mapParamS.put("verify", verifier);
		}
		else throw new RuntimeException("verify is not specified.");
    }
}
