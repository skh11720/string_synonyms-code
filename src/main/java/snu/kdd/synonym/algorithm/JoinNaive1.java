package snu.kdd.synonym.algorithm;

import java.io.IOException;
import java.util.List;

import mine.Record;
import snu.kdd.synonym.data.DataInfo;
import snu.kdd.synonym.tools.NaiveIndex;
import snu.kdd.synonym.tools.StatContainer;
import snu.kdd.synonym.tools.StopWatch;
import tools.DEBUG;
import tools.IntegerPair;
import tools.RuleTrie;
import tools.Rule_ACAutomata;
import tools.StaticFunctions;

/**
 * The Naive algorithm which expands strings from both tables S and T
 */
public class JoinNaive1 extends AlgorithmTemplate {
	public boolean skipequiv = false;

	public Rule_ACAutomata automata;

	/**
	 * Store the original index from expanded string
	 */
	public NaiveIndex idx;
	RuleTrie ruletrie;

	public long threshold = Long.MAX_VALUE;
	public double avgTransformed = 1;

	public double executionTime;

	public JoinNaive1( AlgorithmTemplate o, StatContainer stat ) {
		super( o );

		// build an ac automata / a trie from rule lists
		automata = new Rule_ACAutomata( getRulelist() );
		ruletrie = new RuleTrie( getRulelist() );

		this.stat = stat;
	}

	public JoinNaive1( String rulefile, String Rfile, String Sfile, String outputfile, DataInfo dataInfo, boolean oneSideJoin )
			throws IOException {
		super( rulefile, Rfile, Sfile, outputfile, dataInfo, oneSideJoin );

		// build an ac automata / a trie from rule lists
		automata = new Rule_ACAutomata( getRulelist() );
		ruletrie = new RuleTrie( getRulelist() );
	}

	@Override
	public void run( String[] args, StatContainer stat ) {
		if( args.length != 1 ) {
			System.out.println( "Usage : <R file> <S file> <Rule file> <output file> <exp threshold>" );
		}
		this.stat = stat;
		this.threshold = Long.valueOf( args[ 0 ] );

		StopWatch stepTime = null;
		if( DEBUG.NaiveON ) {
			stat.addPrimary( "cmd_threshold", threshold );
			stepTime = StopWatch.getWatchStarted( "Result_2_Preprocess_Total_Time" );
		}

		preprocess();

		if( DEBUG.NaiveON ) {
			stepTime.stopAndAdd( stat );
			stat.add( "Mem_2_Preprocessed", ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 );
			stepTime.resetAndStart( "Result_3_Run_Time" );
		}

		final List<IntegerPair> list = runWithoutPreprocess( true );

		if( DEBUG.NaiveON ) {
			stepTime.stopAndAdd( stat );
			stepTime.resetAndStart( "Result_4_Write_Time" );
		}

		this.writeResult( list );

		if( DEBUG.NaiveON ) {
			stepTime.stopAndAdd( stat );
		}
	}

	public List<IntegerPair> runWithoutPreprocess( boolean addStat ) {
		// Index building
		StopWatch stepTime = null;
		if( DEBUG.NaiveON ) {
			stepTime = StopWatch.getWatchStarted( "Result_3_1_Index_Building_Time" );
		}

		idx = NaiveIndex.buildIndex( tableIndexed, avgTransformed, stat, threshold, addStat );

		if( DEBUG.NaiveON ) {
			stepTime.stopQuiet();
			if( addStat ) {
				stat.add( stepTime );
			}
			stepTime.resetAndStart( "Result_3_2_Join_Time" );
		}

		// Join
		final List<IntegerPair> rslt = idx.join( tableSearched, stat, threshold, addStat );

		if( DEBUG.NaiveON ) {
			stepTime.stopQuiet();
			if( addStat ) {
				stat.add( stepTime );

				stat.add( "Stat_Counter_Union", StaticFunctions.union_cmp_counter );
				stat.add( "Stat_Counter_Equals", StaticFunctions.compare_cmp_counter );
				idx.addStat( stat, "Counter_Join" );
			}
		}

		return rslt;
	}

	private void preprocess() {
		if( DEBUG.NaiveON ) {
			stat.add( "Mem_1_Initialized", ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 );
		}

		@SuppressWarnings( "unused" )
		long applicableRules = 0;
		for( final Record t : tableSearched ) {
			t.preprocessRules( automata, false );
			applicableRules += t.getNumApplicableRules();
			t.preprocessEstimatedRecords();
		}

		if( DEBUG.NaiveON ) {
			stat.add( "Stat_Applicable Rule TableSearched", applicableRules );
		}

		applicableRules = 0;
		long estTransformed = 0;
		for( final Record s : tableIndexed ) {
			s.preprocessRules( automata, false );
			applicableRules += s.getNumApplicableRules();
			s.preprocessEstimatedRecords();

			estTransformed += s.getEstNumRecords();
		}
		avgTransformed = estTransformed / (double) tableIndexed.size();

		if( DEBUG.NaiveON ) {
			stat.add( "Stat_Applicable Rule TableIndexed", applicableRules );
			stat.add( "Stat_Avg_Transformed_TableIndexed", Double.toString( avgTransformed ) );
		}
	}

	public double getAlpha() {
		return idx.alpha;
	}

	public double getBeta() {
		return idx.beta;
	}

	@Override
	public String getName() {
		return "JoinNaive1";
	}

	@Override
	public String getVersion() {
		return "1.0";
	}
}
