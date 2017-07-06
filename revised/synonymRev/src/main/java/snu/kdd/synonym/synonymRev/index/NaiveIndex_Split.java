package snu.kdd.synonym.synonymRev.index;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import snu.kdd.synonym.synonymRev.data.Dataset;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.tools.WYK_HashMap;

public class NaiveIndex_Split {
	public Int2ObjectOpenHashMap<WYK_HashMap<Record, ArrayList<Integer>>> idxList;

	public double alpha;
	public double beta;

	public double indexTime = 0;
	public double joinTime = 0;

	public double totalExp = 0;
	public double totalExpLength = 0;

	public double expandTime = 0;
	public double searchTime = 0;

	NaiveIndex_Split() {
		idxList = new Int2ObjectOpenHashMap<WYK_HashMap<Record, ArrayList<Integer>>>();
	}

	public static NaiveIndex_Split buildIndex( double avgTransformed, StatContainer stat, long threshold, boolean addStat,
			Query query ) {
		final long starttime = System.nanoTime();

		BufferedWriter bw = null;
		if( DEBUG.PrintNaiveIndexON ) {
			try {
				bw = new BufferedWriter( new FileWriter( "Naive_index.txt" ) );
			}
			catch( IOException e ) {
				e.printStackTrace();
			}
		}

		NaiveIndex_Split naiveIndex = new NaiveIndex_Split();

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
				if( DEBUG.JoinNaiveSkipTooMany ) {
					if( DEBUG.EstTooManyThreshold < recR.getEstNumTransformed() || recR.getEstNumTransformed() <= 0 ) {
						Util.printLog( "Rec " + recR.getID() + "(" + recR
								+ ") is skipped indexing due to too many transformed strings " + recR.getEstNumTransformed() );
						Rule[][] applicable = recR.getApplicableRules();
						for( int j = 0; j < applicable.length; j++ ) {
							Util.printLog( "Applicable Rule at " + j );
							Util.printLog( Arrays.toString( applicable[ j ] ) );
						}
						continue;
					}
				}
				expanded = recR.expandAll();
				totalExpLength += expanded.size() * recR.getTokenCount();

				if( DEBUG.NaiveON ) {
					totalExp += expanded.size();
				}
			}

			long indexingStartTime = System.nanoTime();

			if( !query.oneSideJoin ) {
				for( final Record exp : expanded ) {
					naiveIndex.addExpaneded( exp, i, recR.getMinTransLength() );

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

				naiveIndex.addExpaneded( recR, i, recR.getTokenCount() );
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

	public void addExpaneded( Record expanded, int recordId, int length ) {
		WYK_HashMap<Record, ArrayList<Integer>> idx = idxList.get( length );
		if( idx == null ) {
			idx = new WYK_HashMap<Record, ArrayList<Integer>>();

			idxList.put( length, idx );
		}

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
		ObjectIterator<Entry<WYK_HashMap<Record, ArrayList<Integer>>>> iter = idxList.int2ObjectEntrySet().iterator();

		long getCount = 0;
		long getIterCount = 0;
		long putCount = 0;
		long resizeCount = 0;
		long removeCount = 0;
		long removeIterCount = 0;
		long putRemovedCount = 0;
		long removeFoundCount = 0;

		while( iter.hasNext() ) {
			Entry<WYK_HashMap<Record, ArrayList<Integer>>> entry = iter.next();
			WYK_HashMap<Record, ArrayList<Integer>> idx = entry.getValue();
			getCount += idx.getCount;
			getIterCount += idx.getIterCount;
			putCount += idx.putCount;
			resizeCount += idx.resizeCount;
			removeCount += idx.removeCount;
			removeIterCount += idx.removeIterCount;
			putRemovedCount += idx.putRemovedCount;
			removeFoundCount += idx.removeFoundCount;
		}

		stat.add( prefix + "_Get_Count", getCount );
		stat.add( prefix + "_GetIter_Count", getIterCount );
		stat.add( prefix + "_Put_Count", putCount );
		stat.add( prefix + "_Resize_Count", resizeCount );
		stat.add( prefix + "_Remove_Count", removeCount );
		stat.add( prefix + "_RemoveIter_Count", removeIterCount );
		stat.add( prefix + "_PutRemoved_Count", putRemovedCount );
		stat.add( prefix + "_RemoveFound_Count", removeFoundCount );
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

			if( DEBUG.JoinNaiveSkipTooMany ) {
				if( DEBUG.EstTooManyThreshold < recS.getEstNumTransformed() || recS.getEstNumTransformed() <= 0 ) {
					Util.printLog( "Rec " + recS.getID() + "(" + recS
							+ ") is skipped joining due to too many transformed strings " + recS.getEstNumTransformed() );

					if( query.selfJoin ) {
						rslt.add( new IntegerPair( recS.getID(), recS.getID() ) );
					}

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
		long expandStartTime = System.nanoTime();
		final List<Record> expanded = recS.expandAll();
		expandTime += System.nanoTime() - expandStartTime;

		totalExp += expanded.size();

		final Set<Integer> candidates = new HashSet<Integer>();

		long searchStartTime = System.nanoTime();

		int[] ranges = recS.getTransLengths();

		for( int i = ranges[ 0 ]; i < ranges[ 1 ]; i++ ) {
			WYK_HashMap<Record, ArrayList<Integer>> idx = idxList.get( i );
			for( final Record exp : expanded ) {
				if( idx == null ) {
					continue;
				}

				final List<Integer> overlapidx = idx.get( exp );
				if( overlapidx == null ) {
					continue;
				}

				for( Integer index : overlapidx ) {
					candidates.add( index );
				}
			}
		}
		searchTime += System.nanoTime() - searchStartTime;

		for( final Integer idx : candidates ) {
			rslt.add( new IntegerPair( recS.getID(), idx ) );
		}
	}

}
