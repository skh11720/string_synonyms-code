package snu.kdd.synonym.algorithm.deprecated;

import java.io.IOException;
import java.util.ArrayList;
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

@Deprecated
public class JoinMH extends AlgorithmTemplate {
	public boolean useAutomata = true;
	public boolean skipChecking = false;
	public boolean compact = false;
	public boolean exact2grams = false;
	RecordIDComparator idComparator;
	public int maxIndex = 3;
	static Validator checker;
	/**
	 * Key: twogram<br/>
	 * Value IntervalTree Key: length of record (min, max)<br/>
	 * Value IntervalTree Value: record
	 */
	List<Map<IntegerPair, List<IntIntRecordTriple>>> idx;

	public JoinMH( String rulefile, String Rfile, String Sfile, String outFile, DataInfo dataInfo ) throws IOException {
		super( rulefile, Rfile, Sfile, outFile, dataInfo );
		idComparator = new RecordIDComparator();
	}

	private void buildIndex() {
		try {
			long elements = 0;
			// Build an index

			idx = new ArrayList<Map<IntegerPair, List<IntIntRecordTriple>>>();
			for( int i = 0; i < maxIndex; ++i )
				idx.add( new WYK_HashMap<IntegerPair, List<IntIntRecordTriple>>() );
			for( Record rec : tableSearched ) {
				List<Set<IntegerPair>> available2Grams = exact2grams ? rec.getExact2Grams() : rec.get2Grams();
				int[] range = rec.getCandidateLengths( rec.size() - 1 );
				int boundary = Math.min( range[ 1 ], maxIndex );
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
			System.out.println( "Idx size : " + elements );

			for( int i = 0; i < maxIndex; ++i ) {
				Map<IntegerPair, List<IntIntRecordTriple>> ithidx = idx.get( i );
				System.out.println( i + "th iIdx key-value pairs: " + ithidx.size() );
				// Statistics
				int sum = 0;
				int singlelistsize = 0;
				long count = 0;
				long sqsum = 0;
				for( List<IntIntRecordTriple> list : ithidx.values() ) {
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
			}
		}
		catch( Exception e ) {
			e.printStackTrace();
			System.exit( 1 );
		}
	}

	private WYK_HashSet<IntegerPair> join() {
		WYK_HashSet<IntegerPair> rslt = new WYK_HashSet<IntegerPair>();
		long count = 0;

		long cand_sum[] = new long[ maxIndex ];
		long cand_sum_afterprune[] = new long[ maxIndex ];
		long cand_sum_afterunion[] = new long[ maxIndex ];
		int count_cand[] = new int[ maxIndex ];
		int count_empty[] = new int[ maxIndex ];
		for( Record recS : tableIndexed ) {
			List<List<Record>> candidatesList = new ArrayList<List<Record>>();
			List<Set<IntegerPair>> available2Grams = exact2grams ? recS.getExact2Grams() : recS.get2Grams();

			int[] range = recS.getCandidateLengths( recS.size() - 1 );
			int boundary = Math.min( range[ 0 ], maxIndex );
			for( int i = 0; i < boundary; ++i ) {
				List<List<Record>> ithCandidates = new ArrayList<List<Record>>();
				Map<IntegerPair, List<IntIntRecordTriple>> map = idx.get( i );
				for( IntegerPair twogram : available2Grams.get( i ) ) {
					List<IntIntRecordTriple> tree = map.get( twogram );
					if( tree == null ) {
						++count_empty[ i ];
						continue;
					}
					cand_sum[ i ] += tree.size();
					++count_cand[ i ];
					List<Record> list = new ArrayList<Record>();
					for( IntIntRecordTriple e : tree )
						if( StaticFunctions.overlap( e.min, e.max, range[ 0 ], range[ 1 ] ) )
							list.add( e.rec );
					ithCandidates.add( list );
					cand_sum_afterprune[ i ] += list.size();
				}
				List<Record> union = StaticFunctions.union( ithCandidates, idComparator );
				candidatesList.add( union );
				cand_sum_afterunion[ i ] += union.size();
			}
			List<Record> candidates = StaticFunctions.intersection( candidatesList, idComparator );
			count += candidates.size();

			if( skipChecking ) {
				continue;
			}
			for( Record recR : candidates ) {
				int compare = checker.isEqual( recR, recS );
				if( compare >= 0 ) {
					rslt.add( new IntegerPair( recR.getID(), recS.getID() ) );
				}
			}
		}
		for( int i = 0; i < maxIndex; ++i ) {
			System.out.println( "Avg candidates(w/o empty) : " + cand_sum[ i ] + "/" + count_cand[ i ] );
			System.out.println( "Avg candidates(w/o empty, after prune) : " + cand_sum_afterprune[ i ] + "/" + count_cand[ i ] );
			System.out.println( "Avg candidates(w/o empty, after union) : " + cand_sum_afterunion[ i ] + "/" + count_cand[ i ] );
			System.out.println( "Empty candidates : " + count_empty[ i ] );
		}
		stat.add( "Equiv Comparison", count );
		System.out.println( "comparisions : " + count );

		return rslt;
	}

	public void run() {
		StopWatch stepTime = StopWatch.getWatchStarted( "Preprocess Total Time" );
		preprocess( compact, maxIndex, useAutomata );
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
		return "JoinMH";
	}

	@Override
	public void run( String[] args, StatContainer stat ) {
		this.stat = stat;

		Param params = Param.parseArgs( args, stat );

		maxIndex = params.getMaxIndex();

		// Setup parameters
		useAutomata = params.isUseACAutomata();
		skipChecking = params.isSkipChecking();
		compact = params.isCompact();
		checker = params.getValidator();
		// exact2grams = params.isExact2Grams();

		run();

		Validator.printStats();
	}
}
