package sigmod13.modified;

import java.io.IOException;
import java.util.HashSet;

import mine.Record;
import sigmod13.SI_Tree;
import snu.kdd.synonym.tools.StatContainer;
import tools.Algorithm;
import tools.Pair;

public class SI_SelfJoin_Modified extends Algorithm {
	public SI_SelfJoin_Modified( String DBR_file, String rulefile ) throws IOException {
		super( rulefile, DBR_file, DBR_file );
	}

	protected void preprocess() {
		long currentTime = System.currentTimeMillis();
		for( Record rec : tableR ) {
			rec.preprocessAvailableTokens( 1 );
		}
		long time = System.currentTimeMillis() - currentTime;
		System.out.println( "Preprocess available tokens: " + time );
	}

	public void run( double threshold, int filterType ) throws IOException {
		// BufferedReader br = new BufferedReader(new
		// InputStreamReader(System.in));
		// br.readLine();

		preprocess();

		long startTime = System.currentTimeMillis();

		SI_Tree<Record> tree = new SI_Tree<Record>( threshold, null, tableR );
		System.out.println( "Node size : " + ( tree.FEsize + tree.LEsize ) );
		System.out.println( "Sig size : " + tree.sigsize );

		System.out.print( "Building SI-Tree finished" );
		System.out.println( " " + ( System.currentTimeMillis() - startTime ) );

		// br.readLine();

		selfjoin( tree, threshold );
	}

	public void selfjoin( SI_Tree<Record> treeR, double threshold ) {
		long startTime = System.currentTimeMillis();

		HashSet<Pair<Record>> candidates = treeR.selfjoin( threshold );
		// long counter = treeR.join(treeS, threshold);
		System.out.print( "Retrieveing candidates finished" );

		System.out.println( " " + ( System.currentTimeMillis() - startTime ) );
		System.out.println( "Candidates : " + candidates.size() );

		startTime = System.currentTimeMillis();

		long similar = 0;

		for( Pair<Record> pair : candidates ) {
			System.out.println( pair.rec1 + "\t" + pair.rec2 );
			++similar;
		}

		System.out.print( "Validating finished" );
		System.out.println( " " + ( System.currentTimeMillis() - startTime ) );
		System.out.println( "Similar pairs : " + similar );
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main( String[] args ) throws IOException {
		if( args.length != 4 && args.length != 5 ) {
			printUsage();
			return;
		}
		String Rfile = args[ 0 ];
		String Rulefile = args[ 1 ];
		double threshold = Double.parseDouble( args[ 2 ] );
		int filterNo = Integer.parseInt( args[ 3 ] );
		SI_Tree.exactAnswer = false;// (args.length == 5);

		long startTime = System.currentTimeMillis();
		SI_SelfJoin_Modified inst = new SI_SelfJoin_Modified( Rfile, Rulefile );
		System.out.print( "Constructor finished" );
		System.out.println( " " + ( System.currentTimeMillis() - startTime ) );
		inst.run( threshold, filterNo );
	}

	private static void printUsage() {
		System.out.println( "Usage : <R file> <Rule file> <Threshold> <Filter No.>" );
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