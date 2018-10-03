package snu.kdd.synonym.synonymRev.algorithm;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.ParseException;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import sigmod13.SI_Tree;
import snu.kdd.synonym.synonymRev.algorithm.misc.SampleDataTest;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.Pair;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.validator.TopDown;
import snu.kdd.synonym.synonymRev.validator.TopDownOneSide;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class SIJoin extends AlgorithmTemplate {

	static Validator checker;
	private final double theta = 1.0;

	public SIJoin( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}

	@Override
	public void preprocess() {
		super.preprocess();
		for( Record recS : query.searchedSet.get() ) {
			recS.preprocessAvailableTokens( Integer.MAX_VALUE );
			recS.preprocessSuffixApplicableRules();
		}

		if( !query.selfJoin ) {
			for( Record recT : query.indexedSet.get() ) {
				recT.preprocessAvailableTokens( Integer.MAX_VALUE );
				recT.preprocessSuffixApplicableRules();
			}
		}

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

		StopWatch stepTime = StopWatch.getWatchStarted( "Result_2_Preprocess_Total_Time" );
		preprocess();
		stepTime.stopAndAdd( stat );

//		SI_Tree<Record> treeR = new SI_Tree<Record>( 1, null, query.searchedSet.recordList, checker );
//		SI_Tree<Record> treeS = new SI_Tree<Record>( 1, null, query.indexedSet.recordList, checker );

		stepTime = StopWatch.getWatchStarted( "Result_3_1_Index_Building_Time" );
		SI_Tree<Record> treeS = new SI_Tree<Record>( theta, null, checker, true );
		for ( Record recS : query.searchedSet.recordList ) {
			if ( recS.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) continue;
//			if ( recS.getID() < 10 ) SampleDataTest.inspect_record( recS, query, 1 );
			treeS.add( recS );
		}

		SI_Tree<Record> treeT = new SI_Tree<Record>( theta, null, checker, false );
		for ( Record recT : query.indexedSet.recordList ) {
			if ( recT.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) continue;
			treeT.add( recT );
		}
		stepTime.stopAndAdd( stat );

		if( DEBUG.SIJoinON ) {
			System.out.println( "Node size : " + ( treeS.FEsize + treeS.LEsize ) );
			System.out.println( "Sig size : " + treeS.sigsize );

			System.out.print( "Building SI-Tree finished" );
			System.out.println( " " + ( System.currentTimeMillis() - startTime ) );
		}
		// br.readLine();

		stepTime.resetAndStart( "Result_3_2_Join_Time" );
		rslt = join( treeS, treeT, theta );
		stepTime.stopAndAdd( stat );

		stat.add( "Stat_Equiv_Comparison", treeS.verifyCount );

		writeResult();
	}

	public Set<IntegerPair> join( SI_Tree<Record> treeS, SI_Tree<Record> treeT, double threshold ) {
		long startTime = System.currentTimeMillis();

		List<Pair<Record>> candidates = treeS.join( treeT, threshold );
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

		Set<IntegerPair> rslt = new ObjectOpenHashSet<IntegerPair>();

		for( Pair<Record> ip : candidates ) {
//			rslt.add( new IntegerPair( ip.rec1.getID(), ip.rec2.getID() ) );
			addSeqResult( ip.rec1, ip.rec2, rslt, query.selfJoin );
		}
		return rslt;
	}

	@Override
	public String getVersion() {
		/*
		 * 1.00: initial version
		 * 1.01: ignore records with too many transformations
		 * 1.02: output stats
		 * 1.03: fix bugs related to length filtering
		 */
		return "1.03";
	}

	@Override
	public String getName() {
		return "SIJoin";
	}
}
