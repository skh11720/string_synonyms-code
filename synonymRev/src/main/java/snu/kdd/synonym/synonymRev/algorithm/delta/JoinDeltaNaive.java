package snu.kdd.synonym.synonymRev.algorithm.delta;

import snu.kdd.synonym.synonymRev.algorithm.AbstractParameterizedAlgorithm;
import snu.kdd.synonym.synonymRev.tools.StopWatch;

public class JoinDeltaNaive extends AbstractParameterizedAlgorithm {

	public static boolean useLF = true;

	public final int deltaMax;
	public final String distFunc;
	
	protected JoinDeltaNaiveIndex idx;

	
	public JoinDeltaNaive(String[] args) {
		super(args);
		deltaMax = param.getIntParam("deltaMax");
		distFunc = param.getStringParam("dist");
		useLF = param.getBooleanParam("useLF");
	}
	
	@Override
	public void initialize() {
		super.initialize();
		checker = new DeltaValidatorNaive(deltaMax, distFunc);
	}

	@Override
	protected void reportParamsToStat() {
		stat.add("Param_deltaMax", deltaMax);
		stat.add("Param_distFunct", distFunc);
		stat.add("Param_useLF", useLF);
	}

	@Override
	protected void executeJoin() {
		StopWatch stepTime = StopWatch.getWatchStarted( INDEX_BUILD_TIME );
		idx = new JoinDeltaNaiveIndex(deltaMax, distFunc, query);
		stat.addMemory( "Mem_3_BuildIndex" );
		stepTime.stopAndAdd( stat );
		stepTime.resetAndStart( JOIN_AFTER_INDEX_TIME );

		rslt = idx.join( query, stat, checker, writeResultOn );

		stat.addMemory( "Mem_4_Joined" );
		stepTime.stopAndAdd( stat );
	}

	@Override
	public String getVersion() {
		/*
		 * 1.00: the initial version
		 * 1.01: major update
		 * 1.02: two-level hashing (DEPRECATED)
		 */
		return "1.01";
	}

	@Override
	public String getName() {
		return "JoinDeltaNaive";
	}
	
	@Override
	public String getNameWithParam() {
		return String.format("%s_%d_%s", getName(), deltaMax, distFunc);
	}
}
