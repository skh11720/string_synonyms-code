package snu.kdd.synonym.synonymRev.algorithm.delta;

import java.io.IOException;
import java.util.Set;

import org.apache.commons.cli.ParseException;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.Param;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StaticFunctions;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class JoinDeltaSimple extends AlgorithmTemplate {

	protected int qSize;
	protected int deltaMax;

	public Validator checker;
	protected JoinDeltaSimpleIndex idx;

	protected boolean useLF, usePQF;
	
	

	public JoinDeltaSimple(Query query, String[] args) throws IOException, ParseException {
		super(query, args);
		param = new Param(args);
		qSize = param.getIntParam("qSize");
		deltaMax = param.getIntParam("deltaMax");
		useLF = param.getBooleanParam("useLF");
		usePQF = param.getBooleanParam("usePQF");
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

	protected void buildIndex( boolean writeResult ) {
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
