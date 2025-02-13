package snu.kdd.synonym.synonymRev.estimation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.data.Dataset;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.index.JoinMHIndex;
import snu.kdd.synonym.synonymRev.index.JoinMinIndex;
import snu.kdd.synonym.synonymRev.index.NaiveIndex;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.ResultSet;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.validator.Validator;

// Refactoring code for estimate

public class SampleEstimate {

	// coefficients of naive index
	public double estTime_naive;
	public double coeff_naive_1;
	public double coeff_naive_2;

	public double estTime_mh;
	// gamma : JoinMH indexing time per pogram of table T
	public double coeff_mh_1;
	// zeta: JoinMH time for counting TPQ supersets per pqgram of table S
	public double coeff_mh_2;
	// eta : JoinMH join time per record of table T
	public double coeff_mh_3;

	public double estTime_min;
	// lambda: JoinMin indexing time per twograms of table T
	public double coeff_min_1;
	// mu: JoinMin counting twogram time per twograms of table S
	public double coeff_min_2;
	// rho: JoinMin join time per candidate of table S
	public double coeff_min_3;

	public double sampleRatio;
	public double bestEstTime = Double.MAX_VALUE;

	protected Dataset originalSearched;
	protected Dataset originalIndexed;
	
	public long naive_term1;
	public long[] naive_term2;
	public long mh_term1;
	public long[] mh_term2;
	public long[] mh_term3;
	public long min_term1;
	public long[] min_term2;
	public long[] min_term3;

	protected boolean joinMinSelected = false;

	protected Query query;
	protected Query sampleQuery;
	protected boolean stratified = false;
	public int sampleSearchedSize;
	public long sampleSearchedNumEstTrans;
	protected ObjectArrayList<Record> sampleSearchedList = new ObjectArrayList<Record>();
	protected ObjectArrayList<Record> sampleIndexedList = new ObjectArrayList<Record>();
	protected BufferedWriter bw_log = null;
	
	protected SampleEstimate(final Query query, double sampleRatio ) {
		this.query = query;
		this.sampleRatio = sampleRatio;

		try { 
			String[] tokenList = query.getSearchedPath().split( "\\"+(File.separator) );
			String dataAndSize = tokenList[tokenList.length-1].split( "\\.", 2)[0];
			String nameTmp = String.format( "SampleEst_%s_%.2f", dataAndSize, sampleRatio );
			bw_log = new BufferedWriter( new FileWriter( "tmp/"+nameTmp+".txt" ) );
		}
		catch ( IOException e ) { e.printStackTrace(); }

		int smallTableSize = Integer.min( query.searchedSet.size(), query.indexedSet.size() );
		if( sampleRatio > 1 ) {
			// fixed number of samples
			if( sampleRatio > smallTableSize ) {
				this.sampleRatio = 1;
			}
			else {
				this.sampleRatio = sampleRatio / smallTableSize;
			}
		}

		if( this.sampleRatio * smallTableSize < 1 ) {
			// too low sample ratio
			Util.printLog( "Too low sample ratio" );
			Util.printLog( "Too low sample ratio" );

			this.sampleRatio = 10.0 / smallTableSize;
		}
	}

	public SampleEstimate( final Query query, double sampleRatio, boolean isSelfJoin ) {
		this( query, sampleRatio );
		long seed = System.currentTimeMillis();
		Util.printLog( "Random seed: " + seed );
		Random rn = new Random( seed );
		
		sampleSearchedList = sampleRecords( query.searchedSet.recordList, rn );
		if( isSelfJoin ) {
			for( Record r : sampleSearchedList ) {
				sampleIndexedList.add( r );
			}
		}
		else sampleIndexedList = sampleRecords( query.indexedSet.recordList, rn );

		Util.printLog( sampleSearchedList.size() + " Searched records are sampled" );
		Util.printLog( sampleIndexedList.size() + " Indexed records are sampled" );
		
		initialize();
	}
	
