package snu.kdd.synonym.synonymRev;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.Util;

public class DeltaEstimateSamplingTest {

	static final int N_REPEAT = 10;
	static PrintWriter pw = null;
	static final String[] datasetArray = {"AOL", "SPROT", "USPS"};
	static final int[] sizeArray = {10000, 15848, 25118, 39810, 63095, 100000};
	static final double[] sampleRatioArray = {0.01, 0.1, 0.5};
	static final String[] samplingArray = {"uniform", "strat"};

	@Test
	public void test() throws IOException {
		pw = new PrintWriter(new BufferedWriter(new FileWriter("tmp/DeltaEstimateSamplingTest.txt")));
		long seed = System.currentTimeMillis();
		Random rn = new Random( seed );
		
		for ( String dataset : datasetArray ) {
			for ( int size : sizeArray ) {
				Query query = Util.getQueryWithPreprocessing(dataset, size);
				double avgTrans0 = getAvgEstTrans(query.searchedSet.recordList);
				double avgNAR0 = getAvgNumAppRules(query.searchedSet.recordList);
				
				for ( double sampleRatio : sampleRatioArray ) {
					for ( int samplingIdx=0; samplingIdx<2; ++samplingIdx ) {
						CoeffStat statNET = new CoeffStat(N_REPEAT);
						CoeffStat statNAR = new CoeffStat(N_REPEAT);
						for ( int i=0; i<N_REPEAT; ++i ) {
							ObjectArrayList<Record> sampleSearchedList = null;
							if ( samplingIdx == 0 ) sampleSearchedList = sampleRecordsNaive( query.searchedSet.recordList, sampleRatio, rn );
							else if ( samplingIdx == 1 ) sampleSearchedList = sampleRecordsStratified( query.searchedSet.recordList, sampleRatio, rn );
							double avgTrans1 = getAvgEstTrans(sampleSearchedList);
							double avgNAR1 = getAvgNumAppRules(sampleSearchedList);
							statNET.add(avgTrans1);
							statNAR.add(avgNAR1);

						}

						String line = String.format(
								"%s\t%d\t%.2f\t%s\t:\t%.6f\t%.6f\t%.6e\t|\t%.6f\t%.6f\t%.6e",
								dataset, size, sampleRatio, samplingArray[samplingIdx], avgTrans0, statNET.mean, statNET.var, avgNAR0, statNAR.mean, statNAR.var);
						pw.println(line);
						pw.flush();
					}
				}
			}
		}
		
		pw.close();
	}

	protected ObjectArrayList<Record> sampleRecordsNaive( List<Record> recordList, double sampleRatio, Random rn ) {
		ObjectArrayList<Record> sampledList = new ObjectArrayList<>();
		for( Record r : recordList ) {
			if ( r.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) continue;
			if( rn.nextDouble() < sampleRatio ) {
				sampledList.add( r );
			}
		}
		return sampledList;
	}
	
	protected ObjectArrayList<Record> sampleRecordsStratified( List<Record> recordList, double sampleRatio, Random rn ) {
		Comparator<Record> comp = new RecordComparator();
		
		ObjectArrayList<Record> sampledList = new ObjectArrayList<Record>();
		List<Record> searchedList = new ArrayList<Record>( recordList );
		Collections.sort( searchedList, comp );
		
		int n_strat = getStratID( searchedList.get(searchedList.size()-1) ) + 1;
		int[] bound = new int[n_strat+1]; // right exclusive
		for ( int i=0; i<searchedList.size(); ++i ) {
			int strat_id = getStratID(searchedList.get(i));
			bound[strat_id+1] = i+1;
		}
		bound[bound.length-1] = searchedList.size();
		for ( int j=0; j<n_strat; ++j ) {
			if ( bound[j+1]	== 0 ) bound[j+1] = bound[j];
		}

//		for ( int j=0; j<n_strat; ++j) {
//			System.out.println("strat"+j+"\t"+(bound[j+1]-bound[j])+"\t"+bound[j]+"\t"+bound[j+1]);
//		}
		
		for ( int j=0; j<n_strat; ++j ) {
			if (bound[j+1] - bound[j] == 0) continue;
			// NOTE: sample at least one item from each strata.
			int strat_size = Math.max(1, (int)((bound[j+1]-bound[j])*sampleRatio));
			double p = 1.0*strat_size/(bound[j+1]-bound[j]);
//			double p = ((bound[j+1]-bound[j])*sampleRatio);
			
			for ( int i=bound[j]; i<bound[j+1]; ++i ) {
				if ( rn.nextDouble() < p ) {
					sampledList.add( searchedList.get(i) );
				}
			}
//			System.out.println("strat_size: "+strat_size+", p: "+p);
		}
//		System.out.println("SAMPLE SIZE:"+sampledList.size());
		return sampledList;
	}
	
	protected int getStratID( Record rec ) {
		return (int)Math.floor( Math.log10( rec.getEstNumTransformed() ) );
	}

	public double getAvgEstTrans( final List<Record> recordList ) {
		double avgTrans = 0;
		for ( Record recS : recordList ) {
			avgTrans += recS.getEstNumTransformed();
		}
		avgTrans /= recordList.size();
		return avgTrans;
	}
	
	public double getAvgNumAppRules( final List<Record> recordList ) {
		double avgNAR = 0;
		for ( Record recS : recordList ) {
			avgNAR += recS.getNumApplicableRules();
		}
		avgNAR /= recordList.size();
		return avgNAR;
	}

	
	class RecordComparator implements Comparator<Record> {
		@Override
		public int compare( Record o1, Record o2 ) {
			long est1 = o1.getEstNumTransformed();
			long est2 = o2.getEstNumTransformed();
			return Long.compare( est1, est2 );
		}
	};

	class CoeffStat {
		double mean;
		double var;
		double[] values;
		int i = 0;
		
		public CoeffStat( int size ) {
			values = new double[size];
		}
		
//		public CoeffStat( double[] arr ) {
//			mean = Arrays.stream(arr).average().getAsDouble();
//			var = Arrays.stream(arr).map(x -> sqerr(x)).average().getAsDouble();
//		}
		
		public void add( double value ) {
			if ( i < values.length ) values[i++] = value;
			if ( i == values.length ) {
				mean = mean();
				var = var();
			}
		}
		
		private double mean() {
			return Arrays.stream(values).average().getAsDouble();
		}
		
		private double var() {
			return Arrays.stream(values).map(x -> sqerr(x)).average().getAsDouble();
		}

		private double sqerr(double v) {
			return (v-mean)*(v-mean);
		}
	}
}
