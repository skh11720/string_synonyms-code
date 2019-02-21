package snu.kdd.synonym.synonymRev.tools;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Param extends AbstractParam {
	private static final Options argOptions;

	static {
		argOptions = new Options();
		argOptions.addOption( "K", true, "Represents a number of indexes" );
		argOptions.addOption( "qSize", true, "Q gram size" );
		argOptions.addOption( "sampleB", true, "Sampling Ratio for JoinBKPFast" );
		argOptions.addOption( "sampleH", true, "Sampling Ratio for JoinHybridAll" );
		argOptions.addOption( "delta", true, "deltaMax" );
		argOptions.addOption( "theta", true, "theta" );

		argOptions.addOption( "useLF", true, "" );
		argOptions.addOption( "usePQF", true, "" );
		argOptions.addOption( "useSTPQ", true, "" );
	}

	public Param( String[] args ) {
		try { 
			CommandLineParser parser = new DefaultParser();
			CommandLine cmd = parser.parse( argOptions, args );

			if( cmd.hasOption( "K" ) ) {
				int indexK = Integer.parseInt( cmd.getOptionValue( "K" ) );
				if ( indexK <= 0 ) throw new ParseException("K must be larger than 0, not "+indexK);
				mapParamI.put("indexK", indexK);
			}
			
			if( cmd.hasOption( "qSize" ) ) {
				int qSize = Integer.parseInt( cmd.getOptionValue( "qSize" ) );
				if ( qSize <= 0 ) throw new ParseException("qSize must be larger than 0, not "+qSize);
				mapParamI.put("qSize", qSize);
			}

			if( cmd.hasOption( "sampleB" ) ) {
				double sampleB = Double.parseDouble( cmd.getOptionValue( "sampleB" ) );
				if ( sampleB < 0 || sampleB > 1 ) throw new ParseException("sampleB must be between 0 and 1, not "+sampleB);
				mapParamD.put("sampleB", sampleB);
			}

			if( cmd.hasOption( "sampleH" ) ) {
				double sampleH = Double.parseDouble( cmd.getOptionValue( "sampleH" ) );
				if ( sampleH < 0 || sampleH > 1 ) throw new ParseException("sampleH must be between 0 and 1, not "+sampleH);
				mapParamD.put("sampleH", sampleH);
			}

			if (cmd.hasOption( "delta" )) {
				int deltaMax = Integer.parseInt( cmd.getOptionValue( "delta" ) );
				if ( deltaMax < 0 ) throw new ParseException("delta must be nonnegative integer, not "+deltaMax);
				mapParamI.put("deltaMax", deltaMax);
			}

			if (cmd.hasOption( "theta" )) {
				double theta = Double.parseDouble( cmd.getOptionValue( "theta" ) );
				if ( theta < 0 || theta > 1 ) throw new ParseException("theta must be in [0,1], not "+theta);
				mapParamD.put("theta", theta);
			}

			boolean useLF = true;
			if (cmd.hasOption( "useLF" )) useLF = Boolean.parseBoolean( cmd.getOptionValue( "useLF" ) );
			mapParamB.put("useLF", useLF);

			boolean usePQF = true;
			if (cmd.hasOption( "usePQF" )) usePQF = Boolean.parseBoolean( cmd.getOptionValue( "usePQF" ) );
			mapParamB.put("usePQF", usePQF);

			boolean useSTPQ = true;
			if (cmd.hasOption( "useSTPQ" )) useSTPQ = Boolean.parseBoolean( cmd.getOptionValue( "useSTPQ" ) );
			mapParamB.put("useSTPQ", useSTPQ);
		}
		catch (ParseException e ) {
			e.printStackTrace();
		}
	}
	
//	public static void main(String[] args) throws ParseException {
//		args = new String[]{"-K", "1", "-qSize", "2", "-sampleB", "0.01", "-usePQF", "false"};
//		CommandLineParser parser = new DefaultParser();
//		CommandLine cmd = parser.parse( argOptions, args );
//		System.out.println( cmd.getOptionValue("usePQF") );
//		for ( Option opt : cmd.getOptions() ) System.out.println(opt.toString());
//		for ( Option opt : cmd.getOptions() ) System.out.println(opt.getOpt() );
//		for ( Option opt : argOptions.getOptions() ) System.out.println(opt.getOpt());
//	}
}
