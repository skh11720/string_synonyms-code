package snu.kdd.synonym.synonymRev.index;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.synonymRev.algorithm.misc.EstimationTest;
import snu.kdd.synonym.synonymRev.data.Dataset;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.tools.WYK_HashMap;

public class NaiveIndex {
	public WYK_HashMap<Record, ArrayList<Integer>> idx;
	protected final boolean isSelfJoin;
	protected final String filenamePrefix = "NaiveIndex";

	public double alpha;
	public double beta;

	public long indexTime = 0;
	public long joinTime = 0;
	public long idxsize = 0;
	public long indexingTime = 0;

	public double totalExp = 0;
	public double totalExpLength = 0;

	public long expandTime = 0;
	public long searchTime = 0;

	public int skippedCount = 0;
	protected final long threshold;
	
	public long sumTransLenS = 0;
	public long sumLenT = 0;

	public NaiveIndex( Dataset indexedSet, Query query, StatContainer stat, boolean addStat, long threshold, double avgTransformed ) {
		isSelfJoin = query.selfJoin;

		this.threshold = threshold;
		final long starttime = System.nanoTime();
		int initialSize = (int) ( indexedSet.size() * avgTransformed / 2 );

		if ( initialSize > 10000 ) initialSize = 10000;
		if ( initialSize < 10 ) initialSize = 10;

		if( DEBUG.NaiveON ) {
			stat.add( "Auto_Hash_Initial_Size ", initialSize );
		}

		BufferedWriter bw = null;
		if( DEBUG.PrintNaiveIndexON ) {
			try {
				bw = new BufferedWriter( new FileWriter( "./tmp/"+filenamePrefix+".txt" ) );
			}
			catch( IOException e ) {
				e.printStackTrace();
			}
		}

		idx = new WYK_HashMap<Record, ArrayList<Integer>>( initialSize );

		totalExpLength = 0;
		totalExp = 0;
		idxsize = 0;
		indexingTime = 0;
		expandTime = 0;

		for( int i = 0; i < indexedSet.size(); ++i ) {
			final Record recR = indexedSet.getRecord( i );

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
					addExpaneded( exp, i );

					if( DEBUG.PrintNaiveIndexON ) {
						try {
							bw.write( recR.getID() + " -> " + exp.toString( query.tokenIndex ) + "\n" );
						}
						catch( IOException e ) {
							e.printStackTrace();
						}
					}

					++idxsize;
				}
			}
			else {
				if( DEBUG.PrintNaiveIndexON ) {
					try {
						bw.write( recR.getID() + " -> " + recR.toString( query.tokenIndex ) + " (hash: " + recR.hashCode() + ")"
								+ "\n" );
					}
					catch( IOException e ) {
						e.printStackTrace();
					}
				}
				totalExpLength += recR.getTokenCount();

				addExpaneded( recR, i );
			}

			indexingTime += System.nanoTime() - indexingStartTime;
			sumLenT += recR.size();
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

		indexTime = System.nanoTime() - starttime;
		alpha = indexTime / totalExpLength;
	}

	@Deprecated
	public void addStat( StatContainer stat, boolean addStat ) {
		if( addStat ) {
			if( DEBUG.NaiveON ) {
				// stat.add( "Stat_Size_Indexed_Records", count );
				stat.add( "Stat_Size_Total_Index", idxsize );

				stat.add( "Est_Index_1_expSize", Double.toString( totalExp ) );
				// stat.add( "Est_Index_1_expSizeEstimated", estimatedExpSize );
				// stat.add( "Est_Index_1_executeTimeRatio", Double.toString( alpha ) );
				stat.add( "Est_Index_1_expandTime", expandTime );

				stat.add( "Est_Index_2_idxSize", idxsize );
				stat.add( "Est_Index_2_indexingTime", indexingTime );

//				naiveIndex.addStat( stat, "Counter_Index" );
				stat.add( "Est_Index_2_totalTime", Double.toString( indexTime ) );

				stat.add( "Est_Index_3_expandTimesLength", Double.toString( totalExpLength ) );
				stat.add( "Est_Index_3_expandTimePerETL", Double.toString( expandTime / totalExpLength ) );
				// stat.add( "Est_Index_3_timePerETL", Double.toString( duration / expandTimesLength ) );

				Runtime runtime = Runtime.getRuntime();
				stat.add( "Mem_3_BuildIndex", ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 );
			}
			Util.printGCStats( stat, "Stat_Index" );
		}
		else {
			if( DEBUG.SampleStatON ) {
				System.out.println( "[Alpha] " + alpha );
				System.out.println( "[Alpha] IndexTime " + indexTime );
				System.out.println( "[Alpha] totalExpLength " + totalExpLength );
			}
			if( DEBUG.PrintEstimationON ) {
				BufferedWriter bwEstimation = EstimationTest.getWriter();
				try {
					bwEstimation.write( "[Alpha] " + alpha );
					bwEstimation.write( " IndexTime " + indexTime );
					bwEstimation.write( " totalExpLength " + totalExpLength + "\n" );
				}
				catch( Exception e ) {
					e.printStackTrace();
				}
			}
		}
	}

	protected void addExpaneded( Record expanded, int recordId ) {
		ArrayList<Integer> list = idx.get( expanded );

		if( list == null ) {
			// new expression
			list = new ArrayList<Integer>( 5 );
			if( expanded.getID() == -1 ) {
				idx.putNonExist( expanded, list );
			}
			else {
				idx.putNonExist( new Record( expanded.getTokensArray() ), list );
			}
		}

		// If current list already contains current record as the last element, skip adding
		if( !list.isEmpty() && list.get( list.size() - 1 ) == recordId ) {
			return;
		}

		list.add( recordId );
	}
	
