package snu.kdd.synonym.synonymRev.index;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.synonymRev.algorithm.misc.EstimationTest;
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

public class JoinMHIndex implements JoinMHIndexInterface {
	protected ArrayList<WYK_HashMap<QGram, List<Record>>> joinMHIndex;
	protected Object2IntOpenHashMap<Record> indexedCountList;
	protected Query query;

	int indexK;
	int qgramSize;
	int[] indexPosition;
	protected int maxPosition = 0;

	int qgramCount = 0;
	long indexTime = 0;
	public int predictCount = 0;
	long joinTime = 0;
	long countTime = 0;
	public long countValue = 0;

	double gamma;
	double zeta;
	double eta;

	public long equivComparisons;

	/**
	 * JoinMHIndex: builds a MH Index
	 * 
	 * @param indexK
	 * @param qgramSize
	 * @param indexedSet
	 * @param query
	 * @param stat
	 * @param indexPosition
	 * @param addStat
	 * @param useIndexCount
	 * @param threshold
	 */
	
	// empty constructor
	public JoinMHIndex() {}

	public JoinMHIndex(int indexK, int qgramSize, Iterable<Record> indexedSet, Query query, StatContainer stat,
			int[] indexPosition, boolean addStat, boolean useIndexCount, int threshold) {
		// TODO: Need to be fixed to make index just for given sequences
		// NOW, it makes index for all sequences
		
		long starttime = System.nanoTime();
		this.indexK = indexK;
		this.qgramSize = qgramSize;
		this.indexPosition = indexPosition;
		this.query = query;

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
		this.joinMHIndex = new ArrayList<WYK_HashMap<QGram, List<Record>>>();

		long elements = 0;

		for (int i = 0; i < indexK; ++i) {
			// initialize with indexedSet size
			joinMHIndex.add(new WYK_HashMap<QGram, List<Record>>(query.indexedSet.size()));
		}

		long qGramTime = 0;
		long indexingTime = 0;

		for (Record rec : indexedSet) {

			int minInvokes = Integer.MAX_VALUE;
			long recordStartTime = System.currentTimeMillis();
			List<List<QGram>> availableQGrams = null;
			if (!query.oneSideJoin) {
				availableQGrams = rec.getQGrams(qgramSize, maxPosition + 1);
			} else {
				availableQGrams = rec.getSelfQGrams(qgramSize, maxPosition + 1);
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

				Map<QGram, List<Record>> map = joinMHIndex.get(i);
				List<QGram> qgramList = availableQGrams.get(actualIndex);

				qgramCount += qgramList.size();

				if (minInvokes > qgramList.size()) {
					minInvokes = qgramList.size();
				}
				for (QGram qgram : qgramList) {
					// if( debug ) {
					// System.out.println( qgram + " " + actualIndex );
					// }
					List<Record> list = map.get(qgram);
					if (list == null) {
						list = new ArrayList<Record>();
						map.put(qgram, list);
					}
					list.add(rec);
				}
				elements += availableQGrams.get(actualIndex).size();
			}

			this.predictCount += minInvokes;

			long afterIndexing = System.currentTimeMillis();

			qGramTime += afterQGram - recordStartTime;
			indexingTime += afterIndexing - afterQGram;
		}
		stat.add("Result_3_1_1_qGramTime", qGramTime);
		stat.add("Result_3_1_2_indexingTime", indexingTime);
		stat.add("Stat_Index_Size", elements);
		stat.add( "nList", nInvList());

		if (DEBUG.JoinMHIndexON) {
			if (addStat) {
				// computes the statistics of the indexes
				String indexStr = "";
				for (int i = 0; i < indexK; ++i) {
					Map<QGram, List<Record>> ithidx = joinMHIndex.get(i);

					System.out.println(i + "th iIdx key-value pairs: " + ithidx.size());

					// Statistics
					int sum = 0;

					long singlelistsize = 0;
					long count = 0;
					// long sqsum = 0;
					for (Map.Entry<QGram, List<Record>> entry : ithidx.entrySet()) {
						List<Record> list = entry.getValue();

						if (list.size() == 1) {
							++singlelistsize;
							continue;
						}
						sum++;
						count += list.size();
					}

					if (DEBUG.JoinMHIndexON) {
						System.out.println(i + "th Single value list size : " + singlelistsize);
						System.out.println(i + "th iIdx size(w/o 1) : " + count);
						System.out.println(i + "th Rec per idx(w/o 1) : " + ((double) count) / sum);
						// System.out.println( i + "th Sqsum : " + sqsum );
					}

					long totalCount = count + singlelistsize;
					int exp = 0;
					while (totalCount / 1000 != 0) {
						totalCount = totalCount / 1000;
						exp++;
					}

					if (exp == 1) {
						indexStr = indexStr + totalCount + "k ";
					} else if (exp == 2) {
						indexStr = indexStr + totalCount + "M ";
					} else {
						indexStr = indexStr + totalCount + "G ";
					}
				}

				if (DEBUG.JoinMHIndexON) {
					stat.add("Stat_Index_Size_Per_Position", "\"" + indexStr + "\"");
				}
			}
		}

		this.indexTime = System.nanoTime() - starttime;

		this.gamma = ((double) this.indexTime) / this.qgramCount;
		// this.indexTime: time for indexing records in T
		// this.qgramCount: number of pqgrams from records in T

		if (DEBUG.PrintEstimationON) {
			BufferedWriter bwEstimation = EstimationTest.getWriter();
			try {
				bwEstimation.write("[gamma] " + gamma);
				bwEstimation.write(" IndexTime " + indexTime);
				bwEstimation.write(" IndexedSigCount " + qgramCount + "\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		Util.printGCStats(stat, "Stat_Index");
	}
	
	public void joinOneRecordForSplit(Record recS, List<List<QGram>> availableQGrams, Query query, Validator checker,
			Set<IntegerPair> rslt) {
		long startTime = System.currentTimeMillis();
		// this function is for the splitted data sets only -> qgrams are previously
		// computed and
		// length filtering is not applied here (already applied by the function calling
		// this function)

		// boolean debug = recS.getID() == 4145;
		// long recordStartTime = System.nanoTime();

		int[] range = recS.getTransLengths();

		ObjectOpenHashSet<Record> prevCandidate = null;
		for (int i = 0; i < indexK; ++i) {
			int actualIndex = indexPosition[i];

			ObjectOpenHashSet<Record> ithCandidates = new ObjectOpenHashSet<Record>();

			Map<QGram, List<Record>> map = joinMHIndex.get(i);

			for (QGram qgram : availableQGrams.get(actualIndex)) {
				// if( debug ) {
				// System.out.println( "Q " + qgram + " " + actualIndex );
				// }

				// elements++;
				List<Record> list = map.get(qgram);
				if (list == null) {
					continue;
				}

				for (Record otherRecord : list) {
					// if( debug ) {
					// System.out.println( "record: " + otherRecord );
					// }

					int[] otherRange = otherRecord.getTransLengths();

					if (StaticFunctions.overlap(range[0], range[1], otherRange[0], otherRange[1])) {
						if (prevCandidate == null) {
							ithCandidates.add(otherRecord);
						} else if (prevCandidate.contains(otherRecord)) {
							ithCandidates.add(otherRecord);
						}
					}
				}
			}

			if (prevCandidate != null) {
				prevCandidate.clear();
			}
			prevCandidate = ithCandidates;

			if (prevCandidate.size() == 0) {
				break;
			}
		}

		if (DEBUG.JoinMHIndexON) {
			if (System.currentTimeMillis() - startTime > 0) {
				System.out.println("prevCand: " + prevCandidate.size());
			}
		}

		equivComparisons += prevCandidate.size();
		for (Record recR : prevCandidate) {
			int compare = checker.isEqual(recS, recR);
			if (compare >= 0) {
//				rslt.add(new IntegerPair(recS.getID(), recR.getID()));
				AlgorithmTemplate.addSeqResult( recS, recR, rslt, query.selfJoin );
			}
		}
	}
	
	protected List<List<QGram>> getCandidatePQGrams( Record rec ) {
		List<List<QGram>> availableQGrams = rec.getQGrams( qgramSize, maxPosition+1 );
		List<List<QGram>> candidatePQGrams = new ArrayList<List<QGram>>();
		for ( int k=0; k<availableQGrams.size(); ++k ) {
			if ( k >= joinMHIndex.size() ) continue;
			WYK_HashMap<QGram, List<Record>> curidx = joinMHIndex.get( k );
			List<QGram> qgrams = new ArrayList<QGram>();
			for ( QGram qgram : availableQGrams.get( k ) ) {
				if ( !curidx.containsKey( qgram ) ) continue;
				qgrams.add( qgram );
			}
			candidatePQGrams.add( qgrams );
		}
		return candidatePQGrams;
//		return rec.getQGrams( qgramSize, maxPosition+1 );
	}

	public Set<IntegerPair> join(StatContainer stat, Query query, Validator checker, boolean writeResult) {
		long startTime = System.nanoTime();
		long totalCountTime = 0;
		long totalCountValue = 0;

		Set<IntegerPair> rslt = new ObjectOpenHashSet<IntegerPair>();

		long count = 0;
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
			// boolean debug = false;

			Record recS = query.searchedSet.getRecord(sid);
			Set<Record> candidates = new WYK_HashSet<Record>(100);

			// if( recS.getID() == 94118 ) {
			// debug = true;
			// }

			Object2IntOpenHashMap<Record> candidatesCount = new Object2IntOpenHashMap<Record>();
			candidatesCount.defaultReturnValue(-1);

			long countStartTime = System.nanoTime();

			List<List<QGram>> availableQGrams = getCandidatePQGrams( recS );

			for (List<QGram> list : availableQGrams) {
				totalCountValue += list.size();
			}
			totalCountTime += System.nanoTime() - countStartTime;

			// long recordStartTime = System.nanoTime();
			int[] range = recS.getTransLengths();
			for (int i = 0; i < indexK; ++i) {
				int actualIndex = indexPosition[i];
				if (range[0] <= actualIndex) {
					continue;
				}

				candidateTimes[i].start();

				ObjectOpenHashSet<Record> ithCandidates = new ObjectOpenHashSet<Record>();

				Map<QGram, List<Record>> map = joinMHIndex.get(i);

				for (QGram qgram : availableQGrams.get(actualIndex)) {
					// if( debug ) {
					// System.out.println( "Q " + qgram + " " + actualIndex );
					// }

					// elements++;
					List<Record> list = map.get(qgram);
					if (list == null) {
						++count_empty[i];
						continue;
					}
					cand_sum[i] += list.size();
					++count_cand[i];
					for (Record otherRecord : list) {
						// if( debug ) {
						// System.out.println( "record: " + otherRecord );
						// }

						int[] otherRange = null;

						if (query.oneSideJoin) {
							otherRange = new int[2];
							otherRange[0] = otherRecord.getTokenCount();
							otherRange[1] = otherRecord.getTokenCount();
						} else {
							otherRange = otherRecord.getTransLengths();
						}

						if (StaticFunctions.overlap(otherRange[0], otherRange[1], range[0], range[1])) {
							// length filtering

							ithCandidates.add(otherRecord);
						} else {
							++checker.lengthFiltered;
						}
					}
					cand_sum_afterprune[i] += candidatesCount.size();
				}

				for (Record otherRecord : ithCandidates) {
					int candCount = candidatesCount.getInt(otherRecord);
					if (candCount == -1) {
						candidatesCount.put(otherRecord, 1);
					} else {
						candidatesCount.put(otherRecord, candCount + 1);
					}
				}

				candidateTimes[i].stopQuiet();
			} // end for i from 0 to indexK
			count += candidates.size();

			ObjectIterator<Entry<Record>> iter = candidatesCount.object2IntEntrySet().iterator();
			while (iter.hasNext()) {
				Entry<Record> entry = iter.next();
				Record record = entry.getKey();
				int recordCount = entry.getIntValue();

				/*
				 *  04.27.18, ghsong: in the below condition A || B, why do we check B?
				 *  Since indexedCountList has no info for recS in S, condition B is useless.
				 *  Thus,it seems that checking A only is enough.
				 *  06.27.18. ghsong: condition B is necessary!!
				 */
				if (indexedCountList.getInt(record) <= recordCount || indexedCountList.getInt(recS) <= recordCount) {
					candidates.add(record);
				}
			} // end while iter

			equivTime.start();
			equivComparisons += candidates.size();
			for (Record recR : candidates) {
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

		} // end for query.searchedSet

		if (writeResult) {
			stat.add("Stat_Equiv_Comparison", count);
		}

		if (DEBUG.JoinMHIndexON) {
			if (writeResult) {
				for (int i = 0; i < indexK; ++i) {
					Util.printLog("Avg candidates(w/o empty) : " + cand_sum[i] + "/" + count_cand[i]);
					Util.printLog(
							"Avg candidates(w/o empty, after prune) : " + cand_sum_afterprune[i] + "/" + count_cand[i]);
					Util.printLog("Empty candidates : " + count_empty[i]);
				}

				Util.printLog("comparisions : " + count);

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
		this.countTime = totalCountTime;
		this.countValue = totalCountValue;

		this.zeta = (double) totalCountTime / totalCountValue;
		// totalCountTime: time for generating TPQ supersets
		// totalCountValue: the size of TPQ supersets
		
		this.eta = ((double) this.joinTime / this.predictCount);
		// this.joinTime: time for counting and verifications
		// this.predictCount: the sum of minimum invokes (number of records in searchedSet to be verified) of records in indexedSet

		stat.add( "Const_Gamma", gamma );
		stat.add( "Const_Zeta", zeta );
		stat.add( "Const_Eta", eta );

		return rslt;
	}

	public void joinOneRecordThres( Record recS, Set<IntegerPair> rslt, Validator checker, int threshold, boolean oneSideJoin ) {
		Set<Record> candidates = new WYK_HashSet<Record>(100);

		boolean isUpperRecord = recS.getEstNumTransformed() > threshold;

		Object2IntOpenHashMap<Record> candidatesCount = new Object2IntOpenHashMap<Record>();
		candidatesCount.defaultReturnValue(-1);

		List<List<QGram>> availableQGrams = getCandidatePQGrams( recS );
		for (List<QGram> list : availableQGrams) {
			this.countValue += list.size();
		}

		// long recordStartTime = System.nanoTime();
		int[] range = recS.getTransLengths();
		for (int i = 0; i < indexK; ++i) {
			int actualIndex = indexPosition[i];
			if (range[0] <= actualIndex) {
				continue;
			}

			ObjectOpenHashSet<Record> ithCandidates = new ObjectOpenHashSet<Record>();

			Map<QGram, List<Record>> map = joinMHIndex.get(i);

			for (QGram qgram : availableQGrams.get(actualIndex)) {
				// if( debug ) {
				// System.out.println( "Q " + qgram + " " + actualIndex );
				// }

				// elements++;
				List<Record> list = map.get(qgram);
				if (list == null) {
					continue;
				}

				for (Record otherRecord : list) {
					// if( debug ) {
					// System.out.println( "record: " + otherRecord );
					// }
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

					if (StaticFunctions.overlap(otherRange[0], otherRange[1], range[0], range[1])) {
						// length filtering

						ithCandidates.add(otherRecord);
					}
					else ++checker.lengthFiltered;
				}
			}

			for (Record otherRecord : ithCandidates) {
				int candCount = candidatesCount.getInt(otherRecord);
				if (candCount == -1) {
					candidatesCount.put(otherRecord, 1);
				} else {
					candidatesCount.put(otherRecord, candCount + 1);
				}
			}
		}

		ObjectIterator<Entry<Record>> iter = candidatesCount.object2IntEntrySet().iterator();
		while (iter.hasNext()) {
			Entry<Record> entry = iter.next();
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
	}

	public double getGamma() {
		return this.gamma;
	}

	public double getZeta() {
		return this.zeta;
	}
	
	public double getEta() {
		return this.eta;
	}

	public int size() {
		return joinMHIndex.size();
	}
	
	public WYK_HashMap<QGram, List<Record>> get(int i) {
		return joinMHIndex.get( i );
	}

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
		for (int i=0; i<joinMHIndex.size(); i++) {
			n += joinMHIndex.get( i ).size();
		}
		return n;
	}
	
	public void writeToFile() {
		try { 
			BufferedWriter bw = new BufferedWriter( new FileWriter( "tmp/JoinMHIndex.txt" ) ); 
			for ( int pos=0; pos<joinMHIndex.size(); ++pos ) {
				bw.write( "pos: "+indexPosition[pos]+"\n" );
				for ( Map.Entry<QGram, List<Record>> entry : joinMHIndex.get( pos ).entrySet() ) {
					bw.write( "qgram: "+entry.getKey()+"\n" );
					for ( Record rec : entry.getValue() ) {
						bw.write( Arrays.toString( rec.getTokensArray() ) + "\n");
					}
				}
			}
			bw.flush();
		}
		catch (IOException e ) { e.printStackTrace(); }
	}
}
