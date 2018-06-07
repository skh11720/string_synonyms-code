package snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.seq;

import java.io.IOException;

import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.order.AbstractGlobalOrder;
import snu.kdd.synonym.synonymRev.tools.StatContainer;

/*
 * A wrapper class of the JoinPQFilterDP family.
 */
public class JoinMHDPWrapper extends AlgorithmTemplate{
	
	private JoinPQFilterDP alg;
	protected ParamPQFilterDP params;
	protected Boolean useTopDown;
	protected AbstractGlobalOrder globalOrder;

	public JoinMHDPWrapper( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}

	@Override
	public String getName() {
		return "JoinMHDP";
	}

	@Override
	public String getVersion() {
		/*
		 * 1.00: initial version
		 * 1.01: reduce cand pqgram size
		 * 1.02: some optimizations
		 */
		return "1.02";
	}

	@Override
	public void run( Query query, String[] args ) throws IOException, ParseException {
		params = ParamPQFilterDP.parseArgs( args, stat, query );
		if ( params.mode.equals(  "dp1" )) alg = new JoinMHDP1( query, stat );
		else if ( params.mode.equals(  "dp3" )) alg = new JoinMHDP3( query, stat );
		else throw new RuntimeException("Unexpected exception");
		alg.params = params;
		alg.run( query, args );
	}
}
