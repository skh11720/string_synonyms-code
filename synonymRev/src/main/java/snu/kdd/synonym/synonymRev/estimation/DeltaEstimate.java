package snu.kdd.synonym.synonymRev.estimation;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.synonym.synonymRev.algorithm.delta.DeltaValidatorDPTopDown;
import snu.kdd.synonym.synonymRev.algorithm.delta.DeltaValidatorNaive;
import snu.kdd.synonym.synonymRev.algorithm.delta.JoinDeltaHybrid;
import snu.kdd.synonym.synonymRev.algorithm.delta.JoinDeltaNaiveIndex;
import snu.kdd.synonym.synonymRev.algorithm.delta.JoinDeltaVarBKIndex;
import snu.kdd.synonym.synonymRev.algorithm.delta.JoinDeltaVarIndex;
import snu.kdd.synonym.synonymRev.data.Dataset;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.ResultSet;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.Util;

// Refactoring code for estimate

public class DeltaEstimate {
	
	final int indexK;
	final int qSize;
	final int deltaMax; 
	final String dist;
	final double sampleB;

	// coefficients of naive index
	public double estTime_naive;
	public double coeff_naive_1;
	public double coeff_naive_2;

	public double estTime_FK;
	// gamma : JoinFK indexing time per pogram of table T
	public double coeff_FK_1;
	// zeta: JoinFK time for counting TPQ supersets per pqgram of table S
	public double coeff_FK_2;
	// eta : JoinFK join time per record of table T
	public double coeff_FK_3;

	public double estTime_BK;
	// lambda: JoinBK indexing time per twograms of table T
	public double coeff_BK_1;
	// mu: JoinBK counting twogram time per twograms of table S
	public double coeff_BK_2;
	// rho: JoinBK join time per candidate of table S
	public double coeff_BK_3;


	public double sampleRatio;
	public double bestEstTime = Double.MAX_VALUE;

	protected Dataset originalSearched;
	protected Dataset originalIndexed;
	
	public long naive_term1;
	public long[] naive_term2;
	public long fk_term1;
	public long[] fk_term2;
	public long[] fk_term3;
	public long bk_term1;
	public long[] bk_term2;
	public long[] bk_term3;

	protected boolean joinBKSelected = false;

	protected Query sampleQuery;
	protected boolean stratified = false;
	public int sampleSearchedSize;
	public long sampleSearchedNumEstTrans;
	protected ObjectArrayList<Record> sampleSearchedList = new ObjectArrayList<Record>();
	protected ObjectArrayList<Record> sampleIndexedList = new ObjectArrayList<Record>();
	protected BufferedWriter bw_log = null;
	
	public DeltaEstimate(final Query query, final JoinDeltaHybrid alg ) {
		this.indexK = alg.indexK;
		this.qSize = alg.qSize;
		this.deltaMax = alg.deltaMax;
		this.dist = alg.distFunc;
		this.sampleRatio = alg.sampleH;
		this.sampleB = alg.sampleB;

		try { 
			String logFileName = String.format( "SampleEst_%s_%.2f", query.dataInfo.getName(), sampleRatio );
			bw_log = new BufferedWriter( new FileWriter( "tmp/"+logFileName+".txt" ) );
		}
		catch ( IOException e ) { e.printStackTrace(); }

		long seed = System.currentTimeMillis();
		Util.printLog( "Random seed: " + seed );
		Random rn = new Random( seed );
		
		sampleSearchedList = sampleRecords( query.searchedSet.recordList, rn );
		if( query.selfJoin ) {
			for( Record r : sampleSearchedList ) {
				sampleIndexedList.add( r );
			}
		}
		else sampleIndexedList = sampleRecords( query.indexedSet.recordList, rn );

		Util.printLog( sampleSearchedList.size() + " Searched records are sampled" );
		Util.printLog( sampleIndexedList.size() + " Indexed records are sampled" );
		
		initialize(query);
	}
	
