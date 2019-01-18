package snu.kdd.synonym.synonymRev.algorithm;

import java.io.IOException;
import java.util.Set;

import org.apache.commons.cli.ParseException;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.index.JoinMHIndex;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.Param;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StaticFunctions;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.validator.TopDownOneSide;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class JoinMH extends AlgorithmTemplate {
	// RecordIDComparator idComparator;

	public int indexK;
	public int qSize;

	public Validator checker;

	/**
	 * Key: twogram<br/>
	 * Value IntervalTree Key: length of record (min, max)<br/>
	 * Value IntervalTree Value: record
	 */

	protected JoinMHIndex idx;

	protected boolean useLF, usePQF, useSTPQ;
	
	
	public JoinMH(Query query, StatContainer stat, String[] args) throws IOException, ParseException {
		super(query, stat, args);
		param = new Param(args);
		indexK = param.getIntParam("indexK");
		qSize = param.getIntParam("qSize");
		checker = new TopDownOneSide();
		useLF = param.getBooleanParam("useLF");
		usePQF = param.getBooleanParam("usePQF");
		useSTPQ = param.getBooleanParam("useSTPQ");
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
		stat.addMemory( "Mem_2_Preprocessed" );

		stepTime.stopAndAdd( stat );

		if ( usePQF ) runAfterPreprocess();
		else runAfterPreprocessWithoutIndex();

		checker.addStat( stat );
		stepTime.resetAndStart( "Result_4_Write_Time" );
		writeResult();
		stepTime.stopAndAdd( stat );
	}

	public void runAfterPreprocess() {
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

	public void runAfterPreprocessWithoutIndex() {
		rslt = new ObjectOpenHashSet<IntegerPair>();
		StopWatch runTime = null;
		//StopWatch stepTime = null;

		runTime = StopWatch.getWatchStarted( "Result_3_Run_Time" );
		//stepTime = StopWatch.getWatchStarted( "Result_3_1_Filter_Time" );
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
		
		runTime.stopAndAdd( stat );
	}

	protected void buildIndex( boolean writeResult ) {
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