	public SampleEstimate( final Query query, double sampleRatio, ObjectArrayList<Record> sampledSearchedList, ObjectArrayList<Record> sampleIndexedList ) {
		this( query, sampleRatio );
		this.sampleSearchedList = sampledSearchedList;
		this.sampleIndexedList = sampleIndexedList;
		initialize();
	}
	
	@Override
	protected void finalize() throws Throwable {
		bw_log.flush();
		bw_log.close();
	}

	public void initialize() {
		sampleSearchedSize = sampleSearchedList.size();

		Comparator<Record> comp = new RecordComparator();
		Collections.sort( sampleSearchedList, comp );
//		Collections.sort( sampleIndexedList, comp );

		Dataset sampleIndexed = new Dataset( sampleIndexedList );
		Dataset sampleSearched = new Dataset( sampleSearchedList );
		sampleQuery = new Query( query.ruleSet, sampleIndexed, sampleSearched, query.tokenIndex, query.oneSideJoin,
				query.selfJoin, query.outputPath );
		
		naive_term2 = new long[sampleSearchedList.size()];
		mh_term2 = new long[sampleSearchedList.size()];
		mh_term3 = new long[sampleSearchedList.size()];
		min_term2 = new long[sampleSearchedList.size()];
		min_term3 = new long[sampleSearchedList.size()];
	}
	
	public Object2DoubleMap<String> estimateJoinNaive( StatContainer stat ) {

		// Infer alpha and beta
		NaiveIndex naiveinst;
		StatContainer tmpStat = new StatContainer();
		ResultSet rslt = new ResultSet(query.selfJoin);

		long ts = System.nanoTime();
		naiveinst = new NaiveIndex( sampleQuery, tmpStat, false );
		for (int i = 0; i < sampleQuery.searchedSet.size(); i++) {
			Record recS = sampleQuery.searchedSet.getRecord( i );
			if ( recS.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) {
				naive_term2[i] = (i == 0 ? 0 : naive_term2[i-1]);
			}
			else {
				naiveinst.joinOneRecord( recS, rslt, null );
				naive_term2[i] = naiveinst.sumTransLenS;
//				naive_term3[i] = naiveinst.verifyCost;
//				naive_term3[i] = naiveinst.expCount*sampleIndexedList.size();
			}
		} // end for id
		long joinTime = System.nanoTime() - ts;
		long indexTime = naiveinst.indexTime;
		long searchTime = naiveinst.searchTime;
		naive_term1 = naiveinst.sumLenT;

		// compute coefficients
		coeff_naive_1 = indexTime/(naiveinst.sumLenT+1);
		coeff_naive_2 = joinTime/(naiveinst.sumTransLenS+1);
		
		System.out.println( "estimateJoinNaive" );
		System.out.println( "coeff_naive1: "+coeff_naive_1 );
		System.out.println( "coeff_naive_2: "+coeff_naive_2 );
		System.out.println( "Naive_Term_1: "+naive_term1 );
		System.out.println( "Naive_Term_2: "+naive_term2[sampleSearchedList.size()-1] );

		estTime_naive = getEstimateNaive( naive_term1, naive_term2[sampleSearchedList.size()-1] );
		System.out.println( "Est_Time: "+ estTime_naive );
		System.out.println( "Join_Time: "+String.format( "%.10e", (double)joinTime ) );
		System.out.println( "indexTime: "+indexTime );
		System.out.println( "searchTime: "+searchTime );
		
		Object2DoubleOpenHashMap<String> output = new Object2DoubleOpenHashMap<>();
		output.put( "Naive_Coeff_1", coeff_naive_1 );
		output.put( "Naive_Coeff_2", coeff_naive_2 );
		output.put( "Naive_Term_1", naive_term1 );
		output.put( "Naive_Term_2", naive_term2[sampleSearchedList.size()-1] );
		output.put( "Naive_Est_Time", estTime_naive );
		output.put( "Naive_Join_Time", (double)joinTime );
		return output;
	}
	
