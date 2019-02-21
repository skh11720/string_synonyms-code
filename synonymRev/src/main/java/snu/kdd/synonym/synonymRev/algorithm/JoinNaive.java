package snu.kdd.synonym.synonymRev.algorithm;

import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.index.NaiveIndex;
import snu.kdd.synonym.synonymRev.tools.StopWatch;

public class JoinNaive extends AbstractAlgorithm {

	public NaiveIndex idx;

	// statistics used for building indexes
	public double avgTransformed;

	public JoinNaive(Query query, String[] args) {
		super(query, args);
	}

	@Override
	protected void executeJoin() {
		// Index building
		StopWatch stepTime = null;
		stepTime = StopWatch.getWatchStarted( "Result_3_1_Index_Building_Time" );

		idx = new NaiveIndex( query, stat, writeResult );

		stepTime.stopAndAdd( stat );
		stepTime.resetAndStart( "Result_3_2_Join_Time" );
		stat.addMemory( "Mem_3_BuildIndex" );

		rslt = idx.join( query, stat, null, writeResult );

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
	public String getName() {
		return "JoinNaive";
	}

	@Override
	public String getVersion() {
		/*
		 * 2.00: the latest version by yjpark
		 * 2.01: checkpoint
		 * 2.02: ignore strings with too many transformations
		 */
		return "2.02";
	}
}
