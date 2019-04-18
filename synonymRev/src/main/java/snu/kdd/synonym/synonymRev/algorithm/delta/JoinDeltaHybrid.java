package snu.kdd.synonym.synonymRev.algorithm.delta;

import snu.kdd.synonym.synonymRev.algorithm.AbstractPosQGramBasedAlgorithm;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.estimation.DeltaEstimate;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.ResultSet;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.tools.Util;

public class JoinDeltaHybrid extends AbstractPosQGramBasedAlgorithm {

	DeltaEstimate estimate;

	public final int indexK;
	public final int deltaMax;
	public final String distFunc;
	public final double sampleB;
	public final double sampleH;
	public final boolean stratified = false;

	protected int nEst = 1;
	protected int joinThreshold = 1;
	protected boolean joinQGramRequired = true;
	protected boolean joinBKSelected = false;

	protected JoinDeltaNaiveIndex naiveIndex;
	protected JoinDeltaVarBKIndex joinBKIndex = null;
	protected JoinDeltaVarIndex joinFKIndex = null;
	
	protected final int indexK_fk = 3;
	protected final int qSize_fk = 4;
	protected final int indexK_bk = 2;
	protected final int qSize_bk = 2;


	public JoinDeltaHybrid(String[] args) {
		super(args);
		indexK = param.getIntParam("indexK");
		deltaMax = param.getIntParam("deltaMax");
		distFunc = param.getStringParam("dist");
		sampleB = param.getDoubleParam("sampleB");
		sampleH = param.getDoubleParam("sampleH");
		useLF = param.getBooleanParam("useLF");
		usePQF = param.getBooleanParam("usePQF");
		useSTPQ = param.getBooleanParam("useSTPQ");
	}
	
	@Override
	public void initialize() {
		super.initialize();
		checker = new DeltaValidatorDPTopDown(deltaMax, distFunc);
	}
	
	@Override
	protected void reportParamsToStat() {
		stat.add("Param_indexK", indexK);
		stat.add("Param_qSize", qSize);
		stat.add("Param_deltaMax", deltaMax);
		stat.add("Param_distFunct", distFunc);
		stat.add("Param_sampleB", sampleB);
		stat.add("Param_sampleH", sampleH);
		stat.add("Param_useLF", useLF);
		stat.add("Param_usePQF", usePQF);
		stat.add("Param_useSTPQ", useSTPQ);
	}
	
	@Override
	protected void preprocess() {
		super.preprocess();

		StopWatch estimateTime = StopWatch.getWatchStarted(ESTIMATION_TIME);
		StatContainer statEst = new StatContainer();
		int[] list_thres = new int[nEst];
		double[] list_bestTime = new double[nEst];
		boolean[] list_bkSelected= new boolean[nEst];
		for ( int i=0; i<nEst; ++i ) {
			findConstants( sampleH, statEst );
			list_thres[i] = estimate.findThetaJoinHybridAll(statEst);
			list_bkSelected[i] = estimate.getJoinBKSelected();
			list_bestTime[i] = estimate.bestEstTime;
		}
		double mean = 0, var = 0;
		for ( int i=0; i<nEst; ++i ) mean += list_thres[i];
		mean /= nEst;
		for ( int i=0; i<nEst; ++i ) var += (list_thres[i] - mean)*(list_thres[i] - mean);
		var /= nEst;
		double bestTime = Double.MAX_VALUE;
		for ( int i=0; i<nEst; ++i ) {
			if ( list_bestTime[i] < bestTime ) {
				bestTime = list_bestTime[i];
				joinThreshold = list_thres[i];
				joinBKSelected = list_bkSelected[i];
			}
//			System.out.println( list_thres[i]+"\t"+list_bestTime[i]+"\t"+list_bkSelected[i] );
		}

		Util.printLog( "Selected Threshold: " + joinThreshold );
		Util.printLog( "JoinBKSelected: " + (joinBKSelected? "true":"false") );
		stat.add( "Estimate_Threshold", joinThreshold );
		stat.add( "Estimate_Repeat", nEst );
		stat.add( "Estimate_Best_Time", bestTime );
		stat.add( "Estimate_Mean_Threshold", mean );
		stat.add( "Estimate_Var_Threshold", var );
		stat.add( "Estimate_JoinBKSelected", joinBKSelected? "true":"false" );
		stat.add( "EStimate_Stratified", stratified? "true":"false");
		estimateTime.stopAndAdd( stat );
	}

