package snu.kdd.synonym.synonymRev.index;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	ArrayList<Map<QGram, List<Record>>> idx;

	int indexK;
	int qgramSize;
	int[] indexPosition;

	public JoinMHIndex( int indexK, int qgramSize, Query query, StatContainer stat, int[] indexPosition ) {
		this.indexK = indexK;
		this.qgramSize = qgramSize;
		this.indexPosition = indexPosition;

		if( indexPosition.length != indexK ) {
			throw new RuntimeException( "The length of indexPosition should match indexK" );
		}

		int maxPosition = 0;
		for( int idx : indexPosition ) {
			if( maxPosition < idx ) {
				maxPosition = idx;
			}
		}

		@SuppressWarnings( "unused" )
		long elements = 0;

		for( int i = 0; i < indexK; ++i ) {
			idx.add( new WYK_HashMap<QGram, List<Record>>() );
		}

		for( Record rec : query.indexedSet.get() ) {
			// long recordStartTime = System.nanoTime();

			List<List<QGram>> availableQGrams = null;
			if( !query.oneSideJoin ) {
				availableQGrams = rec.getQGrams( qgramSize, maxPosition + 1 );
			}
			else {
				availableQGrams = rec.getSelfQGrams( qgramSize, maxPosition + 1 );
			}

			for( int i = 0; i < indexPosition.length; i++ ) {
				Map<QGram, List<Record>> map = idx.get( i );
				int actualIndex = indexPosition[ i ];
				for( QGram qgram : availableQGrams.get( actualIndex ) ) {
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

		if( DEBUG.JoinMHIndexOn ) {
			stat.add( "Stat_Index_Size", elements );
			System.out.println( "Index size : " + elements );

			// computes the statistics of the indexes
			String indexStr = "";
			for( int i = 0; i < indexK; ++i ) {
				Map<QGram, List<Record>> ithidx = idx.get( i );

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

				if( DEBUG.JoinMHIndexOn ) {
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

			if( DEBUG.JoinMHIndexOn ) {
				stat.add( "Stat_Index_Size_Per_Position", "\"" + indexStr + "\"" );
				for( int i = 0; i < idx.size(); i++ ) {
					WYK_HashMap<QGram, List<Record>> index = (WYK_HashMap<QGram, List<Record>>) idx.get( i );
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
		long cand_sum_afterunion[] = new long[ indexK ];
		int count_cand[] = new int[ indexK ];
		int count_empty[] = new int[ indexK ];

		StopWatch equivTime = StopWatch.getWatchStopped( "Result_3_2_1_Equiv_Checking_Time" );
		StopWatch[] candidateTimes = new StopWatch[ indexK ];
		for( int i = 0; i < indexK; i++ ) {
			candidateTimes[ i ] = StopWatch.getWatchStopped( "Result_3_2_2_Cand_" + i + " Time" );
		}

		try {
			for( int sid = 0; sid < query.searchedSet.size(); sid++ ) {

				Record recS = query.searchedSet.getRecord( sid );
				Set<Record> candidates = new WYK_HashSet<Record>();

				// List<List<Record>> candidatesList = new ArrayList<List<Record>>();
				List<List<QGram>> availableQGrams = recS.getQGrams( qgramSize, maxPosition + 1 );

				// long recordStartTime = System.nanoTime();
				int[] range = recS.getTransLengths();
				int boundary = Math.min( range[ 0 ], indexK );
				for( int i = 0; i < boundary; ++i ) {
					candidateTimes[ i ].start();

					// List<List<Record>> ithCandidates = new ArrayList<List<Record>>();

					Map<QGram, List<Record>> map = idx.get( i );

					Set<Record> candidatesAppeared = new WYK_HashSet<Record>();

					int actualIndex = indexPosition[ i ];

					for( QGram qgram : availableQGrams.get( actualIndex ) ) {
						// elements++;
						List<Record> list = map.get( qgram );
						if( list == null ) {
							++count_empty[ i ];
							continue;
						}
						cand_sum[ i ] += list.size();
						++count_cand[ i ];
						for( Record otherRecord : list ) {

							int[] otherRange = otherRecord.getTransLengths();
							if( StaticFunctions.overlap( otherRange[ 0 ], otherRange[ 1 ], range[ 0 ], range[ 1 ] ) ) {
								// length filtering
								if( i == 0 ) {
									candidatesAppeared.add( otherRecord );
								}
								else if( candidates.contains( otherRecord ) ) {
									// signature filtering
									candidatesAppeared.add( otherRecord );
								}
							}
							else {
								lengthFiltered++;
							}
						}
						cand_sum_afterprune[ i ] += candidatesAppeared.size();
					}
					candidates.clear();
					Set<Record> temp = candidatesAppeared;
					candidatesAppeared = candidates;
					candidates = temp;

					// if( i == 0 ) {
					// Iterator<Record> itr = candidates.iterator();
					// while( itr.hasNext() ) {
					// Record rec = itr.next();
					// if( !recS.shareLastToken( rec ) ) {
					// itr.remove();
					// lastTokenFiltered++;
					// }
					// }
					// }

					cand_sum_afterunion[ i ] += candidates.size();

					candidateTimes[ i ].stopQuiet();
				}
				// long recordTime = System.nanoTime() - recordStartTime;

				count += candidates.size();

				// DEBUG
				// if( candidates.size() != 1 ) {
				// System.out.println( candidates.size() );
				// for( int i = 0; i < boundary; i++ ) {
				// for( IntegerPair twogram : available2Grams.get( i ) ) {
				// System.out.println( twogram.toStrString() );
				// }
				// }
				// }

				equivTime.start();
				for( Record recR : candidates ) {
					int compare = checker.isEqual( recS, recR );
					if( compare >= 0 ) {
						rslt.add( new IntegerPair( recS.getID(), recR.getID() ) );
					}
				}
				equivTime.stopQuiet();

				// bw.write( recordTime + " " );
				// bw.write( ( elements - debug_elements ) + " " );
				// bw.write( ( getGCCount() - debug_gcCount ) + " " );
				// bw.write( candidates.size() + " " );
				// bw.write( "\n" );
				// debug_elements = elements;
				// debug_gcCount = getGCCount();
			}
			// bw.close();
		}
		catch( Exception e ) {
			e.printStackTrace();
		}

		stat.add( "Stat_Equiv_Comparison", count );

		if( DEBUG.JoinMHOn ) {
			// stat.add( "Last Token Filtered", lastTokenFiltered );
			for( int i = 0; i < indexK; ++i ) {
				Util.printLog( "Avg candidates(w/o empty) : " + cand_sum[ i ] + "/" + count_cand[ i ] );
				Util.printLog( "Avg candidates(w/o empty, after prune) : " + cand_sum_afterprune[ i ] + "/" + count_cand[ i ] );
				Util.printLog( "Avg candidates(w/o empty, after union) : " + cand_sum_afterunion[ i ] + "/" + count_cand[ i ] );
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
