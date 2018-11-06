package snu.kdd.synonym.synonymRev.algorithm.delta;

import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.tools.Util;

public class DeltaValidatorDP extends DeltaValidatorNaive {
	
	private boolean[][][] M; 
	// M [d][i][j] is true if s[:i] can be transformed to a string s' whose edit distance to t[:j] is at most d.

	private int Lx = 5, Ly = 5;

	public DeltaValidatorDP(int deltaMax) {
		super(deltaMax);
		M = new boolean[deltaMax+1][Lx+1][Ly+1];
	}

	@Override
	public int isEqual(Record x, Record y) {
		// Check whether x -> y
		++checked;
		
		// trivial cases
		if ( areSameString( x, y )) {
			++numEqual;
			return 0; 
		}
		if ( Util.edit( x.getTokensArray(), y.getTokensArray() ) <= deltaMax ) {
			++numDeltaEqual;
			return 1;
		}
		
		// enlarge the array M if necessary
		int lx = x.size();
		int ly = y.size();
		if ( lx > Lx || ly > Ly ) {
			Lx = Math.max( lx, Lx );
			Ly = Math.max( ly, Ly );
			M = new boolean[deltaMax+1][Lx+1][Ly+1];
		}
		
		// initialize M
		int[][] L = x.getTransLengthsAll();
		for ( int d=0; d<=deltaMax; ++d ) {
			M[d][0][0] = true;
			for ( int i=1; i<=lx; ++i ) M[d][i][0] = (L[i-1][0] <= d);
			for ( int j=1; j<=ly; ++j ) M[d][0][j] = (j <= d);
			for ( int i=1; i<=lx; ++i ) {
				for ( int j=1; j<=ly; ++j ) M[d][i][j] = false;
			}
		}
		
		for ( int i=1; i<=lx; ++i ) {
			for ( Rule rule : x.getSuffixApplicableRules(i-1) ) {
				if ( i - rule.leftSize() < 0 ) continue;
				int[] rhs = rule.getRight();
				int[][] D = new int[ly+1][];
				/*
				 * D[j0][j1] is the edit distance between rhs and y[j0:j1].
				 * 0 <= j0 <= j1 and 1 <= j1 <= |t|.
				 */
				for ( int j0=0; j0<=ly; ++j0 ) {
					D[j0] = Util.edit_all( rhs, y.getTokensArray(), j0 ); // D[j0][j0], D[j0][j0+1] ... are valid values
				}
				// given the current rule, find j0 which satisfies the condition in the recurrence equation for every 1 <= j <= |y|.
				for ( int j=1; j<=ly; ++j ) {
					for ( int d=0; d<=deltaMax; ++d ) {
						for ( int j0=0; j0<=j; ++j0 ) {
							if ( D[j0][j] > d ) continue;
							M[d][i][j] |= M[ d - D[j0][j] ][i - rule.leftSize()][j0];
						}
					}
				}
				// end updating M[d][i][j] for i and rule
			}
		}
		// end computing M
		if ( M[deltaMax][lx][ly] ) {
			++numDeltaTransEqual;
			return 1;
		}
		else return -1;
	}

	@Override
	public String getName() {
		return "DeltaValidatorDP";
	}

}
