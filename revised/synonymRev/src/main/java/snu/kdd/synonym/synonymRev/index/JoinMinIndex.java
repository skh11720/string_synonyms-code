package snu.kdd.synonym.synonymRev.index;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import snu.kdd.synonym.synonymRev.algorithm.misc.EstimationTest;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.Histogram;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.MinPositionQueue;
import snu.kdd.synonym.synonymRev.tools.MinPositionQueue.MinPosition;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StaticFunctions;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.tools.WYK_HashMap;
import snu.kdd.synonym.synonymRev.tools.WYK_HashSet;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class JoinMinIndex {
	ArrayList<WYK_HashMap<QGram, List<Record>>> idx;
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

	public JoinMinIndex( int nIndex, int qSize, StatContainer stat, Query query, int threshold, boolean writeResult ) {
		this.idx = new ArrayList<WYK_HashMap<QGram, List<Record>>>();
		this.qSize = qSize;

		boolean hybridIndex = threshold != 0;

		if( DEBUG.JoinMinIndexON ) {
			this.countPerPosition = new ArrayList<Integer>();
		}

		long starttime = System.nanoTime();

		// Build an index
		// Count Invokes per each (token, loc) pair
		List<Object2IntOpenHashMap<QGram>> invokes = new ArrayList<Object2IntOpenHashMap<QGram>>();
		List<Object2IntOpenHashMap<QGram>> lowInvokes = null;

		if( hybridIndex ) {
			if( !query.oneSideJoin ) {
				// we do not have to compute the lowInvokes for the oneSideJoin
				lowInvokes = new ArrayList<Object2IntOpenHashMap<QGram>>();
			}
		}

		long getQGramTime = 0;
		long countIndexingTime = 0;

		BufferedWriter bw = null;

		if( DEBUG.JoinMinIndexON ) {
			try {
				bw = new BufferedWriter( new FileWriter( "JoinMin_Index_Count_Debug_" + writeResult + ".txt" ) );
			}
			catch( IOException e ) {
				e.printStackTrace();
			}
		}

		StopWatch stepTime = null;
		if( writeResult ) {
			stepTime = StopWatch.getWatchStarted( "Result_3_1_1_Index_Count_Time" );
		}
		// count number of occurrence of a positional q-grams
		long recordStartTime = 0;
		long recordMidTime = 0;

		for( Record rec : query.searchedSet.get() ) {

			if( DEBUG.JoinMinON ) {
				recordStartTime = System.nanoTime();
			}

			List<List<QGram>> availableQGrams = rec.getQGrams( qSize );

			if( DEBUG.JoinMinON ) {
				recordMidTime = System.nanoTime();
				getQGramTime += recordMidTime - recordStartTime;
			}

			int searchmax = availableQGrams.size();

			for( int i = invokes.size(); i < searchmax; i++ ) {
				Object2IntOpenHashMap<QGram> inv = new Object2IntOpenHashMap<QGram>();
				inv.defaultReturnValue( 0 );

				invokes.add( inv );

				if( DEBUG.JoinMinIndexON ) {
					this.countPerPosition.add( 0 );
				}
			}

			boolean isLowRecord = hybridIndex && ( rec.getEstNumTransformed() <= threshold );

			long qgramCount = 0;
			for( int i = 0; i < searchmax; ++i ) {
				Object2IntOpenHashMap<QGram> curridx_invokes = null;
				if( !isLowRecord ) {
					// it is not the hybrid index or not low record for the hybrid index
					curridx_invokes = invokes.get( i );
				}
				else {
					if( query.oneSideJoin ) {
						// if uni-directional join && isLowRecord, this record will not be compared with joinMin index
						break;
					}
					curridx_invokes = lowInvokes.get( i );
				}

				List<QGram> available = availableQGrams.get( i );
				qgramCount += available.size();
				for( QGram qgram : available ) {
					int count = curridx_invokes.getInt( qgram );
					if( count == 0 ) {
						curridx_invokes.put( qgram, 1 );
					}
					else {
						curridx_invokes.put( qgram, count + 1 );
					}
				}

				if( DEBUG.JoinMinIndexON ) {
					int newSize = this.countPerPosition.get( i ) + available.size();

					this.countPerPosition.set( i, newSize );
				}
			}
			this.searchedTotalSigCount += qgramCount;

			if( DEBUG.JoinMinON ) {
				countIndexingTime += System.nanoTime() - recordMidTime;
			}

			if( DEBUG.JoinMinIndexON ) {
				try {
					bw.write( qgramCount + " " );
					bw.write( recordMidTime - recordStartTime + " " );
					bw.write( "\n" );
				}
				catch( IOException e ) {
					e.printStackTrace();
				}
			}
		}

		if( DEBUG.JoinMinIndexON ) {
			try {
				for( int i = 0; i < invokes.size(); i++ ) {
					Object2IntOpenHashMap<QGram> count = invokes.get( i );
					ObjectIterator<Object2IntMap.Entry<QGram>> iter = count.object2IntEntrySet().iterator();

					while( iter.hasNext() ) {
						Object2IntMap.Entry<QGram> entry = iter.next();

						QGram q = entry.getKey();
						int c = entry.getIntValue();
						bw.write( "Inv: " + q + " " + i + "=>" + c + "\n" );
					}
				}

				bw.close();
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
			stepTime.resetAndStart( "Result_3_1_2_Indexing_Time" );
		}
		else {
			if( DEBUG.SampleStatON ) {
				System.out.println( "[Gamma] " + gamma );
				System.out.println( "[Gamma] CountTime " + countTime );
				System.out.println( "[Gamma] SearchedSigCount " + searchedTotalSigCount );
			}
			if( DEBUG.PrintEstimationON ) {
				BufferedWriter bwEstimation = EstimationTest.getWriter();
				try {
					bwEstimation.write( "[Gamma] " + gamma );
					bwEstimation.write( " CountTime " + countTime );
					bwEstimation.write( " SearchedSigCount " + searchedTotalSigCount + "\n" );
				}
				catch( Exception e ) {
					e.printStackTrace();
				}
			}
		}

		BufferedWriter bw_index = null;

		if( DEBUG.PrintJoinMinIndexON ) {
			try {
				bw_index = new BufferedWriter( new FileWriter( "JoinMin_Index_Content.txt" ) );
			}
			catch( IOException e ) {
				e.printStackTrace();
			}
		}

		this.predictCount = 0;
		long indexedElements = 0;

		// find best K positions for each string in T
		indexedCountMap = new Object2IntOpenHashMap<>();
		estimatedCountMap = new Object2IntOpenHashMap<>();
		for( Record rec : query.indexedSet.get() ) {
			int[] range = rec.getTransLengths();

			int searchmax = Math.min( range[ 0 ], invokes.size() );

			List<List<QGram>> availableQGrams = null;

			boolean isLowRecord = hybridIndex && ( rec.getEstNumTransformed() <= threshold );

			if( query.oneSideJoin ) {
				availableQGrams = rec.getSelfQGrams( qSize, searchmax );
				// System.out.println( availableQGrams.toString() );
			}
			else {
				availableQGrams = rec.getQGrams( qSize, searchmax );
			}

			for( List<QGram> set : availableQGrams ) {
				this.indexedTotalSigCount += set.size();
			}

			MinPositionQueue mpq = new MinPositionQueue( nIndex );

			for( int i = 0; i < searchmax; ++i ) {
				// There is no invocation count: this is the minimum point
				if( i >= invokes.size() ) {
					mpq.add( i, 0 );

					if( DEBUG.PrintJoinMinIndexON ) {
						try {
							bw_index.write( "pos " + i + " 0\n" );
						}
						catch( IOException e ) {
							e.printStackTrace();
						}
					}
					continue;
				}

				Object2IntOpenHashMap<QGram> curridx_invokes = invokes.get( i );
				Object2IntOpenHashMap<QGram> curridx_lowInvokes = null;
				if( hybridIndex ) {
					curridx_lowInvokes = lowInvokes.get( i );
				}
				int invoke = 0;

				for( QGram qgram : availableQGrams.get( i ) ) {
					int count = curridx_invokes.getInt( qgram );

					if( !query.oneSideJoin && !isLowRecord ) {
						count += curridx_lowInvokes.getInt( qgram );
					}

					if( count != 0 ) {
						// upper bound
						invoke += count;
					}
				}

				if( DEBUG.PrintJoinMinIndexON ) {
					try {
						bw_index.write( "pos " + i + " " + invoke + "\n" );
					}
					catch( IOException e ) {
						e.printStackTrace();
					}
				}

				mpq.add( i, invoke );
			}

			this.predictCount += mpq.minInvokes;

			int indexedCount = 0;
			while( !mpq.isEmpty() ) {
				indexedCount++;

				MinPosition minPos = mpq.poll();
				int minIdx = minPos.positionIndex;

				if( DEBUG.PrintJoinMinIndexON ) {
					try {
						bw_index.write( minPos.positionIndex + " " + minPos.candidateCount + "\n" );
					}
					catch( IOException e ) {
						e.printStackTrace();
					}
				}

				this.setIndex( minIdx );
				for( QGram qgram : availableQGrams.get( minIdx ) ) {
					// write2File(bw, minIdx, twogram, rec.getID());

					if( DEBUG.PrintJoinMinIndexON ) {
						try {
							bw_index.write( minIdx + ", " + qgram + " : " + rec + "\n" );
						}
						catch( IOException e ) {
							e.printStackTrace();
						}
					}

					this.put( minIdx, qgram, rec );
				}
				if( DEBUG.JoinMinON ) {
					indexedElements += availableQGrams.get( minIdx ).size();
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
			if( DEBUG.PrintEstimationON ) {
				BufferedWriter bwEstimation = EstimationTest.getWriter();
				try {
					bwEstimation.write( "[Delta] " + delta );
					bwEstimation.write( " IndexTime " + indexTime );
					bwEstimation.write( " IndexedSigCount " + indexedTotalSigCount + "\n" );
				}
				catch( IOException e ) {
					e.printStackTrace();
				}
			}
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
			Util.printGCStats( stat, "Stat_Index" );
		}

		for( Object2IntOpenHashMap<QGram> in : invokes ) {
			in.clear();
		}
	}

	public void setIndex( int position ) {
		while( idx.size() <= position ) {
			idx.add( new WYK_HashMap<QGram, List<Record>>() );
		}
	}

	public void addStat( StatContainer stat ) {
		if( DEBUG.JoinMinIndexON ) {
			for( int i = 0; i < countPerPosition.size(); i++ ) {
				stat.add( String.format( "Stat_JoinMin_COUNT%02d", i ), countPerPosition.get( i ) );
			}

			for( int i = 0; i < idx.size(); i++ ) {
				if( idx.get( i ).size() != 0 ) {
					stat.add( String.format( "Stat_JoinMin_IDX%02d", i ), idx.get( i ).size() );
				}
			}
		}
	}

	public void put( int position, QGram qgram, Record rec ) {
		Map<QGram, List<Record>> map = idx.get( position );

		List<Record> list = map.get( qgram );
		if( list == null ) {
			list = new ArrayList<Record>();
			map.put( qgram, list );
		}

		list.add( rec );
	}

	public List<IntegerPair> joinMaxK( int indexK, boolean writeResult, StatContainer stat, Validator checker, Query query ) {
		BufferedWriter bw = null;

		if( DEBUG.PrintJoinMinJoinON ) {
			try {
				bw = new BufferedWriter( new FileWriter( "JoinMin_Join_Debug.txt" ) );
			}
			catch( Exception e ) {
				e.printStackTrace();
			}
		}

		List<IntegerPair> rslt = new ArrayList<IntegerPair>();

		long joinStartTime = System.nanoTime();
		for( Record recS : query.searchedSet.get() ) {

			joinRecordMaxK( indexK, recS, rslt, writeResult, bw, checker, query.oneSideJoin );

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
		else {
			if( DEBUG.SampleStatON ) {
				System.out.println( "[Epsilon] " + epsilon );
				System.out.println( "[Epsilon] JoinTime " + joinTime );
				System.out.println( "[Epsilon] PredictCount " + predictCount );
				System.out.println( "[Epsilon] ActualCount " + equivComparisons );
			}

			if( DEBUG.PrintEstimationON ) {
				BufferedWriter bwEstimation = EstimationTest.getWriter();
				try {
					bwEstimation.write( "[Epsilon] " + epsilon );
					bwEstimation.write( " JoinTime " + joinTime );
					bwEstimation.write( " PredictCount " + predictCount );
					bwEstimation.write( " ActualCount " + equivComparisons + "\n" );
				}
				catch( Exception e ) {
					e.printStackTrace();
				}
			}
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

	@Deprecated
	public void joinRecord( Record recS, List<IntegerPair> rslt, boolean writeResult, BufferedWriter bw, Validator checker,
			boolean oneSideJoin ) {
		long qgramStartTime = 0;
		long joinStartTime = 0;
		long qgramCount = 0;

		if( DEBUG.JoinMinIndexON ) {
			qgramStartTime = System.nanoTime();
		}

		List<List<QGram>> availableQGrams = recS.getQGrams( qSize );

		// DEBUG
		// boolean debug = false;
		// if( recS.toString().equals( "create new screennames " ) ) {
		// debug = true;
		// }

		if( DEBUG.JoinMinON ) {
			getQGramTime += System.nanoTime() - qgramStartTime;
		}

		int[] range = recS.getTransLengths();
		int searchmax = Integer.min( availableQGrams.size(), idx.size() );

		if( DEBUG.PrintJoinMinJoinON ) {
			joinStartTime = System.nanoTime();
		}

		for( int i = 0; i < searchmax; ++i ) {
			Map<QGram, List<Record>> curridx = idx.get( i );
			if( curridx == null ) {
				continue;
			}

			Set<Record> candidates = new WYK_HashSet<Record>();

			for( QGram qgram : availableQGrams.get( i ) ) {
				if( DEBUG.PrintJoinMinJoinON ) {
					qgramCount++;
				}

				// if( debug ) {
				// System.out.println( "D " + qgram );
				// }

				List<Record> tree = curridx.get( qgram );

				if( tree == null ) {
					continue;
				}

				for( Record e : tree ) {
					if( oneSideJoin ) {
						int eTokenCount = e.getTokenCount();
						if( StaticFunctions.overlap( eTokenCount, eTokenCount, range[ 0 ], range[ 1 ] ) ) {
							candidates.add( e );
							comparisonCount++;
						}
						else {
							lengthFiltered++;
						}
					}
					else {
						int[] eTransLength = e.getTransLengths();
						if( StaticFunctions.overlap( eTransLength[ 0 ], eTransLength[ 1 ], range[ 0 ], range[ 1 ] ) ) {
							candidates.add( e );
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

		List<List<QGram>> availableQGrams = recS.getQGrams( qSize );

		if( DEBUG.JoinMinON ) {
			getQGramTime += System.nanoTime() - qgramStartTime;
		}

		int[] range = recS.getTransLengths();
		int searchmax = Integer.min( availableQGrams.size(), idx.size() );
		ArrayList<String> debugArray = new ArrayList<String>();

		if( DEBUG.PrintJoinMinJoinON ) {
			joinStartTime = System.nanoTime();
		}

		JoinMinCandidateSet allCandidateSet = new JoinMinCandidateSet( nIndex, recS, estimatedCountMap.getInt( recS ) );

		for( int i = 0; i < searchmax; ++i ) {
			Map<QGram, List<Record>> curridx = idx.get( i );
			if( curridx == null ) {
				continue;
			}

			WYK_HashSet<Record> candidates = new WYK_HashSet<Record>();

			for( QGram qgram : availableQGrams.get( i ) ) {
				if( DEBUG.PrintJoinMinJoinON ) {
					debugArray.add( "q :" + qgram + " " + i + "\n" );

					qgramCount++;
				}

				List<Record> tree = curridx.get( qgram );

				if( tree == null ) {
					continue;
				}

				for( Record e : tree ) {
					if( oneSideJoin ) {
						if( StaticFunctions.overlap( e.getTokenCount(), e.getTokenCount(), range[ 0 ], range[ 1 ] ) ) {
							if( DEBUG.PrintJoinMinJoinON ) {
								debugArray.add( "Cand: " + e + " by " + qgram + " at " + i + "\n" );
							}
							candidates.add( e );
							comparisonCount++;
						}
						else {
							lengthFiltered++;
						}
					}
					else {
						if( StaticFunctions.overlap( e.getMinTransLength(), e.getMaxTransLength(), range[ 0 ], range[ 1 ] ) ) {
							candidates.add( e );
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

		ArrayList<Record> candSet = allCandidateSet.getCandSet( indexedCountMap, debugArray );

		equivComparisons += candSet.size();
		for( Record recR : candSet ) {
			if( DEBUG.PrintJoinMinJoinON ) {
				debugArray.add( "Test " + recR + "\n" );
			}

			long st = System.nanoTime();
			int compare = checker.isEqual( recS, recR );
			long duration = System.nanoTime() - st;

			comparisonTime += duration;
			if( compare >= 0 ) {
				rslt.add( new IntegerPair( recS.getID(), recR.getID() ) );
				appliedRulesSum += compare;

				if( DEBUG.PrintJoinMinJoinON ) {
					debugArray.add( "Val " + recS + " : " + recR + "\n" );
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

	public void joinRecordMaxKThres( int nIndex, Record recS, List<IntegerPair> rslt, boolean writeResult, BufferedWriter bw,
			Validator checker, int threshold, boolean oneSideJoin ) {
		long qgramStartTime = 0;
		long joinStartTime = 0;
		long qgramCount = 0;

		boolean isUpperRecord = recS.getEstNumTransformed() > threshold;

		Histogram hist = null;
		if( DEBUG.JoinMinON ) {
			qgramStartTime = System.nanoTime();
			hist = new Histogram( "Validation" );
		}

		List<List<QGram>> availableQGrams = recS.getQGrams( qSize );

		if( DEBUG.JoinMinON ) {
			getQGramTime += System.nanoTime() - qgramStartTime;
		}

		int[] range = recS.getTransLengths();
		int searchmax = Integer.min( availableQGrams.size(), idx.size() );

		if( DEBUG.PrintJoinMinJoinON ) {
			joinStartTime = System.nanoTime();
		}

		JoinMinCandidateSet allCandidateSet = new JoinMinCandidateSet( nIndex, recS, estimatedCountMap.getInt( recS ) );

		for( int i = 0; i < searchmax; ++i ) {
			Map<QGram, List<Record>> curridx = idx.get( i );
			if( curridx == null ) {
				continue;
			}

			WYK_HashSet<Record> candidates = new WYK_HashSet<Record>();

			for( QGram qgram : availableQGrams.get( i ) ) {
				if( DEBUG.PrintJoinMinJoinON ) {
					qgramCount++;
				}

				List<Record> tree = curridx.get( qgram );

				if( tree == null ) {
					continue;
				}

				for( Record e : tree ) {
					if( !isUpperRecord && e.getEstNumTransformed() <= threshold ) {
						// this record will not compared by joinmin index.
						// this will be compared by joinnaive index
						continue;
					}
					if( oneSideJoin ) {
						if( StaticFunctions.overlap( e.getTokenCount(), e.getTokenCount(), range[ 0 ], range[ 1 ] ) ) {

							candidates.add( e );
							comparisonCount++;
						}
						else {
							lengthFiltered++;
						}
					}
					else {
						if( StaticFunctions.overlap( e.getMinTransLength(), e.getMaxTransLength(), range[ 0 ], range[ 1 ] ) ) {
							candidates.add( e );
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

		ArrayList<Record> candSet = allCandidateSet.getCandSet( indexedCountMap, null );

		equivComparisons += candSet.size();
		if( DEBUG.JoinMinON ) {
			hist.add( candSet.size() );
		}
		for( Record recR : candSet ) {

			long st = System.nanoTime();
			int compare = checker.isEqual( recS, recR );
			long duration = System.nanoTime() - st;

			comparisonTime += duration;
			if( compare >= 0 ) {
				rslt.add( new IntegerPair( recS.getID(), recR.getID() ) );
				appliedRulesSum += compare;
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
		if( DEBUG.JoinMinON ) {
			hist.print();
			hist.printLogHistogram();
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

			for( int i = 0; i < idx.size(); i++ ) {
				bw.write( i + "-th index\n" );
				WYK_HashMap<QGram, List<Record>> map = idx.get( i );

				for( Entry<QGram, List<Record>> entry : map.entrySet() ) {
					bw.write( entry.getKey().toString() );

					List<Record> list = entry.getValue();

					for( int idx = 0; idx < list.size(); idx++ ) {
						bw.write( " " + list.get( idx ).getID() );
					}
					bw.write( "\n" );
				}
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

		public ArrayList<Record> getCandSet( Object2IntOpenHashMap<Record> indexedCountMap, ArrayList<String> debugArray ) {
			ArrayList<Record> list = new ArrayList<Record>( appearingMap.size() );
			Iterator<Entry<Record, Integer>> iter = appearingMap.entrySet().iterator();

			while( iter.hasNext() ) {
				Entry<Record, Integer> entry = iter.next();

				Record r = entry.getKey();

				if( DEBUG.PrintJoinMinJoinON ) {
					debugArray.add( r + " " + indexedCountMap.getInt( r ) + " " + entry.getValue() + "\n" );
				}

				if( indexedCountMap.getInt( r ) == entry.getValue() ) {
					list.add( r );
				}
			}

			return list;
		}
	}
}
