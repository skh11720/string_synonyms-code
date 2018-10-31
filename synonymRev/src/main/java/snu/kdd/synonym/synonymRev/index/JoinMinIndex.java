package snu.kdd.synonym.synonymRev.index;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.MinPositionQueue;
import snu.kdd.synonym.synonymRev.tools.MinPositionQueue.MinPosition;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StaticFunctions;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.tools.WYK_HashMap;
import snu.kdd.synonym.synonymRev.tools.WYK_HashSet;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class JoinMinIndex implements JoinMinIndexInterface {
	protected ArrayList<WYK_HashMap<QGram, List<Record>>> idx;
	protected ArrayList<Integer> countPerPosition = null;
	protected Object2IntOpenHashMap<Record> indexedCountMap;
	protected Object2IntOpenHashMap<Record> estimatedCountMap;
	protected WYK_HashSet<Integer> bypassSet = null;
	protected Query query;

	public double mu;
	public double lambda;
	public double rho;
	public double rhoPrime;

	protected final int indexK;
	protected final int qgramSize;

	public long searchedTotalSigCount = 0;
	public long indexedTotalSigCount = 0;
	public long equivComparisons = 0;
	public long appliedRulesSum = 0;
	public long candQGramCountSum = 0;
	public double candQGramAvgCount = 0;

	public long comparisonCount = 0;
	public long lengthFiltered = 0;

	public long indexCountTime = 0;
	public long indexRecordTime = 0;
	public long indexTime = 0;
	public double candQGramTime = 0;
	public double filterTime = 0;
	public long verifyTime = 0;
	public double joinTime = 0;

	public long predictCount = 0;

	public static boolean useLF = true;
	public static boolean usePQF = true;

	public Int2IntOpenHashMap posCounter = new Int2IntOpenHashMap();
	
	protected JoinMinIndex( int indexK, int qSize, Query query ) {
		// TODO: Need to be fixed to make index just for given sequences
		// NOW, it makes index for all sequences
		
		this.idx = new ArrayList<WYK_HashMap<QGram, List<Record>>>();
		this.indexK = indexK;
		this.qgramSize = qSize;
		this.query = query;
		posCounter.defaultReturnValue(0);
	}
		
	public JoinMinIndex( int indexK, int qSize, StatContainer stat, Query query, int threshold, boolean writeResult ) {
		this( indexK, qSize, query );
		initialize( stat, threshold, writeResult );
	} // end JoinMinIndex constructor
	
	protected void initialize( StatContainer stat, int threshold, boolean writeResult ) {
		long ts = System.nanoTime();

		boolean hybridIndex = threshold != 0;
		if( DEBUG.JoinMinIndexON ) {
			this.countPerPosition = new ArrayList<Integer>();
		}

		BufferedWriter bw = null;
		if( DEBUG.JoinMinIndexON ) {
			try { bw = new BufferedWriter( new FileWriter( "JoinMin_Index_Count_Debug_" + writeResult + ".txt" ) ); }
			catch( IOException e ) { e.printStackTrace(); }
		}

		// Build an index
		// Count Invokes per each (token, loc) pair
		List<Object2IntOpenHashMap<QGram>> invokes = new ArrayList<Object2IntOpenHashMap<QGram>>();
		List<Object2IntOpenHashMap<QGram>> lowInvokes = null;

		if( hybridIndex && !query.oneSideJoin ) {
			// we do not have to compute the lowInvokes for the oneSideJoin
			lowInvokes = new ArrayList<Object2IntOpenHashMap<QGram>>();
		}

		countInvokes( invokes, lowInvokes, hybridIndex, threshold, bw );
		long afterGenSuperTPQ = System.nanoTime();

		findBestPositions( invokes, lowInvokes, hybridIndex, threshold, indexK );
		long afterIndexRecordTime = System.nanoTime();

//		this.indexTime = System.nanoTime() - ts;
		this.indexCountTime = afterGenSuperTPQ - ts;
		this.indexRecordTime = afterIndexRecordTime - afterGenSuperTPQ;
		this.indexTime = afterIndexRecordTime - ts;
		this.lambda = this.indexRecordTime / this.indexedTotalSigCount;

		for( Object2IntOpenHashMap<QGram> in : invokes ) {
			in.clear();
		}
		
		stat.add( "posDistribution", posCounter.toString() );
	}

	public void setIndex( int position ) {
		while( idx.size() <= position ) {
			idx.add( new WYK_HashMap<QGram, List<Record>>() );
		}
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
	
	protected List<Record> prepareCountInvokes() {
		return query.searchedSet.recordList;
	}
	
	protected void countInvokes( List<Object2IntOpenHashMap<QGram>> invokes, List<Object2IntOpenHashMap<QGram>> lowInvokes, boolean hybridIndex, int threshold, BufferedWriter bw ) {
		List<Record> searchedList = prepareCountInvokes();
		// count number of occurrence of a positional q-grams
		long recordStartTime = 0;
		long recordMidTime = 0;

		for( Record rec : searchedList ) {

			if( DEBUG.JoinMinON ) {
				recordStartTime = System.nanoTime();
			}

			List<List<QGram>> availableQGrams = rec.getQGrams( qgramSize );

//			if( DEBUG.JoinMinON ) {
			recordMidTime = System.nanoTime();
//			getQGramTime += recordMidTime - recordStartTime;
//			}

			int searchmax = availableQGrams.size();

			for( int i = invokes.size(); i < searchmax; i++ ) {
				Object2IntOpenHashMap<QGram> inv = new Object2IntOpenHashMap<QGram>();
				inv.defaultReturnValue( 0 );

				invokes.add( inv );

				if( lowInvokes != null ) {
					Object2IntOpenHashMap<QGram> invLow = new Object2IntOpenHashMap<QGram>();
					invLow.defaultReturnValue( 0 );

					lowInvokes.add( invLow );
				}

				if( DEBUG.JoinMinIndexON ) {
					this.countPerPosition.add( 0 );
				}
			}

			boolean isLowRecord = hybridIndex && ( rec.getEstNumTransformed() <= threshold );

			long qgramCount = 0;
			for( int i = 0; i < searchmax; ++i ) {
				Object2IntOpenHashMap<QGram> curridx_invokes = null;
				if( !isLowRecord ) {
					// it is not the hybrid index or not low record for the hybrid index
					curridx_invokes = invokes.get( i );
				}
				else {
					if( query.oneSideJoin ) {
						// if uni-directional join && isLowRecord, this record will not be compared with joinMin index
						break;
					}
					curridx_invokes = lowInvokes.get( i );
				}

				List<QGram> available = availableQGrams.get( i );
				qgramCount += available.size();
				for( QGram qgram : available ) {
					int count = curridx_invokes.getInt( qgram );
					if( count == 0 ) {
						curridx_invokes.put( qgram, 1 );
					}
					else {
						curridx_invokes.put( qgram, count + 1 );
					}
				}

				if( DEBUG.JoinMinIndexON ) {
					int newSize = this.countPerPosition.get( i ) + available.size();

					this.countPerPosition.set( i, newSize );
				}
			}
//			this.searchedTotalSigCount += qgramCount;

//			if( DEBUG.JoinMinON ) {
//				countIndexingTime += System.nanoTime() - recordMidTime;
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
		} // end for rec in searchedSet

//		int distinctPQgram = 0;
//		for( int i = 0; i < invokes.size(); i++ ) {
//			Object2IntOpenHashMap<QGram> count = invokes.get( i );
//
//			distinctPQgram += count.size();
//		}
//		stat.add( "Stat_Distinct_PQ", distinctPQgram );

		if( DEBUG.JoinMinIndexON ) {
			try {
				for( int i = 0; i < invokes.size(); i++ ) {
					Object2IntOpenHashMap<QGram> count = invokes.get( i );
					ObjectIterator<Object2IntMap.Entry<QGram>> iter = count.object2IntEntrySet().iterator();

					while( iter.hasNext() ) {
						Object2IntMap.Entry<QGram> entry = iter.next();

						QGram q = entry.getKey();
						int c = entry.getIntValue();
						bw.write( "Inv: " + q + " " + i + "=>" + c + "\n" );
					}
				}

				bw.close();
			}
			catch( IOException e ) {
				e.printStackTrace();
			}
		}
		// we have the number of occurrence of a positional q-grams
	} // end countInvokes
	
	protected void findBestPositions(List<Object2IntOpenHashMap<QGram>> invokes, List<Object2IntOpenHashMap<QGram>> lowInvokes, boolean hybridIndex, int threshold, int nIndex ) {
		// find best K positions for each string in T
		indexedCountMap = new Object2IntOpenHashMap<>();
		estimatedCountMap = new Object2IntOpenHashMap<>();
		for( Record rec : query.targetIndexedSet.get() ) {
//			int[] range = rec.getTransLengths();

			int searchmax;
			if ( query.oneSideJoin ) searchmax = Math.min( rec.size(), invokes.size() );
			else throw new RuntimeException("UNIMPLEMENTED");
//			searchmax = Math.min( range[ 0 ], invokes.size() );

			List<List<QGram>> availableQGrams = null;

			boolean isLowRecord = hybridIndex && ( rec.getEstNumTransformed() <= threshold );

			if( query.oneSideJoin ) {
				availableQGrams = rec.getSelfQGrams( qgramSize, searchmax );
				// System.out.println( availableQGrams.toString() );
			}
			else {
				availableQGrams = rec.getQGrams( qgramSize, searchmax );
			}

			for( List<QGram> set : availableQGrams ) {
				this.indexedTotalSigCount += set.size();
			}

			MinPositionQueue mpq = new MinPositionQueue( nIndex );

			for( int i = 0; i < searchmax; ++i ) {
				// There is no invocation count: this is the minimum point
				if( i >= invokes.size() ) {
					mpq.add( i, 0 );

//					if( DEBUG.PrintJoinMinIndexON ) {
//						try {
//							bw_index.write( "pos " + i + " 0\n" );
//						}
//						catch( IOException e ) {
//							e.printStackTrace();
//						}
//					}
					continue;
				}

				Object2IntOpenHashMap<QGram> curridx_invokes = invokes.get( i );
				Object2IntOpenHashMap<QGram> curridx_lowInvokes = null;
				if( !query.oneSideJoin && hybridIndex ) {
					curridx_lowInvokes = lowInvokes.get( i );
				}
				int invoke = 0;

				for( QGram qgram : availableQGrams.get( i ) ) {
					int count = curridx_invokes.getInt( qgram );

					if( !query.oneSideJoin && hybridIndex && !isLowRecord ) {
						count += curridx_lowInvokes.getInt( qgram );
					}

					if( count != 0 ) {
						// upper bound
						invoke += count;
					}
				}

//				if( DEBUG.PrintJoinMinIndexON ) {
//					try {
//						bw_index.write( "pos " + i + " " + invoke + "\n" );
//					}
//					catch( IOException e ) {
//						e.printStackTrace();
//					}
//				}

				mpq.add( i, invoke );
			}

			this.predictCount += mpq.minInvokes;

			int indexedCount = 0;
			while( !mpq.isEmpty() ) {
				indexedCount++;

				MinPosition minPos = mpq.poll();
				int minIdx = minPos.positionIndex;
				posCounter.addTo( minIdx, 1 );

//				if( DEBUG.PrintJoinMinIndexON ) {
//					try {
//						bw_index.write( minPos.positionIndex + " " + minPos.candidateCount + "\n" );
//					}
//					catch( IOException e ) {
//						e.printStackTrace();
//					}
//				}

				this.setIndex( minIdx );
				for( QGram qgram : availableQGrams.get( minIdx ) ) {
					// write2File(bw, minIdx, twogram, rec.getID());

//					if( DEBUG.PrintJoinMinIndexON ) {
//						try {
//							bw_index.write( minIdx + ", " + qgram + " : " + rec + "\n" );
//						}
//						catch( IOException e ) {
//							e.printStackTrace();
//						}
//					}

					this.put( minIdx, qgram, rec );
				}
//				if( DEBUG.JoinMinON ) {
//					indexedElements += availableQGrams.get( minIdx ).size();
//				}
			}

			indexedCountMap.put( rec, indexedCount );
		} // end for rec in indexedSet
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

		int skipped = 0;
		for( Record recS : query.searchedSet.get() ) {
			if ( recS.getEstNumTransformed() > DEBUG.EstTooManyThreshold ) {
				++skipped;
				continue;
			}
			joinRecordMaxK( indexK, recS, rslt, writeResult, bw, checker, query.oneSideJoin );
		}

		this.candQGramAvgCount = 1.0 * this.candQGramCountSum / (query.searchedSet.size() - skipped);
		stat.add( "Stat_CandQGram_Sum", this.candQGramCountSum );
		stat.add( "Stat_CandQGram_Avg", this.candQGramAvgCount );
		stat.add( "Stat_Equiv_Comparison", this.equivComparisons );
		stat.add( "Stat_Skipped", skipped );


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
		this.mu = ( this.indexCountTime + candQGramTime ) / this.searchedTotalSigCount;
		this.rho = ( joinTime - candQGramTime ) / predictCount;

		// DEBUG
		rhoPrime = joinTime / comparisonCount;

		if( DEBUG.JoinMinON ) {
			Util.printLog( "Est weight : " + comparisonCount );
			Util.printLog( "Join time : " + joinTime );
			Util.printLog( "Rho : " + rho );

			if( writeResult ) {
				// stat.add( "Last Token Filtered", lastTokenFiltered );
				stat.add( "Est_Join_0_GetQGramTime", candQGramTime );

				stat.add( "Result_3_2_1_Equiv_Checking_Time", verifyTime / 1000000 );
			}
		}

		if( writeResult ) {
//			stat.add( "Stat_Equiv_Comparison", equivComparisons );
			stat.add( "Stat_Length_Filtered", lengthFiltered );
			stat.add( "Join_Min_Result", rslt.size() );
		}
		else {
			if( DEBUG.SampleStatON ) {
				System.out.println( "[Rho] " + rho );
				System.out.println( "[Rho] JoinTime " + joinTime );
				System.out.println( "[Rho] PredictCount " + predictCount );
				System.out.println( "[Rho] ActualCount " + equivComparisons );
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
		
//		stat.add( "Const_Lambda", lambda );
//		stat.add( "Const_Mu", mu );
//		stat.add( "Const_Rho", rho );

		return rslt;
	}
	
	protected List<List<QGram>> getCandidatePQGrams( Record rec ) {
		List<List<QGram>> availableQGrams = rec.getQGrams( qgramSize );
		List<List<QGram>> candidatePQGrams = new ArrayList<List<QGram>>();
		for ( int k=0; k<availableQGrams.size(); ++k ) {
			if ( k >= idx.size() ) continue;
			WYK_HashMap<QGram, List<Record>> curidx = idx.get( k );
			List<QGram> qgrams = new ArrayList<QGram>();
			for ( QGram qgram : availableQGrams.get( k ) ) {
				if ( !curidx.containsKey( qgram ) ) continue;
				qgrams.add( qgram );
			}
			candidatePQGrams.add( qgrams );
		}
		return candidatePQGrams;
	}

	public void joinRecordMaxK( int nIndex, Record recS, Set<IntegerPair> rslt, boolean writeResult, BufferedWriter bw,
			Validator checker, boolean oneSideJoin ) {
		joinRecordMaxKThres( nIndex, recS, rslt, writeResult, bw, checker, -1, oneSideJoin );
	}

	public void joinRecordMaxKThres( int nIndex, Record recS, Set<IntegerPair> rslt, boolean writeResult, BufferedWriter bw,
			Validator checker, int threshold, boolean oneSideJoin ) {
	    boolean isUpperRecord = threshold <= 0 ? true : recS.getEstNumTransformed() > threshold;
		if (!isUpperRecord) return;

		long ts = System.nanoTime();

		long candQGramCount = 0;
//		boolean debug = false;
//		if ( recS.getID() == 15756 ) debug = true;

		List<List<QGram>> availableQGrams = getCandidatePQGrams( recS );
		for ( List<QGram> candidateQGrams : availableQGrams ) {
			candQGramCount += candidateQGrams.size();
		}
		this.candQGramCountSum += candQGramCount;
		long afterCandQGramTime = System.nanoTime();

		int[] range = recS.getTransLengths();
		int searchmax = Integer.min( availableQGrams.size(), idx.size() );
//		ArrayList<String> debugArray = new ArrayList<String>();

		JoinMinCandidateSet allCandidateSet = new JoinMinCandidateSet( nIndex, recS, estimatedCountMap.getInt( recS ) );

		for( int i = 0; i < searchmax; ++i ) {
			this.searchedTotalSigCount += availableQGrams.get( i ).size();
			Map<QGram, List<Record>> curridx = idx.get( i );
			if( curridx == null ) {
				continue;
			}

			WYK_HashSet<Record> candidates = new WYK_HashSet<Record>();

			for( QGram qgram : availableQGrams.get( i ) ) {
//				if( DEBUG.PrintJoinMinJoinON ) {
//					debugArray.add( "q :" + qgram + " " + i + "\n" );

//					qgramCount++;
//				}

				List<Record> tree = curridx.get( qgram );
//				if ( debug ) System.out.println( "i: "+i+", qgram: "+qgram+", tree: "+tree );

				if( tree == null ) {
					continue;
				}

				for( Record e : tree ) {
					if( oneSideJoin ) {
						if( !useLF || StaticFunctions.overlap( e.getTokenCount(), e.getTokenCount(), range[ 0 ], range[ 1 ] ) ) {
//							if( DEBUG.PrintJoinMinJoinON ) {
//								debugArray.add( "Cand: " + e + " by " + qgram + " at " + i + "\n" );
//							}
							candidates.add( e );
							comparisonCount++;
						}
						else ++checker.lengthFiltered;
					}
					else {
						throw new RuntimeException( "UNIMPLEMENTED" );
//						if( StaticFunctions.overlap( e.getMinTransLength(), e.getMaxTransLength(), range[ 0 ], range[ 1 ] ) ) {
//							candidates.add( e );
//							comparisonCount++;
//						}
//						else {
//							++checker.lengthFiltered;
//						}
					}
				}
			}
			allCandidateSet.add( candidates );
		}
		ArrayList<Record> candSet = allCandidateSet.getCandSet( indexedCountMap, null );
		checker.pqgramFiltered += allCandidateSet.pqgramFiltered;
//		if ( debug) System.out.println( "candSet: "+candSet );
		long afterFilterTime = System.nanoTime();

		equivComparisons += candSet.size();
		for( Record recR : candSet ) {
//			if( DEBUG.PrintJoinMinJoinON ) {
//				debugArray.add( "Test " + recR + "\n" );
//			}

			long st = System.nanoTime();
			int compare = checker.isEqual( recS, recR );
			long duration = System.nanoTime() - st;

			verifyTime += duration;
			if( compare >= 0 ) {
//				rslt.add( new IntegerPair( recS.getID(), recR.getID() ) );
				AlgorithmTemplate.addSeqResult( recS, recR, rslt, query.selfJoin );
				appliedRulesSum += compare;

//				if( DEBUG.PrintJoinMinJoinON ) {
//					debugArray.add( "Val " + recS + " : " + recR + "\n" );
//				}
			}
		}
		long afterVerifyTime = System.nanoTime();

//		if( DEBUG.PrintJoinMinJoinON ) {
//			long joinTime = System.nanoTime() - ts;
//
//			try {
//				Collections.sort( debugArray );
//				for( String temp : debugArray ) {
//					bw.write( temp );
//				}
//				bw.write( "" + qgramCount );
//				bw.write( " " + joinTime );
//				bw.write( "\n" );
//			}
//			catch( IOException e ) {
//				e.printStackTrace();
//			}
//		}
		
		candQGramTime += afterCandQGramTime - ts;
		filterTime += afterFilterTime - afterCandQGramTime;
		verifyTime += afterVerifyTime - afterFilterTime;
		joinTime += afterVerifyTime - ts;
	}

	public void clear() {
		for( int i = 0; i < idx.size(); i++ ) {
			idx.get( i ).clear();
		}
		idx.clear();
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

				if( !usePQF || indexedCountMap.getInt( r ) == entry.getValue() ) {
					list.add( r );
				}
				else ++pqgramFiltered;
			}

			return list;
		}
	}
	
	public void addStat( StatContainer stat ) {
//		stat.add( "nCandQGrams", nCandQGrams );
	}
	
	public double getMu() { return mu; }
	public double getLambda() { return lambda; }
	public double getRho() { return rho; }
	public double getRhoPrime() { return rhoPrime; }
	public long getSearchedTotalSigCount(){ return searchedTotalSigCount; }
	public long getIndexedTotalSigCount() { return indexedTotalSigCount; }
	public long getCandQGramCountSum() { return candQGramCountSum; } 
	public double getCandQGramAvgCount() { return candQGramAvgCount; }
	public long getEquivComparisons() { return equivComparisons; }
	public long getComparisonTime() { return verifyTime; }
	public long getAppliedRulesSum() { return appliedRulesSum; }
	public long getComparisonCount() { return comparisonCount; }
	public double getIndexTime() { return indexTime; }
	public double getCountTime() { return indexCountTime; }
	public long getPredictCount() { return predictCount; }
}
