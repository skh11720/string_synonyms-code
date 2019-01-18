package snu.kdd.synonym.synonymRev.algorithm.delta;

import java.io.IOException;

import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.Param;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class JoinDeltaNaive extends AlgorithmTemplate {
	
	protected DeltaHashIndex idx;
	protected Validator checker;
	protected int deltaMax;
	
	public static boolean useLF = true;

	
	public JoinDeltaNaive(Query query, StatContainer stat, String[] args) throws IOException, ParseException {
		super(query, stat, args);
		param = new Param(args);
		deltaMax = param.getIntParam("deltaMax");
		useLF = param.getBooleanParam("useLF");
		checker = new DeltaValidatorDPTopDown(deltaMax);
	}

	@Override
	protected void preprocess() {
		super.preprocess();

		for( Record rec : query.indexedSet.get() ) {
			rec.preprocessSuffixApplicableRules();
		}
		if( !query.selfJoin ) {
			for( Record rec : query.searchedSet.get() ) {
				rec.preprocessSuffixApplicableRules();
			}
		}
	}
	
	@Override
	public void run() {
		StopWatch stepTime = StopWatch.getWatchStarted( "Result_2_Preprocess_Total_Time" );
		preprocess();
		stat.addMemory( "Mem_2_Preprocessed" );
		stepTime.stopAndAdd( stat );

		runAfterPreprocess();
	}

	public void runAfterPreprocess() {
		StopWatch runTime = null;
		StopWatch stepTime = null;

		runTime = StopWatch.getWatchStarted( "Result_3_Run_Time" );
		stepTime = StopWatch.getWatchStarted( "Result_3_1_Index_Building_Time" );
		idx = new DeltaHashIndex(deltaMax, query, stat);
		stat.addMemory( "Mem_3_BuildIndex" );
		stepTime.stopAndAdd( stat );
		stepTime.resetAndStart( "Result_3_2_Join_Time" );

		rslt = idx.join( query, stat, checker, writeResult );

		stat.addMemory( "Mem_4_Joined" );
		stepTime.stopAndAdd( stat );

		runTime.stopAndAdd( stat );

		stepTime.resetAndStart( "Result_4_Write_Time" );

		writeResult();

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
