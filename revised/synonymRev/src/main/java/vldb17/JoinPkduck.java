package vldb17;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.ParseException;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
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
import vldb17.PkduckIndex.GlobalOrder;

public class JoinPkduck extends AlgorithmTemplate {

	private PkduckIndex idx = null;
//	private long threshold = Long.MAX_VALUE;
	private final int qgramSize = 1; // a string is represented as a set of (token, pos) pairs.
	private GlobalOrder globalOrder;
	private Validator checker;

	// staticitics used for building indexes
	double avgTransformed;
	
	private long candTokenTime = 0;
	private long validateTime = 0;

	public JoinPkduck( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}

	public void preprocess() {
		super.preprocess();
		
		for (Record rec : query.searchedSet.get()) {
			rec.preprocessSuffixApplicableRules();
		}

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
		if (params.verifier.equals( "naive" )) checker = new NaiveOneSide();
		else if (params.verifier.equals( "greedy" )) checker = new GreedyValidator( true );
//		this.threshold = -1;

		StopWatch stepTime = StopWatch.getWatchStarted( "Result_2_Preprocess_Total_Time" );

		preprocess();

		stepTime.stopAndAdd( stat );
		stat.addMemory( "Mem_2_Preprocessed" );
		stepTime.resetAndStart( "Result_3_Run_Time" );

		final List<IntegerPair> list = runAfterPreprocess( true );

		stepTime.stopAndAdd( stat );
		stepTime.resetAndStart( "Result_4_Write_Time" );

		this.writeResult( list );

		stepTime.stopAndAdd( stat );
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
			stat.add( "ValidateTime", validateTime );
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
		List<QGram> candidateQGrams = new ObjectArrayList<QGram>(100);
		for (int i=0; i<recS.size(); i++) {
			for (Rule rule : recS.getSuffixApplicableRules( i )) {
				for (int j=0; j<rule.rightSize()+1-qgramSize; j++) {
					candidateQGrams.add( new QGram(Arrays.copyOfRange( rule.getRight(), j, j+1 )) );
				}
			}
		}
		long afterCandTokenTime = System.currentTimeMillis();
		
		for (int i=0; i<idx.getIndexRange(); i++) {
			for (QGram qgram : candidateQGrams) {
				List<Record> indexedList = idx.get( i, qgram );
				if ( indexedList == null ) continue;
				for (Record recT : indexedList) {
					int comp = checker.isEqual( recS, recT );
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
		long afterValidateTime = System.currentTimeMillis();

		this.candTokenTime += (afterCandTokenTime - startTime);
		this.validateTime += (afterValidateTime - afterCandTokenTime);
	}
}
