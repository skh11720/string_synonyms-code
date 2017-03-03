package snu.kdd.synonym.algorithm;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import mine.Record;
import mine.RecordPair;
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

	public JoinNaive1( AlgorithmTemplate o ) {
		super( o );

		// build an ac automata / a trie from rule lists
		automata = new Rule_ACAutomata( getRulelist() );
		ruletrie = new RuleTrie( getRulelist() );
	}

	public JoinNaive1( String rulefile, String Rfile, String Sfile, String outputfile ) throws IOException {
		super( rulefile, Rfile, Sfile, outputfile );

		// build an ac automata / a trie from rule lists
		automata = new Rule_ACAutomata( getRulelist() );
		ruletrie = new RuleTrie( getRulelist() );
	}

	private void buildIndex() {
		rec2idx = new WYK_HashMap<>( 1000000 );
		final long starttime = System.nanoTime();
		long totalExpSize = 0;
		int count = 0;
		for( int i = 0; i < tableR.size(); ++i ) {
			final Record recR = tableR.get( i );
			final long est = recR.getEstNumRecords();
			if( threshold != -1 && est > threshold ) {
				continue;
			}
			final List<Record> expanded = recR.expandAll( ruletrie );
			assert ( threshold == -1 || expanded.size() <= threshold );
			totalExpSize += expanded.size();
			for( final Record exp : expanded ) {
				ArrayList<Integer> list = rec2idx.get( exp );
				if( list == null ) {
					list = new ArrayList<>( 5 );
					rec2idx.put( exp, list );
				}
				// If current list already contains current record, skip adding
				if( !list.isEmpty() && list.get( list.size() - 1 ) == i ) {
					continue;
				}
				list.add( i );
			}
			++count;
		}
		long idxsize = 0;
		for( final List<Integer> list : rec2idx.values() ) {
			idxsize += list.size();
		}
		System.out.println( count + " records are indexed" );
		System.out.println( "Total index size: " + idxsize );
		// ((WYK_HashMap<Record, ArrayList<Integer>>) rec2idx).printStat();
		final long duration = System.nanoTime() - starttime;
		alpha = ( (double) duration ) / totalExpSize;
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

		for( int idxS = 0; idxS < tableS.size(); ++idxS ) {
			final Record recS = tableS.get( idxS );
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
		for( final Record r : tableR ) {
			r.preprocessRules( automata, false );
			r.preprocessEstimatedRecords();
		}
		for( final Record s : tableS ) {
			s.preprocessRules( automata, false );
			s.preprocessEstimatedRecords();
		}
	}

	@Override
	public void run( String[] args, StatContainer stat ) {
		if( args.length != 1 ) {
			System.out.println( "Usage : <R file> <S file> <Rule file> <output file> <exp threshold>" );
		}
		this.stat = stat;
		this.threshold = Long.valueOf( args[ 0 ] );

		stat.addPrimary( "cmd_threshold", threshold );

		final StopWatch preprocessTime = StopWatch.getWatchStarted( "Preprocess Time" );
		preprocess();
		preprocessTime.stop();
		stat.add( preprocessTime );

		final StopWatch runTime = StopWatch.getWatchStarted( "Run Time" );
		final List<RecordPair> list = runWithoutPreprocess();
		runTime.stop();
		stat.add( runTime );

		final StopWatch writeTime = StopWatch.getWatchStarted( "Write Time" );
		try {
			final BufferedWriter bw = new BufferedWriter( new FileWriter( this.outputfile ) );
			for( final RecordPair rp : list ) {
				final Record s = rp.record1;
				final Record t = rp.record2;
				if( !s.equals( t ) ) {
					bw.write( s.toString() + " == " + t.toString() + "\n" );
				}
			}
			bw.close();
		}
		catch( final IOException e ) {
			e.printStackTrace();
		}
		writeTime.stop();
		stat.add( writeTime );
	}

	public List<RecordPair> runWithoutPreprocess() {
		StopWatch idxTime = StopWatch.getWatchStarted( "Index building time" );
		buildIndex();
		idxTime.stopQuiet();
		stat.add( idxTime );

		StopWatch joinTime = StopWatch.getWatchStarted( "Join time" );
		final List<IntegerPair> rslt = join();
		joinTime.stopQuiet();
		stat.add( joinTime );

		stat.addPrimary( "Result size", rslt.size() );
		stat.add( "Union counter", StaticFunctions.union_cmp_counter );
		stat.add( "Equals counter", StaticFunctions.compare_cmp_counter );

		final List<RecordPair> rlist = new ArrayList<>();
		for( final IntegerPair ip : rslt ) {
			final Record r = tableR.get( ip.i1 );
			final Record s = tableS.get( ip.i2 );
			rlist.add( new RecordPair( r, s ) );
		}

		// Does we require to sort rlist?
		// Collections.sort( rlist );
		return rlist;
	}
}
