package snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.set;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.ParseException;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.tools.WYK_HashMap;
import snu.kdd.synonym.synonymRev.tools.WYK_HashSet;
import snu.kdd.synonym.synonymRev.validator.Validator;
import vldb17.set.SetGreedyValidator;

public class JoinPQFilterDPSet extends AlgorithmTemplate {

	public WYK_HashMap<Integer, List<Record>> idxS = null;
	public WYK_HashMap<Integer, List<Record>> idxT = null;
	public int indexK;
	public int qgramSize;

	protected Validator checker;
	protected long candTokenTime = 0;
	protected long dpTime = 0;
	protected long filteringTime = 0;
	protected long validateTime = 0;
	protected long nScanList = 0;

	protected WYK_HashMap<Integer, WYK_HashSet<QGram>> mapToken2qgram = null;
	
	private final Boolean useLF = true;


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
			if ( params.verifier.equals( "TD" ) ) checker = new SetTopDownOneSide( query.selfJoin );
			else if ( params.verifier.startsWith( "GR" ) ) checker = new SetGreedyOneSide( query.selfJoin, params.beamWidth );
			else if ( params.verifier.equals( "MIT_GR" ) ) checker = new SetGreedyValidator( query.selfJoin );
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

		final Set<IntegerPair> list = runAfterPreprocess( true );

		stepTime.stopAndAdd( stat );
		stepTime.resetAndStart( "Result_4_Write_Time" );

		this.writeResult( list );

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
		final Set<IntegerPair> rslt = join( stat, query, addStat );

		if( addStat ) {
			stepTime.stopAndAdd( stat );
			stat.addMemory( "Mem_4_Joined" );
		}
		
		return rslt;
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
			stat.add( "Result_3_4_DpTime", dpTime/1e6 );
			stat.add( "Result_3_5_FilteringTime", filteringTime );
			stat.add( "Result_3_6_ValidateTime", validateTime );
			stat.add( "Result_3_7_nScanList", nScanList );
		}
		return rslt;
	}

	protected void buildIndex( boolean writeResult ) {
		idxT = new WYK_HashMap<Integer, List<Record>>();
		for ( Record recT : query.indexedSet.recordList ) {
			for ( int token : recT.getTokensArray() ) {
				if ( idxT.get( token ) == null ) idxT.put( token, new ObjectArrayList<Record>() );
				idxT.get( token ).add( recT );
			}
		}
		
		if ( !query.selfJoin ) {
			idxS = new WYK_HashMap<Integer, List<Record>>();
			for ( Record recS : query.searchedSet.recordList ) {
				for ( int token : recS.getTokensArray() ) {
					if ( idxS.get( token ) == null ) idxS.put( token, new ObjectArrayList<Record>() );
					idxS.get( token ).add( recS );
				}
			}
		}
		
		if (writeResult) {
			try {
				BufferedWriter bw = new BufferedWriter( new FileWriter( "./tmp/PQFilterDPSetIndex_idxT.txt" ) );
				for ( int key : idxT.keySet() ) {
					bw.write( "token: "+query.tokenIndex.getToken( key )+" ("+key+")\n" );
					for ( Record rec : idxT.get( key ) ) bw.write( ""+rec.getID()+", " );
					bw.write( "\n" );
				}
			}
			catch( IOException e ) {
				e.printStackTrace();
				System.exit( 1 );
			}
		}
	}
	
	protected void joinOneRecord( Record rec, Set<IntegerPair> rslt, WYK_HashMap<Integer, List<Record>> idx ) {
		long startTime = System.currentTimeMillis();
		// Enumerate candidate tokens of recS.
		IntOpenHashSet candidateTokens = new IntOpenHashSet();
		for ( int pos=0; pos<rec.size(); pos++ ) {
			for ( Rule rule : rec.getSuffixApplicableRules( pos )) {
				for (int token : rule.getRight() ) candidateTokens.add( token );
			}
		}
		long afterCandidateTime = System.currentTimeMillis();

		// Scan the index and verify candidate record pairs.
		Set<Record> candidateAfterLF = new ObjectOpenHashSet<Record>();
		int rec_maxlen = rec.getMaxTransLength();
		for ( int token : candidateTokens ) {
			if ( !idx.containsKey( token ) ) continue;
			nScanList++;
			for ( Record recOther : idx.get( token ) ) {
				if ( useLF ) {
					if ( rec_maxlen < recOther.size() ) {
						++checker.filtered;
						continue;
					}
					candidateAfterLF.add( recOther );
				}
			}
		}
		long afterFilteringTime = System.currentTimeMillis();
		
		// verification
		for ( Record recOther : candidateAfterLF ) {
			if ( checker.isEqual( rec, recOther ) >= 0 ) {
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
		/*
		 * 1.0: initial version, transform s and compare to t
		 * 1.01: transform s or t and compare to the other
		 */
		return "1.01";
	}
}
