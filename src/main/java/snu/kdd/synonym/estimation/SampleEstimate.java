package snu.kdd.synonym.estimation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import mine.Record;
import snu.kdd.synonym.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.algorithm.JoinMin_Q;
import snu.kdd.synonym.algorithm.JoinNaive1;
import snu.kdd.synonym.tools.StatContainer;
import snu.kdd.synonym.tools.Util;
import tools.DEBUG;
import tools.QGram;
import tools.WYK_HashMap;
import validator.Validator;

public class SampleEstimate {

	// alpha: Naive indexing time per transformed strings of table T
	public double alpha;
	// beta: Navie join time per transformed strings of table S
	public double beta;
	// gamma: JoinMin counting twogram time per twograms of table S
	public double gamma;
	// delta: JoinMin indexing time per twograms of table T
	public double delta;
	// epsilon: JoinMin join time per candidate of table S
	public double epsilon;

	public double sampleRatio;

	List<Record> originalSearched;
	List<Record> originalIndexed;

	List<Record> sampleSearchedList = new ArrayList<Record>();
	List<Record> sampleIndexedList = new ArrayList<Record>();

	public SampleEstimate( final List<Record> tableSearched, final List<Record> tableIndexed, double sampleratio,
			boolean isSelfJoin ) {
		Random rn = new Random( 0 );

		int smallTableSize = Integer.min( tableSearched.size(), tableIndexed.size() );
		this.sampleRatio = sampleratio;

		if( sampleratio > 1 ) {
			// fixed number of samples
			if( sampleratio > smallTableSize ) {
				this.sampleRatio = 1;
			}
			else {
				this.sampleRatio = sampleratio / smallTableSize;
			}
		}

		if( this.sampleRatio * smallTableSize < 1 ) {
			// too low sample ratio
			Util.printLog( "Too low sample ratio" );
			Util.printLog( "Too low sample ratio" );

			this.sampleRatio = 10.0 / smallTableSize;
		}

		for( Record r : tableSearched ) {
			if( rn.nextDouble() < this.sampleRatio ) {
				sampleSearchedList.add( r );
			}
		}

		if( isSelfJoin ) {
			for( Record r : sampleSearchedList ) {
				sampleIndexedList.add( r );
			}
		}
		else {
			for( Record s : tableIndexed ) {
				if( rn.nextDouble() < this.sampleRatio ) {
					sampleIndexedList.add( s );
				}
			}
		}

		if( DEBUG.SampleStatOn ) {
			Util.printLog( sampleSearchedList.size() + " Searched records are sampled" );
			Util.printLog( sampleIndexedList.size() + " Indexed records are sampled" );
		}

		originalSearched = tableSearched;
		originalIndexed = tableIndexed;
	}

	public void estimateNaive( AlgorithmTemplate o, StatContainer stat ) {
		o.tableSearched = sampleSearchedList;
		o.tableIndexed = sampleIndexedList;

		// Infer alpha and beta
		JoinNaive1 naiveinst = new JoinNaive1( o, stat );
		naiveinst.threshold = 100;
		naiveinst.runWithoutPreprocess( false );
		alpha = naiveinst.getAlpha();
		beta = naiveinst.getBeta();

		// Restore tables
		o.tableSearched = originalSearched;
		o.tableIndexed = originalIndexed;

		if( DEBUG.SampleStatOn ) {
			Util.printLog( "Alpha : " + alpha );
			Util.printLog( "Beta : " + beta );

			stat.add( "Const_Alpha", String.format( "%.2f", alpha ) );
			stat.add( "Const_Beta", String.format( "%.2f", beta ) );
		}
	}

