package snu.kdd.synonym.synonymRev.algorithm;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.ParseException;

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
import snu.kdd.synonym.synonymRev.tools.Param;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;

public class SIJoinOriginal extends AlgorithmTemplate {

	private final double theta;
	private final ObjectArrayList<SIRecord> S, T;
	private final Int2IntOpenHashMap tokenFreq;

	public static TokenIndex tokenMap;
	public static PrintWriter pw = null;

	public SIJoinOriginal(Query query, StatContainer stat, String[] args) throws IOException, ParseException {
		super(query, stat, args);
		param = new Param(args);
		theta = param.getDoubleParam("theta");
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
			String[] tokens = query.searchedFile.split("\\"+File.separator);
			String data1Name = tokens[tokens.length-1].split("\\.")[0];
			if ( query.selfJoin ) pw = new PrintWriter( new BufferedWriter( new FileWriter( String.format( "tmp/SIJoinOriginal_verify_%s_%.3f.txt", data1Name, theta ) ) ) ); 
			else {
				tokens = query.indexedFile.split("\\"+File.separator);
				String data2Name = tokens[tokens.length-1].split("\\.")[0];
				pw = new PrintWriter( new BufferedWriter( new FileWriter( String.format( "tmp/SIJoinOriginal_verify_%s_%s_%.3f.txt", data1Name, data2Name, theta) ) ) );
			}
		}
		catch (IOException e) { e.printStackTrace(); }
	}

	@Override
	public void preprocess() {
//		super.preprocess();

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

	public void run() {
		long startTime = System.currentTimeMillis();

		if( DEBUG.SIJoinON ) {
			System.out.print( "Constructor finished" );
			System.out.println( " " + ( System.currentTimeMillis() - startTime ) );
		}

		StopWatch stepTime = StopWatch.getWatchStarted( "Result_2_Preprocess_Total_Time" );
		preprocess();
		stepTime.stopAndAdd( stat );

//		SI_Tree_Original<Record> treeR = new SI_Tree_Original<Record>( 1, null, query.searchedSet.recordList, checker );
//		SI_Tree_Original<Record> treeS = new SI_Tree_Original<Record>( 1, null, query.indexedSet.recordList, checker );

		stepTime = StopWatch.getWatchStarted( "Result_3_1_Index_Building_Time" );
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

		if( DEBUG.SIJoinON ) {
			System.out.println( "Node size : " + ( treeS.FEsize + treeS.LEsize ) );
			System.out.println( "Sig size : " + treeS.sigsize );

			System.out.print( "Building SI-Tree finished" );
			System.out.println( " " + ( System.currentTimeMillis() - startTime ) );
		}
		// br.readLine();

		stepTime.resetAndStart( "Result_3_2_Join_Time" );
		rslt = join( treeS, treeT, theta );
		stepTime.stopAndAdd( stat );

		stat.add( "Stat_Equiv_Comparison", treeS.verifyCount );

		writeResult();
	}

	public Set<IntegerPair> join( SI_Tree_Original<SIRecord> treeS, SI_Tree_Original<SIRecord> treeT, double threshold ) {
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
//			rslt.add( new IntegerPair( ip.rec1.getID(), ip.rec2.getID() ) );
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
}
