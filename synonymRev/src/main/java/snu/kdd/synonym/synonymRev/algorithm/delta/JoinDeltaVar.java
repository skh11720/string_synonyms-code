package snu.kdd.synonym.synonymRev.algorithm.delta;

import java.io.IOException;
import java.util.Set;

import org.apache.commons.cli.ParseException;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.algorithm.AbstractIndexBasedAlgorithm;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.Param;
import snu.kdd.synonym.synonymRev.tools.StaticFunctions;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class JoinDeltaVar extends AbstractIndexBasedAlgorithm {

	protected int indexK;
	protected int qSize;
	protected int deltaMax;

	public Validator checker;
	protected JoinDeltaVarIndex idx;

	protected boolean useLF, usePQF, useSTPQ;
	
	
	public JoinDeltaVar(Query query, String[] args) throws IOException, ParseException {
		super(query, args);
		param = new Param(args);
		indexK = param.getIntParam("indexK");
		qSize = param.getIntParam("qSize");
		deltaMax = param.getIntParam("deltaMax");
		useLF = param.getBooleanParam("useLF");
		usePQF = param.getBooleanParam("usePQF");
		useSTPQ = param.getBooleanParam("useSTPQ");
		checker = new DeltaValidatorDPTopDown(deltaMax);
		
		stat.add("indexK", indexK);
		stat.add("qSize", qSize);
		stat.add("deltaMax", deltaMax);
		stat.add("useLF", useLF);
		stat.add("usePQF", usePQF);
		stat.add("useSTPQ", useSTPQ);
	}
	
	@Override
	protected void executeJoin() {
		if ( usePQF ) runAfterPreprocess();
		else runAfterPreprocessWithoutIndex();
		checker.addStat( stat );
	}

	protected void runAfterPreprocess() {
		StopWatch runTime = null;
		StopWatch stepTime = null;

		runTime = StopWatch.getWatchStarted( "Result_3_Run_Time" );
		stepTime = StopWatch.getWatchStarted( "Result_3_1_Index_Building_Time" );

		buildIndex( writeResult );

		stat.addMemory( "Mem_3_BuildIndex" );
		stepTime.stopAndAdd( stat );
		stepTime.resetAndStart( "Result_3_2_Join_Time" );

		rslt = idx.join( query, stat, checker, writeResult );

		stat.addMemory( "Mem_4_Joined" );
		stepTime.stopAndAdd( stat );

		runTime.stopAndAdd( stat );
	}

	protected void runAfterPreprocessWithoutIndex() {
		rslt = new ObjectOpenHashSet<IntegerPair>();
		StopWatch runTime = StopWatch.getWatchStarted( "Result_3_Run_Time" );
		//stepTime = StopWatch.getWatchStarted( "Result_3_1_Filter_Time" );
		long t_filter = 0;
		long t_verify = 0;

		for ( Record recS : query.searchedSet.recordList ) {
			long ts = System.nanoTime();
			int[] range = recS.getTransLengths();
			ObjectOpenHashSet<Record> candidates = new ObjectOpenHashSet<>();
			for ( Record recT : query.indexedSet.recordList ) {
				if ( !useLF || StaticFunctions.overlap(range[0] - deltaMax, range[1] + deltaMax, recT.size(), recT.size())) {
					candidates.add(recT);
				}
				else ++checker.lengthFiltered;
			}
			
			long afterFilterTime = System.nanoTime();
			for ( Record recT : candidates ) {
				int compare = checker.isEqual(recS, recT);
				if (compare >= 0) {
					addSeqResult( recS, recT, (Set<IntegerPair>)rslt, query.selfJoin );
				}
			}
			long afterVerifyTime = System.nanoTime();
			t_filter += afterFilterTime - ts;
			t_verify += afterVerifyTime - afterFilterTime;
		}

		stat.add( "Result_3_1_Filter_Time", t_filter/1e6 );
		stat.add( "Result_3_2_Verify_Time", t_verify/1e6 );
		
		runTime.stopAndAdd( stat );
	}

	@Override
	protected void buildIndex( boolean writeResult ) {
		JoinDeltaVarIndex.useLF = useLF;
		JoinDeltaVarIndex.usePQF = usePQF;
		JoinDeltaVarIndex.useSTPQ = useSTPQ;
		idx = new JoinDeltaVarIndex(query, indexK, qSize, deltaMax);
		idx.build();
	}

	@Override
	public String getVersion() {
		/*
		 * 1.00: the initial version
		 * 1.01: refactor, consider short strings
		 */
		return "1.01";
	}

	@Override
	public String getName() {
		return "JoinDeltaVar";
	}
}
