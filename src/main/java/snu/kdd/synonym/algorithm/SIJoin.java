package snu.kdd.synonym.algorithm;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import mine.Record;
import sigmod13.SI_Tree;
import snu.kdd.synonym.tools.StatContainer;
import tools.Pair;

public class SIJoin extends AlgorithmTemplate {
	private static String outputfile;

	public SIJoin( String DBR_file, String DBS_file, String rulefile, String outputFile ) throws IOException {
		super( rulefile, DBR_file, DBS_file, outputFile );
	}

	public void run() throws IOException {
		preprocess( false, 1, false );
		// BufferedReader br = new BufferedReader(new
		// InputStreamReader(System.in));
		// br.readLine();

		long startTime = System.currentTimeMillis();

		SI_Tree<Record> treeR = new SI_Tree<Record>( 1, null, tableR );
		SI_Tree<Record> treeS = new SI_Tree<Record>( 1, null, tableS );
		System.out.println( "Node size : " + ( treeR.FEsize + treeR.LEsize ) );
		System.out.println( "Sig size : " + treeR.sigsize );

		System.out.print( "Building SI-Tree finished" );
		System.out.println( " " + ( System.currentTimeMillis() - startTime ) );

		// br.readLine();

		join( treeR, treeS, 1 );
	}

	public void join( SI_Tree<Record> treeR, SI_Tree<Record> treeS, double threshold ) {
		long startTime = System.currentTimeMillis();

		List<Pair<Record>> candidates = treeR.join( treeS, threshold );
		// long counter = treeR.join(treeS, threshold);
		System.out.print( "Retrieveing candidates finished" );

		System.out.println( " " + ( System.currentTimeMillis() - startTime ) );
		System.out.println( "Candidates : " + candidates.size() );

		startTime = System.currentTimeMillis();

		System.out.print( "Validating finished" );
		System.out.println( " " + ( System.currentTimeMillis() - startTime ) );
		System.out.println( "Similar pairs : " + candidates.size() );

		try {
			BufferedWriter bw = new BufferedWriter( new FileWriter( outputfile ) );
			for( Pair<Record> ip : candidates ) {
				if( ip.rec1.getID() != ip.rec2.getID() )
					bw.write( ip.rec1.toString( strlist ) + "\t==\t" + ip.rec2.toString( strlist ) + "\n" );
			}
			bw.close();
		}
		catch( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main( String[] args ) throws IOException {
		String[] remainingArgs = parse( args );
		if( remainingArgs.length != 4 ) {
			printUsage();
			return;
		}
		String Rfile = remainingArgs[ 0 ];
		String Sfile = remainingArgs[ 1 ];
		String Rulefile = remainingArgs[ 2 ];
		outputfile = remainingArgs[ 3 ];

		long startTime = System.currentTimeMillis();
		SIJoin inst = new SIJoin( Rfile, Sfile, Rulefile, outputfile );
		System.out.print( "Constructor finished" );
		System.out.println( " " + ( System.currentTimeMillis() - startTime ) );
		inst.run();
	}

	private static void printUsage() {
		System.out.println( "Usage : <R file> <S file> <Rule file> <output file>" );
	}

	private static String[] parse( String[] args ) {
		Options options = buildOptions();
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse( options, args );
		}
		catch( ParseException e ) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "SI_Join_Modified", options, true );
			System.exit( 1 );
		}
		SI_Tree.skipEquiCheck = cmd.hasOption( "skipequiv" );
		return cmd.getArgs();
	}

	private static Options buildOptions() {
		Options options = new Options();
		options.addOption( "skipequiv", false, "Skip equivalency check" );
		return options;
	}

	@Override
	public String getVersion() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void run( String[] args, StatContainer stat ) {
		// TODO Auto-generated method stub

	}
}
