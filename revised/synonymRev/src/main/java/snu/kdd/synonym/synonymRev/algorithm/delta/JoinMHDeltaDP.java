package snu.kdd.synonym.synonymRev.algorithm.delta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.synonymRev.algorithm.JoinMH;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.index.JoinMHIndex;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.Param;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class JoinMHDeltaDP extends JoinMHDelta {
	// RecordIDComparator idComparator;

	public JoinMHDeltaDP( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}

	@Override
	protected void buildIndex( boolean writeResult ) {
		int[] indexPosition = new int[ indexK ];
		for( int i = 0; i < indexK; i++ ) {
			indexPosition[ i ] = i;
		}
		idx = new JoinMHDeltaDPIndex( indexK, qgramSize, deltaMax, query.indexedSet.get(), query, stat, indexPosition, writeResult, true, 0 );
	}

	@Override
	public String getVersion() {
		return "1.00";
	}

	@Override
	public String getName() {
		return "JoinMHDeltaDP";
	}
}
