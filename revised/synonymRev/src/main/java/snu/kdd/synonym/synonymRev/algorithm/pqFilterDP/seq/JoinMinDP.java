package snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.seq;


import java.io.IOException;

import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.synonymRev.algorithm.JoinMin;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.validator.TopDownOneSide;

public class JoinMinDP extends JoinMin {
	public int qSize = 0;
	public int indexK = 0;
	protected String mode;

	public JoinMinDP( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}

	@Override
	public void run( Query query, String[] args ) throws IOException, ParseException {
		ParamPQFilterDP params = ParamPQFilterDP.parseArgs( args, stat, query );

		// Setup parameters
		checker = new TopDownOneSide();
		qSize = params.qgramSize;
		indexK = params.indexK;
		mode = params.mode;

		StopWatch preprocessTime = StopWatch.getWatchStarted( "Result_2_Preprocess_Total_Time" );

		preprocess();

		preprocessTime.stopAndAdd( stat );

		stat.addMemory( "Mem_2_Preprocessed" );
		preprocessTime.resetAndStart( "Result_3_Run_Time" );

		runWithoutPreprocess( true );

		preprocessTime.stopAndAdd( stat );

		checker.addStat( stat );
	}

	@Override
	protected void buildIndex( boolean writeResult ) throws IOException {
		if ( mode.equals( "dp1" ) ) idx = new PQFilterMinIndex( indexK, qSize, stat, query, 0, writeResult );
		else if ( mode.equals( "dp3" ) ) idx = new PQFilterMinIndexInc( indexK, qSize, stat, query, 0, writeResult );
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

	@Override
	public String getName() {
		return "JoinMinDP";
	}
}
