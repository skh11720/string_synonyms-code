package snu.kdd.synonym.algorithm;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
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
import tools.IntegerPair;
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
	public boolean singleside = false;
	public boolean exact2grams = false;

	RecordIDComparator idComparator;
	RuleTrie ruletrie;

	public static Validator checker;

	/**
	 * Key: (2gram, index) pair<br/>
	 * Value: (min, max, record) triple
	 */
	Map<Integer, Map<IntegerPair, List<Record>>> idx;

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

	private void buildIndex( boolean writeResult ) throws IOException {
		long starttime = System.nanoTime();
		long totalSigCount = 0;

		// Build an index
		// Count Invokes per each (token, loc) pair
		List<Map<IntegerPair, WrappedInteger>> invokes = new ArrayList<Map<IntegerPair, WrappedInteger>>();
		int invokesInitialized = 0;

		StopWatch stepTime = StopWatch.getWatchStarted( "Index Count Time" );
		for( Record rec : tableS ) {
			List<Set<IntegerPair>> available2Grams = exact2grams ? rec.getExact2Grams() : rec.get2Grams();

			int searchmax = Math.min( available2Grams.size(), maxIndex );

			for( int i = invokesInitialized; i < searchmax; i++ ) {
				invokes.add( new WYK_HashMap<IntegerPair, WrappedInteger>() );
			}
			if( invokesInitialized < searchmax ) {
				invokesInitialized = searchmax;
			}

			for( int i = 0; i < searchmax; ++i ) {
				Map<IntegerPair, WrappedInteger> curridx_invokes = invokes.get( i );

				Set<IntegerPair> available = available2Grams.get( i );
				totalSigCount += available.size();
				for( IntegerPair twogram : available ) {
					WrappedInteger count = curridx_invokes.get( twogram );
					if( count == null ) {
						// object ONE is shared to reduce memory usage
						curridx_invokes.put( twogram, ONE );
					}
					else if( count == ONE ) {
						count = new WrappedInteger( 2 );
						curridx_invokes.put( twogram, count );
					}
					else {
						count.increment();
					}
				}
			}
		}

		buildIndexTime1 = System.nanoTime() - starttime;
		gamma = ( (double) buildIndexTime1 ) / totalSigCount;
		System.out.println( "Step 1 Time : " + buildIndexTime1 );
		System.out.println( "Gamma (buildTime / signature): " + gamma );

		starttime = System.nanoTime();
		if( writeResult ) {
			stepTime.stopAndAdd( stat );
		}
		else {
			stepTime.stop();
		}

		stepTime.resetAndStart( "Indexing Time" );
		totalSigCount = 0;
		idx = new WYK_HashMap<Integer, Map<IntegerPair, List<Record>>>();

		long predictCount = 0;
		long indexedElements = 0;
		for( Record rec : tableT ) {
			int[] range = rec.getCandidateLengths( rec.size() - 1 );
			int searchmax = Math.min( range[ 0 ], maxIndex );
			// searchmax = Math.min( searchmax, invokes.size() );

			List<Set<IntegerPair>> available2Grams = exact2grams ? rec.getExact2Grams() : rec.get2GramsWithBound( searchmax );
			for( Set<IntegerPair> set : available2Grams ) {
				totalSigCount += set.size();
			}

			int minIdx = -1;
			int minInvokes = Integer.MAX_VALUE;

			for( int i = 0; i < searchmax; ++i ) {
				if( available2Grams.get( i ).isEmpty() ) {
					continue;
				}

				// There is no invocation count: this is the minimum point

				if( i >= invokes.size() ) {
					minIdx = i;
					minInvokes = 0;
					break;
				}

				Map<IntegerPair, WrappedInteger> curridx_invokes = invokes.get( i );
				if( curridx_invokes.size() == 0 ) {
					minIdx = i;
					minInvokes = 0;
					break;
				}
				int invoke = 0;

				for( IntegerPair twogram : available2Grams.get( i ) ) {
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

			Map<IntegerPair, List<Record>> curridx = idx.get( minIdx );
			if( curridx == null ) {
				curridx = new WYK_HashMap<IntegerPair, List<Record>>( 1000 );
				// curridx = new HashMap<IntegerPair, List<Record>>();
				idx.put( minIdx, curridx );
			}

			for( IntegerPair twogram : available2Grams.get( minIdx ) ) {
				// write2File(bw, minIdx, twogram, rec.getID());
				if( true ) {
					List<Record> list = curridx.get( twogram );
					if( list == null ) {
						list = new ArrayList<Record>();
						curridx.put( twogram, list );
					}
					list.add( rec );
				}
			}
			indexedElements += available2Grams.get( minIdx ).size();
		}
		System.out.println( "Idx size : " + indexedElements );
		System.out.println( "Predict : " + predictCount );

		if( writeResult ) {
			stat.add( "Stat_JoinMin_Index Size", indexedElements );
			stat.add( "Stat_Predicted_Comparison", predictCount );
		}

		buildIndexTime2 = System.nanoTime() - starttime;
		System.out.println( "Step 2 Time : " + buildIndexTime2 );
		delta = ( (double) buildIndexTime2 ) / totalSigCount;
		System.out.println( "Delta (index build / signature ): " + delta );
		if( writeResult ) {
			stepTime.stopAndAdd( stat );
		}
		else {
			stepTime.stop();
		}

		stepTime.resetAndStart( "Statistic Time" );
		int sum = 0;
		int ones = 0;
		long count = 0;
		///// Statistics
		for( Map<IntegerPair, List<Record>> curridx : idx.values() ) {
			WYK_HashMap<IntegerPair, List<Record>> tmp = (WYK_HashMap<IntegerPair, List<Record>>) curridx;
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
		for( Map<IntegerPair, WrappedInteger> curridx : invokes ) {
			WYK_HashMap<IntegerPair, WrappedInteger> tmp = (WYK_HashMap<IntegerPair, WrappedInteger>) curridx;
			if( sum == 0 ) {
				tmp.printStat();
			}
			for( Entry<IntegerPair, WrappedInteger> list : curridx.entrySet() ) {
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

	static ByteBuffer buffer = ByteBuffer.allocate( 16 );

	@SuppressWarnings( "unused" )
	private static void write2File( BufferedOutputStream bos, int idx, IntegerPair twogram, int id ) throws IOException {
		buffer.clear();
		buffer.putInt( idx );
		buffer.putInt( twogram.i1 );
		buffer.putInt( twogram.i2 );
		buffer.putInt( id );
		bos.write( buffer.array() );
	}

	private List<IntegerPair> join( boolean writeResult ) {
		// BufferedWriter bw = new BufferedWriter( new FileWriter( "join.txt" ) );

		List<IntegerPair> rslt = new ArrayList<IntegerPair>();
		long starttime = System.nanoTime() - Record.exectime;
		// long totalSigCount = 0;

		long appliedRules_sum = 0;
		long count = 0;
		long equivComparisons = 0;
		long lastTokenFiltered = 0;
		for( Record recS : tableS ) {
			List<Set<IntegerPair>> available2Grams = exact2grams ? recS.getExact2Grams() : recS.get2Grams();
			// for (Set<IntegerPair> set : available2Grams)
			// totalSigCount += set.size();
			int[] range = recS.getCandidateLengths( recS.size() - 1 );
			int searchmax = Math.min( available2Grams.size(), maxIndex );
			for( int i = 0; i < searchmax; ++i ) {
				Map<IntegerPair, List<Record>> curridx = idx.get( i );
				if( curridx == null ) {
					continue;
				}

				Set<Record> candidates = new HashSet<Record>();

				for( IntegerPair twogram : available2Grams.get( i ) ) {
					List<Record> tree = curridx.get( twogram );

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
					long ruleiters = Validator.niterrules;
					long reccalls = Validator.recursivecalls;
					long entryiters = Validator.niterentry;

					long st = System.nanoTime();
					int compare = checker.isEqual( recR, recS );
					long duration = System.nanoTime() - st;

					ruleiters = Validator.niterrules - ruleiters;
					reccalls = Validator.recursivecalls - reccalls;
					entryiters = Validator.niterentry - entryiters;

					// bw.write( duration + "\t" + compare + "\t" + recR.size() + "\t" + recR.getRuleCount() + "\t"
					// + recR.getFirstRuleCount() + "\t" + recS.size() + "\t" + recS.getRuleCount() + "\t"
					// + recS.getFirstRuleCount() + "\t" + ruleiters + "\t" + reccalls + "\t" + entryiters + "\n" );

					joinTime += duration;
					if( compare >= 0 ) {
						rslt.add( new IntegerPair( recR.getID(), recS.getID() ) );
						appliedRules_sum += compare;
					}
				}
			}
		}
		if( writeResult ) {
			stat.add( "Last Token Filtered", lastTokenFiltered );
		}
		System.out.println( "Avg applied rules : " + appliedRules_sum + "/" + rslt.size() );
		if( checker.getClass() == TopDownHashSetSinglePath_DS_SharedPrefix.class ) {
			System.out.println( "Prefix freq : " + freq );
			System.out.println( "Prefix sumlength : " + sumlength );
		}
		candExtractTime = System.nanoTime() - Record.exectime - starttime - joinTime;
		double weight = count;
		if( weight == 0 ) {
			// To avoid NaN
			weight = 1;
		}
		System.out.println( "Est weight : " + weight );
		System.out.println( "Cand extract time : " + candExtractTime );
		System.out.println( "Join time : " + joinTime );
		epsilon = ( joinTime ) / weight;
		// bw.close();

		if( writeResult ) {
			stat.add( "Equiv Comparison", equivComparisons );
		}

		return rslt;

	}

	private void buildIndexSingleSide() {
		long elements = 0;
		long predictCount = 0;
		// Build an index
		// Count Invokes per each (twogram, loc) pair
		Map<Integer, Map<IntegerPair, Integer>> invokes = new WYK_HashMap<Integer, Map<IntegerPair, Integer>>();
		for( Record rec : tableS ) {
			for( int i = 0; i < rec.size(); ++i ) {
				Map<IntegerPair, Integer> curridx_invokes = invokes.get( i );
				if( curridx_invokes == null ) {
					curridx_invokes = new WYK_HashMap<IntegerPair, Integer>();
					invokes.put( i, curridx_invokes );
				}
				IntegerPair twogram = rec.getOriginal2Gram( i );
				Integer count = curridx_invokes.get( twogram );
				if( count == null )
					count = 1;
				else
					count += 1;
				curridx_invokes.put( twogram, count );
			}
		}

		idx = new WYK_HashMap<Integer, Map<IntegerPair, List<Record>>>();
		for( Record rec : tableT ) {
			List<Set<IntegerPair>> available2Grams = exact2grams ? rec.getExact2Grams() : rec.get2Grams();
			int[] range = rec.getCandidateLengths( rec.size() - 1 );
			int minIdx = -1;
			int minInvokes = Integer.MAX_VALUE;
			int searchmax = Math.min( range[ 0 ], maxIndex );
			for( int i = 0; i < searchmax; ++i ) {
				int invoke = 0;
				Map<IntegerPair, Integer> curridx_invokes = invokes.get( i );
				// There is no invocation count: this is the minimum point
				if( curridx_invokes == null ) {
					minIdx = i;
					minInvokes = 0;
					break;
				}
				for( IntegerPair twogram : available2Grams.get( i ) ) {
					Integer count = curridx_invokes.get( twogram );
					if( count != null )
						invoke += count;
				}
				if( invoke < minInvokes ) {
					minIdx = i;
					minInvokes = invoke;
				}
			}

			predictCount += minInvokes;

			Map<IntegerPair, List<Record>> curridx = idx.get( minIdx );
			if( curridx == null ) {
				curridx = new WYK_HashMap<IntegerPair, List<Record>>();
				idx.put( minIdx, curridx );
			}
			for( IntegerPair twogram : available2Grams.get( minIdx ) ) {
				List<Record> list = curridx.get( twogram );
				if( list == null ) {
					list = new ArrayList<Record>();
					curridx.put( twogram, list );
				}
				list.add( rec );
			}
			elements += available2Grams.get( minIdx ).size();
		}
		System.out.println( "Predict : " + predictCount );
		System.out.println( "Idx size : " + elements );

		///// Statistics
		int sum = 0;
		long count = 0;
		for( Map<IntegerPair, List<Record>> curridx : idx.values() ) {
			for( List<Record> list : curridx.values() ) {
				if( list.size() == 1 )
					continue;
				sum++;
				count += list.size();
			}
		}
		System.out.println( "iIdx size : " + count );
		System.out.println( "Rec per idx : " + ( (double) count ) / sum );
	}

	private WYK_HashSet<IntegerPair> joinSingleSide() {
		WYK_HashSet<IntegerPair> rslt = new WYK_HashSet<IntegerPair>();

		long appliedRules_sum = 0;
		for( Record recS : tableS ) {
			int minlength = recS.getMinLength();
			int maxlength = recS.getMaxLength();
			for( int i = 0; i < recS.size(); ++i ) {
				Map<IntegerPair, List<Record>> curridx = idx.get( i );
				if( curridx == null )
					continue;
				IntegerPair twogram = recS.getOriginal2Gram( i );
				List<Record> candidatesList = new ArrayList<Record>();
				List<Record> tree = curridx.get( twogram );

				if( tree == null ) {
					continue;
				}
				for( Record e : tree ) {
					if( StaticFunctions.overlap( e.getMinLength(), e.getMaxLength(), minlength, maxlength ) )
						candidatesList.add( e );
				}
				if( skipChecking ) {
					continue;
				}
				for( Record recR : candidatesList ) {
					int compare = checker.isEqual( recR, recS );
					if( compare >= 0 ) {
						rslt.add( new IntegerPair( recR.getID(), recS.getID() ) );
						appliedRules_sum += compare;
					}
				}
			}
		}
		System.out.println( "Avg applied rules : " + appliedRules_sum + "/" + rslt.size() );

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

		for( Record rec : tableT ) {
			strmaxinvsearchrangesum += rec.getMaxInvSearchRange();
			int length = rec.getTokenArray().length;
			++strs;
			strlengthsum += length;
			maxstrlength = Math.max( maxstrlength, length );
		}
		for( Record rec : tableS ) {
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
		long startTime = System.nanoTime();
		preprocess( compact, maxIndex, useAutomata );
		System.out.print( "Preprocess finished time " + ( System.nanoTime() - startTime ) );

		runWithoutPreprocess( true );
	}

	public void runWithoutPreprocess( boolean writeResult ) {
		// Retrieve statistics
		statistics();

		StopWatch stepTime = StopWatch.getWatchStarted( "Preprocess Total Time" );
		if( singleside ) {
			buildIndexSingleSide();
		}
		else {
			try {
				buildIndex( writeResult );
			}
			catch( Exception e ) {
				e.printStackTrace();
				System.exit( 0 );
			}
		}
		if( writeResult ) {
			stepTime.stopAndAdd( stat );
		}
		else {
			stepTime.stop();
		}

		stepTime.resetAndStart( "Join Total Time" );
		Collection<IntegerPair> rslt = ( singleside ? joinSingleSide() : join( writeResult ) );
		if( writeResult ) {
			stepTime.stopAndAdd( stat );
		}
		else {
			stepTime.stop();
		}

		System.out.println( "Result Size: " + rslt.size() );
		if( !writeResult ) {
			stat.add( "Sample_JoinMin Result", rslt.size() );
		}
		else {
			this.writeResult( rslt );
		}

		if( checker.getClass() == TopDownHashSetSinglePath_DS_SharedPrefix.class ) {
			System.out.println( "Prev entry count : " + TopDownHashSetSinglePath_DS_SharedPrefix.prevEntryCount );
			System.out.println(
					"Effective prev entry count : " + TopDownHashSetSinglePath_DS_SharedPrefix.effectivePrevEntryCount );
		}
	}

	@Override
	public String getVersion() {
		return "1.0";
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
		exact2grams = params.isExact2Grams();

		StopWatch preprocessTime = StopWatch.getWatchStarted( "preprocessing time" );
		preprocess( compact, maxIndex, useAutomata );
		preprocessTime.stop();

		StopWatch processTime = StopWatch.getWatchStarted( "processing time" );
		runWithoutPreprocess( true );
		processTime.stop();

		Validator.printStats();

		stat.add( preprocessTime );
		stat.add( processTime );
	}
}
