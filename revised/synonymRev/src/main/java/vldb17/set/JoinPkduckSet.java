package vldb17.set;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.ParseException;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.set.SetNaiveOneSide;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.order.QGramGlobalOrder;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.validator.Validator;
import vldb17.ParamPkduck;


public class JoinPkduckSet extends AlgorithmTemplate {

	private PkduckSetIndex idxS = null;
	private PkduckSetIndex idxT = null;
	private final int qgramSize = 1; // a string is represented as a set of (token, pos) pairs.
	QGramGlobalOrder globalOrder;
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

	private final Boolean useLF = true;

	public JoinPkduckSet( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}

	public void preprocess() {
		super.preprocess();
		
		for (Record rec : query.indexedSet.get()) {
			rec.preprocessSuffixApplicableRules();
		}
		
		if ( !query.selfJoin ) {
			for ( Record rec : query.searchedSet.get()) {
				rec.preprocessSuffixApplicableRules();
			}
		}

		globalOrder.initOrder( query );
		
//		double estTransformed = 0.0;
//		for( Record rec : query.indexedSet.get() ) {
//			estTransformed += rec.getEstNumTransformed();
//		}
//		avgTransformed = estTransformed / query.indexedSet.size();
	}

	@Override
	public void run( Query query, String[] args ) throws IOException, ParseException {
//		this.threshold = Long.valueOf( args[ 0 ] );
		ParamPkduck params = ParamPkduck.parseArgs( args, stat, query );
		globalOrder = new QGramGlobalOrder( params.globalOrder, qgramSize );
		useRuleComp = params.useRuleComp;
		if (params.verifier.equals( "naive" )) checker = new SetNaiveOneSide( query.selfJoin );
		else if (params.verifier.equals( "greedy" )) checker = new SetGreedyValidator( query.selfJoin );
//		this.threshold = -1;

		StopWatch stepTime = StopWatch.getWatchStarted( "Result_2_Preprocess_Total_Time" );

		preprocess();
		
		stepTime.stopAndAdd( stat );
		stat.addMemory( "Mem_2_Preprocessed" );
		stepTime.resetAndStart( "Result_3_Run_Time" );

		final Set<IntegerPair> rslt = runAfterPreprocess( true );

		stepTime.stopAndAdd( stat );
		stepTime.resetAndStart( "Result_4_Write_Time" );

		this.writeResult( rslt );

		stepTime.stopAndAdd( stat );
		checker.addStat( stat );
	}

	public Set<IntegerPair> runAfterPreprocess( boolean addStat ) {
		// Index building
		StopWatch stepTime = null;
		if( addStat ) {
			stepTime = StopWatch.getWatchStarted( "Result_3_1_Index_Building_Time" );
		}
		else {
//			if( DEBUG.SampleStatON ) {
//				stepTime = StopWatch.getWatchStarted( "Sample_1_Naive_Index_Building_Time" );
//			}
			try { throw new Exception("UNIMPLEMENTED CASE"); }
			catch( Exception e ) { e.printStackTrace(); }
		}

		buildIndex( false );

		if( addStat ) {
			stepTime.stopAndAdd( stat );
			stepTime.resetAndStart( "Result_3_2_Join_Time" );
			stat.addMemory( "Mem_3_BuildIndex" );
		}
		else {
			if( DEBUG.SampleStatON ) {
				stepTime.stopAndAdd( stat );
				stepTime.resetAndStart( "Sample_2_Pkduck_Join_Time" );
			}
		}

		// Join
		final Set<IntegerPair> rslt = join( stat, query, addStat );

		if( addStat ) {
			stepTime.stopAndAdd( stat );
			stat.addMemory( "Mem_4_Joined" );
		}
//		else {
//			if( DEBUG.SampleStatON ) {
//				stepTime.stopAndAdd( stat );
//				stat.add( "Stat_Expanded", idx.totalExp );
//			}
//		}
//
//		if( DEBUG.NaiveON ) {
//			if( addStat ) {
//				idx.addStat( stat, "Counter_Join" );
//			}
//		}
//		stat.add( "idx_skipped_counter", idx.skippedCount );

		return rslt;
	}
	
