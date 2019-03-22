package snu.kdd.synonym.synonymRev.algorithm;

import snu.kdd.synonym.synonymRev.index.NaiveIndex;
import snu.kdd.synonym.synonymRev.tools.StopWatch;

public class JoinNaive extends AbstractAlgorithm {

	public NaiveIndex idx;

	// statistics used for building indexes
	public double avgTransformed;

	public JoinNaive( String[] args) {
		super(args);
	}

	@Override
	protected void executeJoin() {
		// Index building
		StopWatch stepTime = null;
		stepTime = StopWatch.getWatchStarted( INDEX_BUILD_TIME );

		idx = new NaiveIndex( query, stat, writeResultOn );

		stepTime.stopAndAdd( stat );
		stepTime.resetAndStart( JOIN_AFTER_INDEX_TIME );
		stat.addMemory( "Mem_3_BuildIndex" );

		rslt = idx.join( query, stat, null, writeResultOn );

		stepTime.stopAndAdd( stat );
		stat.addMemory( "Mem_4_Joined" );
		stat.add( "Stat_Expanded", idx.totalExp );
	}

	public double getAlpha() {
		return idx.alpha;
	}

	public double getBeta() {
		return idx.beta;
	}

	@Override
	public String getVersion() {
		/*
		 * 2.00: the latest version by yjpark
		 * 2.01: checkpoint
		 * 2.02: ignore strings with too many transformations
		 * 2.03: major update
		 */
		return "2.03";
	}

	@Override
	public String getName() {
		return "JoinNaive";
	}
	
	@Override
	public String getNameWithParam() {
		return getName();
	}
}
