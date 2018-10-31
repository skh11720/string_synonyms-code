package snu.kdd.synonym.synonymRev.algorithm;

import java.io.IOException;
import java.util.Set;

import org.apache.commons.cli.ParseException;

import passjoin.PassJoinIndexForSynonyms;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;

public class PassJoinExact extends AlgorithmTemplate{
	
	protected PassJoinIndexForSynonyms index = null;
	protected int deltaMax;

	public PassJoinExact( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}
	
	@Override
	protected void preprocess() {
		super.preprocess();
		for( Record rec : query.searchedSet.get() ) {
			rec.preprocessSuffixApplicableRules();
		}
	}

	@Override
	public void run( Query query, String[] args ) throws IOException, ParseException {

		StopWatch stepTime = StopWatch.getWatchStarted( "Result_2_Preprocess_Total_Time" );

		preprocess();

		stepTime.stopAndAdd( stat );
		stat.addMemory( "Mem_2_Preprocessed" );
		stepTime.resetAndStart( "Result_3_Run_Time" );

		rslt = runAfterPreprocess();

		stepTime.stopAndAdd( stat );
		stepTime.resetAndStart( "Result_4_Write_Time" );

		this.writeResult();

		stepTime.stopAndAdd( stat );
	}
	
	protected Set<IntegerPair> runAfterPreprocess() {
		StopWatch stepTime = null;
		stepTime = StopWatch.getWatchStarted( "Result_3_1_Index_Building_Time" );
		buildIndex( writeResult );

		stat.addMemory( "Mem_3_BuildIndex" );
		stepTime.stopAndAdd( stat );
		stepTime.resetAndStart( "Result_3_2_Join_Time" );

		Set<IntegerPair> rslt = join();

		stat.addMemory( "Mem_4_Joined" );
		stepTime.stopAndAdd( stat );
		return rslt;
	}
	
	protected void buildIndex( boolean addStat ) {
		index = new PassJoinIndexForSynonyms( query, deltaMax, stat );
	}

	protected Set<IntegerPair> join() {
		Set<IntegerPair> rslt = index.join();
		return rslt;
	}
	
	@Override
	public String getName() {
		return "PassJoinExact";
	}

	@Override
	public String getVersion() {
		return "1.01";
	}
}
