package snu.kdd.synonym.synonymRev.algorithm.delta;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.synonymRev.algorithm.misc.EstimationTest;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.index.JoinMinIndexInterface;
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

public class JoinMinDeltaIndex implements JoinMinIndexInterface {
	protected ArrayList<ArrayList<WYK_HashMap<QGram, List<Record>>>> index;
	// key: pos -> delta -> qgram -> recordList
	protected ArrayList<Integer> countPerPosition = null;
	protected Object2IntOpenHashMap<Record> indexedCountMap;
	protected Object2IntOpenHashMap<Record> estimatedCountMap;
	protected WYK_HashSet<Integer> bypassSet = null;
	protected Query query;
	protected final QGramDeltaGenerator qdgen;

	protected double gamma;
	protected double delta;
	protected double epsilon;
	protected double epsilonPrime;

	protected int qgramSize;

	protected long searchedTotalSigCount; // number of qgramDelta from TPQ supersets of S
	protected long indexedTotalSigCount;
	protected long appliedRulesSum;

	protected long comparisonCount; // number of varifications...?

	protected double indexCountTime= 0; // time for counting records in S who have a pos qgram when building the index.
	protected double indexTime = 0; // time for inserting records in T into the index.
	protected double joinTime = 0;
	protected long candQGramCountTime= 0; // time for counting candidate qgrams in join
	protected long filterTime = 0; // time for filtering
	protected long equivTime = 0; // time for validation

	protected long candQGramCount = 0;
	protected long emptyListCount = 0;
	protected long predictCount = 0;
	protected long equivComparisons = 0;
	protected final int deltaMax;

	public JoinMinDeltaIndex( int nIndex, int qSize, int deltaMax, StatContainer stat, Query query, int threshold, boolean writeResult ) {
		// TODO: Need to be fixed to make index just for given sequences
		// NOW, it makes index for all sequences
		
		this.index = new ArrayList<ArrayList<WYK_HashMap<QGram, List<Record>>>>();
		this.qgramSize = qSize;
		this.query = query;
		this.deltaMax =deltaMax;
		qdgen = new QGramDeltaGenerator( qSize, deltaMax );

		boolean hybridIndex = threshold != 0;

		if( DEBUG.JoinMinIndexON ) {
			this.countPerPosition = new ArrayList<Integer>();
		}

		long starttime = System.nanoTime();

		// Build an index
		// Count Invokes per each (token, loc, delta) pair
		// key: pos -> delta -> qgram
		List<ArrayList<Object2IntOpenHashMap<QGram>>> invokes = new ArrayList<ArrayList<Object2IntOpenHashMap<QGram>>>();
		List<ArrayList<Object2IntOpenHashMap<QGram>>> lowInvokes = null;
		if( hybridIndex ) {
			if( !query.oneSideJoin ) {
				// we do not have to compute the lowInvokes for the oneSideJoin
				lowInvokes = new ArrayList<ArrayList<Object2IntOpenHashMap<QGram>>>();
			}
		}

		long indexGetQGramTime = 0;
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

//			if( DEBUG.JoinMinON ) {
			recordStartTime = System.nanoTime();
//			}

			// NOTE; cannot use this.getCandidatePQGrams() since index is not completely built yet.
			List<List<QGram>> availableQGrams = rec.getQGrams( qSize+deltaMax );

//			if( DEBUG.JoinMinON ) {
			recordMidTime = System.nanoTime();
			indexGetQGramTime += recordMidTime - recordStartTime;
//			}

			int searchmax = availableQGrams.size();

			// create a counter for each position
			for( int i = invokes.size(); i < searchmax; i++ ) {
				ArrayList<Object2IntOpenHashMap<QGram>> invokes_delta = new ArrayList<Object2IntOpenHashMap<QGram>>();
				invokes.add( invokes_delta );
				for ( int delta_s=0; delta_s<=deltaMax; ++delta_s ) {
					Object2IntOpenHashMap<QGram> inv = new Object2IntOpenHashMap<QGram>();
					inv.defaultReturnValue( 0 );
					invokes_delta.add( inv );
				}

				if( lowInvokes != null ) { // for hybrid and both side only
					ArrayList<Object2IntOpenHashMap<QGram>> lowInvokes_delta = new ArrayList<Object2IntOpenHashMap<QGram>>();
					lowInvokes.add( lowInvokes_delta );
					Object2IntOpenHashMap<QGram> invLow = new Object2IntOpenHashMap<QGram>();
					invLow.defaultReturnValue( 0 );
					lowInvokes_delta.add( invLow );
				}

				if( DEBUG.JoinMinIndexON ) {
					this.countPerPosition.add( 0 );
				}
			}

			boolean isLowRecord = hybridIndex && ( rec.getEstNumTransformed() <= threshold );

			long qgramCount = 0;
			for( int i = 0; i < searchmax; ++i ) {
				ArrayList<Object2IntOpenHashMap<QGram>> curridx_invokes_delta = invokes.get( i );
//				for ( int delta_s=0; delta_s<=deltaMax; ++delta_s ) {
//					Object2IntOpenHashMap<QGram> curridx_invokes = null;
//					if( !isLowRecord ) {
//						// it is not the hybrid index or not low record for the hybrid index
//						curridx_invokes = invokes.get( delta_s ).get( i );
//					}
//					else {
//						if( query.oneSideJoin ) {
//							// if uni-directional join && isLowRecord, this record will not be compared with joinMin index
//							break;
//						}
//						curridx_invokes = lowInvokes.get( delta_s ).get( i );
//					}

				List<QGram> qgramList = availableQGrams.get( i );
				for( QGram qgram : qgramList ) {
//					curridx_invokes.addTo( qgram, 1 );
					for ( Entry<QGram, Integer> entry : qdgen.getQGramDelta( qgram ) ) {
						QGram qgramDelta = entry.getKey();
						int delta = entry.getValue();
						curridx_invokes_delta.get( delta ).addTo( qgramDelta, 1 );

						if( DEBUG.JoinMinIndexON ) {
							this.countPerPosition.add( i, 1 );
						}
						++qgramCount;
					}
				}
//				}
			} // end for i from 0 to searchmax
			this.searchedTotalSigCount += qgramCount;

//			if( DEBUG.JoinMinON ) {
			countIndexingTime += System.nanoTime() - recordMidTime;
//			}

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
		} // end for rec in query.serachedSet

