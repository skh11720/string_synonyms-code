package snu.kdd.synonym.synonymRev.algorithm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.ParseException;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.data.Dataset_Split;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.index.JoinMHIndex_Split;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.MinPositionQueue;
import snu.kdd.synonym.synonymRev.tools.Param;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class JoinBK_Split extends AlgorithmTemplate {
	// RecordIDComparator idComparator;

	public JoinBK_Split( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}

	public int indexK = 3;
	public int qgramSize = 2;

	static Validator checker;

	Dataset_Split splitIndexedSet;

	/**
	 * Key: twogram<br/>
	 * Value IntervalTree Key: length of record (min, max)<br/>
	 * Value IntervalTree Value: record
	 */

	ArrayList<JoinMHIndex_Split> idxList;

	@Override
	protected void preprocess() {
		super.preprocess();

		for( Record rec : query.indexedSet.get() ) {
			rec.preprocessSuffixApplicableRules();
		}
		if( !query.selfJoin ) {
			for( Record rec : query.searchedSet.get() ) {
				rec.preprocessSuffixApplicableRules();
			}
		}

		splitIndexedSet = new Dataset_Split( query.indexedSet, query.oneSideJoin );
	}

	@Override
	public void run( Query query, String[] args ) throws IOException, ParseException {
		// System.out.println( Arrays.toString( args ) );
		Param params = Param.parseArgs( args, stat, query );

		indexK = params.indexK;
		qgramSize = params.qgramSize;

		// Setup parameters
		checker = params.validator;

		run();

		Validator.printStats();
	}

	public void run() {
		StopWatch stepTime = null;
		StopWatch runTime = null;

		stepTime = StopWatch.getWatchStarted( "Result_2_Preprocess_Total_Time" );

		preprocess();

		stat.addMemory( "Mem_2_Preprocessed" );

		stepTime.stopAndAdd( stat );
		stepTime.resetAndStart( "Result_3_1_Index_Building_Time" );

		runTime = StopWatch.getWatchStarted( "Result_3_Run_Time" );

		buildIndex();

		stat.addMemory( "Mem_3_BuildIndex" );
		stepTime.stopAndAdd( stat );
		stepTime.resetAndStart( "Result_3_2_Join_Time" );

		ArrayList<IntegerPair> rslt = join();

		stat.addMemory( "Mem_4_Joined" );
		stepTime.stopAndAdd( stat );

		runTime.stopAndAdd( stat );

		stepTime.resetAndStart( "Result_4_Write_Time" );

		writeResult( rslt );

		stepTime.stopAndAdd( stat );
	}

	private int[] estimateIndexPosition( ObjectArrayList<Record> recordList, int maxIndexLength, IntegerPair key ) {
		int minimumSize = key.i1;

		if( maxIndexLength > minimumSize ) {
			maxIndexLength = minimumSize;
		}

		int[] indexPosition = new int[ maxIndexLength ];
		int[] duplicateCount = new int[ minimumSize ];
		StopWatch estimateIndex = StopWatch.getWatchStarted( "Result_3_1_1_Index_Count_Time" );

		double[] count = new double[ minimumSize ];

		List<ObjectOpenHashSet<QGram>> qgramSetList = new ArrayList<ObjectOpenHashSet<QGram>>();
		for( int i = 0; i < minimumSize; i++ ) {
			qgramSetList.add( new ObjectOpenHashSet<QGram>() );
		}

		for( Record rec : recordList ) {
			List<List<QGram>> qgrams = rec.getQGrams( qgramSize, minimumSize + 1 );

			for( int i = 0; i < minimumSize; i++ ) {
				if( qgrams.size() <= i ) {
					break;
				}

				count[ i ]++;

				ObjectOpenHashSet<QGram> set = qgramSetList.get( i );

				for( QGram q : qgrams.get( i ) ) {
					if( set.contains( q ) ) {
						duplicateCount[ i ]++;
					}
					else {
						set.add( q );
					}
				}
			}
		}

		MinPositionQueue mpq = new MinPositionQueue( maxIndexLength );

		for( int i = 0; i < minimumSize; i++ ) {
			if( qgramSetList.get( i ).size() == 0 ) {
				// skip if there is no q-grams
				continue;
			}

			if( DEBUG.JoinBKON ) {
				Util.printLog(
						"Index " + i + " " + qgramSetList.get( i ).size() + " " + ( qgramSetList.get( i ).size() / count[ i ] )
								+ duplicateCount[ i ] + " " + ( duplicateCount[ i ] / count[ i ] ) );
			}

			double value = duplicateCount[ i ] / count[ i ];
			mpq.add( i, value );
		}

		int i = maxIndexLength - 1;
		while( !mpq.isEmpty() ) {
			indexPosition[ i ] = mpq.pollIndex();

			if( DEBUG.JoinBKON ) {
				Util.printLog( "Index " + indexPosition[ i ] + " selected" );
			}

			i--;
		}

		StringBuilder bld = new StringBuilder();
		for( i = 0; i < indexPosition.length; i++ ) {
			bld.append( indexPosition[ i ] );
			bld.append( " " );
		}

		// stat.add( "Auto_BestPosition", bld.toString() );
		// stat.add( estimateIndex );
		return indexPosition;
	}

	private void buildIndex() {
		idxList = new ArrayList<>();
		for( int i = 0; i < splitIndexedSet.keySetSize(); i++ ) {
			IntegerPair key = splitIndexedSet.getKey( i );
			ObjectArrayList<Record> recordList = splitIndexedSet.getSplitData( i );

			System.out.println( key );

			int[] indexPosition = estimateIndexPosition( recordList, indexK, key );
			JoinMHIndex_Split idx = new JoinMHIndex_Split( indexK, qgramSize, recordList, query, stat, indexPosition );
			idxList.add( idx );
		}
	}

	private ArrayList<IntegerPair> join() {
		ArrayList<IntegerPair> rslt = new ArrayList<IntegerPair>();
		for( int i = 0; i < idxList.size(); i++ ) {
			JoinMHIndex_Split idx = idxList.get( i );
			ArrayList<IntegerPair> partialRslt = idx.join( stat, query, checker );

			rslt.addAll( partialRslt );
		}

		return rslt;
	}

	@Override
	public String getVersion() {
		return "2.0";
	}

	@Override
	public String getName() {
		return "JoinBKSP";
	}

}
