package snu.kdd.synonym.synonymRev.tools;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Param {
	private static final Options argOptions;

	static {
		argOptions = new Options();
		argOptions.addOption( "K", true, "Represents a number of indexes" );
		argOptions.addOption( "qSize", true, "Q gram size" );
		argOptions.addOption( "sampleB", true, "Sampling Ratio for JoinBKPFast" );
		argOptions.addOption( "sampleH", true, "Sampling Ratio for JoinHybridAll" );
		argOptions.addOption( "delta", true, "deltaMax" );

		argOptions.addOption( "useLF", true, "" );
		argOptions.addOption( "usePQF", true, "" );
		argOptions.addOption( "useSTPQ", true, "" );

		argOptions.addOption( "verify", true, "Verification" );
		argOptions.addOption( "rc", true, "Rule compression" );
	}

	public Param( String[] args ) throws IOException, ParseException {
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse( argOptions, args );

		if( cmd.hasOption( "K" ) ) {
			indexK = Integer.parseInt( cmd.getOptionValue( "K" ) );
			if ( indexK <= 0 ) throw new ParseException("K must be larger than 0, not "+indexK);
		}
		
		if( cmd.hasOption( "qSize" ) ) {
			qSize = Integer.parseInt( cmd.getOptionValue( "qSize" ) );
			if ( qSize <= 0 ) throw new ParseException("qSize must be larger than 0, not "+qSize);
		}

		if( cmd.hasOption( "sampleB" ) ) sampleB = Double.parseDouble( cmd.getOptionValue( "sampleB" ) );

		if( cmd.hasOption( "sampleH" ) ) sampleH = Double.parseDouble( cmd.getOptionValue( "sampleH" ) );

		if (cmd.hasOption( "delta" )) deltaMax = Integer.parseInt( cmd.getOptionValue( "delta" ) );

		if (cmd.hasOption( "useLF" )) useLF = Boolean.parseBoolean( cmd.getOptionValue( "useLF" ) );
		else useLF = true;

		if (cmd.hasOption( "usePQF" )) usePQF = Boolean.parseBoolean( cmd.getOptionValue( "usePQF" ) );
		else usePQF = true;

		if (cmd.hasOption( "useSTPQ" )) useSTPQ = Boolean.parseBoolean( cmd.getOptionValue( "useSTPQ" ) );
		else useSTPQ = true;
	}
	
	// TODO toJSON

	public int indexK;
	public int qSize;
	public double sampleB;
	public double sampleH;
	public int deltaMax;
	public boolean useLF;
	public boolean usePQF;
	public boolean useSTPQ;
	
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
