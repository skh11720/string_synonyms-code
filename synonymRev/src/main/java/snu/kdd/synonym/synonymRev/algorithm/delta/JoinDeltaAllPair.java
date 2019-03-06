package snu.kdd.synonym.synonymRev.algorithm.delta;

import java.util.Set;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.algorithm.AbstractParameterizedAlgorithm;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.index.AbstractIndex;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StaticFunctions;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.validator.Validator;

@Deprecated
public class JoinDeltaAllPair extends AbstractParameterizedAlgorithm {

	public final int qSize;
	public final int deltaMax;
	public final String distFunc;
	
	public static boolean useLF = true;

	
	public JoinDeltaAllPair(String[] args) {
		super(args);
		qSize = param.getIntParam("qSize");
		deltaMax = param.getIntParam("deltaMax");
		distFunc = param.getStringParam("dist");
		useLF = param.getBooleanParam("useLF");
	}
	
	@Override
	public void initialize() {
		super.initialize();
		checker = new DeltaValidatorDPTopDown(deltaMax, distFunc);
	}

	@Override
	protected void reportParamsToStat() {
		stat.add("Param_qSize", qSize);
		stat.add("Param_deltaMax", deltaMax);
	}

	@Override
	protected void executeJoin() {
		StopWatch stepTime = StopWatch.getWatchStarted( INDEX_BUILD_TIME );
		Index idx = new Index();
		stat.addMemory( "Mem_3_BuildIndex" );
		stepTime.stopAndAdd( stat );
		stepTime.resetAndStart( JOIN_AFTER_INDEX_TIME );

		rslt = idx.join( query, stat, checker, writeResultOn );

		stat.addMemory( "Mem_4_Joined" );
		stepTime.stopAndAdd( stat );
	}
	
	private class Index extends AbstractIndex {

		long t_filter = 0;
		long t_verify = 0;

		@Override
		protected void joinOneRecord(Record recS, Set<IntegerPair> rslt, Validator checker) {
			long ts = System.nanoTime();
			int[] range = recS.getTransLengths();
			ObjectOpenHashSet<Record> candidates = new ObjectOpenHashSet<>();
			for ( Record recT : query.indexedSet.recordList ) {
				if ( !useLF || StaticFunctions.overlap(range[0] - deltaMax, range[1] + deltaMax, recT.size(), recT.size()) ) {
					candidates.add(recT);
				}
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

		@Override
		protected void postprocessAfterJoin(StatContainer stat) {
			stat.add( "Result_3_1_Filter_Time", t_filter/1e6 );
			stat.add( "Result_3_2_Verify_Time", t_verify/1e6 );
		}
		
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
		return "JoinDeltaAllPair";
	}
	
	@Override
	public String getNameWithParam() {
		return String.format("%s_%d_%d_%s", getName(), qSize, deltaMax, distFunc);
	}
}
