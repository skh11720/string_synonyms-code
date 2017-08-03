package snu.kdd.synonym.synonymRev.index;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.RecordInt;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
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

public class JoinMinPositionIndex {
	ArrayList<WYK_HashMap<QGram, List<RecordInt>>> idx;
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

	public JoinMinPositionIndex( int nIndex, int qSize, StatContainer stat, Query query, boolean writeResult ) {
		this.idx = new ArrayList<WYK_HashMap<QGram, List<RecordInt>>>();
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

					// if( DEBUG.JoinMinIndexON ) {
					// try {
					// bw.write( "qg " + qgram + " " + i + "\n" );
					// }
					// catch( IOException e ) {
					// e.printStackTrace();
					// }
					// }
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
		}
		else {
			stepTime.stop();
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
				if( curridx_invokes.size() == 0 ) {
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

				int invoke = 0;

				for( QGram twogram : availableQGrams.get( i ) ) {
					int count = curridx_invokes.getInt( twogram );
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
			int[] indexedPosition = new int[ mpq.size() ];

			while( !mpq.isEmpty() ) {
				MinPosition minPos = mpq.poll();
				indexedPosition[ indexedCount ] = minPos.positionIndex;
				indexedCount++;
				if( DEBUG.PrintJoinMinIndexON ) {
					try {
						bw_index.write( minPos.positionIndex + " " + minPos.candidateCount + "\n" );
					}
					catch( IOException e ) {
						e.printStackTrace();
					}
				}
			}

			Arrays.sort( indexedPosition );

			for( int i = 0; i < indexedCount; i++ ) {
				int minIdx = indexedPosition[ i ];

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
					// note that i + 1 is added to make the positions start from 1
					this.put( minIdx, qgram, rec, i + 1 );
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
			Util.printGCStats( stat, "Stat_Index" );
		}

		for( Object2IntOpenHashMap<QGram> in : invokes ) {
			in.clear();
		}
	}

	public void setIndex( int position ) {
		while( idx.size() <= position ) {
			idx.add( new WYK_HashMap<QGram, List<RecordInt>>() );
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

	public void put( int position, QGram qgram, Record rec, int count ) {
		Map<QGram, List<RecordInt>> map = idx.get( position );

		List<RecordInt> list = map.get( qgram );
		if( list == null ) {
			list = new ArrayList<RecordInt>();
			map.put( qgram, list );
		}

		list.add( new RecordInt( rec, count ) );
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

		WYK_HashMap<Record, Integer> foundMap = new WYK_HashMap<Record, Integer>();
		for( int i = 0; i < searchmax; ++i ) {
			Map<QGram, List<RecordInt>> curridx = idx.get( i );
			if( curridx == null ) {
				continue;
			}

			for( QGram qgram : availableQGrams.get( i ) ) {
				if( DEBUG.PrintJoinMinJoinON ) {
					debugArray.add( "q :" + qgram + " " + i + "\n" );

					qgramCount++;
				}

				List<RecordInt> tree = curridx.get( qgram );

				if( tree == null ) {
					continue;
				}

				for( RecordInt e : tree ) {
					if( oneSideJoin ) {
						if( StaticFunctions.overlap( e.record.getTokenCount(), e.record.getTokenCount(), range[ 0 ],
								range[ 1 ] ) ) {
							if( DEBUG.PrintJoinMinJoinON ) {
								debugArray.add( "Cand: " + e.record + " by " + qgram + " at " + i + "\n" );
							}

							// if e.index is not 1, check whether previous results are in prevCandidate
							if( e.index != 1 ) {
								if( foundMap == null ) {
									continue;
								}
								Integer indexed = foundMap.get( e.record );

								if( indexed != null ) {
									if( indexed == e.index - 1 ) {
										// previous position is found -> e.record is a proper candidate
										foundMap.put( e.record, e.index );
									}
									else {
										// otherwise, e.record is
										// already added by another qgram ( indexed == e.index )
										// or previous position is not found ( indexed < e.index -1 )
										foundMap.remove( e.record );
									}
								}
							}
							else {
								// this is the first position
								if( !foundMap.containsKey( e.record ) ) {
									foundMap.put( e.record, 1 );
								}
							}
							comparisonCount++;
						}
						else {
							lengthFiltered++;
						}
					}
					else {
						if( StaticFunctions.overlap( e.record.getMinTransLength(), e.record.getMaxTransLength(), range[ 0 ],
								range[ 1 ] ) ) {
							if( DEBUG.PrintJoinMinJoinON ) {
								debugArray.add( "Cand: " + e.record + " by " + qgram + " at " + i + "\n" );
							}

							// if e.index is not 1, check whether previous results are in prevCandidate
							if( e.index != 1 ) {
								if( foundMap == null ) {
									continue;
								}
								Integer indexed = foundMap.get( e.record );

								if( indexed != null ) {
									if( indexed == e.index - 1 ) {
										// previous position is found -> e.record is a proper candidate
										foundMap.put( e.record, e.index );
									}
									else {
										// otherwise, e.record is
										// already added by another qgram ( indexed == e.index )
										// or previous position is not found ( indexed < e.index -1 )
										foundMap.remove( e.record );
									}
								}
							}
							else {
								// this is the first position
								if( !foundMap.containsKey( e.record ) ) {
									foundMap.put( e.record, 1 );
								}
							}
							comparisonCount++;
						}
						else {
							lengthFiltered++;
						}
					}
				}
			}
		}

		for( Entry<Record, Integer> entry : foundMap.entrySet() ) {

			Record recR = entry.getKey();
			if( indexedCountMap.getInt( recR ) != entry.getValue() ) {
				continue;
			}

			if( DEBUG.PrintJoinMinJoinON ) {
				debugArray.add( "Test " + recR + "\n" );
			}

			equivComparisons++;
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