	protected long sampleJoinMH( JoinMHIndex joinmhinst, Validator checker ) {
		long ts = System.nanoTime();
		ResultSet rslt = new ResultSet(query.selfJoin);
		for (int i = 0; i < sampleQuery.searchedSet.size(); i++) {
			Record recS = sampleQuery.searchedSet.getRecord( i );
			if ( recS.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) {
				mh_term2[i] = (i == 0 ? 0 : mh_term2[i-1]);
				mh_term3[i] = (i == 0 ? 0 : mh_term3[i-1]);
			}
			else {
				joinmhinst.joinOneRecord( recS, rslt, checker );
				mh_term2[i] =  joinmhinst.candQGramCountSum;
				mh_term3[i] =  joinmhinst.equivComparisons;
			}
		} // for sid in in searchedSet
		long joinTime = System.nanoTime() - ts;
		return joinTime;
	}

	public Object2DoubleMap<String> estimateJoinMH( StatContainer stat, Validator checker, int indexK, int qSize ) {
		int[] indexPosition = new int[indexK];
		for (int i=0; i<indexK; ++i ) indexPosition[i] = i;
		StatContainer tmpStat = new StatContainer();

		JoinMHIndex joinmhinst = new JoinMHIndex( indexK, qSize, sampleIndexedList, sampleQuery, tmpStat, indexPosition, true, true, -1 );
		long joinTime = sampleJoinMH( joinmhinst, checker );

		mh_term1 = joinmhinst.qgramCount;
		coeff_mh_1 = joinmhinst.indexTime / (joinmhinst.qgramCount+1);
		coeff_mh_2 = (joinmhinst.candQGramCountTime + joinmhinst.filterTime) / (joinmhinst.candQGramCountSum+1);
//		coeff_mh_3 = (double) (joinTime - joinmhinst.candQGramCountTime - joinmhinst.filterTime) / (joinmhinst.predictCount+1);
		coeff_mh_3 = (double) (joinmhinst.verifyTime) / (joinmhinst.equivComparisons+1);

		System.out.println( "estimateJoinMH" );
		System.out.println( "coeff_mh_1: "+coeff_mh_1 );
		System.out.println( "coeff_mh_2: "+coeff_mh_2 );
		System.out.println( "coeff_mh_3: "+coeff_mh_3 );
		System.out.println( "MH_Term_1: "+ mh_term1 );
		System.out.println( "MH_Term_2: "+ mh_term2[sampleSearchedList.size()-1] );
		System.out.println( "MH_Term_3: "+ mh_term3[sampleSearchedList.size()-1] );
//		System.out.println( Arrays.toString( mh_term2 ) );
//		System.out.println( Arrays.toString( mh_term3 ) );
		estTime_mh = getEstimateJoinMH( mh_term1, mh_term2[sampleSearchedList.size()-1], mh_term3[sampleSearchedList.size()-1] );
		System.out.println( "Est_Time: "+ estTime_mh );
		System.out.println( "Join_Time: "+String.format( "%.10e", (double)joinTime ) );

		Object2DoubleOpenHashMap<String> output = new Object2DoubleOpenHashMap<>();
		output.put( "MH_Coeff_1", coeff_mh_1 );
		output.put( "MH_Coeff_2", coeff_mh_2 );
		output.put( "MH_Coeff_3", coeff_mh_3 );
		output.put( "MH_Term_1", mh_term1 );
		output.put( "MH_Term_2", mh_term2[sampleSearchedList.size()-1] );
		output.put( "MH_Term_3", mh_term3[sampleSearchedList.size()-1] );
		output.put( "MH_Est_Time", (double)estTime_mh );
		output.put( "MH_Join_Time", (double)joinTime );
		return output;
	}
	
