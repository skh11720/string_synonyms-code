package snu.kdd.synonym.synonymRev.algorithm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.Param;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class JoinCatesian extends AlgorithmTemplate {

	// staticitics used for building indexes
	double avgTransformed;
	static Validator checker;

	public JoinCatesian( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
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
		// Setup parameters
		Param params = Param.parseArgs( args, stat, query );
		checker = params.validator;

		StopWatch stepTime = StopWatch.getWatchStarted( "Result_2_Preprocess_Total_Time" );

		preprocess();

		stepTime.stopAndAdd( stat );
		stat.addMemory( "Mem_2_Preprocessed" );
		stepTime.resetAndStart( "Result_3_Run_Time" );

		final List<IntegerPair> list = runAfterPreprocess();

		stepTime.stopAndAdd( stat );
		stepTime.resetAndStart( "Result_4_Write_Time" );

		this.writeResult( list );

		stepTime.stopAndAdd( stat );
	}

	public List<IntegerPair> runAfterPreprocess() {
		List<IntegerPair> rslt = new ArrayList<>();
		for( Record r : query.indexedSet.get() ) {
			for( Record s : query.searchedSet.get() ) {
				if( checker.isEqual( s, r ) >= 0 ) {
					rslt.add( new IntegerPair( r.getID(), s.getID() ) );
				}
			}
		}
		return rslt;
	}

	@Override
	public String getName() {
		return "JoinCatesian";
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

}
