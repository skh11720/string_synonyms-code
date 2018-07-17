package snu.kdd.synonym.synonymRev.algorithm.delta;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import passjoin.PassJoinIndexForSynonyms;
import snu.kdd.synonym.synonymRev.algorithm.JoinMH;
import snu.kdd.synonym.synonymRev.algorithm.JoinMin;
import snu.kdd.synonym.synonymRev.algorithm.JoinNaive;
import snu.kdd.synonym.synonymRev.algorithm.misc.EstimationTest;
import snu.kdd.synonym.synonymRev.data.Dataset;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.estimation.BinaryCountEntry;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.tools.WYK_HashMap;
import snu.kdd.synonym.synonymRev.validator.Validator;

// Refactoring code for estimate

public class SampleEstimateDelta {

	// coefficients of naive index
	public double coeff_naive_1;
	public double coeff_naive_2;
	public double coeff_naive_3;

	// gamma : JoinMH indexing time per pogram of table T
	public double coeff_mh_1;
	// zeta: JoinMH time for counting TPQ supersets per pqgram of table S
	public double coeff_mh_2;
	// eta : JoinMH join time per record of table T
	public double coeff_mh_3;

	// lambda: JoinMin indexing time per twograms of table T
	public double coeff_min_1;
	// mu: JoinMin counting twogram time per twograms of table S
	public double coeff_min_2;
	// rho: JoinMin join time per candidate of table S
	public double coeff_min_3;

	public double sampleRatio;

	Dataset originalSearched;
	Dataset originalIndexed;
	
	public long naive_term1;
	public long[] naive_term2;
	public double[] naive_term3;
	public long mh_term1;
	public long[] mh_term2;
	public long[] mh_term3;
	public long min_term1;
	public long[] min_term2;
	public long[] min_term3;

	boolean joinMinSelected = false;

	final Query query;
	final Query sampleQuery;
	private final int deltaMax;
	ObjectArrayList<Record> sampleSearchedList = new ObjectArrayList<Record>();
	ObjectArrayList<Record> sampleIndexedList = new ObjectArrayList<Record>();

	public SampleEstimateDelta( final Query query, int deltaMax, double sampleratio, boolean isSelfJoin ) {
		long seed = System.currentTimeMillis();
		Util.printLog( "Random seed: " + seed );
		Random rn = new Random( seed );

		this.query = query;
		this.deltaMax = deltaMax;

		int smallTableSize = Integer.min( query.searchedSet.size(), query.indexedSet.size() );
		this.sampleRatio = sampleratio;

		if( sampleratio > 1 ) {
			// fixed number of samples
			if( sampleratio > smallTableSize ) {
				this.sampleRatio = 1;
			}
			else {
				this.sampleRatio = sampleratio / smallTableSize;
			}
		}

		if( this.sampleRatio * smallTableSize < 1 ) {
			// too low sample ratio
			Util.printLog( "Too low sample ratio" );
			Util.printLog( "Too low sample ratio" );

			this.sampleRatio = 10.0 / smallTableSize;
		}

		for( Record r : query.searchedSet.get() ) {
			if( rn.nextDouble() < this.sampleRatio ) {
				sampleSearchedList.add( r );
			}
		}

		if( isSelfJoin ) {
			for( Record r : sampleSearchedList ) {
				sampleIndexedList.add( r );
			}
		}
		else {
			for( Record s : query.indexedSet.get() ) {
				if( rn.nextDouble() < this.sampleRatio ) {
					sampleIndexedList.add( s );
				}
			}
		}

		Util.printLog( sampleSearchedList.size() + " Searched records are sampled" );
		Util.printLog( sampleIndexedList.size() + " Indexed records are sampled" );

		Comparator<Record> cmp = new Comparator<Record>() {
			@Override
			public int compare( Record o1, Record o2 ) {
				long est1 = o1.getEstNumTransformed();
				long est2 = o2.getEstNumTransformed();
				return Long.compare( est1, est2 );
			}
		};

		Collections.sort( sampleSearchedList, cmp );
		Collections.sort( sampleIndexedList, cmp );

		Dataset sampleIndexed = new Dataset( sampleIndexedList );
		Dataset sampleSearched = new Dataset( sampleSearchedList );
		sampleQuery = new Query( query.ruleSet, sampleIndexed, sampleSearched, query.tokenIndex, query.oneSideJoin,
				query.selfJoin );

		naive_term2 = new long[sampleSearchedList.size()];
		naive_term3 = new double[sampleSearchedList.size()];
		mh_term2 = new long[sampleSearchedList.size()];
		mh_term3 = new long[sampleSearchedList.size()];
		min_term2 = new long[sampleSearchedList.size()];
		min_term3 = new long[sampleSearchedList.size()];
	}

