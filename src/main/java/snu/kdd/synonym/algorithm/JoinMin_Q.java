package snu.kdd.synonym.algorithm;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import mine.Record;
import mine.RecordIDComparator;
import snu.kdd.synonym.data.DataInfo;
import snu.kdd.synonym.tools.Param;
import snu.kdd.synonym.tools.StatContainer;
import snu.kdd.synonym.tools.StopWatch;
import tools.DEBUG;
import tools.IntegerPair;
import tools.QGram;
import tools.Rule;
import tools.RuleTrie;
import tools.StaticFunctions;
import tools.WYK_HashMap;
import tools.WYK_HashSet;
import validator.TopDownHashSetSinglePath_DS_SharedPrefix;
import validator.Validator;
import wrapped.WrappedInteger;

public class JoinMin_Q extends AlgorithmTemplate {
	public boolean useAutomata = false;
	public boolean skipChecking = false;
	public int maxIndex = Integer.MAX_VALUE;
	public boolean compact = true;
	// public boolean singleside = false;
	public int qSize = 0;
	// public boolean exact2grams = false;

	RecordIDComparator idComparator;
	RuleTrie ruletrie;

	public static Validator checker;

	/**
	 * Key: (2gram, index) pair<br/>
	 * Value: (min, max, record) triple
	 */
	WYK_HashMap<Integer, Map<QGram, List<Record>>> idx;

	private long buildIndexTime1;
	private long buildIndexTime2;
	private long candExtractTime;
	private long joinTime;

	public double gamma;
	public double delta;
	public double epsilon;

	private long freq = 0;
	private long sumlength = 0;

	private static final WrappedInteger ONE = new WrappedInteger( 1 );

	public JoinMin_Q( String rulefile, String Rfile, String Sfile, String outputFile, DataInfo dataInfo ) throws IOException {
		super( rulefile, Rfile, Sfile, outputFile, dataInfo );

		Record.setStrList( strlist );
		idComparator = new RecordIDComparator();
		ruletrie = new RuleTrie( getRulelist() );
		Record.setRuleTrie( ruletrie );
	}

	public JoinMin_Q( AlgorithmTemplate o, StatContainer stat ) {
		super( o );

		Record.setStrList( strlist );
		idComparator = new RecordIDComparator();
		ruletrie = new RuleTrie( rulelist );
		Record.setRuleTrie( ruletrie );

		this.stat = stat;
	}

