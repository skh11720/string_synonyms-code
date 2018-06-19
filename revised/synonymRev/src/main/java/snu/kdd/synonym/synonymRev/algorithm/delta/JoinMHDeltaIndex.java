package snu.kdd.synonym.synonymRev.algorithm.delta;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.synonymRev.algorithm.misc.EstimationTest;
import snu.kdd.synonym.synonymRev.algorithm.misc.SampleDataTest;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.index.JoinMHIndexInterface;
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

public class JoinMHDeltaIndex implements JoinMHIndexInterface {
	private ArrayList<ArrayList<WYK_HashMap<QGram, List<Record>>>> index;
	// key: pos -> delta -> qgram -> recordList
	private Object2IntOpenHashMap<Record> indexedCountList;
	private Query query;
	private final QGramDeltaGenerator qdgen;

	private final int indexK;
	private final int qgramSize;
	private final int[] indexPosition;
	private final int deltaMax;
	private int maxPosition = 0;

	int qgramCount = 0;
	long indexTime = 0;
	public int predictCount = 0;
	long joinTime = 0;
	long countTime = 0;
	public long countValue = 0;

	double eta;
	double theta;
	double iota;

	public long equivComparisons;

	/**
	 * JoinMHIndex: builds a MH Index
	 * 
	 * @param indexK
	 * @param qgramSize
	 * @param deltaMax
	 * @param indexedSet
	 * @param query
	 * @param stat
	 * @param indexPosition
	 * @param addStat
	 * @param useIndexCount
	 * @param threshold
	 */
	
