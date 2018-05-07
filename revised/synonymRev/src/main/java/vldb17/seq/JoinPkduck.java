package vldb17.seq;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.ParseException;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.validator.NaiveOneSide;
import snu.kdd.synonym.synonymRev.validator.Validator;
import vldb17.GreedyValidator;
import vldb17.ParamPkduck;
import vldb17.ParamPkduck.GlobalOrder;

public class JoinPkduck extends AlgorithmTemplate {

	private PkduckIndex idx = null;
//	private long threshold = Long.MAX_VALUE;
	private final int qgramSize = 1; // a string is represented as a set of (token, pos) pairs.
	GlobalOrder globalOrder;
	private Boolean useRuleComp;
	private Validator checker;

	// staticitics used for building indexes
	double avgTransformed;
	
	private long candTokenTime = 0;
	private long isInSigUTime = 0;
	private long validateTime = 0;
	int len_max_S;

	public JoinPkduck( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}

	public void preprocess() {
		super.preprocess();
		
		for (Record rec : query.searchedSet.get()) {
			rec.preprocessSuffixApplicableRules();
		}

		// find the maximum length of records in S.
		for (Record rec : query.searchedSet.recordList) len_max_S = Math.max( len_max_S, rec.size() );
		
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
		globalOrder = params.globalOrder;
		useRuleComp = params.useRuleComp;
		if (params.verifier.equals( "naive" )) checker = new NaiveOneSide();
		else if (params.verifier.equals( "greedy" )) checker = new GreedyValidator( true, false );
//		this.threshold = -1;

		StopWatch stepTime = StopWatch.getWatchStarted( "Result_2_Preprocess_Total_Time" );

		preprocess();

		stepTime.stopAndAdd( stat );
		stat.addMemory( "Mem_2_Preprocessed" );
		stepTime.resetAndStart( "Result_3_Run_Time" );

		final List<IntegerPair> rslt = runAfterPreprocess( true );

		stepTime.stopAndAdd( stat );
		stepTime.resetAndStart( "Result_4_Write_Time" );

		this.writeResult( rslt );

		stepTime.stopAndAdd( stat );
		checker.addStat( stat );
	}

	public List<IntegerPair> runAfterPreprocess( boolean addStat ) {
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
		final List<IntegerPair> rslt = join( stat, query, true );

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
		idx = new PkduckIndex( query, stat, globalOrder, addStat );
	}
	
	public List<IntegerPair> join(StatContainer stat, Query query, boolean addStat) {
		ObjectArrayList<IntegerPair> rslt = new ObjectArrayList<IntegerPair>();
		
		for ( int sid=0; sid<query.searchedSet.size(); sid++ ) {
			if ( !query.oneSideJoin ) {
				throw new RuntimeException("UNIMPLEMENTED CASE");
			}

			final Record recS = query.searchedSet.getRecord( sid );
			joinOneRecord( recS, rslt );
		}
		
		if ( addStat ) {
			stat.add( "CandTokenTime", candTokenTime );
			stat.add( "IsInSigUTime", isInSigUTime/1e6);
			stat.add( "ValidateTime", validateTime/1e6 );
		}
		return rslt;
	}

	@Override
	public String getName() {
		return "JoinPkduck";
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

	private void joinOneRecord( Record recS, List<IntegerPair> rslt ) {
		long startTime = System.currentTimeMillis();
		Set<QGram> candidateQGrams = new ObjectOpenHashSet<QGram>(100);
		for (int i=0; i<recS.size(); i++) {
			for (Rule rule : recS.getSuffixApplicableRules( i )) {
				for (int j=0; j<rule.rightSize()+1-qgramSize; j++) {
					candidateQGrams.add( new QGram(Arrays.copyOfRange( rule.getRight(), j, j+1 )) );
				}
			}
		}
		this.candTokenTime += (System.currentTimeMillis() - startTime);
		
		PkduckDP pkduckDP;
		if (useRuleComp) pkduckDP = new PkduckDPWithRC( recS, this );
		else pkduckDP = new PkduckDP( recS, this );
		for (int pos : idx.keySet() ) {
			for (QGram qgram : candidateQGrams) {
				long startDpTime = System.nanoTime();
				Boolean isInSigU = pkduckDP.isInSigU( recS, qgram, pos );
				isInSigUTime += System.nanoTime() - startDpTime;
				if ( isInSigU ) {
					List<Record> indexedList = idx.get( pos, qgram );
					if ( indexedList == null ) continue;
					for (Record recT : indexedList) {
						long startValidateTime = System.nanoTime();
						int comp = checker.isEqual( recS, recT );
						validateTime += System.nanoTime() - startValidateTime;
						if (comp >= 0) {
	//						System.out.println( recS+", "+recT );
	//						List<Record> expList = recS.expandAll();
	//						for (Record exp : expList) {
	//							System.out.println( exp );
	//						}
	//						System.out.println(  );
							rslt.add( new IntegerPair(recS.getID(), recT.getID()) );
						}
					}
				}
			}
		}

	}
}
