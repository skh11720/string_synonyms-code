package snu.kdd.synonym.algorithm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mine.Record;
import snu.kdd.synonym.data.DataInfo;
import snu.kdd.synonym.tools.Param;
import snu.kdd.synonym.tools.StatContainer;
import snu.kdd.synonym.tools.StopWatch;
import snu.kdd.synonym.tools.Util;
import tools.DEBUG;
import tools.IntIntRecordTriple;
import tools.IntegerPair;
import tools.MinPositionQueue;
import tools.QGram;
import tools.StaticFunctions;
import tools.WYK_HashMap;
import tools.WYK_HashSet;
import validator.Validator;

public class JoinBK_QL extends AlgorithmTemplate {
	// RecordIDComparator idComparator;

	public int maxIndexLength = 3;
	public int qgramSize = 2;

	static Validator checker;

	/**
	 * Key: twogram<br/>
	 * Value IntervalTree Key: length of record (min, max)<br/>
	 * Value IntervalTree Value: record
	 */

	List<Map<QGram, List<IntIntRecordTriple>>> idx;

	public JoinBK_QL( String rulefile, String Rfile, String Sfile, String outFile, DataInfo dataInfo, boolean joinOneSide,
			StatContainer stat ) throws IOException {
		super( rulefile, Rfile, Sfile, outFile, dataInfo, joinOneSide, stat );
	}

	@Override
	public void run( String[] args ) {
		// System.out.println( Arrays.toString( args ) );
		Param params = Param.parseArgs( args, stat );

		maxIndexLength = params.getMaxIndex();
		qgramSize = params.getQGramSize();

		// Setup parameters
		checker = params.getValidator();

		run();

		Validator.printStats();
	}

	public void run() {
		StopWatch stepTime = null;
		StopWatch runTime = null;

		if( DEBUG.JoinMHOn ) {
			stepTime = StopWatch.getWatchStarted( "Result_2_Preprocess_Total_Time" );
		}

		preprocess( false, maxIndexLength, false );

		if( DEBUG.JoinMHOn ) {
			stat.add( "Mem_2_Preprocessed", ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 );
			stepTime.stopAndAdd( stat );
			stepTime.resetAndStart( "Result_3_1_Index_Building_Time" );
			runTime = StopWatch.getWatchStarted( "Result_3_Run_Time" );
		}

		int[] indexPosition = estimateIndexPosition( maxIndexLength );
		buildIndex( indexPosition );

		if( DEBUG.JoinMHOn ) {
			stat.add( "Mem_3_BuildIndex", ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 );
			stepTime.stopAndAdd( stat );
			stepTime.resetAndStart( "Result_3_2_Join_Time" );
		}

		ArrayList<IntegerPair> rslt = join( indexPosition );

		if( DEBUG.JoinMHOn ) {
			stat.add( "Mem_4_Joined", ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 );
			stepTime.stopAndAdd( stat );

			runTime.stopAndAdd( stat );
			System.out.println( "Result " + rslt.size() );
			stepTime.resetAndStart( "Result_4_Write_Time" );
		}

		writeResult( rslt );

		if( DEBUG.JoinMHOn ) {
			stepTime.stopAndAdd( stat );
		}
	}

	private int[] estimateIndexPosition( int maxIndexLength ) {
		int[] indexPosition = new int[ maxIndexLength ];
		StopWatch estimateIndex = StopWatch.getWatchStarted( "Result_3_1_1_Index_Count_Time" );

		int minimumSize = 5;
		int[] count = new int[ minimumSize ];
		for( Record rec : tableSearched ) {
			List<Integer> qgrams = rec.getQGramCount( qgramSize, minimumSize + 1 );

			for( int i = 0; i < minimumSize; i++ ) {
				count[ i ] += qgrams.get( i );
			}
		}

		MinPositionQueue mpq = new MinPositionQueue( maxIndexLength );

		for( int i = 0; i < minimumSize; i++ ) {
			System.out.println( "Index " + i + " " + count[ i ] );
			mpq.add( i, count[ i ] );
		}

		int i = maxIndexLength - 1;
		while( !mpq.isEmpty() ) {
			indexPosition[ i ] = mpq.pollIndex();
			i--;
		}

		StringBuilder bld = new StringBuilder();
		for( i = 0; i < indexPosition.length; i++ ) {
			bld.append( indexPosition[ i ] );
			bld.append( " " );
		}

		stat.add( "Auto_BestPosition", bld.toString() );
		stat.add( estimateIndex );
		return indexPosition;
	}

