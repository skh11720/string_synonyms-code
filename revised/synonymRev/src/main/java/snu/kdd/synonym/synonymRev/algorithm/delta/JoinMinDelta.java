package snu.kdd.synonym.synonymRev.algorithm.delta;

import java.io.IOException;
import java.util.Collection;

import org.apache.commons.cli.ParseException;

import snu.kdd.synonym.synonymRev.algorithm.AlgorithmTemplate;
import snu.kdd.synonym.synonymRev.algorithm.JoinMin;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.index.JoinMinIndex;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.Param;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.tools.WYK_HashMap;
import snu.kdd.synonym.synonymRev.tools.WYK_HashSet;
import snu.kdd.synonym.synonymRev.validator.Validator;

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
		idx = new JoinMinDeltaIndex( indexK, qSize, deltaMax, stat, query, 0, writeResult );
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
		 */
		return "1.02";
	}
}
