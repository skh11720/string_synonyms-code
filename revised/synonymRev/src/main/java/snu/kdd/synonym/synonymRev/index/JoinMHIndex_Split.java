package snu.kdd.synonym.synonymRev.index;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
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

public class JoinMHIndex_Split {
	Object2ObjectOpenHashMap<IntegerPair, ArrayList<Map<QGram, List<Record>>>> joinMHIndexList;
	Object2IntOpenHashMap<Record> indexedCountList = new Object2IntOpenHashMap<Record>();

	int indexK;
	int qgramSize;
	int[] indexPosition;

	public JoinMHIndex_Split( int indexK, int qgramSize, Iterable<Record> recordList, Query query, StatContainer stat,
			int[] indexPosition ) {
		this.indexK = indexK;
		this.qgramSize = qgramSize;
		this.indexPosition = indexPosition;

		if( indexPosition.length != indexK ) {
			if( indexPosition.length < indexK ) {
				Util.printLog( "Using less index than given parameter K: " + indexPosition.length );
				this.indexK = indexPosition.length;
			}
			else {
				throw new RuntimeException( "The length of indexPosition should match indexK" );
			}
		}

		int maxPosition = 0;
		for( int idx : indexPosition ) {
			if( maxPosition < idx ) {
				maxPosition = idx;
			}
		}

		this.joinMHIndexList = new Object2ObjectOpenHashMap<IntegerPair, ArrayList<Map<QGram, List<Record>>>>();

		@SuppressWarnings( "unused" )
		long elements = 0;

		for( Record rec : recordList ) {
			// long recordStartTime = System.nanoTime();
			boolean debug = false;
			if( rec.getID() == 4145 ) {
				debug = true;
			}

			List<List<QGram>> availableQGrams = null;
			IntegerPair pair = null;
			if( !query.oneSideJoin ) {
				availableQGrams = rec.getQGrams( qgramSize, maxPosition + 1 );
				pair = new IntegerPair( rec.getMinTransLength(), rec.getMaxTransLength() );
			}
			else {
				availableQGrams = rec.getSelfQGrams( qgramSize, maxPosition + 1 );
				pair = new IntegerPair( rec.getTokenCount(), rec.getTokenCount() );
			}

			int indexedCount = 0;
			int[] range = rec.getTransLengths();
			for( int i = 0; i < indexPosition.length; i++ ) {
				int actual = indexPosition[ i ];

				if( range[ 0 ] > actual ) {
					indexedCount++;
				}
				indexedCountList.put( rec, indexedCount );
			}

			ArrayList<Map<QGram, List<Record>>> joinMHIndex = joinMHIndexList.get( pair );
			if( joinMHIndex == null ) {
				joinMHIndex = new ArrayList<Map<QGram, List<Record>>>();
				joinMHIndexList.put( pair, joinMHIndex );
				for( int i = 0; i < indexK; ++i ) {
					joinMHIndex.add( new WYK_HashMap<QGram, List<Record>>() );
				}
			}

			for( int i = 0; i < indexPosition.length; i++ ) {
				int actualIndex = indexPosition[ i ];
				if( availableQGrams.size() <= actualIndex ) {
					continue;
				}

				Map<QGram, List<Record>> map = joinMHIndex.get( i );

				for( QGram qgram : availableQGrams.get( actualIndex ) ) {
					if( debug ) {
						System.out.println( "qgram: " + qgram );
					}
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
			stat.add( "Stat_Index_Size", elements );
			System.out.println( "Index size : " + elements );

			// computes the statistics of the indexes
			// String indexStr = "";
			// for( int i = 0; i < indexK; ++i ) {
			// Map<QGram, List<Record>> ithidx = joinMHIndex.get( i );
			//
			// System.out.println( i + "th iIdx key-value pairs: " + ithidx.size() );
			//
			// // Statistics
			// int sum = 0;
			//
			// long singlelistsize = 0;
			// long count = 0;
			// // long sqsum = 0;
			// for( Map.Entry<QGram, List<Record>> entry : ithidx.entrySet() ) {
			// List<Record> list = entry.getValue();
			//
			// if( list.size() == 1 ) {
			// ++singlelistsize;
			// continue;
			// }
			// sum++;
			// count += list.size();
			// }
			//
			// if( DEBUG.JoinMHIndexON ) {
			// System.out.println( i + "th Single value list size : " + singlelistsize );
			// System.out.println( i + "th iIdx size(w/o 1) : " + count );
			// System.out.println( i + "th Rec per idx(w/o 1) : " + ( (double) count ) / sum );
			// // System.out.println( i + "th Sqsum : " + sqsum );
			// }
			//
			// long totalCount = count + singlelistsize;
			// int exp = 0;
			// while( totalCount / 1000 != 0 ) {
			// totalCount = totalCount / 1000;
			// exp++;
			// }
			//
			// if( exp == 1 ) {
			// indexStr = indexStr + totalCount + "k ";
			// }
			// else if( exp == 2 ) {
			// indexStr = indexStr + totalCount + "M ";
			// }
			// else {
			// indexStr = indexStr + totalCount + "G ";
			// }
			// }
			//
			// stat.add( "Stat_Index_Size_Per_Position", "\"" + indexStr + "\"" );
			// for( int i = 0; i < joinMHIndex.size(); i++ ) {
			// WYK_HashMap<QGram, List<Record>> index = (WYK_HashMap<QGram, List<Record>>) joinMHIndex.get( i );
			// stat.add( "Counter_Index_" + i + "_Get_Count", index.getCount );
			// stat.add( "Counter_Index_" + i + "_GetIter_Count", index.getIterCount );
			// stat.add( "Counter_Index_" + i + "_Put_Count", index.putCount );
			// stat.add( "Counter_Index_" + i + "_Resize_Count", index.resizeCount );
			// stat.add( "Counter_Index_" + i + "_Remove_Count", index.removeCount );
			// stat.add( "Counter_Index_" + i + "_RemoveIter_Count", index.removeIterCount );
			// stat.add( "Counter_Index_" + i + "_PutRemoved_Count", index.putRemovedCount );
			// stat.add( "Counter_Index_" + i + "_RemoveFound_Count", index.removeFoundCount );
			// }
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

			// if( recS.getID() == 4145 ) {
			// debug = true;
			// }

			Set<Record> candidates = new WYK_HashSet<Record>();

			List<List<QGram>> availableQGrams = recS.getQGrams( qgramSize, maxPosition + 1 );

			// long recordStartTime = System.nanoTime();
			int[] range = recS.getTransLengths();
			// if( debug ) {
			// System.out.println( "Range : " + range[ 0 ] + " " + range[ 1 ] );
			// }

			ArrayList<IntegerPair> rangeCandidateList = new ArrayList<IntegerPair>();

			ObjectIterator<IntegerPair> pairIter = joinMHIndexList.keySet().iterator();
			while( pairIter.hasNext() ) {
				IntegerPair pair = pairIter.next();

				if( StaticFunctions.overlap( pair.i1, pair.i2, range[ 0 ], range[ 1 ] ) ) {
					// if( debug ) {
					// System.out.println( "Cand: " + pair );
					// }
					rangeCandidateList.add( pair );
				}
			}

			for( int r = 0; r < rangeCandidateList.size(); r++ ) {
				IntegerPair pair = rangeCandidateList.get( r );
				ArrayList<Map<QGram, List<Record>>> joinMHIndex = joinMHIndexList.get( pair );

				ObjectOpenHashSet<Record> prevCandidate = null;
				for( int i = 0; i < indexK; ++i ) {
					int actualIndex = indexPosition[ i ];

					if( range[ 0 ] < actualIndex ) {

						// if( debug ) {
						// System.out.println( actualIndex + " skipped " + range[ 0 ] );
						// }
						continue;
					}

					Map<QGram, List<Record>> map = joinMHIndex.get( i );
					if( map.size() == 0 ) {
						continue;
					}

					candidateTimes[ i ].start();

					ObjectOpenHashSet<Record> ithCandidates = new ObjectOpenHashSet<Record>();

					for( QGram qgram : availableQGrams.get( actualIndex ) ) {
						// elements++;

						List<Record> list = map.get( qgram );
						if( list == null ) {
							++count_empty[ i ];
							continue;
						}
						cand_sum[ i ] += list.size();
						++count_cand[ i ];

						// if( debug ) {
						// System.out.println( "qgram: " + qgram );
						// System.out.println( "list: " + list );
						// }

						for( Record otherRecord : list ) {
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
								if( prevCandidate == null ) {
									ithCandidates.add( otherRecord );
								}
								else if( prevCandidate.contains( otherRecord ) ) {
									ithCandidates.add( otherRecord );
								}
							}
							else {
								lengthFiltered++;
							}
						}
						cand_sum_afterprune[ i ] += ithCandidates.size();
					}

					if( prevCandidate != null ) {
						prevCandidate.clear();
					}
					prevCandidate = ithCandidates;

					candidateTimes[ i ].stopQuiet();
				}
				count += candidates.size();

				// if( prevCandidate != null ) {
				// // TODO why null?
				candidates.addAll( prevCandidate );
				// }
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
