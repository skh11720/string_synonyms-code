package snu.kdd.synonym.estimation;

import java.io.IOException;

import mine.Record;
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
	DataInfo dataInfo;

	public EstimateJoinNaive( String rulefile, String Rfile, String Sfile, String outputfile, DataInfo dataInfo )
			throws IOException {
		super( rulefile, Rfile, Sfile, outputfile, dataInfo, false );

		joinNaive = new JoinNaive1( rulefile, Rfile, Sfile, outputfile, dataInfo, false );
		this.dataInfo = dataInfo;
	}

	@Override
	public void run( String[] args, StatContainer stat ) {
		this.stat = stat;

		preprocess();
		double sampleratio = 0.01;
		SampleEstimate estimate = new SampleEstimate( joinNaive.tableSearched, joinNaive.tableIndexed, sampleratio,
				dataInfo.isSelfJoin() );

		joinNaive.threshold = Long.valueOf( args[ 0 ] );
		joinNaive.stat = stat;
		joinNaive.runWithoutPreprocess( true );

		estimate.estimateNaive( joinNaive, stat );

		stat.add( "Cost_Index_Estimate", joinNaive.idx.estimatedIndexTime( estimate.alpha ) );
		stat.add( "Cost_Index_Actual", joinNaive.idx.indexTime );

		stat.add( "Cost_Join_Estimate", joinNaive.idx.estimatedJoinTime( estimate.beta ) );
		stat.add( "Cost_Join_Actual", joinNaive.idx.joinTime );

		stat.add( "Cost_ALL_Estimate",
				joinNaive.idx.estimatedIndexTime( estimate.alpha ) + joinNaive.idx.estimatedJoinTime( estimate.beta ) );
		stat.add( "Cost_ALL_Actual", joinNaive.idx.indexTime + joinNaive.idx.joinTime );

	}

	private void preprocess() {
		stat.add( "Mem_1_Initialized", ( runtime.totalMemory() - runtime.freeMemory() ) / 1048576 );

		long applicableRules = 0;
		for( final Record t : tableSearched ) {
			t.preprocessRules( joinNaive.automata, false );
			applicableRules += t.getNumApplicableRules();
			t.preprocessEstimatedRecords();
		}
		stat.add( "Stat_Applicable Rule TableSearched", applicableRules );

		applicableRules = 0;
		long estTransformed = 0;
		for( final Record s : tableIndexed ) {
			s.preprocessRules( joinNaive.automata, false );
			applicableRules += s.getNumApplicableRules();
			s.preprocessEstimatedRecords();

			estTransformed += s.getEstNumRecords();
		}
		joinNaive.avgTransformed = estTransformed / (double) tableIndexed.size();

		stat.add( "Stat_Applicable Rule TableIndexed", applicableRules );
		stat.add( "Stat_Avg_Transformed_TableIndexed", Double.toString( joinNaive.avgTransformed ) );
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
