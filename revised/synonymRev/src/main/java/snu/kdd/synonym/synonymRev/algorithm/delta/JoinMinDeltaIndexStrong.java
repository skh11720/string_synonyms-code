package snu.kdd.synonym.synonymRev.algorithm.delta;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
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

public class JoinMinDeltaIndexStrong extends JoinMinDeltaIndex {
	
	
	private List<JoinMinCandidateSet> allCandidateSet;
	List<WYK_HashSet<Record>> candidates;

	public JoinMinDeltaIndexStrong( int nIndex, int qSize, int deltaMax, StatContainer stat, Query query, int threshold,
			boolean writeResult ) {
		super( nIndex, qSize, deltaMax, stat, query, threshold, writeResult );
		// TODO Auto-generated constructor stub

		allCandidateSet = new ArrayList<JoinMinCandidateSet>();
		candidates = new ArrayList<WYK_HashSet<Record>>();
		for ( int delta=0; delta<=deltaMax; ++delta ) candidates.add( new WYK_HashSet<Record>() );
	}

	@Override
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
//		if( DEBUG.PrintJoinMinJoinON ) {
//			joinStartTime = System.nanoTime();
//		}

		allCandidateSet.clear();
		for ( int delta=0; delta<=deltaMax; ++delta ) allCandidateSet.add( new JoinMinCandidateSet( nIndex, recS, estimatedCountMap.getInt( recS ) ) );

		for( int i = 0; i < searchmax; ++i ) {
			List<Set<QGram>> cand_qgrams_pos = candidateQGrams.get( i );
			List<WYK_HashMap<QGram, List<Record>>> curridx_pos = index.get( i );
			if( curridx_pos == null ) continue;

			// Given a position i
			for ( int delta=0; delta<=deltaMax; ++delta ) candidates.get( delta ).clear();
			
			for ( int delta_s=0; delta_s<=deltaMax; ++delta_s ) {
				if ( cand_qgrams_pos.size() <= delta_s ) break;
				candQGramCount += cand_qgrams_pos.get( delta_s ).size();
				this.searchedTotalSigCount += cand_qgrams_pos.get( delta_s ).size();
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
								if( !useLF || StaticFunctions.overlap( e.getTokenCount()-deltaMax, e.getTokenCount(), range[ 0 ]-deltaMax, range[ 1 ] ) ) {
//										if( DEBUG.PrintJoinMinJoinON ) {
//											debugArray.add( "Cand: " + e + " by " + qgram + " at " + i + "\n" );
//										}
									for ( int delta=delta_s; delta<=deltaMax-delta_t; ++delta) candidates.get( delta ).add( e );
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
			for (int delta=0; delta<=deltaMax; ++delta ) {
				allCandidateSet.get( delta ).add( candidates.get( delta ) );
			}
		} // end for i from 0 to searchmax

//			ArrayList<Record> candSet = allCandidateSet.getCandSet( indexedCountMap, debugArray );
		Set<Record> candSet = new WYK_HashSet<Record>();
		for (int delta=0; delta<=deltaMax; ++delta ) {
			candSet.addAll( allCandidateSet.get( delta ).getCandSet( indexedCountMap, null ) );
			checker.pqgramFiltered += allCandidateSet.get( delta ).pqgramFiltered;
		}
		long afterFilterTime = System.nanoTime();
//			if ( debug) System.out.println( "candSet: "+candSet );

		equivComparisons += candSet.size();
		predictCount += candSet.size();
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
}
