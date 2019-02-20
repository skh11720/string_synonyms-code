package snu.kdd.synonym.synonymRev.algorithm.delta;

import java.io.IOException;

import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.synonymRev.algorithm.AbstractAlgorithm;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.tools.Param;
import snu.kdd.synonym.synonymRev.tools.StopWatch;

public class JoinDeltaNaive extends AbstractAlgorithm {
	
	protected DeltaHashIndex idx;
	protected int deltaMax;
	
	public static boolean useLF = true;

	
	public JoinDeltaNaive(Query query, String[] args) throws IOException, ParseException {
		super(query, args);
		param = new Param(args);
		deltaMax = param.getIntParam("deltaMax");
		useLF = param.getBooleanParam("useLF");
		checker = new DeltaValidatorDPTopDown(deltaMax);
	}

	@Override
	protected void executeJoin() {
		StopWatch stepTime = StopWatch.getWatchStarted( "Result_3_1_Index_Building_Time" );
		idx = new DeltaHashIndex(deltaMax, query, stat);
		stat.addMemory( "Mem_3_BuildIndex" );
		stepTime.stopAndAdd( stat );
		stepTime.resetAndStart( "Result_3_2_Join_Time" );

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
