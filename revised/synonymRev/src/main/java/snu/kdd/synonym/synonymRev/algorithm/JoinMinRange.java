package snu.kdd.synonym.synonymRev.algorithm;

import java.io.IOException;
import java.util.Collection;

import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.index.JoinMinRangeIndex;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.Param;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.tools.WYK_HashMap;
import snu.kdd.synonym.synonymRev.tools.WYK_HashSet;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class JoinMinRange extends AlgorithmTemplate {
	public int qSize = 0;
	public int indexK = 0;

	public Validator checker;

	/**
	 * Key: (2gram, index) pair<br/>
	 * Value: (min, max, record) triple
	 */
	public JoinMinRangeIndex idx;

	public JoinMinRange( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}

	@Override
	public void preprocess() {
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

	private void buildIndex( boolean writeResult ) throws IOException {
		idx = new JoinMinRangeIndex( indexK, qSize, stat, query, writeResult );
	}

	public void statistics() {
		long strlengthsum = 0;

		int strs = 0;
		int maxstrlength = 0;

		long rhslengthsum = 0;
		int rules = 0;
		int maxrhslength = 0;

		for( Record rec : query.searchedSet.get() ) {
			int length = rec.getTokenCount();
			++strs;
			strlengthsum += length;
			maxstrlength = Math.max( maxstrlength, length );
		}

		for( Record rec : query.indexedSet.get() ) {
			int length = rec.getTokenCount();
			++strs;
			strlengthsum += length;
			maxstrlength = Math.max( maxstrlength, length );
		}

		for( Rule rule : query.ruleSet.get() ) {
			int length = rule.getRight().length;
			++rules;
			rhslengthsum += length;
			maxrhslength = Math.max( maxrhslength, length );
		}

		Util.printLog( "Average str length: " + strlengthsum + "/" + strs );
		Util.printLog( "Maximum str length: " + maxstrlength );
		Util.printLog( "Average rhs length: " + rhslengthsum + "/" + rules );
		Util.printLog( "Maximum rhs length: " + maxrhslength );
	}

	public void runWithoutPreprocess( boolean writeResult ) throws IOException {
		// Retrieve statistics
		StopWatch stepTime = null;
		if( writeResult ) {
			statistics();
			stepTime = StopWatch.getWatchStarted( "Result_3_1_Index_Building_Time" );
		}

		buildIndex( writeResult );

		if( writeResult ) {
			stat.addMemory( "Mem_3_BuildIndex" );
			stepTime.stopAndAdd( stat );
			stepTime.resetAndStart( "Result_3_2_Join_Time" );
		}

		Collection<IntegerPair> rslt = null;

		rslt = idx.joinMaxK( indexK, writeResult, stat, checker, query );
		if( writeResult ) {
			stepTime.stopAndAdd( stat );
			stat.addMemory( "Mem_4_Joined" );
		}

		if( DEBUG.JoinMinON ) {
			if( writeResult ) {
				stat.add( "Counter_Final_1_HashCollision", WYK_HashSet.collision );
				stat.add( "Counter_Final_1_HashResize", WYK_HashSet.resize );

				stat.add( "Counter_Final_2_MapCollision", WYK_HashMap.collision );
				stat.add( "Counter_Final_2_MapResize", WYK_HashMap.resize );
			}
			else {
				stat.add( "Sample_JoinMin_Result", rslt.size() );
				stepTime.stop();
			}
		}

		if( writeResult ) {
			stepTime.resetAndStart( "Result_4_Write_Time" );
			this.writeResult( rslt );
			stepTime.stopAndAdd( stat );
		}
	}

	@Override
	public void run( Query query, String[] args ) throws IOException, ParseException {
		Param params = Param.parseArgs( args, stat, query );

		// Setup parameters
		checker = params.validator;
		qSize = params.qgramSize;
		indexK = params.indexK;

		StopWatch preprocessTime = StopWatch.getWatchStarted( "Result_2_Preprocess_Total_Time" );

		preprocess();

		preprocessTime.stopAndAdd( stat );

		stat.addMemory( "Mem_2_Preprocessed" );
		preprocessTime.resetAndStart( "Result_3_Run_Time" );

		runWithoutPreprocess( true );

		preprocessTime.stopAndAdd( stat );

		checker.addStat( stat );
	}

	public double getGamma() {
		return idx.gamma;
	}

	public double getDelta() {
		return idx.delta;
	}

	public double getEpsilon() {
		return idx.epsilon;
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

	@Override
	public String getName() {
		return "JoinMinRange";
	}
}
