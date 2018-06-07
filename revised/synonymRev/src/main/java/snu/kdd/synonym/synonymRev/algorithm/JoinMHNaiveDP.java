package snu.kdd.synonym.synonymRev.algorithm;

import java.io.IOException;

import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.index.JoinMHDPIndex;
import snu.kdd.synonym.synonymRev.tools.StatContainer;

public class JoinMHNaiveDP extends JoinMHNaive {

	public JoinMHNaiveDP( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void run( Query query, String[] args ) throws IOException, ParseException {
		super.run( query, args );
		((JoinMHDPIndex)joinMHIdx).addStat( stat );
	}
	
	@Override
	protected void buildJoinMHIndex() {
		int[] indexPosition = new int[ indexK ];
		for( int i = 0; i < indexK; i++ ) {
			indexPosition[ i ] = i;
		}
		joinMHIdx = new JoinMHDPIndex( indexK, qSize, query.indexedSet.get(), query, stat, indexPosition, false, true, 0 );
	}

	@Override
	public String getName() {
		return "JoinMHNaiveDP";
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

}
