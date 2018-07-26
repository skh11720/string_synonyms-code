package snu.kdd.synonym.synonymRev.algorithm.delta;

import java.io.IOException;

import snu.kdd.synonym.synonymRev.algorithm.JoinMin;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.tools.Param;
import snu.kdd.synonym.synonymRev.tools.StatContainer;

public class JoinMinDelta extends JoinMin {
	protected int deltaMax;



	public JoinMinDelta( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}
	
	@Override
	protected void setup( Param params ) {
		super.setup( params );
		deltaMax = params.delta;
		checker = new DeltaValidatorTopDown( deltaMax );
	}

	@Override
	protected void buildIndex( boolean writeResult ) throws IOException {
		idx = new JoinMinDeltaIndexStrong( indexK, qSize, deltaMax, stat, query, 0, writeResult );
	}

	@Override
	public String getName() {
		return "JoinMinDelta";
	}

	@Override
	public String getVersion() {
		/*
		 * 1.00: initial version
		 * 1.01: ignore records with too many transformations
		 * 1.02: use DeltaValidatorTopDown
		 * 1.03: DeltaValidator consider trivial cases
		 * 1.04: strong filter
		 */
		return "1.04";
	}
}
