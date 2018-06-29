package snu.kdd.synonym.synonymRev.estimation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.synonym.synonymRev.algorithm.JoinMH;
import snu.kdd.synonym.synonymRev.algorithm.JoinMin;
import snu.kdd.synonym.synonymRev.algorithm.JoinNaive;
import snu.kdd.synonym.synonymRev.algorithm.misc.EstimationTest;
import snu.kdd.synonym.synonymRev.data.Dataset;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.tools.WYK_HashMap;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class SampleEstimate_Split {

	// alpha: Naive indexing time per transformed strings of table T
	public double alpha;
	// beta: Navie join time per transformed strings of table S
	public double beta;

	// gamma : JoinMH indexing time per pogram of table T
	public double eta;
	// zeta: JoinMH time for counting TPQ supersets per pqgram of table S
	public double zeta;
	// eta : JoinMH join time per record of table T
	public double gamma;

	// lambda : JoinMin indexing time per pqograms of table T
	public double lambda;
	// mu: JoinMin time for counting pqogram time per pqograms of table S
	public double mu;
	// rho: JoinMin join time per record of table T
	public double rho;

	public double sampleRatio;

	Dataset originalSearched;
	Dataset originalIndexed;

	boolean joinMinSelectedHighHigh = false;
	boolean joinMinSelectedLowHigh = false;

	final Query query;
	final Query sampleQuery;
	ObjectArrayList<Record> sampleSearchedList = new ObjectArrayList<Record>();
	ObjectArrayList<Record> sampleIndexedList = new ObjectArrayList<Record>();

	public SampleEstimate_Split( final Query query, double sampleratio, boolean isSelfJoin ) {
		long seed = System.currentTimeMillis();
		Util.printLog( "Random seed: " + seed );
		Random rn = new Random( seed );

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

		Comparator<Record> cmp = new Comparator<Record>() {
			@Override
			public int compare( Record o1, Record o2 ) {
				long est1 = o1.getEstNumTransformed();
				long est2 = o2.getEstNumTransformed();
				return Long.compare( est1, est2 );
			}
		};

		Collections.sort( sampleSearchedList, cmp );
		Collections.sort( sampleIndexedList, cmp );

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
			stat.add( "Stat_Sample Searched size_Min", sampleSearchedList.size() );
			stat.add( "Stat_Sample Indexed size_Min", sampleIndexedList.size() );
		}

		// Infer lambda, mu and rho
		JoinMin joinmininst;
		try {
			joinmininst = new JoinMin( sampleQuery, stat );
			joinmininst.writeResult = false;
			joinmininst.checker = checker;
			joinmininst.qSize = qSize;
			joinmininst.indexK = indexK;

			if( DEBUG.SampleStatON ) {
				Util.printLog( "Joinmininst run" );
			}

			joinmininst.runWithoutPreprocess();

			if( DEBUG.SampleStatON ) {
				Util.printLog( "Joinmininst run done" );
			}

			lambda = joinmininst.getLambda();
			mu = joinmininst.getMu();
			rho = joinmininst.getRho();

			if( DEBUG.SampleStatON ) {
				Util.printLog( "Lambda : " + lambda );
				Util.printLog( "Mu : " + mu );
				Util.printLog( "Rho : " + rho );

				stat.add( "Const_Lambda", String.format( "%.2f", lambda ) );
				stat.add( "Const_Mu", String.format( "%.2f", mu ) );
				stat.add( "Const_Rho", String.format( "%.2f", rho ) );

				stat.add( "Const_EpsilonPrime", String.format( "%.2f", joinmininst.idx.getRhoPrime() ) );
			}
		}
		catch( IOException e ) {
			e.printStackTrace();
		}
	}

	public void estimateJoinMH( StatContainer stat, Validator checker, int indexK, int qSize ) {

		if( DEBUG.SampleStatON ) {
			stat.add( "Stat_Sample Searched size_MH", sampleSearchedList.size() );
			stat.add( "Stat_Sample Indexed size_MH", sampleIndexedList.size() );
		}

		// Infer gamma, zeta and eta
		JoinMH joinmhinst;
		try {
			joinmhinst = new JoinMH( sampleQuery, stat );
			joinmhinst.writeResult = false;
			joinmhinst.checker = checker;
			joinmhinst.qgramSize = qSize;
			joinmhinst.indexK = indexK;

			if( DEBUG.SampleStatON ) {
				Util.printLog( "Joinmininst run" );
			}

			joinmhinst.runAfterPreprocess();

			if( DEBUG.SampleStatON ) {
				Util.printLog( "Joinmh run done" );
			}

			gamma = joinmhinst.getGamma();
			eta = joinmhinst.getEta();
			zeta = joinmhinst.getZeta();

			if( DEBUG.SampleStatON ) {
				Util.printLog( "Gamma : " + gamma );
				Util.printLog( "Zeta : " + zeta );
				Util.printLog( "Eta : " + eta );

				stat.add( "Const_Gamma", String.format( "%.2f", gamma ) );
				stat.add( "Const_Zeta", String.format( "%.2f", zeta ) );
				stat.add( "Const_Eta", String.format( "%.2f", eta ) );
			}
		}
		catch( IOException e ) {
			e.printStackTrace();
		}
	}

	public void estimateJoinMHNaiveWithSample( StatContainer stat, Validator checker, int indexK, int qSize ) {
		estimateJoinMH( stat, checker, indexK, qSize );
		estimateNaive( stat );

		if( DEBUG.PrintEstimationON ) {
			BufferedWriter bw = EstimationTest.getWriter();

			try {
				bw.write( "\n" );
			}
			catch( IOException e ) {
				e.printStackTrace();
			}
		}
	}

	public void estimateJoinMinNaiveWithSample( StatContainer stat, Validator checker, int indexK, int qSize ) {
		estimateJoinMin( stat, checker, indexK, qSize );
		estimateNaive( stat );

		if( DEBUG.PrintEstimationON ) {
			BufferedWriter bw = EstimationTest.getWriter();

			try {
				bw.write( "\n" );
			}
			catch( IOException e ) {
				e.printStackTrace();
			}
		}
	}

	public void estimateJoinHybridWithSample( StatContainer stat, Validator checker, int indexK, int qSize ) {
		estimateJoinMin( stat, checker, indexK, qSize );
		estimateJoinMH( stat, checker, indexK, qSize );
		estimateNaive( stat );

		if( DEBUG.PrintEstimationON ) {
			BufferedWriter bw = EstimationTest.getWriter();

			try {
				bw.write( "\n" );
			}
			catch( IOException e ) {
				e.printStackTrace();
			}
		}
	}

	public double getEstimateNaive( double totalExpLengthIndex, double totalExpJoin ) {
		// computes estimated execution time of JoinNaive
		if( DEBUG.SampleStatON ) {
			Util.printLog( "totalExpLength " + totalExpLengthIndex + ", TotalExp " + totalExpJoin );
			Util.printLog( "Naive Index Time " + ( alpha * totalExpLengthIndex ) );
			Util.printLog( "Naive Join Time " + ( beta * totalExpJoin ) );
		}

		if( DEBUG.PrintEstimationON ) {
			if( DEBUG.PrintEstimationON ) {
				BufferedWriter bwEstimation = EstimationTest.getWriter();
				try {
					bwEstimation.write( "[Alpha] " + alpha + " IndexTime " + ( alpha * totalExpLengthIndex ) + " totalExpLength "
							+ totalExpLengthIndex + "\n" );
					bwEstimation.write(
							"[Beta] " + beta + " JoinTime " + ( beta * totalExpJoin ) + " TotalExp " + totalExpJoin + "\n" );
				}
				catch( IOException e ) {
					e.printStackTrace();
				}
			}
		}

		return alpha * totalExpLengthIndex + beta * totalExpJoin;
	}

	public double getEstimateJoinMin( double searchedTotalSigCount, double indexedTotalSigCount, double estimatedInvokes ) {
		if( DEBUG.SampleStatON ) {
			Util.printLog( "SearchedSigCount " + searchedTotalSigCount + ", IndexedSigCount " + indexedTotalSigCount
					+ " PredictCount " + estimatedInvokes );
			Util.printLog( "JoinMin Count Time " + ( searchedTotalSigCount * mu ) );
			Util.printLog( "JoinMin Index Time " + ( indexedTotalSigCount * lambda ) );
			Util.printLog( "JoinMin Join Time " + ( estimatedInvokes * rho ) );
		}

		if( DEBUG.PrintEstimationON ) {
			if( DEBUG.PrintEstimationON ) {
				BufferedWriter bwEstimation = EstimationTest.getWriter();
				try {
					bwEstimation.write( "[Gamma] " + mu + " CountTime " + ( mu * searchedTotalSigCount )
							+ " SearchedSigCount " + searchedTotalSigCount + "\n" );
					bwEstimation.write( "[Delta] " + lambda + " IndexTime " + ( lambda * indexedTotalSigCount )
							+ " IndexedSigCount " + indexedTotalSigCount + "\n" );
					bwEstimation.write( "[Epsilon] " + rho + " JoinTime " + ( rho * estimatedInvokes ) + " PredictCount "
							+ estimatedInvokes + "\n" );
				}
				catch( IOException e ) {
					e.printStackTrace();
				}
			}
		}

		return mu * searchedTotalSigCount + lambda * indexedTotalSigCount + rho * estimatedInvokes;
	}

	public double getEstimateJoinMH( double searchedTotalSigCount, double indexedTotalSigCount, double estimatedInvokes ) {
		if( DEBUG.SampleStatON ) {
			Util.printLog( "IndexedSigCount " + indexedTotalSigCount + " PredictCount " + estimatedInvokes );
			Util.printLog( "JoinMH Index Time " + ( indexedTotalSigCount * gamma ) );
			Util.printLog( "JoinMH count time " + ( searchedTotalSigCount * zeta ) );
			Util.printLog( "JoinMH Join Time " + ( estimatedInvokes * eta ) );
		}

		if( DEBUG.PrintEstimationON ) {
			BufferedWriter bwEstimation = EstimationTest.getWriter();
			try {
				bwEstimation.write( "[Gamma] " + gamma + " IndexTime " + ( gamma * indexedTotalSigCount ) + " IndexedSigCount "
						+ indexedTotalSigCount + "\n" );
				bwEstimation.write( "[Zeta] " + zeta + " QgramTime " + ( zeta * searchedTotalSigCount )
						+ " searchedTotalSigCount " + searchedTotalSigCount + "\n" );
				bwEstimation.write( "[Eta] " + eta + " JoinTime " + ( eta * estimatedInvokes ) + " PredictCount "
						+ estimatedInvokes + "\n" );
			}
			catch( IOException e ) {
				e.printStackTrace();
			}
		}

		return gamma * indexedTotalSigCount + zeta * searchedTotalSigCount + eta * estimatedInvokes;
	}

	public int findThetaJoinMinNaive( int qSize, StatContainer stat, long maxIndexedEstNumRecords, long maxSearchedEstNumRecords,
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
			if( currentThreshold > 100000 ) {
				Util.printLog( "Current Threshold is more than 100000" );
				break;
			}

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

			if( DEBUG.PrintEstimationON ) {
				BufferedWriter bw = EstimationTest.getWriter();

				try {
					bw.write( "Estimation " + currentThreshold + "\n" );
				}
				catch( IOException e ) {
					e.printStackTrace();
				}
			}

			double joinminEstimation = this.getEstimateJoinMin( searchedTotalSigCount, indexedTotalSigCount,
					totalInvokes - removedComparison );

			double naiveEstimation = this.getEstimateNaive( currExpLengthSize, currExpSize );

			if( DEBUG.PrintEstimationON ) {
				BufferedWriter bw = EstimationTest.getWriter();

				try {
					bw.write( "\n" );
				}
				catch( IOException e ) {
					e.printStackTrace();
				}
			}

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

		// if( sampleIndexedList.size() > 100 ) {
		// double naiveOnlyEstimation = this.getEstimateNaive( currExpLengthSize, currExpSize );
		// if( bestEstTime > naiveOnlyEstimation ) {
		// bestEstTime = naiveOnlyEstimation;
		// bestThreshold = Integer.MAX_VALUE;
		// }
		// if( DEBUG.SampleStatON ) {
		// Util.printLog( String.format( "T: %d TT: %.2f", Integer.MAX_VALUE, naiveOnlyEstimation ) );
		// }
		// }

		if( bestThreshold > 100000 ) {
			bestThreshold = 100000;
		}
		stat.add( "Auto_Best_Threshold", bestThreshold );
		stat.add( "Auto_Best_Estimated_Time", bestEstTime );
		return bestThreshold;
	}

	public int findThetaJoinMHNaive( int qSize, int indexK, StatContainer stat, long maxIndexedEstNumRecords,
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
			// for each record
			Record rec = sampleSearchedList.get( recId );

			List<List<QGram>> availableQGrams = rec.getQGrams( qSize, indexK );

			for( int i = invokes.size(); i < availableQGrams.size(); i++ ) {
				// expand invokes if it is smaller than avaiableQGrams.size()
				invokes.add( new WYK_HashMap<QGram, BinaryCountEntry>() );
			}

			for( int i = 0; i < availableQGrams.size(); ++i ) {
				// count the numbers of occurrences of a positional q-grams
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
				availableQGrams = rec.getSelfQGrams( qSize, indexK );
			}
			else {
				availableQGrams = rec.getQGrams( qSize, indexK );
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
			if( currentThreshold > 100000 ) {
				Util.printLog( "Current Threshold is more than 100000" );
				break;
			}

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

				// for joinmh estimation
				List<List<QGram>> availableQGrams = rec.getQGrams( qSize, indexK );
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

			if( DEBUG.PrintEstimationON ) {
				BufferedWriter bw = EstimationTest.getWriter();

				try {
					bw.write( "Estimation " + currentThreshold + "\n" );
				}
				catch( IOException e ) {
					e.printStackTrace();
				}
			}

			double joinmhEstimation = this.getEstimateJoinMH( searchedTotalSigCount, indexedTotalSigCount,
					totalInvokes - removedComparison );

			double naiveEstimation = this.getEstimateNaive( currExpLengthSize, currExpSize );

			if( DEBUG.PrintEstimationON ) {
				BufferedWriter bw = EstimationTest.getWriter();

				try {
					bw.write( "\n" );
				}
				catch( IOException e ) {
					e.printStackTrace();
				}
			}

			if( DEBUG.SampleStatON ) {
				Util.printLog( String.format( "T: %d nT: %d NT: %.2f JT: %.2f TT: %.2f", currentThreshold, nextThreshold,
						naiveEstimation, joinmhEstimation, naiveEstimation + joinmhEstimation ) );
			}

			if( bestEstTime > joinmhEstimation + naiveEstimation ) {
				bestEstTime = joinmhEstimation + naiveEstimation;

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

		// if( sampleIndexedList.size() > 100 ) {
		// double naiveOnlyEstimation = this.getEstimateNaive( currExpLengthSize, currExpSize );
		// if( bestEstTime > naiveOnlyEstimation ) {
		// bestEstTime = naiveOnlyEstimation;
		// bestThreshold = Integer.MAX_VALUE;
		// }
		// if( DEBUG.SampleStatON ) {
		// Util.printLog( String.format( "T: %d TT: %.2f", Integer.MAX_VALUE, naiveOnlyEstimation ) );
		// }
		// }

		if( bestThreshold > 100000 ) {
			bestThreshold = 100000;
		}
		stat.add( "Auto_Best_Threshold", bestThreshold );
		stat.add( "Auto_Best_Estimated_Time", bestEstTime );
		return bestThreshold;
	}

	public int findThetaJoinHybridAll( int qSize, int indexK, StatContainer stat, long maxIndexedEstNumRecords,
			long maxSearchedEstNumRecords, boolean oneSideJoin ) {
		// TODO: Estimation needs to be computed by using 2 different indexes
		
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
		List<List<BinaryCountEntry>> indexedJoinMinPositions = new ArrayList<List<BinaryCountEntry>>();
		List<List<BinaryCountEntry>> indexedJoinMHPositions = new ArrayList<List<BinaryCountEntry>>();

		// Number of bigrams generated by expanded TL records
		long searchedTotalSigCount = 0; // Original count
		
		// build invokes for counting, Same as SampleEstimate
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
		
		// For Naive
		double currExpLengthSize = 0;
		
		// Original
		double joinMHIndexedSigCount = 0;
		long indexedTotalSigCount = 0;
		double totalJoinMinInvokes = 0;
		double totalJoinMHInvokes = 0;
		
		// Added for HybridJoin
		long removedSearchedSigCount = 0;
		List<Long> comparisonMHList = new ArrayList<Long>();
		List<Long> comparisonMinList = new ArrayList<Long>();
		
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

			List<BinaryCountEntry> joinMinList = new ArrayList<BinaryCountEntry>();
			List<BinaryCountEntry> joinMHList = new ArrayList<BinaryCountEntry>();

			long minJoinMinComparison = Long.MAX_VALUE;
			long minJoinMHComparison = Long.MAX_VALUE;
			int minJoinMinIndex = -1;
			int minJoinMHIndex = -1;

			for( int position = 0; position < availableQGrams.size(); position++ ) {
				List<QGram> qgrams = availableQGrams.get( position );

				Map<QGram, BinaryCountEntry> curridxInvokes = invokes.get( position );

				long comparison = 0;
				if( position < indexK ) {
					joinMHIndexedSigCount += qgrams.size();
				}
				indexedTotalSigCount += qgrams.size();

				for( QGram qgram : qgrams ) {
					BinaryCountEntry entry = curridxInvokes.get( qgram );
					if( entry != null ) {
						comparison += entry.largeListSize;
					}
				}
				
				if( minJoinMinComparison > comparison ) {
					if( position < indexK ) {
						minJoinMHComparison = comparison;
						minJoinMHIndex = position;
					}
					minJoinMinComparison = comparison;
					minJoinMinIndex = position;
				}

				if( minJoinMinComparison == 0 ) {
					break;
				}
			}
			
			// TODO: Question
			// Min indexes are fixed without considering threshold. Is it correct?
			Map<QGram, BinaryCountEntry> minJoinMinInvokes = invokes.get( minJoinMinIndex );
			Map<QGram, BinaryCountEntry> minJoinMHInvokes = invokes.get( minJoinMHIndex );
			
			// Added
			comparisonMinList.add(minJoinMinComparison);
			comparisonMHList.add(minJoinMHComparison);
			
			totalJoinMinInvokes += minJoinMinComparison;
			totalJoinMHInvokes += minJoinMHComparison;

			for( QGram qgram : availableQGrams.get( minJoinMinIndex ) ) {
				BinaryCountEntry entry = minJoinMinInvokes.get( qgram );
				if( entry != null ) {
					joinMinList.add( entry );
				}
			}

			for( QGram qgram : availableQGrams.get( minJoinMHIndex ) ) {
				BinaryCountEntry entry = minJoinMHInvokes.get( qgram );
				if( entry != null ) {
					joinMHList.add( entry );
				}
			}

			indexedJoinMinPositions.add( joinMinList );
			indexedJoinMHPositions.add( joinMHList );
		}

		// Prefix sums
		double currExpSize = 0;

		// Added
		long removedMinTwoThree = 0;
		long removedMHTwoThree = 0;
		
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
			if( currentThreshold > 100000 ) {
				Util.printLog( "Current Threshold is more than 100000" );
				break;
			}

			long nextThresholdIndexed = -1;
			long nextThresholdSearched = -1;
			
			// Incremental Calculation
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
					// From Searched
					Map<QGram, BinaryCountEntry> currPositionalCount = invokes.get( i );
					// From Searched
					List<QGram> positionalQGram = availableQGrams.get( i );
					
					// Added
					removedSearchedSigCount += positionalQGram.size();
					
					for( QGram qgram : positionalQGram ) {
						BinaryCountEntry count = currPositionalCount.get( qgram );
						count.fromLargeToSmall();
					}
				}
			}

			if( DEBUG.SampleStatON ) {
				Util.printLog( "searchedIdx " + searchedIdx );
			}

			double removedJoinMinComparison = 0;
			double removedJoinMHComparison = 0;

			// Added
			long removedIndexedSigCount = 0;
			long removedMHIndexedSigCount = 0;

			// Non-Incremental Way!
			for( indexedIdx = 0; indexedIdx < tableIndexedSize; indexedIdx++ ) {
				Record rec = sampleIndexedList.get( indexedIdx );

				long est = rec.getEstNumTransformed();
				if( est > currentThreshold ) {
					nextThresholdIndexed = est;
					indexedIdx--;
					break;
				}

				
				// Added 
				// To calculate removedIndexedSigCount and removedMHIndexedSigCount
				List<List<QGram>> availableQGrams = null;
				if( oneSideJoin ) {
					availableQGrams = rec.getSelfQGrams( qSize, invokes.size() );
				}
				else {
					availableQGrams = rec.getQGrams( qSize, invokes.size() );
				}
				for( int position = 0; position < availableQGrams.size(); position++ ) {
					List<QGram> qgrams = availableQGrams.get( position );
					if( position < indexK ) {
						removedMHIndexedSigCount += qgrams.size();
					}
					removedIndexedSigCount += qgrams.size();
				}
				
				
				
				if( indexedIdx > prevAddedIndex ) {
					// for naive estimation
					if( !oneSideJoin ) { // TODO: Question: Why it runs only when it is not a oneSideJoin?
						currExpLengthSize += est * rec.getTokenCount();
					}
				}

				List<BinaryCountEntry> list = indexedJoinMinPositions.get( indexedIdx );

				for( BinaryCountEntry count : list ) {
					// for joinmin estimation
					removedJoinMinComparison += count.smallListSize;
				}

				list = indexedJoinMHPositions.get( indexedIdx );
				for( BinaryCountEntry count : list ) {
					// for joinmh estimation
					removedJoinMHComparison += count.smallListSize;
				}
			}

			// Added
			// Implementing....
			// TODO::Estimation of 2 + 3
			Util.printLog(String.format( "prevIdx: %d, Idx: %d, currThreshold: %d", prevAddedIndex, indexedIdx, currentThreshold ));
			for( int index = prevAddedIndex+1; index <= indexedIdx && index < tableIndexedSize; index++ ) {
				removedMinTwoThree += comparisonMinList.get(index); 
				removedMHTwoThree += comparisonMHList.get(index); 
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

			if( DEBUG.PrintEstimationON ) {
				BufferedWriter bw = EstimationTest.getWriter();

				try {
					bw.write( "Estimation " + currentThreshold + "\n" );
				}
				catch( IOException e ) {
					e.printStackTrace();
				}
			}
			
			/*
			double joinminEstimationHighHigh = this.getEstimateJoinMin( searchedTotalSigCount, indexedTotalSigCount,
					totalJoinMinInvokes - removedJoinMinComparison );
			double joinmhEstimationHighHigh = this.getEstimateJoinMH( searchedTotalSigCount, joinMHIndexedSigCount,
					totalJoinMHInvokes - removedJoinMHComparison );

			double joinminEstimationLowHigh = 0;
			double joinmhEstimationLowHigh = 0;
			*/
			
			// Added for HybridJoin
			double joinminEstimationHighHigh = this.getEstimateJoinMin( searchedTotalSigCount, indexedTotalSigCount - removedIndexedSigCount,
					totalJoinMinInvokes- removedMinTwoThree );
			double joinmhEstimationHighHigh = this.getEstimateJoinMH( searchedTotalSigCount, joinMHIndexedSigCount - removedMHIndexedSigCount,
					totalJoinMHInvokes- removedMHTwoThree );
			
			double joinminEstimationLowHigh = this.getEstimateJoinMin( searchedTotalSigCount - removedSearchedSigCount, removedIndexedSigCount,
					removedMinTwoThree - removedJoinMinComparison );
			double joinmhEstimationLowHigh = this.getEstimateJoinMH( searchedTotalSigCount - removedSearchedSigCount, removedMHIndexedSigCount,
					removedMHTwoThree - removedJoinMHComparison );
			

			boolean tempJoinMinSelectedHighHigh = joinminEstimationHighHigh < joinmhEstimationHighHigh;
			boolean tempJoinMinSelectedLowHigh = joinminEstimationLowHigh < joinmhEstimationLowHigh;

			double naiveEstimation = this.getEstimateNaive( currExpLengthSize, currExpSize );

			if( DEBUG.PrintEstimationON ) {
				BufferedWriter bw = EstimationTest.getWriter();

				try {
					bw.write( "\n" );
				}
				catch( IOException e ) {
					e.printStackTrace();
				}
			}
			
			// Added
			Util.printLog("Min");
			Util.printLog( String.format( "COMPARISON MinTotal: %.1f 23_removed: %d 12_removed: %.1f", totalJoinMinInvokes,
					removedMinTwoThree, removedJoinMinComparison ) );
			Util.printLog( String.format( "TIME Naive: %.1f LowHigh: %.1f HighHigh: %.1f", naiveEstimation, joinminEstimationLowHigh,
					joinminEstimationHighHigh) );
			Util.printLog("MH");
			Util.printLog( String.format( "COMPARISON MinTotal: %.1f 23_removed: %d 12_removed: %.1f", totalJoinMHInvokes,
					removedMHTwoThree, removedJoinMHComparison ) );
			Util.printLog( String.format( "TIME Naive: %.1f LowHigh: %.1f HighHigh: %.1f", naiveEstimation, joinmhEstimationLowHigh,
					joinmhEstimationHighHigh) );
			
			Util.printLog( String.format( "TIME COMPARISON Min %.1f VS MH %.1f", (naiveEstimation+joinminEstimationLowHigh+joinminEstimationHighHigh)/10000,
					(naiveEstimation+joinmhEstimationLowHigh+joinmhEstimationHighHigh)/10000) );
			
			
			Util.printLog( String.format( "INDEX_SIZE S_Total: %d S_removed: %d I_Total: %d I_Min_removed: %d I_MH_removed: %d", searchedTotalSigCount, removedSearchedSigCount,
					indexedTotalSigCount, removedIndexedSigCount, removedMHIndexedSigCount ) );
			
			
			Util.printLog( String.format( "T: %d nT: %d NT: %.2f JT: %.2f TT: %.2f", currentThreshold, nextThreshold,
					naiveEstimation, joinminEstimationHighHigh, naiveEstimation + joinminEstimationHighHigh ) );
			Util.printLog( String.format( "T: %d nT: %d NT: %.2f JT: %.2f TT: %.2f", currentThreshold, nextThreshold,
					naiveEstimation, joinmhEstimationHighHigh, naiveEstimation + joinmhEstimationHighHigh ) );
			Util.printLog( "JoinMin Selected HighHigh & Low High" + tempJoinMinSelectedHighHigh + tempJoinMinSelectedLowHigh);
			//Util.printLog( "JoinMin Selected LowHigh " +  );
			
			double tempBestTime = naiveEstimation;

			if( tempJoinMinSelectedHighHigh ) {
				tempBestTime += joinminEstimationHighHigh;
			}
			else {
				tempBestTime += joinmhEstimationHighHigh;
			}
			
			if( tempJoinMinSelectedLowHigh ) {
				tempBestTime += joinminEstimationLowHigh;
			}
			else {
				tempBestTime += joinmhEstimationLowHigh;
			}

			if( bestEstTime > tempBestTime ) {
				bestEstTime = tempBestTime;
				joinMinSelectedHighHigh = tempJoinMinSelectedHighHigh;
				joinMinSelectedLowHigh = tempJoinMinSelectedLowHigh;
				
				if( currentThreshold < Integer.MAX_VALUE ) {
					bestThreshold = (int) currentThreshold;
				}
				else {
					currentThreshold = Integer.MAX_VALUE;
				}

				if( DEBUG.SampleStatON ) {
					Util.printLog( "New Best " + bestThreshold + " with " + joinMinSelectedHighHigh + " and " + joinMinSelectedLowHigh);
				}
			}

			currentThreshold = nextThreshold;
		}

		// if( sampleIndexedList.size() > 100 ) {
		// double naiveOnlyEstimation = this.getEstimateNaive( currExpLengthSize, currExpSize );
		// if( bestEstTime > naiveOnlyEstimation ) {
		// bestEstTime = naiveOnlyEstimation;
		// bestThreshold = Integer.MAX_VALUE;
		// }
		// if( DEBUG.SampleStatON ) {
		// Util.printLog( String.format( "T: %d TT: %.2f", Integer.MAX_VALUE, naiveOnlyEstimation ) );
		// }
		// }

		stat.add( "Auto_Best_Threshold", bestThreshold );
		stat.add( "Auto_Best_Estimated_Time", bestEstTime );
		stat.add( "Auto_JoinMin_Selected_HighHigh", "" + joinMinSelectedHighHigh );
		stat.add( "Auto_JoinMin_Selected_LowHigh", "" + joinMinSelectedLowHigh );
		return bestThreshold;
	}

	public boolean getJoinMinSelectedHighHigh() {
		return joinMinSelectedHighHigh;
	}
	
	public boolean getJoinMinSelectedLowHigh() {
		return joinMinSelectedLowHigh;
	}
}
