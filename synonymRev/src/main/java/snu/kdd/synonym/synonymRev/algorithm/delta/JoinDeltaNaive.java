package snu.kdd.synonym.synonymRev.algorithm.delta;

import snu.kdd.synonym.synonymRev.algorithm.AbstractParameterizedAlgorithm;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.tools.StopWatch;

public class JoinDeltaNaive extends AbstractParameterizedAlgorithm {

	public static boolean useLF = true;

	public final int deltaMax;
	
	protected DeltaHashIndex idx;

	
	public JoinDeltaNaive(Query query, String[] args) {
		super(query, args);
		deltaMax = param.getIntParam("deltaMax");
		useLF = param.getBooleanParam("useLF");
		checker = new DeltaValidatorDPTopDown(deltaMax);
	}

	@Override
	protected void reportParamsToStat() {
		stat.add("Param_deltaMax", deltaMax);
		stat.add("Param_useLF", useLF);
	}

	@Override
	protected void executeJoin() {
		StopWatch stepTime = StopWatch.getWatchStarted( INDEX_BUILD_TIME );
		idx = new DeltaHashIndex(deltaMax, query, stat);
		stat.addMemory( "Mem_3_BuildIndex" );
		stepTime.stopAndAdd( stat );
		stepTime.resetAndStart( JOIN_AFTER_INDEX_TIME );

		rslt = idx.join( query, stat, checker, writeResult );

		stat.addMemory( "Mem_4_Joined" );
		stepTime.stopAndAdd( stat );
	}

	@Override
	public String getVersion() {
		/*
		 * 1.00: the initial version
		 */
		return "1.00";
	}

	@Override
	public String getName() {
		return "JoinDeltaNaive";
	}
}
