package snu.kdd.synonym.synonymRev.algorithm.delta;

import java.io.IOException;
import java.util.Set;

import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.index.NaiveIndex;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.Param;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;

public class JoinNaiveDelta extends AlgorithmTemplate{
	
	private NaiveIndex index;
	private int deltaMax;

	public JoinNaiveDelta( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
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

		Set<IntegerPair> rslt = index.join( query, stat, writeResult );

		stat.addMemory( "Mem_4_Joined" );
		stepTime.stopAndAdd( stat );
		return rslt;
	}
	
	private void buildIndex( boolean addStat ) {
		long avgTransformed = 0;
		for( Record rec : query.indexedSet.get() ) {
			avgTransformed += rec.getEstNumTransformed();
		}
		avgTransformed /= query.indexedSet.size();
		if ( deltaMax == 0 ) index = new NaiveIndex( query.indexedSet, query, stat, addStat, -1, avgTransformed );
		else index = new NaiveDeltaIndex( query.indexedSet, query, stat, addStat, deltaMax, -1, avgTransformed );
	}

	@Override
	public String getName() {
		return "JoinNaiveDelta";
	}

	@Override
	public String getVersion() {
		return "1.00";
	}
}
