package snu.kdd.synonym.synonymRev.algorithm.misc;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.StatContainer;

public class TransformDataSet extends AlgorithmTemplate {
	int seed = 0;

	protected TransformDataSet( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}

	@Override
	protected void preprocess() {
		super.preprocess();
	}

	@Override
	public void run( Query query, String[] args ) throws IOException, ParseException {

	}

	public void transform() {
		preprocess();

		Random random = new Random( seed );
		try {
			BufferedWriter bw = new BufferedWriter( new FileWriter( query.outputFile + "/transformed.txt" ) );

			for( Record rec : query.searchedSet.get() ) {
				ArrayList<Record> exp = rec.expandAll();

				int selected = random.nextInt( exp.size() );
				Record selectedRecord = exp.get( selected );
				selectedRecord.preprocessRules( automata );

				ArrayList<Record> exp2 = selectedRecord.expandAll();
				int selected2 = random.nextInt( exp2.size() );
				bw.write( exp2.get( selected2 ).toString() );
				bw.write( "\n" );
			}
			bw.close();
		}
		catch( Exception e ) {
			e.printStackTrace();
		}
	}

	public static void main( String args[] ) throws IOException {
		String rulefile = args[ 0 ];
		String Rfile = args[ 1 ];
		String Sfile = args[ 2 ];
		String outputPath = args[ 3 ];

		StatContainer stat = new StatContainer();
		Query query = new Query( rulefile, Rfile, Sfile, false, outputPath );
		TransformDataSet info = new TransformDataSet( query, stat );

		info.transform();
	}

	@Override
	public String getName() {
		return "TransformDataSet";
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

}
