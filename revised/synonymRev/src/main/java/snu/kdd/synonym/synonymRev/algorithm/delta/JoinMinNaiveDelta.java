package snu.kdd.synonym.synonymRev.algorithm.delta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.apache.commons.cli.ParseException;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.algorithm.JoinMinNaive;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.estimation.SampleEstimate;
import snu.kdd.synonym.synonymRev.index.JoinMinIndex;
import snu.kdd.synonym.synonymRev.index.JoinMinIndexInterface;
import snu.kdd.synonym.synonymRev.index.NaiveIndex;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.Param;
import snu.kdd.synonym.synonymRev.tools.StatContainer;
import snu.kdd.synonym.synonymRev.tools.StopWatch;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.validator.Validator;

/**
 * Given threshold, if a record has more than 'threshold' 1-expandable strings,
 * use an index to store them.
 * Otherwise, generate all 1-expandable strings and then use them to check
 * if two strings are equivalent.
 * Utilize only one index by sorting records according to their expanded size.
 * It first build JoinMin(JoinH2Gram) index and then change threshold / modify
 * index in order to find the best execution time.
 */
public class JoinMinNaiveDelta extends JoinMinNaive {
	public JoinMinNaiveDelta( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}
	
	private int deltaMax;


	
	@Override 
	protected void setup( Param params ) {
		super.setup( params );
		deltaMax = params.delta;
		checker = new DeltaValidatorTopDown( deltaMax );
	}

	@Override
	protected void buildJoinMinIndex() {
		// Build an index
		joinMinIdx = new JoinMinDeltaIndex( indexK, qSize, deltaMax, stat, query, joinThreshold, true );
	}

	@Override
	protected void buildNaiveIndex() {
		if ( deltaMax == 0 ) naiveIndex = new NaiveIndex( query.indexedSet, query, stat, true, joinThreshold, joinThreshold/2 );
		else naiveIndex = new NaiveDeltaIndex( query.indexedSet, query, stat, true, deltaMax, joinThreshold, joinThreshold / 2 );
	}

	@Override
	public String getName() {
		return "JoinMinNaiveDelta";
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
