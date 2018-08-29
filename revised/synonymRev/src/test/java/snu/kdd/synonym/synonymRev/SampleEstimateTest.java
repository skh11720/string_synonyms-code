package snu.kdd.synonym.synonymRev;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

import org.apache.commons.cli.ParseException;
import org.junit.Ignore;
import org.junit.Test;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.estimation.SampleEstimate;
import snu.kdd.synonym.synonymRev.estimation.SelectivityEstimator;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.validator.TopDownOneSide;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class SampleEstimateTest {

	static final long[] sizeList = {10000, 15848, 25118, 39810, 63095, 100000, 158489, 251188, 398107, 630957, 1000000};
	final int indexK = 1;
	final int qSize = 2;
	Validator checker = new TopDownOneSide();
	String dataset = "AOL";
	
	@Test
	public void test() throws ParseException, IOException {
		testSelectivityEstimation();
//		testVaryingRatio( 10000 );
	}
	
	public void testSelectivityEstimation() throws IOException {
		long target_size = 10000;

		double sampleRatio = 0.05;
		double[] ratio_list = {0.01, 0.02, 0.03, 0.04, 0.05};
//		double sampleRatio = 0.01;
//		double[] ratio_list = {0.002, 0.004, 0.006, 0.008, 0.01};

		// get a query
		Query query = TestUtils.getTestQuery( dataset, target_size );
		StatContainer stat = new StatContainer();

		// sample records
		Random rn = new Random();
		long seed= rn.nextLong();
		System.out.println( "seed: "+seed );
		rn = new Random(seed);
		ObjectArrayList<Record> sampleSearchedList = new ObjectArrayList<>();
		for( Record r : query.searchedSet.recordList ) {
			if ( r.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) continue;
			if( rn.nextDouble() < sampleRatio ) sampleSearchedList.add( r );
		}
		Collections.shuffle( sampleSearchedList, rn );
		ObjectArrayList<Record> sampleIndexedList = sampleSearchedList;

		// estimate
		double[][] output = new double[ratio_list.length][];
		for ( int i=0; i<ratio_list.length; ++i ) {
			double ratio = ratio_list[i];
			ObjectArrayList<Record> sampleSearchedSublist = new ObjectArrayList<Record>(sampleSearchedList.subList( 0, sampleSearchedList.size()/ratio_list.length*(i+1)));
			ObjectArrayList<Record> sampleIndexedSublist = new ObjectArrayList<Record>(sampleIndexedList.subList( 0, sampleIndexedList.size()/ratio_list.length*(i+1)));
			long maxSearchedEstNumRecords = 0;
			long maxIndexedEstNumRecords = 0;
			for( Record rec : sampleIndexedSublist ) {
				rec.preprocessSuffixApplicableRules();
				if( maxIndexedEstNumRecords < rec.getEstNumTransformed() ) {
					maxIndexedEstNumRecords = rec.getEstNumTransformed();
				}
			}
			if( !query.selfJoin ) {
				for( Record rec : sampleSearchedSublist ) {
					rec.preprocessSuffixApplicableRules();
					if( maxSearchedEstNumRecords < rec.getEstNumTransformed() ) {
						maxSearchedEstNumRecords = rec.getEstNumTransformed();
					}
				}
			}
			else {
				maxSearchedEstNumRecords = maxIndexedEstNumRecords;
			}
			SampleEstimate estimate = new SampleEstimate( query, ratio, sampleSearchedSublist, sampleIndexedSublist );
			estimate.estimateJoinHybridWithSample( stat, checker, indexK, qSize );
			estimate.findThetaJoinHybridAll( qSize, indexK, stat, maxIndexedEstNumRecords, maxSearchedEstNumRecords, query.oneSideJoin );
			output[i] = new double[] { estimate.sampleSearchedSize, estimate.mh_term3[estimate.sampleSearchedSize-1], estimate.min_term3[estimate.sampleSearchedSize-1] };
		}
		
		// print outputs
		for ( int i=0; i<ratio_list.length; ++i ) {
			System.out.println( ratio_list[i]+"\t"+output[i][0]+"\t"+output[i][1]+"\t"+output[i][2] );
		}
		
		double[][] x1, x2;
		double[] y;
		SelectivityEstimator selest1, selest2;

		// regression, mh
		x1 = new double[ratio_list.length][];
		x2 = new double[ratio_list.length][];
		y = new double[ratio_list.length];
		for ( int i=0; i<ratio_list.length; ++i ) {
			x1[i] = new double[] {output[i][0]};
			x2[i] = new double[] {output[i][0], output[i][0]*output[i][0]};
			y[i] = output[i][1];
		}
		selest1 = new SelectivityEstimator( x1, y );
		selest2 = new SelectivityEstimator( x2, y );
		System.out.println( "selest1 parameter: "+Arrays.toString( selest1.getParameters() ) );
		System.out.println( "selest2 parameter: "+Arrays.toString( selest2.getParameters() ) );
		System.out.println( "mh prediction: ");
		for ( int i=0; i<ratio_list.length; ++i ) {
//			System.out.println( output[i][0]+"\t"+selest1.predict( x1[i] )+"\t"+selest2.predict( x2[i] )+"\t"+y[i] );
			System.out.println( output[i][0]+"\t"+selest2.predict( x2[i] )+"\t"+y[i] );
		}
		for ( long size : sizeList ) {
//			System.out.println( size+"\t"+selest1.predict( new double[] {size} )+"\t"+selest2.predict( new double[] {size, size*size} ) );
			System.out.println( size+"\t"+selest2.predict( new double[] {size, size*size} ) );
		}
		
		// regression, min
		y = new double[ratio_list.length];
		for ( int i=0; i<ratio_list.length; ++i ) {
			y[i] = output[i][2];
		}
		selest1 = new SelectivityEstimator( x1, y );
		selest2 = new SelectivityEstimator( x2, y );
		System.out.println( "selest1 parameter: "+Arrays.toString( selest1.getParameters() ) );
		System.out.println( "selest2 parameter: "+Arrays.toString( selest2.getParameters() ) );
		System.out.println( "min prediction: ");
		for ( int i=0; i<ratio_list.length; ++i ) {
//			System.out.println( output[i][0]+"\t"+selest1.predict( x1[i] )+"\t"+selest2.predict( x2[i] )+"\t"+y[i] );
			System.out.println( output[i][0]+"\t"+selest2.predict( x2[i] )+"\t"+y[i] );
		}
		for ( long size : sizeList ) {
//			System.out.println( size+"\t"+selest1.predict( new double[] {size} )+"\t"+selest2.predict( new double[] {size, size*size} ) );
			System.out.println( size+"\t"+selest2.predict( new double[] {size, size*size} ) );
		}
	}

	public void testVaryingSize(double sampleRatio) throws ParseException, IOException {
//		final double sampleRatio = 1.00;
//		final int[] sizeList = {10000, 15848, 25118, 39810, 63095, 100000, 158489, 251188, 398107, 630957, 1000000};

		for ( final long size : sizeList ) {
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
