package snu.kdd.synonym.synonymRev.algorithm.delta;

import java.io.IOException;
import java.util.Set;

import org.apache.commons.cli.ParseException;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.algorithm.AbstractPosQGramBasedAlgorithm;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.Param;
import snu.kdd.synonym.synonymRev.tools.StaticFunctions;
import snu.kdd.synonym.synonymRev.tools.StopWatch;

public class JoinDeltaVar extends AbstractPosQGramBasedAlgorithm {

	protected int indexK;
	protected int deltaMax;

	protected JoinDeltaVarIndex idx;
	
	
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
	protected void runAfterPreprocess() {
		StopWatch stepTime = StopWatch.getWatchStarted( "Result_3_1_Index_Building_Time" );

		buildIndex();

		stat.addMemory( "Mem_3_BuildIndex" );
		stepTime.stopAndAdd( stat );
		stepTime.resetAndStart( "Result_3_2_Join_Time" );

		rslt = idx.join( query, stat, checker, writeResult );

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
