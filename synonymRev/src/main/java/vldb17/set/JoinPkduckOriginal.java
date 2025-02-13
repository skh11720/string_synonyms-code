package vldb17.set;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.algorithm.AbstractIndexBasedAlgorithm;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.order.AbstractGlobalOrder;
import snu.kdd.synonym.synonymRev.order.AbstractGlobalOrder.Ordering;
import snu.kdd.synonym.synonymRev.order.FrequencyFirstOrder;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.ResultSet;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import vldb17.GreedyValidatorOriginal;


public class JoinPkduckOriginal extends AbstractIndexBasedAlgorithm {

	public final int qSize = 1; // a string is represented as a set of (token, pos) pairs.
	public final Ordering mode;
	public final double theta;
	public boolean useRuleComp;

//	private PkduckSetIndex idxS = null;
	private PkduckSetIndex idxT = null;
	AbstractGlobalOrder globalOrder;

	// staticitics used for building indexes
	double avgTransformed;
	
	private long candTokenTime = 0;
	private long isInSigUTime = 0;
	private long filteringTime = 0;
	private long validateTime = 0;
	private long nScanList = 0;
	private long nRunDP = 0;

	private boolean useLF;
	
	public static PrintWriter pw = null;

	public JoinPkduckOriginal(String[] args) {
		super(args);
		mode = Ordering.valueOf( param.getStringParam("ord") );
		theta = param.getDoubleParam("theta");
		useRuleComp = param.getBooleanParam("rc");
		useLF = param.getBooleanParam("useLF");
	}
	
	@Override
	public void initialize() {
		super.initialize();
		switch(mode) {
		case FF: globalOrder = new FrequencyFirstOrder( 1 ); break;
		default: throw new RuntimeException("Unexpected error");
		}

		checker = new GreedyValidatorOriginal(theta);

		try {
			String[] tokens = query.getSearchedPath().split("\\"+File.separator);
			String data1Name = tokens[tokens.length-1].split("\\.")[0];
			if ( query.selfJoin ) pw = new PrintWriter( new BufferedWriter( new FileWriter( String.format( "tmp/JoinPkduckOriginal_verify_%s_%.3f.txt", data1Name, theta ) ) ) ); 
			else {
				tokens = query.getIndexedPath().split("\\"+File.separator);
				String data2Name = tokens[tokens.length-1].split("\\.")[0];
				pw = new PrintWriter( new BufferedWriter( new FileWriter( String.format( "tmp/JoinPkduckOriginal_verify_%s_%s_%.3f.txt", data1Name, data2Name, theta) ) ) );
			}
		}
		catch (IOException e) { e.printStackTrace(); }
	}
	
	@Override
	protected void reportParamsToStat() {
		stat.add("Param_mode", mode.toString());
		stat.add("Param_theta", theta);
		stat.add("Param_useRuleComp", useRuleComp);
		stat.add("Param_useLF", useLF);
	}

	@Override
	public void preprocess() {
		super.preprocess();

		globalOrder.initializeForSet( query );
	}
	
	@Override
	protected void executeJoin() {
		StopWatch stepTime = null;
		stepTime = StopWatch.getWatchStarted( INDEX_BUILD_TIME );
		buildIndex();
		stepTime.stopAndAdd( stat );
		stepTime.resetAndStart( JOIN_AFTER_INDEX_TIME );
		stat.addMemory( "Mem_3_BuildIndex" );

		rslt = join( stat, query, writeResultOn );
		stepTime.stopAndAdd( stat );
		stat.addMemory( "Mem_4_Joined" );
	}
	
	public void buildIndex() {
		idxT = new PkduckSetIndex( query.indexedSet.recordList, query, theta, stat, globalOrder, writeResultOn );
	}
	
