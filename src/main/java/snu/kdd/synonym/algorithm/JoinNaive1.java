package snu.kdd.synonym.algorithm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import mine.Record;
import snu.kdd.synonym.data.DataInfo;
import snu.kdd.synonym.tools.IntegerComparator;
import snu.kdd.synonym.tools.StatContainer;
import snu.kdd.synonym.tools.StopWatch;
import tools.IntegerPair;
import tools.RuleTrie;
import tools.Rule_ACAutomata;
import tools.StaticFunctions;
import tools.WYK_HashMap;

/**
 * The Naive algorithm which expands strings from both tables S and T
 */
public class JoinNaive1 extends AlgorithmTemplate {
	public boolean skipequiv = false;

	Rule_ACAutomata automata;
	public double alpha;
	public double beta;

	/**
	 * Store the original index from expanded string
	 */

	Map<Record, ArrayList<Integer>> rec2idx;
	RuleTrie ruletrie;

	public long threshold = Long.MAX_VALUE;

	public JoinNaive1( AlgorithmTemplate o, StatContainer stat ) {
		super( o );

		// build an ac automata / a trie from rule lists
		automata = new Rule_ACAutomata( getRulelist() );
		ruletrie = new RuleTrie( getRulelist() );

		this.stat = stat;
	}

	public JoinNaive1( String rulefile, String Rfile, String Sfile, String outputfile, DataInfo dataInfo ) throws IOException {
		super( rulefile, Rfile, Sfile, outputfile, dataInfo );

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

		stat.addPrimary( "cmd_threshold", threshold );

		final StopWatch preprocessTime = StopWatch.getWatchStarted( "Result_2_Preprocess_Total_Time" );
		preprocess();
		preprocessTime.stop();
		stat.add( preprocessTime );

		final StopWatch runTime = StopWatch.getWatchStarted( "Result_3_Run_Time" );
		final List<IntegerPair> list = runWithoutPreprocess( true );
		runTime.stop();
		stat.add( runTime );

		final StopWatch writeTime = StopWatch.getWatchStarted( "Result_4_Write_Time" );
		this.writeResult( list );
		writeTime.stop();
		stat.add( writeTime );
	}

	private void buildIndex( boolean addStat ) {
		rec2idx = new WYK_HashMap<>( 1000000 );
		final long starttime = System.nanoTime();

		long totalExpSize = 0;
		long estimatedExpSize = 0;
		long idxsize = 0;
		int count = 0;

		long expandTime = 0;
		long indexingTime = 0;

		// DEBUG
		// try {
		// boolean debug = true;
		// BufferedWriter debug_bw = new BufferedWriter( new FileWriter( "est_debug.txt" ) );

		for( int i = 0; i < tableSearched.size(); ++i ) {
			final Record recR = tableSearched.get( i );
			final long est = recR.getEstNumRecords();

			if( threshold != -1 && est > threshold ) {
				// if threshold is set (!= -1), index is built selectively for supporting hybrid algorithm
				continue;
			}

			long expandStartTime = System.nanoTime();
			final List<Record> expanded = recR.expandAll( ruletrie );
			expandTime += System.nanoTime() - expandStartTime;

			assert ( threshold == -1 || expanded.size() <= threshold );

			// if( debug ) {
			// if( expanded.size() != est ) {
			// debug_bw.write( recR.toString() + "\n" );
			// debug_bw.write( "expaneded: " + expanded.size() + "\n" );
			// debug_bw.write( "estimated: " + est + "\n" );
			// for( int x = 0; x < expanded.size(); x++ ) {
			// debug_bw.write( expanded.get( x ).toString() + "\n" );
			// }
			// debug = false;
			// }
			// }

			totalExpSize += expanded.size();
			estimatedExpSize += est;

			long indexingStartTime = System.nanoTime();
			for( final Record exp : expanded ) {
				ArrayList<Integer> list = rec2idx.get( exp );

				if( list == null ) {
					// new expression
					list = new ArrayList<>( 5 );
					rec2idx.put( exp, list );
				}

				// If current list already contains current record as the last element, skip adding
				if( !list.isEmpty() && list.get( list.size() - 1 ) == i ) {
					continue;
				}
				list.add( i );
				idxsize++;
			}
			indexingTime += System.nanoTime() - indexingStartTime;
			++count;
		}
		// debug_bw.close();
		// }
		// catch( Exception e ) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// ((WYK_HashMap<Record, ArrayList<Integer>>) rec2idx).printStat();
		final long duration = System.nanoTime() - starttime;
		alpha = ( (double) duration ) / totalExpSize;

		if( addStat ) {
			stat.add( "Stat_Size_Indexed_Records", count );
			stat.add( "Stat_Size_Total_Index", idxsize );

			stat.add( "Est_Index_1_expSize", totalExpSize );
			stat.add( "Est_Index_2_expSizeEstimated", estimatedExpSize );
			stat.add( "Est_Index_3_executeTimeRatio", Double.toString( alpha ) );

			stat.add( "Est_Index_1_expandTime", expandTime );
			stat.add( "Est_Index_2_indexingTime", indexingTime );
			stat.add( "Est_Index_3_totalTime", duration );
		}
	}