	public JoinMHDeltaIndex(int indexK, int qgramSize, int deltaMax, Iterable<Record> indexedSet, Query query, StatContainer stat,
			int[] indexPosition, boolean addStat, boolean useIndexCount, int threshold) {
		// TODO: Need to be fixed to make index just for given sequences
		// NOW, it makes index for all sequences
		
		long starttime = System.nanoTime();
		this.indexK = indexK;
		this.qgramSize = qgramSize;
		this.indexPosition = indexPosition;
		this.query = query;
		this.deltaMax = deltaMax;
		this.qdgen = new QGramDeltaGenerator( qgramSize, deltaMax );

		if (indexPosition.length != indexK) {
			throw new RuntimeException("The length of indexPosition should match indexK");
		}

		// indexed count list keeps the number of positions of a record to be used to
		// join
		if (useIndexCount) {
			indexedCountList = new Object2IntOpenHashMap<Record>();
		}

		for (int idx : indexPosition) {
			if (maxPosition < idx) {
				maxPosition = idx;
			}
		}

		// one WKY_HashMap per position
		this.index = new ArrayList<ArrayList<WYK_HashMap<QGram, List<Record>>>>();

		long elements = 0;

		for (int i = 0; i < indexK; ++i) {
			// initialize with indexedSet size
			ArrayList<WYK_HashMap<QGram, List<Record>>> indexPos = new ArrayList<WYK_HashMap<QGram, List<Record>>>();
			for ( int d=0; d<=deltaMax; ++d ) {
				indexPos.add( new WYK_HashMap<QGram, List<Record>>(query.indexedSet.size()) );
			}
			index.add( indexPos );
		}

		long qGramTime = 0;
		long indexingTime = 0;

		for (Record rec : indexedSet) {

			int minInvokes = Integer.MAX_VALUE;
			long recordStartTime = System.currentTimeMillis();
			List<List<QGram>> availableQGrams = null;
			if (!query.oneSideJoin) {
				availableQGrams = rec.getQGrams(qgramSize+deltaMax, maxPosition + 1);
			} else {
				availableQGrams = rec.getSelfQGrams(qgramSize+deltaMax, maxPosition + 1);
			}

			long afterQGram = System.currentTimeMillis();

			int indexedCount = 0;
			int[] range = rec.getTransLengths();

			if (useIndexCount) {
				for (int i = 0; i < indexPosition.length; i++) {
					int actual = indexPosition[i];

					if (range[0] > actual) {
						indexedCount++;
					}
				}
				indexedCountList.put(rec, indexedCount);
			}

			for (int i = 0; i < indexPosition.length; i++) {
				int actualIndex = indexPosition[i];
				if (availableQGrams.size() <= actualIndex) {
					continue;
				}
//	private ArrayList<ArrayList<WYK_HashMap<QGram, List<Record>>>> index;
				ArrayList<WYK_HashMap<QGram, List<Record>>> map_pos = index.get( actualIndex );
//				for ( int d=0; d<=deltaMax; ++d ) map_pos.add( new WYK_HashMap<>() );

//				Map<QGram, List<Record>> map = index.get(i);
				List<QGram> qgramList = availableQGrams.get(actualIndex); // of length qgramSize+deltaMax

				qgramCount += qgramList.size();

				if (minInvokes > qgramList.size()) {
					minInvokes = qgramList.size();
				}
				
				/*
				 * For each qgram, consdier all combinations of tokens given deltaMax.
				 * For instance, given q=2 and deltaMax=2, from a qgram ABCD, 
				 * delta=0: ABCD, 
				 * delta=1: ABC, ABD, ACD, BCD, 
				 * delta=2: AB, AC, AD, BC, BD, CD are generated.
				 */
				for (QGram qgram : qgramList) {
					// if( debug ) {
					// System.out.println( qgram + " " + actualIndex );
					// }
					
					/*
					 * An example
					 * q=3, deltaMax=2
					 * Given an extended qgram ABCDE
					 * All combinations of (3+2) choose 3: [0,1,2], [0,1,3], ...
					 * [0,1,2]: ABC, delta=0
					 * [0,1,3]: ABD, delta=1
					 * [0,1,4]: ABE, delta=2
					 * [0,2,3]: ACD, delta=1
					 * [0,2,4]: ACE, delta=2
					 * [0,3,4]: ADE, delta=2
					 * [1,2,3]: BCD, delta=1
					 * [1,2,4]: BCE, delta=2
					 * [1,3,4]: BDE, delta=2
					 * ...
					 */
					for ( Entry<QGram, Integer> entry : qdgen.getQGramDelta( qgram ) ) {
						QGram qgramDelta = entry.getKey();
						int delta = entry.getValue();
						List<Record> recordList = map_pos.get( delta ).get( qgramDelta );
						if ( recordList == null ) {
							recordList = new ObjectArrayList<Record>();
							map_pos.get( delta ).put( qgramDelta, recordList );
						}
						recordList.add( rec );
					}
				} // end for qgram in qgramList
				elements += availableQGrams.get(actualIndex).size();
//				index.add( map_pos );
			} // end for i of indexPosition

			this.predictCount += minInvokes;

			long afterIndexing = System.currentTimeMillis();

			qGramTime += afterQGram - recordStartTime;
			indexingTime += afterIndexing - afterQGram;
		} // end for rec in indexedSet
		stat.add("Result_3_1_1_qGramTime", qGramTime);
		stat.add("Result_3_1_2_indexingTime", indexingTime);
		stat.add("Stat_Index_Size", elements);
		stat.add( "nList", nInvList());

//		if (DEBUG.JoinMHIndexON) {
//			if (addStat) {
//				// computes the statistics of the indexes
//				String indexStr = "";
//				for (int i = 0; i < indexK; ++i) {
//					List<WYK_HashMap<QGram, List<Record>>> ithidx = index.get( i );
//
//					System.out.println(i + "th iIdx key-value pairs: " + ithidx.size());
//
//					// Statistics
//					int sum = 0;
//
//					long singlelistsize = 0;
//					long count = 0;
//					// long sqsum = 0;
//					for (Map.Entry<QGram, List<Record>> entry : ithidx.entrySet()) {
//						List<Record> list = entry.getValue();
//
//						if (list.size() == 1) {
//							++singlelistsize;
//							continue;
//						}
//						sum++;
//						count += list.size();
//					}
//
//					if (DEBUG.JoinMHIndexON) {
//						System.out.println(i + "th Single value list size : " + singlelistsize);
//						System.out.println(i + "th iIdx size(w/o 1) : " + count);
//						System.out.println(i + "th Rec per idx(w/o 1) : " + ((double) count) / sum);
//						// System.out.println( i + "th Sqsum : " + sqsum );
//					}
//
//					long totalCount = count + singlelistsize;
//					int exp = 0;
//					while (totalCount / 1000 != 0) {
//						totalCount = totalCount / 1000;
//						exp++;
//					}
//
//					if (exp == 1) {
//						indexStr = indexStr + totalCount + "k ";
//					} else if (exp == 2) {
//						indexStr = indexStr + totalCount + "M ";
//					} else {
//						indexStr = indexStr + totalCount + "G ";
//					}
//				}
//
//				if (DEBUG.JoinMHIndexON) {
//					stat.add("Stat_Index_Size_Per_Position", "\"" + indexStr + "\"");
//				}
//			}
//		} 

		this.indexTime = System.nanoTime() - starttime;

		this.eta = ((double) this.indexTime) / this.qgramCount;

		if (DEBUG.PrintEstimationON) {
			BufferedWriter bwEstimation = EstimationTest.getWriter();
			try {
				bwEstimation.write("[Eta] " + eta);
				bwEstimation.write(" IndexTime " + indexTime);
				bwEstimation.write(" IndexedSigCount " + qgramCount + "\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		Util.printGCStats(stat, "Stat_Index");
		writeToFile();
	}
	
//	public void joinOneRecordForSplit(Record recS, List<List<QGram>> availableQGrams, Query query, Validator checker,
//			Set<IntegerPair> rslt) {
//		long startTime = System.currentTimeMillis();
//		// this function is for the splitted data sets only -> qgrams are previously
//		// computed and
//		// length filtering is not applied here (already applied by the function calling
//		// this function)
//
//		// boolean debug = recS.getID() == 4145;
//		// long recordStartTime = System.nanoTime();
//
//		int[] range = recS.getTransLengths();
//
//		ObjectOpenHashSet<Record> prevCandidate = null;
//		for (int i = 0; i < indexK; ++i) {
//			int actualIndex = indexPosition[i];
//
//			ObjectOpenHashSet<Record> ithCandidates = new ObjectOpenHashSet<Record>();
//
//			Map<QGram, List<Record>> map = index.get(i);
//
//			for (QGram qgram : availableQGrams.get(actualIndex)) {
//				// if( debug ) {
//				// System.out.println( "Q " + qgram + " " + actualIndex );
//				// }
//
//				// elements++;
//				List<Record> list = map.get(qgram);
//				if (list == null) {
//					continue;
//				}
//
//				for (Record otherRecord : list) {
//					// if( debug ) {
//					// System.out.println( "record: " + otherRecord );
//					// }
//
//					int[] otherRange = otherRecord.getTransLengths();
//
//					if (StaticFunctions.overlap(range[0], range[1], otherRange[0], otherRange[1])) {
//						if (prevCandidate == null) {
//							ithCandidates.add(otherRecord);
//						} else if (prevCandidate.contains(otherRecord)) {
//							ithCandidates.add(otherRecord);
//						}
//					}
//				}
//			}
//
//			if (prevCandidate != null) {
//				prevCandidate.clear();
//			}
//			prevCandidate = ithCandidates;
//
//			if (prevCandidate.size() == 0) {
//				break;
//			}
//		}
//
//		if (DEBUG.JoinMHIndexON) {
//			if (System.currentTimeMillis() - startTime > 0) {
//				System.out.println("prevCand: " + prevCandidate.size());
//			}
//		}
//
//		equivComparisons += prevCandidate.size();
//		for (Record recR : prevCandidate) {
//			int compare = checker.isEqual(recS, recR);
//			if (compare >= 0) {
////				rslt.add(new IntegerPair(recS.getID(), recR.getID()));
//				AlgorithmTemplate.addSeqResult( recS, recR, rslt, query.selfJoin );
//			}
//		}
//	}
	
	private List<List<Set<QGram>>> getCandidatePQGrams( Record rec ) {
		/*
		 * Return the lists of qgrams, where each list is indexed by pos and delta.
		 * key: pos -> delta 
		 */
		boolean debug = false;
		List<List<QGram>> availableQGrams = rec.getQGrams( qgramSize+deltaMax, maxPosition+1 );
		if (debug) {
			System.out.println( "availableQGrams:" );
			for ( List<QGram> qgramList : availableQGrams ) {
				for ( QGram qgram : qgramList ) System.out.println( qgram );
			}
		}
		List< List<Set<QGram>>> candidatePQGrams = new ArrayList<List<Set<QGram>>>();
		for ( int k=0; k<availableQGrams.size(); ++k ) {
			if ( k >= index.size() ) continue;
			List<WYK_HashMap<QGram, List<Record>>> curidx = index.get( k );
			List<Set<QGram>> cand_pos = new ArrayList<Set<QGram>>();
			for ( int d=0; d<=deltaMax; ++d ) cand_pos.add( new ObjectOpenHashSet<QGram>() );
			for ( QGram qgram : availableQGrams.get( k ) ) {
//			List<QGram> qgrams = new ArrayList<QGram>();
				if (debug) System.out.println( "qgram: "+qgram );
				if (debug) System.out.println( "qgramDelta: "+qdgen.getQGramDelta( qgram ) );
				for ( Entry<QGram, Integer> entry: qdgen.getQGramDelta( qgram ) ) {
					QGram qgramDelta = entry.getKey();
					int delta_s = entry.getValue();
					for ( int delta_t=0; delta_t<=deltaMax-delta_s; ++delta_t ) {
						if ( curidx.get( delta_t ).containsKey( qgramDelta ) ) {
							if (debug) System.out.println( "cand_pos.get("+delta_s+").add: "+qgramDelta );
							cand_pos.get( delta_s ).add( qgramDelta );
							break;
						}
					}
				} // end for (qgramDelta, delta)
//				qgrams.add( qgram );
			} // end for qgram in availableQGrams
			candidatePQGrams.add( cand_pos );
		} // end for k
		return candidatePQGrams;
//		return rec.getQGrams( qgramSize, maxPosition+1 );
	}

	public Set<IntegerPair> join(StatContainer stat, Query query, Validator checker, boolean writeResult) {
		long startTime = System.nanoTime();
		long totalCountTime = 0;
		long totalCountValue = 0;

		Set<IntegerPair> rslt = new ObjectOpenHashSet<IntegerPair>();

		@SuppressWarnings("unused")
		long lengthFiltered = 0;

		long cand_sum[] = new long[indexK];
		long cand_sum_afterprune[] = new long[indexK];
		int count_cand[] = new int[indexK];
		int count_empty[] = new int[indexK];

		StopWatch equivTime = StopWatch.getWatchStopped("Result_3_2_1_Equiv_Checking_Time");
		StopWatch[] candidateTimes = new StopWatch[indexK];
		for (int i = 0; i < indexK; i++) {
			candidateTimes[i] = StopWatch.getWatchStopped("Result_3_2_2_Cand_" + i + " Time");
		}

		for (int sid = 0; sid < query.searchedSet.size(); sid++) {
			// long startTime = System.currentTimeMillis();
			 boolean debug = false;

			Record recS = query.searchedSet.getRecord(sid);
			Set<Record> candidates = new WYK_HashSet<Record>(100);

//			 if( recS.getID() == 677 ) debug = true;
			 if (debug) System.out.println( "record: "+recS.getID()+" : "+Arrays.toString( recS.getTokensArray() ) );
			 if (debug) SampleDataTest.inspect_record( recS, query, qgramSize );

			Object2IntOpenHashMap<Record> candidatesCount = new Object2IntOpenHashMap<Record>();
			candidatesCount.defaultReturnValue(0);

			long countStartTime = System.nanoTime();

			List<List<Set<QGram>>> candidateQGrams = getCandidatePQGrams( recS );
			if (debug) System.out.println( "candidateQGrams: "+candidateQGrams );

			for ( List<Set<QGram>> qgrams_pos : candidateQGrams ) {
				for ( Set<QGram> qgrams_delta : qgrams_pos ) {
					totalCountValue += qgrams_delta.size();
				}
			}
			totalCountTime += System.nanoTime() - countStartTime;

			// long recordStartTime = System.nanoTime();
			int[] range = recS.getTransLengths();
			for (int i = 0; i < indexK; ++i) {
				int actualIndex = indexPosition[i];
				if (debug) System.out.println( "actualIndex: "+actualIndex+", "+range[0] );
				if (range[0] <= actualIndex) {
					continue;
				}
				candidateTimes[i].start();
				
				// Given a position
				List<Set<QGram>> cand_qgrams_pos = candidateQGrams.get( actualIndex );
				ObjectOpenHashSet<Record> ithCandidates = new ObjectOpenHashSet<Record>();

				List<WYK_HashMap<QGram, List<Record>>> map = index.get(i);
				
				for ( int delta_s=0; delta_s<=deltaMax; ++delta_s ) {
					if ( cand_qgrams_pos.size() <= delta_s ) break;
					for ( QGram qgram : cand_qgrams_pos.get( delta_s ) ) {
						for ( int delta_t=0; delta_t<=deltaMax-delta_s; ++delta_t ) {
							List<Record> recordList = map.get( delta_t ).get( qgram );
							if ( recordList == null ) {
								++count_empty[i];
								continue;
							}
							if (debug) System.out.println( "(pos, delta_s, delta_t, qgram): "+i+", "+delta_s+", "+delta_t+", "+qgram );
							if (debug) System.out.println( recordList );
							cand_sum[i] += recordList.size();
							++count_cand[i];
							
							// Perform length filtering.
							for ( Record otherRecord : recordList ) {
								int[] otherRange = null;

								if (query.oneSideJoin) {
									otherRange = new int[2];
									otherRange[0] = otherRecord.getTokenCount();
									otherRange[1] = otherRecord.getTokenCount();
								} else otherRange = otherRecord.getTransLengths();

								if (StaticFunctions.overlap(otherRange[0]-deltaMax, otherRange[1], range[0]-deltaMax, range[1])) {
									ithCandidates.add(otherRecord);
									if (debug) System.out.println( otherRecord.getID()+" passes the length filter" );
								}
								else {
									++checker.lengthFiltered;
									if (debug) System.out.println( otherRecord.getID()+" is filtered out by the length filter" );
									if (debug) System.out.println( Arrays.toString( range )+", "+Arrays.toString( otherRange ));
								}
							} // end for otherRecord in recordList
						} // end for delta_t
					} // end for qgram in cand_qgrams_pos
				} // end for delta_s
				
				for (Record otherRecord : ithCandidates) candidatesCount.addTo( otherRecord, 1 );
				candidateTimes[i].stopQuiet();
				cand_sum_afterprune[i] += candidatesCount.size();
			} // end for i in 0...indexK

			ObjectIterator<Object2IntMap.Entry<Record>> iter = candidatesCount.object2IntEntrySet().iterator();
			while (iter.hasNext()) {
				Object2IntMap.Entry<Record> entry = iter.next();
				Record record = entry.getKey();
				int recordCount = entry.getIntValue();

				/*
				 *  04.27.18, ghsong: in the below condition A || B, why do we check B?
				 *  Since indexedCountList has no info for recS in S, condition B is useless.
				 *  Thus,it seems that checking A only is enough.
				 */
				if (debug) System.out.println( record.getID()+": "+indexedCountList.getInt(record)+", "+recordCount );
				if (indexedCountList.getInt(record) <= recordCount || indexedCountList.getInt(recS) <= recordCount) {
					candidates.add(record);
				}
			}

			equivTime.start();
			equivComparisons += candidates.size();
			for (Record recR : candidates) {
				if (debug) System.out.println( "check "+recS.getID()+" and "+recR.getID() );
				int compare = checker.isEqual(recS, recR);
				if (compare >= 0) {
//					rslt.add(new IntegerPair(recS.getID(), recR.getID()));
					AlgorithmTemplate.addSeqResult( recS, recR, rslt, query.selfJoin );
				}
			}
			equivTime.stopQuiet();

			// long executionTime = System.currentTimeMillis() - startTime;

			// if( candidates.size() > 100 ) {
			// System.out.println( recS.getID() + " compared " );
			// System.out.println( candidates.size() );
			// }

			// if( executionTime > 2 ) {
			// System.out.println( recS.getID() + " processed " + executionTime );
			// }

		} // for sid in in searchedSet

		if (writeResult) {
			stat.add("Stat_Equiv_Comparison", equivComparisons);
		}

		if (DEBUG.JoinMHIndexON) {
			if (writeResult) {
				for (int i = 0; i < indexK; ++i) {
					Util.printLog("Avg candidates(w/o empty) : " + cand_sum[i] + "/" + count_cand[i]);
					Util.printLog(
							"Avg candidates(w/o empty, after prune) : " + cand_sum_afterprune[i] + "/" + count_cand[i]);
					Util.printLog("Empty candidates : " + count_empty[i]);
				}

				Util.printLog("comparisions : " + equivComparisons);

				stat.addMemory("Mem_4_Joined");

				stat.add("Counter_Final_1_HashCollision", WYK_HashSet.collision);
				stat.add("Counter_Final_1_HashResize", WYK_HashSet.resize);

				stat.add("Counter_Final_2_MapCollision", WYK_HashMap.collision);
				stat.add("Counter_Final_2_MapResize", WYK_HashMap.resize);

				stat.add("Stat_Length_Filtered", lengthFiltered);
				stat.add(equivTime);

				String candTimeStr = "";
				for (int i = 0; i < indexK; i++) {
					candidateTimes[i].printTotal();

					candTimeStr = candTimeStr + (candidateTimes[i].getTotalTime()) + " ";
				}
				stat.add("Stat_Candidate_Times_Per_Index", candTimeStr);
			}
		}
		this.joinTime = System.nanoTime() - startTime - totalCountTime;
		this.theta = ((double) this.joinTime / this.predictCount);
		this.countTime = totalCountTime;
		this.countValue = totalCountValue;
		this.iota = (double) totalCountTime / totalCountValue;

		return rslt;
	}

	public void joinOneRecordThres( Record recS, Set<IntegerPair> rslt, Validator checker, int threshold, boolean oneSideJoin ) {
		Set<Record> candidates = new WYK_HashSet<Record>(100);

		boolean isUpperRecord = recS.getEstNumTransformed() > threshold;

		Object2IntOpenHashMap<Record> candidatesCount = new Object2IntOpenHashMap<Record>();
		candidatesCount.defaultReturnValue(0);

		List<List<Set<QGram>>> candidateQGrams = getCandidatePQGrams( recS );
		for (List<Set<QGram>> qgrams_pos : candidateQGrams) {
			for ( Set<QGram> qgrams_delta : qgrams_pos ) {
				this.countValue += qgrams_delta.size();
			}
		}

		// long recordStartTime = System.nanoTime();
		int[] range = recS.getTransLengths();
		for (int i = 0; i < indexK; ++i) {
			int actualIndex = indexPosition[i];
			if (range[0] <= actualIndex) {
				continue;
			}

			// Given a position
			List<Set<QGram>> cand_qgrams_pos = candidateQGrams.get( actualIndex );
			ObjectOpenHashSet<Record> ithCandidates = new ObjectOpenHashSet<Record>();

			List<WYK_HashMap<QGram, List<Record>>> map = index.get(i);

			for ( int delta_s=0; delta_s<=deltaMax; ++delta_s ) {
				if ( cand_qgrams_pos.size() <= delta_s ) break;
				for ( QGram qgram : cand_qgrams_pos.get( delta_s ) ) {
					for ( int delta_t=0; delta_t<=deltaMax-delta_s; ++delta_t ) {
						List<Record> recordList = map.get( delta_t ).get( qgram );
						if (recordList == null) {
							continue;
						}

						for (Record otherRecord : recordList) {
							if (!isUpperRecord && otherRecord.getEstNumTransformed() <= threshold) {
								continue;
							}

							int[] otherRange = null;

							if (oneSideJoin) {
								otherRange = new int[2];
								otherRange[0] = otherRecord.getTokenCount();
								otherRange[1] = otherRecord.getTokenCount();
							} else {
								otherRange = otherRecord.getTransLengths();
							}

							if (StaticFunctions.overlap(otherRange[0]-deltaMax, otherRange[1], range[0]-deltaMax, range[1])) {
								ithCandidates.add(otherRecord);
							}
							else ++checker.lengthFiltered;
						} // end for otherRecord in recordList
					} // end for delta_t
				} // end for qgram in cand_qgrams_pos
			} // end for delta_s

			for (Record otherRecord : ithCandidates) candidatesCount.addTo( otherRecord, 1 );
		} // end for i from 0 to indexK

		ObjectIterator<Object2IntMap.Entry<Record>> iter = candidatesCount.object2IntEntrySet().iterator();
		while (iter.hasNext()) {
			Object2IntMap.Entry<Record> entry = iter.next();
			Record record = entry.getKey();
			int recordCount = entry.getIntValue();

			if (indexedCountList.getInt(record) <= recordCount || indexedCountList.getInt(recS) <= recordCount) {
				candidates.add(record);
			}
			else ++checker.pqgramFiltered;
		}

		equivComparisons += candidates.size();
		for (Record recR : candidates) {
			int compare = checker.isEqual(recS, recR);
			if (compare >= 0) {
//				rslt.add(new IntegerPair(recS.getID(), recR.getID()));
				AlgorithmTemplate.addSeqResult( recS, recR, rslt, query.selfJoin );
			}
		}
	} // end joinOneRecordThres

	public double getEta() {
		return this.eta;
	}

	public double getTheta() {
		return this.theta;
	}

	public double getIota() {
		return this.iota;
	}
	
	public int size() {
		return index.size();
	}
	
//	public WYK_HashMap<QGram, List<Record>> get(int i) {
//		return index.get( i );
//	}

	public int getIndexedCount( Record rec ) {
		return indexedCountList.getInt( rec );
	}

	@Override
	public long getCountValue() {
		return countValue;
	}

	@Override
	public long getEquivComparisons() {
		return equivComparisons;
	}

	public int nInvList() {
		int n = 0;
		for (int i=0; i<index.size(); i++) {
			n += index.get( i ).size();
		}
		return n;
	}
	
	public void writeToFile() {
		try { 
			BufferedWriter bw = new BufferedWriter( new FileWriter( "tmp/JoinMHDeltaIndex.txt" ) ); 
			for ( int pos=0; pos<index.size(); ++pos ) {
//				bw.write( "pos: "+indexPosition[pos]+"\n" );
				for ( int d=0; d<=deltaMax; ++d ) {
//					bw.write( "delta: "+d+"\n" );
					WYK_HashMap<QGram, List<Record>> map = index.get( pos ).get( d );
					for ( Map.Entry<QGram, List<Record>> entry : map.entrySet() ) {
//						bw.write( "qgram: "+entry.getKey()+"\n" );
						for ( Record rec : entry.getValue() ) {
							bw.write( indexPosition[pos]+"\t"+d+"\t"+entry.getKey()+"\t"+Arrays.toString( rec.getTokensArray() ) + " (" +rec.getID()+ ")\n");
						}
					}
				}
			}
			bw.flush();
		}
		catch (IOException e ) { e.printStackTrace(); }
	}
}
