package snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.set;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.cli.ParseException;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.synonymRev.data.Dataset;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.order.AbstractGlobalOrder;
import snu.kdd.synonym.synonymRev.order.FrequencyFirstOrder;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.tools.WYK_HashMap;
import snu.kdd.synonym.synonymRev.tools.WYK_HashSet;
import snu.kdd.synonym.synonymRev.validator.Validator;
import vldb17.set.SetGreedyValidator;

public class JoinBKPSet extends AlgorithmTemplate {

	public WYK_HashMap<Integer, List<Record>> idxS = null;
	public WYK_HashMap<Integer, List<Record>> idxT = null;
	public Object2IntOpenHashMap<Record> idxCountS = null;
	public Object2IntOpenHashMap<Record> idxCountT = null;
	public int indexK = 0;

	protected Validator checker;
	protected long candTokenTime = 0;
	protected long dpTime = 0;
	protected long CountingTime = 0;
	protected long validateTime = 0;
	protected long nScanList = 0;

	protected long isInSigUTime = 0;
	protected long nRunDP = 0;
	protected boolean useRuleComp;

	protected WYK_HashMap<Integer, WYK_HashSet<QGram>> mapToken2qgram = null;
	
	protected final Boolean useLF = true;

	protected AbstractGlobalOrder globalOrder = new FrequencyFirstOrder( 1 );


	// staticitics used for building indexes
	double avgTransformed;

	public JoinBKPSet( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}

	public void preprocess() {
		super.preprocess();

		for( Record rec : query.indexedSet.get() ) {
			rec.preprocessSuffixApplicableRules();
		}
		idxT = new WYK_HashMap<Integer, List<Record>>();
		idxCountT = new Object2IntOpenHashMap<Record>(query.indexedSet.size());
		if( !query.selfJoin ) {
			for( Record rec : query.searchedSet.get() ) {
				rec.preprocessSuffixApplicableRules();
			}
			idxS = new WYK_HashMap<Integer, List<Record>>();
			idxCountS = new Object2IntOpenHashMap<Record>(query.searchedSet.size());
		}
		globalOrder.initializeForSet( query );
	}

	@Override
	public void run( Query query, String[] args ) throws IOException, ParseException {
		getParams( query, args );
		
		StopWatch stepTime = StopWatch.getWatchStarted( "Result_2_Preprocess_Total_Time" );

//		if( DEBUG.NaiveON ) {
//			stat.addPrimary( "cmd_threshold", threshold );
//		}

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
	
	protected ParamPQFilterDPSet getParams( Query query, String[] args ) throws IOException, ParseException {
		ParamPQFilterDPSet params = ParamPQFilterDPSet.parseArgs( args, stat, query );
		indexK = params.K;
		if( query.oneSideJoin ) {
			if ( params.verifier.equals( "TD" ) ) checker = new SetTopDownOneSide( query.selfJoin );
			else if ( params.verifier.startsWith( "GR" ) ) checker = new SetGreedyOneSide( query.selfJoin, params.beamWidth );
			else if ( params.verifier.equals( "MIT_GR" ) ) checker = new SetGreedyValidator( query.selfJoin );
			else throw new RuntimeException("Unexpected value for verifier: "+params.verifier);
		}
		else throw new RuntimeException("BothSide is not supported.");
		return params;
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

		buildIndex( query.indexedSet );
		if ( !query.selfJoin ) buildIndex( query.searchedSet );

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
			if ( recS.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) continue;
			joinOneRecord( recS, rslt, idxT, idxCountT );
		}

		if ( !query.selfJoin ) {
			// T -> T' ~ S
			for ( Record recT : query.indexedSet.recordList ) {
				if ( recT.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) continue;
				joinOneRecord( recT, rslt, idxS, idxCountS );
			}
		}
		
		if ( addStat ) {
			stat.add( "Result_3_3_CandTokenTime", candTokenTime );
			stat.add( "Result_3_4_DpTime", dpTime/1e6 );
			stat.add( "Result_3_5_CountingTime", CountingTime );
			stat.add( "Result_3_6_ValidateTime", validateTime );
			stat.add( "Result_3_7_nScanList", nScanList );
		}
		return rslt;
	}

