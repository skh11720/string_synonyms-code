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
	public double totalExp = 0;
	double totalExpLength = 0;

	public double indexTime = 0;
	public double joinTime = 0;

	NaiveIndex( int initialSize ) {
		if( initialSize < 10 ) {
			initialSize = 10;
		}
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

		for( int idxS = 0; idxS < tableSearched.size(); ++idxS ) {
			final Record recS = tableSearched.get( idxS );
			final long est = recS.getEstNumRecords();
			if( threshold != -1 && est > threshold ) {
				continue;
			}

			joinOneRecord( recS, rslt );
		}

		joinTime = System.nanoTime() - starttime;
		beta = joinTime / totalExp;

		if( addStat ) {
			stat.add( "Est_Join_1_expandTime", expandTime );
			stat.add( "Est_Join_2_searchTime", searchTime );
			stat.add( "Est_Join_3_totalTime", Double.toString( joinTime ) );

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

		totalExp += expanded.size();
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

		for( final Integer idx : candidates ) {
			rslt.add( new IntegerPair( recS.getID(), idx ) );
		}
	}

	public double estimatedIndexTime( double alpha ) {
		return alpha * totalExpLength;
	}

	public double estimatedJoinTime( double beta ) {
		return beta * totalExp;
	}

	public double estimatedExecutionTimeAfterJoin( double alpha, double beta ) {
		return estimatedIndexTime( alpha ) + estimatedJoinTime( beta );
	}

	public static NaiveIndex buildIndex( List<Record> tableIndexed, double avgTransformed, StatContainer stat, long threshold,
			boolean addStat ) {
		final long starttime = System.nanoTime();
		int initialsize = (int) ( tableIndexed.size() * avgTransformed / 2 );
		stat.add( "Auto_Hash_Initial_Size ", initialsize );
		NaiveIndex naiveIndex = new NaiveIndex( initialsize );

		long totalExpLength = 0;

		long idxsize = 0;

		double totalExp = 0;
		long expandTime = 0;
		long indexingTime = 0;

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

			totalExpLength += expanded.size() * recR.getTokenArray().length;
			totalExp += expanded.size();

			long indexingStartTime = System.nanoTime();
			for( final Record exp : expanded ) {
				naiveIndex.add( exp, i );

				idxsize++;
			}
			indexingTime += System.nanoTime() - indexingStartTime;

		}
		// debug_bw.close();
		// }
		// catch( Exception e ) {
		// e.printStackTrace();
		// }

		if( totalExpLength == 0 ) {
			totalExpLength = 1;
		}

		naiveIndex.indexTime = System.nanoTime() - starttime;
		naiveIndex.alpha = naiveIndex.indexTime / totalExpLength;
		naiveIndex.totalExpLength = totalExpLength;

		if( DEBUG.NaiveON ) {
			if( addStat ) {
				// stat.add( "Stat_Size_Indexed_Records", count );
				stat.add( "Stat_Size_Total_Index", idxsize );

				stat.add( "Est_Index_1_expSize", Double.toString( totalExp ) );
				// stat.add( "Est_Index_1_expSizeEstimated", estimatedExpSize );
				// stat.add( "Est_Index_1_executeTimeRatio", Double.toString( alpha ) );
				stat.add( "Est_Index_1_expandTime", expandTime );

				stat.add( "Est_Index_2_idxSize", idxsize );
				stat.add( "Est_Index_2_indexingTime", indexingTime );

				naiveIndex.addStat( stat, "Counter_Index" );
				stat.add( "Est_Index_2_totalTime", Double.toString( naiveIndex.indexTime ) );

				stat.add( "Est_Index_3_expandTimesLength", Double.toString( totalExpLength ) );
				stat.add( "Est_Index_3_expandTimePerETL", Double.toString( expandTime / totalExpLength ) );
				// stat.add( "Est_Index_3_timePerETL", Double.toString( duration / expandTimesLength ) );

				Runtime runtime = Runtime.getRuntime();
				stat.add( "Mem_3_BuildIndex", ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 );
			}
		}

		return naiveIndex;
	}
}
