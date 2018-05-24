package vldb17.set;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.ParseException;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.synonymRev.algorithm.misc.SampleDataTest;
import snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.set.SetNaiveOneSide;
import snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.set.SetTopDownOneSide;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.order.AbstractGlobalOrder;
import snu.kdd.synonym.synonymRev.order.AbstractGlobalOrder.Ordering;
import snu.kdd.synonym.synonymRev.order.FrequencyFirstOrder;
import snu.kdd.synonym.synonymRev.order.PositionFirstOrder;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.validator.Validator;
import vldb17.ParamPkduck;


public class JoinPkduckSet extends AlgorithmTemplate {

	private PkduckSetIndex idxS = null;
	private PkduckSetIndex idxT = null;
	private final int qgramSize = 1; // a string is represented as a set of (token, pos) pairs.
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

		globalOrder.initializeForSet( query );
		
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
		Ordering mode = Ordering.valueOf( params.globalOrder );
		switch(mode) {
		case PF: globalOrder = new PositionFirstOrder( 1 ); break;
		case FF: globalOrder = new FrequencyFirstOrder( 1 ); break;
		default: throw new RuntimeException("Unexpected error");
		}
		useRuleComp = params.useRuleComp;
		if (params.verifier.equals( "naive" )) checker = new SetNaiveOneSide( query.selfJoin );
		else if (params.verifier.equals( "greedy" )) checker = new SetGreedyValidator( query.selfJoin );
		else if (params.verifier.equals( "TD" )) checker = new SetTopDownOneSide( query.selfJoin );
		else throw new RuntimeException(getName()+" does not support verification: "+params.verifier);
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

	private void joinOneRecord( Record rec, Set<IntegerPair> rslt, PkduckSetIndex idx ) {
		long startTime = System.currentTimeMillis();
		IntOpenHashSet candidateTokens = new IntOpenHashSet();
		for (int i=0; i<rec.size(); i++) {
			for (Rule rule : rec.getSuffixApplicableRules( i )) {
				int[] rhs = rule.getRight();
				for (int j=0; j<rule.rightSize()+1-qgramSize; j++) {
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
		if (useRuleComp) pkduckSetDP = new PkduckSetDPWithRC( rec, globalOrder );
		else pkduckSetDP = new PkduckSetDP( rec, globalOrder );
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
		 */
		return "1.05";
	}
}