	public void buildIndex(boolean addStat ) {
		idxT = new PkduckSetIndex( query.indexedSet.recordList, query, stat, globalOrder, addStat );
		if ( !query.selfJoin ) idxS = new PkduckSetIndex( query.searchedSet.recordList, query, stat, globalOrder, addStat );
	}
	
	public Set<IntegerPair> join(StatContainer stat, Query query, boolean addStat) {
		ObjectOpenHashSet<IntegerPair> rslt = new ObjectOpenHashSet<IntegerPair>();
		if ( !query.oneSideJoin ) throw new RuntimeException("UNIMPLEMENTED CASE");
		
		// S -> S' ~ T
		for ( Record recS : query.searchedSet.recordList ) {
			joinOneRecord( recS, rslt, idxT );
		}
		
		if ( !query.selfJoin ) {
			// T -> T' ~ S
			for ( Record recT : query.indexedSet.recordList ) {
				joinOneRecord( recT, rslt, idxS );
			}
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
		 */
		return "1.03";
	}

	private void joinOneRecord( Record rec, Set<IntegerPair> rslt, PkduckSetIndex idx ) {
		long startTime = System.currentTimeMillis();
		Set<QGram> candidateQGrams = new ObjectOpenHashSet<QGram>(100);
		for (int i=0; i<rec.size(); i++) {
			for (Rule rule : rec.getSuffixApplicableRules( i )) {
				for (int j=0; j<rule.rightSize()+1-qgramSize; j++) {
					candidateQGrams.add( new QGram(Arrays.copyOfRange( rule.getRight(), j, j+1 )) );
				}
			}
		}
		long afterCandTokenTime = System.currentTimeMillis();
		
		Set<Record> candidateAfterLF = new ObjectOpenHashSet<Record>();
		int rec_maxlen = rec.getMaxTransLength();
		PkduckSetDP pkduckSetDP;
		if (useRuleComp) pkduckSetDP = new PkduckSetDPWithRC( rec, globalOrder );
		else pkduckSetDP = new PkduckSetDP( rec, globalOrder );
//		Boolean debug = false;
//		if ( rec.getID() == 0 ) debug = true;
//		if (debug) SampleDataTest.inspect_record( rec, query, 1 );
		for (QGram qgram : candidateQGrams) {
			long startDPTime = System.nanoTime();
			Boolean isInSigU = pkduckSetDP.isInSigU( qgram );
			++nRunDP;
//			if (debug) System.out.println( ""+qgram+": "+isInSigU );
			isInSigUTime += System.nanoTime() - startDPTime;
			if ( isInSigU ) {
				List<Record> indexedList = idx.get( qgram );
				if ( indexedList == null ) continue;
				++nScanList;
				for (Record recOther : indexedList) {
					if ( useLF ) {
						if ( rec_maxlen < recOther.size() ) {
							++checker.filtered;
							continue;
						}
						candidateAfterLF.add( recOther );
					}
				}
			}
		}
//		if (debug) System.exit( 1 );
		long afterFilteringTime = System.currentTimeMillis();
		
		// verification
		for (Record recOther : candidateAfterLF ) {
			int comp = checker.isEqual( rec, recOther );
			if (comp >= 0) {
				if ( query.selfJoin ) {
					int id_smaller = rec.getID() < recOther.getID()? rec.getID() : recOther.getID();
					int id_larger = rec.getID() >= recOther.getID()? rec.getID() : recOther.getID();
					rslt.add( new IntegerPair( id_smaller, id_larger) );
				}
				else {
					if ( idx == idxT ) rslt.add( new IntegerPair( rec.getID(), recOther.getID()) );
					else if ( idx == idxS ) rslt.add( new IntegerPair( recOther.getID(), rec.getID()) );
					else throw new RuntimeException("Unexpected error");
				}
			}
		}
		long afterValidateTime = System.currentTimeMillis();
		
		candTokenTime += afterCandTokenTime - startTime;
		filteringTime += afterFilteringTime - afterCandTokenTime;
		validateTime += afterValidateTime - afterFilteringTime;
	}
}