	private void buildIndex( int[] indexPosition ) {

		int maxPosition = 0;
		for( int idx : indexPosition ) {
			if( maxPosition < idx ) {
				maxPosition = idx;
			}
		}

		try {
			// BufferedWriter bw = new BufferedWriter( new FileWriter( "Debug_est.txt" ) );
			// long debug_elements = 0;
			// long debug_gcCount = getGCCount();
			@SuppressWarnings( "unused" )
			long elements = 0;
			// Build an index

			idx = new ArrayList<Map<QGram, List<IntIntRecordTriple>>>();
			for( int i = 0; i < maxIndexLength; ++i ) {
				idx.add( new WYK_HashMap<QGram, List<IntIntRecordTriple>>() );
			}

			for( Record rec : tableIndexed ) {
				// long recordStartTime = System.nanoTime();

				int[] range = rec.getCandidateLengths( rec.size() - 1 );

				List<List<QGram>> availableQGrams = null;
				if( !oneSideJoin ) {
					availableQGrams = rec.getQGrams( qgramSize, maxPosition + 1 );
				}
				else {
					availableQGrams = rec.getSelfQGrams( qgramSize, maxPosition + 1 );
				}

				for( int i = 0; i < indexPosition.length; i++ ) {
					Map<QGram, List<IntIntRecordTriple>> map = idx.get( i );
					int actualIndex = indexPosition[ i ];
					for( QGram qgram : availableQGrams.get( actualIndex ) ) {
						List<IntIntRecordTriple> list = map.get( qgram );
						if( list == null ) {
							list = new ArrayList<IntIntRecordTriple>();
							map.put( qgram, list );
						}
						list.add( new IntIntRecordTriple( range[ 0 ], range[ 1 ], rec ) );
					}
					elements += availableQGrams.get( actualIndex ).size();
				}
			}

			// bw.close();

			if( DEBUG.JoinMHOn ) {
				stat.add( "Stat_Index_Size", elements );
				System.out.println( "Index size : " + elements );

				// computes the statistics of the indexes
				String indexStr = "";
				for( int i = 0; i < maxIndexLength; ++i ) {
					Map<QGram, List<IntIntRecordTriple>> ithidx = idx.get( i );

					System.out.println( i + "th iIdx key-value pairs: " + ithidx.size() );

					// Statistics
					int sum = 0;

					long singlelistsize = 0;
					long count = 0;
					// long sqsum = 0;
					for( Map.Entry<QGram, List<IntIntRecordTriple>> entry : ithidx.entrySet() ) {
						List<IntIntRecordTriple> list = entry.getValue();

						// bw.write( "Key " + Record.strlist.get( entry.getKey().i1 ) + " " + Record.strlist.get( entry.getKey().i2 )
						// + "\n" );
						// for( IntIntRecordTriple triple : list ) {
						// bw.write( triple.toString() + "\n" );
						// }

						// sqsum += list.size() * list.size();
						if( list.size() == 1 ) {
							++singlelistsize;
							continue;
						}
						sum++;
						count += list.size();
					}

					if( DEBUG.JoinMHDetailOn ) {
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

				if( DEBUG.JoinMHDetailOn ) {
					stat.add( "Stat_Index_Size_Per_Position", "\"" + indexStr + "\"" );
					for( int i = 0; i < idx.size(); i++ ) {
						WYK_HashMap<QGram, List<IntIntRecordTriple>> index = (WYK_HashMap<QGram, List<IntIntRecordTriple>>) idx
								.get( i );
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
		catch( Exception e ) {
			e.printStackTrace();
		}
	}

	private ArrayList<IntegerPair> join( int[] indexPosition ) {
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

		long cand_sum[] = new long[ maxIndexLength ];
		long cand_sum_afterprune[] = new long[ maxIndexLength ];
		long cand_sum_afterunion[] = new long[ maxIndexLength ];
		int count_cand[] = new int[ maxIndexLength ];
		int count_empty[] = new int[ maxIndexLength ];

		StopWatch equivTime = StopWatch.getWatchStopped( "Result_3_2_1_Equiv_Checking_Time" );
		StopWatch[] candidateTimes = new StopWatch[ maxIndexLength ];
		for( int i = 0; i < maxIndexLength; i++ ) {
			candidateTimes[ i ] = StopWatch.getWatchStopped( "Result_3_2_2_Cand_" + i + " Time" );
		}

		try {
			// BufferedWriter bw = new BufferedWriter( new FileWriter( "Debug_est.txt" ) );
			// long debug_elements = 0;
			// long debug_gcCount = getGCCount();
			// long elements = 0;

			// long lastTokenFiltered = 0;

			for( int sid = 0; sid < tableSearched.size(); sid++ ) {

				Record recS = tableSearched.get( sid );
				Set<Record> candidates = new WYK_HashSet<Record>();

				// List<List<Record>> candidatesList = new ArrayList<List<Record>>();
				List<List<QGram>> availableQGrams = recS.getQGrams( qgramSize, maxPosition + 1 );

				// long recordStartTime = System.nanoTime();
				int[] range = recS.getCandidateLengths( recS.size() - 1 );
				int boundary = Math.min( range[ 0 ], maxIndexLength );
				for( int i = 0; i < boundary; ++i ) {
					candidateTimes[ i ].start();

					// List<List<Record>> ithCandidates = new ArrayList<List<Record>>();

					Map<QGram, List<IntIntRecordTriple>> map = idx.get( i );

					Set<Record> candidatesAppeared = new WYK_HashSet<Record>();

					int actualIndex = indexPosition[ i ];

					for( QGram qgram : availableQGrams.get( actualIndex ) ) {
						// elements++;
						List<IntIntRecordTriple> list = map.get( qgram );
						if( list == null ) {
							++count_empty[ i ];
							continue;
						}
						cand_sum[ i ] += list.size();
						++count_cand[ i ];
						for( IntIntRecordTriple e : list ) {
							if( StaticFunctions.overlap( e.min, e.max, range[ 0 ], range[ 1 ] ) ) {
								// length filtering
								if( i == 0 ) {
									// last token filtering
									// if( recS.shareLastToken( e.rec ) ) {
									candidatesAppeared.add( e.rec );
									// }
									// else {
									// lastTokenFiltered++;
									// }
								}
								else if( candidates.contains( e.rec ) ) {
									// signature filtering
									candidatesAppeared.add( e.rec );
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
			for( int i = 0; i < maxIndexLength; ++i ) {
				Util.printLog( "Avg candidates(w/o empty) : " + cand_sum[ i ] + "/" + count_cand[ i ] );
				Util.printLog( "Avg candidates(w/o empty, after prune) : " + cand_sum_afterprune[ i ] + "/" + count_cand[ i ] );
				Util.printLog( "Avg candidates(w/o empty, after union) : " + cand_sum_afterunion[ i ] + "/" + count_cand[ i ] );
				Util.printLog( "Empty candidates : " + count_empty[ i ] );
			}

			Util.printLog( "comparisions : " + count );

			stat.add( "Mem_4_Joined", ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 );

			stat.add( "Counter_Final_1_HashCollision", WYK_HashSet.collision );
			stat.add( "Counter_Final_1_HashResize", WYK_HashSet.resize );

			stat.add( "Counter_Final_2_MapCollision", WYK_HashMap.collision );
			stat.add( "Counter_Final_2_MapResize", WYK_HashMap.resize );

			stat.add( "Stat_Length_Filtered", lengthFiltered );
			stat.add( equivTime );

			String candTimeStr = "";
			for( int i = 0; i < maxIndexLength; i++ ) {
				candidateTimes[ i ].printTotal();

				candTimeStr = candTimeStr + ( candidateTimes[ i ].getTotalTime() ) + " ";
			}
			stat.add( "Stat_Candidate_Times_Per_Index", candTimeStr );
		}
		return rslt;
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

	@Override
	public String getName() {
		return "JoinBK_QL";
	}

}