	protected long sampleJoinMin( JoinMinIndex joinmininst, Validator checker, int indexK ) {
		ResultSet rslt = new ResultSet(query.selfJoin);

		long ts = System.nanoTime();
		for (int i = 0; i < sampleQuery.searchedSet.size(); i++) {
			Record recS = sampleQuery.searchedSet.getRecord( i );
			if ( recS.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) {
				min_term2[i] = ( i== 0? 0 : min_term2[i-1]);
				min_term3[i] = ( i== 0? 0 : min_term3[i-1]);
			}
			else {
				joinmininst.joinOneRecord( recS, rslt, checker );
				min_term2[i] = joinmininst.searchedTotalSigCount;
				min_term3[i] = joinmininst.equivComparisons;
			}
		}
		long joinTime = System.nanoTime() - ts;
		return joinTime;
	}

	public Object2DoubleMap<String> estimateJoinMin( StatContainer stat, Validator checker, int indexK, int qSize ) {
		// Infer lambda, mu and rho
		StatContainer tmpStat = new StatContainer();

		JoinMinIndex joinmininst = new JoinMinIndex( indexK, qSize, tmpStat, sampleQuery, -1, false );
		long joinTime = sampleJoinMin( joinmininst, checker, indexK );

		if ( joinmininst.predictCount == 0 ) joinmininst.predictCount = 1; // prevent from dividing by zero
		min_term1 = joinmininst.indexedTotalSigCount;
		coeff_min_1 = joinmininst.indexRecordTime / (joinmininst.indexedTotalSigCount+1);
		coeff_min_2 = ( joinmininst.indexCountTime + joinmininst.candQGramTime + joinmininst.filterTime ) / (joinmininst.searchedTotalSigCount+1);
		coeff_min_3 = joinmininst.verifyTime / (joinmininst.equivComparisons+1);

		System.out.println( "estimateJoinMin" );
		System.out.println( "coeff_min_1: "+coeff_min_1 );
		System.out.println( "coeff_min_2: "+coeff_min_2 );
		System.out.println( "coeff_min_3: "+coeff_min_3 );
		System.out.println( "Min_Term_1: "+ min_term1 );
		System.out.println( "Min_Term_2: "+ min_term2[sampleSearchedList.size()-1] );
		System.out.println( "Min_Term_3: "+ min_term3[sampleSearchedList.size()-1] );
//		System.out.println( Arrays.toString( min_term2 ) );
//		System.out.println( Arrays.toString( min_term3 ) );
		estTime_min = getEstimateJoinMin( min_term1, min_term2[sampleSearchedList.size()-1], min_term3[sampleSearchedList.size()-1]);
		System.out.println( "Est_Time: "+ estTime_min );
		System.out.println( "Join_Time: "+String.format( "%.10e", (double)joinTime ) );

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

		Object2DoubleOpenHashMap<String> output = new Object2DoubleOpenHashMap<>();
		output.put( "Min_Coeff_1", coeff_min_1 );
		output.put( "Min_Coeff_2", coeff_min_2 );
		output.put( "Min_Coeff_3", coeff_min_3 );
		output.put( "Min_Term_1", min_term1 );
		output.put( "Min_Term_2", min_term2[sampleSearchedList.size()-1] );
		output.put( "Min_Term_3", min_term3[sampleSearchedList.size()-1] );
		output.put( "Min_Est_Time", (double)estTime_min );
		output.put( "Min_Join_Time", (double)joinTime );
		return output;
	}

    public void estimateJoinMHNaiveWithSample( StatContainer stat, Validator checker, int indexK, int qSize ) {
        estimateJoinMH( stat, checker, indexK, qSize );
        estimateJoinNaive( stat );
    }

    public void estimateJoinMinNaiveWithSample( StatContainer stat, Validator checker, int indexK, int qSize ) {
        estimateJoinMin( stat, checker, indexK, qSize );
        estimateJoinNaive( stat );
    }