	public void estimateJoinMin( AlgorithmTemplate o, StatContainer stat, Validator checker, int qSize ) {
		o.tableSearched = sampleSearchedList;
		o.tableIndexed = sampleIndexedList;

		if( DEBUG.SampleStatOn ) {
			stat.add( "Stat_Sample Searched size", sampleSearchedList.size() );
			stat.add( "Stat_Sample Indexed size", sampleIndexedList.size() );
		}

		// Infer gamma, delta and epsilon
		JoinMin_Q joinmininst = new JoinMin_Q( o, stat );

		joinmininst.checker = checker;
		joinmininst.qSize = qSize;
		joinmininst.outputfile = null;

		if( DEBUG.SampleStatOn ) {
			Util.printLog( "Joinmininst run" );
		}

		joinmininst.runWithoutPreprocess( false );

		if( DEBUG.SampleStatOn ) {
			Util.printLog( "Joinmininst run done" );
		}

		gamma = joinmininst.getGamma();
		delta = joinmininst.getDelta();
		epsilon = joinmininst.getEpsilon();

		Validator.printStats();

		if( DEBUG.SampleStatOn ) {
			Util.printLog( "Gamma : " + gamma );
			Util.printLog( "Delta : " + delta );
			Util.printLog( "Epsilon : " + epsilon );

			stat.add( "Const_Gamma", String.format( "%.2f", gamma ) );
			stat.add( "Const_Delta", String.format( "%.2f", delta ) );
			stat.add( "Const_Epsilon", String.format( "%.2f", epsilon ) );

			stat.add( "Const_EpsilonPrime", String.format( "%.2f", joinmininst.idx.epsilonPrime ) );
		}

		// Restore tables
		o.tableSearched = originalSearched;
		o.tableIndexed = originalIndexed;
	}

	public void estimateWithSample( StatContainer stat, AlgorithmTemplate o, Validator checker, int qSize ) {
		estimateNaive( o, stat );
		estimateJoinMin( o, stat, checker, qSize );
	}

	public double getEstimateNaive( double totalExpLengthIndex, double totalExpJoin ) {
		return alpha * totalExpLengthIndex + beta * totalExpJoin;
	}

	public double getEstimateJoinMin( double searchedTotalSigCount, double indexedTotalSigCount, double estimatedInvokes ) {
		return gamma * searchedTotalSigCount + delta * indexedTotalSigCount + epsilon * estimatedInvokes;
	}