//	public void addStat( StatContainer stat, String prefix ) {
//		stat.add( prefix + "_Get_Count", WYK_HashMap.getCount );
//		stat.add( prefix + "_GetIter_Count", WYK_HashMap.getIterCount );
//		stat.add( prefix + "_Put_Count", WYK_HashMap.putCount );
//		stat.add( prefix + "_Resize_Count", WYK_HashMap.resizeCount );
//		stat.add( prefix + "_Remove_Count", WYK_HashMap.removeCount );
//		stat.add( prefix + "_RemoveIter_Count", WYK_HashMap.removeIterCount );
//		stat.add( prefix + "_PutRemoved_Count", WYK_HashMap.putRemovedCount );
//		stat.add( prefix + "_RemoveFound_Count", WYK_HashMap.removeFoundCount );
//	}

	public Set<IntegerPair> join( Query query, StatContainer stat, boolean addStat ) {
		final Set<IntegerPair> rslt = new ObjectOpenHashSet<>();
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
//						rslt.add( new IntegerPair( recS.getID(), recS.getID() ) );
						AlgorithmTemplate.addSeqResult( recS, recS, rslt, true );
					}
					skippedCount++;

					continue;
				}
			}
			
//			if ( recS.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) continue;

			joinOneRecord( recS, rslt );
		}

		joinTime = System.nanoTime() - starttime;
		beta = joinTime / totalExp;

		stat.add( "Join_Naive_Result", rslt.size() );
		if (addStat) addStatAfterJoin(stat);
		return rslt;
	}

	public void joinOneRecord( Record recS, Set<IntegerPair> rslt ) {
		long expandStartTime = System.nanoTime();
		final List<Record> expanded = recS.expandAll();
		expandTime += System.nanoTime() - expandStartTime;

		totalExp += expanded.size();

		final Set<Integer> candidates = new HashSet<Integer>();

		long searchStartTime = System.nanoTime();
		for( final Record exp : expanded ) {

			final List<Integer> overlapidx = idx.get( exp );
			sumTransLenS += exp.size();

			if( overlapidx == null ) {
				continue;
			}

			for( Integer i : overlapidx ) {
				candidates.add( i );
			}
			
		}
		for( final Integer idx : candidates ) {
//			rslt.add( new IntegerPair( recS.getID(), idx ) );
			AlgorithmTemplate.addSeqResult( recS, idx, rslt, isSelfJoin );
		}

		searchTime += System.nanoTime() - searchStartTime;
	}
	
	public void addStatAfterJoin( StatContainer stat ) {
		if( DEBUG.NaiveON ) {
			stat.add( "Est_Join_1_expandTime", expandTime );
			stat.add( "Est_Join_2_searchTime", searchTime );
			stat.add( "Est_Join_3_totalTime", Double.toString( joinTime ) );

			Runtime runtime = Runtime.getRuntime();
			stat.add( "Mem_4_Joined", ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 );
			stat.add( "Stat_Counter_ExpandAll", Record.expandAllCount );
		}
		else {
			if( DEBUG.SampleStatON ) {
				System.out.println( "[Beta] " + beta );
				System.out.println( "[Beta] JoinTime " + joinTime );
				System.out.println( "[Beta] TotalExp " + totalExp );
			}

			if( DEBUG.PrintEstimationON ) {
				BufferedWriter bwEstimation = EstimationTest.getWriter();
				try {
					bwEstimation.write( "[Beta] " + beta );
					bwEstimation.write( " JoinTime " + joinTime );
					bwEstimation.write( " TotalExp " + totalExp + "\n" );
				}
				catch( Exception e ) {
					e.printStackTrace();
				}
			}
		}
	}
}
