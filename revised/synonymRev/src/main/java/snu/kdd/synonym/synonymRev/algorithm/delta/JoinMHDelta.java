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

public class JoinMHDelta extends JoinMH {
	// RecordIDComparator idComparator;

	public JoinMHDelta( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}

	protected int deltaMax;

	/**
	 * Key: twogram<br/>
	 * Value IntervalTree Key: length of record (min, max)<br/>
	 * Value IntervalTree Value: record
	 */
	
	@Override
	protected void setup( Param params ) {
		super.setup( params );
		deltaMax = params.delta;
		checker = new DeltaValidatorTopDown( deltaMax );
	}

	@Override
	protected void preprocess() {
		super.preprocess();

		for( Record rec : query.indexedSet.get() ) {
			rec.preprocessSuffixApplicableRules();
		}
		if( !query.selfJoin ) {
			for( Record rec : query.searchedSet.get() ) {
				rec.preprocessSuffixApplicableRules();
			}
		}
	}

	@Override
	protected void buildIndex( boolean writeResult ) {
		int[] indexPosition = new int[ indexK ];
		for( int i = 0; i < indexK; i++ ) {
			indexPosition[ i ] = i;
		}
		idx = new JoinMHDeltaIndex( indexK, qgramSize, deltaMax, query.indexedSet.get(), query, stat, indexPosition, writeResult, true, 0 );
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

	@Override
	public String getName() {
		return "JoinMHDelta";
	}
}
