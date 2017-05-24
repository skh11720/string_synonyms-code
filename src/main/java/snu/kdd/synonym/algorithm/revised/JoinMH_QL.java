package snu.kdd.synonym.algorithm.revised;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mine.Record;
import mine.RecordIDComparator;
import snu.kdd.synonym.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.data.DataInfo;
import snu.kdd.synonym.tools.Param;
import snu.kdd.synonym.tools.StatContainer;
import snu.kdd.synonym.tools.StopWatch;
import tools.IntIntRecordTriple;
import tools.IntegerPair;
import tools.StaticFunctions;
import tools.WYK_HashMap;
import tools.WYK_HashSet;
import validator.Validator;

public class JoinMH_QL extends AlgorithmTemplate {
	public boolean useAutomata = true;
	public boolean skipChecking = false;
	public boolean compact = false;
	public boolean exact2grams = false;
	RecordIDComparator idComparator;
	public int maxIndexLength = 3;
	static Validator checker;
	/**
	 * Key: twogram<br/>
	 * Value IntervalTree Key: length of record (min, max)<br/>
	 * Value IntervalTree Value: record
	 */
	List<Map<IntegerPair, List<IntIntRecordTriple>>> idx;

	public JoinMH_QL( String rulefile, String Rfile, String Sfile, String outFile, DataInfo dataInfo, boolean oneSideJoin )
			throws IOException {
		super( rulefile, Rfile, Sfile, outFile, dataInfo, oneSideJoin );
		idComparator = new RecordIDComparator();
	}

	private void buildIndex() {
		try {
			// BufferedWriter bw = new BufferedWriter( new FileWriter( "debug.txt" ) );
			long elements = 0;
			// Build an index

			idx = new ArrayList<Map<IntegerPair, List<IntIntRecordTriple>>>();
			for( int i = 0; i < maxIndexLength; ++i ) {
				idx.add( new WYK_HashMap<IntegerPair, List<IntIntRecordTriple>>() );
			}

			for( Record rec : tableSearched ) {
				List<Set<IntegerPair>> available2Grams = exact2grams ? rec.getExact2Grams()
						: rec.get2GramsWithBound( maxIndexLength );

				int[] range = rec.getCandidateLengths( rec.size() - 1 );
				int boundary = Math.min( range[ 1 ], maxIndexLength );
				for( int i = 0; i < boundary; ++i ) {
					Map<IntegerPair, List<IntIntRecordTriple>> map = idx.get( i );
					for( IntegerPair twogram : available2Grams.get( i ) ) {
						List<IntIntRecordTriple> list = map.get( twogram );
						if( list == null ) {
							list = new ArrayList<IntIntRecordTriple>();
							map.put( twogram, list );
						}
						list.add( new IntIntRecordTriple( range[ 0 ], range[ 1 ], rec ) );
					}
					elements += available2Grams.get( i ).size();
				}
			}
			stat.add( "Index Size", elements );
			System.out.println( "Idx size : " + elements );

			// computes the statistics of the indexes
			String indexStr = "";
			for( int i = 0; i < maxIndexLength; ++i ) {
				Map<IntegerPair, List<IntIntRecordTriple>> ithidx = idx.get( i );
				System.out.println( i + "th iIdx key-value pairs: " + ithidx.size() );
				// Statistics
				int sum = 0;
				long singlelistsize = 0;
				long count = 0;
				long sqsum = 0;
				for( Map.Entry<IntegerPair, List<IntIntRecordTriple>> entry : ithidx.entrySet() ) {
					List<IntIntRecordTriple> list = entry.getValue();

					// bw.write( "Key " + Record.strlist.get( entry.getKey().i1 ) + " " + Record.strlist.get( entry.getKey().i2 )
					// + "\n" );
					// for( IntIntRecordTriple triple : list ) {
					// bw.write( triple.toString() + "\n" );
					// }

					sqsum += list.size() * list.size();
					if( list.size() == 1 ) {
						++singlelistsize;
						continue;
					}
					sum++;
					count += list.size();
				}
				System.out.println( i + "th Single value list size : " + singlelistsize );
				System.out.println( i + "th iIdx size(w/o 1) : " + count );
				System.out.println( i + "th Rec per idx(w/o 1) : " + ( (double) count ) / sum );
				System.out.println( i + "th Sqsum : " + sqsum );

				long totalCount = count + singlelistsize;
				int exp = 0;
				while( totalCount / 1000 != 0 ) {
					totalCount = totalCount / 1000;
					exp++;
				}

				if( exp == 1 ) {
					indexStr = indexStr + totalCount + "k ";
				}
				else if( exp == 2 ) {
					indexStr = indexStr + totalCount + "M ";
				}
				else {
					indexStr = indexStr + totalCount + "G ";
				}
			}

			stat.add( "Index Size Per Position", indexStr );

			// bw.close();
		}
		catch( Exception e ) {
			e.printStackTrace();
			System.exit( 1 );
		}
	}

