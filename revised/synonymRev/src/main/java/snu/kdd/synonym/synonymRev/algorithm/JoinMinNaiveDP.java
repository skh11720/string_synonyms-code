package snu.kdd.synonym.synonymRev.algorithm;

import java.io.IOException;

import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.index.JoinMinDPIndex;
import snu.kdd.synonym.synonymRev.tools.StatContainer;

public class JoinMinNaiveDP extends JoinMinNaive {

	public JoinMinNaiveDP( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}
	
	@Override
	public void run( Query query, String[] args ) throws IOException, ParseException {
		super.run( query, args );
	}

	@Override
	protected void buildJoinMinIndex() {
		// Build an index
		joinMinIdx = new JoinMinDPIndex( indexK, qSize, stat, query, joinThreshold, true );
	}
	
	@Override
	public String getName() {
		return "JoinMinNaiveDP";
	}
	
	@Override
	public String getVersion() {
		return "1.00";
	}
}
