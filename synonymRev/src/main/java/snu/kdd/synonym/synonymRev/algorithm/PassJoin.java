package snu.kdd.synonym.synonymRev.algorithm;

import passjoin.PassJoinIndexForSynonyms;
import passjoin.PassJoinValidator;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.Param;
import snu.kdd.synonym.synonymRev.tools.StopWatch;

public class PassJoin extends AbstractIndexBasedAlgorithm {
	
	protected PassJoinIndexForSynonyms idx = null;
	protected int deltaMax;

	public PassJoin(Query query, String[] args) {
		super(query, args);
		param = new Param(args);
		deltaMax = param.getIntParam("deltaMax");
		checker = new PassJoinValidator(deltaMax);
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
		stepTime = StopWatch.getWatchStarted( "Result_3_1_Index_Building_Time" );
		buildIndex();

		stat.addMemory( "Mem_3_BuildIndex" );
		stepTime.stopAndAdd( stat );
		stepTime.resetAndStart( "Result_3_2_Join_Time" );

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
