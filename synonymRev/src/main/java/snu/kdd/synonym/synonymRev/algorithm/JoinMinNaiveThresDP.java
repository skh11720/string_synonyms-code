package snu.kdd.synonym.synonymRev.algorithm;

import java.io.IOException;

import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.index.JoinMinDPIndex;
import snu.kdd.synonym.synonymRev.tools.StatContainer;

public class JoinMinNaiveThresDP extends JoinMinNaiveThres {

	public JoinMinNaiveThresDP( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}
	
	@Override
	protected void buildJoinMinIndex() {
		// Build an index
		joinMinIdx = new JoinMinDPIndex( indexK, qSize, stat, query, joinThreshold, true );
	}
	
	@Override
	public String getName() {
		return "JoinMinNaiveThresDP";
	}
	
	@Override
	public String getVersion() {
		/*
		 * 1.00: initial version
		 * 1.01: ignore records with too many transformations
		 */
		return "1.01";
	}
}
