package snu.kdd.synonym.tools;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mine.Record;
import tools.DEBUG;
import tools.IntegerPair;
import tools.WYK_HashMap;

public class NaiveIndex {
	public double alpha;
	public double beta;

	public WYK_HashMap<Record, ArrayList<Integer>> idx;

	long expandTime = 0;
	long searchTime = 0;
	long totalExpSize = 0;

	NaiveIndex( int initialSize ) {
		idx = new WYK_HashMap<Record, ArrayList<Integer>>( initialSize );
	}

	public void add( Record expanded, int recordId ) {
		ArrayList<Integer> list = idx.get( expanded );

		if( list == null ) {
			// new expression
			list = new ArrayList<>( 5 );
			idx.putNonExist( expanded, list );
		}

		// If current list already contains current record as the last element, skip adding
		if( !list.isEmpty() && list.get( list.size() - 1 ) == recordId ) {
			return;
		}

		list.add( recordId );
	}

	public ArrayList<Integer> get( Record expanded ) {
		return idx.get( expanded );
	}

	public void addStat( StatContainer stat, String prefix ) {
		stat.add( prefix + "_Get_Count", idx.getCount );
		stat.add( prefix + "_GetIter_Count", idx.getIterCount );
		stat.add( prefix + "_Put_Count", idx.putCount );
		stat.add( prefix + "_Resize_Count", idx.resizeCount );
		stat.add( prefix + "_Remove_Count", idx.removeCount );
		stat.add( prefix + "_RemoveIter_Count", idx.removeIterCount );
		stat.add( prefix + "_PutRemoved_Count", idx.putRemovedCount );
		stat.add( prefix + "_RemoveFound_Count", idx.removeFoundCount );
	}

	public List<IntegerPair> join( List<Record> tableSearched, StatContainer stat, long threshold, boolean addStat ) {
		final List<IntegerPair> rslt = new ArrayList<>();
		final long starttime = System.nanoTime();

		// try {
		// BufferedWriter debug_bw = new BufferedWriter( new FileWriter( "DEBUG_JOIN.txt" ) );
		// boolean debug = true;
		// long debug_Count = Record.expandAllCount;
		// long debug_IterCount = rec2idx.getIterCount;
		// long debug_putCount = rec2idx.putCount;
		// long debug_resizeCount = rec2idx.resizeCount;
		// long debug_RemoveCount = rec2idx.removeCount;
		// long debug_RemoveIterCount = rec2idx.removeIterCount;
		// long debug_gcCount = getGCCount();
		// long debug_expandIterCount = Record.expandAllIterCount;

		for( int idxS = 0; idxS < tableSearched.size(); ++idxS ) {
			final Record recS = tableSearched.get( idxS );
			final long est = recS.getEstNumRecords();
			if( threshold != -1 && est > threshold ) {
				continue;
			}

			joinOneRecord( recS, rslt );

		}
		// debug_bw.close();
		// }
		// catch( Exception e ) {
		// e.printStackTrace();
		// }

		final long duration = System.nanoTime() - starttime;
		beta = ( (double) duration ) / totalExpSize;

		if( addStat ) {
			stat.add( "Est_Join_1_expandTime", expandTime );
			stat.add( "Est_Join_2_searchTime", searchTime );
			stat.add( "Est_Join_3_totalTime", duration );

			Runtime runtime = Runtime.getRuntime();
			stat.add( "Mem_4_Joined", ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 );
			stat.add( "Stat_Counter_ExpandAll", Record.expandAllCount );
		}

		return rslt;
	}

