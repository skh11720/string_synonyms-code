package snu.kdd.synonym.synonymRev.algorithm.delta;

import java.util.Arrays;

import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class DeltaValidator extends Validator{
	
	private static final boolean debug = false;
	private final int deltaMax;
	private boolean[][][] M;
	private int[] D;
	private int[] D_prev;
	private Record x;
	private int[] y_arr;
	private int lx = 0, ly = 0;
	
	public DeltaValidator( int deltaMax ) {
		this.deltaMax = deltaMax;
		M = new boolean[deltaMax+1][][];
	}

	@Override
	public int isEqual( Record x, Record y ) {
		// Check whether x -> y
		++checked;
		if( areSameString( x, y )) return 0; 
		if ( x.getMinTransLength() + y.size() <= deltaMax ) return 1;
		
		int[][] L = x.getTransLengthsAll();
		if ( L[x.size()-1][0] + y.size() <= deltaMax ) return 1; // trivial case
		
		if ( x.size() > lx || y.size() > ly ) {
			if ( y.size() > ly ) {
				ly = y.size();
				D = new int[ly+1];
				D_prev = new int[ly+1];
			}
			lx = x.size();
			M = new boolean[deltaMax+1][lx+1][ly+1];
		}

		this.x = x;
		y_arr = y.getTokensArray();
//		M = new boolean[deltaMax+1][x.size()+1][y.size()+1];
		// M[i][j][d] is true if s[1:i] can be transformed to t[1:j] with at most d errors.
		// M is initially filled with false.
		
		for ( int d=0; d<=deltaMax; ++d ) {
			// base cases
			M[d][0][0] = true;
			for ( int i=1; i<=x.size(); ++i ) M[d][i][0] = (L[i-1][0] <= d);
			for ( int j=1; j<=y.size(); ++j ) M[d][0][j] = (j <= d);

			for ( int i=1; i<=x.size(); ++i ) {
				for ( int j=1; j<=y.size(); ++j ) {
					if (debug) System.out.println( "i: "+i +", j: " + j );
					M[d][i][j] = computeM( d, i, j );
				}
			}
		}
		if (debug) {
			for ( int d=0; d<=deltaMax; ++d ) {
				System.out.println( "delta: "+d );
				for ( int i=0; i<=x.size(); ++i ) {
					for ( int j=0; j<=y.size(); ++j ) {
						System.out.print( M[d][i][j]+", " );
					}
					System.out.println( "" );
				}
			}
		}
		if ( M[deltaMax][x.size()][y.size()] ) return 1;
		else return -1;
	}
	
	private boolean computeM( int d, int i, int j ) {
		if ( d > 0 && (M[d-1][i][j-1] || M[d-1][i][j]) ) return true;
		for ( Rule rule : x.getSuffixApplicableRules( i-1 ) ) {
			int[] rhs = rule.getRight();
			if ( d == 0 ) {
				if ( rhs.length > j ) continue;
				boolean suffixOf = true;
				for ( int k=1; k<=rhs.length; ++k ) {
					if ( rhs[rhs.length-k] != y_arr[j-k] ) {
						suffixOf = false;
						break;
					}
				}
				if ( suffixOf && M[d][i-rule.leftSize()][j-rule.rightSize()] ) return true;
			}
			else {
				if (debug) System.out.println( rule );
				lcsDist( rhs, y_arr, j );
				int j0 = Math.max( 0, j-rule.rightSize()-d );
				for ( int p=j0; p<=j; ++p ) {
					if ( D[p] > d ) continue;
					if ( M[d-D[p]][i-rule.leftSize()][p] ) return true;
				}
			}
		}
		return false;
	}
	
	@Deprecated
	private int isSuffixWithErrors( int[] pat, int[] seq, int end ) {
		/*
		 * Check if pat is a suffix of seq[1:end], allowing at most n_max_errors in pat.
		 * seq is right exclusive.
		 * Return the number of errors used to match.
		 */
		int n_errors = 0;
		for ( int i=pat.length-1, j=end-1; i>=0; --i ) {
			if ( j < 0 ) ++n_errors;
			else if ( pat[i] == seq[j] ) --j;
			else ++n_errors;
		}
		return n_errors;
	}
	
	private void lcsDist( int[] pat, int[] seq, int end ) {
		/*
		 * Return an integer array D of length end+1, 
		 * whose element D[j] is the minimum number of errors required to match pat to seq[j,end] (right exclusive).
		 * end-j represents the length of the suffix of seq[1:end] covered by pat.
		 */
//		int[] D = new int[end+1];
//		int[] D_prev = new int[end+1];
		int j0 = Math.max( 0, end-pat.length-deltaMax );
		for ( int j=end; j>=j0; --j ) D_prev[j] = end-j;

		if (debug) System.out.println( Arrays.toString( D_prev ) );
		for ( int i=pat.length-1; i>=0; --i ) {
			Arrays.fill( D, Integer.MAX_VALUE );
			D[end] = D_prev[end]+1;
			for ( int j=end-1; j>=j0; --j ) {
				D[j] = Math.min( D[j], D[j+1]+1 );
				D[j] = Math.min( D[j], D_prev[j]+1 );
				if ( pat[i] == seq[j] ) D[j] = Math.min( D[j], D_prev[j+1] );
				else D[j] = Math.min( D[j], D_prev[j+1]+2 );
			}
			if (debug) System.out.println( Arrays.toString( D ) );
			for ( int j=end; j>=0; --j ) D_prev[j] = D[j];
		}
	}
//
//		int min_error = pat.length;
//		int min_pos = 0;
//		for ( int j=end; j>=0; --j ) {
//			if ( D[j] <= min_error ) {
//				min_error = D[j];
//				min_pos = end - j;
//			}
//		}
//		return D;
		
		/*
		 * Note: there can be multiple positions with min_error.
		 * Currently, the leftmost position is selected among them.
		 */

	@Override
	public String getName() {
		return "DeltaValidator";
	}
}
