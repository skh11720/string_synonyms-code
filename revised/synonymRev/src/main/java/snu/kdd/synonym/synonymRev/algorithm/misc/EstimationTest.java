package snu.kdd.synonym.synonymRev.algorithm.misc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.estimation.SampleEstimate;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.Param;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class EstimationTest extends AlgorithmTemplate {

	public EstimationTest( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}

	private long maxSearchedEstNumRecords = 0;
	private long maxIndexedEstNumRecords = 0;

	public Validator checker;
	SampleEstimate estimate;
	private int qSize = 0;
	private int indexK = 0;
	private double sampleRatio = 0;
	private int joinThreshold = 1;
	private boolean joinMinRequired = true;

	@Override
	public void preprocess() {
		super.preprocess();

		for( Record rec : query.indexedSet.get() ) {
			rec.preprocessSuffixApplicableRules();
			if( maxIndexedEstNumRecords < rec.getEstNumTransformed() ) {
				maxIndexedEstNumRecords = rec.getEstNumTransformed();
			}
		}
		if( !query.selfJoin ) {
			for( Record rec : query.searchedSet.get() ) {
				rec.preprocessSuffixApplicableRules();
				if( maxSearchedEstNumRecords < rec.getEstNumTransformed() ) {
					maxSearchedEstNumRecords = rec.getEstNumTransformed();
				}
			}
		}
		else {
			maxSearchedEstNumRecords = maxIndexedEstNumRecords;
		}
	}

	@Override
	public String getName() {
		return "EstimationTest";
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

	@Override
	public void run( Query query, String[] args ) throws IOException, ParseException {
		Param params = Param.parseArgs( args, stat, query );
		// Setup parameters
		checker = params.validator;
		qSize = params.qgramSize;
		indexK = params.indexK;
		sampleRatio = params.sampleRatio;

		StopWatch stepTime = StopWatch.getWatchStarted( "Result_2_Preprocess_Total_Time" );
		preprocess();
		stepTime.stopAndAdd( stat );
		// Retrieve statistics

		stepTime.resetAndStart( "Result_3_Run_Time" );
		// Estimate constants

		Collection<IntegerPair> rslt = join();
		stepTime.stopAndAdd( stat );
		stat.addMemory( "Mem_4_Joined" );

		stepTime.resetAndStart( "Result_4_Write_Time" );

		stepTime.stopAndAdd( stat );
	}

	private ArrayList<IntegerPair> join() {

		StopWatch buildTime = StopWatch.getWatchStarted( "Result_3_1_Index_Building_Time" );
		findConstants( sampleRatio );

		joinThreshold = estimate.findThetaUnrestricted( qSize, stat, maxIndexedEstNumRecords, maxSearchedEstNumRecords,
				query.oneSideJoin );

		if( Long.max( maxSearchedEstNumRecords, maxIndexedEstNumRecords ) <= joinThreshold ) {
			joinMinRequired = false;
		}

		Util.printLog( "Selected Threshold: " + joinThreshold );
		return null;
	}

	private void findConstants( double sampleratio ) {
		// Sample
		estimate = new SampleEstimate( query, sampleratio, query.selfJoin );
		estimate.estimateWithSample( stat, checker, indexK, qSize );
	}
}
