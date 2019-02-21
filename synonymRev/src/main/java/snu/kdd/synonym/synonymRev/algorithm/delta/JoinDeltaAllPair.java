package snu.kdd.synonym.synonymRev.algorithm.delta;

import java.util.Set;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.algorithm.AbstractAlgorithm;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.index.AbstractIndex;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StaticFunctions;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.validator.Validator;

@Deprecated
public class JoinDeltaAllPair extends AbstractAlgorithm {

	protected int qSize;
	protected int deltaMax;
	
	public static boolean useLF = true;

	
	public JoinDeltaAllPair(Query query, String[] args) {
		super(query, args);
		qSize = param.getIntParam("qSize");
		deltaMax = param.getIntParam("deltaMax");
		checker = new DeltaValidatorDPTopDown(deltaMax);
		useLF = param.getBooleanParam("useLF");
	}

	@Override
	protected void executeJoin() {
		StopWatch stepTime = StopWatch.getWatchStarted( "Result_3_1_Index_Building_Time" );
		Index idx = new Index();
		stat.addMemory( "Mem_3_BuildIndex" );
		stepTime.stopAndAdd( stat );
		stepTime.resetAndStart( "Result_3_2_Join_Time" );

		rslt = idx.join( query, stat, checker, writeResult );

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
}
