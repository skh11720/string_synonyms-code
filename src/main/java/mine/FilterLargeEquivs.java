package mine;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import snu.kdd.synonym.tools.StatContainer;
import tools.Algorithm;
import tools.Rule_ACAutomata;

public class FilterLargeEquivs extends Algorithm {

	protected FilterLargeEquivs( String rulefile, String Rfile ) throws IOException {
		super( rulefile, Rfile, Rfile );
	}

	public static void main( String[] args ) throws Exception {
		String inputfile = args[ 0 ];
		String rulefile = args[ 1 ];
		String outputfile = args[ 2 ];
		int threshold = Integer.valueOf( args[ 3 ] );
		FilterLargeEquivs inst = new FilterLargeEquivs( rulefile, inputfile );
		BufferedWriter bw = new BufferedWriter( new FileWriter( outputfile ) );
		Rule_ACAutomata automata = new Rule_ACAutomata( inst.rulelist );
		Record.setStrList( inst.strlist );
		for( Record rec : inst.tableR ) {
			rec.preprocessRules( automata, false );
			rec.preprocessEstimatedRecords();
			if( rec.getEstNumRecords() > threshold )
				continue;
			bw.write( rec.toString() + "\n" );
		}
		bw.close();
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

	@Override
	public String getName() {
		return "FilterLargeEquivs";
	}

	@Override
	public void run( String[] args, StatContainer stat ) {
		// TODO Auto-generated method stub

	}
}
