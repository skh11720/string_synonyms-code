package snu.kdd.synonym.synonymRev.algorithm;

import java.util.List;

import sigmod13.SI_Tree;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.order.FrequencyFirstOrder;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.Pair;
import snu.kdd.synonym.synonymRev.tools.ResultSet;
import snu.kdd.synonym.synonymRev.tools.Stat;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.validator.TopDownOneSide;

public class SIJoin extends AbstractAlgorithm {

	private final double theta = 1.0;


	public SIJoin(String[] args) {
		super(args);
	}
	
	@Override
	public void initialize() {
		super.initialize();
		checker = new TopDownOneSide();
	}

	@Override
	public void preprocess() {
		super.preprocess();

		FrequencyFirstOrder globalOrder = new FrequencyFirstOrder( 1 );
		globalOrder.initializeForSequence( query, true );

		for( Record recS : query.searchedSet.get() ) {
			recS.preprocessAvailableTokens( Integer.MAX_VALUE );
		}
	}

	@Override
	protected void executeJoin() {
		StopWatch stepTime = null;
		stepTime = StopWatch.getWatchStarted( INDEX_BUILD_TIME );
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

		stepTime.resetAndStart( JOIN_AFTER_INDEX_TIME );
		rslt = join( treeS, treeT, theta );
		stepTime.stopAndAdd( stat );
		stat.add( Stat.NUM_VERIFY, treeS.verifyCount );
	}

	protected ResultSet join( SI_Tree<Record> treeS, SI_Tree<Record> treeT, double threshold ) {
		List<Pair<Record>> candidates = treeS.join( treeT, threshold );
		ResultSet rslt = new ResultSet(query.selfJoin);
		for( Pair<Record> ip : candidates ) {
			rslt.add( ip.rec1, ip.rec2 );
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
		 * 1.04: use token global order, modify signature generation
		 * 1.05: major update
		 */
		return "1.05";
	}

	@Override
	public String getName() {
		return "SIJoin";
	}
	
	@Override
	public String getNameWithParam() {
		return getName();
	}
}