	public void estimateJoinNaiveDelta( StatContainer stat ) {

		// Infer alpha and beta
		PassJoinIndexForSynonyms naiveinst;
		StatContainer tmpStat = new StatContainer();
		Set<IntegerPair> rslt = new ObjectOpenHashSet<IntegerPair>();

		long ts = System.nanoTime();
		naiveinst = new PassJoinIndexForSynonyms( sampleQuery, deltaMax, tmpStat );
		for (int i = 0; i < sampleQuery.searchedSet.size(); i++) {
			Record recS = sampleQuery.searchedSet.getRecord( i );
			if ( recS.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) {
				naive_term2[i] = (i == 0 ? 0 : naive_term2[i-1]);
				naive_term3[i] = (i == 0 ? 0 : naive_term3[i-1]);
			}
			else {
				naiveinst.joinOneRecord( recS, rslt );
				naive_term2[i] = naiveinst.sumLenS;
				naive_term3[i] = naiveinst.verifyCost;
			}
		} // end for id
		long joinTime = System.nanoTime() - ts;
		naive_term1 = naiveinst.sumLenT;

		// compute coefficients
		coeff_naive_1 = naiveinst.indexTime/naiveinst.sumLenT;
		coeff_naive_2 = (joinTime - naiveinst.verifyTime)/naiveinst.sumLenS;
		coeff_naive_3 = naiveinst.verifyTime/sampleIndexedList.size()/naiveinst.verifyCost;
		
//		stat.add( "Stat_Coeff_Naive_1", coeff_naive_1 );
//		stat.add( "Stat_Coeff_Naive_2", coeff_naive_2 );
//		stat.add( "Stat_Coeff_Naive_3", coeff_naive_3 );
		
		System.out.println( "estimateJoinNaiveDelta" );
		System.out.println( "coeff_naive1: "+coeff_naive_1 );
		System.out.println( "coeff_naive_2: "+coeff_naive_2 );
		System.out.println( "coeff_naive_3: "+coeff_naive_3 );
//		System.out.println( Arrays.toString( naive_term2 ) );
//		System.out.println( Arrays.toString( naive_term3 ) );
		System.out.println( "est time: "+getEstimateNaiveDelta( naive_term1, naive_term2[sampleSearchedList.size()-1], naive_term3[sampleSearchedList.size()-1] ) );
		System.out.println( "join time: "+String.format( "%.10e", (double)joinTime ) );
	}