		stat.add( "Stat_IndexGetQGramTime", (long)(indexGetQGramTime/1e6) ); // time for enumerating pos qgrams of size (q+deltaMax)
		stat.add( "Stat_CountIndexingTime", (long)(countIndexingTime/1e6) ); // time for occurrences of every pos qgramDelta


		int distinctPQgram = 0;
		for( int i = 0; i < invokes.size(); i++ ) {
			ArrayList<Object2IntOpenHashMap<QGram>> invokes_delta = invokes.get( i );
			for ( int delta=0; delta<=deltaMax; ++delta ) {
				Object2IntOpenHashMap<QGram> count = invokes_delta.get( delta );
				distinctPQgram += count.size();
			}
		}
		stat.add( "Stat_Distinct_PQ", distinctPQgram ); // number of pos qgramDelta after counting with S.

		if( DEBUG.JoinMinIndexON ) {
			try {
				for( int i = 0; i < invokes.size(); i++ ) {
					ArrayList<Object2IntOpenHashMap<QGram>> invokes_delta = invokes.get( i );
					for ( int delta=0; delta<=deltaMax; ++delta ) {
						Object2IntOpenHashMap<QGram> count = invokes_delta.get( delta );
						ObjectIterator<Object2IntMap.Entry<QGram>> iter = count.object2IntEntrySet().iterator();
						while( iter.hasNext() ) {
							Object2IntMap.Entry<QGram> entry = iter.next();

							QGram q = entry.getKey();
							int c = entry.getIntValue();
							bw.write( "Inv: " + q + " " + i + " " + delta + " => " + c + "\n" );
						}
					}
				}
				bw.close();
			}
			catch( IOException e ) {
				e.printStackTrace();
			}
		}
		// we have the number of occurrence of a positional q-grams

