package snu.kdd.synonym.synonymRev.estimation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.synonym.synonymRev.algorithm.JoinMin;
import snu.kdd.synonym.synonymRev.algorithm.JoinNaive;
import snu.kdd.synonym.synonymRev.data.Dataset;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.tools.WYK_HashMap;
import snu.kdd.synonym.synonymRev.validator.Validator;

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

	Dataset originalSearched;
	Dataset originalIndexed;

	final Query query;
	final Query sampleQuery;
	ObjectArrayList<Record> sampleSearchedList = new ObjectArrayList<Record>();
	ObjectArrayList<Record> sampleIndexedList = new ObjectArrayList<Record>();

	public SampleEstimate( final Query query, double sampleratio, boolean isSelfJoin ) {
		Random rn = new Random( 0 );

		this.query = query;

		int smallTableSize = Integer.min( query.searchedSet.size(), query.indexedSet.size() );
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

		for( Record r : query.searchedSet.get() ) {
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
			for( Record s : query.indexedSet.get() ) {
				if( rn.nextDouble() < this.sampleRatio ) {
					sampleIndexedList.add( s );
				}
			}
		}

		Util.printLog( sampleSearchedList.size() + " Searched records are sampled" );
		Util.printLog( sampleIndexedList.size() + " Indexed records are sampled" );

		Dataset sampleIndexed = new Dataset( sampleIndexedList );
		Dataset sampleSearched = new Dataset( sampleSearchedList );
		sampleQuery = new Query( query.ruleSet, sampleIndexed, sampleSearched, query.tokenIndex, query.oneSideJoin,
				query.selfJoin );

	}

	public void estimateNaive( StatContainer stat ) {

		// Infer alpha and beta
		JoinNaive naiveinst;
		try {
			naiveinst = new JoinNaive( sampleQuery, stat );
			naiveinst.threshold = 100;
			naiveinst.runAfterPreprocess( false );
			alpha = naiveinst.getAlpha();
			beta = naiveinst.getBeta();

			if( DEBUG.SampleStatON ) {
				Util.printLog( "Alpha : " + alpha );
				Util.printLog( "Beta : " + beta );

				stat.add( "Const_Alpha", String.format( "%.2f", alpha ) );
				stat.add( "Const_Beta", String.format( "%.2f", beta ) );
			}
		}
		catch( IOException e ) {
			e.printStackTrace();
		}
	}

	public void estimateJoinMin( StatContainer stat, Validator checker, int indexK, int qSize ) {

		if( DEBUG.SampleStatON ) {
			stat.add( "Stat_Sample Searched size", sampleSearchedList.size() );
			stat.add( "Stat_Sample Indexed size", sampleIndexedList.size() );
		}

		// Infer gamma, delta and epsilon
		JoinMin joinmininst;
		try {
			joinmininst = new JoinMin( sampleQuery, stat );
			joinmininst.checker = checker;
			joinmininst.qSize = qSize;
			joinmininst.indexK = indexK;

			if( DEBUG.SampleStatON ) {
				Util.printLog( "Joinmininst run" );
			}

			joinmininst.runWithoutPreprocess( false );

			if( DEBUG.SampleStatON ) {
				Util.printLog( "Joinmininst run done" );
			}

			gamma = joinmininst.getGamma();
			delta = joinmininst.getDelta();
			epsilon = joinmininst.getEpsilon();

			if( DEBUG.SampleStatON ) {
				Util.printLog( "Gamma : " + gamma );
				Util.printLog( "Delta : " + delta );
				Util.printLog( "Epsilon : " + epsilon );

				stat.add( "Const_Gamma", String.format( "%.2f", gamma ) );
				stat.add( "Const_Delta", String.format( "%.2f", delta ) );
				stat.add( "Const_Epsilon", String.format( "%.2f", epsilon ) );

				stat.add( "Const_EpsilonPrime", String.format( "%.2f", joinmininst.idx.epsilonPrime ) );
			}
		}
		catch( IOException e ) {
			e.printStackTrace();
		}
	}

	public void estimateWithSample( StatContainer stat, Validator checker, int indexK, int qSize ) {
		estimateNaive( stat );
		estimateJoinMin( stat, checker, indexK, qSize );
	}

	public double getEstimateNaive( double totalExpLengthIndex, double totalExpJoin ) {
		return alpha * totalExpLengthIndex + beta * totalExpJoin;
	}

	public double getEstimateJoinMin( double searchedTotalSigCount, double indexedTotalSigCount, double estimatedInvokes ) {
		return gamma * searchedTotalSigCount + delta * indexedTotalSigCount + epsilon * estimatedInvokes;
	}

	public int findThetaUnrestricted( int qSize, StatContainer stat, long maxIndexedEstNumRecords, long maxSearchedEstNumRecords,
			boolean oneSideJoin ) {
		// Find the best threshold
		int bestThreshold = 0;
		double bestEstTime = Double.MAX_VALUE;

		// Indicates the minimum indices which have more that 'theta' expanded
		// records
		int indexedIdx = 0;
		int searchedIdx = 0;
		long currentThreshold = Math.min( sampleSearchedList.get( 0 ).getEstNumTransformed(),
				sampleIndexedList.get( 0 ).getEstNumTransformed() );

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
		double totalInvokes = 0;
		double currExpLengthSize = 0;

		for( int recId = 0; recId < tableIndexedSize; recId++ ) {
			Record rec = sampleIndexedList.get( recId );

			if( oneSideJoin ) {
				currExpLengthSize += rec.getTokenCount();
			}

			List<List<QGram>> availableQGrams = null;
			if( oneSideJoin ) {
				availableQGrams = rec.getSelfQGrams( qSize, invokes.size() );
			}
			else {
				availableQGrams = rec.getQGrams( qSize, invokes.size() );
			}

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

			totalInvokes += minComparison;

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

		long maxThreshold = Long.min( maxIndexedEstNumRecords, maxSearchedEstNumRecords );
		int prevAddedIndex = -1;

		if( DEBUG.SampleStatON ) {
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

				long est = rec.getEstNumTransformed();
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

			if( DEBUG.SampleStatON ) {
				Util.printLog( "searchedIdx " + searchedIdx );
			}

			double removedComparison = 0;

			for( indexedIdx = 0; indexedIdx < tableIndexedSize; indexedIdx++ ) {
				Record rec = sampleIndexedList.get( indexedIdx );

				long est = rec.getEstNumTransformed();
				if( est > currentThreshold ) {
					nextThresholdIndexed = est;
					indexedIdx--;
					break;
				}

				if( indexedIdx > prevAddedIndex ) {
					// for naive estimation

					if( !oneSideJoin ) {
						currExpLengthSize += est * rec.getTokenCount();
					}
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

			joinminEstimation = this.getEstimateJoinMin( searchedTotalSigCount, indexedTotalSigCount,
					totalInvokes - removedComparison );

			if( DEBUG.SampleStatON ) {
				Util.printLog( String.format( "T: %d nT: %d NT: %.2f JT: %.2f TT: %.2f", currentThreshold, nextThreshold,
						naiveEstimation, joinminEstimation, naiveEstimation + joinminEstimation ) );
			}

			if( bestEstTime > joinminEstimation + naiveEstimation ) {
				bestEstTime = joinminEstimation + naiveEstimation;

				if( currentThreshold < Integer.MAX_VALUE ) {
					bestThreshold = (int) currentThreshold;
				}
				else {
					currentThreshold = Integer.MAX_VALUE;
				}

				if( DEBUG.SampleStatON ) {
					Util.printLog( "New Best " + bestThreshold );
				}
			}

			currentThreshold = nextThreshold;
		}

		if( sampleIndexedList.size() > 100 ) {
			double naiveOnlyEstimation = this.getEstimateNaive( currExpLengthSize, currExpSize );
			if( bestEstTime > naiveOnlyEstimation ) {
				bestEstTime = naiveOnlyEstimation;
				bestThreshold = Integer.MAX_VALUE;
			}
			if( DEBUG.SampleStatON ) {
				Util.printLog( String.format( "T: %d TT: %.2f", Integer.MAX_VALUE, naiveOnlyEstimation ) );
			}
		}

		if( bestThreshold > 1000 ) {
			bestThreshold = 1000;
		}
		stat.add( "Auto_Best_Threshold", bestThreshold );
		stat.add( "Auto_Best_Estimated_Time", bestEstTime );
		return bestThreshold;

	}

	public int findThetaUnrestrictedDebug( int qSize, StatContainer stat, long maxIndexedEstNumRecords,
			long maxSearchedEstNumRecords, boolean oneSideJoin ) {
		// Find the best threshold
		int bestThreshold = 0;
		double bestEstTime = Double.MAX_VALUE;

		// Indicates the minimum indices which have more that 'theta' expanded
		// records
		int indexedIdx = 0;
		int searchedIdx = 0;
		long currentThreshold = Math.min( sampleSearchedList.get( 0 ).getEstNumTransformed(),
				sampleIndexedList.get( 0 ).getEstNumTransformed() );

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
		double totalInvokes = 0;
		double currExpLengthSize = 0;

		for( int recId = 0; recId < tableIndexedSize; recId++ ) {
			Record rec = sampleIndexedList.get( recId );

			if( oneSideJoin ) {
				currExpLengthSize += rec.getTokenCount();
			}

			List<List<QGram>> availableQGrams = null;
			if( oneSideJoin ) {
				availableQGrams = rec.getSelfQGrams( qSize, invokes.size() );
			}
			else {
				availableQGrams = rec.getQGrams( qSize, invokes.size() );
			}

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

			totalInvokes += minComparison;

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

		long maxThreshold = Long.min( maxIndexedEstNumRecords, maxSearchedEstNumRecords );
		int prevAddedIndex = -1;

		Util.printLog( "MaxThreshold " + maxThreshold );

		boolean stop = false;
		if( maxThreshold == Long.MAX_VALUE ) {
			stop = true;
		}

		while( currentThreshold <= maxThreshold ) {
			System.out.println( "[T: " + currentThreshold + "]" );
			long nextThresholdIndexed = -1;
			long nextThresholdSearched = -1;

			for( ; searchedIdx < tableSearchedSize; searchedIdx++ ) {
				Record rec = sampleSearchedList.get( searchedIdx );

				long est = rec.getEstNumTransformed();
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

			Util.printLog( "searchedIdx " + searchedIdx );

			double removedComparison = 0;

			for( indexedIdx = 0; indexedIdx < tableIndexedSize; indexedIdx++ ) {
				Record rec = sampleIndexedList.get( indexedIdx );

				long est = rec.getEstNumTransformed();
				if( est > currentThreshold ) {
					nextThresholdIndexed = est;
					indexedIdx--;
					break;
				}

				if( indexedIdx > prevAddedIndex ) {
					// for naive estimation
					// System.out.println(
					// "Adding " + indexedIdx + " est " + est + " estLength " + ( est * rec.getTokenArray().length ) );
					if( !oneSideJoin ) {
						currExpLengthSize += est * rec.getTokenCount();
					}
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

			// we assume that joinmin index built from entire data takes 0 sec.
			// thus, the execution time smaller than that is represented by minus execution time
			joinminEstimation = this.getEstimateJoinMin( searchedTotalSigCount, indexedTotalSigCount,
					totalInvokes - removedComparison );

			System.out.println( "CurrExpSize : " + currExpSize );
			System.out.println( "CurrExpLengthSize : " + currExpLengthSize );
			System.out.println( "SearchedTotalSigCount : " + searchedTotalSigCount );
			System.out.println( "IndexedTotalSigCount : " + indexedTotalSigCount );
			System.out.println( "EstimatedInvoke : " + ( totalInvokes - removedComparison ) );

			Util.printLog( String.format( "T: %d nT: %d NT: %.2f JT: %.2f TT: %.2f", currentThreshold, nextThreshold,
					naiveEstimation, joinminEstimation, naiveEstimation + joinminEstimation ) );

			if( bestEstTime > joinminEstimation + naiveEstimation ) {
				bestEstTime = joinminEstimation + naiveEstimation;

				if( currentThreshold < Integer.MAX_VALUE ) {
					bestThreshold = (int) currentThreshold;
				}
				else {
					currentThreshold = Integer.MAX_VALUE;
				}

				Util.printLog( "New Best " + bestThreshold );
			}

			currentThreshold = nextThreshold;
		}

		System.out.println( "CurrExpSize : " + currExpSize );
		System.out.println( "CurrExpLengthSize : " + currExpLengthSize );

		double naiveOnlyEstimation = this.getEstimateNaive( currExpLengthSize, currExpSize );
		if( bestEstTime > naiveOnlyEstimation ) {
			bestEstTime = naiveOnlyEstimation;
			bestThreshold = Integer.MAX_VALUE;
		}

		// if( bestThreshold > 10000 ) {
		// bestThreshold = 10000;
		// }
		Util.printLog( "Auto_Best_Threshold: " + bestThreshold );
		Util.printLog( "Auto_BestEst_Time: " + bestEstTime );
		return bestThreshold;
	}
}
