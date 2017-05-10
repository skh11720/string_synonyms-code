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

	DataInfo dataInfo;

	public EstimateJoinMin( String rulefile, String Rfile, String Sfile, String outputfile, DataInfo dataInfo )
			throws IOException {
		super( rulefile, Rfile, Sfile, outputfile, dataInfo );

		joinMin = new JoinMin_Q( rulefile, Rfile, Sfile, outputfile, dataInfo );

		this.dataInfo = dataInfo;
	}

	@Override
	public void run( String[] args, StatContainer stat ) {
		this.stat = stat;
		joinMin.run( args, stat );

		double sampleratio = 0.1;
		SampleEstimate estimate = new SampleEstimate( joinMin.tableSearched, joinMin.tableIndexed, sampleratio,
				dataInfo.isSelfJoin() );

		estimate.estimateJoinMin( joinMin, stat, JoinMin_Q.checker, joinMin.qSize );

		stat.add( "Cost_Count_Estimate", joinMin.idx.estimatedCountTime( estimate.gamma ) );
		stat.add( "Cost_Count_Actual", joinMin.idx.countTime );

		stat.add( "Cost_Index_Estimate", joinMin.idx.estimatedIndexTime( estimate.delta ) );
		stat.add( "Cost_Index_Actual", joinMin.idx.indexTime );

		stat.add( "Cost_Join_Estimate", joinMin.idx.estimatedJoinTime( estimate.epsilon ) );
		stat.add( "Cost_Join_Actual", joinMin.idx.joinTime );

		stat.add( "Cost_ALL_Estimate", joinMin.idx.estimatedCountTime( estimate.gamma )
				+ joinMin.idx.estimatedIndexTime( estimate.delta ) + joinMin.idx.estimatedJoinTime( estimate.epsilon ) );
		stat.add( "Cost_ALL_Actual", joinMin.idx.countTime + joinMin.idx.indexTime + joinMin.idx.joinTime );

	}

	@Override
	public String getName() {
		return "EstimateJoinMin";
	}

	@Override
	public String getVersion() {
		return "1.0";
	}
}