	public void joinOneRecord( Record recS, List<IntegerPair> rslt ) {
		long expandStartTime = System.nanoTime();
		// final List<Record> expanded = recS.expandAll( ruletrie );
		final List<Record> expanded = recS.expandAll();
		expandTime += System.nanoTime() - expandStartTime;

		totalExpSize += expanded.size();
		// final List<List<Integer>> candidates = new ArrayList<List<Integer>>();
		final Set<Integer> candidates = new HashSet<Integer>();

		long searchStartTime = System.nanoTime();
		for( final Record exp : expanded ) {
			final List<Integer> overlapidx = idx.get( exp );
			if( overlapidx == null ) {
				continue;
			}

			// candidates.add( overlapidx );
			for( Integer i : overlapidx ) {
				candidates.add( i );
			}
		}
		searchTime += System.nanoTime() - searchStartTime;

		// if( debug ) {
		// double time = System.nanoTime() - searchStartTime;
		// long gcCount = getGCCount();
		// debug_bw.write( "" + expanded.size() );
		// debug_bw.write( " " + recS.getTokenArray().length );
		// // debug_bw.write( " " + ( rec2idx.getIterCount - debug_IterCount ) );
		// debug_bw.write( " " + ( Record.expandAllCount - debug_Count ) );
		// debug_bw.write( String.format( " %.2f", time / expanded.size() ) );
		// debug_bw.write( String.format( " %.2f", time / recS.getTokenArray().length ) );
		// debug_bw.write( String.format( " %.2f", time / ( Record.expandAllCount - debug_Count ) ) );
		// debug_bw.write( " " + time );
		// debug_bw.write( " " + Math.pow( 2, recS.getNumApplicableRules() ) );
		// debug_bw.write( " " + ( rec2idx.putCount - debug_putCount ) );
		// debug_bw.write( " " + ( rec2idx.resizeCount - debug_resizeCount ) );
		// debug_bw.write( " " + ( rec2idx.getIterCount - debug_IterCount ) );
		// debug_bw.write( " " + ( rec2idx.removeCount - debug_RemoveCount ) );
		// debug_bw.write( " " + ( rec2idx.removeIterCount - debug_RemoveIterCount ) );
		// debug_bw.write( " " + recS.getID() );
		// debug_bw.write( " " + ( gcCount - debug_gcCount ) );
		// debug_bw.write( " " + ( Record.expandAllIterCount - debug_expandIterCount ) );
		// debug_bw.write( "\n" );
		//
		// debug_Count = Record.expandAllCount;
		// debug_IterCount = rec2idx.getIterCount;
		// debug_putCount = rec2idx.putCount;
		// debug_resizeCount = rec2idx.resizeCount;
		// debug_RemoveCount = rec2idx.removeCount;
		// debug_RemoveIterCount = rec2idx.removeIterCount;
		// debug_expandIterCount = Record.expandAllIterCount;
		// debug_gcCount = gcCount;
		// }

		// final List<Integer> union = StaticFunctions.union( candidates, new IntegerComparator() );
		for( final Integer idx : candidates ) {
			// for( final Integer idx : union ) {
			rslt.add( new IntegerPair( recS.getID(), idx ) );
		}
	}

