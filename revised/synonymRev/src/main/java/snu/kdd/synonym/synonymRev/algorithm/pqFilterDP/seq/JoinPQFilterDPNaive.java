package snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.seq;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.ParseException;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.algorithm.misc.SampleDataTest;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.order.FrequencyFirstOrder;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.QGramComparator;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StaticFunctions;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.tools.WYK_HashSet;
import snu.kdd.synonym.synonymRev.validator.TopDown;
import snu.kdd.synonym.synonymRev.validator.TopDownOneSide;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class JoinPQFilterDPNaive extends JoinPQFilterDP {

	public AbstractPQFilterIndex idx;
	public int indexK;
	public int qgramSize;
	protected String indexOpt;

	protected Boolean useLF;
	protected Validator checker;
	protected long candPQGramTime = 0;
	protected long filteringTime = 0;
	protected long dpTime = 0;
	protected long validateTime = 0;
	protected long checkTPQ = 0;

//	protected Int2ObjectOpenHashMap<WYK_HashMap<Integer, WYK_HashSet<QGram>>> mapToken2qgram = null;


	// staticitics used for building indexes
	double avgTransformed;

	public JoinPQFilterDPNaive( Query query, StatContainer stat ) throws IOException {
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
		if ( indexOpt.equals( "FF" ) ) globalOrder.initializeForSequence( query, false );
	}

	// TODO: refactor; move run(), runAfter...(), ... to the parent.
	@Override
	public void run( Query query, String[] args ) throws IOException, ParseException {
//		Param params = Param.parseArgs( args, stat, query );
		
		indexK = params.indexK;
		qgramSize = params.qgramSize;
		useLF = params.useLF;
		useTopDown = params.useTopDown;
		indexOpt = params.indexOpt;
		globalOrder = new FrequencyFirstOrder( qgramSize );

//		indexPosition = new int[ indexK ];
//		for( int i = 0; i < indexK; i++ ) {
//			indexPosition[ i ] = i;
//		}

		if( query.oneSideJoin ) checker = new TopDownOneSide();
		else checker = new TopDown(); 

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
		if ( DEBUG.bIndexWriteToFile ) idx.writeToFile();

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
		ObjectOpenHashSet <IntegerPair> rslt = new ObjectOpenHashSet<IntegerPair>();
		
		for ( int sid=0; sid<query.searchedSet.size(); sid++ ) {
			if ( !query.oneSideJoin ) {
				throw new RuntimeException("UNIMPLEMENTED CASE");
			}

			final Record recS = query.searchedSet.getRecord( sid );
			joinOneRecord( recS, rslt );
		}
		
		if ( addStat ) {
			stat.add( "Result_3_3_CandPQGramTime", candPQGramTime );
			stat.add( "Result_3_4_DpTime", dpTime/1e6 );
			stat.add( "Result_3_5_FilteringTime", filteringTime );
			stat.add( "Result_3_6_ValidateTime", validateTime );
			stat.add( "Result_3_7_checkTPQ", checkTPQ );
		}
		return rslt;
	}

	protected void buildIndex( boolean writeResult ) {
//		idx = new JoinMHIndex( indexK, qgramSize, query.indexedSet.get(), query, stat, indexPosition, writeResult, true, 0 );
	}
	
	protected void joinOneRecord( Record recS, Set<IntegerPair> rslt ) {
		long startTime = System.currentTimeMillis();
		// Enumerate candidate pos-qgrams of recS.
		Int2ObjectOpenHashMap<ObjectOpenHashSet<QGram>> candidatePQGrams = getCandidatePQGrams( recS );
		long afterCandidateTime = System.currentTimeMillis();

		// prepare filtering
		AbstractPosQGramFilterDP filter;
		if ( useTopDown ) filter = new PosQGramFilterDPTopDown( recS, qgramSize );
		else filter = new PosQGramFilterDP(recS, qgramSize);
		Object2IntOpenHashMap<Record> candidatesCount = new Object2IntOpenHashMap<Record>();
		candidatesCount.defaultReturnValue(-1);
		int[] range = recS.getTransLengths();
		
		boolean debug = false;
//		if (recS.getID() == 19964) debug = true;
		if (debug) SampleDataTest.inspect_record( recS, query, qgramSize );

		// Scan the index and verify candidate record pairs.
		for ( int pos : idx.getPosSet() ) {
			if (debug) System.out.println( "pos: "+pos );
			if ( !candidatePQGrams.containsKey( pos ) ) continue;
			for ( QGram qgram : candidatePQGrams.get( pos ) ) {
				if ( !idx.get( pos ).containsKey( qgram ) ) continue;
				checkTPQ++;
				long startDPTime = System.nanoTime();
				Boolean isInTPQ = ((NaiveDP)filter).existence( qgram, pos );
				dpTime += System.nanoTime() - startDPTime;
				if (debug) System.out.println( qgram.toString()+", "+pos+": "+isInTPQ );
				if (isInTPQ) {
					for ( Record recT : idx.get( pos ).get( qgram ) ) {
						// length filtering
						if ( useLF ) {
							int[] otherRange = new int[2];
							if ( query.oneSideJoin ) {
								otherRange[0] = otherRange[1] = recT.getTokenCount();
							}
							else throw new RuntimeException("oneSideJoin is supported only.");
							if (!StaticFunctions.overlap(otherRange[0], otherRange[1], range[0], range[1])) {
								++checker.lengthFiltered;
								continue;
							}
						}
						
						// count the number of appearance of recT in the index.
						int candCount = candidatesCount.getInt( recT );
						if (candCount == -1) candidatesCount.put( recT, 1 );
						else candidatesCount.put( recT, candCount+1 );
					}
				}
			}
		}
		
		Set<Record> candidatesAfterDP = new ObjectOpenHashSet<Record>();
		for (Record recT : candidatesCount.keySet()) {
			if ( idx.getIndexedCount( recT ) <= candidatesCount.getInt( recT ) ) candidatesAfterDP.add( recT );
			else ++checker.pqgramFiltered;
		}
		long afterFilteringTime = System.currentTimeMillis();

		for ( Record recT : candidatesAfterDP ) {
			int comp = checker.isEqual( recS, recT );
			if (comp >= 0) addSeqResult( recS, recT, rslt, query.selfJoin );
		}

		long afterValidateTime = System.currentTimeMillis();
		
		candPQGramTime += afterCandidateTime - startTime;
		filteringTime += afterFilteringTime - afterCandidateTime;
		validateTime += afterValidateTime - afterFilteringTime;
	}
	
	protected Int2ObjectOpenHashMap<ObjectOpenHashSet<QGram>> getCandidatePQGrams(Record rec) {
		// Since the algorithm is "Naive", the input record is not used.
		Int2ObjectOpenHashMap<ObjectOpenHashSet<QGram>> candidatePQGrams = new Int2ObjectOpenHashMap<ObjectOpenHashSet<QGram>>();
		for ( int pos : idx.getPosSet() ) {
			candidatePQGrams.put( pos, new ObjectOpenHashSet<QGram>() );
			for (QGram qgram : idx.get( pos ).keySet()) {
				candidatePQGrams.get( pos ).add( qgram );
			}
		}
		return candidatePQGrams;
	}
	
	// used in dp1 and dp3
//	protected void buildMapToken2qgram() {
//		/*
//		 * Build a map from a position p and a token t to a set of pos qgrams containing t and whose position is p.
//		 * In fact, the structure is implemented using two nested maps: p -> t -> set of qgrams
//		 */
//		mapToken2qgram = new Int2ObjectOpenHashMap<>();
//		for ( int pos : idx.getPosSet() ) {
//			WYK_HashMap<Integer, WYK_HashSet<QGram>> map = new WYK_HashMap<Integer, WYK_HashSet<QGram>>();
//			for (QGram qgram : idx.get( pos ).keySet()) {
//				for ( int token : qgram.qgram ) {
//					if ( map.get( token ) == null ) map.put(token, new WYK_HashSet<QGram>());
//					map.get( token ).add( qgram );
//				}
//			}
//			mapToken2qgram.put( pos, map );
//		}
//	}

	// used in dp2 and dp3
}
