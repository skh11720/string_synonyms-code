package snu.kdd.synonym.synonymRev.algorithm;

import java.io.IOException;
import java.util.Set;

import org.apache.commons.cli.ParseException;

import passjoin.PassJoinIndexForSynonyms;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.Param;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;

public class PassJoin extends AlgorithmTemplate{
	
	protected PassJoinIndexForSynonyms index = null;
	protected int deltaMax;


	public PassJoin(Query query, String[] args) throws IOException, ParseException {
		super(query, args);
		param = new Param(args);
		deltaMax = param.getIntParam("deltaMax");
	}

	@Override
	protected void preprocess() {
		super.preprocess();
		for( Record rec : query.searchedSet.get() ) {
			rec.preprocessSuffixApplicableRules();
		}
	}

	@Override
	public void run() {
		StopWatch stepTime = StopWatch.getWatchStarted( "Result_2_Preprocess_Total_Time" );

		preprocess();

		stepTime.stopAndAdd( stat );
		stat.addMemory( "Mem_2_Preprocessed" );
		stepTime.resetAndStart( "Result_3_Run_Time" );

		rslt = runAfterPreprocess();

		stepTime.stopAndAdd( stat );
		stepTime.resetAndStart( "Result_4_Write_Time" );

		this.writeResult();

		stepTime.stopAndAdd( stat );
	}
	
	protected Set<IntegerPair> runAfterPreprocess() {
		StopWatch stepTime = null;
		stepTime = StopWatch.getWatchStarted( "Result_3_1_Index_Building_Time" );
		buildIndex( writeResult );

		stat.addMemory( "Mem_3_BuildIndex" );
		stepTime.stopAndAdd( stat );
		stepTime.resetAndStart( "Result_3_2_Join_Time" );

		Set<IntegerPair> rslt = index.join( query, stat, null, writeResult );

		stat.addMemory( "Mem_4_Joined" );
		stepTime.stopAndAdd( stat );
		return rslt;
	}
	
	protected void buildIndex( boolean addStat ) {
		index = new PassJoinIndexForSynonyms( query, deltaMax, stat );
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