	private WYK_HashSet<IntegerPair> join() {
		WYK_HashSet<IntegerPair> rslt = new WYK_HashSet<IntegerPair>();
		long count = 0;
		long lengthFiltered = 0;

		long cand_sum[] = new long[ maxIndexLength ];
		long cand_sum_afterprune[] = new long[ maxIndexLength ];
		long cand_sum_afterunion[] = new long[ maxIndexLength ];
		int count_cand[] = new int[ maxIndexLength ];
		int count_empty[] = new int[ maxIndexLength ];

		StopWatch equivTime = StopWatch.getWatchStopped( "Equiv Checking Time" );
		StopWatch[] candidateTimes = new StopWatch[ maxIndexLength ];
		for( int i = 0; i < maxIndexLength; i++ ) {
			candidateTimes[ i ] = StopWatch.getWatchStopped( "Cand" + i + " Time" );
		}

		long lastTokenFiltered = 0;
		for( int sid = 0; sid < tableIndexed.size(); sid++ ) {
			Record recS = tableIndexed.get( sid );
			Set<Record> candidates = new HashSet<Record>();

			// List<List<Record>> candidatesList = new ArrayList<List<Record>>();
			List<Set<IntegerPair>> available2Grams = exact2grams ? recS.getExact2Grams()
					: recS.get2GramsWithBound( maxIndexLength );

			int[] range = recS.getCandidateLengths( recS.size() - 1 );
			int boundary = Math.min( range[ 0 ], maxIndexLength );
			for( int i = 0; i < boundary; ++i ) {
				candidateTimes[ i ].start();

				// List<List<Record>> ithCandidates = new ArrayList<List<Record>>();

				Map<IntegerPair, List<IntIntRecordTriple>> map = idx.get( i );

				Set<Record> candidatesAppeared = new HashSet<Record>();

				for( IntegerPair twogram : available2Grams.get( i ) ) {

					List<IntIntRecordTriple> list = map.get( twogram );
					if( list == null ) {
						++count_empty[ i ];
						continue;
					}
					cand_sum[ i ] += list.size();
					++count_cand[ i ];
					for( IntIntRecordTriple e : list ) {
						if( StaticFunctions.overlap( e.min, e.max, range[ 0 ], range[ 1 ] ) ) {
							// length filtering
							if( i == 0 ) {
								// last token filtering
								// if( recS.shareLastToken( e.rec ) ) {
								candidatesAppeared.add( e.rec );
								// }
								// else {
								// lastTokenFiltered++;
								// }
							}
							else if( candidates.contains( e.rec ) ) {
								// signature filtering
								candidatesAppeared.add( e.rec );
							}
						}
						else {
							lengthFiltered++;
						}
					}
					cand_sum_afterprune[ i ] += candidatesAppeared.size();

				}
				candidates.clear();
				Set<Record> temp = candidatesAppeared;
				candidatesAppeared = candidates;
				candidates = temp;

				// if( i == 0 ) {
				// Iterator<Record> itr = candidates.iterator();
				// while( itr.hasNext() ) {
				// Record rec = itr.next();
				// if( !recS.shareLastToken( rec ) ) {
				// itr.remove();
				// lastTokenFiltered++;
				// }
				// }
				// }

				cand_sum_afterunion[ i ] += candidates.size();

				candidateTimes[ i ].stopQuiet();
			}

			count += candidates.size();

			// DEBUG
			// if( candidates.size() != 1 ) {
			// System.out.println( candidates.size() );
			// for( int i = 0; i < boundary; i++ ) {
			// for( IntegerPair twogram : available2Grams.get( i ) ) {
			// System.out.println( twogram.toStrString() );
			// }
			// }
			// }

			if( skipChecking ) {
				continue;
			}
			equivTime.start();
			for( Record recR : candidates ) {
				int compare = checker.isEqual( recR, recS );
				if( compare >= 0 ) {
					rslt.add( new IntegerPair( recR.getID(), recS.getID() ) );
				}
			}
			equivTime.stopQuiet();
		}
		stat.add( "Last Token Filtered", lastTokenFiltered );
		for(

				int i = 0; i < maxIndexLength; ++i ) {
			System.out.println( "Avg candidates(w/o empty) : " + cand_sum[ i ] + "/" + count_cand[ i ] );
			System.out.println( "Avg candidates(w/o empty, after prune) : " + cand_sum_afterprune[ i ] + "/" + count_cand[ i ] );
			System.out.println( "Avg candidates(w/o empty, after union) : " + cand_sum_afterunion[ i ] + "/" + count_cand[ i ] );
			System.out.println( "Empty candidates : " + count_empty[ i ] );
		}

		System.out.println( "comparisions : " + count );
		stat.add( "Equiv Comparison", count );

		stat.add( "Length Filtered", lengthFiltered );
		stat.add( equivTime );

		String candTimeStr = "";
		for( int i = 0; i < maxIndexLength; i++ ) {
			candidateTimes[ i ].printTotal();

			candTimeStr = candTimeStr + ( candidateTimes[ i ].getTotalTime() ) + " ";
		}
		stat.add( "Candidate Times Per Index", candTimeStr );
		return rslt;
	}

