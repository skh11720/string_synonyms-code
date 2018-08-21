package snu.kdd.synonym.synonymRev;

import java.io.IOException;

import org.apache.commons.cli.ParseException;
import org.junit.Test;

import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.estimation.SampleEstimate;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.validator.TopDownOneSide;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class SampleEstimateTest {

	final int indexK = 1;
	final int qSize = 2;
	Validator checker = new TopDownOneSide();
	
	@Test 
	public void test() throws ParseException, IOException {
		testVaryingSize(1.00);
	}

	public void testVaryingSize(double sampleRatio) throws ParseException, IOException {
//		final double sampleRatio = 1.00;
		final int[] sizeList = {10000, 15848, 25118, 39810, 63095, 100000, 158489, 251188, 398107, 630957, 1000000};

		for ( final int size : sizeList ) {
			runSampleEstimate( size, sampleRatio );
		}
	}
	
	public void testVaryingRatio(int size) throws IOException {
		final double[] sampleRatioList = {0.01, 0.02, 0.05, 0.1, 0.2, 0.5, 1.0};
//		final int size = 100000;
		
		for ( final double sampleRatio : sampleRatioList ) {
			runSampleEstimate( size, sampleRatio );
		}
	}

	public void runSampleEstimate( int size, double sampleRatio ) throws IOException {
		Query query = TestUtils.getTestQuery( "USPS", size );
		StatContainer stat = new StatContainer();

		long maxSearchedEstNumRecords = 0;
		long maxIndexedEstNumRecords = 0;

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
		
		SampleEstimate estimate = new SampleEstimate( query, sampleRatio, query.selfJoin );
		estimate.estimateJoinHybridWithSample( stat, checker, indexK, qSize );
		estimate.findThetaJoinHybridAll( qSize, indexK, stat, maxIndexedEstNumRecords, maxSearchedEstNumRecords, query.oneSideJoin );
	}
}