	@Override
	protected void finalize() throws Throwable {
		bw_log.flush();
		bw_log.close();
	}

	public void initialize( final Query query ) {
		sampleSearchedSize = sampleSearchedList.size();

		Comparator<Record> comp = new RecordComparator();
		Collections.sort( sampleSearchedList, comp );
		Collections.sort( sampleIndexedList, comp );

		Dataset sampleIndexed = new Dataset( sampleIndexedList );
		Dataset sampleSearched = new Dataset( sampleSearchedList );
		sampleQuery = new Query( query.ruleSet, sampleIndexed, sampleSearched, query.tokenIndex, query.oneSideJoin,
				query.selfJoin, query.outputPath );
		
		naive_term2 = new long[sampleSearchedList.size()];
		fk_term2 = new long[sampleSearchedList.size()];
		fk_term3 = new long[sampleSearchedList.size()];
		bk_term2 = new long[sampleSearchedList.size()];
		bk_term3 = new long[sampleSearchedList.size()];
		inspectSample(query);
	}
	
	public void inspectSample( final Query query ) {
		
		double avgTrans0 = getAvgEstTrans(query);
		double avgTrans1 = getAvgEstTrans(sampleQuery);
		double avgNAR0 = getAvgNumAppRules(query);
		double avgNAR1 = getAvgNumAppRules(sampleQuery);
		
		System.out.println(String.format("AvgEstTrans: %.6f\t%.6f", avgTrans0, avgTrans1));
		System.out.println(String.format("AvgNAR: %.6f\t%.6f", avgNAR0, avgNAR1));
	}
	
	public double getAvgEstTrans( final Query query ) {
		double avgTrans = 0;
		for ( Record recS : query.searchedSet.recordList ) {
			avgTrans += recS.getEstNumTransformed();
		}
		avgTrans /= query.searchedSet.size();
		return avgTrans;
	}
	
	public double getAvgNumAppRules( final Query query ) {
		double avgNAR = 0;
		for ( Record recS : query.searchedSet.recordList ) {
			avgNAR += recS.getNumApplicableRules();
		}
		avgNAR /= query.searchedSet.size();
		return avgNAR;
	}
	
	public Object2DoubleMap<String> estimateJoinNaive() {

		JoinDeltaNaiveIndex naiveinst;
		ResultSet rslt = new ResultSet(sampleQuery.selfJoin);
		DeltaValidatorNaive checker = new DeltaValidatorNaive(deltaMax, dist);

		long ts = System.nanoTime();
		naiveinst = new JoinDeltaNaiveIndex(deltaMax, dist, sampleQuery);
		double indexTime = (System.nanoTime() - ts)/1e6;

		ts = System.nanoTime();
		for (int i = 0; i < sampleQuery.searchedSet.size(); i++) {
			Record recS = sampleQuery.searchedSet.getRecord( i );
			if ( recS.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) {
				naive_term2[i] = (i == 0 ? 0 : naive_term2[i-1]);
			}
			else {
				naiveinst.joinOneRecord( recS, rslt, checker );
				naive_term2[i] = naiveinst.algstat.numCand;
//				naive_term3[i] = naiveinst.verifyCost;
//				naive_term3[i] = naiveinst.expCount*sampleIndexedList.size();
			}
		} // end for id
		double joinTime = (System.nanoTime() - ts)/1e6;
		naive_term1 = naiveinst.algstat.numVPQt;

		// compute coefficients
		coeff_naive_1 = indexTime/(naiveinst.algstat.numVPQt+1);
		coeff_naive_2 = joinTime/(naiveinst.algstat.numCand+1);
		
		System.out.println( "estimateJoinNaive" );
		System.out.println( "coeff_naive1: "+coeff_naive_1 );
		System.out.println( "coeff_naive_2: "+coeff_naive_2 );
		System.out.println( "Naive_Term_1: "+naive_term1 );
		System.out.println( "Naive_Term_2: "+naive_term2[sampleSearchedList.size()-1] );

		estTime_naive = getEstimateNaive( naive_term1, naive_term2[sampleSearchedList.size()-1] );
		System.out.println( "Est_Time: "+ estTime_naive );
		System.out.println( "indexTime: "+String.format("%.6e", indexTime/1e6));
		System.out.println( "Join_Time: "+String.format("%.6e", joinTime/1e6));
		
		Object2DoubleOpenHashMap<String> output = new Object2DoubleOpenHashMap<>();
		output.put( "Naive_Coeff_1", coeff_naive_1 );
		output.put( "Naive_Coeff_2", coeff_naive_2 );
		output.put( "Naive_Term_1", naive_term1 );
		output.put( "Naive_Term_2", naive_term2[sampleSearchedList.size()-1] );
		output.put( "Naive_Est_Time", estTime_naive );
		output.put( "Naive_Join_Time", joinTime );
		return output;
	}
	
