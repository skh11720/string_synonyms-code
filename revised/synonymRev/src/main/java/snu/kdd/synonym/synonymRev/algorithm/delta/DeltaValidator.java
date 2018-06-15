package snu.kdd.synonym.synonymRev.algorithm.delta;

import java.util.Arrays;

import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class DeltaValidator extends Validator{
	
	private final int deltaMax;
	private boolean[][][] M;
	private static final boolean debug = false;
	
	public DeltaValidator( int deltaMax ) {
		this.deltaMax = deltaMax;
	}

	@Override
	public int isEqual( Record x, Record y ) {
		// Check whether x -> y
		++checked;
		if( areSameString( x, y )) return 0; 
		
		int[] y_arr = y.getTokensArray();
		M = new boolean[x.size()+1][y.size()+1][deltaMax+1];
		// M[i][j][d] is true if s[1:i] can be transformed to t[1:j] with at most d errors.
		// M is initially filled with false.
		
		int[][] L = x.getTransLengthsAll();
		
		for ( int d=0; d<=deltaMax; ++d ) {
			// base cases
			M[0][0][d] = true;
			for ( int i=1; i<=x.size(); ++i ) M[i][0][d] = (L[i-1][0] <= d);
			for ( int j=1; j<=y.size(); ++j ) M[0][j][d] = (j <= d);

			for ( int i=1; i<=x.size(); ++i ) {
				for ( int j=1; j<=y.size(); ++j ) {
					if (debug) System.out.println( "i: "+i +", j: " + j );
					if ( d > 0 && M[i][j-1][d-1] ) M[i][j][d] = true;
					else {
						for ( Rule rule : x.getSuffixApplicableRules( i-1 ) ) {
							int[] rhs = rule.getRight();
//							int n_errors = isSuffixWithErrors( rhs, y_arr, j );
							if (debug) System.out.println( rule );
							IntegerPair ip = lcsDist( rhs, y_arr, j );
							if (debug) System.out.println( "(n_errors, l_rhs_cover): "+ip );
							int n_errors = ip.i1;
							int l_rhs_cover = ip.i2;

//							if ( i-rule.leftSize() < 0 ) throw new RuntimeException("i: "+i+", lhs.length: "+rule.leftSize());
//							if ( j-rule.rightSize()+n_errors < 0 ) throw new RuntimeException("j: "+", rhs.length: "+rule.rightSize());
//							if ( n_errors <= d && d-n_errors < 0 ) throw new RuntimeException("d: "+d+", n_errors: "+n_errors);
							if ( n_errors <= d && M[i-rule.leftSize()][j-l_rhs_cover][d-n_errors] ) {
								M[i][j][d] = true;
								break;
							}
						}
					}
				}
			}
		}
		if (debug) {
			for ( int d=0; d<=deltaMax; ++d ) {
				System.out.println( "delta: "+d );
				for ( int i=0; i<=x.size(); ++i ) {
					for ( int j=0; j<=y.size(); ++j ) {
						System.out.print( M[i][j][d]+", " );
					}
					System.out.println( "" );
				}
			}
		}
		if ( M[x.size()][y.size()][deltaMax] ) return 1;
		else return -1;
	}
	
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
	
	private IntegerPair lcsDist( int[] pat, int[] seq, int end ) {
		/*
		 * Check if pat is a suffix of seq[1:end], allowing at most n_max_errors in both strings.
		 * seq is right exclusive.
		 * Return the pair of 
		 * 	 (i) the number of minimum errors used to match, and
		 *   (ii) the length of covered region by pat. 
		 */
		int[] D = new int[pat.length+1];
		int[] D_prev = new int[pat.length+1];
		int min_error = pat.length;
		int min_pos = 0;
		for ( int i=pat.length; i>=0; --i ) D_prev[i] = pat.length-i;

		if (debug) System.out.println( Arrays.toString( D_prev ) );
		for ( int j=end-1; j>=0; --j ) {
			Arrays.fill( D, Integer.MAX_VALUE );
			D[pat.length] = D_prev[pat.length]+1;
			for ( int i=pat.length-1; i>=0; --i ) {
				D[i] = Math.min( D[i], D[i+1]+1 );
				D[i] = Math.min( D[i], D_prev[i]+1 );
				if ( pat[i] == seq[j] ) D[i] = Math.min( D[i], D_prev[i+1] );
				else D[i] = Math.min( D[i], D_prev[i+1]+2 );
			}
			if ( D[0] <= min_error ) {
				min_error = D[0];
				min_pos = end - j;
			}
			if (debug) System.out.println( Arrays.toString( D ) );
			for ( int i=pat.length; i>=0; --i ) D_prev[i] = D[i];
		}
		return new IntegerPair( min_error, min_pos );
		
		/*
		 * Note: there can be multiple positions with min_error.
		 * Currently, the leftmost position is selected among them.
		 */
	}

	@Override
	public String getName() {
		return "DeltaValidator";
	}
}
