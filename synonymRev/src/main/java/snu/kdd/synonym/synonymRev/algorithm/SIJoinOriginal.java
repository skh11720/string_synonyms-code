package snu.kdd.synonym.synonymRev.algorithm;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import sigmod13.SIRecord;
import sigmod13.SI_Tree_Original;
import sigmod13.filter.ITF_Filter;
import snu.kdd.synonym.synonymRev.data.ACAutomataR;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.data.TokenIndex;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.Pair;
import snu.kdd.synonym.synonymRev.tools.ResultSet;
import snu.kdd.synonym.synonymRev.tools.Stat;
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
		Record.tokenIndex = query.tokenIndex;

		for( Record recS : query.searchedSet.get() ) {
			SIRecord sirec = new SIRecord( recS.getID(), recS.toString(), Record.tokenIndex);
			sirec.preprocess(automata);
			S.add(sirec);
			for ( int token : recS.getTokens() ) tokenFreq.addTo(token, 1);
		}

		if( !query.selfJoin ) {
			for( Record recT : query.indexedSet.get() ) {
				SIRecord sirec =  new SIRecord( recT.getID(), recT.toString(), Record.tokenIndex);
				sirec.preprocess(automata);
				T.add(sirec);
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
		stat.add( Stat.NUM_VERIFY, treeS.verifyCount );
	}

	protected ResultSet join( SI_Tree_Original<SIRecord> treeS, SI_Tree_Original<SIRecord> treeT, double threshold ) {
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

		ResultSet rslt = new ResultSet(query.selfJoin);

		for( Pair<SIRecord> ip : candidates ) {
			rslt.add( query.searchedSet.getRecord(ip.rec1.getID()), query.indexedSet.getRecord(ip.rec2.getID()));
		}
		return rslt;
	}

	@Override
	public String getVersion() {
		/*
		 * 1.00: initial version
		 * 1.01: major update
		 * 1.02: use ExpandIterator
		 */
		return "1.02";
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