	protected double sampleJoinMH( JoinDeltaVarIndex joinFKInst ) {
		DeltaValidatorDPTopDown checker = new DeltaValidatorDPTopDown(deltaMax, dist);
		long ts = System.nanoTime();
		ResultSet rslt = new ResultSet(sampleQuery.selfJoin);
		for (int i = 0; i < sampleQuery.searchedSet.size(); i++) {
			Record recS = sampleQuery.searchedSet.getRecord( i );
			if ( recS.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) {
				fk_term2[i] = (i == 0 ? 0 : fk_term2[i-1]);
				fk_term3[i] = (i == 0 ? 0 : fk_term3[i-1]);
			}
			else {
				joinFKInst.joinOneRecord( recS, rslt, checker );
				fk_term2[i] =  joinFKInst.algstat.candQGramCount;
				fk_term3[i] =  joinFKInst.algstat.numVerified;
			}
		} // for sid in in searchedSet
		double joinTime = (System.nanoTime() - ts)/1e6;
		return joinTime;
	}

	public Object2DoubleMap<String> estimateJoinFK() {
		int[] indexPosition = new int[indexK];
		for (int i=0; i<indexK; ++i ) indexPosition[i] = i;

		long ts = System.nanoTime();
		JoinDeltaVarIndex joinFKInst = JoinDeltaVarIndex.getInstance(sampleQuery, indexK, qSize, deltaMax, dist);
		double indexTime = (System.nanoTime() - ts)/1e6;
		double joinTime = sampleJoinMH(joinFKInst);

		fk_term1 = joinFKInst.algstat.idxQGramCount;
		coeff_FK_1 = indexTime / (fk_term1+1);
		coeff_FK_2 = joinFKInst.algstat.candFilterTime / (joinFKInst.algstat.candQGramCount+1);
		coeff_FK_3 = joinFKInst.algstat.verifyTime / (joinFKInst.algstat.numVerified+1);
		estTime_FK = getEstimateJoinFK( fk_term1, fk_term2[sampleSearchedList.size()-1], fk_term3[sampleSearchedList.size()-1] );

		System.out.println( "estimateJoinFK" );
		System.out.println( "coeff_FK_1: "+coeff_FK_1 );
		System.out.println( "coeff_FK_2: "+coeff_FK_2 );
		System.out.println( "coeff_FK_3: "+coeff_FK_3 );
		System.out.println( "FK_Term_1: "+ fk_term1 );
		System.out.println( "FK_Term_2: "+ fk_term2[sampleSearchedList.size()-1] );
		System.out.println( "FK_Term_3: "+ fk_term3[sampleSearchedList.size()-1] );
		System.out.println( "Est_Time: "+ estTime_FK );
		System.out.println( "Join_Time: "+String.format( "%.10e", joinTime ) );

		Object2DoubleOpenHashMap<String> output = new Object2DoubleOpenHashMap<>();
		output.put( "FK_Coeff_1", coeff_FK_1 );
		output.put( "FK_Coeff_2", coeff_FK_2 );
		output.put( "FK_Coeff_3", coeff_FK_3 );
		output.put( "FK_Term_1", fk_term1 );
		output.put( "FK_Term_2", fk_term2[sampleSearchedList.size()-1] );
		output.put( "FK_Term_3", fk_term3[sampleSearchedList.size()-1] );
		output.put( "FK_Est_Time", estTime_FK );
		output.put( "FK_Join_Time", joinTime );
		return output;
	}
	
