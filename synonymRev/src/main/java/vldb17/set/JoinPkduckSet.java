package vldb17.set;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.ParseException;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.synonymRev.algorithm.set.SetNaiveOneSide;
import snu.kdd.synonym.synonymRev.algorithm.set.SetTopDownOneSide;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.order.AbstractGlobalOrder;
import snu.kdd.synonym.synonymRev.order.AbstractGlobalOrder.Ordering;
import snu.kdd.synonym.synonymRev.order.FrequencyFirstOrder;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.validator.Validator;
import vldb17.ParamPkduck;


public class JoinPkduckSet extends AlgorithmTemplate {

//	private PkduckSetIndex idxS = null;
	private PkduckSetIndex idxT = null;
	private final int qSize = 1; // a string is represented as a set of (token, pos) pairs.
	AbstractGlobalOrder globalOrder;
	private Boolean useRuleComp;
	private Validator checker;

	// staticitics used for building indexes
	double avgTransformed;
	
	private long candTokenTime = 0;
	private long isInSigUTime = 0;
	private long filteringTime = 0;
	private long validateTime = 0;
	private long nScanList = 0;
	private long nRunDP = 0;

	private boolean useLF;


	public JoinPkduckSet(Query query, String[] args) throws IOException, ParseException {
		super(query, args);
		param = new ParamPkduck(args);
		Ordering mode = Ordering.valueOf( param.getStringParam("ord") );
		switch(mode) {
		case FF: globalOrder = new FrequencyFirstOrder( 1 ); break;
		default: throw new RuntimeException("Unexpected error");
		}
		String verify = param.getStringParam("verify");
		if (verify.equals( "naive" )) checker = new SetNaiveOneSide( query.selfJoin );
		else if (verify.equals( "greedy" )) checker = new SetGreedyValidator( query.oneSideJoin );
		else if (verify.equals( "TD" )) checker = new SetTopDownOneSide( query.selfJoin );
		else throw new RuntimeException(getName()+" does not support verification: "+verify);
		useRuleComp = param.getBooleanParam("rc");
		useLF = param.getBooleanParam("useLF");
	}
	
	@Override
	public void preprocess() {
		super.preprocess();
		
		globalOrder.initializeForSet( query );
	}
	
	@Override
	protected void executeJoin() {
		StopWatch stepTime = null;
		stepTime = StopWatch.getWatchStarted( "Result_3_1_Index_Building_Time" );
		buildIndex( false );
		stepTime.stopAndAdd( stat );
		stepTime.resetAndStart( "Result_3_2_Join_Time" );
		stat.addMemory( "Mem_3_BuildIndex" );

		rslt = join( stat, query, writeResult );
		stepTime.stopAndAdd( stat );
		stat.addMemory( "Mem_4_Joined" );

		checker.addStat( stat );
	}
	
	public void buildIndex(boolean addStat ) {
		idxT = new PkduckSetIndex( query.indexedSet.recordList, query, 1, stat, globalOrder, addStat );
//		if ( !query.selfJoin ) idxS = new PkduckSetIndex( query.searchedSet.recordList, query, stat, globalOrder, addStat );
	}
	
	public Set<IntegerPair> join(StatContainer stat, Query query, boolean addStat) {
		ObjectOpenHashSet<IntegerPair> rslt = new ObjectOpenHashSet<IntegerPair>();
		if ( !query.oneSideJoin ) throw new RuntimeException("UNIMPLEMENTED CASE");
		
		// S -> S' ~ T
		for ( Record recS : query.searchedSet.recordList ) {
			if ( recS.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) continue;
			joinOneRecord( recS, rslt, idxT );
		}
		
//		if ( !query.selfJoin ) {
//			// T -> T' ~ S
//			for ( Record recT : query.indexedSet.recordList ) {
//				if ( recT.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) continue;
//				joinOneRecord( recT, rslt, idxS );
//			}
//		}
		
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

	private void joinOneRecord( Record rec, Set<IntegerPair> rslt, PkduckSetIndex idx ) {
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

//		Boolean debug = false;
//		if ( rec.getID() == 161 ) debug = true;
//		if (debug) SampleDataTest.inspect_record( rec, query, 1 );
		
		Set<Record> candidateAfterLF = new ObjectOpenHashSet<Record>();
		int rec_maxlen = rec.getMaxTransLength();
		PkduckSetDP pkduckSetDP;
		if (useRuleComp) pkduckSetDP = new PkduckSetDPWithRC( rec, 1, globalOrder );
		else pkduckSetDP = new PkduckSetDP( rec, 1, globalOrder );
		for (int token : candidateTokens) {
			long startDPTime = System.nanoTime();
			Boolean isInSigU = pkduckSetDP.isInSigU( token );
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
			int comp = checker.isEqual( rec, recOther );
//			if (debug) System.out.println( "compare "+rec.getID()+" and "+recOther.getID()+": "+comp );
			if (comp >= 0) addSetResult( rec, recOther, rslt, idx == idxT, query.selfJoin );
		}
		long afterValidateTime = System.currentTimeMillis();
		
		candTokenTime += afterCandTokenTime - startTime;
		filteringTime += afterFilteringTime - afterCandTokenTime;
		validateTime += afterValidateTime - afterFilteringTime;
	}

	@Override
	public String getName() {
		return "JoinPkduckSet";
	}

	@Override
	public String getVersion() {
		/*
		 * 1.0: initial version, transform s and compare to t
		 * 1.01: transform s or t and compare to the other
		 * 1.02: optimized rule compression
		 * 1.03: support token frequency order
		 * 1.04: optimization, bug fix in RC when using FF
		 * 1.05: checkpoint
		 * 1.06: reduce memory usage
		 * 1.07: fix length filter
		 * 1.08: ignore records with too many transformations
		 * 1.09: enable the option for length filter
		 */
		return "1.09";
	}
}
