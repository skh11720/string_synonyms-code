package snu.kdd.synonym.synonymRev.algorithm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

import org.apache.commons.cli.ParseException;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.index.JoinMHIndex;
import snu.kdd.synonym.synonymRev.index.JoinMHIndexInterface;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.Param;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StaticFunctions;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class JoinMH extends AlgorithmTemplate {
	// RecordIDComparator idComparator;

	public JoinMH( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}

	public int indexK;
	public int qgramSize;

	public Validator checker;

	/**
	 * Key: twogram<br/>
	 * Value IntervalTree Key: length of record (min, max)<br/>
	 * Value IntervalTree Value: record
	 */

	protected JoinMHIndexInterface idx;

	protected boolean useLF, usePQF;
	
	
	
	protected void setup( Param params ) {
		indexK = params.indexK;
		qgramSize = params.qgramSize;
		checker = params.validator;
		useLF = params.useLF;
		usePQF = params.usePQF;
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
	public void run( Query query, String[] args ) throws IOException, ParseException {
		// System.out.println( Arrays.toString( args ) );
		Param params = Param.parseArgs( args, stat, query );
		setup( params );

		run();

		checker.addStat( stat );
	}

	public void run() {
		StopWatch stepTime = StopWatch.getWatchStarted( "Result_2_Preprocess_Total_Time" );

		preprocess();

		stat.addMemory( "Mem_2_Preprocessed" );

		stepTime.stopAndAdd( stat );

		if ( usePQF ) runAfterPreprocess();
		else runAfterPreprocessWithoutIndex();
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

		rslt = idx.join( stat, query, checker, writeResult );

		stat.addMemory( "Mem_4_Joined" );
		stepTime.stopAndAdd( stat );

		runTime.stopAndAdd( stat );

		stepTime.resetAndStart( "Result_4_Write_Time" );

		writeResult();

		stepTime.stopAndAdd( stat );
	}

	public void runAfterPreprocessWithoutIndex() {
		StopWatch runTime = null;
		//StopWatch stepTime = null;

		runTime = StopWatch.getWatchStarted( "Result_3_Run_Time" );
		//stepTime = StopWatch.getWatchStarted( "Result_3_1_Filter_Time" );
		long t_filter = 0;
		long t_verify = 0;

		for ( Record recS : query.searchedSet.recordList ) {
			long ts = System.nanoTime();
			int[] range = recS.getTransLengths();
			ObjectOpenHashSet<Record> candidates = new ObjectOpenHashSet<>();
			for ( Record recT : query.indexedSet.recordList ) {
				int[] otherRange = recT.getTransLengths();
				if ( !useLF || StaticFunctions.overlap(range[0], range[1], otherRange[0], otherRange[1])) {
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

		stat.add( "Result_3_1_Filter_Time", t_filter/1e6 );
		stat.add( "Result_3_2_Verify_Time", t_verify/1e6 );
		
		runTime.stopAndAdd( stat );
	}

	protected void buildIndex( boolean writeResult ) {
		int[] indexPosition = new int[ indexK ];
		for( int i = 0; i < indexK; i++ ) {
			indexPosition[ i ] = i;
		}
		idx = new JoinMHIndex( indexK, qgramSize, query.indexedSet.get(), query, stat, indexPosition, writeResult, true, 0 );
	}

	@Override
	public String getVersion() {
		/*
		 * 2.5: the latest version by yjpark
		 * 2.51: checkpoint
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