	public int findTheta( int qSize, int maxIndex, StatContainer stat, double totalExpLengthNaiveIndex, double totalExpNaiveJoin,
			double[] partialExpLengthNaiveIndex, double[] partialExpNaiveJoin, long maxIndexedEstNumRecords,
			long maxSearchedEstNumRecords ) {
		List<Map<QGram, CountEntry>> positionalQCountMap = new ArrayList<Map<QGram, CountEntry>>();

		// count qgrams for each that will be searched
		double searchedTotalSigCount = 0;

		for( Record rec : sampleSearchedList ) {
			List<List<QGram>> availableQGrams = rec.getQGrams( qSize );
			int searchmax = Math.min( availableQGrams.size(), maxIndex );

			for( int i = positionalQCountMap.size(); i < searchmax; i++ ) {
				positionalQCountMap.add( new WYK_HashMap<QGram, CountEntry>() );
			}

			long qgramCount = 0;
			for( int i = 0; i < searchmax; ++i ) {
				Map<QGram, CountEntry> currPositionalCount = positionalQCountMap.get( i );

				List<QGram> positionalQGram = availableQGrams.get( i );
				qgramCount += positionalQGram.size();
				for( QGram qgram : positionalQGram ) {
					CountEntry count = currPositionalCount.get( qgram );

					if( count == null ) {
						count = new CountEntry();
						currPositionalCount.put( qgram, count );
					}

					count.increase( rec.getEstNumRecords() );

				}
			}

			searchedTotalSigCount += qgramCount;
		}

		// since both tables are sorted with est num records, the two values are minimum est num records in both tables
		int threshold = 1;

		long bestThreshold = Long.max( maxSearchedEstNumRecords, maxIndexedEstNumRecords );
		double bestEstimatedTime = getEstimateNaive( totalExpLengthNaiveIndex, totalExpNaiveJoin );

		if( DEBUG.JoinHybridON ) {
			stat.add( "Est_Theta_Start_Threshold", bestThreshold );
			stat.add( "Est_Theta_" + CountEntry.countMax + "_1_NaiveTime", bestEstimatedTime );
			stat.add( "Est_Theta_" + CountEntry.countMax + "_2_JoinMinTime", 0 );
			stat.add( "Est_Theta_" + CountEntry.countMax + "_3_TotalTime", bestEstimatedTime );

			stat.add( "Const_Beta_JoinTime_2", String.format( "%.2f", totalExpNaiveJoin * beta ) );
			stat.add( "Const_Beta_TotalExp_2", String.format( "%.2f", totalExpNaiveJoin ) );

			stat.add( "Const_Alpha_IndexTime_" + CountEntry.countMax, String.format( "%.2f", totalExpLengthNaiveIndex * alpha ) );
			stat.add( "Const_Alpha_ExpLength_" + CountEntry.countMax, String.format( "%.2f", totalExpLengthNaiveIndex ) );

			Util.printLog( "ThresholdId: " + bestThreshold );
			Util.printLog( "Naive Time: " + bestEstimatedTime );
			Util.printLog( "JoinMin Time: " + 0 );
			Util.printLog( "Total Time: " + bestEstimatedTime );
		}

		int startThresIndex = CountEntry.getIndex( bestThreshold ) - 1;
		threshold = (int) Math.pow( 10, startThresIndex + 1 );

		// estimate time if only naive algorithm is used

		int indexedIdx = sampleIndexedList.size() - 1;

		double indexedTotalSigCount = 0;

		double fixedInvokes = 0;

		for( int thresholdExponent = startThresIndex; thresholdExponent >= 0; thresholdExponent-- ) {

			// estimate naive time
			double diffExpNaiveJoin = partialExpNaiveJoin[ thresholdExponent ];
			double diffExpLengthNaiveIndex = partialExpLengthNaiveIndex[ thresholdExponent ];

			for( int i = 0; i < thresholdExponent; i++ ) {
				diffExpNaiveJoin += partialExpNaiveJoin[ i ];
				diffExpLengthNaiveIndex += partialExpLengthNaiveIndex[ i ];
			}

			double naiveTime = getEstimateNaive( diffExpLengthNaiveIndex, diffExpNaiveJoin );

			if( DEBUG.JoinHybridON ) {
				stat.add( "Const_Beta_JoinTime_" + thresholdExponent, String.format( "%.2f", diffExpNaiveJoin * beta ) );
				stat.add( "Const_Beta_TotalExp_" + thresholdExponent, String.format( "%.2f", diffExpNaiveJoin ) );

				stat.add( "Const_Alpha_IndexTime_" + thresholdExponent,
						String.format( "%.2f", diffExpLengthNaiveIndex * alpha ) );
				stat.add( "Const_Alpha_ExpLength_" + thresholdExponent, String.format( "%.2f", diffExpLengthNaiveIndex ) );
			}

			// estimate joinmin time

			// process records with large expanded sizes
			int recordIdx = indexedIdx;

			for( ; recordIdx >= 0; recordIdx-- ) {
				Record rec = sampleIndexedList.get( recordIdx );

				if( rec.getEstNumRecords() <= threshold ) {
					break;
				}

				int[] range = rec.getCandidateLengths( rec.size() - 1 );

				int searchmax = Math.min( range[ 0 ], positionalQCountMap.size() );

				List<List<QGram>> availableQGrams = rec.getQGrams( qSize, searchmax );
				if( thresholdExponent == startThresIndex ) {
					for( List<QGram> set : availableQGrams ) {
						indexedTotalSigCount += set.size();
					}
				}

				@SuppressWarnings( "unused" )
				int minIdx = 0;
				double minInvokes = Double.MAX_VALUE;

				for( int i = 0; i < searchmax; ++i ) {
					if( availableQGrams.get( i ).isEmpty() ) {
						continue;
					}

					// There is no invocation count: this is the minimum point
					if( i >= positionalQCountMap.size() ) {
						minIdx = i;
						minInvokes = 0;
						break;
					}

					Map<QGram, CountEntry> curridx_invokes = positionalQCountMap.get( i );
					if( curridx_invokes.size() == 0 ) {
						minIdx = i;
						minInvokes = 0;
						break;
					}

					int invoke = 0;

					for( QGram qgram : availableQGrams.get( i ) ) {
						CountEntry count = curridx_invokes.get( qgram );
						if( count != null ) {
							// upper bound
							invoke += count.total;
						}
					}
					if( invoke < minInvokes ) {
						minIdx = i;
						minInvokes = invoke;
					}
				}

				fixedInvokes += minInvokes;
			}

			indexedIdx = recordIdx;

			double variableInvokes = 0;
			for( ; recordIdx >= 0; recordIdx-- ) {
				Record rec = sampleIndexedList.get( recordIdx );

				int[] range = rec.getCandidateLengths( rec.size() - 1 );
				int searchmax = Math.min( range[ 0 ], positionalQCountMap.size() );

				List<List<QGram>> availableQGrams = rec.getQGrams( qSize, searchmax );
				if( thresholdExponent == startThresIndex ) {
					for( List<QGram> set : availableQGrams ) {
						indexedTotalSigCount += set.size();
					}
				}

				double minInvokes = Double.MAX_VALUE;

				for( int i = 0; i < searchmax; ++i ) {
					if( availableQGrams.get( i ).isEmpty() ) {
						continue;
					}

					// There is no invocation count: this is the minimum point
					if( i >= positionalQCountMap.size() ) {
						minInvokes = 0;
						break;
					}

					Map<QGram, CountEntry> curridx_invokes = positionalQCountMap.get( i );
					if( curridx_invokes.size() == 0 ) {
						minInvokes = 0;
						break;
					}

					int invoke = 0;

					for( QGram qgram : availableQGrams.get( i ) ) {
						CountEntry count = curridx_invokes.get( qgram );
						if( count != null ) {
							// upper bound
							for( int c = thresholdExponent + 1; c < 3; c++ ) {
								invoke += count.count[ c ];
							}
						}
					}
					if( invoke < minInvokes ) {
						minInvokes = invoke;
					}
				}
				variableInvokes += minInvokes;
			}

			double joinminTime = getEstimateJoinMin( searchedTotalSigCount / sampleRatio, indexedTotalSigCount / sampleRatio,
					( fixedInvokes + variableInvokes ) / sampleRatio );
			double totalTime = naiveTime + joinminTime;

			if( DEBUG.JoinHybridON ) {
				stat.add( "Const_Gamma_CountTime_" + thresholdExponent, String.format( "%.2f", searchedTotalSigCount * gamma ) );
				stat.add( "Const_Gamma_SearchedSigCount" + thresholdExponent, String.format( "%.2f", searchedTotalSigCount ) );

				stat.add( "Const_Delta_IndexTime_" + thresholdExponent, String.format( "%.2f", indexedTotalSigCount * delta ) );
				stat.add( "Const_Delta_IndexSigCount_" + thresholdExponent, String.format( "%.2f", indexedTotalSigCount ) );

				stat.add( "Const_Epsilon_JoinTime_" + thresholdExponent,
						String.format( "%.2f", ( fixedInvokes + variableInvokes ) * epsilon ) );
				stat.add( "Const_Epsilon_Predict_" + thresholdExponent, String.format( "%.2f", fixedInvokes + variableInvokes ) );

				stat.add( "Est_Theta_" + thresholdExponent + "_1_NaiveTime", naiveTime );
				stat.add( "Est_Theta_" + thresholdExponent + "_2_JoinMinTime", joinminTime );
				stat.add( "Est_Theta_" + thresholdExponent + "_3_TotalTime", totalTime );

				Util.printLog( "ThresholdId: " + threshold );
				Util.printLog( "Naive Time: " + naiveTime );
				Util.printLog( "JoinMin Time: " + joinminTime );
				Util.printLog( "Total Time: " + totalTime );
			}

			if( bestEstimatedTime > totalTime ) {
				bestEstimatedTime = totalTime;
				bestThreshold = threshold;
			}

			threshold = threshold / 10;
		}

		// if( bestThreshold > 10000 ) {
		// bestThreshold = 10000;
		// }
		stat.add( "Auto_Best_Threshold", bestThreshold );
		return (int) bestThreshold;
	}