	protected double sampleJoinBK( JoinDeltaVarBKIndex joinBKInst ) {
		ResultSet rslt = new ResultSet(sampleQuery.selfJoin);
		DeltaValidatorDPTopDown checker = new DeltaValidatorDPTopDown(deltaMax, dist);

		long ts = System.nanoTime();
		for (int i = 0; i < sampleQuery.searchedSet.size(); i++) {
			Record recS = sampleQuery.searchedSet.getRecord( i );
			if ( recS.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) {
				bk_term2[i] = ( i== 0? 0 : bk_term2[i-1]);
				bk_term3[i] = ( i== 0? 0 : bk_term3[i-1]);
			}
			else {
				joinBKInst.joinOneRecord( recS, rslt, checker );
				bk_term2[i] = joinBKInst.algstat.candQGramCount;
				bk_term3[i] = joinBKInst.algstat.numVerified;
			}
		}
		double joinTime = (System.nanoTime() - ts)/1e6;
		return joinTime;
	}

	public Object2DoubleMap<String> estimateJoinBK() {
		long ts = System.nanoTime();
		JoinDeltaVarBKIndex joinBKInst = JoinDeltaVarBKIndex.getInstance(sampleQuery, indexK, qSize, indexK, dist, sampleB);
		double indexTime = (System.nanoTime() - ts)/1e6;
		double joinTime = sampleJoinBK(joinBKInst);

		bk_term1 = joinBKInst.algstat.idxQGramCount+1;
		coeff_BK_1 = indexTime / bk_term1;
		coeff_BK_2 = joinBKInst.algstat.candFilterTime / (joinBKInst.algstat.candQGramCount+1);
		coeff_BK_3 = joinBKInst.algstat.verifyTime / (joinBKInst.algstat.numVerified+1);
		estTime_BK = getEstimateJoinBK( bk_term1, bk_term2[sampleSearchedList.size()-1], bk_term3[sampleSearchedList.size()-1]);

		System.out.println( "estimateJoinBK" );
		System.out.println( "coeff_BK_1: "+coeff_BK_1 );
		System.out.println( "coeff_BK_2: "+coeff_BK_2 );
		System.out.println( "coeff_BK_3: "+coeff_BK_3 );
		System.out.println( "BK_Term_1: "+ bk_term1 );
		System.out.println( "BK_Term_2: "+ bk_term2[sampleSearchedList.size()-1] );
		System.out.println( "BK_Term_3: "+ bk_term3[sampleSearchedList.size()-1] );
		System.out.println( "Est_Time: "+ estTime_BK );
		System.out.println( "Join_Time: "+String.format( "%.10e", joinTime ) );

		Object2DoubleOpenHashMap<String> output = new Object2DoubleOpenHashMap<>();
		output.put( "BK_Coeff_1", coeff_BK_1 );
		output.put( "BK_Coeff_2", coeff_BK_2 );
		output.put( "BK_Coeff_3", coeff_BK_3 );
		output.put( "BK_Term_1", bk_term1 );
		output.put( "BK_Term_2", bk_term2[sampleSearchedList.size()-1] );
		output.put( "BK_Term_3", bk_term3[sampleSearchedList.size()-1] );
		output.put( "BK_Est_Time", estTime_BK );
		output.put( "BK_Join_Time", joinTime );
		return output;
	}

