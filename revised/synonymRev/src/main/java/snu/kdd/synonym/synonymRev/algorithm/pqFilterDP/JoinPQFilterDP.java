package snu.kdd.synonym.synonymRev.algorithm.pqFilterDP;

import java.io.IOException;

import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.tools.StatContainer;

/*
 * A wrapper class of the JoinPQFilterDP family.
 */
public class JoinPQFilterDP extends AlgorithmTemplate{
	
	private JoinPQFilterDP alg;
	protected ParamPQFilterDP params;
	public JoinPQFilterDP( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}

	@Override
	public String getName() {
		return "JoinPQFilterDP";
	}

	@Override
	public String getVersion() {
		return "1.01";
	}

	@Override
	public void run( Query query, String[] args ) throws IOException, ParseException {
		params = ParamPQFilterDP.parseArgs( args, stat, query );
		if ( params.mode.equals( "naive" ) ) alg = new JoinPQFilterDPNaive( query, stat );
		else if ( params.mode.equals(  "dp1" )) alg = new JoinPQFilterDP1( query, stat );
		else if ( params.mode.equals(  "dp2" )) alg = new JoinPQFilterDP2( query, stat );
		else if ( params.mode.equals(  "dp3" )) alg = new JoinPQFilterDP3( query, stat );
		else throw new RuntimeException("Unexpected exception");
		alg.params = params;
		alg.run( query, args );
	}
}