	@Override
	protected void executeJoin() {
		buildIndex();
		
		// join
		ResultSet rsltNaive = new ResultSet(query.selfJoin);
		ResultSet rsltPQGram = new ResultSet(query.selfJoin);
		int pqgramSearch = 0;
		int naiveSearch = 0;
		int skipped = 0;
		long joinNaiveTime = 0;
		long joinPQGramTime = 0;
		long joinStart = System.nanoTime();
		for( Record s : query.searchedSet.get() ) {
			long joinStartOne = System.nanoTime();
			// System.out.println( "test " + s + " " + s.getEstNumRecords() );
			if ( s.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) {
				++skipped;
				continue;
			}
			if ( joinQGramRequired && s.getEstNumTransformed() > joinThreshold ) {
				if ( joinBKSelected ) joinBKIndex.joinOneRecord( s, rsltPQGram, checker );
				else joinFKIndex.joinOneRecord( s, rsltPQGram, checker );
				++pqgramSearch;
				joinPQGramTime += System.nanoTime() - joinStartOne;
			}
			else {
				naiveIndex.joinOneRecord( s, rsltNaive, checker );
				++naiveSearch;
				joinNaiveTime += System.nanoTime() - joinStartOne;
			}
		}
		long joinTime = System.nanoTime() - joinStart;

		stat.add( "Join_Naive_Result", rsltNaive.size() );
		stat.add( "Join_PQGram_Result", rsltPQGram.size() );
		stat.add( JOIN_AFTER_INDEX_TIME, joinTime/1e6 );
		stat.add( "Result_3_2_1_Join_Naive_Time", joinNaiveTime/1e6 );
		stat.add( "Result_3_2_2_Join_PQGram_Time", joinPQGramTime/1e6 );
		stat.add( "Stat_Naive_Search", naiveSearch );
		stat.add( "Stat_PQGram_Search", pqgramSearch );
		stat.add( "Stat_Skipped", skipped );
		if ( joinQGramRequired ) {
			long candQGramCountSum = 0;
			double candQGramAvgCount = 0;
			if ( joinBKSelected ) candQGramCountSum = joinBKIndex.algstat.candQGramCount;
			else candQGramCountSum = joinFKIndex.algstat.candQGramCount;
			candQGramAvgCount = pqgramSearch == 0 ? 0 : 1.0 * candQGramCountSum / pqgramSearch;
			stat.add( "Stat_CandQGram_Sum", candQGramCountSum );
			stat.add( "Stat_CandQGram_Avg", candQGramAvgCount );
		}
		
		// return the final result
		rslt = new ResultSet(query.selfJoin);
		rslt.addAll( rsltNaive );
		rslt.addAll( rsltPQGram );
	}
	
	@Override
	protected void buildIndex() {
		StopWatch buildTime = StopWatch.getWatchStarted( INDEX_BUILD_TIME );
		long maxSearchedEstNumRecords = computeMaxEstTransNum();
		if ( maxSearchedEstNumRecords <= joinThreshold ) {
			joinQGramRequired = false; // in this case both joinFK and joinBK are not used.
		}
		stat.add("JoinQGramRequired:", joinQGramRequired);

		if( joinQGramRequired ) {
			if( joinBKSelected ) buildJoinBKIndex();
			else buildJoinFKIndex();
		}
		buildNaiveIndex();
		buildTime.stopAndAdd( stat );
	}

    private long computeMaxEstTransNum() {
        long estTransNum = 0;
        for( Record rec : query.searchedSet.get() ) {
            if( estTransNum < rec.getEstNumTransformed() ) {
                estTransNum = rec.getEstNumTransformed();
            }
        }
        stat.add( "MaxSearchedEstNumRecords", estTransNum );
        return estTransNum;
    }

	protected void buildJoinBKIndex() {
		joinBKIndex= JoinDeltaVarBKIndex.getInstance(query, indexK_bk, qSize_bk, deltaMax, distFunc, sampleB);
	}

	protected void buildJoinFKIndex() {
		joinFKIndex = JoinDeltaVarIndex.getInstance(query, indexK_fk, qSize_fk, deltaMax, distFunc);
	}

	protected void buildNaiveIndex() {
		naiveIndex = new JoinDeltaNaiveIndex(deltaMax, distFunc, query);
	}

	protected void findConstants( double sampleratio, StatContainer stat ) {
		estimate = new DeltaEstimate(query, this);
		estimate.estimateJoinHybridWithSample();
		
		this.stat.add( "Estimate_Coeff1_Naive", estimate.coeff_naive_1);
		this.stat.add( "Estimate_Coeff2_Naive", estimate.coeff_naive_2);
		this.stat.add( "Estimate_Term1_Naive", estimate.naive_term1);
		this.stat.add( "Estimate_Term2_Naive", estimate.naive_term2[estimate.sampleSearchedSize-1]);
		this.stat.add( "Estimate_Time_Naive", estimate.estTime_naive );

		this.stat.add( "Estimate_Coeff1_FK", estimate.coeff_FK_1);
		this.stat.add( "Esitmate_Coeff2_FK", estimate.coeff_FK_2);
		this.stat.add( "Estimate_Coeff3_FK", estimate.coeff_FK_3);
		this.stat.add( "Estimate_Term1_FK", estimate.fk_term1);
		this.stat.add( "Estimate_Term2_FK", estimate.fk_term2[estimate.sampleSearchedSize-1]);
		this.stat.add( "Estimate_Term3_FK", estimate.fk_term3[estimate.sampleSearchedSize-1]);
		this.stat.add( "Estimate_Time_FK", estimate.estTime_FK );

		this.stat.add( "Estimate_Coeff1_BK", estimate.coeff_BK_1);
		this.stat.add( "Estimate_Coeff2_BK", estimate.coeff_BK_2);
		this.stat.add( "Estimate_Coeff3_BK", estimate.coeff_BK_3);
		this.stat.add( "Estimate_Term1_BK", estimate.bk_term1);
		this.stat.add( "Estimate_Term2_BK", estimate.bk_term2[estimate.sampleSearchedSize-1]);
		this.stat.add( "Estimate_Term3_BK", estimate.bk_term3[estimate.sampleSearchedSize-1]);
		this.stat.add( "Estimate_Time_BK", estimate.estTime_BK );
	}

	@Override
	protected void runAfterPreprocess() {
		System.err.println("THIS METHOD IS NOT SUPPOSED TO BE CALLED!");
	}

	@Override
	protected void runAfterPreprocessWithoutIndex() {
		System.err.println("THIS METHOD IS NOT SUPPOSED TO BE CALLED!");
	}

	@Override
	public String getVersion() {
		/*
		 * 1.00: initial version
		 * 1.01: change default paramters for fk and bk, set term2 = numTransS for JoinDeltaNaive
		 */
		return "1.01";
	}

	@Override
	public String getName() {
		return "JoinDeltaHybrid";
	}
	
	@Override
	public String getNameWithParam() {
		return String.format("%s_%d_%d_%.2f", getName(), indexK, qSize, sampleH);
	}
}