	public void clearIndex() {
		if( rec2idx != null ) {
			rec2idx.clear();
		}
		rec2idx = null;
	}

	@Override
	public String getName() {
		return "JoinNaive1";
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

	private List<IntegerPair> join() {
		final List<IntegerPair> rslt = new ArrayList<>();
		final long starttime = System.nanoTime();
		long totalExpSize = 0;

		long expandTime = 0;
		long searchTime = 0;

		for( int idxS = 0; idxS < tableIndexed.size(); ++idxS ) {
			final Record recS = tableIndexed.get( idxS );
			final long est = recS.getEstNumRecords();
			if( threshold != -1 && est > threshold ) {
				continue;
			}

			long expandStartTime = System.nanoTime();
			final List<Record> expanded = recS.expandAll( ruletrie );
			expandTime += System.nanoTime() - expandStartTime;

			totalExpSize += expanded.size();
			final List<List<Integer>> candidates = new ArrayList<>( expanded.size() * 2 );

			long searchStartTime = System.nanoTime();
			for( final Record exp : expanded ) {
				final List<Integer> overlapidx = rec2idx.get( exp );
				if( overlapidx == null ) {
					continue;
				}
				candidates.add( overlapidx );
			}

			if( !skipequiv ) {
				final List<Integer> union = StaticFunctions.union( candidates, new IntegerComparator() );
				for( final Integer idx : union ) {
					rslt.add( new IntegerPair( idx, idxS ) );
				}
			}

			searchTime += System.nanoTime() - searchStartTime;
		}

		final long duration = System.nanoTime() - starttime;
		beta = ( (double) duration ) / totalExpSize;

		stat.add( "Est_Join_3_totalTime", duration );
		stat.add( "Est_Join_1_expandTime", expandTime );
		stat.add( "Est_Join_2_searchTime", searchTime );

		return rslt;
	}

	private void preprocess() {
		long applicableRules = 0;
		for( final Record t : tableSearched ) {
			t.preprocessRules( automata, false );
			applicableRules += t.getNumApplicableRules();
			t.preprocessEstimatedRecords();
		}
		stat.add( "Stat_Applicable Rule TableSearched", applicableRules );

		applicableRules = 0;
		for( final Record s : tableIndexed ) {
			s.preprocessRules( automata, false );
			applicableRules += s.getNumApplicableRules();
			s.preprocessEstimatedRecords();
		}
		stat.add( "Stat_Applicable Rule TableIndexed", applicableRules );
	}

	public List<IntegerPair> runWithoutPreprocess( boolean addStat ) {
		// Index building
		StopWatch idxTime = StopWatch.getWatchStarted( "Result_3_1_Index_Building_Time" );
		buildIndex( addStat );
		idxTime.stopQuiet();
		if( addStat ) {
			stat.add( idxTime );
		}

		// Join
		StopWatch joinTime = StopWatch.getWatchStarted( "Result_3_2_Join_Time" );
		final List<IntegerPair> rslt = join();
		joinTime.stopQuiet();
		if( addStat ) {
			stat.add( joinTime );
			// stat.addPrimary( "Naive Result size", rslt.size() );
			stat.add( "Stat_Counter_Union", StaticFunctions.union_cmp_counter );
			stat.add( "Stat_Counter_Equals", StaticFunctions.compare_cmp_counter );
		}

		return rslt;
	}

}
