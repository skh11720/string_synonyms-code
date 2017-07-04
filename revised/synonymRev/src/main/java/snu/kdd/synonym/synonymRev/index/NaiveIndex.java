package snu.kdd.synonym.synonymRev.index;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import snu.kdd.synonym.synonymRev.data.Dataset;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.WYK_HashMap;

public class NaiveIndex {
	public WYK_HashMap<Record, ArrayList<Integer>> idx;

	public double alpha;
	public double beta;

	public double indexTime = 0;
	public double joinTime = 0;

	public double totalExp = 0;
	public double totalExpLength = 0;

	public double expandTime = 0;
	public double searchTime = 0;

	NaiveIndex( int initialSize ) {
		if( initialSize < 10 ) {
			initialSize = 10;
		}
		idx = new WYK_HashMap<Record, ArrayList<Integer>>( initialSize );
	}

	public static NaiveIndex buildIndex( double avgTransformed, StatContainer stat, long threshold, boolean addStat,
			Query query ) {
		final long starttime = System.nanoTime();
		int initialsize = (int) ( query.indexedSet.size() * avgTransformed / 2 );

		if( initialsize > 10000 ) {
			initialsize = 10000;
		}

		if( DEBUG.NaiveON ) {
			stat.add( "Auto_Hash_Initial_Size ", initialsize );
		}

		BufferedWriter bw = null;
		if( DEBUG.PrintNaiveIndexON ) {
			try {
				bw = new BufferedWriter( new FileWriter( "Naive_index.txt" ) );
			}
			catch( IOException e ) {
				e.printStackTrace();
			}
		}

		NaiveIndex naiveIndex = new NaiveIndex( initialsize );

		long totalExpLength = 0;

		double totalExp = 0;
		@SuppressWarnings( "unused" )
		long idxsize = 0;
		@SuppressWarnings( "unused" )
		long indexingTime = 0;

		long expandTime = 0;

		for( int i = 0; i < query.indexedSet.size(); ++i ) {
			final Record recR = query.indexedSet.getRecord( i );

			if( !query.oneSideJoin ) {
				final long est = recR.getEstNumTransformed();

				if( threshold != -1 && est > threshold ) {
					// if threshold is set (!= -1), index is built selectively for supporting hybrid algorithm
					continue;
				}
			}

			long expandStartTime;
			if( DEBUG.NaiveON ) {
				expandStartTime = System.nanoTime();
			}

			List<Record> expanded = null;

			if( DEBUG.NaiveON ) {
				expandTime += System.nanoTime() - expandStartTime;
			}

			if( !query.oneSideJoin ) {
				expanded = recR.expandAll();
				totalExpLength += expanded.size() * recR.getTokenCount();

				if( DEBUG.NaiveON ) {
					totalExp += expanded.size();
				}
			}

			long indexingStartTime = System.nanoTime();

			if( !query.oneSideJoin ) {
				for( final Record exp : expanded ) {
					naiveIndex.addExpaneded( exp, i );

					if( DEBUG.PrintNaiveIndexON ) {
						try {
							bw.write( recR.toString( query.tokenIndex ) + "(" + i + ") -> " + exp.toString( query.tokenIndex )
									+ "\n" );
						}
						catch( IOException e ) {
							e.printStackTrace();
						}
					}

					idxsize++;
				}
			}
			else {
				if( DEBUG.PrintNaiveIndexON ) {
					try {
						bw.write( recR.toString( query.tokenIndex ) + "(" + i + ") -> " + recR.toString( query.tokenIndex )
								+ " (hash: " + recR.hashCode() + ")" + "\n" );
					}
					catch( IOException e ) {
						e.printStackTrace();
					}
				}

				naiveIndex.addExpaneded( recR, i );
			}

			indexingTime += System.nanoTime() - indexingStartTime;
		}

		if( DEBUG.PrintNaiveIndexON ) {
			try {
				bw.close();
			}
			catch( IOException e ) {
				e.printStackTrace();
			}
		}

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

	public void addExpaneded( Record expanded, int recordId ) {
		ArrayList<Integer> list = idx.get( expanded );

		if( list == null ) {
			// new expression
			list = new ArrayList<Integer>( 5 );
			idx.putNonExist( expanded, list );
		}

		// If current list already contains current record as the last element, skip adding
		if( !list.isEmpty() && list.get( list.size() - 1 ) == recordId ) {
			return;
		}

		list.add( recordId );
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

	public List<IntegerPair> join( StatContainer stat, long threshold, boolean addStat, Query query ) {
		final List<IntegerPair> rslt = new ArrayList<>();
		final long starttime = System.nanoTime();

		Dataset searchedSet = query.searchedSet;

		for( int idxS = 0; idxS < searchedSet.size(); ++idxS ) {
			final Record recS = searchedSet.getRecord( idxS );

			if( !query.oneSideJoin ) {
				final long est = recS.getEstNumTransformed();
				if( threshold != -1 && est > threshold ) {
					continue;
				}
			}

			joinOneRecord( recS, rslt );
		}

		joinTime = System.nanoTime() - starttime;
		beta = joinTime / totalExp;

		if( DEBUG.NaiveON ) {
			if( addStat ) {
				stat.add( "Est_Join_1_expandTime", expandTime );
				stat.add( "Est_Join_2_searchTime", searchTime );
				stat.add( "Est_Join_3_totalTime", Double.toString( joinTime ) );

				Runtime runtime = Runtime.getRuntime();
				stat.add( "Mem_4_Joined", ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 );
				stat.add( "Stat_Counter_ExpandAll", Record.expandAllCount );
			}
		}

		if( addStat ) {
			stat.add( "Join_Naive_Result", rslt.size() );
		}

		return rslt;
	}

	public void joinOneRecord( Record recS, List<IntegerPair> rslt ) {
		// boolean debug = false;
		// if( recS.toString().equals( "david almeroth Road cicchetto ALASKA 48939 " ) ) {
		// debug = true;
		// }

		long expandStartTime = System.nanoTime();
		final List<Record> expanded = recS.expandAll();
		expandTime += System.nanoTime() - expandStartTime;

		totalExp += expanded.size();

		final Set<Integer> candidates = new HashSet<Integer>();

		long searchStartTime = System.nanoTime();
		for( final Record exp : expanded ) {

			System.out.println( exp.toString() + " " + exp.hashCode() );

			final List<Integer> overlapidx = idx.get( exp );
			if( overlapidx == null ) {
				continue;
			}

			// if( debug ) {
			// System.out.println( exp + " " + overlapidx );
			// }

			// candidates.add( overlapidx );
			for( Integer i : overlapidx ) {
				candidates.add( i );
			}
		}
		searchTime += System.nanoTime() - searchStartTime;

		for( final Integer idx : candidates ) {

			// DEBUG
			// if( debug ) {
			// System.out.println( "naive " + recS + " " + recS.getID() + " " + idx );
			// }

			rslt.add( new IntegerPair( recS.getID(), idx ) );
		}
	}

}
