package snu.kdd.synonym.synonymRev.index;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.RecordInt;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.MinPositionQueue;
import snu.kdd.synonym.synonymRev.tools.MinPositionQueue.MinPosition;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.QGramRange;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StaticFunctions;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.tools.WYK_HashMap;
import snu.kdd.synonym.synonymRev.tools.WYK_HashSet;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class JoinMinRangeIndex {
	WYK_HashMap<QGram, List<RecordInt>> idx;
	ArrayList<Integer> countPerPosition = null;
	Object2IntOpenHashMap<Record> indexedCountMap;
	Object2IntOpenHashMap<Record> estimatedCountMap;
	WYK_HashSet<Integer> bypassSet = null;

	public double gamma;
	public double delta;
	public double epsilon;
	public double epsilonPrime;

	private int qSize;

	public long searchedTotalSigCount;
	public long indexedTotalSigCount;
	public long equivComparisons;
	public long comparisonTime;
	public long appliedRulesSum;

	long getQGramTime;
	public long comparisonCount;
	public long lengthFiltered;

	public double joinTime;
	public double indexTime;
	public double countTime;

	public long predictCount;

	private int maxPosition = 0;

	public static class RangeCount {

		public Int2IntOpenHashMap rangeToCount;

		public RangeCount( int min, int max ) {
			int size = ( max - min ) * 2;
			if( size < 10 ) {
				size = 10;
			}
			rangeToCount = new Int2IntOpenHashMap( size );
			increase( min, max );
		}

		public void increase( int min, int max ) {
			for( int i = min; i <= max; i++ ) {
				increaseOne( i );
			}
		}

		public void increaseOne( int x ) {
			int count = rangeToCount.get( x );
			rangeToCount.put( x, count + 1 );
		}

		public int getCount( int x ) {
			return rangeToCount.get( x );
		}
	}

	public JoinMinRangeIndex( int nIndex, int qSize, StatContainer stat, Query query, boolean writeResult ) {
		this.idx = new WYK_HashMap<QGram, List<RecordInt>>();
		this.qSize = qSize;

		if( DEBUG.JoinMinIndexON ) {
			this.countPerPosition = new ArrayList<Integer>();
		}

		long starttime = System.nanoTime();

		// Build an index
		// Count Invokes per each (token, loc) pair
		Object2ObjectOpenHashMap<QGram, RangeCount> invokes = new Object2ObjectOpenHashMap<QGram, RangeCount>();
		long getQGramTime = 0;
		long countIndexingTime = 0;

		BufferedWriter bw_debug_count = null;

		if( DEBUG.PrintJoinMinIndexON ) {
			try {
				bw_debug_count = new BufferedWriter( new FileWriter( "JoinMinRange_Index_Count_Debug.txt" ) );
			}
			catch( IOException e ) {
				e.printStackTrace();
			}
		}

		StopWatch stepTime = StopWatch.getWatchStarted( "Result_3_1_1_Index_Count_Time" );
		// count number of occurrence of a positional q-grams
		long recordStartTime = 0;
		long recordMidTime = 0;

		for( Record rec : query.searchedSet.get() ) {
			if( DEBUG.JoinMinON ) {
				recordStartTime = System.nanoTime();
			}

			List<QGramRange> availableQGrams = rec.getQGramRange( qSize );

			if( DEBUG.JoinMinON ) {
				recordMidTime = System.nanoTime();
				getQGramTime += recordMidTime - recordStartTime;
			}

			long qgramCount = 0;
			for( QGramRange qgram : availableQGrams ) {
				qgramCount += qgram.max - qgram.min + 1;

				RangeCount count = invokes.get( qgram.qgram );
				if( count == null ) {
					invokes.put( qgram.qgram, new RangeCount( qgram.min, qgram.max ) );
				}
				else {
					count.increase( qgram.min, qgram.max );
				}

				// if( DEBUG.JoinMinIndexON ) {
				// try {
				// for( int i = qgram.min; i <= qgram.max; i++ ) {
				// bw_debug_count.write( "qg " + qgram.qgram + " " + i + "\n" );
				// }
				// }
				// catch( IOException e ) {
				// e.printStackTrace();
				// }
				// }
			}
			this.searchedTotalSigCount += qgramCount;

			if( DEBUG.JoinMinON ) {
				countIndexingTime += System.nanoTime() - recordMidTime;
			}

			if( DEBUG.PrintJoinMinIndexON ) {
				try {
					bw_debug_count.write( qgramCount + " " );
					bw_debug_count.write( recordMidTime - recordStartTime + " " );
					bw_debug_count.write( "\n" );
				}
				catch( IOException e ) {
					e.printStackTrace();
				}
			}
		}

		if( DEBUG.PrintJoinMinIndexON ) {
			ObjectIterator<Entry<QGram, RangeCount>> iter = invokes.entrySet().iterator();
			try {
				while( iter.hasNext() ) {
					Entry<QGram, RangeCount> entry = iter.next();
					RangeCount c = entry.getValue();
					QGram q = entry.getKey();

					ObjectIterator<it.unimi.dsi.fastutil.ints.Int2IntMap.Entry> count_iter = c.rangeToCount.int2IntEntrySet()
							.iterator();
					while( count_iter.hasNext() ) {

						bw_debug_count.write( "Inv: " + q + " " + count_iter.next() + "\n" );

					}
				}
				bw_debug_count.close();
			}
			catch( IOException e ) {
				e.printStackTrace();
			}
		}
		// we have the number of occurrence of a positional q-grams

		this.countTime = System.nanoTime() - starttime;
		this.gamma = this.countTime / this.searchedTotalSigCount;

		if( DEBUG.JoinMinON ) {
			Util.printLog( "Step 1 Time : " + this.countTime );
			Util.printLog( "Gamma (buildTime / signature): " + this.gamma );

			if( writeResult ) {
				stat.add( "Est_Index_0_GetQGramTime", getQGramTime );
				stat.add( "Est_Index_0_CountIndexingTime", countIndexingTime );

				stat.add( "Est_Index_1_Index_Count_Time", this.countTime );
				stat.add( "Est_Index_1_Time_Per_Sig", Double.toString( this.gamma ) );
			}
		}

		starttime = System.nanoTime();

		if( writeResult ) {
			stepTime.stopAndAdd( stat );
		}
		else {
			stepTime.stop();
		}

		BufferedWriter bw_index = null;

		if( DEBUG.PrintJoinMinIndexON ) {
			try {
				bw_index = new BufferedWriter( new FileWriter( "JoinMinRange_Index_Content.txt" ) );
			}
			catch( IOException e ) {
				e.printStackTrace();
			}
		}

		stepTime.resetAndStart( "Result_3_1_2_Indexing_Time" );

		this.predictCount = 0;
		long indexedElements = 0;

		// find best K positions for each string in T
		indexedCountMap = new Object2IntOpenHashMap<>();
		estimatedCountMap = new Object2IntOpenHashMap<>();
		for( Record rec : query.indexedSet.get() ) {

			int searchmax = Math.min( rec.getMinTransLength(), invokes.size() );

			List<QGramRange> availableQGrams = null;

			if( query.oneSideJoin ) {
				availableQGrams = rec.getSelfQGramsWithRange( qSize );
			}
			else {
				availableQGrams = rec.getQGramRange( qSize );
			}

			for( QGramRange qgram : availableQGrams ) {
				this.indexedTotalSigCount += qgram.max - qgram.min + 1;
			}

			MinPositionQueue mpq = new MinPositionQueue( nIndex );

			int[] positionalCount = new int[ searchmax ];
			for( QGramRange qgramRange : availableQGrams ) {
				// There is no invocation count: this is the minimum point
				RangeCount range = invokes.get( qgramRange.qgram );
				if( range != null ) {
					for( int i = qgramRange.min; i <= qgramRange.max; i++ ) {
						if( i >= searchmax ) {
							break;
						}
						int c = range.getCount( i );
						positionalCount[ i ] += c;
					}
				}
			}

			for( int i = 0; i < searchmax; i++ ) {
				if( DEBUG.PrintJoinMinIndexON ) {
					try {
						bw_index.write( "pos " + i + " " + positionalCount[ i ] + "\n" );
					}
					catch( IOException e ) {
						e.printStackTrace();
					}
				}
				mpq.add( i, positionalCount[ i ] );
			}

			this.predictCount += mpq.minInvokes;

			int indexedCount = 0;
			int[] indexedPosition = new int[ mpq.size() ];
			double[] candidateCount = new double[ mpq.size() ];
			int p = 0;
			while( !mpq.isEmpty() ) {
				indexedCount++;
				MinPosition minPos = mpq.poll();
				indexedPosition[ p ] = minPos.positionIndex;
				candidateCount[ p ] = minPos.candidateCount;

				if( maxPosition < minPos.positionIndex ) {
					maxPosition = minPos.positionIndex;
				}

				if( DEBUG.PrintJoinMinIndexON ) {

					try {
						bw_index.write( minPos.positionIndex + " " + minPos.candidateCount + "\n" );
					}
					catch( IOException e ) {
						e.printStackTrace();
					}
				}
				p++;
			}

			Arrays.sort( indexedPosition );
			if( DEBUG.PrintJoinMinIndexON ) {
				try {
					bw_index.write( "indexed " + Arrays.toString( indexedPosition ) + "\n" );
				}
				catch( IOException e ) {
					e.printStackTrace();
				}
			}

			for( QGramRange qgramRange : availableQGrams ) {
				// since indexedPosition is sorted by ascending order,
				// if minIdx > qgramRange.max, we skip the qgramRange
				if( DEBUG.PrintJoinMinIndexON ) {
					try {
						bw_index.write( "qgramRange " + qgramRange.qgram + " " + qgramRange.min + " " + qgramRange.max + "\n" );
					}
					catch( IOException e ) {
						e.printStackTrace();
					}
				}
				for( int minIdx : indexedPosition ) {
					// write2File(bw, minIdx, twogram, rec.getID());

					if( minIdx > qgramRange.max ) {
						break;
					}
					else if( qgramRange.min <= minIdx ) {
						if( DEBUG.PrintJoinMinIndexON ) {
							try {
								bw_index.write( minIdx + ", " + qgramRange.qgram + " : " + rec + " " + "\n" );
							}
							catch( IOException e ) {
								e.printStackTrace();
							}
						}

						this.put( minIdx, qgramRange.qgram, rec );
						if( DEBUG.JoinMinON ) {
							indexedElements += 1;
						}
					}
				}
			}

			indexedCountMap.put( rec, indexedCount );
		}

		this.indexTime = System.nanoTime() - starttime;
		this.delta = this.indexTime / this.indexedTotalSigCount;

		if( writeResult ) {
			stepTime.stopAndAdd( stat );
		}
		else {
			stepTime.stop();
		}

		if( DEBUG.JoinMinON ) {
			Util.printLog( "Idx size : " + indexedElements );
			Util.printLog( "Predict : " + this.predictCount );
			Util.printLog( "Step 2 Time : " + this.indexTime );
			Util.printLog( "Delta (index build / signature ): " + this.delta );

			if( writeResult ) {
				stat.add( "Stat_JoinMin_Index_Size", indexedElements );
				stat.add( "Stat_Predicted_Comparison", this.predictCount );

				stat.add( "Est_Index_2_Build_Index_Time", this.indexTime );
				stat.add( "Est_Index_2_Time_Per_Sig", Double.toString( this.delta ) );
			}
		}

		if( DEBUG.PrintJoinMinIndexON ) {
			try {
				bw_index.close();
			}
			catch( IOException e ) {
				e.printStackTrace();
			}
		}

		if( writeResult ) {
			this.addStat( stat );
			stat.add( "Counter_Index_1_HashCollision", WYK_HashSet.collision );
			stat.add( "Counter_Index_1_HashResize", WYK_HashSet.resize );
		}
		invokes.clear();
	}

	public void addStat( StatContainer stat ) {
		if( DEBUG.JoinMinIndexON ) {
			for( int i = 0; i < countPerPosition.size(); i++ ) {
				stat.add( String.format( "Stat_JoinMin_COUNT%02d", i ), countPerPosition.get( i ) );
			}
		}
	}

	public void put( int position, QGram qgram, Record rec ) {
		List<RecordInt> list = idx.get( qgram );

		if( list == null ) {
			list = new ArrayList<RecordInt>();
			idx.put( qgram, list );
		}

		list.add( new RecordInt( rec, position ) );
	}

	// utilizes k positions
	public List<IntegerPair> joinMaxK( int indexK, boolean writeResult, StatContainer stat, Validator checker, Query query ) {
		BufferedWriter bw = null;

		if( DEBUG.PrintJoinMinJoinON ) {
			try {
				bw = new BufferedWriter( new FileWriter( "JoinMinRange_Join_Debug.txt" ) );
			}
			catch( Exception e ) {
				e.printStackTrace();
			}
		}

		List<IntegerPair> rslt = new ArrayList<IntegerPair>();

		long joinStartTime = System.nanoTime();
		for( Record recS : query.searchedSet.get() ) {
			// long startTime = System.currentTimeMillis();

			joinRecordMaxK( indexK, recS, rslt, writeResult, bw, checker, query.oneSideJoin );

			// long executionTime = System.currentTimeMillis() - startTime;
			// if( executionTime > 0 ) {
			// Util.printLog( recS.getID() + " processed " + executionTime );
			// }
		}
		joinTime = System.nanoTime() - joinStartTime;

		if( DEBUG.PrintJoinMinJoinON ) {
			try {
				bw.close();
			}
			catch( IOException e ) {
				e.printStackTrace();
			}
		}

		if( comparisonCount == 0 ) {
			// To avoid NaN
			comparisonCount = 1;
		}

		if( predictCount == 0 ) {
			Util.printLog( "Warning: predictCount is zero" );
			predictCount = 1;
		}
		epsilon = joinTime / predictCount;

		// DEBUG
		epsilonPrime = joinTime / comparisonCount;

		if( DEBUG.JoinMinON ) {
			Util.printLog( "Est weight : " + comparisonCount );
			Util.printLog( "Join time : " + joinTime );
			Util.printLog( "Epsilon : " + epsilon );

			if( writeResult ) {
				// stat.add( "Last Token Filtered", lastTokenFiltered );
				stat.add( "Est_Join_0_GetQGramTime", getQGramTime );
				stat.add( "Stat_Length_Filtered", lengthFiltered );
				stat.add( "Result_3_2_1_Equiv_Checking_Time", comparisonTime / 1000000 );
			}
		}

		if( writeResult ) {
			stat.add( "Stat_Equiv_Comparison", equivComparisons );
			stat.add( "Join_Min_Result", rslt.size() );
		}

		if( DEBUG.PrintJoinMinJoinON ) {
			try {
				bw.close();
			}
			catch( Exception e ) {
				e.printStackTrace();
			}
		}

		return rslt;
	}

	public void joinRecord( Record recS, List<IntegerPair> rslt, boolean writeResult, BufferedWriter bw, Validator checker,
			boolean oneSideJoin ) {
		long qgramStartTime = 0;
		long joinStartTime = 0;
		long qgramCount = 0;

		if( DEBUG.JoinMinIndexON ) {
			qgramStartTime = System.nanoTime();
		}

		List<QGramRange> availableQGrams = recS.getQGramRange( qSize );

		// DEBUG
		// boolean debug = false;
		// if( recS.toString().equals( "create new screennames " ) ) {
		// debug = true;
		// }

		if( DEBUG.JoinMinON ) {
			getQGramTime += System.nanoTime() - qgramStartTime;
		}

		int[] range = recS.getTransLengths();

		if( DEBUG.PrintJoinMinJoinON ) {
			joinStartTime = System.nanoTime();
		}

		Set<Record> candidates = new WYK_HashSet<Record>();

		for( QGramRange qgramRange : availableQGrams ) {
			if( DEBUG.PrintJoinMinJoinON ) {
				qgramCount++;
			}

			// if( debug ) {
			// System.out.println( "D " + qgram );
			// }

			List<RecordInt> tree = idx.get( qgramRange.qgram );

			if( tree == null ) {
				continue;
			}

			for( RecordInt e : tree ) {
				if( qgramRange.min <= e.index && e.index <= qgramRange.max ) {
					if( oneSideJoin ) {
						int eTokenCount = e.record.getTokenCount();
						if( StaticFunctions.overlap( eTokenCount, eTokenCount, range[ 0 ], range[ 1 ] ) ) {
							candidates.add( e.record );
							comparisonCount++;
						}
						else {
							lengthFiltered++;
						}
					}
					else {
						int[] eTransLength = e.record.getTransLengths();
						if( StaticFunctions.overlap( eTransLength[ 0 ], eTransLength[ 1 ], range[ 0 ], range[ 1 ] ) ) {
							candidates.add( e.record );
							comparisonCount++;
						}
						else {
							lengthFiltered++;
						}
					}
				}
			}

			equivComparisons += candidates.size();
			for( Record recR : candidates ) {
				// long ruleiters = 0;
				// long reccalls = 0;
				// long entryiters = 0;
				//
				// if( DEBUG.JoinMinON ) {
				// ruleiters = Validator.niterrules;
				// reccalls = Validator.recursivecalls;
				// entryiters = Validator.niterentry;
				// }

				long st = System.nanoTime();
				int compare = checker.isEqual( recS, recR );

				// if( debug ) {
				// System.out.println( "comp " + recR.toString() + " " + recR.getID() + " " + recS.toString() + " "
				// + recS.getID() + " " + compare );
				// }

				long duration = System.nanoTime() - st;

				// if( DEBUG.JoinMinON ) {
				// ruleiters = Validator.niterrules - ruleiters;
				// reccalls = Validator.recursivecalls - reccalls;
				// entryiters = Validator.niterentry - entryiters;
				// }

				comparisonTime += duration;
				if( compare >= 0 ) {
					rslt.add( new IntegerPair( recS.getID(), recR.getID() ) );
					appliedRulesSum += compare;
				}
			}
		}

		if( DEBUG.PrintJoinMinJoinON ) {
			long joinTime = System.nanoTime() - joinStartTime;

			try {
				bw.write( "" + qgramCount );
				bw.write( " " + joinTime );
				bw.write( "\n" );
			}
			catch( IOException e ) {
				e.printStackTrace();
			}
		}
	}

	public void joinRecordMaxK( int nIndex, Record recS, List<IntegerPair> rslt, boolean writeResult, BufferedWriter bw,
			Validator checker, boolean oneSideJoin ) {
		long qgramStartTime = 0;
		long joinStartTime = 0;
		long qgramCount = 0;

		if( DEBUG.JoinMinON ) {
			qgramStartTime = System.nanoTime();
		}

		List<QGramRange> availableQGrams = recS.getQGramRange( qSize );
		// int searchmax = Integer.min( availableQGrams.size(), idx.size() );

		if( DEBUG.JoinMinON ) {
			getQGramTime += System.nanoTime() - qgramStartTime;
		}

		int[] range = recS.getTransLengths();
		ArrayList<String> debugArray = new ArrayList<String>();
		if( DEBUG.PrintJoinMinJoinON ) {
			joinStartTime = System.nanoTime();
		}

		JoinMinCandidateSet allCandidateSet = new JoinMinCandidateSet( nIndex, recS, estimatedCountMap.getInt( recS ) );

		for( QGramRange qgramRange : availableQGrams ) {
			WYK_HashSet<Record> candidates = new WYK_HashSet<Record>();

			if( DEBUG.PrintJoinMinJoinON ) {
				for( int i = qgramRange.min; i <= qgramRange.max; i++ ) {
					if( i > maxPosition ) {
						break;
					}
					debugArray.add( "q :" + qgramRange.qgram + " " + i + "\n" );
					qgramCount++;
				}
			}

			List<RecordInt> tree = idx.get( qgramRange.qgram );

			if( tree == null ) {
				continue;
			}

			for( RecordInt e : tree ) {
				if( qgramRange.min <= e.index && e.index <= qgramRange.max ) {
					if( oneSideJoin ) {
						if( StaticFunctions.overlap( e.record.getTokenCount(), e.record.getTokenCount(), range[ 0 ],
								range[ 1 ] ) ) {

							if( DEBUG.PrintJoinMinJoinON ) {
								debugArray.add( "Cand: " + e.record + " by " + qgramRange.qgram + "\n" );
							}
							candidates.add( e.record );
							comparisonCount++;
						}
						else {
							lengthFiltered++;
						}
					}
					else {
						if( StaticFunctions.overlap( e.record.getMinTransLength(), e.record.getMaxTransLength(), range[ 0 ],
								range[ 1 ] ) ) {
							candidates.add( e.record );
							comparisonCount++;
						}
						else {
							lengthFiltered++;
						}
					}
				}
			}

			allCandidateSet.add( candidates );
		}

		ArrayList<Record> candSet = allCandidateSet.getCandSet( indexedCountMap );

		equivComparisons += candSet.size();
		for( Record recR : candSet ) {
			// long ruleiters = 0;
			// long reccalls = 0;
			// long entryiters = 0;
			//
			// if( DEBUG.JoinMinON ) {
			// ruleiters = Validator.niterrules;
			// reccalls = Validator.recursivecalls;
			// entryiters = Validator.niterentry;
			// }

			long st = System.nanoTime();
			int compare = checker.isEqual( recS, recR );
			long duration = System.nanoTime() - st;

			// if( DEBUG.JoinMinON ) {
			// ruleiters = Validator.niterrules - ruleiters;
			// reccalls = Validator.recursivecalls - reccalls;
			// entryiters = Validator.niterentry - entryiters;
			// }

			comparisonTime += duration;
			if( compare >= 0 ) {
				rslt.add( new IntegerPair( recS.getID(), recR.getID() ) );
				appliedRulesSum += compare;
				if( DEBUG.PrintJoinMinJoinON ) {
					debugArray.add( "Val " + recS + " : " + recR );
				}

			}
		}

		if( DEBUG.PrintJoinMinJoinON ) {
			long joinTime = System.nanoTime() - joinStartTime;

			try {
				Collections.sort( debugArray );
				for( String temp : debugArray ) {
					bw.write( temp );
				}

				bw.write( "" + qgramCount );
				bw.write( " " + joinTime );
				bw.write( "\n" );
			}
			catch( IOException e ) {
				e.printStackTrace();
			}
		}
	}

	public void clear() {
		for( int i = 0; i < idx.size(); i++ ) {
			idx.get( i ).clear();
		}
		idx.clear();
	}

	public void DebugWriteToFile( String filename ) {
		try {
			BufferedWriter bw = new BufferedWriter( new FileWriter( filename ) );

			for( Entry<QGram, List<RecordInt>> entry : idx.entrySet() ) {
				bw.write( entry.getKey().toString() );

				List<RecordInt> list = entry.getValue();

				for( int idx = 0; idx < list.size(); idx++ ) {
					bw.write( " " + list.get( idx ).record.getID() );
				}
				bw.write( "\n" );
			}

			bw.close();
		}
		catch( IOException e ) {
			e.printStackTrace();
		}
	}

	public double estimatedCountTime( double gamma ) {
		return gamma * searchedTotalSigCount;
	}

	public double estimatedIndexTime( double delta ) {
		return delta * indexedTotalSigCount;
	}

	public double estimatedJoinTime( double epsilon ) {
		return epsilon * predictCount;
	}

	public static class JoinMinCandidateSet {
		int nIndex;

		WYK_HashMap<Record, Integer> appearingMap = null;

		public JoinMinCandidateSet( int nIndex, Record rec, int predictedInvokes ) {
			this.nIndex = nIndex;

			if( predictedInvokes < 10 ) {
				appearingMap = new WYK_HashMap<Record, Integer>( 10 );
			}
			else {
				appearingMap = new WYK_HashMap<Record, Integer>( predictedInvokes * 2 );
			}
		}

		public void add( WYK_HashSet<Record> set ) {
			for( Record r : set ) {
				Integer count = appearingMap.get( r );

				if( count == null ) {
					appearingMap.put( r, 1 );
				}
				else {
					appearingMap.put( r, count + 1 );
				}
			}
		}

		public ArrayList<Record> getCandSet( Object2IntOpenHashMap<Record> indexedCountMap ) {
			ArrayList<Record> list = new ArrayList<Record>( appearingMap.size() );
			Iterator<Entry<Record, Integer>> iter = appearingMap.entrySet().iterator();

			while( iter.hasNext() ) {
				Entry<Record, Integer> entry = iter.next();

				Record r = entry.getKey();
				if( indexedCountMap.getInt( r ) == entry.getValue() ) {
					list.add( r );
				}
			}

			return list;
		}
	}
}
