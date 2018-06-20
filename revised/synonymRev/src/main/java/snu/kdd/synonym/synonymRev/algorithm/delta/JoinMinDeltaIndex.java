package snu.kdd.synonym.synonymRev.algorithm.delta;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
	protected ArrayList<ArrayList<WYK_HashMap<QGram, List<Record>>>> indexMap;
	// key: pos -> delta -> qgram -> recordList
	protected ArrayList<Integer> countPerPosition = null;
	protected Object2IntOpenHashMap<Record> indexedCountMap;
	protected Object2IntOpenHashMap<Record> estimatedCountMap;
	protected WYK_HashSet<Integer> bypassSet = null;
	protected Query query;
	private final QGramDeltaGenerator qdgen;

	public double gamma;
	public double delta;
	public double epsilon;
	public double epsilonPrime;

	protected int qgramSize;

	public long searchedTotalSigCount;
	public long indexedTotalSigCount;
	public long equivComparisons;
	public long comparisonTime;
	public long appliedRulesSum;

	double getQGramTime;
	public long comparisonCount;
	public long lengthFiltered;
	protected long nCandQGrams;

	public double joinTime;
	public double indexTime;
	public double countTime;

	public long predictCount;
	protected final int deltaMax;

	public JoinMinDeltaIndex( int nIndex, int qSize, int deltaMax, StatContainer stat, Query query, int threshold, boolean writeResult ) {
		// TODO: Need to be fixed to make index just for given sequences
		// NOW, it makes index for all sequences
		
		this.indexMap = new ArrayList<ArrayList<WYK_HashMap<QGram, List<Record>>>>();
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

		long getQGramTime = 0;
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

			if( DEBUG.JoinMinON ) {
				recordStartTime = System.nanoTime();
			}

			// NOTE; cannot use this.getCandidatePQGrams() since index is not completely built yet.
			List<List<QGram>> availableQGrams = rec.getQGrams( qSize+deltaMax );

			if( DEBUG.JoinMinON ) {
				recordMidTime = System.nanoTime();
				getQGramTime += recordMidTime - recordStartTime;
			}

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
				qgramCount += qgramList.size();
				for( QGram qgram : qgramList ) {
//					curridx_invokes.addTo( qgram, 1 );
					for ( Entry<QGram, Integer> entry : qdgen.getQGramDelta( qgram ) ) {
						QGram qgramDelta = entry.getKey();
						int delta = entry.getValue();
						curridx_invokes_delta.get( delta ).addTo( qgramDelta, 1 ); // TODO: maybe too many duplications?

						if( DEBUG.JoinMinIndexON ) {
							this.countPerPosition.add( i, 1 );
						}
					}
				}
//				}
			} // end for i from 0 to searchmax
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
		} // end for rec in query.serachedSet

		int distinctPQgram = 0;
		for( int i = 0; i < invokes.size(); i++ ) {
			ArrayList<Object2IntOpenHashMap<QGram>> invokes_delta = invokes.get( i );
			for ( int delta=0; delta<=deltaMax; ++delta ) {
				Object2IntOpenHashMap<QGram> count = invokes_delta.get( delta );
				distinctPQgram += count.size();
			}
		}
		stat.add( "Stat_Distinct_PQ", distinctPQgram );

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

		this.countTime = System.nanoTime() - starttime;

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
			stepTime.resetAndStart( "Result_3_1_2_Indexing_Time" );
		}
		else {
			if( DEBUG.SampleStatON ) {
				System.out.println( "[Gamma] " + gamma );
				System.out.println( "[Gamma] CountTime " + countTime );
				System.out.println( "[Gamma] SearchedSigCount " + searchedTotalSigCount );
			}
			if( DEBUG.PrintEstimationON ) {
				BufferedWriter bwEstimation = EstimationTest.getWriter();
				try {
					bwEstimation.write( "[Gamma] " + gamma );
					bwEstimation.write( " CountTime " + countTime );
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
		long indexedElements = 0;

		// find best K positions for each string in T
		indexedCountMap = new Object2IntOpenHashMap<>();
		estimatedCountMap = new Object2IntOpenHashMap<>();
		for( Record rec : query.targetIndexedSet.get() ) {
			int[] range = rec.getTransLengths();

			int searchmax = Math.min( range[ 0 ], invokes.size() );

			List<List<QGram>> availableQGrams = null;

			boolean isLowRecord = hybridIndex && ( rec.getEstNumTransformed() <= threshold );

			if( query.oneSideJoin ) {
				availableQGrams = rec.getSelfQGrams( qSize+deltaMax, searchmax );
				// System.out.println( availableQGrams.toString() );
			}
			else {
				availableQGrams = rec.getQGrams( qSize+deltaMax, searchmax );
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

			this.predictCount += mpq.minInvokes;

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
				if( DEBUG.JoinMinON ) {
					indexedElements += availableQGrams.get( minIdx ).size();
				}
			} // end while mpq.isEmpty

			indexedCountMap.put( rec, indexedCount );
		} // end for rec in query.targetIndexedSet

		this.indexTime = System.nanoTime() - starttime;
		this.delta = this.indexTime / this.indexedTotalSigCount;

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
			if( DEBUG.JoinMinIndexON ) {
				for( int i = 0; i < countPerPosition.size(); i++ ) {
					stat.add( String.format( "Stat_JoinMin_COUNT%02d", i ), countPerPosition.get( i ) );
				}

				for( int i = 0; i < indexMap.size(); i++ ) {
					if( indexMap.get( i ).size() != 0 ) {
						stat.add( String.format( "Stat_JoinMin_IDX%02d", i ), indexMap.get( i ).size() );
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
		while( indexMap.size() <= position ) {
			indexMap.add( new ArrayList<WYK_HashMap<QGram, List<Record>>>() );
		}
	}

	public void put( int position, int delta, QGram qgram, Record rec ) {
		ArrayList<WYK_HashMap<QGram, List<Record>>> index_pos = indexMap.get( position );
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

		Set<IntegerPair> rslt = new ObjectOpenHashSet<IntegerPair>();

		for( Record recS : query.searchedSet.get() ) {
			joinRecordMaxK( indexK, recS, rslt, writeResult, bw, checker, query.oneSideJoin );
		}

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
		epsilon = ( joinTime - getQGramTime ) / predictCount;
		this.gamma = ( this.countTime + getQGramTime ) / this.searchedTotalSigCount;

		// DEBUG
		epsilonPrime = joinTime / comparisonCount;

		if( DEBUG.JoinMinON ) {
			Util.printLog( "Est weight : " + comparisonCount );
			Util.printLog( "Join time : " + joinTime );
			Util.printLog( "Epsilon : " + epsilon );

			if( writeResult ) {
				// stat.add( "Last Token Filtered", lastTokenFiltered );
				stat.add( "Est_Join_0_GetQGramTime", getQGramTime );

				stat.add( "Result_3_2_1_Equiv_Checking_Time", comparisonTime / 1000000 );
			}
		}

		if( writeResult ) {
			stat.add( "Stat_Equiv_Comparison", equivComparisons );
			stat.add( "Stat_Length_Filtered", lengthFiltered );
			stat.add( "Join_Min_Result", rslt.size() );
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
			if ( k >= indexMap.size() ) continue;
			List<WYK_HashMap<QGram, List<Record>>> curidx = indexMap.get( k );
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
		long joinStartTime = System.nanoTime();

		long qgramCount = 0;
//		boolean debug = false;
//		if ( recS.getID() == 15756 ) debug = true;

		List<List<Set<QGram>>> candidateQGrams = getCandidatePQGrams( recS );
		// pos -> delta
		
		getQGramTime += System.nanoTime() - joinStartTime;

		int[] range = recS.getTransLengths();
		int searchmax = Integer.min( candidateQGrams.size(), indexMap.size() );
		ArrayList<String> debugArray = new ArrayList<String>();

		JoinMinCandidateSet allCandidateSet = new JoinMinCandidateSet( nIndex, recS, estimatedCountMap.getInt( recS ) );

		for( int i = 0; i < searchmax; ++i ) {
			List<Set<QGram>> cand_qgrams_pos = candidateQGrams.get( i );
			for ( Set<QGram> qgrams_delta : cand_qgrams_pos ) nCandQGrams += qgrams_delta.size();
			List<WYK_HashMap<QGram, List<Record>>> curridx_pos = indexMap.get( i );
			if( curridx_pos == null ) continue;

			// Given a position i
			WYK_HashSet<Record> candidates = new WYK_HashSet<Record>();
			
			for ( int delta_s=0; delta_s<=deltaMax; ++delta_s ) {
				if ( cand_qgrams_pos.size() <= delta_s ) break;
				for ( QGram qgram : cand_qgrams_pos.get( delta_s ) ) {
					for ( int delta_t=0; delta_t<=deltaMax-delta_s; ++delta_t ) {
						if( DEBUG.PrintJoinMinJoinON ) {
							debugArray.add( "q :" + qgram + " " + i + " " + delta_s + " " + delta_t + "\n" );
							qgramCount++;
						}

						List<Record> tree = curridx_pos.get( delta_t ).get( qgram );
		//				if ( debug ) System.out.println( "i: "+i+", qgram: "+qgram+", tree: "+tree );
						if( tree == null ) continue;

						for( Record e : tree ) {
							// length filtering
							if( oneSideJoin ) {
								if( StaticFunctions.overlap( e.getTokenCount()-deltaMax, e.getTokenCount(), range[ 0 ]-deltaMax, range[ 1 ] ) ) {
									if( DEBUG.PrintJoinMinJoinON ) {
										debugArray.add( "Cand: " + e + " by " + qgram + " at " + i + "\n" );
									}
									candidates.add( e );
									comparisonCount++;
								}
								else {
									++checker.lengthFiltered;
								}
							}
							else {
								throw new RuntimeException("UNIMPLEMENTED");
//								if( StaticFunctions.overlap( e.getMinTransLength(), e.getMaxTransLength(), range[ 0 ], range[ 1 ] ) ) {
//									candidates.add( e );
//									comparisonCount++;
//								}
//								else {
//									++checker.lengthFiltered;
//								}
							}
						} // end for Record e in tree
					} // end for delta_t
				} // end for qgram
			} // end for delta_s
			allCandidateSet.add( candidates );
		} // end for i from 0 to searchmax

		ArrayList<Record> candSet = allCandidateSet.getCandSet( indexedCountMap, debugArray );
		checker.pqgramFiltered += allCandidateSet.pqgramFiltered;
//		if ( debug) System.out.println( "candSet: "+candSet );

		equivComparisons += candSet.size();
		for( Record recR : candSet ) {
			if( DEBUG.PrintJoinMinJoinON ) {
				debugArray.add( "Test " + recR + "\n" );
			}

			long st = System.nanoTime();
			int compare = checker.isEqual( recS, recR );
			long duration = System.nanoTime() - st;

			comparisonTime += duration;
			if( compare >= 0 ) {
//				rslt.add( new IntegerPair( recS.getID(), recR.getID() ) );
				AlgorithmTemplate.addSeqResult( recS, recR, rslt, query.selfJoin );
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
		joinTime += System.nanoTime() - joinStartTime;
	}

	public void joinRecordMaxKThres( int nIndex, Record recS, Set<IntegerPair> rslt, boolean writeResult, BufferedWriter bw,
			Validator checker, int threshold, boolean oneSideJoin ) {
		long qgramStartTime = 0;
		long joinStartTime = 0;
		long qgramCount = 0;

		boolean isUpperRecord = recS.getEstNumTransformed() > threshold;

		Histogram hist = null;
		if( DEBUG.JoinMinON ) {
			qgramStartTime = System.nanoTime();
			hist = new Histogram( "Validation" );
		}

		List<List<Set<QGram>>> candidateQGrams = getCandidatePQGrams( recS );

		if( DEBUG.JoinMinON ) {
			getQGramTime += System.nanoTime() - qgramStartTime;
		}

		int[] range = recS.getTransLengths();
		int searchmax = Integer.min( candidateQGrams.size(), indexMap.size() );

		if( DEBUG.PrintJoinMinJoinON ) {
			joinStartTime = System.nanoTime();
		}

		JoinMinCandidateSet allCandidateSet = new JoinMinCandidateSet( nIndex, recS, estimatedCountMap.getInt( recS ) );

		for( int i = 0; i < searchmax; ++i ) {
			List<Set<QGram>> cand_qgrams_pos = candidateQGrams.get( i );
			for ( Set<QGram> qgrams_delta : cand_qgrams_pos ) nCandQGrams += qgrams_delta.size();
			List<WYK_HashMap<QGram, List<Record>>> curridx_pos = indexMap.get( i );
			if( curridx_pos == null ) continue;

			// Given a position i
			WYK_HashSet<Record> candidates = new WYK_HashSet<Record>();

			for ( int delta_s=0; delta_s<=deltaMax; ++delta_s ) {
				if ( cand_qgrams_pos.size() <= delta_s ) break;
				for( QGram qgram : cand_qgrams_pos.get( delta_s ) ) {
					for ( int delta_t=0; delta_t<=deltaMax-delta_s; ++delta_t ) {
						if( DEBUG.PrintJoinMinJoinON ) {
							qgramCount++;
						}

						List<Record> tree = curridx_pos.get( delta_t ).get( qgram );
						if( tree == null ) continue;

						for( Record e : tree ) {
							// length filtering
							if( !isUpperRecord && e.getEstNumTransformed() <= threshold ) {
								// this record will not compared by joinmin index.
								// this will be compared by joinnaive index
								continue;
							}
							if( oneSideJoin ) {
								if( StaticFunctions.overlap( e.getTokenCount()-deltaMax, e.getTokenCount(), range[ 0 ]-deltaMax, range[ 1 ] ) ) {

									candidates.add( e );
									comparisonCount++;
								}
								else {
									++checker.lengthFiltered;
								}
							}
							else {
								throw new RuntimeException("UNIMPLEMENTED");
//								if( StaticFunctions.overlap( e.getMinTransLength(), e.getMaxTransLength(), range[ 0 ], range[ 1 ] ) ) {
//									candidates.add( e );
//									comparisonCount++;
//								}
//								else {
//									++checker.lengthFiltered;
//								}
							}
						} // end for Record e in tree
					} // end for delta_t
				} // end for qgram
			} // end for delta_s
			allCandidateSet.add( candidates );
		} // end for i from 0 to searchmax

		ArrayList<Record> candSet = allCandidateSet.getCandSet( indexedCountMap, null );
		checker.pqgramFiltered += allCandidateSet.pqgramFiltered;

		equivComparisons += candSet.size();
		if( DEBUG.JoinMinON ) {
			hist.add( candSet.size() );
		}
		for( Record recR : candSet ) {

			long st = System.nanoTime();
			int compare = checker.isEqual( recS, recR );
			long duration = System.nanoTime() - st;

			comparisonTime += duration;
			if( compare >= 0 ) {
//				rslt.add( new IntegerPair( recS.getID(), recR.getID() ) );
				AlgorithmTemplate.addSeqResult( recS, recR, rslt, query.selfJoin );
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
		if( DEBUG.JoinMinON ) {
			hist.print();
			hist.printLogHistogram();
		}
	}

	public void clear() {
		for( int i = 0; i < indexMap.size(); i++ ) {
			indexMap.get( i ).clear();
		}
		indexMap.clear();
	}

	public void DebugWriteToFile( String filename ) {
		try {
			BufferedWriter bw = new BufferedWriter( new FileWriter( filename ) );

			for( int i = 0; i < indexMap.size(); i++ ) {
//				bw.write( i + "-th index\n" );
				ArrayList<WYK_HashMap<QGram, List<Record>>> map_pos = indexMap.get( i );
				
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
		stat.add( "nCandQGrams", nCandQGrams );
	}

	public double getGamma() { return gamma; }
	public double getDelta() { return delta; }
	public double getEpsilon() { return epsilon; }
	public double getEpsilonPrime() { return epsilonPrime; }
	public long getSearchedTotalSigCount(){ return searchedTotalSigCount; }
	public long getIndexedTotalSigCount() { return indexedTotalSigCount; }
	public long getEquivComparisons() { return equivComparisons; }
	public long getComparisonTime() { return comparisonTime; }
	public long getAppliedRulesSum() { return appliedRulesSum; }
	public long getComparisonCount() { return comparisonCount; }
	public double getIndexTime() { return indexTime; }
	public double getCountTime() { return countTime; }
	public long getPredictCount() { return predictCount; }
}
