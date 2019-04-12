package snu.kdd.synonym.synonymRev;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.Test;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import jdk.nashorn.internal.ir.annotations.Ignore;
import snu.kdd.synonym.synonymRev.algorithm.delta.DeltaValidatorDPTopDown;
import snu.kdd.synonym.synonymRev.algorithm.delta.JoinDeltaNaiveIndex;
import snu.kdd.synonym.synonymRev.algorithm.delta.JoinDeltaVarBKIndex;
import snu.kdd.synonym.synonymRev.data.ACAutomataR;
import snu.kdd.synonym.synonymRev.data.Dataset;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.ResultSet;
import snu.kdd.synonym.synonymRev.tools.Stat;
import snu.kdd.synonym.synonymRev.tools.StatContainer;

//@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DeltaHybridEstimationTest {
	
	static final int N_REPEAT = 10;
	static int size = 10000;
	static final int indexK = 1;
	static final int qSize = 2;
	static final String dist = "lcs";
	static PrintWriter pw = null;
	static final String[] datasetArray = {"AOL", "SPROT", "USPS"};
	static final int[] sizeArray = {10000, 15848, 25118, 39810, 63095, 100000};
	static final double[] sampleRatioArray = {0.01, 0.1, 0.5};
	static final int[] deltaMaxArray = {0, 1, 2};
	
	
	@Test
	public void outputStatNV() throws IOException {
		pw = new PrintWriter(new BufferedWriter(new FileWriter("tmp/DeltaHybridEstimationTest_NV.txt")));
		String[] headerArray = {
				"alg", "dataset", "delta", "sampleRatio",
				"t_idx", "t_join",
				"n_strS", "n_strT", "sum_strLenT", "sum_strLenT^d", "sum_appRule"};
		for ( String header : headerArray ) pw.print(header+'\t');
		pw.println();;

		for (String dataset : datasetArray ) {
			Query query = getQuery(dataset, size);
			for ( int deltaMax : deltaMaxArray ) {
				for ( double sampleRatio : sampleRatioArray ) {
					for ( int i=0; i<N_REPEAT; ++i ) {
						String[] configArray = {"NV", dataset, Integer.toString(deltaMax), Double.toString(sampleRatio)};
						String[] coeffStatArray = getStatJoinNV(query, deltaMax, sampleRatio);
						for ( String val : configArray ) pw.print(val+'\t');
						for ( String val : coeffStatArray ) pw.print(val+'\t');
						pw.println();
						pw.flush();
					}
				}
			}
		}
	}

	@Ignore
	public void outputStatVB() throws IOException {
		pw = new PrintWriter(new BufferedWriter(new FileWriter("tmp/DeltaHybridEstimationTest_VB.txt")));
		String[] headerArray = {
				"alg", "dataset", "delta", "sampleRatio",
				"t_idx", "t_gen", "t_filter", "t_verify", 
				"n_strS", "n_strT", "sum_strLenT", "sum_strLenT^d", "sum_appRule", "sum_appRule^q", "n_TPQ", "n_verify"};
		for ( String header : headerArray ) pw.print(header+'\t');
		pw.println();

		for (String dataset : datasetArray ) {
			Query query = getQuery(dataset, size);
			for ( int deltaMax : deltaMaxArray ) {
				for ( double sampleRatio : sampleRatioArray ) {
					for ( int i=0; i<N_REPEAT; ++i ) {
						String[] configArray = {"VB", dataset, Integer.toString(deltaMax), Double.toString(sampleRatio)};
						String[] coeffStatArray = getStatJoinVB(query, deltaMax, sampleRatio);
						for ( String val : configArray ) pw.print(val+'\t');
						for ( String val : coeffStatArray ) pw.print(val+'\t');
						pw.println();
						pw.flush();
					}
				}
			}
		}
		pw.close();
	}

	String[] getStatJoinNV( Query query, int deltaMax, double sampleRatio ) {
		Query sampleQuery = getSampleQuery(query, sampleRatio);
		long ts = System.nanoTime();
		JoinDeltaNaiveIndex idx = new JoinDeltaNaiveIndex(deltaMax, dist, sampleQuery);
		double t_index = (System.nanoTime()-ts)/1e6;
		StatContainer stat = new StatContainer();
		DeltaValidatorDPTopDown checker = new DeltaValidatorDPTopDown(deltaMax, dist);
		ts = System.nanoTime();
		ResultSet rslt = idx.join(query, stat, checker, false);
		double t_join = (System.nanoTime()-ts)/1e6;
		checker.addStat(stat);
		
		String[] statArray = new String[7];
		statArray[0] = Double.toString(t_index);
		statArray[1] = Double.toString(t_join);
		statArray[2] = Long.toString(getNumStrS(sampleQuery));
		statArray[3] = Long.toString(getNumStrT(sampleQuery));
		statArray[4] = Long.toString(getSumStrLenT(sampleQuery));
		statArray[5] = Long.toString(getSumStrLenTDelta(sampleQuery, deltaMax));
		statArray[6] = Long.toString(getSumNumAppRule(sampleQuery));
		return statArray;
	}
	
	String[] getStatJoinVB( Query query, int deltaMax, double sampleRatio ) {
		Query sampleQuery = getSampleQuery(query, sampleRatio);
		long ts = System.nanoTime();
		JoinDeltaVarBKIndex idx = JoinDeltaVarBKIndex.getInstance(sampleQuery, indexK, qSize, deltaMax, dist, 1);
		double t_index = (System.nanoTime()-ts)/1e6;
		DeltaValidatorDPTopDown checker = new DeltaValidatorDPTopDown(deltaMax, dist);
		StatContainer stat = new StatContainer();
		ResultSet rslt = idx.join( query, stat, checker, false );
		checker.addStat(stat);
		double t_gen = stat.getDouble(Stat.CAND_PQGRAM_TIME);
		double t_filter = stat.getDouble(Stat.FILTER_TIME);
		double t_verify = stat.getDouble(Stat.VERIFY_TIME);
		
		String[] statArray = new String[12];
		statArray[0] = Double.toString(t_index);
		statArray[1] = Double.toString(t_gen);
		statArray[2] = Double.toString(t_filter);
		statArray[3] = Double.toString(t_verify);
		statArray[4] = Long.toString(getNumStrS(sampleQuery));
		statArray[5] = Long.toString(getNumStrT(sampleQuery));
		statArray[6] = Long.toString(getSumStrLenT(sampleQuery));
		statArray[7] = Long.toString(getSumStrLenTDelta(sampleQuery, deltaMax));
		statArray[8] = Long.toString(getSumNumAppRule(sampleQuery));
		statArray[9] = Long.toString(getSumNumAppRuleQ(sampleQuery));
		statArray[10] = Long.toString(getNumTPQ(sampleQuery, idx));
		statArray[11] = Long.toString(getNumVerify(stat));
		return statArray;
	}
	
	long getNumStrS( Query query ) {
		return query.searchedSet.size();
	}
	
	long getNumStrT( Query query ) {
		return query.indexedSet.size();
	}
	
	long getSumStrLenT( Query query ) {
		long term = 0;
		for ( Record recT : query.indexedSet.recordList ) term += recT.size();
		return term;
	}
	
	long getSumStrLenTDelta( Query query, int deltaMax ) {
		long term = 0;
		for ( Record recT : query.indexedSet.recordList ) term += Math.pow(recT.size(), deltaMax);
		return term;
	}

	long getSumNumAppRule( Query query ) {
		long term = 0;
		for ( Record recS : query.searchedSet.recordList ) term += recS.getNumApplicableRules();
		return term;
	}
	
	long getSumNumAppRuleQ( Query query ) {
		long term = 0;
		for ( Record recS : query.searchedSet.recordList ) term += Math.pow(recS.getNumApplicableRules(), qSize);
		return term;
	}
	
	long getNumTPQ( Query query, JoinDeltaVarBKIndex idx ) {
		long term = 0;
		for ( Record recS : query.searchedSet.recordList ) {
			List<List<Set<QGram>>> qgrams = idx.getVarSTPQ(recS, false);
			for ( List<Set<QGram>> qgrams_pos : qgrams ) {
				for ( Set<QGram> qgrams_delta : qgrams_pos ) {
					term += qgrams_delta.size();
				}
			}
		}
		return term;
	}
	
	long getNumVerify( StatContainer stat ) {
		return stat.getLong(Stat.NUM_VERIFY);
	}
	
	



	Query getQuery( String name, int size ) throws IOException {
		Query query = TestUtils.getTestQuery(name, size);
		ACAutomataR automata = new ACAutomataR( query.ruleSet.get() );
		for( final Record record : query.searchedSet.get() ) {
			record.preprocessApplicableRules( automata );
			record.preprocessSuffixApplicableRules();
			record.preprocessTransformLength();
			record.preprocessEstimatedRecords();
		}
		return query;
	}
	
	ObjectArrayList<Record> sampleRecords( List<Record> recordList, double sampleRatio ) {
		ObjectArrayList<Record> sampleList = new ObjectArrayList<>();
		Random rn = new Random();
		for( Record r : recordList ) {
			if ( r.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) continue;
			if( rn.nextDouble() < sampleRatio ) {
				sampleList.add( r );
			}
		}
		return sampleList;
	}

	ObjectArrayList<Record> sampleRecordsStratified( List<Record> recordList, double sampleRatio ) {
		Comparator<Record> comp = new RecordComparator();
		Random rn = new Random();
		
		ObjectArrayList<Record> sampledList = new ObjectArrayList<Record>();
		List<Record> searchedList = new ObjectArrayList<Record>( recordList );
		Collections.sort( searchedList, comp );
		int n_stratum = 20;
		
		for ( int stratum_idx=0; stratum_idx<n_stratum; ++stratum_idx ) {
			int start = searchedList.size()/n_stratum*stratum_idx;
			int end = searchedList.size()/n_stratum*(stratum_idx+1);
			for ( int i=start; i<end; ++i ) {
				if (rn.nextDouble() < sampleRatio) {
					sampledList.add( searchedList.get( i ) );
				}
			}
		}
		return sampledList;
	}
	
	Dataset getSampleDataset( Dataset dataset, double sampleRatio ) {
		ObjectArrayList<Record> sampleList = sampleRecords(dataset.recordList, sampleRatio);
		Comparator<Record> comp = new RecordComparator();
		Collections.sort( sampleList, comp );
		return new Dataset( sampleList );
	}
	
	Query getSampleQuery( Query query, double sampleRatio ) {
		Dataset sampleSearchedDataset = getSampleDataset(query.searchedSet, sampleRatio);
		Dataset sampleIndexedDataset = getSampleDataset(query.searchedSet, sampleRatio);
		return new Query( query.ruleSet, sampleIndexedDataset, sampleSearchedDataset, query.tokenIndex, query.oneSideJoin, query.selfJoin, query.outputPath );
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
		
		public CoeffStat( double[] arr ) {
			mean = Arrays.stream(arr).average().getAsDouble();
			var = Arrays.stream(arr).map(x -> sqerr(x)).average().getAsDouble();
		}
		
		double sqerr(double v) {
			return (v-mean)*(v-mean);
		}
	}
	
	interface CoeffFunction<M, P1, P2, P3, R> {
		R apply(M m, P1 p1, P2 p2, P3 p3);
	}
}
