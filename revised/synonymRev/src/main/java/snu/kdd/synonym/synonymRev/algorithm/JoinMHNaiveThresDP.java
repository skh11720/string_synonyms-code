package snu.kdd.synonym.synonymRev.algorithm;

import java.io.IOException;

import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.index.JoinMHDPIndex;
import snu.kdd.synonym.synonymRev.tools.StatContainer;

public class JoinMHNaiveThresDP extends JoinMHNaiveThres {

	public JoinMHNaiveThresDP( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void run( Query query, String[] args ) throws IOException, ParseException {
		super.run( query, args );
		if ( joinMHRequired ) ((JoinMHDPIndex)joinMHIndex).addStat( stat );
	}
	
	@Override
	protected void buildJoinMHIndex(int threshold) {
		// Build an index
		int[] index = new int[ indexK ];
		for( int i = 0; i < indexK; i++ ) {
			index[ i ] = i;
		}
		joinMHIndex = new JoinMHDPIndex( indexK, qSize, query.indexedSet.get(), query, stat, index, true, true, threshold );
	}
	
	@Override
	public String getName() {
		return "JoinMHNaiveThresDP";
	}
	
	@Override
	public String getVersion() {
		return "1.00";
	}
}