	public int findThetaUnrestricted( int qSize, StatContainer stat, long maxIndexedEstNumRecords,
			long maxSearchedEstNumRecords ) {
		// Find the best threshold
		int bestThreshold = 0;
		double bestEstTime = Double.MAX_VALUE;

		// Indicates the minimum indices which have more that 'theta' expanded
		// records
		int indexedIdx = 0;
		int searchedIdx = 0;
		long currentThreshold = Math.min( sampleSearchedList.get( 0 ).getEstNumRecords(),
				sampleIndexedList.get( 0 ).getEstNumRecords() );

		int tableIndexedSize = sampleIndexedList.size();
		int tableSearchedSize = sampleSearchedList.size();

		List<Map<QGram, BinaryCountEntry>> invokes = new ArrayList<Map<QGram, BinaryCountEntry>>();
		List<List<BinaryCountEntry>> indexedPositions = new ArrayList<List<BinaryCountEntry>>();

		// Number of bigrams generated by expanded TL records
		double searchedTotalSigCount = 0;

		for( int recId = 0; recId < tableSearchedSize; recId++ ) {
			Record rec = sampleSearchedList.get( recId );

			List<List<QGram>> availableQGrams = rec.getQGrams( qSize );

			int searchmax = availableQGrams.size();

			for( int i = invokes.size(); i < searchmax; i++ ) {
				invokes.add( new WYK_HashMap<QGram, BinaryCountEntry>() );
			}

			for( int i = 0; i < searchmax; ++i ) {
				Map<QGram, BinaryCountEntry> curridxInvokes = invokes.get( i );

				List<QGram> available = availableQGrams.get( i );
				searchedTotalSigCount += available.size();

				for( QGram qgram : available ) {
					BinaryCountEntry count = curridxInvokes.get( qgram );

					if( count == null ) {
						count = new BinaryCountEntry();
						curridxInvokes.put( qgram, count );
					}

					count.increaseLarge();
				}
			}
		}

		double indexedTotalSigCount = 0;

		for( int recId = 0; recId < tableIndexedSize; recId++ ) {
			Record rec = sampleIndexedList.get( recId );

			List<List<QGram>> availableQGrams = rec.getQGrams( qSize, invokes.size() );

			List<BinaryCountEntry> list = new ArrayList<BinaryCountEntry>();

			long minComparison = Long.MAX_VALUE;
			int minIndex = -1;

			for( int position = 0; position < availableQGrams.size(); position++ ) {
				List<QGram> qgrams = availableQGrams.get( position );

				Map<QGram, BinaryCountEntry> curridxInvokes = invokes.get( position );

				long comparison = 0;

				indexedTotalSigCount += qgrams.size();

				for( QGram qgram : qgrams ) {
					BinaryCountEntry entry = curridxInvokes.get( qgram );
					if( entry != null ) {
						comparison += entry.largeListSize;
					}
				}

				if( minComparison > comparison ) {
					minComparison = comparison;
					minIndex = position;
				}

				if( minComparison == 0 ) {
					break;
				}
			}

			Map<QGram, BinaryCountEntry> minInvokes = invokes.get( minIndex );

			for( QGram qgram : availableQGrams.get( minIndex ) ) {
				BinaryCountEntry entry = minInvokes.get( qgram );
				if( entry != null ) {
					list.add( entry );
				}
			}

			indexedPositions.add( list );
		}

		// Prefix sums
		double currExpSize = 0;
		double currExpLengthSize = 0;

		long maxThreshold = Long.min( maxIndexedEstNumRecords, maxSearchedEstNumRecords );
		int prevAddedIndex = -1;

		if( DEBUG.SampleStatOn ) {
			Util.printLog( "MaxThreshold " + maxThreshold );
		}

		boolean stop = false;
		if( maxThreshold == Long.MAX_VALUE ) {
			stop = true;
		}

		while( currentThreshold <= maxThreshold ) {
			long nextThresholdIndexed = -1;
			long nextThresholdSearched = -1;

			for( ; searchedIdx < tableSearchedSize; searchedIdx++ ) {
				Record rec = sampleSearchedList.get( searchedIdx );

				long est = rec.getEstNumRecords();
				if( est > currentThreshold ) {
					nextThresholdSearched = est;
					break;
				}

				// for naive estimation
				currExpSize += est;

				// for joinmin estimation
				List<List<QGram>> availableQGrams = rec.getQGrams( qSize );
				int searchmax = availableQGrams.size();

				for( int i = 0; i < searchmax; ++i ) {
					Map<QGram, BinaryCountEntry> currPositionalCount = invokes.get( i );

					List<QGram> positionalQGram = availableQGrams.get( i );
					for( QGram qgram : positionalQGram ) {
						BinaryCountEntry count = currPositionalCount.get( qgram );

						count.fromLargeToSmall();
					}
				}
			}

			if( DEBUG.SampleStatOn ) {
				Util.printLog( "searchedIdx " + searchedIdx );
			}

			double removedComparison = 0;

			for( indexedIdx = 0; indexedIdx < tableIndexedSize; indexedIdx++ ) {
				Record rec = sampleIndexedList.get( indexedIdx );

				long est = rec.getEstNumRecords();
				if( est > currentThreshold ) {
					nextThresholdIndexed = est;
					break;
				}

				if( indexedIdx > prevAddedIndex ) {
					// for naive estimation
					currExpSize += est * rec.getTokenArray().length;
				}

				List<BinaryCountEntry> list = indexedPositions.get( indexedIdx );

				for( BinaryCountEntry count : list ) {
					// for joinmin estimation
					removedComparison += count.smallListSize;
				}
			}

			prevAddedIndex = indexedIdx;
			long nextThreshold;

			if( nextThresholdIndexed == -1 && nextThresholdSearched == -1 ) {
				if( stop ) {
					break;
				}
				nextThreshold = maxThreshold + 1;
			}
			else if( nextThresholdIndexed == -1 ) {
				nextThreshold = nextThresholdSearched;
			}
			else if( nextThresholdSearched == -1 ) {
				nextThreshold = nextThresholdIndexed;
			}
			else {
				nextThreshold = Long.min( nextThresholdSearched, nextThresholdIndexed );
			}

			double naiveEstimation = this.getEstimateNaive( currExpLengthSize, currExpSize );
			double joinminEstimation = 0;

			if( nextThreshold == maxThreshold + 1 ) {
				// we compute difference of joinmin estimation time.
				// estimation time of building joinmin index is not added even if it utilizes joinmin index
				// in this case we do not use joinmin index and thus the execution time of building joinmin index is <b>subtracted</b>
				joinminEstimation = this.getEstimateJoinMin( -searchedTotalSigCount, -indexedTotalSigCount, -removedComparison );
			}
			else {
				// we assume that joinmin index built from entire data takes 0 sec.
				// thus, the execution time smaller than that is represented by minus execution time
				joinminEstimation = this.getEstimateJoinMin( 0, 0, -removedComparison );
			}

			if( DEBUG.SampleStatOn ) {
				Util.printLog( "T: " + currentThreshold + " nT: " + nextThreshold + " NT: " + naiveEstimation + " JT: "
						+ joinminEstimation );
			}

			if( bestEstTime > joinminEstimation + naiveEstimation ) {
				bestEstTime = joinminEstimation + naiveEstimation;

				if( currentThreshold < Integer.MAX_VALUE ) {
					bestThreshold = (int) currentThreshold;
				}
				else {
					currentThreshold = Integer.MAX_VALUE;
				}

				if( DEBUG.SampleStatOn ) {
					Util.printLog( "New Best " + bestThreshold );
				}
			}

			currentThreshold = nextThreshold;
		}

		// if( bestThreshold > 10000 ) {
		// bestThreshold = 10000;
		// }
		stat.add( "Auto_Best_Threshold", bestThreshold );
		stat.add( "Auto_BestEst_Time", bestEstTime );
		return bestThreshold;
	}

