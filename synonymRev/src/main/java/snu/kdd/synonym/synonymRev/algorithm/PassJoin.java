package snu.kdd.synonym.synonymRev.algorithm;

import passjoin.PassJoinIndexForSynonyms;
import passjoin.PassJoinValidator;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.StopWatch;

public class PassJoin extends AbstractIndexBasedAlgorithm {
	
	public final int deltaMax;
	public final String distFunc;
	protected PassJoinIndexForSynonyms idx = null;

	public PassJoin(String[] args) {
		super(args);
		deltaMax = param.getIntParam("deltaMax");
		distFunc = param.getStringParam("dist");
	}
	
	@Override
	public void initialize() {
		super.initialize();
		checker = new PassJoinValidator(deltaMax, distFunc);
	}
	
	@Override
	protected void reportParamsToStat() {
		stat.add("Param_deltaMax", deltaMax);
		stat.add("Param_distFunct", distFunc);
	}

	@Override
	protected void preprocess() {
		super.preprocess();
		for( Record rec : query.searchedSet.get() ) {
			rec.preprocessSuffixApplicableRules();
		}
	}

	@Override
	protected void executeJoin() {
		StopWatch stepTime = null;
		stepTime = StopWatch.getWatchStarted( INDEX_BUILD_TIME );
		buildIndex();

		stat.addMemory( "Mem_3_BuildIndex" );
		stepTime.stopAndAdd( stat );
		stepTime.resetAndStart( JOIN_AFTER_INDEX_TIME );

		rslt = idx.join( query, stat, checker, writeResultOn );

		stat.addMemory( "Mem_4_Joined" );
		stepTime.stopAndAdd( stat );
	}
	
	@Override
	protected void buildIndex() {
		idx = new PassJoinIndexForSynonyms( query, deltaMax, distFunc, stat );
	}

	@Override
	public String getVersion() {
        /*
         * 1.00: initial version
         * 1.01: ignore records with too many transformations
         * 1.02: modify from PassJoinExact to PassJoin
		 * 1.03: major update
         */
		return "1.03";
	}
	
	@Override
	public String getName() {
		return "PassJoin";
	}
	
	@Override
	public String getNameWithParam() {
		return String.format("%s_%d", getName(), deltaMax);
	}
}
