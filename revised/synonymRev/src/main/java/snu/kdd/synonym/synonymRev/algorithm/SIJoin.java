package snu.kdd.synonym.synonymRev.algorithm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.ParseException;

import sigmod13.SI_Tree;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.Pair;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.validator.TopDown;
import snu.kdd.synonym.synonymRev.validator.TopDownOneSide;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class SIJoin extends AlgorithmTemplate {

	static Validator checker;

	public SIJoin( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}

	public void run( Query query, String[] args ) throws IOException, ParseException {
		if( query.oneSideJoin ) {
			checker = new TopDownOneSide();
		}
		else {
			checker = new TopDown();
		}

		long startTime = System.currentTimeMillis();

		if( DEBUG.SIJoinON ) {
			System.out.print( "Constructor finished" );
			System.out.println( " " + ( System.currentTimeMillis() - startTime ) );
		}

		preprocess();

		SI_Tree<Record> treeR = new SI_Tree<Record>( 1, null, query.searchedSet.recordList, checker );
		SI_Tree<Record> treeS = new SI_Tree<Record>( 1, null, query.indexedSet.recordList, checker );

		if( DEBUG.SIJoinON ) {
			System.out.println( "Node size : " + ( treeR.FEsize + treeR.LEsize ) );
			System.out.println( "Sig size : " + treeR.sigsize );

			System.out.print( "Building SI-Tree finished" );
			System.out.println( " " + ( System.currentTimeMillis() - startTime ) );
		}
		// br.readLine();

		List<IntegerPair> rslt = join( treeR, treeS, 1 );

		writeResult( rslt );
	}

	public List<IntegerPair> join( SI_Tree<Record> treeR, SI_Tree<Record> treeS, double threshold ) {
		long startTime = System.currentTimeMillis();

		List<Pair<Record>> candidates = treeR.join( treeS, threshold );
		// long counter = treeR.join(treeS, threshold);

		if( DEBUG.SIJoinON ) {
			System.out.print( "Retrieveing candidates finished" );

			System.out.println( " " + ( System.currentTimeMillis() - startTime ) );
			System.out.println( "Candidates : " + candidates.size() );

			startTime = System.currentTimeMillis();

			System.out.print( "Validating finished" );
			System.out.println( " " + ( System.currentTimeMillis() - startTime ) );
			System.out.println( "Similar pairs : " + candidates.size() );
		}

		List<IntegerPair> rslt = new ArrayList<IntegerPair>();

		for( Pair<Record> ip : candidates ) {
			rslt.add( new IntegerPair( ip.rec1.getID(), ip.rec2.getID() ) );
		}
		return rslt;
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

	@Override
	public String getName() {
		return "SIJoin";
	}
}
