package snu.kdd.synonym.synonymRev.algorithm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.ParseException;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.index.JoinMHIndex;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.MinPositionPairQueue;
import snu.kdd.synonym.synonymRev.tools.Param;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class JoinBK extends AlgorithmTemplate {
	// RecordIDComparator idComparator;

	public JoinBK( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}

	public int indexK = 3;
	public int qgramSize = 2;

	static Validator checker;

	/**
	 * Key: twogram<br/>
	 * Value IntervalTree Key: length of record (min, max)<br/>
	 * Value IntervalTree Value: record
	 */

	JoinMHIndex idx;
	int indexedMinLength = Integer.MAX_VALUE;

	@Override
	protected void preprocess() {
		super.preprocess();

		for( Record rec : query.indexedSet.get() ) {
			rec.preprocessSuffixApplicableRules();
			if( indexedMinLength > rec.getMinTransLength() ) {
				indexedMinLength = rec.getMinTransLength();
			}
		}
		if( !query.selfJoin ) {
			for( Record rec : query.searchedSet.get() ) {
				rec.preprocessSuffixApplicableRules();
			}
		}
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

		checker.addStat( stat );
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

		rslt = idx.join( stat, query, checker, true );

		stat.addMemory( "Mem_4_Joined" );
		stepTime.stopAndAdd( stat );

		runTime.stopAndAdd( stat );

		stepTime.resetAndStart( "Result_4_Write_Time" );

		writeResult();

		stepTime.stopAndAdd( stat );
	}

	private int[] estimateIndexPosition( int maxIndexLength ) {
		int[] indexPosition = new int[ maxIndexLength ];

		StopWatch estimateIndex = StopWatch.getWatchStarted( "Result_3_1_1_Index_Count_Time" );
		int minAmongValidIndex = -1;
		double minAmongValidValue = Double.MAX_VALUE;

		int minimumSize = 20;
		double[] count = new double[ minimumSize ];
		int[] duplicateCount = new int[ minimumSize ];

		List<ObjectOpenHashSet<QGram>> qgramSetList = new ArrayList<ObjectOpenHashSet<QGram>>();
		for( int i = 0; i < minimumSize; i++ ) {
			qgramSetList.add( new ObjectOpenHashSet<QGram>() );
		}

		for( Record rec : query.searchedSet.get() ) {
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

		MinPositionPairQueue mpq = new MinPositionPairQueue( maxIndexLength );

		for( int i = 0; i < minimumSize; i++ ) {
			if( DEBUG.JoinBKON ) {
				Util.printLog(
						"Index " + i + " " + qgramSetList.get( i ).size() + " " + ( qgramSetList.get( i ).size() / count[ i ] )
								+ duplicateCount[ i ] + " " + ( duplicateCount[ i ] / count[ i ] ) );
			}
			double overlapValue = duplicateCount[ i ] / count[ i ];
			double candidateValue = qgramSetList.get( i ).size() / count[ i ];
			mpq.add( i, overlapValue, candidateValue );

			if( i < indexedMinLength ) {
				if( minAmongValidValue > overlapValue ) {
					minAmongValidIndex = i;
					minAmongValidValue = overlapValue;
				}
			}
		}

		int i = maxIndexLength - 1;

		while( !mpq.isEmpty() ) {
			indexPosition[ i ] = mpq.pollIndex();

			if( DEBUG.JoinBKON ) {
				Util.printLog( "Selected " + indexPosition[ i ] );
			}

			i--;
		}

		boolean validPositions = false;
		for( i = 0; i < indexPosition.length; i++ ) {
			int actualIndex = indexPosition[ i ];

			if( indexedMinLength > actualIndex ) {
				validPositions = true;
			}
		}

		if( !validPositions ) {
			// to join short strings correctly
			if( DEBUG.JoinBKON ) {
				Util.printLog( "Replace " + indexPosition[ maxIndexLength - 1 ] + " with " + minAmongValidIndex );
			}
			indexPosition[ maxIndexLength - 1 ] = minAmongValidIndex;
		}

		StringBuilder bld = new StringBuilder();
		for( i = 0; i < indexPosition.length; i++ ) {
			bld.append( indexPosition[ i ] );
			bld.append( " " );
		}
		estimateIndex.stopAndAdd( stat );
		stat.add( "Auto_BestPosition", "\"" + bld.toString() + "\"" );
		return indexPosition;
	}

	private void buildIndex() {
		int[] indexPosition = estimateIndexPosition( indexK );
		idx = new JoinMHIndex( indexK, qgramSize, query.indexedSet.get(), query, stat, indexPosition, true, true, 0 );
	}

	@Override
	public String getVersion() {
		return "2.3";
	}

	@Override
	public String getName() {
		return "JoinBK";
	}

}