	public void run() {
		StopWatch stepTime = StopWatch.getWatchStarted( "Preprocess Total Time" );
		preprocess( compact, maxIndexLength, useAutomata );
		stepTime.stopAndAdd( stat );

		stepTime.resetAndStart( "BuildIndex Total Time" );
		buildIndex();
		stepTime.stopAndAdd( stat );

		stepTime.resetAndStart( "Join Total Time" );
		WYK_HashSet<IntegerPair> rslt = join();
		stepTime.stopAndAdd( stat );

		System.out.println( "Result " + rslt.size() );
		System.out.println( "Set union items:" + StaticFunctions.union_item_counter );
		System.out.println( "Set union cmps:" + StaticFunctions.union_cmp_counter );
		System.out.println( "Set inter items:" + StaticFunctions.inter_item_counter );
		System.out.println( "Set inter cmps:" + StaticFunctions.inter_cmp_counter );

		stepTime.resetAndStart( "Result Write Time" );
		writeResult( rslt );
		stepTime.stopAndAdd( stat );
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

	@Override
	public String getName() {
		return "JoinMH_QL";
	}

	@Override
	public void run( String[] args, StatContainer stat ) {
		this.stat = stat;

		Param params = Param.parseArgs( args, stat );

		maxIndexLength = params.getMaxIndex();

		// Setup parameters
		useAutomata = params.isUseACAutomata();
		skipChecking = params.isSkipChecking();
		compact = params.isCompact();
		checker = params.getValidator();
		exact2grams = params.isExact2Grams();

		StopWatch runTime = StopWatch.getWatchStarted( "Run Time" );
		run();
		runTime.stopAndAdd( stat );

		Validator.printStats();
	}
}