	public ResultSet join(StatContainer stat, Query query, boolean addStat) {
		ResultSet rslt = new ResultSet(query.selfJoin);
		if ( !query.oneSideJoin ) throw new RuntimeException("UNIMPLEMENTED CASE");
		
		// S -> S' ~ T
		for ( Record recS : query.searchedSet.recordList ) {
			if ( recS.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) continue;
			joinOneRecord( recS, rslt, idxT );
		}
		
		if ( addStat ) {
			stat.add( "Result_3_3_CandTokenTime", candTokenTime );
			stat.add( "Result_3_4_IsInSigUTime", isInSigUTime/1e6 );
			stat.add( "Result_3_5_FilteringTime", filteringTime );
			stat.add( "Result_3_6_ValidateTime", validateTime );
			stat.add( "Result_3_7_nScanList", nScanList );
			stat.add( "Result_3_8_nRunDP", nRunDP );
		}
		return rslt;
	}

	private void joinOneRecord( Record rec, ResultSet rslt, PkduckSetIndex idx ) {
		long startTime = System.currentTimeMillis();
		IntOpenHashSet candidateTokens = new IntOpenHashSet();
		for (int i=0; i<rec.size(); i++) {
			for (Rule rule : rec.getSuffixApplicableRules( i )) {
				int[] rhs = rule.getRight();
				for (int j=0; j<rule.rightSize()+1-qSize; j++) {
					candidateTokens.add( rhs[j] );
				}
			}
		}
		long afterCandTokenTime = System.currentTimeMillis();

//		boolean debug = true;
//		if ( rec.getID() == 161 ) debug = true;
//		if (debug) SampleDataTest.inspect_record( rec, query, 1 );
//		if (debug) System.out.println(rec);
		
		Set<Record> candidateAfterLF = new ObjectOpenHashSet<Record>();
		int rec_maxlen = rec.getMaxTransLength();
		PkduckSetDP pkduckSetDP;
		if (useRuleComp) pkduckSetDP = new PkduckSetDPWithRC( rec, theta, globalOrder );
		else pkduckSetDP = new PkduckSetDP( rec, theta, globalOrder );
		for (int token : candidateTokens) {
			long startDPTime = System.nanoTime();
			boolean isInSigU = pkduckSetDP.isInSigU( token );
			++nRunDP;
//			if (debug) try { bw.write( rec.getID()+", "+token+": "+isInSigU+"\n" ); bw.flush(); } catch (IOException e ) {}
//			if (debug) System.out.println( rec.getID()+", "+token+": "+isInSigU );
			isInSigUTime += System.nanoTime() - startDPTime;
			if ( isInSigU ) {
				List<Record> indexedList = idx.get( token );
				if ( indexedList == null ) continue;
				++nScanList;
				for (Record recOther : indexedList) {
					if ( useLF ) {
//						if (debug) System.out.println( "length filtered?: "+(rec_maxlen < recOther.size()) );
						if ( rec_maxlen < recOther.getDistinctTokenCount() ) {
							++checker.lengthFiltered;
							continue;
						}
					}
					candidateAfterLF.add( recOther );
				}
			}
		}
//		if (debug) System.exit( 1 );
		long afterFilteringTime = System.currentTimeMillis();
		
		// verification
		for (Record recOther : candidateAfterLF ) {
			if ( rslt.contains(rec, recOther) ) continue;
			int comp = checker.isEqual( rec, recOther );
//			if (debug) System.out.println( "compare "+rec.getID()+" and "+recOther.getID()+": "+comp );
			if ( comp >= 0 ) rslt.add(rec, recOther);;
		}
		long afterValidateTime = System.currentTimeMillis();
		
		candTokenTime += afterCandTokenTime - startTime;
		filteringTime += afterFilteringTime - afterCandTokenTime;
		validateTime += afterValidateTime - afterFilteringTime;
	}

	@Override
	public String getVersion() {
		/*
		 * 1.00: initial version
		 * 1.01: major update
		 * 1.02: bug fix
		 */
		return "1.02";
	}
	
	@Override
	public String getName() {
		return "JoinPkduckOriginal";
	}
	
	@Override
	public String getNameWithParam() {
		return String.format( "%s_%s_%.2f_%s", getName(), mode, theta, useRuleComp?"T":"F" );
	}
}
