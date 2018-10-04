package vldb17.seq;

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.cli.ParseException;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate;
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
import snu.kdd.synonym.synonymRev.tools.StaticFunctions;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.validator.NaiveOneSide;
import snu.kdd.synonym.synonymRev.validator.TopDownOneSide;
import snu.kdd.synonym.synonymRev.validator.Validator;
import vldb17.GreedyValidator;
import vldb17.ParamPkduck;
import vldb17.set.PkduckSetDP;
import vldb17.set.PkduckSetDPWithRC;
import vldb17.set.PkduckSetIndex;

public class JoinPkduck extends AlgorithmTemplate {

	private PkduckSetIndex idx = null;
//	private long threshold = Long.MAX_VALUE;
//	private final int qgramSize = 1; // a string is represented as a set of (token, pos) pairs.
	AbstractGlobalOrder globalOrder;
	private Boolean useRuleComp;
	private Validator checker;

	// staticitics used for building indexes
	double avgTransformed;
	
	private long candTokenTime = 0;
	private long isInSigUTime = 0;
	private long validateTime = 0;
	private long nScanList = 0;
	private long nRunDP = 0;
	private boolean useLF;
	
	public JoinPkduck( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}

	public void preprocess() {
		super.preprocess();
		
		for (Record rec : query.searchedSet.get()) {
			rec.preprocessSuffixApplicableRules();
		}
		globalOrder.initializeForSequence( query, true );

//		double estTransformed = 0.0;
//		for( Record rec : query.indexedSet.get() ) {
//			estTransformed += rec.getEstNumTransformed();
//		}
//		avgTransformed = estTransformed / query.indexedSet.size();
	}

	@Override
	public void run( Query query, String[] args ) throws IOException, ParseException {
		ParamPkduck params = ParamPkduck.parseArgs( args, stat, query );
		Ordering mode = Ordering.valueOf( params.globalOrder );
		switch(mode) {
		case PF: globalOrder = new PositionFirstOrder( 1 ); break;
		case FF: globalOrder = new FrequencyFirstOrder( 1 ); break;
		default: throw new RuntimeException("Unexpected error");
		}
		useRuleComp = params.useRuleComp;
		if (params.verifier.equals( "naive" )) checker = new NaiveOneSide();
		else if (params.verifier.equals( "greedy" )) checker = new GreedyValidator( query.oneSideJoin );
		else if (params.verifier.equals( "TD" )) checker = new TopDownOneSide();
		else throw new RuntimeException(getName()+" does not support verification: "+params.verifier);
		useLF = params.useLF;
//		this.threshold = -1;

		StopWatch stepTime = StopWatch.getWatchStarted( "Result_2_Preprocess_Total_Time" );

		preprocess();

		stepTime.stopAndAdd( stat );
		stat.addMemory( "Mem_2_Preprocessed" );
		stepTime.resetAndStart( "Result_3_Run_Time" );

		rslt = runAfterPreprocess( true );

		stepTime.stopAndAdd( stat );
		stepTime.resetAndStart( "Result_4_Write_Time" );

		this.writeResult();

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

		buildIndex( addStat );
//		if ( DEBUG.bIndexWriteToFile ) idx.writeToFile();

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
		final Set<IntegerPair> rslt = join( stat, query, true );

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
		idx = new PkduckSetIndex( query.indexedSet.recordList, query, stat, globalOrder, addStat );
	}
	
	public Set<IntegerPair> join(StatContainer stat, Query query, boolean addStat) {
		ObjectOpenHashSet <IntegerPair> rslt = new ObjectOpenHashSet<IntegerPair>();
		
		for ( int sid=0; sid<query.searchedSet.size(); sid++ ) {
			if ( !query.oneSideJoin ) {
				throw new RuntimeException("UNIMPLEMENTED CASE");
			}

			final Record recS = query.searchedSet.getRecord( sid );
			if ( recS.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) continue;
			joinOneRecord( recS, rslt );
		}
		
		if ( addStat ) {
			stat.add( "Result_3_3_CandTokenTime", candTokenTime );
			stat.add( "Result_3_4_IsInSigUTime", isInSigUTime/1e6);
			stat.add( "Result_3_5_ValidateTime", validateTime/1e6 );
			stat.add( "Result_3_6_nScanList", nScanList );
			stat.add( "Result_3_7_nRunDP", nRunDP );
		}
		return rslt;
	}