	public void estimateJoinHybridWithSample( StatContainer stat, Validator checker, int indexK, int qSize ) {
		estimateJoinNaive( stat );
		estimateJoinMH( stat, checker, indexK, qSize );
		estimateJoinMin( stat, checker, indexK, qSize );
	}

	/*
	 * scale up terms by (1/sampleRatio)^p
	 */
	public double getEstimateNaive( double term1, double term2 ) {
		return coeff_naive_1 * term1 / sampleRatio 
				+ coeff_naive_2 * term2 / sampleRatio;
	}

	public double getEstimateJoinMH( double term1, double term2, double term3 ) {
		return coeff_mh_1 * term1 / sampleRatio 
				+ coeff_mh_2 * term2 / sampleRatio
				+ coeff_mh_3 * term3 / sampleRatio / sampleRatio;
	}

	public double getEstimateJoinMin( double term1, double term2, double term3 ) {
		return coeff_min_1 * term1 / sampleRatio 
				+ coeff_min_2 * term2 / sampleRatio
				+ coeff_min_3 * term3 / sampleRatio / sampleRatio;
	}
	
	public int findThetaJoinHybridAll( int qSize, int indexK, StatContainer stat, long maxIndexedEstNumRecords, long maxSearchedEstNumRecords, boolean oneSideJoin ) {
		// Find the best threshold
		int bestThreshold = 0;
//		double bestEstTime = Double.MAX_VALUE;

		// Indicates the minimum indices which have more that 'theta' expanded
		// records
//		int indexedIdx = 0;
		int sidx = 0;
		sampleSearchedNumEstTrans = 0;
//		long currentThreshold = Math.min( sampleSearchedList.get( 0 ).getEstNumTransformed(), sampleIndexedList.get( 0 ).getEstNumTransformed() );
		long currentThreshold = 0;
		long maxThreshold = Long.min( maxIndexedEstNumRecords, maxSearchedEstNumRecords );
//		int tableIndexedSize = sampleIndexedList.size();
		int tableSearchedSize = sampleSearchedList.size();

		boolean stop = false;
		if( maxThreshold == Long.MAX_VALUE ) {
			stop = true;
		}

		while( sidx < tableSearchedSize ) {
			if( currentThreshold > 100000 ) {
//				Util.printLog( "Current Threshold is more than 100000" );
				currentThreshold = Integer.MAX_VALUE;
				stop = true;
			}

			long nextThresholdIndexed = -1;
			long nextThresholdSearched = -1;

			for( ; sidx < tableSearchedSize; sidx++ ) {
				Record rec = sampleSearchedList.get( sidx );

				long est = rec.getEstNumTransformed();
				sampleSearchedNumEstTrans += est;
				if( est > currentThreshold ) {
					nextThresholdSearched = est;
					break;
				}
			}

			long nextThreshold;

			if( nextThresholdIndexed == -1 && nextThresholdSearched == -1 ) {
//				if( stop ) {
//					break;
//				}
				nextThreshold = Integer.MAX_VALUE;
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

			long curr_naive_term2 = (sidx==0?0:naive_term2[sidx-1]);
			long curr_mh_term2 = (mh_term2[tableSearchedSize-1] - (sidx==0?0:mh_term2[sidx-1]));
			long curr_mh_term3 = (mh_term3[tableSearchedSize-1] - (sidx==0?0:mh_term3[sidx-1]));
			long curr_min_term2 = (min_term2[tableSearchedSize-1] - (sidx==0?0:min_term2[sidx-1]));
			long curr_min_term3 = (min_term3[tableSearchedSize-1] - (sidx==0?0:min_term3[sidx-1]));
			
			double naiveEstimation = this.getEstimateNaive( naive_term1, curr_naive_term2);
			// currExpLengthSize: sum of lengths of records in sampleIndexedSet
			// currExpSize: sum of estimated number of transformations of records in sampleSearchedSet

			double joinmhEstimation = this.getEstimateJoinMH( mh_term1, curr_mh_term2, curr_mh_term3);
			// searchedTotalSigCount: the sum of the size of TPQ superset of records in sampleSearchedSet
			// indexedTotalSigCount: the number of pos qgrams from records in sampleIndexedSet
			// totalJoinMHInvokes: the sum of the minimum number of records to be verified with t for every t in sampleIndexedSet
			// removedJoinMHComparison: 

			double joinminEstimation = this.getEstimateJoinMin( min_term1, curr_min_term2, curr_min_term3);
			// searchedTotalSigCount: the sum of the size of TPQ superset of records in sampleSearchedSet
			// indexedTotalSigCount: the number of pos qgrams from records in sampleIndexedSet

			boolean tempJoinMinSelected = joinminEstimation < joinmhEstimation;

			System.out.print( (sidx)+"\t" );
			System.out.print( sampleSearchedNumEstTrans+"\t" );
			System.out.print(naive_term1+"\t");
			System.out.print(curr_naive_term2+"\t");
			System.out.print( mh_term1+"\t");
			System.out.print( curr_mh_term2+"\t" );
			System.out.print( curr_mh_term3+"\t" );
			System.out.print(min_term1+"\t");
			System.out.print(curr_min_term2+"\t");
			System.out.print(curr_min_term3+"\t");
			System.out.print("|\t");
			System.out.print(currentThreshold+"\t");
			System.out.print(naiveEstimation+"\t");
			System.out.print(joinmhEstimation+"\t");
			System.out.print((naiveEstimation+joinmhEstimation)+"\t");
			System.out.print(joinminEstimation+"\t");
			System.out.print((naiveEstimation+joinminEstimation)+"\t");
			System.out.println(  );
			
			try {
				bw_log.write( String.format( "%d\t", currentThreshold ) );

				// naive
				bw_log.write( String.format( "%d\t%d\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t|\t", 
						naive_term1, curr_naive_term2, coeff_naive_1*naive_term1/1e6, coeff_naive_2*curr_naive_term2/1e6, 
						getEstimateNaive( naive_term1, curr_naive_term2 )/1e6,
						coeff_naive_1*naive_term1/sampleRatio/1e6, coeff_naive_2*curr_naive_term2/sampleRatio/1e6,
						getEstimateNaive( naive_term1/sampleRatio, curr_naive_term2/sampleRatio )/1e6 
						) );

				// FKP
				bw_log.write( String.format( "%d\t%d\t%d\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t|\t", 
						mh_term1, curr_mh_term2, curr_mh_term3,
						coeff_mh_1*mh_term1/1e6, coeff_mh_2*curr_mh_term2/1e6, coeff_mh_3*curr_mh_term3/1e6,
						getEstimateJoinMH( mh_term1, curr_mh_term2, curr_mh_term3)/1e6,
						coeff_mh_1*mh_term1/sampleRatio/1e6, coeff_mh_2*curr_mh_term2/sampleRatio/1e6, 
						coeff_mh_3*curr_mh_term3/sampleRatio/sampleRatio/1e6, 
						getEstimateJoinMH( mh_term1/sampleRatio, curr_mh_term2/sampleRatio, curr_mh_term3/sampleRatio/sampleRatio)/1e6
						) );

				// BKP
				bw_log.write( String.format( "%d\t%d\t%d\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t|\t", 
						min_term1, curr_min_term2, curr_min_term3, 
						coeff_min_1*min_term1/1e6, coeff_min_2*curr_min_term2/1e6, coeff_min_3*curr_min_term3/1e6,
						getEstimateJoinMin( min_term1, curr_min_term2, curr_min_term3 )/1e6,
						coeff_min_1*min_term1/sampleRatio/1e6, coeff_min_2*curr_min_term2/sampleRatio/1e6, 
						coeff_min_3*curr_min_term3/sampleRatio/sampleRatio/1e6, 
						getEstimateJoinMin( min_term1/sampleRatio, curr_min_term2/sampleRatio, curr_min_term3/sampleRatio/sampleRatio)/1e6
						) );

				bw_log.write( "\n" );
				bw_log.flush();
			}
			catch ( IOException e ) { e.printStackTrace(); }

//			Util.printLog( String.format( "T: %d nT: %d NT: %.10e JT(JoinMH): %.10e TT: %.10e JT(JoinMin): %.10e TT: %.10e", currentThreshold, nextThreshold,
//					naiveEstimation, joinmhEstimation, naiveEstimation + joinmhEstimation, joinminEstimation, naiveEstimation + joinminEstimation ) );
//			Util.printLog( String.format( "T: %d nT: %d NT: %.10e JT(JoinMin): %.10e TT: %.10e", currentThreshold, nextThreshold,
//					naiveEstimation, joinminEstimation, naiveEstimation + joinminEstimation ) );
//			Util.printLog( "JoinMin Selected " + tempJoinMinSelected );

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
			
			if (stop) break;
		} // end while searching best threshold
		
//		if ( getEstimateNaive( naive_term1/sampleRatio, naive_term2[sampleSearchedList.size()-1]/sampleRatio, naive_term3[sampleSearchedList.size()-1]/sampleRatio/sampleRatio ) < bestEstTime ) {
//			bestThreshold = Integer.MAX_VALUE;
//			bestEstTime = estTime_naive;
//		}

		stat.add( "Auto_Best_Threshold", bestThreshold );
		stat.add( "Auto_Best_Estimated_Time", bestEstTime );
		stat.add( "Auto_JoinMin_Selected", "" + joinMinSelected );
		return bestThreshold;
	}

	public boolean getJoinMinSelected() {
		return joinMinSelected;
	}

	protected ObjectArrayList<Record> sampleRecords( List<Record> recordList, Random rn ) {
		if (!stratified) return sampleRecordsNaive( recordList, rn );
		else return sampleRecordsStratified( recordList, rn );
	}

	protected ObjectArrayList<Record> sampleRecordsNaive( List<Record> recordList, Random rn ) {
		ObjectArrayList<Record> sampledList = new ObjectArrayList<>();
		for( Record r : recordList ) {
			if ( r.getEstNumTransformedArray() != null && r.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) continue;
			if( rn.nextDouble() < this.sampleRatio ) {
				sampledList.add( r );
			}
		}
		return sampledList;
	}
	
	protected ObjectArrayList<Record> sampleRecordsStratified( List<Record> recordList, Random rn ) {
		Comparator<Record> comp = new RecordComparator();
		
		ObjectArrayList<Record> sampledList = new ObjectArrayList<Record>();
		List<Record> searchedList = new ArrayList<Record>( recordList );
		Collections.sort( searchedList, comp );
		int n_stratum = 20;
		for ( int stratum_idx=0; stratum_idx<n_stratum; ++stratum_idx ) {
			System.out.println( stratum_idx+"\t"+searchedList.size()/n_stratum*stratum_idx+"\t"+searchedList.get( searchedList.size()/n_stratum*stratum_idx ).getEstNumTransformed() );
		}
		
		for ( int stratum_idx=0; stratum_idx<n_stratum; ++stratum_idx ) {
			int start = searchedList.size()/n_stratum*stratum_idx;
			int end = searchedList.size()/n_stratum*(stratum_idx+1);
			for ( int i=start; i<end; ++i ) {
				if (rn.nextDouble() < this.sampleRatio) {
					sampledList.add( searchedList.get( i ) );
				}
			}
		}
		return sampledList;
	}

	protected class RecordComparator implements Comparator<Record> {
		@Override
		public int compare( Record o1, Record o2 ) {
			long est1 = o1.getEstNumTransformed();
			long est2 = o2.getEstNumTransformed();
			return Long.compare( est1, est2 );
		}
	};
}