	public void estimateJoinMinDelta( StatContainer stat, Validator checker, int indexK, int qSize ) {
		// Infer lambda, mu and rho
		StatContainer tmpStat = new StatContainer();
		Set<IntegerPair> rslt = new ObjectOpenHashSet<IntegerPair>();

		long ts = System.nanoTime();
		JoinMinDeltaIndex joinmininst = new JoinMinDeltaIndex( indexK, qSize, deltaMax, tmpStat, sampleQuery, -1, false );
		for (int i = 0; i < sampleQuery.searchedSet.size(); i++) {
			Record recS = sampleQuery.searchedSet.getRecord( i );
			if ( recS.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) {
				min_term2[i] = ( i== 0? 0 : min_term2[i-1]);
				min_term3[i] = ( i== 0? 0 : min_term3[i-1]);
			}
			else {
				joinmininst.joinRecordMaxK( indexK, recS, rslt, false, null, checker, query.oneSideJoin );
				min_term2[i] = joinmininst.searchedTotalSigCount;
				min_term3[i] = joinmininst.predictCount;
			}
		}
		long joinTime = System.nanoTime() - ts;

		min_term1 = joinmininst.indexedTotalSigCount;
		coeff_min_1 = joinmininst.indexTime / joinmininst.indexedTotalSigCount;
		coeff_min_2 = ( joinmininst.indexCountTime + joinmininst.candQGramCountTime + joinmininst.filterTime ) / joinmininst.searchedTotalSigCount;
		coeff_min_3 = joinmininst.equivTime / joinmininst.predictCount;

		System.out.println( "estimateJoinMinDelta" );
		System.out.println( "coeff_min_1: "+coeff_min_1 );
		System.out.println( "coeff_min_2: "+coeff_min_2 );
		System.out.println( "coeff_min_3: "+coeff_min_3 );
//		System.out.println( Arrays.toString( min_term2 ) );
//		System.out.println( Arrays.toString( min_term3 ) );
		System.out.println( "est time: "+getEstimateJoinMinDelta( min_term1, min_term2[sampleSearchedList.size()-1], min_term3[sampleSearchedList.size()-1]) );
		System.out.println( "join time: "+String.format( "%.10e", (double)joinTime ) );

//		System.out.println( "est verify time: "+String.format( "%.10e", coeff_min_3*min_term3[sampleSearchedList.size()-1] ) );
//		System.out.println( "verify time: "+String.format( "%.10e", (double)(joinTime-joinmininst.candQGramCountTime) ) );
//
//		System.out.println( "est tpq time: "+String.format( "%.10e", coeff_min_2*min_term2[sampleSearchedList.size()-1] ) );
//		System.out.println( "tpq time: "+String.format( "%.10e", (double)(joinmininst.indexCountTime + joinmininst.candQGramCountTime) ) );
//		System.out.println( "tpq, indexCounttime: "+String.format( "%.10e", (double)(joinmininst.candQGramCountTime) ) );
//		System.out.println( "tpq, candQGramCounttime: "+String.format( "%.10e", (double)(joinmininst.candQGramCountTime) ) );
//		
//		System.out.println( "est index time: "+String.format( "%.10e", coeff_min_1*min_term1 ) );
//		System.out.println( "index time: "+String.format( "%.10e", (double)(joinmininst.indexTime) ) );
	}

	public void estimateJoinMHDelta( StatContainer stat, Validator checker, int indexK, int qSize ) {

		int[] indexPosition = new int[indexK];
		for (int i=0; i<indexK; ++i ) indexPosition[i] = i;
		StatContainer tmpStat = new StatContainer();
		Set<IntegerPair> rslt = new ObjectOpenHashSet<IntegerPair>();

		long ts = System.nanoTime();
		JoinMHDeltaIndex joinmhinst = new JoinMHDeltaIndex( indexK, qSize, deltaMax, sampleIndexedList, sampleQuery, tmpStat, indexPosition, true, true, -1 );
		for (int i = 0; i < sampleQuery.searchedSet.size(); i++) {
			Record recS = sampleQuery.searchedSet.getRecord( i );
			if ( recS.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) {
				mh_term2[i] = (i == 0 ? 0 : mh_term2[i-1]);
				mh_term3[i] = (i == 0 ? 0 : mh_term3[i-1]);
			}
			else {
				joinmhinst.joinOneRecordThres( recS, rslt, checker, -1, sampleQuery.oneSideJoin );
				mh_term2[i] =  joinmhinst.candQGramCount;
				mh_term3[i] =  joinmhinst.predictCount;
			}
		} // for sid in in searchedSet
		long joinTime = System.nanoTime() - ts;

		mh_term1 = joinmhinst.qgramCount;
		coeff_mh_1 = joinmhinst.indexTime / joinmhinst.qgramCount;
		coeff_mh_2 = joinmhinst.candQGramCountTime / joinmhinst.candQGramCount;
		coeff_mh_3 = (double) (joinTime - joinmhinst.candQGramCountTime) / joinmhinst.predictCount;

		System.out.println( "estimateJoinMHDelta" );
		System.out.println( "coeff_mh_1: "+coeff_mh_1 );
		System.out.println( "coeff_mh_2: "+coeff_mh_2 );
		System.out.println( "coeff_mh_3: "+coeff_mh_3 );
//		System.out.println( Arrays.toString( mh_term2 ) );
//		System.out.println( Arrays.toString( mh_term3 ) );
		System.out.println( "est time: "+getEstimateJoinMHDelta( mh_term1, mh_term2[sampleSearchedList.size()-1], mh_term3[sampleSearchedList.size()-1] ) );
		System.out.println( "join time: "+String.format( "%.10e", (double)joinTime ) );
	}