		this.indexCountTime = System.nanoTime() - starttime;

		if( DEBUG.JoinMinON ) {
			Util.printLog( "Step 1 Time : " + this.indexCountTime );
			Util.printLog( "Gamma (buildTime / signature): " + this.gamma );

			if( writeResult ) {
				stat.add( "Est_Index_0_GetQGramTime", indexGetQGramTime );
				stat.add( "Est_Index_0_CountIndexingTime", countIndexingTime );

				stat.add( "Est_Index_1_Index_Count_Time", this.indexCountTime );
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
				System.out.println( "[Gamma] CountTime " + indexCountTime );
				System.out.println( "[Gamma] SearchedSigCount " + searchedTotalSigCount );
			}
			if( DEBUG.PrintEstimationON ) {
				BufferedWriter bwEstimation = EstimationTest.getWriter();
				try {
					bwEstimation.write( "[Gamma] " + gamma );
					bwEstimation.write( " CountTime " + indexCountTime );
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
		long indexedRecordCount = 0;

		// find best K positions for each string in T
		indexedCountMap = new Object2IntOpenHashMap<>();
		estimatedCountMap = new Object2IntOpenHashMap<>();
		for( Record rec : query.targetIndexedSet.get() ) {
			int[] range = rec.getTransLengths();

//			int searchmax = Math.min( Math.max( range[0]-deltaMax, 1), invokes.size() );
			int searchmax;
			if ( query.oneSideJoin ) searchmax = Math.min( Math.max( rec.size()-deltaMax, 1), invokes.size() );
			else throw new RuntimeException("UNIMPLEMENTED");

			List<List<QGram>> availableQGrams = null;

			boolean isLowRecord = hybridIndex && ( rec.getEstNumTransformed() <= threshold );

			if( query.oneSideJoin ) {
				availableQGrams = rec.getSelfQGrams( qSize+deltaMax, searchmax );
				// System.out.println( availableQGrams.toString() );
			}
			else {
				throw new RuntimeException("UNIMPLEMENTED");
//				availableQGrams = rec.getQGrams( qSize+deltaMax, searchmax );
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

				ArrayList<Object2IntOpenHashMap<QGram>> curridx_invokes_delta = invokes.get( i );
				ArrayList<Object2IntOpenHashMap<QGram>> curridx_lowInvokes_delta = null;
				if( !query.oneSideJoin && hybridIndex ) {
					throw new RuntimeException("UNIMPLEMENTED");
//					curridx_lowInvokes_delta = lowInvokes.get( i );
				}
				int invoke = 0;

				for( QGram qgram : availableQGrams.get( i ) ) {
					/*
					 * Since only oneSideJoin is supported currently, 
					 * the number of qgrams for each position i is 1.
					 * the qgram comes from a record in the indexedSet.
					 */
					for ( Entry<QGram, Integer> entry : qdgen.getQGramDelta( qgram ) ) {
						QGram qgramDelta = entry.getKey();
						int delta_t = entry.getValue();
						for ( int delta_s=0; delta_s<=deltaMax-delta_t; ++delta_s ) {
							invoke += curridx_invokes_delta.get( delta_s ).getInt( qgramDelta );
						}

						if( !query.oneSideJoin && hybridIndex && !isLowRecord ) {
							throw new RuntimeException("UNIMPLEMENTED");
//							count += curridx_lowInvokes_delta.get( delta ).getInt( qgramDelta );
						}
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
			} // end for i from 0 to searchmax

			this.predictCount += mpq.minInvokes; // predictCount is the sum of minimum invokes ...

			int indexedCount = 0;
			while( !mpq.isEmpty() ) { // NOTE: the size of mpq is at most nIndex.
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
					for ( Entry<QGram, Integer> entry : qdgen.getQGramDelta( qgram ) ) {
						QGram qgramDelta = entry.getKey();
						int delta_t = entry.getValue();

						if( DEBUG.PrintJoinMinIndexON ) {
							try {
								bw_index.write( minIdx + ", " + delta_t + ", " + qgramDelta + " : " + rec + "\n" );
							}
							catch( IOException e ) {
								e.printStackTrace();
							}
						}
						
						this.put( minIdx, delta_t, qgramDelta, rec );
					}
				}
			} // end while mpq.isEmpty

			indexedCountMap.put( rec, indexedCount );
			++indexedRecordCount;
		} // end for rec in query.targetIndexedSet

		this.indexTime = System.nanoTime() - starttime;
		this.delta = this.indexTime / this.indexedTotalSigCount;
		
		stat.add("Stat_IndexCountTime", (long)(indexCountTime/1e6) );
		stat.add("Stat_IndexTime", (long)(indexTime/1e6) );
		stat.add("Stat_IndexedRecordCount", indexedRecordCount );
		stat.add("Stat_InvList_Count", nInvList());
		stat.add("Stat_InvSize", indexSize());

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
			Util.printLog( "Idx size : " + indexedRecordCount );
			Util.printLog( "Predict : " + this.predictCount );
			Util.printLog( "Step 2 Time : " + this.indexTime );
			Util.printLog( "Delta (index build / signature ): " + this.delta );

			if( writeResult ) {
				stat.add( "Stat_JoinMin_Index_Size", indexedRecordCount );
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
			if( DEBUG.JoinMinIndexON ) {
				for( int i = 0; i < countPerPosition.size(); i++ ) {
					stat.add( String.format( "Stat_JoinMin_COUNT%02d", i ), countPerPosition.get( i ) );
				}

				for( int i = 0; i < index.size(); i++ ) {
					if( index.get( i ).size() != 0 ) {
						stat.add( String.format( "Stat_JoinMin_IDX%02d", i ), index.get( i ).size() );
					}
				}
			}
			stat.add( "Counter_Index_1_HashCollision", WYK_HashSet.collision );
			stat.add( "Counter_Index_1_HashResize", WYK_HashSet.resize );
			Util.printGCStats( stat, "Stat_Index" );
		}
		
		for ( ArrayList<Object2IntOpenHashMap<QGram>> invokes_delta : invokes ) {
			for( Object2IntOpenHashMap<QGram> in : invokes_delta ) {
				in.clear();
			}
		}
	} // end constructor

	public void setIndex( int position ) {
		while( index.size() <= position ) {
			index.add( new ArrayList<WYK_HashMap<QGram, List<Record>>>() );
		}
	}

	public void put( int position, int delta, QGram qgram, Record rec ) {
		ArrayList<WYK_HashMap<QGram, List<Record>>> index_pos = index.get( position );
		while ( index_pos.size() <= delta ) index_pos.add( new WYK_HashMap<QGram, List<Record>>() );
		WYK_HashMap<QGram, List<Record>> map = index_pos.get( delta );

		List<Record> list = map.get( qgram );
		if( list == null ) {
			list = new ArrayList<Record>();
			map.put( qgram, list );
		}

		list.add( rec );
	}

	public Set<IntegerPair> joinMaxK( int indexK, boolean writeResult, StatContainer stat, Validator checker, Query query ) {
		BufferedWriter bw = null;

		if( DEBUG.PrintJoinMinJoinON ) {
			try {
				bw = new BufferedWriter( new FileWriter( "JoinMin_Join_Debug.txt" ) );
			}
			catch( Exception e ) {
				e.printStackTrace();
			}
		}

		long joinStartTime = System.nanoTime();
		Set<IntegerPair> rslt = new ObjectOpenHashSet<IntegerPair>();

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
		epsilon = ( joinTime - candQGramCountTime ) / predictCount;
		this.gamma = ( this.indexCountTime + candQGramCountTime ) / this.searchedTotalSigCount;

		// DEBUG
		epsilonPrime = joinTime / comparisonCount;

		if( DEBUG.JoinMinON ) {
			Util.printLog( "Est weight : " + comparisonCount );
			Util.printLog( "Join time : " + joinTime );
			Util.printLog( "Epsilon : " + epsilon );

			if( writeResult ) {
				// stat.add( "Last Token Filtered", lastTokenFiltered );
				stat.add( "Est_Join_0_GetQGramTime", candQGramCountTime );

				stat.add( "Result_3_2_1_Equiv_Checking_Time", equivTime / 1000000 );
			}
		}

		if( writeResult ) {
			stat.add( "Join_Min_Result", rslt.size() );
			addStat( stat );
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

	private List<List<Set<QGram>>> getCandidatePQGrams( Record rec ) {
		/*
		 * Return the lists of qgrams, where each list is indexed by pos and delta.
		 * key: pos -> delta 
		 */
		boolean debug = false;
		List<List<QGram>> availableQGrams = rec.getQGrams( qgramSize+deltaMax );
		if (debug) {
			System.out.println( "availableQGrams:" );
			for ( List<QGram> qgramList : availableQGrams ) {
				for ( QGram qgram : qgramList ) System.out.println( qgram );
			}
		}
		List< List<Set<QGram>>> candidatePQGrams = new ArrayList<List<Set<QGram>>>();
		for ( int k=0; k<availableQGrams.size(); ++k ) {
			if ( k >= index.size() ) continue;
			List<WYK_HashMap<QGram, List<Record>>> curidx = index.get( k );
			List<Set<QGram>> cand_pos = new ArrayList<Set<QGram>>();
			for ( int d=0; d<=deltaMax; ++d ) cand_pos.add( new ObjectOpenHashSet<QGram>() );
			for ( QGram qgram : availableQGrams.get( k ) ) {
//			List<QGram> qgrams = new ArrayList<QGram>();
				if (debug) System.out.println( "qgram: "+qgram );
				if (debug) System.out.println( "qgramDelta: "+qdgen.getQGramDelta( qgram ) );
				for ( Entry<QGram, Integer> entry: qdgen.getQGramDelta( qgram ) ) {
					QGram qgramDelta = entry.getKey();
					int delta_s = entry.getValue();
					for ( int delta_t=0; delta_t<=deltaMax-delta_s; ++delta_t ) {
						if ( curidx.size() <= delta_t ) break;
						if ( curidx.get( delta_t ).containsKey( qgramDelta ) ) {
							if (debug) System.out.println( "cand_pos.get("+delta_s+").add: "+qgramDelta );
							cand_pos.get( delta_s ).add( qgramDelta );
							break;
						}
					}
				} // end for (qgramDelta, delta)
//				qgrams.add( qgram );
			} // end for qgram in availableQGrams
			candidatePQGrams.add( cand_pos );
		} // end for k
		return candidatePQGrams;
//		return rec.getQGrams( qgramSize, maxPosition+1 );
	}

	public void joinRecordMaxK( int nIndex, Record recS, Set<IntegerPair> rslt, boolean writeResult, BufferedWriter bw,
		Validator checker, boolean oneSideJoin ) {
		joinRecordMaxKThres( nIndex, recS, rslt, writeResult, bw, checker, -1, oneSideJoin );
	}

	public void joinRecordMaxKThres( int nIndex, Record recS, Set<IntegerPair> rslt, boolean writeResult, BufferedWriter bw,
		Validator checker, int threshold, boolean oneSideJoin ) {
		long joinStartTime = System.nanoTime();
//			boolean debug = false;
//			if ( recS.getID() == 15756 ) debug = true;

		boolean isUpperRecord = recS.getEstNumTransformed() > threshold;

		Histogram hist = null;
		if( DEBUG.JoinMinON ) {
			hist = new Histogram( "Validation" );
		}

		// pos -> delta
		List<List<Set<QGram>>> candidateQGrams = getCandidatePQGrams( recS );
		long afterCandQGramTime = System.nanoTime();

		int[] range = recS.getTransLengths();
		int searchmax = Integer.min( candidateQGrams.size(), index.size() );
//			ArrayList<String> debugArray = new ArrayList<String>();
		if( DEBUG.PrintJoinMinJoinON ) {
			joinStartTime = System.nanoTime();
		}

		JoinMinCandidateSet allCandidateSet = new JoinMinCandidateSet( nIndex, recS, estimatedCountMap.getInt( recS ) );

		for( int i = 0; i < searchmax; ++i ) {
			List<Set<QGram>> cand_qgrams_pos = candidateQGrams.get( i );
			List<WYK_HashMap<QGram, List<Record>>> curridx_pos = index.get( i );
			if( curridx_pos == null ) continue;

			// Given a position i
			WYK_HashSet<Record> candidates = new WYK_HashSet<Record>();
			
			for ( int delta_s=0; delta_s<=deltaMax; ++delta_s ) {
				if ( cand_qgrams_pos.size() <= delta_s ) break;
				candQGramCount += cand_qgrams_pos.get( delta_s ).size();
				for ( QGram qgram : cand_qgrams_pos.get( delta_s ) ) {
					if ( !isInTPQ( qgram, i, delta_s ) ) continue;
					for ( int delta_t=0; delta_t<=deltaMax-delta_s; ++delta_t ) {
//						if( DEBUG.PrintJoinMinJoinON ) {
//								debugArray.add( "q :" + qgram + " " + i + " " + delta_s + " " + delta_t + "\n" );
//						}

						List<Record> recordList = curridx_pos.get( delta_t ).get( qgram );
		//				if ( debug ) System.out.println( "i: "+i+", qgram: "+qgram+", tree: "+tree );
						if( recordList == null ) {
	                    	++emptyListCount;
							continue;
						}

						for( Record e : recordList ) {
							// length filtering
							if( !isUpperRecord && e.getEstNumTransformed() <= threshold ) {
								// this record will not compared by joinmin index.
								// this will be compared by joinnaive index
								continue;
							}
							if( oneSideJoin ) {
								if( StaticFunctions.overlap( e.getTokenCount()-deltaMax, e.getTokenCount(), range[ 0 ]-deltaMax, range[ 1 ] ) ) {
//										if( DEBUG.PrintJoinMinJoinON ) {
//											debugArray.add( "Cand: " + e + " by " + qgram + " at " + i + "\n" );
//										}
									candidates.add( e );
									comparisonCount++;
								}
								else ++checker.lengthFiltered;
							}
							else {
								throw new RuntimeException("UNIMPLEMENTED");
//									if( StaticFunctions.overlap( e.getMinTransLength(), e.getMaxTransLength(), range[ 0 ], range[ 1 ] ) ) {
//										candidates.add( e );
//										comparisonCount++;
//									}
//									else {
//										++checker.lengthFiltered;
//									}
							}
						} // end for Record e in otherRecordList
					} // end for delta_t
				} // end for qgram
			} // end for delta_s
			allCandidateSet.add( candidates );
		} // end for i from 0 to searchmax

//			ArrayList<Record> candSet = allCandidateSet.getCandSet( indexedCountMap, debugArray );
		ArrayList<Record> candSet = allCandidateSet.getCandSet( indexedCountMap, null );
		checker.pqgramFiltered += allCandidateSet.pqgramFiltered;
		long afterFilterTime = System.nanoTime();
//			if ( debug) System.out.println( "candSet: "+candSet );

		equivComparisons += candSet.size();
		if( DEBUG.JoinMinON ) {
			hist.add( candSet.size() );
		}
		for( Record recR : candSet ) {
//				if( DEBUG.PrintJoinMinJoinON ) {
//					debugArray.add( "Test " + recR + "\n" );
//				}
			int compare = checker.isEqual( recS, recR );
			if( compare >= 0 ) {
//					rslt.add( new IntegerPair( recS.getID(), recR.getID() ) );
				AlgorithmTemplate.addSeqResult( recS, recR, rslt, query.selfJoin );
				appliedRulesSum += compare;

//					if( DEBUG.PrintJoinMinJoinON ) {
//						debugArray.add( "Val " + recS + " : " + recR + "\n" );
//					}
			}
		}
		long afterEquivTime = System.nanoTime();

		if( DEBUG.PrintJoinMinJoinON ) {
			long joinTime = System.nanoTime() - joinStartTime;

//				try {
//					Collections.sort( debugArray );
//					for( String temp : debugArray ) {
//						bw.write( temp );
//					}
//					bw.write( " " + joinTime );
//					bw.write( "\n" );
//				}
//				catch( IOException e ) {
//					e.printStackTrace();
//				}
		}

		candQGramCountTime += afterCandQGramTime - joinStartTime;
		filterTime += afterFilterTime - afterCandQGramTime;
		equivTime += afterEquivTime - afterFilterTime;
		if( DEBUG.JoinMinON ) {
			hist.print();
			hist.printLogHistogram();
		}
	} // end joinRecordMaxK

	public void clear() {
		for( int i = 0; i < index.size(); i++ ) {
			index.get( i ).clear();
		}
		index.clear();
	}

	public void DebugWriteToFile( String filename ) {
		try {
			BufferedWriter bw = new BufferedWriter( new FileWriter( filename ) );

			for( int i = 0; i < index.size(); i++ ) {
//				bw.write( i + "-th index\n" );
				ArrayList<WYK_HashMap<QGram, List<Record>>> map_pos = index.get( i );
				
				for ( int d=0; d<map_pos.size(); ++d ) {
//					bw.write( "delta: "+d+"\n" );
					WYK_HashMap<QGram, List<Record>> map_pos_delta = map_pos.get( d );
					for( Entry<QGram, List<Record>> entry : map_pos_delta.entrySet() ) {
//						bw.write( entry.getKey().toString() );
						List<Record> list = entry.getValue();
						for( int idx = 0; idx < list.size(); idx++ ) {
							bw.write( i+"\t"+d+"\t"+entry.getKey()+"\t"+list.get( idx ).getID()+"\n" );
						}
					}
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

	public class JoinMinCandidateSet {
		int nIndex;
		int pqgramFiltered = 0;

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
				else ++pqgramFiltered;
			}

			return list;
		}
	}
	
	public void addStat( StatContainer stat ) {
		stat.add("Stat_CandQGramCount", candQGramCount );
		stat.add("Stat_CandQGramCountTime", (long)(candQGramCountTime/1e6));
		stat.add("Stat_FilterTime", (long)(filterTime/1e6));
		stat.add("Stat_EmtpyListCount", emptyListCount);
		stat.add("Stat_EquivComparison", equivComparisons);
		stat.add("Stat_EquivTime", (long)(equivTime/1e6) );
	}
	
	// will be overridden by subclasses
	protected boolean isInTPQ( QGram qgram, int k, int delta ) {
		return true;
	}

	public int nInvList() {
		int n = 0;
		for (int i=0; i<index.size(); i++) {
			ArrayList<WYK_HashMap<QGram, List<Record>>> index_pos = index.get( i );
			for ( WYK_HashMap<QGram, List<Record>> index_pos_delta : index_pos ) {
				n += index_pos_delta.size();
			}
		}
		return n;
	}

	public int indexSize() {
		int n = 0;
		for (int i=0; i<index.size(); i++) {
			ArrayList<WYK_HashMap<QGram, List<Record>>> index_pos = index.get( i );
			for ( WYK_HashMap<QGram, List<Record>> index_pos_delta : index_pos ) {
				for ( Entry<QGram, List<Record>> entry : index_pos_delta.entrySet() ) {
					n += entry.getValue().size();
				}
			}
		}
		return n;
	}

	public double getGamma() { return gamma; }
	public double getDelta() { return delta; }
	public double getEpsilon() { return epsilon; }
	public double getEpsilonPrime() { return epsilonPrime; }
	public long getSearchedTotalSigCount(){ return searchedTotalSigCount; }
	public long getIndexedTotalSigCount() { return indexedTotalSigCount; }
	public long getEquivComparisons() { return equivComparisons; }
	public long getComparisonTime() { return equivTime; }
	public long getAppliedRulesSum() { return appliedRulesSum; }
	public long getComparisonCount() { return comparisonCount; }
	public double getIndexTime() { return indexTime; }
	public double getCountTime() { return indexCountTime; }
	public long getPredictCount() { return predictCount; }
}
