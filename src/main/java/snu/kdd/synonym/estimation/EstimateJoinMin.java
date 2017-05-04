package snu.kdd.synonym.estimation;

import java.io.IOException;

import snu.kdd.synonym.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.algorithm.JoinMin_Q;
import snu.kdd.synonym.data.DataInfo;
import snu.kdd.synonym.tools.StatContainer;

/**
 * The Naive algorithm which expands strings from both tables S and T
 */
public class EstimateJoinMin extends AlgorithmTemplate {
	/**
	 * Store the original index from expanded string
	 */

	JoinMin_Q joinMin;

	public EstimateJoinMin( String rulefile, String Rfile, String Sfile, String outputfile, DataInfo dataInfo )
			throws IOException {
		super( rulefile, Rfile, Sfile, outputfile, dataInfo );

		joinMin = new JoinMin_Q( rulefile, Rfile, Sfile, outputfile, dataInfo );
	}

	@Override
	public void run( String[] args, StatContainer stat ) {
		double sampleratio = Double.parseDouble( args[ 0 ] );
		SampleEstimate estimate = new SampleEstimate( joinMin.tableSearched, joinMin.tableIndexed, sampleratio );

		// estimate.estimateJoinMin( joinMin, stat, );

		joinMin.run( args, stat );

		// System.out.println( "Estimate Index: " + joinMin.idx.estimatedIndexTime( estimate.alpha ) );
		// System.out.println( "Actual Index : " + joinMin.idx.indexTime );
		//
		// System.out.println( "Estimate Join : " + joinMin.idx.estimatedJoinTime( estimate.beta ) );
		// System.out.println( "Actual Join : " + joinMin.idx.joinTime );

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
