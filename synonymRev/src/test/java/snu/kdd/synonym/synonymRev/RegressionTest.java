package snu.kdd.synonym.synonymRev;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

import org.apache.commons.lang.ArrayUtils;
import org.junit.Test;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.estimation.SampleEstimate;
import snu.kdd.synonym.synonymRev.estimation.SelectivityEstimator;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.validator.TopDownOneSide;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class RegressionTest {

	static final double sampleRatio = 0.05;
	static final int num_ratio = 5;

	static final Object2ObjectOpenHashMap<String, long[]> sizeList;
	static final Object2ObjectOpenHashMap<String, long[]> term3List;
	static final Object2ObjectOpenHashMap<String, long[]> estNumTransList;
	static BufferedWriter bw = null;
	static {
		final long[] sizeList1 = {10000, 15848, 25118, 39810, 63095, 100000, 158489, 251188, 398107, 630957, 1000000};
		final long[] sizeList2 = {10000, 15848, 25118, 39810, 63095, 100000, 158489, 251188, 466158};
		sizeList = new Object2ObjectOpenHashMap<>();
		sizeList.put( "AOL", sizeList1 );
		sizeList.put( "SPROT", sizeList2 );
		sizeList.put( "USPS", sizeList1 );
		
		term3List = new Object2ObjectOpenHashMap<>();
		term3List.put( "AOL,mh", new long[] {142364, 210920, 291105, 541433, 1983102, 2949982, 5109236, 8583779, 17123674, 25993872, 65460941,} );
		term3List.put( "AOL,min", new long[] {11443, 18213, 29176, 48381, 79138, 130165, 215538, 385146, 688274, 1265201, 2391504,} );
		term3List.put( "SPROT,mh", new long[] {110123, 267949, 679963, 1679696, 4225553, 10423874, 26353647, 65236482, 223463522,} );
		term3List.put( "SPROT,min", new long[] {10047, 15981, 25496, 41043, 66593, 109279, 182870, 316332, 698853,} );
		term3List.put( "USPS,mh", new long[] {829932, 1782112, 4837507, 11830181, 29703376, 75132831, 191796914, 474173740, 1178350907, 2951942821L,} );
		term3List.put( "USPS,min", new long[] {10000, 15848, 25118, 39811, 63095, 100009, 158590, 252126, 404218, 660015,} );
		
		estNumTransList = new Object2ObjectOpenHashMap<>();
		estNumTransList.put( "AOL", new long[] {171217086, 212220745, 237825124, 517197097, 704104642, 140978317787L, 144133613856L, 147593548568L, 171012311024L, 6718686168127L, 6773910053234L} );
		estNumTransList.put( "SPROT", new long[] {578485, 795009, 858607, 1143989, 1374547, 11479583, 14321219, 24268467, 35074788} );
		estNumTransList.put( "USPS", new long[] {58192, 92999, 147504, 232938, 369432, 584588, 929330, 1469937, 2328905, 3692397, 5855101} );
		
		try {
			bw = new BufferedWriter( new FileWriter( String.format( "tmp/regression_test_%.2f_%d.txt", sampleRatio, num_ratio ), false ) );
		} catch ( IOException e ) { e.printStackTrace(); }
	}
	final int indexK = 1;
	final int qSize = 2;
	Validator checker = new TopDownOneSide();
//	String dataset = "AOL";
	
	/*
	 * estNumTrans
	 * AOL: [171217086, 212220745, 237825124, 517197097, 704104642, 140978317787, 144133613856, 147593548568, 171012311024, 6718686168127, 6773910053234]
	 * SPROT: [578485, 795009, 858607, 1143989, 1374547, 11479583, 14321219, 24268467, 35074788]
	 * USPS: [58192, 92999, 147504, 232938, 369432, 584588, 929330, 1469937, 2328905, 3692397, 5855101]
	 */

	@Test
	public void test() throws IOException {
		for ( String dataset : new String[] {"AOL", "SPROT", "USPS"} ) {
			for ( long size : sizeList.get( dataset  ) ) {
				testSelectivityEstimation( dataset, size );
			}
		}
	}


	public void testSelectivityEstimation(String dataset, long targetSize ) throws IOException {
		double[] ratio_list = new double[num_ratio];
		for ( int i=0; i<num_ratio; ++i ) ratio_list[i] = sampleRatio*(i+1)/num_ratio;
//		double sampleRatio = 0.01;
//		double[] ratio_list = {0.002, 0.004, 0.006, 0.008, 0.01};

		// get a query
		Query query = TestUtils.getTestQuery( dataset, targetSize );
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
		double[][] output = new double[ratio_list.length+1][];
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

			output[i] = new double[7];
			output[i][0] = estimate.sampleSearchedSize;
			output[i][1] = estimate.sampleSearchedNumEstTrans;
			output[i][2] = estimate.mh_term3[estimate.sampleSearchedSize-1];
			output[i][3] = estimate.min_term3[estimate.sampleSearchedSize-1];
			output[i][4] = estimate.sampleSearchedSize * estimate.sampleSearchedSize;
			output[i][5] = estimate.sampleSearchedNumEstTrans * estimate.sampleSearchedNumEstTrans;
			output[i][6] = estimate.sampleSearchedSize * estimate.sampleSearchedNumEstTrans;
		}
		
		// last output (when ratio=1.00)
		int sidx = ArrayUtils.indexOf( sizeList.get( dataset ), targetSize );
		output[ratio_list.length] = new double[7];
		output[ratio_list.length][0] = targetSize;
		output[ratio_list.length][1] = estNumTransList.get( dataset )[sidx];
		output[ratio_list.length][2] = term3List.get( dataset+",mh" )[sidx];
		output[ratio_list.length][3] = term3List.get( dataset+",min" )[sidx];
		output[ratio_list.length][4] = output[ratio_list.length][0] * output[ratio_list.length][0];
		output[ratio_list.length][5] = output[ratio_list.length][1] * output[ratio_list.length][1];
		output[ratio_list.length][6] = output[ratio_list.length][0] * output[ratio_list.length][1];
		
		// print outputs
		System.out.println( "output values:" );
		for ( int i=0; i<output.length; ++i ) {
			if ( i < ratio_list.length ) System.out.print( ratio_list[i]+"\t" );
			else System.out.print( "1.00\t" );
			for ( int j=0; j<output[i].length; ++j ) {
				System.out.print( output[i][j]+"\t" );
			}
			System.out.println(  );
		}
		
		
		// regression test
		int[][] regModelList = {
				{0},
				{0,4,},
				{1,},
				{1,5,},
				{0,1,6},
//				{0,1,4,5,6,},
		};

		// output results
		String result_prefix = dataset+"\t"+targetSize+"\t"+sampleRatio+"\t"+num_ratio+"\t";
		
		for ( int iy=2; iy<=3; ++iy ) {
			for ( int ireg=0; ireg<regModelList.length; ++ireg ) {
				int[] idxList = regModelList[ireg];
				System.out.println( "ireg: "+ireg+"\tJoin"+(iy==2?"MH":"Min") );
				String result_prefix2 = result_prefix + "Join" + (iy==2?"MH":"Min") + "\t" + ireg + "\t";
				double[][] x = new double[ratio_list.length][];
				double[] y = new double[ratio_list.length];
				for ( int i=0; i<ratio_list.length; ++i ) {
					x[i] = new double[idxList.length];
					for ( int j=0; j<idxList.length; ++j ) x[i][j] = output[i][idxList[j]];
					y[i] = output[i][iy];
				}
				
				SelectivityEstimator selest = new SelectivityEstimator( x, y );
				System.out.println( "selest parameter: "+Arrays.toString( selest.getParameters() ) );
				for ( int i=0; i<output.length; ++i ) {
					if ( i < ratio_list.length ) {
						System.out.print( ratio_list[i]+"\t" );
						System.out.print( selest.predict( x[i] ) + "\t" );
						System.out.print( y[i] + "\n" );
						bw.write( result_prefix2 + String.format("%.5f", ratio_list[i])+"\t"+selest.predict( x[i] ) + "\t" + y[i] + "\n" );
					}
					else {
						double[] x0 = new double[idxList.length];
						for ( int j=0; j<idxList.length; ++j ) x0[j] = output[output.length-1][idxList[j]];
						System.out.print( "1.000\t" );
						System.out.print( selest.predict( x0 ) + "\t" );
						System.out.print( output[output.length-1][iy] + "\n" );
						bw.write( result_prefix2 + "1.000\t"+selest.predict( x0 ) + "\t" + output[output.length-1][iy]+"\n" );
					}
				}
			} // end for ireg
		} // end for iy
		bw.flush();
	}

	public long[] getEstNumTrans( String dataset ) throws IOException {
		long[] sizeList = RegressionTest.sizeList.get( dataset );
		long[] estNumTransList = new long[sizeList.length];
		for ( int i=0; i<sizeList.length; ++i ) {
			long size = sizeList[i];
			Query query = TestUtils.getTestQuery( dataset, size );
			long estNumTrans = 0;
			for ( Record rec : query.searchedSet.recordList ) estNumTrans += rec.getEstNumTransformed();
			estNumTransList[i] = estNumTrans;
		}
		return estNumTransList;
	}
}
