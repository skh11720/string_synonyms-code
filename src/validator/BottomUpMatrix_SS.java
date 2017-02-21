package validator;

import mine.Record;
import tools.Rule;
import tools.StaticFunctions;

public class BottomUpMatrix_SS extends Validator {
	private final boolean useEE;
	private final boolean useEP;

	/**
	 * Temporary matrix to save match result of DP_SingleSide
	 */
	private static int matrix[][] = new int[ 1 ][ 0 ];
	/**
	 * Temporary matrix to save sum of matrix values
	 */
	private static int[][] P = new int[ 1 ][ 0 ];

	public BottomUpMatrix_SS( boolean useEE, boolean useEP ) {
		this.useEE = useEE;
		this.useEP = useEP;
	}

	/**
	 * Check if s can be transformed to t
	 *
	 * @param s
	 * @param t
	 * @return true if s can be transformed to t
	 */
	public int isEqual( Record s, Record t ) {
		++checked;
		/**
		 * If temporary matrix size is not enough, enlarge the space
		 */
		if( s.size() >= matrix.length || t.size() >= matrix[ 0 ].length ) {
			int rows = Math.max( s.size() + 1, matrix.length );
			int cols = Math.max( t.size() + 1, matrix[ 0 ].length );
			matrix = new int[ rows ][ cols ];
			matrix[ 0 ][ 0 ] = 1;
		}
		if( s.size() >= P.length || t.size() >= P[ 0 ].length ) {
			int rows = Math.max( s.size() + 1, P.length );
			int cols = Math.max( t.size() + 1, P[ 0 ].length );
			P = new int[ rows ][ cols ];
			P[ 0 ][ 0 ] = 1;
			for( int i = 1; i < rows; ++i )
				P[ i ][ 0 ] = 1;
			for( int i = 1; i < cols; ++i )
				P[ 0 ][ i ] = 1;
		}
		short maxsearchrange = s.getMaxSearchRange();
		short maxinvsearchrange = s.getMaxInvSearchRange();
		for( int i = 1; i <= s.size(); ++i ) {
			/* P[i][j]: sum_{i'=0}^i sum_{j'=0}^j M[i',j'] */
			int Q = 0;
			short searchrange = s.getSearchRange( i - 1 );
			short invsearchrange = s.getInvSearchRange( i - 1 );

			int ip = Math.max( 0, i - searchrange );
			for( int j = 1; j <= t.size(); ++j ) {
				/* Q: sum_{j'=0}^{j-1} M[i,j'] */
				++niterentry;
				matrix[ i ][ j ] = 0;
				int jp = Math.max( 0, j - invsearchrange );
				int valid = 1;
				if( useEE ) {
					/* Claim 1
					 * Let i' = i - searchrange and j' = j - invsearchrange.
					 * M[i,j] = 1
					 * ==> M[i'..i, j'..j] has at least one 1 (except M[i,j])
					 * <==> P[i-1, j] + Q - P[i'-1, j-1] - P[i-1, j'-1] + P[i'-1, j'-1]
					 * \neq
					 * 0
					 * If there is no empty string in lhs/rhs of a rule,
					 * <==> P[i-1, j-1] - P[i'-1, j-1] - P[i-1, j'-1] + P[i'-1, j'-1] \neq
					 * 0 */
					valid = P[ i - 1 ][ j - 1 ];
					if( ip > 0 ) {
						valid -= P[ ip - 1 ][ j - 1 ];
						if( jp > 0 )
							valid += P[ ip - 1 ][ jp - 1 ];
					}
					if( jp > 0 )
						valid -= P[ i - 1 ][ jp - 1 ];
				}

				/* compute matrix[i][j], P[i][j] and Q.
				 * Note that P[i][j] = P[i-1][j] + Q + matrix[i][j]. */
				if( valid > 0 ) {
					Rule[] rules = s.getSuffixApplicableRules( i - 1 );
					for( Rule rule : rules ) {
						++niterrules;
						int[] lhs = rule.getFrom();
						int[] rhs = rule.getTo();
						if( j - rhs.length < 0 )
							continue;
						else if( matrix[ i - lhs.length ][ j - rhs.length ] == 0 )
							continue;
						else if( StaticFunctions.compare( rhs, 0, t.getTokenArray(), j - rhs.length, rhs.length ) == 0 ) {
							matrix[ i ][ j ] = matrix[ i - lhs.length ][ j - rhs.length ];
							if( !StaticFunctions.isSelfRule( rule ) )
								++matrix[ i ][ j ];
							++Q;
							break;
						}
					}
				}
				else
					++earlyevaled;

				/* Q is updated. (Q = sum_{j'=1}^j M[i,j'])
				 * Thus, P[i][j] = P[i-1][j] + Q. */
				P[ i ][ j ] = P[ i - 1 ][ j ] + Q;

				if( useEP ) {
					/* Claim 2
					 * Let i' = i - maxsearchrange and j' = j - maxinvsearchrange.
					 * P[i, j] - P[i', j'] = 0
					 * <==> s cannot be transformed to t */
					valid = P[ i ][ j ];
					if( i >= maxsearchrange && j >= maxinvsearchrange )
						valid -= P[ i - maxsearchrange ][ j - maxinvsearchrange ];
					if( valid == 0 ) {
						++earlystopped;
						return -1;
					}
				}
			}
		}
		return matrix[ s.size() ][ t.size() ] - 1;
	}

}
