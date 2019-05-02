package snu.kdd.synonym.synonymRev;

import java.io.IOException;

import org.apache.commons.cli.ParseException;
import org.junit.Test;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.estimation.SampleEstimate;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.validator.TopDownOneSide;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class SampleEstimateTest {

	static final Object2ObjectOpenHashMap<String, long[]> sizeList;
	static {
		final long[] sizeList1 = {10000, 15848, 25118, 39810, 63095, 100000, 158489, 251188, 398107, 630957, 1000000};
		final long[] sizeList2 = {10000, 15848, 25118, 39810, 63095, 100000, 158489, 251188, 466158};
		sizeList = new Object2ObjectOpenHashMap<>();
		sizeList.put( "AOL", sizeList1 );
		sizeList.put( "SPROT", sizeList2 );
		sizeList.put( "USPS", sizeList1 );
	}
	final int indexK = 1;
	final int qSize = 2;
	Validator checker = new TopDownOneSide();
	String dataset = "AOL";
	
	@Test
	public void test() throws ParseException, IOException {
//		testVaryingRatio( 10000 );
//		for ( String dataset : new String[] {"AOL", "SPROT", "USPS"} ) {
//			System.out.println( dataset+": " + Arrays.toString( getEstNumTrans( dataset ) ) );
//		}
	}
	

	public void testVaryingSize(double sampleRatio) throws ParseException, IOException {
//		final double sampleRatio = 1.00;
//		final int[] sizeList = {10000, 15848, 25118, 39810, 63095, 100000, 158489, 251188, 398107, 630957, 1000000};

		for ( final long size : sizeList.get( dataset ) ) {
			runSampleEstimate( this.dataset, size, sampleRatio );
		}
	}
	
	public void testVaryingRatio(int size) throws IOException {
		final double[] sampleRatioList = {0.01, 0.02, 0.05, 0.1, 0.2, 0.5, 1.0};
//		final int size = 100000;
		
		for ( final double sampleRatio : sampleRatioList ) {
			runSampleEstimate( this.dataset, size, sampleRatio );
		}
	}

	public SampleEstimate runSampleEstimate( String dataset, long size, double sampleRatio ) throws IOException {
		Query query = TestUtils.getTestQuery( dataset, size );
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
		return estimate;
	}
}
