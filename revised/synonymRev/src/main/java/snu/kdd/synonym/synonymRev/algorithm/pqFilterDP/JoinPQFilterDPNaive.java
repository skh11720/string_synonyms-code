package snu.kdd.synonym.synonymRev.algorithm.pqFilterDP;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.ParseException;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.index.JoinMHIndex;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.QGramComparator;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.tools.WYK_HashMap;
import snu.kdd.synonym.synonymRev.tools.WYK_HashSet;
import snu.kdd.synonym.synonymRev.validator.TopDown;
import snu.kdd.synonym.synonymRev.validator.TopDownOneSide;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class JoinPQFilterDPNaive extends JoinPQFilterDP {

	public JoinMHIndex idx;
	public int indexK = 3;
	public int qgramSize = 2;

	protected Validator checker;
	protected long candPQGramTime = 0;
	protected long filteringTime = 0;
	protected long dpTime = 0;
	protected long validateTime = 0;
	protected long nScanList = 0;

	protected ObjectArrayList<WYK_HashMap<Integer, WYK_HashSet<QGram>>> mapToken2qgram = null;


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
	}

	@Override
	public void run( Query query, String[] args ) throws IOException, ParseException {
//		Param params = Param.parseArgs( args, stat, query );
		
		indexK = params.indexK;
		qgramSize = params.qgramSize;
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
			stat.add( "CandPQGramTime", candPQGramTime );
			stat.add( "DpTime", dpTime/1e6 );
			stat.add( "FilteringTime", filteringTime );
			stat.add( "ValidateTime", validateTime );
			stat.add( "nScanList", nScanList );
		}
		return rslt;
	}

	protected void buildIndex( boolean writeResult ) {
		int[] indexPosition = new int[ indexK ];
		for( int i = 0; i < indexK; i++ ) {
			indexPosition[ i ] = i;
		}
		idx = new JoinMHIndex( indexK, qgramSize, query.indexedSet.get(), query, stat, indexPosition, writeResult, true, 0 );
	}
	
	protected void joinOneRecord( Record recS, List<IntegerPair> rslt ) {
		long startTime = System.currentTimeMillis();
		// Enumerate candidate pos-qgrams of recS.
		ObjectArrayList<WYK_HashSet<QGram>> candidatePQGrams = getCandidatePQGrams( recS );
		long afterCandidateTime = System.currentTimeMillis();

		// prepare filtering
		PosQGramFilterDP filter = new PosQGramFilterDP(recS, qgramSize);
		Object2IntOpenHashMap<Record> candidatesCount = new Object2IntOpenHashMap<Record>();
		candidatesCount.defaultReturnValue(-1);

		// Scan the index and verify candidate record pairs.
		for ( int pos=0; pos<indexK; pos++ ) {
			for ( QGram qgram : candidatePQGrams.get( pos ) ) {
				nScanList++;
				long startDPTime = System.nanoTime();
				Boolean isInTPQ = filter.existence( qgram, pos );
				dpTime += System.nanoTime() - startDPTime;
				if (isInTPQ) {
					for ( Record recT : idx.get( pos ).get( qgram ) ) {
						// TODO: length filtering
						
						int candCount = candidatesCount.getInt( recT );
						if (candCount == -1) candidatesCount.put( recT, 1 );
						else candidatesCount.put( recT, candCount+1 );
					}
				}
			}
		}
		
		Set<Record> candidatesAfterDP = new WYK_HashSet<Record>(100);
		for (Record recT : candidatesCount.keySet()) {
			if ( idx.indexedCountList.getInt( recT ) <= candidatesCount.getInt( recT ) ) candidatesAfterDP.add( recT );
		}
		long afterFilteringTime = System.currentTimeMillis();

		for ( Record recT : candidatesAfterDP ) {
			if ( checker.isEqual( recS, recT ) >= 0 ) 
				rslt.add( new IntegerPair( recS.getID(), recT.getID()) );
		}

		long afterValidateTime = System.currentTimeMillis();
		
		candPQGramTime += afterCandidateTime - startTime;
		filteringTime += afterFilteringTime - afterCandidateTime;
		validateTime += afterValidateTime - afterFilteringTime;
	}
	
	protected ObjectArrayList<WYK_HashSet<QGram>> getCandidatePQGrams(Record rec) {
		ObjectArrayList<WYK_HashSet<QGram>> candidatePQGrams = new ObjectArrayList<WYK_HashSet<QGram>>();
		for ( int pos=0; pos<indexK; pos++ ) {
			candidatePQGrams.add( new WYK_HashSet<QGram>(100) );
			for (QGram qgram : idx.get( pos ).keySet()) {
				candidatePQGrams.get( pos ).add( qgram );
			}
		}
		return candidatePQGrams;
	}
	
	// used in dp1 and dp3
	protected void buildMapToken2qgram() {
		mapToken2qgram = new ObjectArrayList<>();
		for ( int pos=0; pos<indexK; pos++ ) {
			WYK_HashMap<Integer, WYK_HashSet<QGram>> map = new WYK_HashMap<Integer, WYK_HashSet<QGram>>();
			for (QGram qgram : idx.get( pos ).keySet()) {
				for ( int token : qgram.qgram ) {
					if ( map.get( token ) == null ) map.put(token, new WYK_HashSet<QGram>());
					map.get( token ).add( qgram );
				}
			}
			mapToken2qgram.add( map );
		}
	}

	// used in dp2 and dp3
	protected ObjectArrayList<IntegerPair> getQGramPrefixList(Set<QGram> qgramSet) {
		ObjectArrayList<IntegerPair> qgramPrefixList = new ObjectArrayList<IntegerPair>();
		List<QGram> keyList = new ObjectArrayList<QGram>( qgramSet );
		keyList.sort( new QGramComparator() );
		int d = 1;
		QGram qgramPrev = null;
		for (QGram qgram : keyList ) {
			if ( qgramPrev != null ) {
				--d;
				while ( d > 1 ) {
					if ( qgram.qgram[d-2] != qgramPrev.qgram[d-2] ) --d;
					else break;
				}
			}
			for (; d<=qgramSize; d++) {
				qgramPrefixList.add(new IntegerPair( qgram.qgram[d-1], d ));
//					System.out.println( new IntegerPair( qgram.qgram[d-1], d) );
			}
			qgramPrev = qgram;
		}
		return qgramPrefixList;
	}
}
