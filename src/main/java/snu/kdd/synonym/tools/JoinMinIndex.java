package snu.kdd.synonym.tools;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import mine.Record;
import tools.DEBUG;
import tools.IntegerPair;
import tools.QGram;
import tools.StaticFunctions;
import tools.WYK_HashMap;
import tools.WYK_HashSet;
import validator.Validator;
import wrapped.WrappedInteger;

public class JoinMinIndex {
	ArrayList<WYK_HashMap<QGram, List<Record>>> idx;
	ArrayList<Integer> countPerPosition = null;
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

	private static final WrappedInteger ONE = new WrappedInteger( 1 );
	public long predictCount;

	public JoinMinIndex( int qSize ) {
		idx = new ArrayList<WYK_HashMap<QGram, List<Record>>>();
		this.qSize = qSize;

		if( DEBUG.JoinMinIndexCountON ) {
			countPerPosition = new ArrayList<Integer>();
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

		if( DEBUG.JoinMinIndexCountON ) {
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

	public List<IntegerPair> join( List<Record> tableSearched, boolean writeResult, StatContainer stat, Validator checker,
			boolean oneSideJoin ) {
		BufferedWriter bw = null;

		if( DEBUG.JoinMinJoinON ) {
			try {
				bw = new BufferedWriter( new FileWriter( "JoinMin_Join_Debug.txt" ) );
			}
			catch( Exception e ) {
				e.printStackTrace();
			}
		}

		List<IntegerPair> rslt = new ArrayList<IntegerPair>();

		long joinStartTime = System.nanoTime();
		for( Record recS : tableSearched ) {
			joinRecord( recS, rslt, writeResult, bw, checker, oneSideJoin );
		}
		joinTime = System.nanoTime() - joinStartTime;

		if( DEBUG.JoinMinJoinON ) {
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
				stat.add( "Stat_getQGramCount", Record.getQGramCount );
				stat.add( "Result_3_2_1_Equiv_Checking_Time", comparisonTime / 1000000 );
			}
		}

		if( writeResult ) {
			stat.add( "Stat_Equiv_Comparison", equivComparisons );
			stat.add( "Join_Min_Result", rslt.size() );
		}

		if( DEBUG.JoinMinJoinON ) {
			try {
				bw.close();
			}
			catch( Exception e ) {
				e.printStackTrace();
			}
		}

		return rslt;
	}

	public List<IntegerPair> joinTwo( List<Record> tableSearched, boolean writeResult, StatContainer stat, Validator checker ) {
		BufferedWriter bw = null;

		if( DEBUG.JoinMinJoinON ) {
			try {
				bw = new BufferedWriter( new FileWriter( "JoinMin_Join_Debug.txt" ) );
			}
			catch( Exception e ) {
				e.printStackTrace();
			}
		}

		List<IntegerPair> rslt = new ArrayList<IntegerPair>();

		long appliedRules_sum = 0;

		long joinStartTime = System.nanoTime();
		for( Record recS : tableSearched ) {
			joinRecordTwo( recS, rslt, writeResult, bw, checker );
		}
		joinTime = System.nanoTime() - joinStartTime;

		if( DEBUG.JoinMinJoinON ) {
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
			Util.printLog( "Avg applied rules : " + appliedRules_sum + "/" + rslt.size() );
			Util.printLog( "Est weight : " + comparisonCount );
			Util.printLog( "Join time : " + joinTime );
			Util.printLog( "Epsilon : " + epsilon );

			if( writeResult ) {
				// stat.add( "Last Token Filtered", lastTokenFiltered );
				stat.add( "Est_Join_0_GetQGramTime", getQGramTime );
				stat.add( "Stat_Equiv_Comparison", equivComparisons );
				stat.add( "Stat_Length_Filtered", lengthFiltered );
				stat.add( "Stat_getQGramCount", Record.getQGramCount );
				stat.add( "Result_3_2_1_Equiv_Checking_Time", comparisonTime / 1000000 );
			}
		}

		if( DEBUG.JoinMinJoinON ) {
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

		if( DEBUG.JoinMinON ) {
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

		int[] range = recS.getCandidateLengths( recS.size() - 1 );
		int searchmax = Integer.min( availableQGrams.size(), idx.size() );

		if( DEBUG.JoinMinJoinON ) {
			joinStartTime = System.nanoTime();
		}

		for( int i = 0; i < searchmax; ++i ) {
			Map<QGram, List<Record>> curridx = idx.get( i );
			if( curridx == null ) {
				continue;
			}

			Set<Record> candidates = new WYK_HashSet<Record>();

			for( QGram qgram : availableQGrams.get( i ) ) {
				if( DEBUG.JoinMinJoinON ) {
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
						if( StaticFunctions.overlap( e.getTokenArray().length, e.getTokenArray().length, range[ 0 ],
								range[ 1 ] ) ) {
							// if( debug ) {
							// System.out.println( "C " + e.toString() + "(" + e.getID() + ")" );
							// }
							candidates.add( e );
							comparisonCount++;
						}
						else {
							lengthFiltered++;
						}
					}
					else {
						if( StaticFunctions.overlap( e.getMinLength(), e.getMaxLength(), range[ 0 ], range[ 1 ] ) ) {
							// if( debug ) {
							// System.out.println( "C " + e.toString() + "(" + e.getID() + ")" );
							// }
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

					// bw.write( duration + "\t" + compare + "\t" + recR.size() + "\t" + recR.getRuleCount() + "\t"
					// + recR.getFirstRuleCount() + "\t" + recS.size() + "\t" + recS.getRuleCount() + "\t"
					// + recS.getFirstRuleCount() + "\t" + ruleiters + "\t" + reccalls + "\t" + entryiters + "\n" );
				}

				comparisonTime += duration;
				if( compare >= 0 ) {
					// if( debug ) {
					// System.out.println( "E " + recR.toString() + "(" + recR.getID() + ")" );
					// }

					rslt.add( new IntegerPair( recS.getID(), recR.getID() ) );
					appliedRulesSum += compare;
				}
			}
		}

		if( DEBUG.JoinMinJoinON ) {
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

	public void joinRecordTwo( Record recS, List<IntegerPair> rslt, boolean writeResult, BufferedWriter bw, Validator checker ) {
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

		int[] range = recS.getCandidateLengths( recS.size() - 1 );
		int searchmax = Integer.min( availableQGrams.size(), idx.size() );

		if( DEBUG.JoinMinJoinON ) {
			joinStartTime = System.nanoTime();
		}

		Set<Record> oneCandidates = new WYK_HashSet<Record>();
		Set<Record> twoCandidates = new WYK_HashSet<Record>();

		for( int i = 0; i < searchmax; ++i ) {
			Map<QGram, List<Record>> curridx = idx.get( i );
			if( curridx == null ) {
				continue;
			}

			Set<Record> candidates = new WYK_HashSet<Record>();

			for( QGram qgram : availableQGrams.get( i ) ) {
				if( DEBUG.JoinMinJoinON ) {
					qgramCount++;
				}

				List<Record> tree = curridx.get( qgram );

				if( tree == null ) {
					continue;
				}

				for( Record e : tree ) {
					if( StaticFunctions.overlap( e.getMinLength(), e.getMaxLength(), range[ 0 ], range[ 1 ] ) ) {
						candidates.add( e );
						comparisonCount++;
					}
					else {
						lengthFiltered++;
					}
				}
			}

			for( Record e : candidates ) {
				if( oneCandidates.contains( e ) ) {
					twoCandidates.add( e );
				}
				else if( bypassSet.contains( e.getID() ) ) {
					twoCandidates.add( e );
				}
				else {
					oneCandidates.add( e );
				}
			}
		}

		equivComparisons += twoCandidates.size();
		for( Record recR : twoCandidates ) {
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

				// bw.write( duration + "\t" + compare + "\t" + recR.size() + "\t" + recR.getRuleCount() + "\t"
				// + recR.getFirstRuleCount() + "\t" + recS.size() + "\t" + recS.getRuleCount() + "\t"
				// + recS.getFirstRuleCount() + "\t" + ruleiters + "\t" + reccalls + "\t" + entryiters + "\n" );
			}

			comparisonTime += duration;
			if( compare >= 0 ) {
				rslt.add( new IntegerPair( recS.getID(), recR.getID() ) );
				appliedRulesSum += compare;
			}
		}

		if( DEBUG.JoinMinJoinON ) {
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

	public void joinRecordThres( Record recS, List<IntegerPair> rslt, boolean writeResult, BufferedWriter bw, Validator checker,
			int threshold, boolean oneSideJoin ) {
		long qgramStartTime = 0;
		long joinStartTime = 0;
		long qgramCount = 0;

		// System.out.println( "recS " + recS );

		boolean isUpperRecord = recS.getEstNumRecords() > threshold;

		if( DEBUG.JoinMinON ) {
			qgramStartTime = System.nanoTime();
		}

		List<List<QGram>> availableQGrams = recS.getQGrams( qSize, idx.size() );

		// DEBUG
		// boolean debug = false;
		// if( recS.toString().equals( "create new screennames " ) ) {
		// debug = true;
		// }

		if( DEBUG.JoinMinON ) {
			joinStartTime = System.nanoTime();
			getQGramTime += joinStartTime - qgramStartTime;
		}

		int[] range = recS.getCandidateLengths( recS.size() - 1 );
		int searchmax = availableQGrams.size();

		for( int i = 0; i < searchmax; ++i ) {
			Map<QGram, List<Record>> curridx = idx.get( i );

			Set<Record> candidates = new HashSet<Record>();

			for( QGram qgram : availableQGrams.get( i ) ) {
				if( DEBUG.JoinMinJoinON ) {
					qgramCount++;
				}

				// if( debug ) {
				// System.out.println( "D " + qgram );
				// }

				List<Record> tree = curridx.get( qgram );

				if( tree == null ) {
					continue;
				}

				for( int idx = tree.size() - 1; idx >= 0; idx-- ) {
					Record e = tree.get( idx );
					if( !oneSideJoin && !isUpperRecord && e.getEstNumRecords() <= threshold ) {
						break;
					}

					if( oneSideJoin ) {
						if( StaticFunctions.overlap( e.getTokenArray().length, e.getTokenArray().length, range[ 0 ],
								range[ 1 ] ) ) {
							// if( debug ) {
							// System.out.println( "C " + e.toString() + "(" + e.getID() + ")" );
							// }
							candidates.add( e );
							comparisonCount++;
						}
						else {
							lengthFiltered++;
						}
					}
					else {
						if( StaticFunctions.overlap( e.getMinLength(), e.getMaxLength(), range[ 0 ], range[ 1 ] ) ) {
							// if( debug ) {
							// System.out.println( "C " + e.toString() + "(" + e.getID() + ")" );
							// }
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
				long st = 0;

				if( DEBUG.JoinMinON ) {
					ruleiters = Validator.niterrules;
					reccalls = Validator.recursivecalls;
					entryiters = Validator.niterentry;
					st = System.nanoTime();
				}

				int compare = checker.isEqual( recS, recR );

				if( DEBUG.JoinMinON ) {
					ruleiters = Validator.niterrules - ruleiters;
					reccalls = Validator.recursivecalls - reccalls;
					entryiters = Validator.niterentry - entryiters;

					// bw.write( duration + "\t" + compare + "\t" + recR.size() + "\t" + recR.getRuleCount() + "\t"
					// + recR.getFirstRuleCount() + "\t" + recS.size() + "\t" + recS.getRuleCount() + "\t"
					// + recS.getFirstRuleCount() + "\t" + ruleiters + "\t" + reccalls + "\t" + entryiters + "\n" );
					long duration = System.nanoTime() - st;
					comparisonTime += duration;
				}

				if( compare >= 0 ) {
					rslt.add( new IntegerPair( recS.getID(), recR.getID() ) );
					appliedRulesSum += compare;

					// DEBUG
					// if( recS.getID() != recR.getID() ) {
					// System.out.println( "JoinMin rslt " + recS + " " + recS.getID() + ", " + recR + " " + recR.getID() );
					// }
				}
			}
		}

		if( DEBUG.JoinMinJoinON ) {
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

	public static JoinMinIndex buildIndex( List<Record> tableSearched, List<Record> tableIndexed, int maxIndex, int qSize,
			StatContainer stat, boolean writeResult, boolean oneSideJoin ) {
		long starttime = System.nanoTime();

		// Build an index
		// Count Invokes per each (token, loc) pair
		List<Map<QGram, WrappedInteger>> invokes = new ArrayList<Map<QGram, WrappedInteger>>();
		long getQGramTime = 0;
		long countIndexingTime = 0;

		JoinMinIndex idx = new JoinMinIndex( qSize );

		try {
			BufferedWriter bw = null;

			if( DEBUG.JoinMinIndexON ) {
				bw = new BufferedWriter( new FileWriter( "JoinMin_Index_Debug.txt" ) );
			}

			StopWatch stepTime = StopWatch.getWatchStarted( "Result_3_1_1_Index_Count_Time" );
			for( Record rec : tableSearched ) {
				long recordStartTime = 0;
				long recordMidTime = 0;

				if( DEBUG.JoinMinON ) {
					recordStartTime = System.nanoTime();
				}

				List<List<QGram>> availableQGrams = rec.getQGrams( qSize );

				if( DEBUG.JoinMinON ) {
					recordMidTime = System.nanoTime();
					getQGramTime += recordMidTime - recordStartTime;
				}

				int searchmax = Math.min( availableQGrams.size(), maxIndex );

				for( int i = invokes.size(); i < searchmax; i++ ) {
					invokes.add( new WYK_HashMap<QGram, WrappedInteger>() );

					if( DEBUG.JoinMinIndexCountON ) {
						idx.countPerPosition.add( 0 );
					}
				}

				long qgramCount = 0;
				for( int i = 0; i < searchmax; ++i ) {
					Map<QGram, WrappedInteger> curridx_invokes = invokes.get( i );

					List<QGram> available = availableQGrams.get( i );
					qgramCount += available.size();
					for( QGram qgram : available ) {
						WrappedInteger count = curridx_invokes.get( qgram );
						if( count == null ) {
							// object ONE is shared to reduce memory usage
							curridx_invokes.put( qgram, ONE );
						}
						else if( count == ONE ) {
							count = new WrappedInteger( 2 );
							curridx_invokes.put( qgram, count );
						}
						else {
							count.increment();
						}
					}

					if( DEBUG.JoinMinIndexCountON ) {
						int newSize = idx.countPerPosition.get( i ) + available.size();

						idx.countPerPosition.set( i, newSize );
					}
				}
				idx.searchedTotalSigCount += qgramCount;

				if( DEBUG.JoinMinON ) {
					countIndexingTime += System.nanoTime() - recordMidTime;
				}

				if( DEBUG.JoinMinIndexON ) {
					bw.write( recordMidTime - recordStartTime + " " );
					bw.write( qgramCount + " " );
					bw.write( "\n" );
				}
			}

			if( DEBUG.JoinMinIndexON ) {
				bw.close();
			}

			idx.countTime = System.nanoTime() - starttime;
			idx.gamma = idx.countTime / idx.searchedTotalSigCount;

			if( DEBUG.JoinMinON ) {
				Util.printLog( "Step 1 Time : " + idx.countTime );
				Util.printLog( "Gamma (buildTime / signature): " + idx.gamma );

				if( writeResult ) {
					stat.add( "Est_Index_0_GetQGramTime", getQGramTime );
					stat.add( "Est_Index_0_CountIndexingTime", countIndexingTime );

					stat.add( "Est_Index_1_Index_Count_Time", idx.countTime );
					stat.add( "Est_Index_1_Time_Per_Sig", Double.toString( idx.gamma ) );
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
				bw_index = new BufferedWriter( new FileWriter( "JoinMin_Index_Content.txt" ) );
			}

			stepTime.resetAndStart( "Result_3_1_2_Indexing_Time" );

			idx.predictCount = 0;
			long indexedElements = 0;

			for( Record rec : tableIndexed ) {
				int[] range = rec.getCandidateLengths( rec.size() - 1 );

				int searchmax = Math.min( range[ 0 ], invokes.size() );

				List<List<QGram>> availableQGrams = null;

				if( oneSideJoin ) {
					availableQGrams = rec.getSelfQGrams( qSize, searchmax );
					// System.out.println( availableQGrams.toString() );
				}
				else {
					availableQGrams = rec.getQGrams( qSize, searchmax );
				}

				for( List<QGram> set : availableQGrams ) {
					idx.indexedTotalSigCount += set.size();
				}

				int minIdx = -1;
				int minInvokes = Integer.MAX_VALUE;

				for( int i = 0; i < searchmax; ++i ) {
					if( availableQGrams.get( i ).isEmpty() ) {
						continue;
					}

					// There is no invocation count: this is the minimum point
					if( i >= invokes.size() ) {
						minIdx = i;
						minInvokes = 0;
						break;
					}

					Map<QGram, WrappedInteger> curridx_invokes = invokes.get( i );
					if( curridx_invokes.size() == 0 ) {
						minIdx = i;
						minInvokes = 0;
						break;
					}
					int invoke = 0;

					for( QGram twogram : availableQGrams.get( i ) ) {
						WrappedInteger count = curridx_invokes.get( twogram );
						if( count != null ) {
							// upper bound
							invoke += count.get();
						}
					}
					if( invoke < minInvokes ) {
						minIdx = i;
						minInvokes = invoke;
					}
				}

				idx.predictCount += minInvokes;

				idx.setIndex( minIdx );

				for( QGram qgram : availableQGrams.get( minIdx ) ) {
					// write2File(bw, minIdx, twogram, rec.getID());

					if( DEBUG.PrintJoinMinIndexON ) {
						bw_index.write( minIdx + ", " + qgram + " : " + rec + "\n" );
					}

					idx.put( minIdx, qgram, rec );
				}

				if( DEBUG.JoinMinON ) {
					indexedElements += availableQGrams.get( minIdx ).size();
				}
			}

			idx.indexTime = System.nanoTime() - starttime;
			idx.delta = idx.indexTime / idx.indexedTotalSigCount;

			if( DEBUG.JoinMinON ) {
				Util.printLog( "Idx size : " + indexedElements );
				Util.printLog( "Predict : " + idx.predictCount );
				Util.printLog( "Step 2 Time : " + idx.indexTime );
				Util.printLog( "Delta (index build / signature ): " + idx.delta );

				if( writeResult ) {
					stat.add( "Stat_JoinMin_Index_Size", indexedElements );
					stat.add( "Stat_Predicted_Comparison", idx.predictCount );

					stat.add( "Est_Index_2_Build_Index_Time", idx.indexTime );
					stat.add( "Est_Index_2_Time_Per_Sig", Double.toString( idx.delta ) );
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
				bw_index.close();
			}
		}
		catch( Exception e ) {
			e.printStackTrace();
		}

		if( DEBUG.JoinMinON ) {
			if( writeResult ) {
				idx.addStat( stat );
				stat.add( "Counter_Index_1_HashCollision", WYK_HashSet.collision );
				stat.add( "Counter_Index_1_HashResize", WYK_HashSet.resize );
			}
		}

		return idx;
	}

	public static JoinMinIndex buildIndexTwo( List<Record> tableSearched, List<Record> tableIndexed, int maxIndex, int qSize,
			StatContainer stat, boolean writeResult ) {
		long starttime = System.nanoTime();

		// Build an index
		// Count Invokes per each (token, loc) pair
		List<Map<QGram, WrappedInteger>> invokes = new ArrayList<Map<QGram, WrappedInteger>>();
		long getQGramTime = 0;
		long countIndexingTime = 0;

		JoinMinIndex idx = new JoinMinIndex( qSize );

		try {
			BufferedWriter bw = null;

			if( DEBUG.JoinMinIndexON ) {
				bw = new BufferedWriter( new FileWriter( "JoinMin_Index_Debug.txt" ) );
			}

			StopWatch stepTime = StopWatch.getWatchStarted( "Result_3_1_1_Index_Count_Time" );
			for( Record rec : tableSearched ) {
				long recordStartTime = 0;
				long recordMidTime = 0;

				if( DEBUG.JoinMinON ) {
					recordStartTime = System.nanoTime();
				}

				List<List<QGram>> availableQGrams = rec.getQGrams( qSize );

				if( DEBUG.JoinMinON ) {
					recordMidTime = System.nanoTime();
					getQGramTime += recordMidTime - recordStartTime;
				}

				int searchmax = Math.min( availableQGrams.size(), maxIndex );

				for( int i = invokes.size(); i < searchmax; i++ ) {
					invokes.add( new WYK_HashMap<QGram, WrappedInteger>() );

					if( DEBUG.JoinMinIndexCountON ) {
						idx.countPerPosition.add( 0 );
					}
				}

				long qgramCount = 0;
				for( int i = 0; i < searchmax; ++i ) {
					Map<QGram, WrappedInteger> curridx_invokes = invokes.get( i );

					List<QGram> available = availableQGrams.get( i );
					qgramCount += available.size();
					for( QGram qgram : available ) {
						WrappedInteger count = curridx_invokes.get( qgram );
						if( count == null ) {
							// object ONE is shared to reduce memory usage
							curridx_invokes.put( qgram, ONE );
						}
						else if( count == ONE ) {
							count = new WrappedInteger( 2 );
							curridx_invokes.put( qgram, count );
						}
						else {
							count.increment();
						}
					}

					if( DEBUG.JoinMinIndexCountON ) {
						int newSize = idx.countPerPosition.get( i ) + available.size();

						idx.countPerPosition.set( i, newSize );
					}
				}
				idx.searchedTotalSigCount += qgramCount;

				if( DEBUG.JoinMinON ) {
					countIndexingTime += System.nanoTime() - recordMidTime;
				}

				if( DEBUG.JoinMinIndexON ) {
					bw.write( recordMidTime - recordStartTime + " " );
					bw.write( qgramCount + " " );
					bw.write( "\n" );
				}
			}

			if( DEBUG.JoinMinIndexON ) {
				bw.close();
			}

			idx.countTime = System.nanoTime() - starttime;
			idx.gamma = idx.countTime / idx.searchedTotalSigCount;

			if( DEBUG.JoinMinON ) {
				Util.printLog( "Step 1 Time : " + idx.countTime );
				Util.printLog( "Gamma (buildTime / signature): " + idx.gamma );

				if( writeResult ) {
					stat.add( "Est_Index_0_GetQGramTime", getQGramTime );
					stat.add( "Est_Index_0_CountIndexingTime", countIndexingTime );

					stat.add( "Est_Index_1_Index_Count_Time", idx.countTime );
					stat.add( "Est_Index_1_Time_Per_Sig", Double.toString( idx.gamma ) );
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

			stepTime.resetAndStart( "Result_3_1_2_Indexing_Time" );

			idx.predictCount = 0;
			long indexedElements = 0;

			if( DEBUG.PrintJoinMinIndexON ) {
				bw = new BufferedWriter( new FileWriter( "debug_indextwo.txt" ) );
			}

			idx.bypassSet = new WYK_HashSet<Integer>();

			for( Record rec : tableIndexed ) {
				int[] range = rec.getCandidateLengths( rec.size() - 1 );

				int searchmax = Math.min( range[ 0 ], invokes.size() );

				List<List<QGram>> availableQGrams = rec.getQGrams( qSize, searchmax );
				for( List<QGram> set : availableQGrams ) {
					idx.indexedTotalSigCount += set.size();
				}

				int minIdx = -1;
				int minTwoIdx = -1;
				int minInvokes = Integer.MAX_VALUE;
				int minTwoInvokes = Integer.MAX_VALUE;

				for( int i = 0; i < searchmax; ++i ) {
					if( availableQGrams.get( i ).isEmpty() ) {
						continue;
					}

					// There is no invocation count: this is the minimum point
					if( i >= invokes.size() ) {
						if( minInvokes != 0 ) {
							minIdx = i;
							minInvokes = 0;
						}
						else {
							minTwoIdx = i;
							minTwoInvokes = 0;
							break;
						}
					}

					Map<QGram, WrappedInteger> curridx_invokes = invokes.get( i );
					if( curridx_invokes.size() == 0 ) {
						if( minInvokes != 0 ) {
							minIdx = i;
							minInvokes = 0;
						}
						else {
							minTwoIdx = i;
							minTwoInvokes = 0;
							break;
						}
					}

					int invoke = 0;

					for( QGram twogram : availableQGrams.get( i ) ) {
						WrappedInteger count = curridx_invokes.get( twogram );

						// upper bound
						invoke += count.get();
					}

					if( invoke < minInvokes ) {
						minTwoIdx = minIdx;
						minTwoInvokes = minInvokes;

						minIdx = i;
						minInvokes = invoke;
					}
					else if( invoke < minTwoInvokes ) {
						minTwoIdx = i;
						minTwoInvokes = invoke;
					}
				}

				idx.predictCount += minInvokes;

				idx.setIndex( minIdx );
				idx.setIndex( minTwoIdx );

				for( QGram qgram : availableQGrams.get( minIdx ) ) {
					if( DEBUG.PrintJoinMinIndexON ) {
						bw.write( "1 " + minIdx + ", " + qgram + " : " + rec + "\n" );
					}
					idx.put( minIdx, qgram, rec );
				}

				if( minTwoIdx != -1 ) {
					// System.out.println( "Rec: " + rec + " has no minTwoIdx" );
					for( QGram qgram : availableQGrams.get( minTwoIdx ) ) {
						if( DEBUG.PrintJoinMinIndexON ) {
							bw.write( "2 " + minTwoIdx + ", " + qgram + " : " + rec + "\n" );
						}
						idx.put( minTwoIdx, qgram, rec );
					}
				}
				else {
					idx.bypassSet.add( rec.getID() );
				}

				if( DEBUG.JoinMinON ) {
					indexedElements += availableQGrams.get( minIdx ).size();

					if( minTwoIdx != -1 ) {
						indexedElements += availableQGrams.get( minTwoIdx ).size();
					}
					else {
						indexedElements += availableQGrams.get( minIdx ).size();
					}
				}
			}

			if( DEBUG.PrintJoinMinIndexON ) {
				bw.close();
			}

			idx.indexTime = System.nanoTime() - starttime;
			idx.delta = idx.indexTime / idx.indexedTotalSigCount;

			if( DEBUG.JoinMinON ) {
				Util.printLog( "Idx size : " + indexedElements );
				Util.printLog( "Predict : " + idx.predictCount );
				Util.printLog( "Step 2 Time : " + idx.indexTime );
				Util.printLog( "Delta (index build / signature ): " + idx.delta );

				if( writeResult ) {
					stat.add( "Stat_JoinMin_Index_Size", indexedElements );
					stat.add( "Stat_Predicted_Comparison", idx.predictCount );

					stat.add( "Est_Index_2_Build_Index_Time", idx.indexTime );
					stat.add( "Est_Index_2_Time_Per_Sig", Double.toString( idx.delta ) );
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
		}
		catch( Exception e ) {
			e.printStackTrace();
		}

		if( DEBUG.JoinMinON ) {
			if( writeResult ) {
				idx.addStat( stat );
				stat.add( "Counter_Index_1_HashCollision", WYK_HashSet.collision );
				stat.add( "Counter_Index_1_HashResize", WYK_HashSet.resize );
			}
		}

		return idx;
	}

	public static JoinMinIndex buildIndexMaxK( List<Record> tableSearched, List<Record> tableIndexed, int nIndex, int qSize,
			StatContainer stat, boolean writeResult, boolean oneSideJoin ) {
		long starttime = System.nanoTime();

		// Build an index
		// Count Invokes per each (token, loc) pair
		List<Map<QGram, WrappedInteger>> invokes = new ArrayList<Map<QGram, WrappedInteger>>();
		long getQGramTime = 0;
		long countIndexingTime = 0;

		JoinMinIndex idx = new JoinMinIndex( qSize );

		try {
			BufferedWriter bw = null;

			if( DEBUG.JoinMinIndexON ) {
				bw = new BufferedWriter( new FileWriter( "JoinMin_Index_Debug.txt" ) );
			}

			StopWatch stepTime = StopWatch.getWatchStarted( "Result_3_1_1_Index_Count_Time" );
			for( Record rec : tableSearched ) {
				long recordStartTime = 0;
				long recordMidTime = 0;

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
					invokes.add( new WYK_HashMap<QGram, WrappedInteger>() );

					if( DEBUG.JoinMinIndexCountON ) {
						idx.countPerPosition.add( 0 );
					}
				}

				long qgramCount = 0;
				for( int i = 0; i < searchmax; ++i ) {
					Map<QGram, WrappedInteger> curridx_invokes = invokes.get( i );

					List<QGram> available = availableQGrams.get( i );
					qgramCount += available.size();
					for( QGram qgram : available ) {
						WrappedInteger count = curridx_invokes.get( qgram );
						if( count == null ) {
							// object ONE is shared to reduce memory usage
							curridx_invokes.put( qgram, ONE );
						}
						else if( count == ONE ) {
							count = new WrappedInteger( 2 );
							curridx_invokes.put( qgram, count );
						}
						else {
							count.increment();
						}
					}

					if( DEBUG.JoinMinIndexCountON ) {
						int newSize = idx.countPerPosition.get( i ) + available.size();

						idx.countPerPosition.set( i, newSize );
					}
				}
				idx.searchedTotalSigCount += qgramCount;

				if( DEBUG.JoinMinON ) {
					countIndexingTime += System.nanoTime() - recordMidTime;
				}

				if( DEBUG.JoinMinIndexON ) {
					bw.write( recordMidTime - recordStartTime + " " );
					bw.write( qgramCount + " " );
					bw.write( "\n" );
				}
			}

			if( DEBUG.JoinMinIndexON ) {
				bw.close();
			}

			idx.countTime = System.nanoTime() - starttime;
			idx.gamma = idx.countTime / idx.searchedTotalSigCount;

			if( DEBUG.JoinMinON ) {
				Util.printLog( "Step 1 Time : " + idx.countTime );
				Util.printLog( "Gamma (buildTime / signature): " + idx.gamma );

				if( writeResult ) {
					stat.add( "Est_Index_0_GetQGramTime", getQGramTime );
					stat.add( "Est_Index_0_CountIndexingTime", countIndexingTime );

					stat.add( "Est_Index_1_Index_Count_Time", idx.countTime );
					stat.add( "Est_Index_1_Time_Per_Sig", Double.toString( idx.gamma ) );
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
				bw_index = new BufferedWriter( new FileWriter( "JoinMin_Index_Content.txt" ) );
			}

			stepTime.resetAndStart( "Result_3_1_2_Indexing_Time" );

			idx.predictCount = 0;
			long indexedElements = 0;

			for( Record rec : tableIndexed ) {
				int[] range = rec.getCandidateLengths( rec.size() - 1 );

				int searchmax = Math.min( range[ 0 ], invokes.size() );

				List<List<QGram>> availableQGrams = null;

				if( oneSideJoin ) {
					availableQGrams = rec.getSelfQGrams( qSize, searchmax );
					// System.out.println( availableQGrams.toString() );
				}
				else {
					availableQGrams = rec.getQGrams( qSize, searchmax );
				}

				for( List<QGram> set : availableQGrams ) {
					idx.indexedTotalSigCount += set.size();
				}

				int minIdx = -1;
				int minInvokes = Integer.MAX_VALUE;

				for( int i = 0; i < searchmax; ++i ) {
					if( availableQGrams.get( i ).isEmpty() ) {
						continue;
					}

					// There is no invocation count: this is the minimum point
					if( i >= invokes.size() ) {
						minIdx = i;
						minInvokes = 0;
						break;
					}

					Map<QGram, WrappedInteger> curridx_invokes = invokes.get( i );
					if( curridx_invokes.size() == 0 ) {
						minIdx = i;
						minInvokes = 0;
						break;
					}
					int invoke = 0;

					for( QGram twogram : availableQGrams.get( i ) ) {
						WrappedInteger count = curridx_invokes.get( twogram );
						if( count != null ) {
							// upper bound
							invoke += count.get();
						}
					}
					if( invoke < minInvokes ) {
						minIdx = i;
						minInvokes = invoke;
					}
				}

				idx.predictCount += minInvokes;

				idx.setIndex( minIdx );

				for( QGram qgram : availableQGrams.get( minIdx ) ) {
					// write2File(bw, minIdx, twogram, rec.getID());

					if( DEBUG.PrintJoinMinIndexON ) {
						bw_index.write( minIdx + ", " + qgram + " : " + rec + "\n" );
					}

					idx.put( minIdx, qgram, rec );
				}

				if( DEBUG.JoinMinON ) {
					indexedElements += availableQGrams.get( minIdx ).size();
				}
			}

			idx.indexTime = System.nanoTime() - starttime;
			idx.delta = idx.indexTime / idx.indexedTotalSigCount;

			if( DEBUG.JoinMinON ) {
				Util.printLog( "Idx size : " + indexedElements );
				Util.printLog( "Predict : " + idx.predictCount );
				Util.printLog( "Step 2 Time : " + idx.indexTime );
				Util.printLog( "Delta (index build / signature ): " + idx.delta );

				if( writeResult ) {
					stat.add( "Stat_JoinMin_Index_Size", indexedElements );
					stat.add( "Stat_Predicted_Comparison", idx.predictCount );

					stat.add( "Est_Index_2_Build_Index_Time", idx.indexTime );
					stat.add( "Est_Index_2_Time_Per_Sig", Double.toString( idx.delta ) );
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
				bw_index.close();
			}
		}
		catch( Exception e ) {
			e.printStackTrace();
		}

		if( DEBUG.JoinMinON ) {
			if( writeResult ) {
				idx.addStat( stat );
				stat.add( "Counter_Index_1_HashCollision", WYK_HashSet.collision );
				stat.add( "Counter_Index_1_HashResize", WYK_HashSet.resize );
			}
		}

		return idx;
	}

	public static JoinMinIndex buildIndexThreshold( List<Record> tableSearched, List<Record> tableIndexed, int maxIndex,
			int qSize, StatContainer stat, boolean writeResult, int threshold ) {
		long starttime = System.nanoTime();

		// Build an index
		// Count Invokes per each (token, loc) pair
		List<Map<QGram, IntegerPair>> invokes = new ArrayList<Map<QGram, IntegerPair>>();
		long getQGramTime = 0;
		long countIndexingTime = 0;

		JoinMinIndex idx = new JoinMinIndex( qSize );

		try {
			BufferedWriter bw = null;

			if( DEBUG.JoinMinIndexON ) {
				bw = new BufferedWriter( new FileWriter( "JoinMin_Index_Debug.txt" ) );
			}

			StopWatch stepTime = StopWatch.getWatchStarted( "Result_3_1_1_Index_Count_Time" );
			for( Record rec : tableSearched ) {
				long recordStartTime = 0;
				long recordMidTime = 0;

				if( DEBUG.JoinMinON ) {
					recordStartTime = System.nanoTime();
				}

				List<List<QGram>> availableQGrams = rec.getQGrams( qSize );

				if( DEBUG.JoinMinON ) {
					recordMidTime = System.nanoTime();
					getQGramTime += recordMidTime - recordStartTime;
				}

				int searchmax = Math.min( availableQGrams.size(), maxIndex );

				for( int i = invokes.size(); i < searchmax; i++ ) {
					invokes.add( new WYK_HashMap<QGram, IntegerPair>() );

					if( DEBUG.JoinMinIndexCountON ) {
						idx.countPerPosition.add( 0 );
					}
				}

				long qgramCount = 0;
				for( int i = 0; i < searchmax; ++i ) {
					Map<QGram, IntegerPair> curridx_invokes = invokes.get( i );

					List<QGram> available = availableQGrams.get( i );
					qgramCount += available.size();
					for( QGram qgram : available ) {
						IntegerPair count = curridx_invokes.get( qgram );
						if( count == null ) {
							count = new IntegerPair( 0, 0 );
							curridx_invokes.put( qgram, count );
						}

						increment( count, rec.getEstNumRecords(), threshold );
					}

					if( DEBUG.JoinMinIndexCountON ) {
						int newSize = idx.countPerPosition.get( i ) + available.size();

						idx.countPerPosition.set( i, newSize );
					}
				}
				idx.searchedTotalSigCount += qgramCount;

				if( DEBUG.JoinMinON ) {
					countIndexingTime += System.nanoTime() - recordMidTime;
				}

				if( DEBUG.JoinMinIndexON ) {
					bw.write( recordMidTime - recordStartTime + " " );
					bw.write( qgramCount + " " );
					bw.write( "\n" );
				}
			}

			if( DEBUG.JoinMinIndexON ) {
				bw.close();
			}

			idx.countTime = System.nanoTime() - starttime;
			idx.gamma = idx.countTime / idx.searchedTotalSigCount;

			if( DEBUG.JoinMinON ) {
				Util.printLog( "Step 1 Time : " + idx.countTime );
				Util.printLog( "Gamma (buildTime / signature): " + idx.gamma );

				if( writeResult ) {
					stat.add( "Est_Index_0_GetQGramTime", getQGramTime );
					stat.add( "Est_Index_0_CountIndexingTime", countIndexingTime );

					stat.add( "Est_Index_1_Index_Count_Time", idx.countTime );
					stat.add( "Est_Index_1_Time_Per_Sig", Double.toString( idx.gamma ) );
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

			stepTime.resetAndStart( "Result_3_1_2_Indexing_Time" );

			idx.predictCount = 0;
			long indexedElements = 0;

			for( Record rec : tableIndexed ) {
				int[] range = rec.getCandidateLengths( rec.size() - 1 );

				int searchmax = Math.min( range[ 0 ], invokes.size() );

				List<List<QGram>> availableQGrams = rec.getQGrams( qSize, searchmax );
				for( List<QGram> set : availableQGrams ) {
					idx.indexedTotalSigCount += set.size();
				}

				int minIdx = -1;
				int minInvokes = Integer.MAX_VALUE;

				for( int i = 0; i < searchmax; ++i ) {
					if( availableQGrams.get( i ).isEmpty() ) {
						continue;
					}

					// There is no invocation count: this is the minimum point
					if( i >= invokes.size() ) {
						minIdx = i;
						minInvokes = 0;
						break;
					}

					Map<QGram, IntegerPair> curridx_invokes = invokes.get( i );
					if( curridx_invokes.size() == 0 ) {
						minIdx = i;
						minInvokes = 0;
						break;
					}
					int invoke = 0;

					for( QGram twogram : availableQGrams.get( i ) ) {
						IntegerPair count = curridx_invokes.get( twogram );
						if( count != null ) {
							// upper bound
							if( rec.getEstNumRecords() > threshold ) {
								invoke += count.i1 + count.i2;
							}
							else {
								invoke += count.i2;
							}
						}
					}
					if( invoke < minInvokes ) {
						minIdx = i;
						minInvokes = invoke;
					}
				}

				idx.predictCount += minInvokes;

				idx.setIndex( minIdx );

				for( QGram qgram : availableQGrams.get( minIdx ) ) {
					// write2File(bw, minIdx, twogram, rec.getID());

					idx.put( minIdx, qgram, rec );
				}

				if( DEBUG.JoinMinON ) {
					indexedElements += availableQGrams.get( minIdx ).size();
				}
			}

			idx.indexTime = System.nanoTime() - starttime;
			idx.delta = idx.indexTime / idx.indexedTotalSigCount;

			if( DEBUG.JoinMinON ) {
				Util.printLog( "Idx size : " + indexedElements );
				Util.printLog( "Predict : " + idx.predictCount );
				Util.printLog( "Step 2 Time : " + idx.indexTime );
				Util.printLog( "Delta (index build / signature ): " + idx.delta );

				if( writeResult ) {
					stat.add( "Stat_JoinMin_Index_Size", indexedElements );
					stat.add( "Stat_Predicted_Comparison", idx.predictCount );

					stat.add( "Est_Index_2_Build_Index_Time", idx.indexTime );
					stat.add( "Est_Index_2_Time_Per_Sig", Double.toString( idx.delta ) );
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
		}
		catch( Exception e ) {
			e.printStackTrace();
		}

		if( DEBUG.JoinMinON ) {
			if( writeResult ) {
				idx.addStat( stat );
				stat.add( "Counter_Index_1_HashCollision", WYK_HashSet.collision );
				stat.add( "Counter_Index_1_HashResize", WYK_HashSet.resize );
			}
		}

		return idx;
	}

	public static void increment( IntegerPair pair, long expandSize, long threshold ) {
		if( expandSize <= threshold ) {
			pair.i1++;
		}
		else {
			pair.i2++;
		}
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
}
