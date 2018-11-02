package snu.kdd.synonym.synonymRev.tools;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.validator.Naive;
import snu.kdd.synonym.synonymRev.validator.NaiveOneSide;
import snu.kdd.synonym.synonymRev.validator.TopDown;
import snu.kdd.synonym.synonymRev.validator.TopDownOneSide;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class Param {
	protected static Options argOptions;

	static {
		Options options = new Options();
		options.addOption( "K", true, "Represents a number of indexes" );
		options.addOption( "qSize", true, "Q gram size" );
		options.addOption( "sample", true, "Sampling Ratio" );
		options.addOption( "t", true, "Threshold" );
		options.addOption( "noLength", false, "No Length Filtering" );
		options.addOption( "naiveVal", false, "Naive Validator" );
		options.addOption( "delta", true, "deltaMax" );
		options.addOption( "nEst", true, "nEst" );

		options.addOption( "useLF", true, "" );
		options.addOption( "usePQF", true, "" );

		argOptions = options;
	}

	public static Param parseArgs( String[] args, StatContainer stat, Query query ) throws IOException, ParseException {
		CommandLineParser parser = new DefaultParser();
		Param param = new Param();

		CommandLine cmd = parser.parse( argOptions, args );

		stat.add( cmd );

		if( cmd.hasOption( "K" ) ) {
			param.indexK = Integer.parseInt( cmd.getOptionValue( "K" ) );
		}

		if( cmd.hasOption( "qSize" ) ) {
			param.qgramSize = Integer.parseInt( cmd.getOptionValue( "qSize" ) );
		}

		if( cmd.hasOption( "sample" ) ) {
			param.sampleRatio = Double.parseDouble( cmd.getOptionValue( "sample" ) );
		}

		if( cmd.hasOption( "t" ) ) {
			param.threshold = Integer.parseInt( cmd.getOptionValue( "t" ) );
		}

		if( cmd.hasOption( "noLength" ) ) {
			param.noLength = true;
		}
		
		if( cmd.hasOption( "naiveVal" ) ) {
			if( query.oneSideJoin ) {
				param.validator = new NaiveOneSide();
			}
			else {
				param.validator = new Naive();
			}
		}
		else {
			if( query.oneSideJoin ) {
				param.validator = new TopDownOneSide();
			}
			else {
				param.validator = new TopDown();
			}
		}
		
		if (cmd.hasOption( "delta" )) {
			param.deltaMax = Integer.parseInt( cmd.getOptionValue( "delta" ) );
		}

		if (cmd.hasOption( "nEst" )) {
			param.nEst = Integer.parseInt( cmd.getOptionValue( "nEst" ) );
		}
		else param.nEst = 1;

		if (cmd.hasOption( "useLF" )) {
			param.useLF = Boolean.parseBoolean( cmd.getOptionValue( "UseLF" ) );
		}

		if (cmd.hasOption( "usePQF" )) {
			param.usePQF = Boolean.parseBoolean( cmd.getOptionValue( "UseLF" ) );
		}

		return param;
	}

	public int indexK = 2;
	public int qgramSize = 2;
	public double sampleRatio = -1;
	public Validator validator;
	public int threshold = 10;
	public boolean noLength = false;
	public int deltaMax;
	public int nEst;
	public boolean useLF = true;
	public boolean usePQF = true;
}