	public static NaiveIndex buildIndex( List<Record> tableIndexed, double avgTransformed, StatContainer stat, long threshold,
			boolean addStat ) {
		final long starttime = System.nanoTime();
		int initialsize = (int) ( tableIndexed.size() * avgTransformed / 2 );
		stat.add( "Auto_Hash_Initial_Size ", initialsize );
		NaiveIndex naiveIndex = new NaiveIndex( initialsize );

		long totalExpSize = 0;
		// long estimatedExpSize = 0;
		long idxsize = 0;
		// int count = 0;

		double expandTimesLength = 0;

		long expandTime = 0;
		long indexingTime = 0;

		// try {
		// boolean debug = true;
		// BufferedWriter debug_bw = new BufferedWriter( new FileWriter( "est_debug.txt" ) );
		// long debug_Count = 0;
		// long debug_IterCount = 0;
		// long debug_putCount = 0;
		// long debug_resizeCount = 0;
		// long debug_RemoveCount = 0;
		// long debug_RemoveIterCount = 0;

		for( int i = 0; i < tableIndexed.size(); ++i ) {
			final Record recR = tableIndexed.get( i );
			final long est = recR.getEstNumRecords();

			if( threshold != -1 && est > threshold ) {
				// if threshold is set (!= -1), index is built selectively for supporting hybrid algorithm
				continue;
			}

			long expandStartTime;
			if( DEBUG.NaiveON ) {
				expandStartTime = System.nanoTime();
			}

			final List<Record> expanded = recR.expandAll();

			if( DEBUG.NaiveON ) {
				expandTime += System.nanoTime() - expandStartTime;
			}

			totalExpSize += expanded.size();
			expandTimesLength += expanded.size() * recR.getTokenArray().length;

			long indexingStartTime = System.nanoTime();
			for( final Record exp : expanded ) {
				naiveIndex.add( exp, i );

				idxsize++;
			}
			indexingTime += System.nanoTime() - indexingStartTime;

			// if( debug ) {
			// double time = System.nanoTime() - indexingStartTime;
			// debug_bw.write( "" + expanded.size() );
			// debug_bw.write( " " + recR.getTokenArray().length );
			// // debug_bw.write( " " + ( rec2idx.getIterCount - debug_IterCount ) );
			// debug_bw.write( " " + ( rec2idx.getCount - debug_Count ) );
			// debug_bw.write( String.format( " %.2f", time / expanded.size() ) );
			// debug_bw.write( String.format( " %.2f", time / recR.getTokenArray().length ) );
			// debug_bw.write( String.format( " %.2f", time / ( rec2idx.getCount - debug_Count ) ) );
			// debug_bw.write( " " + time );
			// debug_bw.write( " " + Math.pow( 2, recR.getNumApplicableRules() ) );
			// debug_bw.write( " " + ( rec2idx.putCount - debug_putCount ) );
			// debug_bw.write( " " + ( rec2idx.resizeCount - debug_resizeCount ) );
			// debug_bw.write( " " + ( rec2idx.getIterCount - debug_IterCount ) );
			// debug_bw.write( " " + ( rec2idx.removeCount - debug_RemoveCount ) );
			// debug_bw.write( " " + ( rec2idx.removeIterCount - debug_RemoveIterCount ) );
			// debug_bw.write( "\n" );
			//
			// debug_Count = rec2idx.getCount;
			// debug_IterCount = rec2idx.getIterCount;
			// debug_putCount = rec2idx.putCount;
			// debug_resizeCount = rec2idx.resizeCount;
			// debug_RemoveCount = rec2idx.removeCount;
			// debug_RemoveIterCount = rec2idx.removeIterCount;
			// }

		}
		// debug_bw.close();
		// }
		// catch( Exception e ) {
		// e.printStackTrace();
		// }

		final long duration = System.nanoTime() - starttime;
		naiveIndex.alpha = ( (double) duration ) / totalExpSize;

		if( addStat ) {
			// stat.add( "Stat_Size_Indexed_Records", count );
			stat.add( "Stat_Size_Total_Index", idxsize );

			stat.add( "Est_Index_1_expSize", totalExpSize );
			// stat.add( "Est_Index_1_expSizeEstimated", estimatedExpSize );
			// stat.add( "Est_Index_1_executeTimeRatio", Double.toString( alpha ) );
			stat.add( "Est_Index_1_expandTime", expandTime );

			stat.add( "Est_Index_2_idxSize", idxsize );
			stat.add( "Est_Index_2_indexingTime", indexingTime );

			naiveIndex.addStat( stat, "Counter_Index" );
			stat.add( "Est_Index_2_totalTime", duration );

			stat.add( "Est_Index_3_expandTimesLength", Double.toString( expandTimesLength ) );
			stat.add( "Est_Index_3_expandTimePerETL", Double.toString( expandTime / expandTimesLength ) );
			// stat.add( "Est_Index_3_timePerETL", Double.toString( duration / expandTimesLength ) );

			Runtime runtime = Runtime.getRuntime();
			stat.add( "Mem_3_BuildIndex", ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 );

		}

		return naiveIndex;
	}
}
