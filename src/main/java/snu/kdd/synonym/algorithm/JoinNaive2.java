package snu.kdd.synonym.algorithm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import mine.Record;
import snu.kdd.synonym.data.DataInfo;
import snu.kdd.synonym.tools.StatContainer;
import tools.IntegerPair;
import tools.RuleTrie;
import tools.Rule_ACAutomata;
import tools.WYK_HashSet;

public class JoinNaive2 extends AlgorithmTemplate {
	/**
	 * Map each record to its own index
	 */
	HashMap<Record, Integer> rec2idx;
	Rule_ACAutomata automata;
	RuleTrie ruletrie;

	static int threshold = 1000;

	public JoinNaive2( String rulefile, String Rfile, String Sfile, String outputfile, DataInfo dataInfo, boolean joinOneSide,
			StatContainer stat ) throws IOException {
		super( rulefile, Rfile, Sfile, outputfile, dataInfo, joinOneSide, stat );
		rec2idx = new HashMap<Record, Integer>();
		for( int i = 0; i < tableSearched.size(); ++i )
			rec2idx.put( tableSearched.get( i ), i );
	}

	@SuppressWarnings( "deprecation" )
	private List<IntegerPair> join() {
		automata = new Rule_ACAutomata( getRulelist() );
		ruletrie = new RuleTrie( getRulelist() );
		List<IntegerPair> rslt = new ArrayList<IntegerPair>();

		for( int idxS = 0; idxS < tableIndexed.size(); ++idxS ) {
			Record recS = tableIndexed.get( idxS );
			recS.preprocessRules( automata, false );
			recS.preprocessEstimatedRecords();
			long est = recS.getEstNumRecords();
			if( est > threshold )
				continue;
			ArrayList<Record> expanded = recS.expandAll( ruletrie );
			for( Record exp : expanded ) {
				ArrayList<Record> double_expanded = exp.expandAll( ruletrie );
				WYK_HashSet<Integer> candidates = new WYK_HashSet<Integer>();
				for( Record dexp : double_expanded ) {
					Integer idx = rec2idx.get( dexp );
					if( idx == null )
						continue;
					candidates.add( idx );
				}
				for( Integer idx : candidates ) {
					rslt.add( new IntegerPair( idx, idxS ) );
				}
			}
		}

		return rslt;
	}

	public void run() {
		long startTime = System.currentTimeMillis();
		List<IntegerPair> rslt = join();
		System.out.print( "Join finished" );
		System.out.println( " " + ( System.currentTimeMillis() - startTime ) );
		System.out.println( rslt.size() );

		this.writeResult( rslt );
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

	@Override
	public String getName() {
		return "JoinNaive2";
	}

	@Override
	public void run( String[] args ) {
		this.run();
	}
}