	protected void buildIndex( Dataset indexedSet ) {
		WYK_HashMap<Integer, List<Record>> idx = null;
		Object2IntOpenHashMap<Record> idxCount = null;

		if ( indexedSet == query.indexedSet ) {
			idx = idxT;
			idxCount = idxCountT;
		}
		else if ( indexedSet == query.searchedSet ) {
			idx = idxS;
			idxCount = idxCountS;
		}
		idxCount.defaultReturnValue( 0 );

		for ( Record rec : indexedSet.recordList ) {
			int[] tokens = rec.getTokensArray();
			int j_max = Math.min( indexK, tokens.length );
			IntOpenHashSet smallestK = new IntOpenHashSet(indexK);
			for ( int j=0; j<j_max; j++ ) {
				int smallest = tokens[0];
				for ( int i=1; i<tokens.length; i++ ) {
					if ( smallestK.contains( tokens[j] )) continue;
					if ( globalOrder.compare( tokens[i], smallest ) == -1 ) smallest = tokens[i];
				}
				smallestK.add( smallest );
			}
			idxCount.put( rec, smallestK.size() );
			for ( int token : smallestK ) {
				if ( idx.get( token ) == null ) idx.put( token, new ObjectArrayList<Record>() );
				idx.get( token ).add( rec );
			}
		}
		
		if (DEBUG.bIndexWriteToFile) {
			try {
				String name = "";
				if ( idx == idxS ) name = "idxS";
				else if ( idx == idxT ) name = "idxT";
				BufferedWriter bw = new BufferedWriter( new FileWriter( "./tmp/PQFilterDPSetIndex_"+name+".txt" ) );
				for ( int key : idx.keySet() ) {
					bw.write( "token: "+query.tokenIndex.getToken( key )+" ("+key+")\n" );
					for ( Record rec : idx.get( key ) ) bw.write( ""+rec.getID()+", " );
					bw.write( "\n" );
				}
			}
			catch( IOException e ) {
				e.printStackTrace();
				System.exit( 1 );
			}
		}
	}
	
	protected void joinOneRecord( Record rec, Set<IntegerPair> rslt, WYK_HashMap<Integer, List<Record>> idx, Object2IntOpenHashMap<Record> idxCount ) {
		long startTime = System.currentTimeMillis();
		// Enumerate candidate tokens of recS.
		IntOpenHashSet candidateTokens = new IntOpenHashSet();
		for ( int pos=0; pos<rec.size(); pos++ ) {
			for ( Rule rule : rec.getSuffixApplicableRules( pos )) {
				for (int token : rule.getRight() ) candidateTokens.add( token );
			}
		}
		long afterCandidateTime = System.currentTimeMillis();
		
		// Count the number of matches.
		Object2IntOpenHashMap<Record> count = getCount( rec, idx, candidateTokens );
		List<Record> candidateAfterCount = new ObjectArrayList<Record>();
		for ( Entry<Record, Integer> entry : count.entrySet() ) {
			Record recOther = entry.getKey();
			int recCount = entry.getValue();
			if ( recCount >= idxCount.getInt( recOther ) ) candidateAfterCount.add( recOther );
		}
		long afterCountTime = System.currentTimeMillis();
		
		// length filtering and verification
		int rec_maxlen = rec.getMaxTransLength();
		for ( Record recOther : candidateAfterCount ) {
			if ( useLF ) {
				if ( rec_maxlen < recOther.getDistinctTokenCount() ) {
					++checker.lengthFiltered;
					continue;
				}
			}
			int comp = checker.isEqual( rec, recOther );
			if ( comp >= 0 ) addSetResult( rec, recOther, rslt, idx == idxT, query.selfJoin );
		}
		long afterValidateTime = System.currentTimeMillis();
		
		candTokenTime += afterCandidateTime - startTime;
		CountingTime += afterCountTime - afterCandidateTime;
		validateTime += afterValidateTime - afterCountTime;
	}
	
	protected Object2IntOpenHashMap<Record> getCount( Record rec, WYK_HashMap<Integer, List<Record>> idx, IntOpenHashSet candidateTokens ) {
		Object2IntOpenHashMap<Record> count = new Object2IntOpenHashMap<Record>();
//		PkduckSetDP pkduckSetDP;
//		if ( useRuleComp ) pkduckSetDP = new PkduckSetDPWithRC( rec, globalOrder );
//		else pkduckSetDP = new PkduckSetDP( rec, globalOrder );
		count.defaultReturnValue(0);
		for ( int token : candidateTokens ) {
			if ( !idx.containsKey( token ) ) continue;
//			long startDPTime = System.nanoTime();
//			boolean isInSigU = true;
//			++nRunDP;
//			isInSigUTime += System.nanoTime() - startDPTime;
//			if ( isInSigU ) {
				for ( Record recOther : idx.get( token ) ) {
					count.addTo( recOther, 1 );
				}
				++nScanList;
//			}
		}
		return count;
	}

	@Override
	public String getName() {
		return "JoinBKPSet";
	}

	@Override
	public String getVersion() {
		/*
		 * 1.00: initial version
		 */
		return "1.00";
	}
}