	public int findThetaUnrestrictedCountAll( int qSize, StatContainer stat, long maxIndexedEstNumRecords,
			long maxSearchedEstNumRecords ) {
		// Find the best threshold
		int bestThreshold = 0;
		double bestEstTime = Double.MAX_VALUE;

		for( int i = 0; i < sampleIndexedList.size(); i++ ) {
			Record rec = sampleIndexedList.get( i );
			System.out.println( rec.getID() + ": " + rec.toString() + " " + rec.getEstNumRecords() + " "
					+ ( rec.getEstNumRecords() * rec.getTokenArray().length ) );
		}

		// Indicates the minimum indices which have more that 'theta' expanded
		// records
		long currentThreshold = Math.min( sampleSearchedList.get( 0 ).getEstNumRecords(),
				sampleIndexedList.get( 0 ).getEstNumRecords() );

		long maxThreshold = Math.max( sampleSearchedList.get( sampleSearchedList.size() - 1 ).getEstNumRecords(),
				sampleIndexedList.get( sampleIndexedList.size() - 1 ).getEstNumRecords() );
		int maxT = (int) Math.min( maxThreshold, 1000 );

		for( int t = (int) currentThreshold; t <= maxT; t++ ) {
			double cost = findThetaUnrestrictedCountAll( qSize, stat, maxIndexedEstNumRecords, maxSearchedEstNumRecords, t );

			if( bestEstTime > cost ) {
				bestEstTime = cost;
				bestThreshold = t;
			}
		}

		System.out.println( "Best t : " + bestThreshold + " time: " + bestEstTime );

		stat.add( "Auto_Best_Threshold", bestThreshold );
		stat.add( "Auto_BestEst_Time", bestEstTime );
		return bestThreshold;
	}

