package snu.kdd.synonym.synonymRev.algorithm;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.index.JoinMHIndex_Split;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.Param;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class JoinMH_Split extends AlgorithmTemplate {
	// RecordIDComparator idComparator;

	public JoinMH_Split( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}

	public int indexK = 3;
	public int qgramSize = 2;

	static Validator checker;

	/**
	 * Key: twogram<br/>
	 * Value IntervalTree Key: length of record (min, max)<br/>
	 * Value IntervalTree Value: record
	 */

	JoinMHIndex_Split idx;

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

		indexK = params.indexK;
		qgramSize = params.qgramSize;

		// Setup parameters
		checker = params.validator;

		run();

		checker.addStat( stat );
	}

	public void run() {
		StopWatch stepTime = null;
		StopWatch runTime = null;

		stepTime = StopWatch.getWatchStarted( "Result_2_Preprocess_Total_Time" );

		preprocess();

		stat.addMemory( "Mem_2_Preprocessed" );

		stepTime.stopAndAdd( stat );
		stepTime.resetAndStart( "Result_3_1_Index_Building_Time" );

		runTime = StopWatch.getWatchStarted( "Result_3_Run_Time" );

		buildIndex();

		stat.addMemory( "Mem_3_BuildIndex" );
		stepTime.stopAndAdd( stat );
		stepTime.resetAndStart( "Result_3_2_Join_Time" );

		ArrayList<IntegerPair> rslt = idx.join( stat, query, checker );

		stat.addMemory( "Mem_4_Joined" );
		stepTime.stopAndAdd( stat );

		runTime.stopAndAdd( stat );

		stepTime.resetAndStart( "Result_4_Write_Time" );

		writeResult( rslt );

		stepTime.stopAndAdd( stat );
	}

	private void buildIndex() {
		int[] indexPosition = new int[ indexK ];
		for( int i = 0; i < indexK; i++ ) {
			indexPosition[ i ] = i;
		}
		idx = new JoinMHIndex_Split( indexK, qgramSize, query.indexedSet.get(), query, stat, indexPosition );
	}

	@Override
	public String getVersion() {
		return "2.0";
	}

	@Override
	public String getName() {
		return "JoinMHSP";
	}

}
