package snu.kdd.synonym.synonymRev.algorithm;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import sigmod13.SIRecord;
import sigmod13.SI_Tree_Original;
import sigmod13.filter.ITF_Filter;
import snu.kdd.synonym.synonymRev.data.ACAutomataR;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.TokenIndex;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.Pair;
import snu.kdd.synonym.synonymRev.tools.StopWatch;

public class SIJoinOriginal extends AbstractParameterizedAlgorithm {

	public final double theta;
	private ObjectArrayList<SIRecord> S, T;
	private Int2IntOpenHashMap tokenFreq;

	public static TokenIndex tokenMap;
	public static PrintWriter pw = null;

	public SIJoinOriginal(String[] args) {
		super(args);
		theta = param.getDoubleParam("theta");
	}
	
	@Override
	public void initialize() {
		super.initialize();

		S = new ObjectArrayList<>();
		tokenFreq = new Int2IntOpenHashMap();
		if ( query.selfJoin ) {
			T = S;
		}
		else {
			T = new ObjectArrayList<>();
		}

		tokenMap = query.tokenIndex;

		try {
			String[] tokens = query.getSearchedPath().split("\\"+File.separator);
			String data1Name = tokens[tokens.length-1].split("\\.")[0];
			if ( query.selfJoin ) pw = new PrintWriter( new BufferedWriter( new FileWriter( String.format( "tmp/SIJoinOriginal_verify_%s_%.3f.txt", data1Name, theta ) ) ) ); 
			else {
				tokens = query.getIndexedPath().split("\\"+File.separator);
				String data2Name = tokens[tokens.length-1].split("\\.")[0];
				pw = new PrintWriter( new BufferedWriter( new FileWriter( String.format( "tmp/SIJoinOriginal_verify_%s_%s_%.3f.txt", data1Name, data2Name, theta) ) ) );
			}
		}
		catch (IOException e) { e.printStackTrace(); }
	}

	@Override
	protected void reportParamsToStat() {
		stat.add("Param_theta", theta);
	}

	@Override
	public void preprocess() {
		ACAutomataR automata = new ACAutomataR( query.ruleSet.get() );
		Map<String, Integer> str2int = query.tokenIndex.getMap();

		for( Record recS : query.searchedSet.get() ) {
			S.add( new SIRecord( recS.getID(), recS.toString(), str2int, automata) );
			for ( int token : recS.getTokens() ) tokenFreq.addTo(token, 1);
		}

		if( !query.selfJoin ) {
			for( Record recT : query.indexedSet.get() ) {
				T.add( new SIRecord( recT.getID(), recT.toString(), str2int, automata) );
				for ( int token : recT.getTokens() ) tokenFreq.addTo(token, 1);
			}
		}
	}

	@Override
	protected void executeJoin() {
		StopWatch stepTime = null;
		stepTime = StopWatch.getWatchStarted( INDEX_BUILD_TIME );
		ITF_Filter filter = new ITF_Filter(null, null) {
			@Override
			public int compare(int t1, boolean t1_from_record, int t2, boolean t2_from_record) {
				return Integer.compare( tokenFreq.get(t1), tokenFreq.get(t2) );
			}
		};
		SI_Tree_Original<SIRecord> treeS = new SI_Tree_Original<SIRecord>( theta, filter, S, null );
		SI_Tree_Original<SIRecord> treeT = null;
		if ( !query.selfJoin ) {
			treeT = new SI_Tree_Original<SIRecord>( theta, filter, T, null );
		}
		else treeT = treeS;
		stepTime.stopAndAdd( stat );
		stat.addMemory( "Mem_3_BuildIndex" );

		stepTime.resetAndStart( JOIN_AFTER_INDEX_TIME );
		rslt = join( treeS, treeT, theta );
		stepTime.stopAndAdd( stat );

		stat.addMemory( "Mem_4_Joined" );
		stat.add( "Stat_Equiv_Comparison", treeS.verifyCount );
	}

	protected Set<IntegerPair> join( SI_Tree_Original<SIRecord> treeS, SI_Tree_Original<SIRecord> treeT, double threshold ) {
		long startTime = System.currentTimeMillis();

		List<Pair<SIRecord>> candidates = treeS.join( treeT, threshold );
		// long counter = treeR.join(treeS, threshold);

		if( DEBUG.SIJoinON ) {
			System.out.print( "Retrieveing candidates finished" );

			System.out.println( " " + ( System.currentTimeMillis() - startTime ) );
			System.out.println( "Candidates : " + candidates.size() );

			startTime = System.currentTimeMillis();

			System.out.print( "Validating finished" );
			System.out.println( " " + ( System.currentTimeMillis() - startTime ) );
			System.out.println( "Similar pairs : " + candidates.size() );
		}

		Set<IntegerPair> rslt = new ObjectOpenHashSet<IntegerPair>();

		for( Pair<SIRecord> ip : candidates ) {
			addSetResult( query.searchedSet.getRecord(ip.rec1.getID()), query.indexedSet.getRecord(ip.rec2.getID()), rslt, true, query.selfJoin );
		}
		return rslt;
	}

	@Override
	public String getVersion() {
		/*
		 * 1.00: initial version
		 */
		return "1.00";
	}

	@Override
	public String getName() {
		return "SIJoinOriginal";
	}
	
	@Override
	public String getNameWithParam() {
		return String.format("%s_%.2f", getName(), theta);
	}
}