	public void estimateJoinHybridWithSample( StatContainer stat, Validator checker, int indexK, int qSize ) {
		estimateJoinNaiveDelta( stat );
		estimateJoinMHDelta( stat, checker, indexK, qSize );
		estimateJoinMinDelta( stat, checker, indexK, qSize );

		if( DEBUG.PrintEstimationON ) {
			BufferedWriter bw = EstimationTest.getWriter();

			try {
				bw.write( "\n" );
			}
			catch( IOException e ) {
				e.printStackTrace();
			}
		}
	}

	public double getEstimateNaiveDelta( long term1, long term2, double term3 ) {
		return coeff_naive_1 * term1 + coeff_naive_2 * term2 + coeff_naive_3 * term3 * sampleIndexedList.size();
	}

	public double getEstimateJoinMinDelta( long term1, long term2, long term3 ) {
		return coeff_min_1 * term1 + coeff_min_2 * term2 + coeff_min_3 * term3;
	}

	public double getEstimateJoinMHDelta( long term1, long term2, long term3 ) {
		return coeff_mh_1 * term1 + coeff_mh_2 * term2 + coeff_mh_3 * term3;
	}

	public int findThetaJoinHybridAllDelta( int qSize, int indexK, StatContainer stat, long maxIndexedEstNumRecords, long maxSearchedEstNumRecords, boolean oneSideJoin ) {
		// Find the best threshold
		int bestThreshold = 0;
		double bestEstTime = Double.MAX_VALUE;

		// Indicates the minimum indices which have more that 'theta' expanded
		// records
		int indexedIdx = 0;
		int sidx = 0;
		long currentThreshold = Math.min( sampleSearchedList.get( 0 ).getEstNumTransformed(), sampleIndexedList.get( 0 ).getEstNumTransformed() );
		long maxThreshold = Long.min( maxIndexedEstNumRecords, maxSearchedEstNumRecords );
		int tableIndexedSize = sampleIndexedList.size();
		int tableSearchedSize = sampleSearchedList.size();

		boolean stop = false;
		if( maxThreshold == Long.MAX_VALUE ) {
			stop = true;
		}

		while( currentThreshold <= maxThreshold ) {
			if( currentThreshold > 100000 ) {
				Util.printLog( "Current Threshold is more than 100000" );
				break;
			}

			long nextThresholdIndexed = -1;
			long nextThresholdSearched = -1;

			for( ; sidx < tableSearchedSize; sidx++ ) {
				Record rec = sampleSearchedList.get( sidx );

				long est = rec.getEstNumTransformed();
				if( est > currentThreshold ) {
					nextThresholdSearched = est;
					break;
				}
			}

			long nextThreshold;

			if( nextThresholdIndexed == -1 && nextThresholdSearched == -1 ) {
				if( stop ) {
					break;
				}
				nextThreshold = maxThreshold + 1;
			}
			else if( nextThresholdIndexed == -1 ) {
				nextThreshold = nextThresholdSearched;
			}
			else if( nextThresholdSearched == -1 ) {
				nextThreshold = nextThresholdIndexed;
			}
			else {
				nextThreshold = Long.min( nextThresholdSearched, nextThresholdIndexed );
			}

			if( DEBUG.PrintEstimationON ) {
				BufferedWriter bw = EstimationTest.getWriter();

				try {
					bw.write( "Estimation " + currentThreshold + "\n" );
				}
				catch( IOException e ) {
					e.printStackTrace();
				}
			}

			double joinmhEstimation = this.getEstimateJoinMHDelta( mh_term1, mh_term2[tableSearchedSize-1] - (sidx==0?0:mh_term2[sidx-1]), 
					mh_term3[tableSearchedSize-1] - (sidx==0?0:mh_term3[sidx-1]) );
			// searchedTotalSigCount: the sum of the size of TPQ superset of records in sampleSearchedSet
			// indexedTotalSigCount: the number of pos qgrams from records in sampleIndexedSet
			// totalJoinMHInvokes: the sum of the minimum number of records to be verified with t for every t in sampleIndexedSet
			// removedJoinMHComparison: 

			double joinminEstimation = this.getEstimateJoinMinDelta( min_term1, min_term2[tableIndexedSize-1] - (sidx==0?0:min_term2[sidx-1]), 
					min_term3[tableIndexedSize-1] - (sidx==0?0:min_term3[sidx-1]) );
			// searchedTotalSigCount: the sum of the size of TPQ superset of records in sampleSearchedSet
			// indexedTotalSigCount: the number of pos qgrams from records in sampleIndexedSet

			boolean tempJoinMinSelected = joinminEstimation < joinmhEstimation;

			double naiveEstimation = this.getEstimateNaiveDelta( naive_term1, (sidx==0?0:naive_term2[sidx-1]), (sidx==0?0:naive_term3[sidx-1]) );
			// currExpLengthSize: sum of lengths of records in sampleIndexedSet
			// currExpSize: sum of estimated number of transformations of records in sampleSearchedSet

			if( DEBUG.PrintEstimationON ) {
				BufferedWriter bw = EstimationTest.getWriter();

				try {
					bw.write( "\n" );
				}
				catch( IOException e ) {
					e.printStackTrace();
				}
			}

			Util.printLog( String.format( "T: %d nT: %d NT: %.10e JT(JoinMH): %.10e TT: %.10e", currentThreshold, nextThreshold,
					naiveEstimation, joinmhEstimation, naiveEstimation + joinmhEstimation ) );
			Util.printLog( String.format( "T: %d nT: %d NT: %.10e JT(JoinMin): %.10e TT: %.10e", currentThreshold, nextThreshold,
					naiveEstimation, joinminEstimation, naiveEstimation + joinminEstimation ) );
			Util.printLog( "JoinMin Selected " + tempJoinMinSelected );

			double tempBestTime = naiveEstimation;

			if( tempJoinMinSelected ) tempBestTime += joinminEstimation;
			else tempBestTime += joinmhEstimation;

			if( bestEstTime > tempBestTime ) {
				bestEstTime = tempBestTime;
				joinMinSelected = tempJoinMinSelected;

				if( currentThreshold < Integer.MAX_VALUE ) {
					bestThreshold = (int) currentThreshold;
				}
				else {
					currentThreshold = Integer.MAX_VALUE;
				}

				if( DEBUG.SampleStatON ) {
					Util.printLog( "New Best " + bestThreshold + " with " + joinMinSelected );
				}
			}

			currentThreshold = nextThreshold;
		} // end while searching best threshold

		stat.add( "Auto_Best_Threshold", bestThreshold );
		stat.add( "Auto_Best_Estimated_Time", bestEstTime );
		stat.add( "Auto_JoinMin_Selected", "" + joinMinSelected );
		return bestThreshold;
	}

	public boolean getJoinMinSelected() {
		return joinMinSelected;
	}
}
