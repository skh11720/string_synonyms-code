package snu.kdd.synonym.synonymRev.algorithm;

import passjoin.PassJoinIndexForSynonyms;
import passjoin.PassJoinValidator;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.StopWatch;

public class PassJoin extends AbstractIndexBasedAlgorithm {
	
	public final int deltaMax;
	protected PassJoinIndexForSynonyms idx = null;

	public PassJoin(Query query, String[] args) {
		super(query, args);
		deltaMax = param.getIntParam("deltaMax");
		checker = new PassJoinValidator(deltaMax);
	}
	
	@Override
	protected void reportParamsToStat() {
		stat.add("Param_deltaMax", deltaMax);
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

		rslt = idx.join( query, stat, checker, writeResult );

		stat.addMemory( "Mem_4_Joined" );
		stepTime.stopAndAdd( stat );
	}
	
	@Override
	protected void buildIndex() {
		idx = new PassJoinIndexForSynonyms( query, deltaMax, stat );
	}
	
	@Override
	public String getName() {
		return "PassJoin";
	}

	@Override
	public String getVersion() {
        /*
         * 1.00: initial version
         * 1.01: ignore records with too many transformations
         * 1.02: modify from PassJoinExact to PassJoin
         */
		return "1.02";
	}
}
