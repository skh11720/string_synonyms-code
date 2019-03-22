package snu.kdd.synonym.synonymRev.algorithm.delta;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.algorithm.AbstractPosQGramBasedAlgorithm;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.ResultSet;
import snu.kdd.synonym.synonymRev.tools.Stat;
import snu.kdd.synonym.synonymRev.tools.StaticFunctions;
import snu.kdd.synonym.synonymRev.tools.StopWatch;

public class JoinDeltaSimple extends AbstractPosQGramBasedAlgorithm {

	public final int deltaMax;
	public final String distFunc;

	protected JoinDeltaSimpleIndex idx;


	public JoinDeltaSimple(String[] args) {
		super(args);
		deltaMax = param.getIntParam("deltaMax");
		distFunc = param.getStringParam("dist");
		useLF = param.getBooleanParam("useLF");
		usePQF = param.getBooleanParam("usePQF");
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
		rslt = new ResultSet(query.selfJoin);
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
				if ( rslt.contains(recS, recT) ) continue;
				int compare = checker.isEqual(recS, recT);
				if (compare >= 0) {
					rslt.add(recS, recT);
				}
			}
			long afterVerifyTime = System.nanoTime();
			t_filter += afterFilterTime - ts;
			t_verify += afterVerifyTime - afterFilterTime;
		}

		stat.add( Stat.FILTER_TIME, t_filter/1e6 );
		stat.add( Stat.VERIFY_TIME, t_verify/1e6 );
	}

	@Override
	protected void buildIndex() {
		idx = new JoinDeltaSimpleIndex( qSize, deltaMax, query );
		JoinDeltaSimpleIndex.useLF = useLF;
		JoinDeltaSimpleIndex.usePQF = usePQF;
	}

	@Override
	public String getVersion() {
		/*
		 * 1.00: the initial version
		 * 1.01: major update
		 */
		return "1.01";
	}

	@Override
	public String getName() {
		return "JoinDeltaSimple";
	}
	
	@Override
	public String getNameWithParam() {
		return String.format("%s_%d_%d_%s", getName(), qSize, deltaMax, distFunc);
	}
}
