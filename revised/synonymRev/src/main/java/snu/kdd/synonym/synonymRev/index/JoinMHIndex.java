package snu.kdd.synonym.synonymRev.index;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StaticFunctions;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.tools.WYK_HashMap;
import snu.kdd.synonym.synonymRev.tools.WYK_HashSet;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class JoinMHIndex {
	ArrayList<Map<QGram, List<Record>>> joinMHIndex;
	Object2IntOpenHashMap<Record> indexedCountList;

	int indexK;
	int qgramSize;
	int[] indexPosition;

	public JoinMHIndex( int indexK, int qgramSize, Iterable<Record> indexedSet, Query query, StatContainer stat,
			int[] indexPosition, boolean addStat, boolean useIndexCount ) {

		this.indexK = indexK;
		this.qgramSize = qgramSize;
		this.indexPosition = indexPosition;

		if( indexPosition.length != indexK ) {
			throw new RuntimeException( "The length of indexPosition should match indexK" );
		}

		if( useIndexCount ) {
			indexedCountList = new Object2IntOpenHashMap<Record>();
		}

		int maxPosition = 0;
		for( int idx : indexPosition ) {
			if( maxPosition < idx ) {
				maxPosition = idx;
			}
		}

		this.joinMHIndex = new ArrayList<Map<QGram, List<Record>>>();

		@SuppressWarnings( "unused" )
		long elements = 0;

		for( int i = 0; i < indexK; ++i ) {
			joinMHIndex.add( new WYK_HashMap<QGram, List<Record>>() );
		}

		for( Record rec : indexedSet ) {
			// boolean debug = rec.getID() == 4145;

			// long recordStartTime = System.nanoTime();

			List<List<QGram>> availableQGrams = null;
			if( !query.oneSideJoin ) {
				availableQGrams = rec.getQGrams( qgramSize, maxPosition + 1 );
			}
			else {
				availableQGrams = rec.getSelfQGrams( qgramSize, maxPosition + 1 );
			}

			int indexedCount = 0;
			int[] range = rec.getTransLengths();

			if( useIndexCount ) {
				for( int i = 0; i < indexPosition.length; i++ ) {
					int actual = indexPosition[ i ];

					if( range[ 0 ] > actual ) {
						indexedCount++;
					}
					indexedCountList.put( rec, indexedCount );
				}
			}

			for( int i = 0; i < indexPosition.length; i++ ) {
				int actualIndex = indexPosition[ i ];
				if( availableQGrams.size() <= actualIndex ) {
					continue;
				}

				Map<QGram, List<Record>> map = joinMHIndex.get( i );

				for( QGram qgram : availableQGrams.get( actualIndex ) ) {
					// if( debug ) {
					// System.out.println( qgram + " " + actualIndex );
					// }
					List<Record> list = map.get( qgram );
					if( list == null ) {
						list = new ArrayList<Record>();
						map.put( qgram, list );
					}
					list.add( rec );
				}
				elements += availableQGrams.get( actualIndex ).size();
			}
		}

		if( DEBUG.JoinMHIndexON ) {
			if( addStat ) {
				stat.add( "Stat_Index_Size", elements );
				System.out.println( "Index size : " + elements );

				// computes the statistics of the indexes
				String indexStr = "";
				for( int i = 0; i < indexK; ++i ) {
					Map<QGram, List<Record>> ithidx = joinMHIndex.get( i );

					System.out.println( i + "th iIdx key-value pairs: " + ithidx.size() );

					// Statistics
					int sum = 0;

					long singlelistsize = 0;
					long count = 0;
					// long sqsum = 0;
					for( Map.Entry<QGram, List<Record>> entry : ithidx.entrySet() ) {
						List<Record> list = entry.getValue();

						if( list.size() == 1 ) {
							++singlelistsize;
							continue;
						}
						sum++;
						count += list.size();
					}

					if( DEBUG.JoinMHIndexON ) {
						System.out.println( i + "th Single value list size : " + singlelistsize );
						System.out.println( i + "th iIdx size(w/o 1) : " + count );
						System.out.println( i + "th Rec per idx(w/o 1) : " + ( (double) count ) / sum );
						// System.out.println( i + "th Sqsum : " + sqsum );
					}

					long totalCount = count + singlelistsize;
					int exp = 0;
					while( totalCount / 1000 != 0 ) {
						totalCount = totalCount / 1000;
						exp++;
					}

					if( exp == 1 ) {
						indexStr = indexStr + totalCount + "k ";
					}
					else if( exp == 2 ) {
						indexStr = indexStr + totalCount + "M ";
					}
					else {
						indexStr = indexStr + totalCount + "G ";
					}
				}

				if( DEBUG.JoinMHIndexON ) {
					stat.add( "Stat_Index_Size_Per_Position", "\"" + indexStr + "\"" );
					for( int i = 0; i < joinMHIndex.size(); i++ ) {
						WYK_HashMap<QGram, List<Record>> index = (WYK_HashMap<QGram, List<Record>>) joinMHIndex.get( i );
						stat.add( "Counter_Index_" + i + "_Get_Count", index.getCount );
						stat.add( "Counter_Index_" + i + "_GetIter_Count", index.getIterCount );
						stat.add( "Counter_Index_" + i + "_Put_Count", index.putCount );
						stat.add( "Counter_Index_" + i + "_Resize_Count", index.resizeCount );
						stat.add( "Counter_Index_" + i + "_Remove_Count", index.removeCount );
						stat.add( "Counter_Index_" + i + "_RemoveIter_Count", index.removeIterCount );
						stat.add( "Counter_Index_" + i + "_PutRemoved_Count", index.putRemovedCount );
						stat.add( "Counter_Index_" + i + "_RemoveFound_Count", index.removeFoundCount );
					}
				}
			}
		}
	}

	public void joinOneRecordForSplit( Record recS, List<List<QGram>> availableQGrams, Query query, Validator checker,
			ArrayList<IntegerPair> rslt ) {
		// this function is for the splitted data sets only -> qgrams are previously computed and
		// length filtering is not applied here (already applied by the function calling this function)

		// boolean debug = recS.getID() == 4145;
		// long recordStartTime = System.nanoTime();

		ObjectOpenHashSet<Record> prevCandidate = null;
		for( int i = 0; i < indexK; ++i ) {
			int actualIndex = indexPosition[ i ];

			ObjectOpenHashSet<Record> ithCandidates = new ObjectOpenHashSet<Record>();

			Map<QGram, List<Record>> map = joinMHIndex.get( i );

			for( QGram qgram : availableQGrams.get( actualIndex ) ) {
				// if( debug ) {
				// System.out.println( "Q " + qgram + " " + actualIndex );
				// }

				// elements++;
				List<Record> list = map.get( qgram );
				if( list == null ) {
					continue;
				}

				for( Record otherRecord : list ) {
					// if( debug ) {
					// System.out.println( "record: " + otherRecord );
					// }

					if( prevCandidate == null ) {
						ithCandidates.add( otherRecord );
					}
					else if( prevCandidate.contains( otherRecord ) ) {
						ithCandidates.add( otherRecord );
					}
				}
			}

			if( prevCandidate != null ) {
				prevCandidate.clear();
			}
			prevCandidate = ithCandidates;
		}

		for( Record recR : prevCandidate ) {
			int compare = checker.isEqual( recS, recR );
			if( compare >= 0 ) {
				rslt.add( new IntegerPair( recS.getID(), recR.getID() ) );
			}
		}
	}

	public ArrayList<IntegerPair> join( StatContainer stat, Query query, Validator checker ) {
		int maxPosition = 0;
		for( int idx : indexPosition ) {
			if( maxPosition < idx ) {
				maxPosition = idx;
			}
		}

		ArrayList<IntegerPair> rslt = new ArrayList<IntegerPair>();

		long count = 0;
		@SuppressWarnings( "unused" )
		long lengthFiltered = 0;

		long cand_sum[] = new long[ indexK ];
		long cand_sum_afterprune[] = new long[ indexK ];
		int count_cand[] = new int[ indexK ];
		int count_empty[] = new int[ indexK ];

		StopWatch equivTime = StopWatch.getWatchStopped( "Result_3_2_1_Equiv_Checking_Time" );
		StopWatch[] candidateTimes = new StopWatch[ indexK ];
		for( int i = 0; i < indexK; i++ ) {
			candidateTimes[ i ] = StopWatch.getWatchStopped( "Result_3_2_2_Cand_" + i + " Time" );
		}

		for( int sid = 0; sid < query.searchedSet.size(); sid++ ) {
			// boolean debug = false;

			Record recS = query.searchedSet.getRecord( sid );
			Set<Record> candidates = new WYK_HashSet<Record>();

			// if( recS.getID() == 94118 ) {
			// debug = true;
			// }

			Object2IntOpenHashMap<Record> candidatesCount = new Object2IntOpenHashMap<Record>();
			candidatesCount.defaultReturnValue( -1 );

			List<List<QGram>> availableQGrams = recS.getQGrams( qgramSize, maxPosition + 1 );

			// long recordStartTime = System.nanoTime();
			int[] range = recS.getTransLengths();
			for( int i = 0; i < indexK; ++i ) {
				int actualIndex = indexPosition[ i ];
				if( range[ 0 ] <= actualIndex ) {
					continue;
				}

				candidateTimes[ i ].start();

				ObjectOpenHashSet<Record> ithCandidates = new ObjectOpenHashSet<Record>();

				Map<QGram, List<Record>> map = joinMHIndex.get( i );

				for( QGram qgram : availableQGrams.get( actualIndex ) ) {
					// if( debug ) {
					// System.out.println( "Q " + qgram + " " + actualIndex );
					// }

					// elements++;
					List<Record> list = map.get( qgram );
					if( list == null ) {
						++count_empty[ i ];
						continue;
					}
					cand_sum[ i ] += list.size();
					++count_cand[ i ];
					for( Record otherRecord : list ) {
						// if( debug ) {
						// System.out.println( "record: " + otherRecord );
						// }

						int[] otherRange = null;

						if( query.oneSideJoin ) {
							otherRange = new int[ 2 ];
							otherRange[ 0 ] = otherRecord.getTokenCount();
							otherRange[ 1 ] = otherRecord.getTokenCount();
						}
						else {
							otherRange = otherRecord.getTransLengths();
						}

						if( StaticFunctions.overlap( otherRange[ 0 ], otherRange[ 1 ], range[ 0 ], range[ 1 ] ) ) {
							// length filtering

							ithCandidates.add( otherRecord );
						}
						else {
							lengthFiltered++;
						}
					}
					cand_sum_afterprune[ i ] += candidatesCount.size();
				}

				for( Record otherRecord : ithCandidates ) {
					int candCount = candidatesCount.getInt( otherRecord );
					if( candCount == -1 ) {
						candidatesCount.put( otherRecord, 1 );
					}
					else {
						candidatesCount.put( otherRecord, candCount + 1 );
					}
				}

				candidateTimes[ i ].stopQuiet();
			}
			count += candidates.size();

			ObjectIterator<Entry<Record>> iter = candidatesCount.object2IntEntrySet().iterator();
			while( iter.hasNext() ) {
				Entry<Record> entry = iter.next();
				Record record = entry.getKey();
				int recordCount = entry.getIntValue();

				if( indexedCountList.getInt( record ) <= recordCount || indexedCountList.getInt( recS ) <= recordCount ) {
					candidates.add( record );
				}
			}

			equivTime.start();
			for( Record recR : candidates ) {
				int compare = checker.isEqual( recS, recR );
				if( compare >= 0 ) {
					rslt.add( new IntegerPair( recS.getID(), recR.getID() ) );
				}
			}
			equivTime.stopQuiet();
		}

		stat.add( "Stat_Equiv_Comparison", count );

		if( DEBUG.JoinMHIndexON ) {
			for( int i = 0; i < indexK; ++i ) {
				Util.printLog( "Avg candidates(w/o empty) : " + cand_sum[ i ] + "/" + count_cand[ i ] );
				Util.printLog( "Avg candidates(w/o empty, after prune) : " + cand_sum_afterprune[ i ] + "/" + count_cand[ i ] );
				Util.printLog( "Empty candidates : " + count_empty[ i ] );
			}

			Util.printLog( "comparisions : " + count );

			stat.addMemory( "Mem_4_Joined" );

			stat.add( "Counter_Final_1_HashCollision", WYK_HashSet.collision );
			stat.add( "Counter_Final_1_HashResize", WYK_HashSet.resize );

			stat.add( "Counter_Final_2_MapCollision", WYK_HashMap.collision );
			stat.add( "Counter_Final_2_MapResize", WYK_HashMap.resize );

			stat.add( "Stat_Length_Filtered", lengthFiltered );
			stat.add( equivTime );

			String candTimeStr = "";
			for( int i = 0; i < indexK; i++ ) {
				candidateTimes[ i ].printTotal();

				candTimeStr = candTimeStr + ( candidateTimes[ i ].getTotalTime() ) + " ";
			}
			stat.add( "Stat_Candidate_Times_Per_Index", candTimeStr );
		}
		return rslt;
	}
}
