package snu.kdd.synonym.synonymRev.algorithm.delta;

import java.io.IOException;

import snu.kdd.synonym.synonymRev.algorithm.JoinMin;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.tools.Param;
import snu.kdd.synonym.synonymRev.tools.StatContainer;

public class JoinMinStrongDelta extends JoinMinDelta {

	public JoinMinStrongDelta( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}
	
	@Override
	protected void buildIndex( boolean writeResult ) throws IOException {
		idx = new JoinMinStrongDeltaIndex( indexK, qSize, deltaMax, stat, query, 0, writeResult );
	}

	@Override
	public String getName() {
		return "JoinMinStrongDelta";
	}

	@Override
	public String getVersion() {
		/*
		 * 1.00: initial version
		 */
		return "1.00";
	}
}
