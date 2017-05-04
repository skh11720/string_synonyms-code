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
		this.stat = stat;

		joinNaive.run( args, stat );

		double sampleratio = 0.1;
		SampleEstimate estimate = new SampleEstimate( joinNaive.tableSearched, joinNaive.tableIndexed, sampleratio );

		estimate.estimateNaive( joinNaive, stat );

		stat.add( "Cost_Index_Estimate", joinNaive.idx.estimatedIndexTime( estimate.alpha ) );
		stat.add( "Cost_Index_Actual", joinNaive.idx.indexTime );

		stat.add( "Cost_Join_Estimate", joinNaive.idx.estimatedJoinTime( estimate.beta ) );
		stat.add( "Cost_Join_Actual", joinNaive.idx.joinTime );

		stat.add( "Cost_ALL_Estimate",
				joinNaive.idx.estimatedIndexTime( estimate.alpha ) + joinNaive.idx.estimatedJoinTime( estimate.beta ) );
		stat.add( "Cost_ALL_Actual", joinNaive.idx.indexTime + joinNaive.idx.joinTime );

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
