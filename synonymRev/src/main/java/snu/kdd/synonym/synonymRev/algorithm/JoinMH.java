package snu.kdd.synonym.synonymRev.algorithm;

import java.util.Set;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.index.JoinMHIndex;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.StaticFunctions;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.validator.TopDownOneSide;

public class JoinMH extends AbstractPosQGramBasedAlgorithm {
	// RecordIDComparator idComparator;

	public final int indexK;

	/**
	 * Key: twogram<br/>
	 * Value IntervalTree Key: length of record (min, max)<br/>
	 * Value IntervalTree Value: record
	 */

	protected JoinMHIndex idx;

	
	public JoinMH(Query query, String[] args) {
		super(query, args);
		indexK = param.getIntParam("indexK");
		useLF = param.getBooleanParam("useLF");
		usePQF = param.getBooleanParam("usePQF");
		useSTPQ = param.getBooleanParam("useSTPQ");
		checker = new TopDownOneSide();
	}
	
	@Override
	protected void reportParamsToStat() {
		stat.add("Param_indexK", indexK);
		stat.add("param_qSize", qSize);
		stat.add("Param_useLF", useLF);
		stat.add("Param_usePQF", usePQF);
		stat.add("Param_useSTPQ", useSTPQ);
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
		long t_filter = 0;
		long t_verify = 0;

		for ( Record recS : query.searchedSet.recordList ) {
			if ( recS.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) continue;
			long ts = System.nanoTime();
			int[] range = recS.getTransLengths();
			ObjectOpenHashSet<Record> candidates = new ObjectOpenHashSet<>();
			for ( Record recT : query.indexedSet.recordList ) {
				if ( !useLF || StaticFunctions.overlap(range[0], range[1], recT.size(), recT.size())) {
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

		stat.add( "Result_5_1_Filter_Time", t_filter/1e6 );
		stat.add( "Result_5_2_Verify_Time", t_verify/1e6 );
	}

	@Override
	protected void buildIndex() {
		int[] indexPosition = new int[ indexK ];
		for( int i = 0; i < indexK; i++ ) {
			indexPosition[ i ] = i;
		}
		idx = new JoinMHIndex( indexK, qSize, query.indexedSet.get(), query, stat, indexPosition, writeResult, true, 0 );
		JoinMHIndex.useLF = useLF;
		JoinMHIndex.usePQF = usePQF;
		JoinMHIndex.useSTPQ = useSTPQ;
	}

	@Override
	public String getVersion() {
		/*
		 * 2.5: the latest version by yjpark
		 * 2.51: checkpoint
		 * 2.511: test for filtering power test
		 */
		return "2.511";
	}

	@Override
	public String getName() {
		return "JoinMH";
	}

	public double getGamma() {
		return idx.getGamma();
	}

	public double getZeta() {
		return idx.getZeta();
	}

	public double getEta() {
		return idx.getEta();
	}
}
