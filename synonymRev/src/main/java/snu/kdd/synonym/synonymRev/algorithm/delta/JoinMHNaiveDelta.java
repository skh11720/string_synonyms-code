package snu.kdd.synonym.synonymRev.algorithm.delta;

import java.io.IOException;

import snu.kdd.synonym.synonymRev.algorithm.JoinMHNaive;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.index.JoinMHIndexInterface;
import snu.kdd.synonym.synonymRev.index.NaiveIndex;
import snu.kdd.synonym.synonymRev.tools.Param;
import snu.kdd.synonym.synonymRev.tools.StatContainer;

public class JoinMHNaiveDelta extends JoinMHNaive {

	public JoinMHNaiveDelta( Query query, StatContainer stat ) throws IOException {
		super( query, stat );
	}

	protected int deltaMax;
	
	
	
	@Override
	protected void setup( Param params ) {
		super.setup( params );
		deltaMax = params.delta;
		checker = new DeltaValidatorTopDown( deltaMax );
	}

	@Override
	protected void buildJoinMHIndex() {
		// Build an index
		int[] index = new int[ indexK ];
		for( int i = 0; i < indexK; i++ ) {
			index[ i ] = i;
		}
		joinMHIdx = (JoinMHIndexInterface) new JoinMHDeltaIndex( indexK, qSize, deltaMax, query.indexedSet.get(), query, stat, index, true, true, joinThreshold );
	}

	@Override
	protected void buildNaiveIndex() {
		if ( deltaMax == 0 ) naiveIndex = new NaiveIndex( query.indexedSet, query, stat, true, joinThreshold, joinThreshold/2 );
		else naiveIndex = new NaiveDeltaIndex( query.indexedSet, query, stat, true, deltaMax, joinThreshold, joinThreshold / 2 );
	}

	@Override
	public String getName() {
		return "JoinMHNaiveDelta";
	}

	@Override
	public String getVersion() {
		/*
		 * 1.00: initial version
		 * 1.01: ignore records with too many transformations
		 * 1.02: use DeltaValidatorTopDown
		 * 1.03: DeltaValidator consider trivial cases
		 */
		return "1.03";
	}
}