	private void joinOneRecord( Record recS, Set<IntegerPair> rslt ) {
		long startTime = System.currentTimeMillis();
		final int[][] transLen = recS.getTransLengthsAll();
		Int2ObjectOpenHashMap<IntOpenHashSet> candidateTokens = new Int2ObjectOpenHashMap<IntOpenHashSet>();
		for (int i=0; i<recS.size(); i++) {
			for (Rule rule : recS.getSuffixApplicableRules( i )) {
				int[] rhs = rule.getRight();
				int prefLen = i - rule.leftSize(); // pos means the prefix length of the rule is s[0:pos+1). pos can be -1.
				for (int j=0; j<rule.rightSize(); j++) {
					// rhs[j] can have a position from recS[0:pos+1).transLength.min+j ~ max+j (both side inclusive).
					if ( prefLen < 0 ) {
						if ( !candidateTokens.containsKey( j ) ) candidateTokens.put( j, new IntOpenHashSet() );
						candidateTokens.get( j ).add( rhs[j] );
					}
					else {
						for ( int pos=transLen[prefLen][0]+j; pos<=transLen[prefLen][1]+j; pos++ ) {
							if ( !candidateTokens.containsKey( pos ) ) candidateTokens.put( pos, new IntOpenHashSet() );
							candidateTokens.get( pos ).add( rhs[j] );
						}
						
					}
				}
			}
		}
		this.candTokenTime += (System.currentTimeMillis() - startTime);
		
//		Boolean debug = false;
//		if ( recS.getID() == 11487 ) debug = true;
//		if (debug) {
//			System.out.println( recS.getID() );
//			System.out.println( candidateTokens.toString() );
//			System.out.println( idx.keySet().toString() );
//
//			for (int i=0; i<recS.size(); i++) {
//				System.out.println( "pos: "+i );
//				for (Rule rule : recS.getSuffixApplicableRules( i )) {
//					System.out.println( rule );
//				}
//			}
//		}
//		if (debug) SampleDataTest.inspect_record( recS, query, 1 );

		int[] range = recS.getTransLengths();
		PkduckSetDP pkduckDP;
		if (useRuleComp) pkduckDP = new PkduckSetDPWithRC( recS, globalOrder );
		pkduckDP = new PkduckSetDP( recS, globalOrder );
		for ( Entry<Integer, IntOpenHashSet> entry : candidateTokens.entrySet() ) {
			int pos = entry.getKey();
			if ( !idx.keySet().contains( pos ) ) continue;
			IntOpenHashSet tokenSet = entry.getValue();
			for (int token : tokenSet) {
				long startDpTime = System.nanoTime();
				Boolean isInSigU = pkduckDP.isInSigU( token );
//				Boolean isInSigU = true; // DEBUGgg
				isInSigUTime += System.nanoTime() - startDpTime;
				++nRunDP;
//				if (debug) System.out.println( "["+token+", "+pos+"]: "+isInSigU );
				if ( isInSigU ) {
					List<Record> indexedList = idx.get( token );
					if ( indexedList == null ) continue;
					++nScanList;
					for (Record recT : indexedList) {
						// length filtering
						if ( !useLF || StaticFunctions.overlap(recT.size(), recT.size(), range[0], range[1])) {
							long startValidateTime = System.nanoTime();
							int comp = checker.isEqual( recS, recT );
							validateTime += System.nanoTime() - startValidateTime;
							if (comp >= 0) addSeqResult( recS, recT, rslt, query.selfJoin );
						}
						else ++checker.lengthFiltered;
					}
				}
			}
		}
//		if (debug) System.exit( 1 );
	}

	@Override
	public String getName() {
		return "JoinPkduck";
	}

	@Override
	public String getVersion() {
		/*
		 * 1.00: initial version
		 * 1.01: ?
		 * 1.02: bug fix
		 * 1.03: bug fix
		 * 1.04: optimized rule compression
		 * 1.05: FF based indexing, improved DP, RC
		 * 1.06: reduce memory usage
		 * 1.07: ignore records with too many transformations
		 * 1.08: apply length filter, introduce TD validator
		 * 1.09: use set based filtering
		 */
		return "1.09";
	}
}
