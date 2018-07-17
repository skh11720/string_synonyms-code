package snu.kdd.synonym.synonymRev.algorithm.delta;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.ParseException;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import passjoin.PassJoinIndexForSynonyms;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.synonymRev.data.Dataset;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.Param;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;

public class JoinNaiveDelta2 extends AlgorithmTemplate{
	
	private PassJoinIndexForSynonyms index = null;
	private int deltaMax;

	public JoinNaiveDelta2( Query query, StatContainer stat ) throws IOException {
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
		Param param = Param.parseArgs( args, stat, query );
		this.deltaMax = param.delta;

		StopWatch stepTime = StopWatch.getWatchStarted( "Result_2_Preprocess_Total_Time" );
		stat.add( "cmd_delta", deltaMax );

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
	
	private Set<IntegerPair> runAfterPreprocess() {
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
	
	private void buildIndex( boolean addStat ) {
		index = new PassJoinIndexForSynonyms( query, deltaMax, stat );
	}

	private Set<IntegerPair> join() {
		Set<IntegerPair> rslt = index.join();
		return rslt;
	}
	
	@Override
	public String getName() {
		return "JoinNaiveDelta2";
	}

	@Override
	public String getVersion() {
		/*
		 * 1.00: initial version
		 * 1.01: ignore records with too many transformations
		 */
		return "1.01";
	}
}
