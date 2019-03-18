package snu.kdd.synonym.synonymRev.algorithm.delta;

import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.tools.IntTriple;

public class DeltaValidatorDPTopDown extends DeltaValidatorNaive {
	
	private Object2BooleanOpenHashMap<IntTriple> M;
	// M[(d,i,j)] is true if s[:i] can be transformed to a string s' whose edit distance to t[:j] is at most d.
	
	private Object2ObjectOpenHashMap<Rule, int[][]> mapD;
	// D[rule] is the edit distance matrix.

	private int[][] L;
	// L[i] is the minimum and maximum transform lengths of x[0:i+1].
	
	private Record x, y;

	private int lx, ly;

	public DeltaValidatorDPTopDown( int deltaMax, String strDistFunc ) {
		super(deltaMax, strDistFunc);
		M = new Object2BooleanOpenHashMap<>();
		mapD = new Object2ObjectOpenHashMap<>();
	}

	@Override
	protected boolean isDeltaTransEqual( Record x, Record y ) {
		// initialize
		M.clear();
		mapD.clear();
		L = x.getTransLengthsAll();
		this.x = x;
		this.y = y;
		lx = x.size();
		ly = y.size();

		return computeM( deltaMax, lx, ly );
	}
	
	private boolean computeM( int d, int i, int j ) {
		/*
		 * Compute and return M[d][i][j].
		 */
		
		// base cases
		if ( i == 0 && j == 0 ) return true;
		if ( j == 0 ) return (L[i-1][0] <= d);
		if ( i == 0 ) return (j <= d);
		
		// look for the saved value
		IntTriple key = new IntTriple(d, i, j);
		if ( M.containsKey(key) ) return M.getBoolean(key);
		
		// recursion
		for ( Rule rule : x.getSuffixApplicableRules(i-1) ) {
			if ( i - rule.leftSize() < 0 ) continue;
			int[] rhs = rule.getRight();
			int[][] D;
			/*
			 * D[j0][j1] is the edit distance between rhs and y[j0:j1].
			 * 0 <= j0 <= j1 and 1 <= j1 <= |t|.
			 */
			if ( mapD.containsKey(rule) ) D = mapD.get(rule);
			else {
				D = new int[ly+1][];
				for ( int j0=0; j0<=ly; ++j0 ) {
					D[j0] = distAll.eval( rhs, y.getTokensArray(), j0 ); // D[j0][j0], D[j0][j0+1] ... are valid values
				}
				mapD.put(rule, D);
			}

			// given the current rule, find j0 which satisfies the condition in the recurrence equation for every 1 <= j <= |y|.
			for ( int j0=0; j0<=j; ++j0 ) {
				if ( D[j0][j] > d ) continue;
				if ( computeM( d - D[j0][j], i - rule.leftSize(), j0 ) ) {
					M.put(key, true);
					return true;
				}
			}
		}
		M.put(key, false);
		return false;
	}

	@Override
	public String getName() {
		return "DeltaValidatorDPTopDown";
	}
}