	@SuppressWarnings( "unused" )
	private void buildIndex( boolean writeResult ) throws IOException {
		long starttime = System.nanoTime();
		long totalSigCount = 0;

		// Build an index
		// Count Invokes per each (token, loc) pair
		List<Map<QGram, WrappedInteger>> invokes = new ArrayList<Map<QGram, WrappedInteger>>();
		long getQGramTime = 0;
		long countIndexingTime = 0;

		try {
			BufferedWriter bw = null;

			if( DEBUG.JoinMinIndexOn ) {
				bw = new BufferedWriter( new FileWriter( "JoinMin_Index_Debug.txt" ) );
			}

			StopWatch stepTime = StopWatch.getWatchStarted( "Result_3_1_1_Index_Count_Time" );
			for( Record rec : tableSearched ) {
				long recordStartTime = 0;
				long recordMidTime = 0;

				if( DEBUG.ON ) {
					recordStartTime = System.nanoTime();
				}

				List<List<QGram>> availableQGrams = rec.getQGrams( qSize );

				if( DEBUG.ON ) {
					recordMidTime = System.nanoTime();
					getQGramTime += recordMidTime - recordStartTime;
				}

				int searchmax = Math.min( availableQGrams.size(), maxIndex );

				for( int i = invokes.size(); i < searchmax; i++ ) {
					invokes.add( new WYK_HashMap<QGram, WrappedInteger>() );
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
				}
				totalSigCount += qgramCount;

				if( DEBUG.ON ) {
					countIndexingTime += System.nanoTime() - recordMidTime;
				}

				if( DEBUG.JoinMinIndexOn ) {
					bw.write( recordMidTime - recordStartTime + " " );
					bw.write( qgramCount + " " );
					bw.write( "\n" );
				}
			}

			if( DEBUG.JoinMinIndexOn ) {
				bw.close();
			}

			buildIndexTime1 = System.nanoTime() - starttime;
			gamma = ( (double) buildIndexTime1 ) / totalSigCount;

			if( DEBUG.ON ) {
				System.out.println( "Step 1 Time : " + buildIndexTime1 );
				System.out.println( "Gamma (buildTime / signature): " + gamma );

				if( writeResult ) {
					stat.add( "Est_Index_0_GetQGramTime", getQGramTime );
					stat.add( "Est_Index_0_CountIndexingTime", countIndexingTime );

					stat.add( "Est_Index_1_Index_Count_Time", buildIndexTime1 );
					stat.add( "Est_Index_1_Time_Per_Sig", Double.toString( gamma ) );
				}
			}

			starttime = System.nanoTime();

			if( DEBUG.ON ) {
				if( writeResult ) {
					stepTime.stopAndAdd( stat );
				}
				else {
					stepTime.stop();
				}
			}

			stepTime.resetAndStart( "Result_3_1_2_Indexing_Time" );
			totalSigCount = 0;
			idx = new WYK_HashMap<Integer, Map<QGram, List<Record>>>();

			long predictCount = 0;
			long indexedElements = 0;
			for( Record rec : tableIndexed ) {
				int[] range = rec.getCandidateLengths( rec.size() - 1 );
				int searchmax;

				if( range[ 0 ] == 1 ) {
					searchmax = 1;
				}
				else {
					searchmax = Math.min( range[ 0 ] - 1, maxIndex );
				}
				// searchmax = Math.min( searchmax, invokes.size() );

				List<List<QGram>> availableQGrams = rec.getQGrams( qSize, searchmax );
				for( List<QGram> set : availableQGrams ) {
					totalSigCount += set.size();
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

				predictCount += minInvokes;

				Map<QGram, List<Record>> curridx = idx.get( minIdx );
				if( curridx == null ) {
					curridx = new WYK_HashMap<QGram, List<Record>>( 1000 );
					// curridx = new HashMap<IntegerPair, List<Record>>();
					idx.put( minIdx, curridx );
				}

				for( QGram qgram : availableQGrams.get( minIdx ) ) {
					// write2File(bw, minIdx, twogram, rec.getID());
					if( true ) {
						List<Record> list = curridx.get( qgram );
						if( list == null ) {
							list = new ArrayList<Record>();
							curridx.put( qgram, list );
						}
						list.add( rec );
					}
				}
				indexedElements += availableQGrams.get( minIdx ).size();
			}

			buildIndexTime2 = System.nanoTime() - starttime;
			delta = ( (double) buildIndexTime2 ) / totalSigCount;

			if( DEBUG.ON ) {
				System.out.println( "Idx size : " + indexedElements );
				System.out.println( "Predict : " + predictCount );
				System.out.println( "Step 2 Time : " + buildIndexTime2 );
				System.out.println( "Delta (index build / signature ): " + delta );

				if( writeResult ) {
					stat.add( "Stat_JoinMin_Index_Size", indexedElements );
					stat.add( "Stat_Predicted_Comparison", predictCount );

					stat.add( "Est_Index_2_Build_Index_Time", buildIndexTime2 );
					stat.add( "Est_Index_2_Time_Per_Sig", Double.toString( delta ) );
					stepTime.stopAndAdd( stat );
				}
				else {
					stepTime.stop();
				}

				stepTime.resetAndStart( "Result_3_3_Statistic Time" );

				int sum = 0;
				int ones = 0;
				long count = 0;
				///// Statistics
				for( Map<QGram, List<Record>> curridx : idx.values() ) {
					WYK_HashMap<QGram, List<Record>> tmp = (WYK_HashMap<QGram, List<Record>>) curridx;
					if( sum == 0 ) {
						tmp.printStat();
					}

					for( List<Record> list : curridx.values() ) {
						if( list.size() == 1 ) {
							++ones;
							continue;
						}
						sum++;
						count += list.size();
					}
				}
				System.out.println( "key-value pairs(all) : " + ( sum + ones ) );
				System.out.println( "iIdx size(all) : " + ( count + ones ) );
				System.out.println( "Rec per idx(all) : " + ( (double) ( count + ones ) ) / ( sum + ones ) );
				System.out.println( "key-value pairs(w/o 1) : " + sum );
				System.out.println( "iIdx size(w/o 1) : " + count );
				System.out.println( "Rec per idx(w/o 1) : " + ( (double) count ) / sum );
				System.out.println( "2Gram retrieval: " + Record.exectime );
				///// Statistics
				sum = 0;
				ones = 0;
				count = 0;
				for( Map<QGram, WrappedInteger> curridx : invokes ) {
					WYK_HashMap<QGram, WrappedInteger> tmp = (WYK_HashMap<QGram, WrappedInteger>) curridx;
					if( sum == 0 ) {
						tmp.printStat();
					}
					for( Entry<QGram, WrappedInteger> list : curridx.entrySet() ) {
						if( list.getValue().get() == 1 ) {
							++ones;
							continue;
						}
						sum++;
						count += list.getValue().get();
					}
				}
				System.out.println( "key-value pairs(all) : " + ( sum + ones ) );
				System.out.println( "iIdx size(all) : " + ( count + ones ) );
				System.out.println( "Rec per idx(all) : " + ( (double) ( count + ones ) ) / ( sum + ones ) );
				System.out.println( "key-value pairs(w/o 1) : " + sum );
				System.out.println( "iIdx size(w/o 1) : " + count );
				System.out.println( "Rec per idx(w/o 1) : " + ( (double) count ) / sum );
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

		if( DEBUG.ON ) {
			if( writeResult ) {
				stat.add( "Counter_Index_0_Get_Count", idx.getCount );
				stat.add( "Counter_Index_0_GetIter_Count", idx.getIterCount );
				stat.add( "Counter_Index_0_Put_Count", idx.putCount );
				stat.add( "Counter_Index_0_Resize_Count", idx.resizeCount );
				stat.add( "Counter_Index_0_Remove_Count", idx.removeCount );
				stat.add( "Counter_Index_0_RemoveIter_Count", idx.removeIterCount );
				stat.add( "Counter_Index_0_PutRemoved_Count", idx.putRemovedCount );
				stat.add( "Counter_Index_0_RemoveFound_Count", idx.removeFoundCount );
				stat.add( "Counter_Index_1_HashCollision", WYK_HashSet.collision );
				stat.add( "Counter_Index_1_HashResize", WYK_HashSet.resize );
			}
		}
	}

	@SuppressWarnings( "unused" )
	private List<IntegerPair> join( boolean writeResult ) {
		BufferedWriter bw = null;

		if( DEBUG.JoinMinJoinOn ) {
			try {
				bw = new BufferedWriter( new FileWriter( "JoinMin_Join_Debug.txt" ) );
			}
			catch( Exception e ) {
				e.printStackTrace();
			}
		}

		List<IntegerPair> rslt = new ArrayList<IntegerPair>();
		long starttime = System.nanoTime() - Record.exectime;
		// long totalSigCount = 0;

		long count = 0;

		long appliedRules_sum = 0;
		long equivComparisons = 0;
		long getQGramTime = 0;
		// long lastTokenFiltered = 0;

		for( Record recS : tableSearched ) {
			long qgramStartTime = 0;
			long joinStartTime = 0;
			long qgramCount = 0;

			if( DEBUG.ON ) {
				qgramStartTime = System.nanoTime();
			}

			List<List<QGram>> availableQGrams = recS.getQGrams( qSize );

			if( DEBUG.ON ) {
				getQGramTime += System.nanoTime() - qgramStartTime;
			}

			int[] range = recS.getCandidateLengths( recS.size() - 1 );
			int searchmax = Math.min( availableQGrams.size(), maxIndex );

			if( DEBUG.JoinMinJoinOn ) {
				joinStartTime = System.nanoTime();
			}

			for( int i = 0; i < searchmax; ++i ) {
				Map<QGram, List<Record>> curridx = idx.get( i );
				if( curridx == null ) {
					continue;
				}

				Set<Record> candidates = new WYK_HashSet<Record>();

				for( QGram qgram : availableQGrams.get( i ) ) {
					if( DEBUG.JoinMinJoinOn ) {
						qgramCount++;
					}

					List<Record> tree = curridx.get( qgram );

					if( tree == null ) {
						continue;
					}

					for( Record e : tree ) {
						if( StaticFunctions.overlap( e.getMinLength(), e.getMaxLength(), range[ 0 ], range[ 1 ] ) ) {
							// if( recS.shareLastToken( e ) ) {
							candidates.add( e );
							count++;
							// }
							// else {
							// lastTokenFiltered++;
							// }
						}
					}
				}

				if( skipChecking ) {
					continue;
				}

				equivComparisons += candidates.size();
				for( Record recR : candidates ) {
					long ruleiters = 0;
					long reccalls = 0;
					long entryiters = 0;

					if( DEBUG.ON ) {
						ruleiters = Validator.niterrules;
						reccalls = Validator.recursivecalls;
						entryiters = Validator.niterentry;
					}

					long st = System.nanoTime();
					int compare = checker.isEqual( recR, recS );
					long duration = System.nanoTime() - st;

					if( DEBUG.ON ) {
						ruleiters = Validator.niterrules - ruleiters;
						reccalls = Validator.recursivecalls - reccalls;
						entryiters = Validator.niterentry - entryiters;

						// bw.write( duration + "\t" + compare + "\t" + recR.size() + "\t" + recR.getRuleCount() + "\t"
						// + recR.getFirstRuleCount() + "\t" + recS.size() + "\t" + recS.getRuleCount() + "\t"
						// + recS.getFirstRuleCount() + "\t" + ruleiters + "\t" + reccalls + "\t" + entryiters + "\n" );
					}

					joinTime += duration;
					if( compare >= 0 ) {
						rslt.add( new IntegerPair( recS.getID(), recR.getID() ) );
						appliedRules_sum += compare;
					}
				}
			}

			if( DEBUG.JoinMinJoinOn ) {
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

		if( DEBUG.JoinMinJoinOn ) {
			try {
				bw.close();
			}
			catch( IOException e ) {
				e.printStackTrace();
			}
		}

		if( count == 0 ) {
			// To avoid NaN
			count = 1;
		}
		epsilon = joinTime / count;

		if( DEBUG.ON ) {
			System.out.println( "Avg applied rules : " + appliedRules_sum + "/" + rslt.size() );
			if( checker.getClass() == TopDownHashSetSinglePath_DS_SharedPrefix.class ) {
				System.out.println( "Prefix freq : " + freq );
				System.out.println( "Prefix sumlength : " + sumlength );
			}

			candExtractTime = System.nanoTime() - Record.exectime - starttime - joinTime;

			System.out.println( "Est weight : " + count );
			System.out.println( "Cand extract time : " + candExtractTime );
			System.out.println( "Join time : " + joinTime );

			if( writeResult ) {
				// stat.add( "Last Token Filtered", lastTokenFiltered );
				stat.add( "Est_Join_0_GetQGramTime", getQGramTime );
				stat.add( "Stat_Equiv_Comparison", equivComparisons );
				stat.add( "Stat_getQGramCount", Record.getQGramCount );
			}
		}

		if( DEBUG.JoinMinIndexOn ) {
			try {
				bw.close();
			}
			catch( Exception e ) {
				e.printStackTrace();
			}
		}

		return rslt;

	}

	public void statistics() {
		long strlengthsum = 0;
		long strmaxinvsearchrangesum = 0;
		int strs = 0;
		int maxstrlength = 0;

		long rhslengthsum = 0;
		int rules = 0;
		int maxrhslength = 0;

		for( Record rec : tableSearched ) {
			strmaxinvsearchrangesum += rec.getMaxInvSearchRange();
			int length = rec.getTokenArray().length;
			++strs;
			strlengthsum += length;
			maxstrlength = Math.max( maxstrlength, length );
		}
		for( Record rec : tableIndexed ) {
			strmaxinvsearchrangesum += rec.getMaxInvSearchRange();
			int length = rec.getTokenArray().length;
			++strs;
			strlengthsum += length;
			maxstrlength = Math.max( maxstrlength, length );
		}

		for( Rule rule : getRulelist() ) {
			int length = rule.getTo().length;
			++rules;
			rhslengthsum += length;
			maxrhslength = Math.max( maxrhslength, length );
		}

		System.out.println( "Average str length: " + strlengthsum + "/" + strs );
		System.out.println( "Average maxinvsearchrange: " + strmaxinvsearchrangesum + "/" + strs );
		System.out.println( "Maximum str length: " + maxstrlength );
		System.out.println( "Average rhs length: " + rhslengthsum + "/" + rules );
		System.out.println( "Maximum rhs length: " + maxrhslength );
	}

	public void run() {
		long startTime = 0;

		if( DEBUG.ON ) {
			startTime = System.nanoTime();
		}

		preprocess( compact, maxIndex, useAutomata );

		if( DEBUG.ON ) {
			stat.add( "Mem_2_Preprocessed", ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 );
			System.out.print( "Preprocess finished time " + ( System.nanoTime() - startTime ) );
		}

		runWithoutPreprocess( true );
	}

	public void runWithoutPreprocess( boolean writeResult ) {
		// Retrieve statistics
		StopWatch stepTime = null;
		if( DEBUG.ON ) {
			statistics();

			stepTime = StopWatch.getWatchStarted( "Result_3_1_Index_Building_Time" );
		}

		try {
			buildIndex( writeResult );
		}
		catch( Exception e ) {
			e.printStackTrace();
		}

		if( DEBUG.ON ) {
			if( writeResult ) {
				stepTime.stopAndAdd( stat );
				stat.add( "Mem_3_BuildIndex", ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 );
			}
			else {
				stepTime.stop();
			}
			stepTime.resetAndStart( "Result_3_2_Join_Time" );
		}

		Collection<IntegerPair> rslt = join( writeResult );

		if( DEBUG.ON ) {
			if( writeResult ) {
				stepTime.stopAndAdd( stat );
				stat.add( "Mem_4_Joined", ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 );

				stat.add( "Counter_Final_1_HashCollision", WYK_HashSet.collision );
				stat.add( "Counter_Final_1_HashResize", WYK_HashSet.resize );

				stat.add( "Counter_Final_2_MapCollision", WYK_HashMap.collision );
				stat.add( "Counter_Final_2_MapResize", WYK_HashMap.resize );
			}
			else {
				stat.add( "Sample_JoinMin_Result", rslt.size() );
				stepTime.stop();
			}
		}

		if( writeResult ) {
			if( DEBUG.ON ) {
				stepTime.resetAndStart( "Result_4_Write_Time" );
			}

			this.writeResult( rslt );

			if( DEBUG.ON ) {
				stepTime.stopAndAdd( stat );
			}
		}
	}

	@Override
	public String getVersion() {
		return "1.2";
	}

	@Override
	public String getName() {
		return "JoinMin_Q";
	}

	@Override
	public void run( String[] args, StatContainer stat ) {
		this.stat = stat;

		Param params = Param.parseArgs( args, stat );

		// Setup parameters
		useAutomata = params.isUseACAutomata();
		skipChecking = params.isSkipChecking();
		compact = params.isCompact();
		checker = params.getValidator();
		qSize = params.getQGramSize();
		// exact2grams = params.isExact2Grams();

		StopWatch preprocessTime = null;
		if( DEBUG.ON ) {
			preprocessTime = StopWatch.getWatchStarted( "Result_2_Preprocess_Total_Time" );
		}
		preprocess( compact, maxIndex, useAutomata );

		if( DEBUG.ON ) {
			preprocessTime.stopAndAdd( stat );

			preprocessTime.resetAndStart( "Result_3_Run_Time" );
		}

		runWithoutPreprocess( true );

		if( DEBUG.ON ) {
			preprocessTime.stopAndAdd( stat );
			Validator.printStats();
		}
	}
}
