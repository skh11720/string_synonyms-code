package snu.kdd.synonym.synonymRev.index;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.MinPositionQueue;
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

	public JoinMinIndex( int nIndex, int qSize, StatContainer stat, Query query, boolean writeResult ) {
		this.idx = new ArrayList<WYK_HashMap<QGram, List<Record>>>();
		this.qSize = qSize;

		if( DEBUG.JoinMinIndexON ) {
			this.countPerPosition = new ArrayList<Integer>();
		}

		long starttime = System.nanoTime();

		// Build an index
		// Count Invokes per each (token, loc) pair
		List<Object2IntOpenHashMap<QGram>> invokes = new ArrayList<Object2IntOpenHashMap<QGram>>();
		long getQGramTime = 0;
		long countIndexingTime = 0;

		BufferedWriter bw = null;

		if( DEBUG.JoinMinIndexON ) {
			try {
				bw = new BufferedWriter( new FileWriter( "JoinMin_Index_Count_Debug.txt" ) );
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

			long qgramCount = 0;
			for( int i = 0; i < searchmax; ++i ) {
				Object2IntOpenHashMap<QGram> curridx_invokes = invokes.get( i );

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
					bw.write( recordMidTime - recordStartTime + " " );
					bw.write( qgramCount + " " );
					bw.write( "\n" );
				}
				catch( IOException e ) {
					e.printStackTrace();
				}
			}
		}

		if( DEBUG.JoinMinIndexON ) {
			try {
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

		if( DEBUG.JoinMinON ) {
			if( writeResult ) {
				stepTime.stopAndAdd( stat );
			}
			else {
				stepTime.stop();
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

		stepTime.resetAndStart( "Result_3_1_2_Indexing_Time" );

		this.predictCount = 0;
		long indexedElements = 0;

		// find best K positions for each string in T
		indexedCountMap = new Object2IntOpenHashMap<>();
		estimatedCountMap = new Object2IntOpenHashMap<>();
		for( Record rec : query.indexedSet.get() ) {
			int[] range = rec.getTransLengths();

			int searchmax = Math.min( range[ 0 ], invokes.size() );

			List<List<QGram>> availableQGrams = null;

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
					continue;
				}

				Object2IntOpenHashMap<QGram> curridx_invokes = invokes.get( i );
				if( curridx_invokes.size() == 0 ) {
					mpq.add( i, 0 );
					continue;
				}

				int invoke = 0;

				for( QGram twogram : availableQGrams.get( i ) ) {
					int count = curridx_invokes.getInt( twogram );
					if( count != 0 ) {
						// upper bound
						invoke += count;
					}
				}
				mpq.add( i, invoke );
			}

			this.predictCount += mpq.minInvokes;

			int indexedCount = 0;
			while( !mpq.isEmpty() ) {
				indexedCount++;

				int minIdx = mpq.pollIndex();
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
				stepTime.stopAndAdd( stat );
			}
			else {
				stepTime.stop();
			}

			stepTime.resetAndStart( "Result_3_3_Statistic Time" );

			if( writeResult ) {
				stepTime.stopAndAdd( stat );
			}
			else {
				stepTime.stop();
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

		if( DEBUG.JoinMinON ) {
			if( writeResult ) {
				this.addStat( stat );
				stat.add( "Counter_Index_1_HashCollision", WYK_HashSet.collision );
				stat.add( "Counter_Index_1_HashResize", WYK_HashSet.resize );
			}
		}
	}

	public void setIndex( int position ) {
		while( idx.size() <= position ) {
			idx.add( new WYK_HashMap<QGram, List<Record>>() );
		}
	}

	public void addStat( StatContainer stat ) {
		long getCount = 0;
		long getIterCount = 0;
		long putCount = 0;
		long resizeCount = 0;
		long removeCount = 0;
		long removeIterCount = 0;
		long putRemovedCount = 0;
		long removeFoundCount = 0;

		for( int i = 0; i < idx.size(); i++ ) {
			WYK_HashMap<QGram, List<Record>> map = idx.get( i );

			getCount += map.getCount;
			getIterCount += map.getIterCount;
			putCount += map.putCount;
			resizeCount += map.resizeCount;
			removeCount += map.removeCount;
			removeIterCount += map.removeIterCount;
			putRemovedCount += map.putRemovedCount;
			removeFoundCount += map.removeFoundCount;
		}

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

		stat.add( "Counter_Index_0_Get_Count", getCount );
		stat.add( "Counter_Index_0_GetIter_Count", getIterCount );
		stat.add( "Counter_Index_0_Put_Count", putCount );
		stat.add( "Counter_Index_0_Resize_Count", resizeCount );
		stat.add( "Counter_Index_0_Remove_Count", removeCount );
		stat.add( "Counter_Index_0_RemoveIter_Count", removeIterCount );
		stat.add( "Counter_Index_0_PutRemoved_Count", putRemovedCount );
		stat.add( "Counter_Index_0_RemoveFound_Count", removeFoundCount );
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
				long ruleiters = 0;
				long reccalls = 0;
				long entryiters = 0;

				if( DEBUG.JoinMinON ) {
					ruleiters = Validator.niterrules;
					reccalls = Validator.recursivecalls;
					entryiters = Validator.niterentry;
				}

				long st = System.nanoTime();
				int compare = checker.isEqual( recS, recR );

				// if( debug ) {
				// System.out.println( "comp " + recR.toString() + " " + recR.getID() + " " + recS.toString() + " "
				// + recS.getID() + " " + compare );
				// }

				long duration = System.nanoTime() - st;

				if( DEBUG.JoinMinON ) {
					ruleiters = Validator.niterrules - ruleiters;
					reccalls = Validator.recursivecalls - reccalls;
					entryiters = Validator.niterentry - entryiters;
				}

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

		boolean debug = false;
		if( recS.getID() == 9368 ) {
			debug = true;
		}

		if( DEBUG.JoinMinON ) {
			qgramStartTime = System.nanoTime();
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

				if( debug ) {
					System.out.println( "qgram " + qgram );
					System.out.println( tree );
				}
				
				if( tree == null ) {
					continue;
				}

				for( Record e : tree ) {
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

		ArrayList<Record> candSet = allCandidateSet.getCandSet( indexedCountMap );

		equivComparisons += candSet.size();
		for( Record recR : candSet ) {
			long ruleiters = 0;
			long reccalls = 0;
			long entryiters = 0;

			if( DEBUG.JoinMinON ) {
				ruleiters = Validator.niterrules;
				reccalls = Validator.recursivecalls;
				entryiters = Validator.niterentry;
			}

			long st = System.nanoTime();
			int compare = checker.isEqual( recS, recR );
			long duration = System.nanoTime() - st;

			if( DEBUG.JoinMinON ) {
				ruleiters = Validator.niterrules - ruleiters;
				reccalls = Validator.recursivecalls - reccalls;
				entryiters = Validator.niterentry - entryiters;
			}

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
