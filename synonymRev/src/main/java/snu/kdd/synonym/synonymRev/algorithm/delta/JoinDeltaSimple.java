package snu.kdd.synonym.synonymRev.algorithm.delta;

import java.util.Set;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.algorithm.AbstractPosQGramBasedAlgorithm;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.StaticFunctions;
import snu.kdd.synonym.synonymRev.tools.StopWatch;

public class JoinDeltaSimple extends AbstractPosQGramBasedAlgorithm {

	public final int deltaMax;
	public final String distFunc;

	protected JoinDeltaSimpleIndex idx;


	public JoinDeltaSimple(Query query, String[] args) {
		super(query, args);
		deltaMax = param.getIntParam("deltaMax");
		distFunc = param.getStringParam("dist");
		useLF = param.getBooleanParam("useLF");
		usePQF = param.getBooleanParam("usePQF");
		checker = new DeltaValidatorDPTopDown(deltaMax, distFunc);
	}

	@Override
	protected void reportParamsToStat() {
		stat.add("Param_qSize", qSize);
		stat.add("Param_deltaMax", deltaMax);
		stat.add("Param_distFunct", distFunc);
		stat.add("Param_useLF", useLF);
		stat.add("Param_usePQF", usePQF);
	}

	@Override
	protected void runAfterPreprocess() {
		StopWatch stepTime = StopWatch.getWatchStarted( INDEX_BUILD_TIME );

		buildIndex();

		stat.addMemory( "Mem_3_BuildIndex" );
		stepTime.stopAndAdd( stat );
		stepTime.resetAndStart( JOIN_AFTER_INDEX_TIME );

		rslt = idx.join( query, stat, checker, writeResultOn );

		stat.addMemory( "Mem_4_Joined" );
		stepTime.stopAndAdd( stat );
	}

	@Override
	protected void runAfterPreprocessWithoutIndex() {
		rslt = new ObjectOpenHashSet<IntegerPair>();
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
	}

	@Override
	protected void buildIndex() {
		idx = new JoinDeltaSimpleIndex( qSize, deltaMax, query, stat );
		JoinDeltaSimpleIndex.useLF = useLF;
		JoinDeltaSimpleIndex.usePQF = usePQF;
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
		return "JoinDeltaSimple";
	}
	
	@Override
	public String getOutputName() {
		return String.format( "%s_d%d", getName(), deltaMax );
	}
}