    public void estimateJoinFKNaiveWithSample() {
        estimateJoinNaive();
        estimateJoinFK();
    }

    public void estimateJoinBKNaiveWithSample() {
        estimateJoinNaive();
        estimateJoinBK();
    }

	public void estimateJoinHybridWithSample() {
		estimateJoinNaive();
		estimateJoinFK();
		estimateJoinBK();
	}

	/*
	 * scale up terms by (1/sampleRatio)^p
	 */
	public double getEstimateNaive( double term1, double term2 ) {
		return coeff_naive_1 * term1 / sampleRatio 
				+ coeff_naive_2 * term2 / sampleRatio;
	}

	public double getEstimateJoinFK( double term1, double term2, double term3 ) {
		return coeff_FK_1 * term1 / sampleRatio 
				+ coeff_FK_2 * term2 / sampleRatio
				+ coeff_FK_3 * term3 / sampleRatio / sampleRatio;
	}

	public double getEstimateJoinBK( double term1, double term2, double term3 ) {
		return coeff_BK_1 * term1 / sampleRatio 
				+ coeff_BK_2 * term2 / sampleRatio
				+ coeff_BK_3 * term3 / sampleRatio / sampleRatio;
	}
	
	public int findThetaJoinHybridAll( StatContainer stat ) {
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
//		int tableIndexedSize = sampleIndexedList.size();
		int tableSearchedSize = sampleSearchedList.size();

		boolean stop = false;
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
			long curr_FK_term2 = (fk_term2[tableSearchedSize-1] - (sidx==0?0:fk_term2[sidx-1]));
			long curr_FK_term3 = (fk_term3[tableSearchedSize-1] - (sidx==0?0:fk_term3[sidx-1]));
			long curr_BK_term2 = (bk_term2[tableSearchedSize-1] - (sidx==0?0:bk_term2[sidx-1]));
			long curr_BK_term3 = (bk_term3[tableSearchedSize-1] - (sidx==0?0:bk_term3[sidx-1]));
			
			double naiveEstimation = this.getEstimateNaive( naive_term1, curr_naive_term2);
			// currExpLengthSize: sum of lengths of records in sampleIndexedSet
			// currExpSize: sum of estimated number of transformations of records in sampleSearchedSet

			double joinFKEstimation = this.getEstimateJoinFK( fk_term1, curr_FK_term2, curr_FK_term3);
			// searchedTotalSigCount: the sum of the size of TPQ superset of records in sampleSearchedSet
			// indexedTotalSigCount: the number of pos qgrams from records in sampleIndexedSet
			// totalJoinMHInvokes: the sum of the minimum number of records to be verified with t for every t in sampleIndexedSet
			// removedJoinMHComparison: 

			double joinBKEstimation = this.getEstimateJoinBK( bk_term1, curr_BK_term2, curr_BK_term3);
			// searchedTotalSigCount: the sum of the size of TPQ superset of records in sampleSearchedSet
			// indexedTotalSigCount: the number of pos qgrams from records in sampleIndexedSet

			boolean tempJoinBKSelected = joinBKEstimation < joinFKEstimation;

			System.out.print( (sidx)+"\t" );
			System.out.print( sampleSearchedNumEstTrans+"\t" );
			System.out.print(naive_term1+"\t");
			System.out.print(curr_naive_term2+"\t");
			System.out.print( fk_term1+"\t");
			System.out.print( curr_FK_term2+"\t" );
			System.out.print( curr_FK_term3+"\t" );
			System.out.print(bk_term1+"\t");
			System.out.print(curr_BK_term2+"\t");
			System.out.print(curr_BK_term3+"\t");
			System.out.print("|\t");
			System.out.print(currentThreshold+"\t");
			System.out.print(naiveEstimation+"\t");
			System.out.print(joinFKEstimation+"\t");
			System.out.print((naiveEstimation+joinFKEstimation)+"\t");
			System.out.print(joinBKEstimation+"\t");
			System.out.print((naiveEstimation+joinBKEstimation)+"\t");
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
						fk_term1, curr_FK_term2, curr_FK_term3,
						coeff_FK_1*fk_term1/1e6, coeff_FK_2*curr_FK_term2/1e6, coeff_FK_3*curr_FK_term3/1e6,
						getEstimateJoinFK( fk_term1, curr_FK_term2, curr_FK_term3)/1e6,
						coeff_FK_1*fk_term1/sampleRatio/1e6, coeff_FK_2*curr_FK_term2/sampleRatio/1e6, 
						coeff_FK_3*curr_FK_term3/sampleRatio/sampleRatio/1e6, 
						getEstimateJoinFK( fk_term1/sampleRatio, curr_FK_term2/sampleRatio, curr_FK_term3/sampleRatio/sampleRatio)/1e6
						) );

				// BKP
				bw_log.write( String.format( "%d\t%d\t%d\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t|\t", 
						bk_term1, curr_BK_term2, curr_BK_term3, 
						coeff_BK_1*bk_term1/1e6, coeff_BK_2*curr_BK_term2/1e6, coeff_BK_3*curr_BK_term3/1e6,
						getEstimateJoinBK( bk_term1, curr_BK_term2, curr_BK_term3 )/1e6,
						coeff_BK_1*bk_term1/sampleRatio/1e6, coeff_BK_2*curr_BK_term2/sampleRatio/1e6, 
						coeff_BK_3*curr_BK_term3/sampleRatio/sampleRatio/1e6, 
						getEstimateJoinBK( bk_term1/sampleRatio, curr_BK_term2/sampleRatio, curr_BK_term3/sampleRatio/sampleRatio)/1e6
						) );

				bw_log.write( "\n" );
				bw_log.flush();
			}
			catch ( IOException e ) { e.printStackTrace(); }

			double tempBestTime = naiveEstimation;

			if( tempJoinBKSelected ) tempBestTime += joinBKEstimation;
			else tempBestTime += joinFKEstimation;

			if( bestEstTime > tempBestTime ) {
				bestEstTime = tempBestTime;
				joinBKSelected = tempJoinBKSelected;

				if( currentThreshold < Integer.MAX_VALUE ) {
					bestThreshold = (int) currentThreshold;
				}
				else {
					currentThreshold = Integer.MAX_VALUE;
				}

				if( DEBUG.SampleStatON ) {
					Util.printLog( "New Best " + bestThreshold + " with " + joinBKSelected );
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
		stat.add( "Auto_JoinBK_Selected", "" + joinBKSelected );
		return bestThreshold;
	}

	public boolean getJoinBKSelected() {
		return joinBKSelected;
	}

	protected ObjectArrayList<Record> sampleRecords( List<Record> recordList, Random rn ) {
		if (!stratified) return sampleRecordsNaive( recordList, rn );
		else return sampleRecordsStratified( recordList, rn );
	}

	protected ObjectArrayList<Record> sampleRecordsNaive( List<Record> recordList, Random rn ) {
		ObjectArrayList<Record> sampledList = new ObjectArrayList<>();
		for( Record r : recordList ) {
			if ( r.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) continue;
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

		for ( int j=0; j<n_strat; ++j ) {
			if (bound[j+1] - bound[j] == 0) continue;
			// NOTE: sample at least one item from each strata.
			int strat_size = Math.max(1, (int)((bound[j+1]-bound[j])*sampleRatio));
			double p = 1.0*strat_size/(bound[j+1]-bound[j]);
			
			for ( int i=bound[j]; i<bound[j+1]; ++i ) {
				if ( rn.nextDouble() < p ) {
					sampledList.add( searchedList.get(i) );
				}
			}
		}
		return sampledList;
	}

	protected int getStratID( Record rec ) {
		return (int)Math.floor( Math.log10( rec.getEstNumTransformed() ) );
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
