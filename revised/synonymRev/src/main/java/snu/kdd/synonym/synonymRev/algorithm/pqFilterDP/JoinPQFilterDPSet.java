package snu.kdd.synonym.synonymRev.algorithm.pqFilterDP;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.ParseException;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.QGramComparator;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StaticFunctions;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.tools.WYK_HashMap;
import snu.kdd.synonym.synonymRev.tools.WYK_HashSet;
import snu.kdd.synonym.synonymRev.validator.TopDown;
import snu.kdd.synonym.synonymRev.validator.TopDownOneSide;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class JoinPQFilterDPSet extends AlgorithmTemplate {

	public WYK_HashMap<Integer, List<Record>> idx;
	public int indexK;
	public int qgramSize;

	protected Validator checker;
	protected long candTokenTime = 0;
	protected long filteringTime = 0;
	protected long dpTime = 0;
	protected long validateTime = 0;
	protected long nScanList = 0;
	protected long lengthFiltered = 0;

	protected WYK_HashMap<Integer, WYK_HashSet<QGram>> mapToken2qgram = null;
	
	private Boolean useLF = true;


	// staticitics used for building indexes
	double avgTransformed;

	public JoinPQFilterDPSet( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}

	public void preprocess() {
		super.preprocess();

		for( Record rec : query.indexedSet.get() ) {
			rec.preprocessSuffixApplicableRules();
		}
		if( !query.selfJoin ) {
			for( Record rec : query.searchedSet.get() ) {
				rec.preprocessSuffixApplicableRules();
			}
		}
	}

	@Override
	public void run( Query query, String[] args ) throws IOException, ParseException {
		ParamPQFilterDPSet params = ParamPQFilterDPSet.parseArgs( args, stat, query );
		
		if( query.oneSideJoin ) {
			if ( params.verifier.equals( "TD" ) ) checker = new SetTopDownOneSide();
			else if ( params.verifier.equals( "GR" ) ) checker = new SetGreedyOneSide();
			else throw new RuntimeException("Unexpected value for verifier: "+params.verifier);
		}
		else throw new RuntimeException("BothSide is not supported.");

		StopWatch stepTime = StopWatch.getWatchStarted( "Result_2_Preprocess_Total_Time" );

//		if( DEBUG.NaiveON ) {
//			stat.addPrimary( "cmd_threshold", threshold );
//		}

		preprocess();

		stepTime.stopAndAdd( stat );
		stat.addMemory( "Mem_2_Preprocessed" );
		stepTime.resetAndStart( "Result_3_Run_Time" );

		final List<IntegerPair> list = runAfterPreprocess( true );

		stepTime.stopAndAdd( stat );
		stepTime.resetAndStart( "Result_4_Write_Time" );

		this.writeResult( list );

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
			throw new RuntimeException("UNIMPLEMENTED CASE");
		}

		buildIndex( false );

		if( addStat ) {
			stepTime.stopAndAdd( stat );
			stepTime.resetAndStart( "Result_3_2_Join_Time" );
			stat.addMemory( "Mem_3_BuildIndex" );
		}
		else {
//			if( DEBUG.SampleStatON ) {
//				stepTime.stopAndAdd( stat );
//				stepTime.resetAndStart( "Sample_2_Naive_Join_Time" );
//			}
			throw new RuntimeException("UNIMPLEMENTED CASE");
		}

		// Join
		final List<IntegerPair> rslt = join( stat, query, addStat );

		if( addStat ) {
			stepTime.stopAndAdd( stat );
			stat.addMemory( "Mem_4_Joined" );
		}
		
		return rslt;
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
			stat.add( "Result_3_3_CandTokenTime", candTokenTime );
			stat.add( "Result_3_4_DpTime", dpTime/1e6 );
			stat.add( "Result_3_5_FilteringTime", filteringTime );
			stat.add( "Result_3_6_ValidateTime", validateTime );
			stat.add( "Result_3_7_nScanList", nScanList );
		}
		return rslt;
	}

	protected void buildIndex( boolean writeResult ) {
		idx = new WYK_HashMap<Integer, List<Record>>();
		for ( Record recT : query.indexedSet.recordList ) {
			for ( int token : recT.getTokensArray() ) {
				if ( idx.get( token ) == null ) idx.put( token, new ObjectArrayList<Record>() );
				idx.get( token ).add( recT );
			}
		}
	}
	
	protected void joinOneRecord( Record recS, List<IntegerPair> rslt ) {
		long startTime = System.currentTimeMillis();
		// Enumerate candidate tokens of recS.
		IntOpenHashSet candidateTokens = new IntOpenHashSet();
		for ( int pos=0; pos<recS.size(); pos++ ) {
			for ( Rule rule : recS.getSuffixApplicableRules( pos )) {
				for (int token : rule.getRight() ) candidateTokens.add( token );
			}
		}
		long afterCandidateTime = System.currentTimeMillis();

		// Scan the index and verify candidate record pairs.
		Set<Record> candidatesAfterLF = new WYK_HashSet<Record>(100);
		int[] range = recS.getTransLengths();
		for ( int token : candidateTokens ) {
			if ( !idx.containsKey( token ) ) continue;
			nScanList++;
			for ( Record recT : idx.get( token ) ) {
				// length filtering
				if ( useLF ) {
					int[] otherRange = new int[2];
					if ( query.oneSideJoin ) {
						otherRange[0] = otherRange[1] = recT.getTokenCount();
					}
					else throw new RuntimeException("oneSideJoin is supported only.");
					if (!StaticFunctions.overlap(otherRange[0], otherRange[1], range[0], range[1])) {
						lengthFiltered++;
						continue;
					}
					candidatesAfterLF.add( recT );
				}
			}
		}
		long afterFilteringTime = System.currentTimeMillis();

		// verification
		for ( Record recT : candidatesAfterLF ) {
			if ( checker.isEqual( recS, recT ) >= 0 ) 
				rslt.add( new IntegerPair( recS.getID(), recT.getID()) );
		}

		long afterValidateTime = System.currentTimeMillis();
		
		candTokenTime += afterCandidateTime - startTime;
		filteringTime += afterFilteringTime - afterCandidateTime;
		validateTime += afterValidateTime - afterFilteringTime;
	}

	@Override
	public String getName() {
		return "JoinPQFilterDPSet";
	}

	@Override
	public String getVersion() {
		return "1.0";
	}
}
