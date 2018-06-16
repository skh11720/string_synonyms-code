package snu.kdd.synonym.synonymRev.algorithm.delta;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.cli.ParseException;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.index.NaiveIndex;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.Param;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.tools.Util;

public class JoinNaiveDelta extends AlgorithmTemplate{
	
	private NaiveIndex index;
	private Int2ObjectOpenHashMap<List<Record>> token2rec;
	private Object2IntOpenHashMap<Record> counter;
	private int delta;
	private DeltaValidator checker;

	private long countTime = 0;
	private long candidateTime =0;
	private long validateTime = 0;

	public JoinNaiveDelta( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
		counter = new Object2IntOpenHashMap<Record>();
		counter.defaultReturnValue( 0 );
	}

	@Override
	public void run( Query query, String[] args ) throws IOException, ParseException {
		Param param = Param.parseArgs( args, stat, query );
		this.delta = param.delta;
		checker = new DeltaValidator( delta );

		StopWatch stepTime = StopWatch.getWatchStarted( "Result_2_Preprocess_Total_Time" );
		stat.add( "cmd_delta", delta );

		preprocess();

		stepTime.stopAndAdd( stat );
		stat.addMemory( "Mem_2_Preprocessed" );
		stepTime.resetAndStart( "Result_3_Run_Time" );

		final Set<IntegerPair> list = runAfterPreprocess();

		stepTime.stopAndAdd( stat );
		stepTime.resetAndStart( "Result_4_Write_Time" );

		this.writeResult( list );

		stepTime.stopAndAdd( stat );
		checker.addStat( stat );
	}
	
	private Set<IntegerPair> runAfterPreprocess() {
		StopWatch stepTime = null;
		stepTime = StopWatch.getWatchStarted( "Result_3_1_Index_Building_Time" );
		buildIndex( writeResult );

		stat.addMemory( "Mem_3_BuildIndex" );
		stepTime.stopAndAdd( stat );
		stepTime.resetAndStart( "Result_3_2_Join_Time" );

		Set<IntegerPair> rslt = join( stat, query, writeResult );

		stat.addMemory( "Mem_4_Joined" );
		stepTime.stopAndAdd( stat );
		return rslt;
	}
	
	private void buildIndex( boolean addStat ) {
		long avgTransformed = 0;
		for( Record rec : query.indexedSet.get() ) {
			avgTransformed += rec.getEstNumTransformed();
		}
		avgTransformed /= query.indexedSet.size();
		index = NaiveIndex.buildIndex( avgTransformed, stat, -1, addStat, query );
		token2rec = new Int2ObjectOpenHashMap<List<Record>>();
		for ( Record rec : index.keySet() ) {
			for ( int token : rec.getTokens() ) {
				if ( !token2rec.containsKey( token ) )  token2rec.put( token, new ObjectArrayList<Record>() );
				token2rec.get( token ).add( rec );
			}
		}
	}
	
	private Set<IntegerPair> join( StatContainer stat, Query query, boolean addStat ) {
		Set<IntegerPair> rslt = new ObjectOpenHashSet<IntegerPair>();
		
//		for ( Record recS : query.searchedSet.recordList ) System.out.println( "s "+recS.getID()+": "+recS );
//		for ( Record recT : query.indexedSet.recordList ) System.out.println( "t "+recT.getID()+": "+recT );

		for ( int sid=0; sid<query.searchedSet.size(); sid++ ) {
			if ( !query.oneSideJoin ) {
				throw new RuntimeException("UNIMPLEMENTED CASE");
			}

			final Record recS = query.searchedSet.getRecord( sid );
			joinOneRecord( recS, rslt );
		}
		
		if ( addStat ) {
			stat.add( "Result_3_2_CountTime", countTime );
			stat.add( "Result_3_3_CandidateTime", candidateTime );
			stat.add( "Result_3_4_ValidateTime", validateTime );
		}
		return rslt;
	}
	
	private void joinOneRecord( Record recS, Set<IntegerPair> rslt ) {
		
		for ( Record expS: recS.expandAll() ) {
			counter.clear();
			long ts = System.currentTimeMillis();
			int len_expS = expS.size();
			for ( int token : expS.getTokens() ) {
				if ( ! token2rec.containsKey( token ) ) continue;
				for ( Record recT: token2rec.get( token ) ) counter.addTo( recT, 1 );
			}
			long afterCountTime = System.currentTimeMillis();
			
			Set<Record> candidates = new ObjectOpenHashSet<Record>();
			for ( Entry<Record, Integer> entry : counter.entrySet() ) {
				Record recT = entry.getKey();
				if ( entry.getValue() >= (len_expS + recT.size() - delta)/2 ) candidates.add(recT);
			}
			long afterCandidateTime = System.currentTimeMillis();
//			for ( Record recT : candidates ) System.out.println( "recT "+recT.getID()+": "+recT );
			
			for ( Record recT : candidates ) {
				++checker.checked;
				if ( len_expS + recT.size() - 2*Util.lcs( expS.getTokensArray(), recT.getTokensArray() ) <= delta ) {
					for ( int id : index.getValue( recT ) ) addSeqResult( recS, id, rslt, query.selfJoin );
				}
			}
			long afterValidateTime = System.currentTimeMillis();
				
			countTime += afterCountTime - ts;
			candidateTime += afterCandidateTime - afterCountTime;
			validateTime += afterValidateTime - afterCandidateTime;
		}
	}

	@Override
	public String getName() {
		return "JoinNaiveDelta";
	}

	@Override
	public String getVersion() {
		return "1.00";
	}
}