	private double findThetaUnrestrictedCountAll( int qSize, StatContainer stat, long maxIndexedEstNumRecords,
			long maxSearchedEstNumRecords, int threshold ) {
		System.out.println( "[T: " + threshold + "]" );

		List<Map<QGram, BinaryCountEntry>> invokes = new ArrayList<Map<QGram, BinaryCountEntry>>();
		List<List<BinaryCountEntry>> indexedPositions = new ArrayList<List<BinaryCountEntry>>();

		// Number of bigrams generated by expanded TL records
		double searchedTotalSigCount = 0;

		for( int recId = 0; recId < sampleSearchedList.size(); recId++ ) {
			Record rec = sampleSearchedList.get( recId );

			List<List<QGram>> availableQGrams = rec.getQGrams( qSize );

			int searchmax = availableQGrams.size();

			boolean isSmall = rec.getEstNumRecords() <= threshold;

			for( int i = invokes.size(); i < searchmax; i++ ) {
				invokes.add( new WYK_HashMap<QGram, BinaryCountEntry>() );
			}

			for( int i = 0; i < searchmax; ++i ) {
				Map<QGram, BinaryCountEntry> curridxInvokes = invokes.get( i );

				List<QGram> available = availableQGrams.get( i );
				searchedTotalSigCount += available.size();

				for( QGram qgram : available ) {
					BinaryCountEntry count = curridxInvokes.get( qgram );

					if( count == null ) {
						count = new BinaryCountEntry();
						curridxInvokes.put( qgram, count );
					}
					if( isSmall ) {
						count.increaseSmall();
					}
					else {
						count.increaseLarge();
					}
				}
			}
		}

		double indexedTotalSigCount = 0;

		for( int recId = 0; recId < sampleIndexedList.size(); recId++ ) {
			Record rec = sampleIndexedList.get( recId );

			List<List<QGram>> availableQGrams = rec.getQGrams( qSize, invokes.size() );
			List<BinaryCountEntry> list = new ArrayList<BinaryCountEntry>();

			long minComparison = Long.MAX_VALUE;
			int minIndex = -1;

			for( int position = 0; position < availableQGrams.size(); position++ ) {
				List<QGram> qgrams = availableQGrams.get( position );

				Map<QGram, BinaryCountEntry> curridxInvokes = invokes.get( position );

				long comparison = 0;

				indexedTotalSigCount += qgrams.size();

				for( QGram qgram : qgrams ) {
					BinaryCountEntry entry = curridxInvokes.get( qgram );
					if( entry != null ) {
						comparison += entry.largeListSize + entry.smallListSize;
					}
				}

				if( minComparison > comparison ) {
					minComparison = comparison;
					minIndex = position;
				}

				if( minComparison == 0 ) {
					break;
				}
			}

			Map<QGram, BinaryCountEntry> minInvokes = invokes.get( minIndex );

			for( QGram qgram : availableQGrams.get( minIndex ) ) {
				BinaryCountEntry entry = minInvokes.get( qgram );
				if( entry != null ) {
					list.add( entry );
				}
			}

			indexedPositions.add( list );
		}

		// Prefix sums
		double currExpSize = 0;

		for( int recId = 0; recId < sampleSearchedList.size(); recId++ ) {
			Record rec = sampleSearchedList.get( recId );

			if( rec.getEstNumRecords() <= threshold ) {
				currExpSize += rec.getEstNumRecords();
			}
		}

		double currExpLengthSize = 0;

		double estimatedInvokes = 0;

		for( int indexedIdx = 0; indexedIdx < sampleIndexedList.size(); indexedIdx++ ) {
			Record rec = sampleIndexedList.get( indexedIdx );

			long est = rec.getEstNumRecords();
			if( est <= threshold ) {
				// for naive estimation
				currExpLengthSize += est * rec.getTokenArray().length;
			}

			List<BinaryCountEntry> list = indexedPositions.get( indexedIdx );

			for( BinaryCountEntry count : list ) {
				// for joinmin estimation
				estimatedInvokes += count.largeListSize;
			}
		}

		System.out.println( "CurrExpSize : " + currExpSize );
		System.out.println( "CurrExpLengthSize : " + currExpLengthSize );

		double naiveEstimation = this.getEstimateNaive( currExpLengthSize, currExpSize );
		double joinminEstimation = 0;

		System.out.println( "SearchedTotalSigCount : " + searchedTotalSigCount );
		System.out.println( "IndexedTotalSigCount : " + indexedTotalSigCount );
		System.out.println( "EstimatedInvoke : " + estimatedInvokes );

		joinminEstimation = this.getEstimateJoinMin( searchedTotalSigCount, indexedTotalSigCount, estimatedInvokes );

		Util.printLog( String.format( "T: %d  NT: %.2f JT: %.2f TT: %.2f", threshold, naiveEstimation, joinminEstimation,
				( naiveEstimation + joinminEstimation ) ) );

		return joinminEstimation + naiveEstimation;
	}
}
