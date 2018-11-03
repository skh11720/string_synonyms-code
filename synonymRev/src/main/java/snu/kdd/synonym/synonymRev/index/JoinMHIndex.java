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
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StaticFunctions;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.tools.WYK_HashMap;
import snu.kdd.synonym.synonymRev.tools.WYK_HashSet;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class JoinMHIndex extends AbstractIndex {
	protected ArrayList<WYK_HashMap<QGram, List<Record>>> idx;
	protected Object2IntOpenHashMap<Record> indexedCountList;
	protected Query query;
	protected final long threshold;

	protected final int indexK;
	protected final int qgramSize;
	protected final int[] indexPosition;

	protected int maxPosition = 0;

	public long qgramCount = 0;
//	public long candQGramCount = 0;
	public long candQGramCountSum = 0;
	public double candQGramAvgCount = 0;
	public int predictCount = 0;
	public long equivComparisons = 0;

	public long indexTime = 0;
	public long candQGramCountTime = 0;
	public long filterTime = 0;
	public long verifyTime = 0;
	public long joinTime = 0;

	double gamma;
	double zeta;
	double eta;
	
	public static boolean useLF = true;
	public static boolean usePQF = true;
	public static boolean useSTPQ = true;


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
	
	public JoinMHIndex(int indexK, int qgramSize, Iterable<Record> indexedSet, Query query, StatContainer stat,
			int[] indexPosition, boolean addStat, boolean useIndexCount, int threshold) {
		// TODO: Need to be fixed to make index just for given sequences
		// NOW, it makes index for all sequences
		
		long starttime = System.nanoTime();
		this.indexK = indexK;
		this.qgramSize = qgramSize;
		this.indexPosition = indexPosition;
		this.query = query;
		this.threshold = threshold;

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
		this.idx = new ArrayList<WYK_HashMap<QGram, List<Record>>>();

		long elements = 0;

		for (int i = 0; i < indexK; ++i) {
			// initialize with indexedSet size
			idx.add(new WYK_HashMap<QGram, List<Record>>(query.indexedSet.size()));
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

				Map<QGram, List<Record>> map = idx.get(i);
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
					Map<QGram, List<Record>> ithidx = idx.get(i);

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

		Util.printGCStats(stat, "Stat_Index");
	}
	
	protected List<List<QGram>> getCandidatePQGrams( Record rec ) {
		List<List<QGram>> availableQGrams = rec.getQGrams( qgramSize, maxPosition+1 );
		List<List<QGram>> candidatePQGrams = new ArrayList<List<QGram>>();
		for ( int k=0; k<availableQGrams.size(); ++k ) {
			if ( k >= idx.size() ) continue;
			WYK_HashMap<QGram, List<Record>> curidx = idx.get( k );
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
	
	protected void postprocessAfterJoin( StatContainer stat ) {
		this.candQGramAvgCount = 1.0 * this.candQGramCountSum / (query.searchedSet.size() - skipped);
		stat.add( "Stat_CandQGram_Sum", this.candQGramCountSum );
		stat.add( "Stat_CandQGram_Avg", this.candQGramAvgCount );
		stat.add( "Stat_Equiv_Comparison", this.equivComparisons );
		stat.add( "Stat_Skipped", skipped );
//		this.zeta = (double) totalCountTime / totalCountValue;
		// totalCountTime: time for generating TPQ supersets
		// totalCountValue: the size of TPQ supersets
		
//		this.eta = ((double) (this.joinTime - totalCountTime) / this.predictCount);
		// this.joinTime: time for counting and verifications
		// this.predictCount: the sum of minimum invokes (number of records in searchedSet to be verified) of records in indexedSet
		stat.add( "Result_5_1_Filter_Time", filterTime/1e6 );
		stat.add( "Result_5_2_Verify_Time", verifyTime/1e6 );
	}

	public void joinOneRecord( Record recS, Set<IntegerPair> rslt, Validator checker ) {
//	    boolean isUpperRecord = threshold <= 0 ? true : recS.getEstNumTransformed() > threshold;
//	    if (!isUpperRecord) return;

	    int candQGramCount = 0;
	    long ts = System.nanoTime();
		List<List<QGram>> availableQGrams = null;
		if ( useSTPQ ) availableQGrams = getCandidatePQGrams( recS );
		else {
			List<Set<QGram>> availableQGramsSet = new ObjectArrayList<>();
			for ( int k=0; k<indexK; ++k ) availableQGramsSet.add( new ObjectOpenHashSet<>() );
			for ( Record exp : recS.expandAll() ) {
				List<List<QGram>> qgramsList = exp.getSelfQGrams( qgramSize, indexK );
				int maxK = Math.min( indexK, qgramsList.size() );
				for ( int k=0; k<maxK; ++k ) {
					WYK_HashMap<QGram, List<Record>> curidx = idx.get( k );
					QGram qgram = qgramsList.get( k ).get( 0 );
					if ( !curidx.containsKey( qgram ) ) continue;
					else availableQGramsSet.get( k ).add( qgram );
				}
			}
			availableQGrams = new ObjectArrayList<>();
			for ( int k=0; k<indexK; ++k ) availableQGrams.add( new ObjectArrayList<>( availableQGramsSet.get( k ) ) );
		}
		for (List<QGram> list : availableQGrams) {
			candQGramCount += list.size();
		}
		this.candQGramCountSum += candQGramCount;
		long afterCandQgramTime = System.nanoTime();

		Object2IntOpenHashMap<Record> candidatesCount = new Object2IntOpenHashMap<Record>();
		candidatesCount.defaultReturnValue(0);
		int[] range = recS.getTransLengths();
		for (int i = 0; i < indexK; ++i) {
			int actualIndex = indexPosition[i];
			if (range[0] <= actualIndex) continue;

			ObjectOpenHashSet<Record> ithCandidates = new ObjectOpenHashSet<Record>();
			Map<QGram, List<Record>> map = idx.get(i);

			for (QGram qgram : availableQGrams.get(actualIndex)) {
				// if( debug ) {
				// System.out.println( "Q " + qgram + " " + actualIndex );
				// }

				// elements++;
				List<Record> list = map.get(qgram);
				if (list == null) {
//					++count_empty[i];
					continue;
				}
//				cand_sum[i] += list.size();
//				++count_cand[i];

				for (Record otherRecord : list) {
					// if( debug ) {
					// System.out.println( "record: " + otherRecord );
					// }

					if ( !useLF || StaticFunctions.overlap(otherRecord.size(), otherRecord.size(), range[0], range[1])) {
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
			// candidateTimes[i].stopQuiet();
		} // end for i from 0 to indexK
//		count += candidates.size();

		ObjectIterator<Entry<Record>> iter = candidatesCount.object2IntEntrySet().iterator();
		Set<Record> candidates = new WYK_HashSet<Record>(100);
		while (iter.hasNext()) {
			Entry<Record> entry = iter.next();
			Record record = entry.getKey();
			int recordCount = entry.getIntValue();

			if ( !usePQF || indexedCountList.getInt(record) <= recordCount || indexedCountList.getInt(recS) <= recordCount) {
				candidates.add(record);
			}
			else ++checker.pqgramFiltered;
		}
		long afterFilterTime = System.nanoTime();

		equivComparisons += candidates.size();
		for (Record recR : candidates) {
			int compare = checker.isEqual(recS, recR);
			if (compare >= 0) {
//				rslt.add(new IntegerPair(recS.getID(), recR.getID()));
				AlgorithmTemplate.addSeqResult( recS, recR, rslt, query.selfJoin );
			}
		}
		long afterVerifyTime = System.nanoTime();
		
		candQGramCountTime += afterCandQgramTime - ts;
		filterTime += afterFilterTime - afterCandQgramTime;
		verifyTime += afterVerifyTime - afterFilterTime;
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
		return idx.size();
	}
	
	public WYK_HashMap<QGram, List<Record>> get(int i) {
		return idx.get( i );
	}

	public int getIndexedCount( Record rec ) {
		return indexedCountList.getInt( rec );
	}

	public long getCountValue() {
		return candQGramCountSum;
	}
	
	public long getCandQGramCountSum() {
		return candQGramCountSum;
	}
	
	public double getCandQGramAvgCount() {
		return candQGramAvgCount;
	}

	public long getEquivComparisons() {
		return equivComparisons;
	}
	
	public int nInvList() {
		int n = 0;
		for (int i=0; i<idx.size(); i++) {
			n += idx.get( i ).size();
		}
		return n;
	}
	
	public void writeToFile() {
		BufferedWriter bw;
		try { 
			bw = new BufferedWriter( new FileWriter( "tmp/JoinMHIndex.txt" ) ); 
			for ( int pos=0; pos<idx.size(); ++pos ) {
				bw.write( "pos: "+indexPosition[pos]+"\n" );
				for ( Map.Entry<QGram, List<Record>> entry : idx.get( pos ).entrySet() ) {
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
