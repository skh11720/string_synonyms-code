package snu.kdd.synonym.estimation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import mine.Record;
import snu.kdd.synonym.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.algorithm.JoinMin_Q;
import snu.kdd.synonym.algorithm.JoinNaive1;
import snu.kdd.synonym.tools.StatContainer;
import snu.kdd.synonym.tools.Util;
import tools.DEBUG;
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

	List<Record> originalSearched;
	List<Record> originalIndexed;

	List<Record> sampleSearchedList = new ArrayList<Record>();
	List<Record> sampleIndexedList = new ArrayList<Record>();

	public SampleEstimate( final List<Record> tableSearched, final List<Record> tableIndexed, double sampleratio,
			boolean isSelfJoin ) {
		Random rn = new Random( 0 );

		int smallTableSize = Integer.min( tableSearched.size(), tableIndexed.size() );

		if( sampleratio * smallTableSize < 1 ) {
			// too low sample ratio
			Util.printLog( "Too low sample ratio" );
			Util.printLog( "Too low sample ratio" );

			sampleratio = 10.0 / smallTableSize;
		}

		for( Record r : tableSearched ) {
			if( rn.nextDouble() < sampleratio ) {
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
				if( rn.nextDouble() < sampleratio ) {
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

		stat.add( "Stat_Sample Searched size", sampleSearchedList.size() );
		stat.add( "Stat_Sample Indexed size", sampleIndexedList.size() );

		// Infer gamma, delta and epsilon
		JoinMin_Q joinmininst = new JoinMin_Q( o, stat );

		JoinMin_Q.checker = checker;
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

			// TODO DEBUG
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
}
