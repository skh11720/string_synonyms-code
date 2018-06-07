package snu.kdd.synonym.synonymRev.algorithm;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.index.JoinMHDPIndex;
import snu.kdd.synonym.synonymRev.index.JoinMHIndex;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.Param;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class JoinMHNaiveDP extends JoinMHNaive {

	public JoinMHNaiveDP( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
		// TODO Auto-generated constructor stub
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
