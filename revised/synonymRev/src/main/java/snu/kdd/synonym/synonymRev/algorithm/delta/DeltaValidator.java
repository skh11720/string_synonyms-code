package snu.kdd.synonym.synonymRev.algorithm.delta;

import java.util.Arrays;

import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class DeltaValidator extends Validator{
	
	private static final int INF = Integer.MAX_VALUE/10;
	private boolean debug = false;
	private final int deltaMax;
	private int[][] M;
	private int[] D;
	private int[] D_prev;
	private Record x;
	private int[] y_arr;
	private int lx = 0, ly = 0;
	
	public DeltaValidator( int deltaMax ) {
		this.deltaMax = deltaMax;
		M = new int[lx+1][ly+1];
		initM();
	}

	@Override
	public int isEqual( Record x, Record y ) {
		// Check whether x -> y
		++checked;
		if( areSameString( x, y )) return 0; 
		
//		if ( x.getID() == 6660 && y.getID() == 6661 ) debug = true;
		else debug = false;
		if (debug) System.out.println( "x: "+Arrays.toString( x.getTokensArray() ) );
		if (debug) System.out.println( "y: "+Arrays.toString( y.getTokensArray() ) );
		
//		if ( x.size() > lx || y.size() > ly ) {
			if ( y.size() > ly ) {
				ly = y.size();
				D = new int[ly+1];
				D_prev = new int[ly+1];
			}
			lx = x.size();
			M = new int[lx+1][ly+1];
			initM();
//		}

		this.x = x;
		y_arr = y.getTokensArray();
//		M = new boolean[deltaMax+1][x.size()+1][y.size()+1];
		// M[i][j][d] is true if s[1:i] can be transformed to t[1:j] with at most d errors.
		// M is initially filled with false.
		
		int[][] L = x.getTransLengthsAll();
		
		// base cases
		M[0][0] = 0;
		for ( int i=1; i<=x.size(); ++i ) M[i][0] = L[i-1][0];
		for ( int j=1; j<=y.size(); ++j ) M[0][j] = j;

		for ( int i=1; i<=x.size(); ++i ) {
			for ( int j=1; j<=y.size(); ++j ) {
				if (debug) System.out.println( "i: "+i +", j: " + j );
				computeM( i, j );
			}
		}
		
		if (debug) for ( int i=0; i<=x.size(); ++i ) System.out.println( Arrays.toString(M[i]) );
		if ( M[x.size()][y.size()] <= deltaMax ) return 1;
		else return -1;
	}
	
	private void computeM( int i, int j ) {
		M[i][j] = Math.min( M[i][j], M[i][j-1]+1 );
		for ( Rule rule : x.getSuffixApplicableRules( i-1 ) ) {
			int[] rhs = rule.getRight();
			if ( rhs.length > j ) continue;
//			boolean suffixOf = true;
//			for ( int k=1; k<=rhs.length; ++k ) {
//				if ( rhs[rhs.length-k] != y_arr[j-k] ) {
//					suffixOf = false;
//					break;
//				}
//			}
//			if ( suffixOf ) M[i][j] = Math.min( M[i][j], & M[d][i-rule.leftSize()][j-rule.rightSize()] ) return true;
			
//			else {
				if (debug) System.out.println( rule );
				lcsDist( rhs, y_arr, j );
				if (debug) System.out.println( Arrays.toString( D ) );
//				int j0 = Math.max( 0, j-rule.rightSize()-d );
				if (debug) System.out.println( Arrays.toString( D ) );
				for ( int p=0; p<=j; ++p ) {
					M[i][j] = Math.min( M[i][j], M[i-rule.leftSize()][p] + D[p] );
					if ( M[i][j] > INF ) M[i][j] = INF;
//					if ( D[p] > d ) continue;
//					if ( M[d-D[p]][i-rule.leftSize()][p] ) return true;
				}
//			}
		}
//		return false;
	}
	
	private void initM() {
		for ( int i=0; i<M.length; ++i ) Arrays.fill( M[i], INF );
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
			Arrays.fill( D, INF );
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
