package snu.kdd.synonym.estimation;

import java.io.IOException;

import snu.kdd.synonym.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.algorithm.JoinNaive1;
import snu.kdd.synonym.data.DataInfo;
import snu.kdd.synonym.tools.StatContainer;

/**
 * The Naive algorithm which expands strings from both tables S and T
 */
public class EstimateJoinNaive extends AlgorithmTemplate {
	/**
	 * Store the original index from expanded string
	 */

	JoinNaive1 joinNaive;

	public EstimateJoinNaive( String rulefile, String Rfile, String Sfile, String outputfile, DataInfo dataInfo )
			throws IOException {
		super( rulefile, Rfile, Sfile, outputfile, dataInfo );

		joinNaive = new JoinNaive1( rulefile, Rfile, Sfile, outputfile, dataInfo );
	}

	@Override
	public void run( String[] args, StatContainer stat ) {
		joinNaive.run( args, stat );

		double sampleratio = 0.1;
		SampleEstimate estimate = new SampleEstimate( joinNaive.tableSearched, joinNaive.tableIndexed, sampleratio );

		estimate.estimateNaive( joinNaive, stat );

		System.out.println( "Estimate Index: " + joinNaive.idx.estimatedIndexTime( estimate.alpha ) );
		System.out.println( "Actual Index  : " + joinNaive.idx.indexTime );

		System.out.println( "Estimate Join : " + joinNaive.idx.estimatedJoinTime( estimate.beta ) );
		System.out.println( "Actual Join   : " + joinNaive.idx.joinTime );

	}

	@Override
	public String getName() {
		return "EstimateNaive1";
	}

	@Override
	public String getVersion() {
		return "1.0";
	}
}
