package snu.kdd.synonym.algorithm;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
	WYK_HashMap<Record, ArrayList<Integer>> rec2idx;
	RuleTrie ruletrie;

	public long threshold = Long.MAX_VALUE;
	public double avgTransformed = 1;

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
		final List<IntegerPair> rslt = join( addStat );
		joinTime.stopQuiet();
		if( addStat ) {
			stat.add( joinTime );
			stat.add( "Stat_Counter_Union", StaticFunctions.union_cmp_counter );
			stat.add( "Stat_Counter_Equals", StaticFunctions.compare_cmp_counter );
		}

		return rslt;
	}

	private void preprocess() {
		stat.add( "Mem_1_Initialized", ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 );

		long applicableRules = 0;
		for( final Record t : tableSearched ) {
			t.preprocessRules( automata, false );
			applicableRules += t.getNumApplicableRules();
			t.preprocessEstimatedRecords();
		}
		stat.add( "Stat_Applicable Rule TableSearched", applicableRules );

		applicableRules = 0;
		long estTransformed = 0;
		for( final Record s : tableIndexed ) {
			s.preprocessRules( automata, false );
			applicableRules += s.getNumApplicableRules();
			s.preprocessEstimatedRecords();

			estTransformed += s.getEstNumRecords();
		}
		avgTransformed = estTransformed / (double) tableIndexed.size();

		stat.add( "Stat_Applicable Rule TableIndexed", applicableRules );
		stat.add( "Stat_Avg_Transformed_TableIndexed", Double.toString( avgTransformed ) );

		stat.add( "Mem_2_Preprocessed", ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 );
	}

	private void buildIndex( boolean addStat ) {

		final long starttime = System.nanoTime();
		int initialsize = (int) ( tableSearched.size() * avgTransformed / 2 );
		stat.add( "Hash Initial size ", initialsize );
		rec2idx = new WYK_HashMap<>( initialsize );

		long totalExpSize = 0;
		// long estimatedExpSize = 0;
		long idxsize = 0;
		// int count = 0;

		double expandTimesLength = 0;

		long expandTime = 0;
		long indexingTime = 0;

		// TODO DEBUG
		try {
			boolean debug = true;
			BufferedWriter debug_bw = new BufferedWriter( new FileWriter( "est_debug.txt" ) );
			long debug_Count = 0;
			long debug_IterCount = 0;
			long debug_putCount = 0;
			long debug_resizeCount = 0;
			long debug_RemoveCount = 0;
			long debug_RemoveIterCount = 0;

			for( int i = 0; i < tableSearched.size(); ++i ) {
				final Record recR = tableSearched.get( i );
				final long est = recR.getEstNumRecords();

				if( threshold != -1 && est > threshold ) {
					// if threshold is set (!= -1), index is built selectively for supporting hybrid algorithm
					continue;
				}

				long expandStartTime = System.nanoTime();
				// final List<Record> expanded = recR.expandAll( ruletrie );
				final List<Record> expanded = recR.expandAll();
				expandTime += System.nanoTime() - expandStartTime;

				assert ( threshold == -1 || expanded.size() <= threshold );

				totalExpSize += expanded.size();
				expandTimesLength += expanded.size() * recR.getTokenArray().length;
				// estimatedExpSize += est;

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

				if( debug ) {
					double time = System.nanoTime() - indexingStartTime;
					debug_bw.write( "" + expanded.size() );
					debug_bw.write( " " + recR.getTokenArray().length );
					// debug_bw.write( " " + ( rec2idx.getIterCount - debug_IterCount ) );
					debug_bw.write( " " + ( rec2idx.getCount - debug_Count ) );
					debug_bw.write( String.format( " %.2f", time / expanded.size() ) );
					debug_bw.write( String.format( " %.2f", time / recR.getTokenArray().length ) );
					debug_bw.write( String.format( " %.2f", time / ( rec2idx.getCount - debug_Count ) ) );
					debug_bw.write( " " + time );
					debug_bw.write( " " + Math.pow( 2, recR.getNumApplicableRules() ) );
					debug_bw.write( " " + ( rec2idx.putCount - debug_putCount ) );
					debug_bw.write( " " + ( rec2idx.resizeCount - debug_resizeCount ) );
					debug_bw.write( " " + ( rec2idx.getIterCount - debug_IterCount ) );
					debug_bw.write( " " + ( rec2idx.removeCount - debug_RemoveCount ) );
					debug_bw.write( " " + ( rec2idx.removeIterCount - debug_RemoveIterCount ) );
					debug_bw.write( "\n" );

					debug_Count = rec2idx.getCount;
					debug_IterCount = rec2idx.getIterCount;
					debug_putCount = rec2idx.putCount;
					debug_resizeCount = rec2idx.resizeCount;
					debug_RemoveCount = rec2idx.removeCount;
					debug_RemoveIterCount = rec2idx.removeIterCount;
				}

			}
			debug_bw.close();
		}
		catch( Exception e ) {
			e.printStackTrace();
		}

		final long duration = System.nanoTime() - starttime;
		alpha = ( (double) duration ) / totalExpSize;

		if( addStat ) {
			// stat.add( "Stat_Size_Indexed_Records", count );
			stat.add( "Stat_Size_Total_Index", idxsize );

			stat.add( "Est_Index_1_expSize", totalExpSize );
			// stat.add( "Est_Index_1_expSizeEstimated", estimatedExpSize );
			// stat.add( "Est_Index_1_executeTimeRatio", Double.toString( alpha ) );
			stat.add( "Est_Index_1_expandTime", expandTime );

			stat.add( "Est_Index_2_idxSize", idxsize );
			stat.add( "Est_Index_2_indexingTime", indexingTime );
			stat.add( "Est_Index_2_rec2idx_getcount", rec2idx.getCount );
			stat.add( "Est_Index_2_rec2idx_putcount", rec2idx.putCount );
			stat.add( "Est_Index_2_totalTime", duration );

			stat.add( "Est_Index_3_expandTimesLength", Double.toString( expandTimesLength ) );
			stat.add( "Est_Index_3_expandTimePerETL", Double.toString( expandTime / expandTimesLength ) );
			// stat.add( "Est_Index_3_timePerETL", Double.toString( duration / expandTimesLength ) );

			stat.add( "Mem_3_BuildIndex", ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 );
		}
	}

	public void clearIndex() {
		if( rec2idx != null ) {
			rec2idx.clear();
		}
		rec2idx = null;
	}

	private List<IntegerPair> join( boolean addStat ) {
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
			// final List<Record> expanded = recS.expandAll( ruletrie );
			final List<Record> expanded = recS.expandAll();
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

		if( addStat ) {
			stat.add( "Est_Join_1_expandTime", expandTime );
			stat.add( "Est_Join_2_searchTime", searchTime );
			stat.add( "Est_Join_3_totalTime", duration );
			stat.add( "Mem_4_Joined", ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 );
			stat.add( "Stat_Counter_ExpandAll", Record.expandAllCount );
		}

		return rslt;
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
