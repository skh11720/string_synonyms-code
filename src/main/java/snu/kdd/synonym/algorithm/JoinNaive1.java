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

		final StopWatch writeTime = StopWatch.getWatchStarted( "Write Time" );
		this.writeResult( list );
		writeTime.stop();
		stat.add( writeTime );
	}

	private void buildIndex() {
		rec2idx = new WYK_HashMap<>( 1000000 );
		final long starttime = System.nanoTime();

		long totalExpSize = 0;
		long estimatedExpSize = 0;
		long idxsize = 0;
		int count = 0;

		for( int i = 0; i < tableSearched.size(); ++i ) {
			final Record recR = tableSearched.get( i );
			final long est = recR.getEstNumRecords();

			if( threshold != -1 && est > threshold ) {
				// if threshold is set (!= -1), index is built selectively for supporting hybrid algorithm
				continue;
			}

			final List<Record> expanded = recR.expandAll( ruletrie );

			assert ( threshold == -1 || expanded.size() <= threshold );

			totalExpSize += expanded.size();
			estimatedExpSize += est;

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
			++count;
		}

		stat.add( "Stat_Indexed Records", count );
		stat.add( "Stat_Total index size", idxsize );

		// ((WYK_HashMap<Record, ArrayList<Integer>>) rec2idx).printStat();
		final long duration = System.nanoTime() - starttime;
		alpha = ( (double) duration ) / totalExpSize;

		stat.add( "Est_Stat_totalExpSize", totalExpSize );
		stat.add( "Est_Stat_estimatedExpSize", estimatedExpSize );
		stat.add( "Est_Stat_executeTimeRatio", Double.toString( alpha ) );

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

		for( int idxS = 0; idxS < tableIndexed.size(); ++idxS ) {
			final Record recS = tableIndexed.get( idxS );
			final long est = recS.getEstNumRecords();
			if( threshold != -1 && est > threshold ) {
				continue;
			}
			final List<Record> expanded = recS.expandAll( ruletrie );
			totalExpSize += expanded.size();
			final List<List<Integer>> candidates = new ArrayList<>( expanded.size() * 2 );
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
		}

		final long duration = System.nanoTime() - starttime;
		beta = ( (double) duration ) / totalExpSize;

		return rslt;
	}

	private void preprocess() {
		for( final Record r : tableSearched ) {
			r.preprocessRules( automata, false );
			r.preprocessEstimatedRecords();
		}
		for( final Record s : tableIndexed ) {
			s.preprocessRules( automata, false );
			s.preprocessEstimatedRecords();
		}
	}

	public List<IntegerPair> runWithoutPreprocess( boolean addStat ) {
		// Index building
		StopWatch idxTime = StopWatch.getWatchStarted( "Result_3_1_Index_Building_Time" );
		buildIndex();
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
			stat.add( "Stat_Union counter", StaticFunctions.union_cmp_counter );
			stat.add( "Stat_Equals counter", StaticFunctions.compare_cmp_counter );
		}

		return rslt;
	}
}
