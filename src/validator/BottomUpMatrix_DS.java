package validator;

import java.util.HashSet;

import mine.Record;
import tools.Rule;
import tools.StaticFunctions;

public class BottomUpMatrix_DS extends Validator {
	private static final Submatch EQUALMATCH = new Submatch( null, false, 0, 0 );

	private final boolean useEE;
	private final boolean useEP;

	public BottomUpMatrix_DS( boolean useEE, boolean useEP ) {
		this.useEE = useEE;
		this.useEP = useEP;
	}

	/**
	 * Represents a remaining substring rule.rhs[idx..|rule.rhs|] for DP_A_Matrix.
	 * <br/>
	 * Represents a remaining substring rule.rhs[1..idx] for DP_A_TopdownMatrix.
	 * In DP_A_TopdownMatrix, we do not use remainS and nAppliedRules.
	 */
	private static class Submatch {
		Rule rule;
		boolean remainS;
		short idx;
		short nAppliedRules;

		Submatch( Rule rule, boolean remainS, int idx, int nAppliedRules ) {
			this.rule = rule;
			this.remainS = remainS;
			this.idx = (short) idx;
			this.nAppliedRules = (short) nAppliedRules;
		}

		@Override
		public boolean equals( Object o ) {
			if( o.getClass() != Submatch.class )
				return false;
			Submatch os = (Submatch) o;
			return ( rule == os.rule && remainS == os.remainS && idx == os.idx );
		}

		@Override
		public int hashCode() {
			if( rule == null )
				return maxRHS;
			return ( rule.hashCode() * maxRHS ) + idx;
		}
	}

	/**
	 * Temporary matrix to save match result of doubleside equivalence check.</br>
	 * dsmatrix[i][j] stores all sub-matches for s[1..i]=>x1,
	 * t[1..j]=>x2 where x1 is sub/superstring of x2 and |x1|-|x2|<=0 </br>
	 * Every rule applied to s must use this matrix to retrieve previous matches.
	 */
	@SuppressWarnings( { "unchecked" } )
	private static HashSet<Submatch>[][] dsmatrix = new HashSet[ 1 ][ 0 ];
	/**
	 * Temporary matrix to save match result of doubleside equivalence check.</br>
	 * dtmatrix[i][j] stores all sub-matches for s[1..i]=>x1,
	 * t[1..j]=>x2 where x1 is sub/superstring of x2 and |x1|-|x2|>0
	 * Every rule applied to t must use this matrix to retrieve previous matches.
	 */
	@SuppressWarnings( "unchecked" )
	private static HashSet<Submatch>[][] dtmatrix = new HashSet[ 1 ][ 0 ];
	private static final int maxRHS = 10;
	/**
	 * Temporary matrix to save sum of matrix values
	 */
	private static int[][] dsP = new int[ 1 ][ 0 ];
	private static int[][] dtP = new int[ 1 ][ 0 ];

	@SuppressWarnings( { "rawtypes", "unchecked" } )
	private static void enlargeDSMatchMatrix( int slen, int tlen ) {
		if( slen >= dsmatrix.length || tlen >= dsmatrix[ 0 ].length ) {
			int rows = Math.max( slen + 1, dsmatrix.length );
			int cols = Math.max( tlen + 1, dsmatrix[ 0 ].length );
			dsmatrix = new HashSet[ rows ][ cols ];
			dtmatrix = new HashSet[ rows ][ cols ];
			for( int i = 0; i < dsmatrix.length; ++i )
				for( int j = 0; j < dsmatrix[ 0 ].length; ++j ) {
					dsmatrix[ i ][ j ] = new HashSet();
					dtmatrix[ i ][ j ] = new HashSet();
				}
			dsmatrix[ 0 ][ 0 ].add( EQUALMATCH );
		}
	}

	private static void enlargeDSPMatrix( int slen, int tlen ) {
		if( slen >= dsP.length || tlen >= dsP[ 0 ].length ) {
			int rows = Math.max( slen + 1, dsP.length );
			int cols = Math.max( tlen + 1, dsP[ 0 ].length );
			dsP = new int[ rows ][ cols ];
			dtP = new int[ rows ][ cols ];
			dsP[ 0 ][ 0 ] = 1;
		}
	}

	public int isEqual( Record s, Record t ) {
		++checked;
		if( areSameString( s, t ) )
			return 0;
		/**
		 * If temporary matrix size is not enough, enlarge the space
		 */
		enlargeDSMatchMatrix( s.size(), t.size() );
		enlargeDSPMatrix( s.size(), t.size() );

		short smaxsearchrange = s.getMaxSearchRange();
		short tmaxsearchrange = t.getMaxSearchRange();
		for( int i = 0; i <= s.size(); ++i ) {
			int sQ = 0;
			int tQ = 0;
			for( int j = 0; j <= t.size(); ++j ) {
				if( i == 0 && j == 0 ) {
					++sQ;
					continue;
				}
				dsmatrix[ i ][ j ].clear();
				dtmatrix[ i ][ j ].clear();
				++niterentry;

				boolean s_skipped = i == 0;
				boolean t_skipped = j == 0;
				/* Applicable rules of suffixes of s[1..i] */
				if( i != 0 ) {
					int valid = 1;
					if( useEE ) {
						// Check if we can skip evaluating current entry
						short searchrange = s.getSearchRange( i - 1 );
						int ip = Math.max( 0, i - searchrange );
						valid = dsP[ i - 1 ][ j ];
						if( ip > 0 ) {
							valid -= dsP[ ip - 1 ][ j ];
							if( j > 0 )
								valid += dsP[ ip - 1 ][ j - 1 ];
						}
						if( j > 0 )
							valid -= dsP[ i - 1 ][ j - 1 ];
					}

					if( valid == 0 )
						s_skipped = true;
					else {
						Rule[] rules = s.getSuffixApplicableRules( i - 1 );
						for( Rule rule : rules ) {
							++niterrules;
							int[] lhs = rule.getFrom();
							int[] rhs = rule.getTo();
							HashSet<Submatch> prevmatches = (HashSet<Submatch>) dsmatrix[ i - lhs.length ][ j ];
							if( prevmatches.isEmpty() )
								continue;
							for( Submatch match : prevmatches ) {
								++nitermatches;
								int nextNRules = match.nAppliedRules + ( StaticFunctions.isSelfRule( rule ) ? 0 : 1 );
								// If previous match is 'equals', simply add current rule
								if( match.rule == null ) {
									dtmatrix[ i ][ j ].add( new Submatch( rule, true, 0, nextNRules ) );
									continue;
								}
								// Do not append additional rule if remaining string of previous
								// match is from s.
								else if( match.remainS )
									continue;
								int[] remainRHS = match.rule.getTo();
								int sidx = 0;
								int tidx = match.idx;
								boolean expandable = true;
								while( tidx < remainRHS.length && sidx < rhs.length && expandable ) {
									++nitertokens;
									if( rhs[ sidx ] != remainRHS[ tidx ] )
										expandable = false;
									++sidx;
									++tidx;
								}
								if( expandable ) {
									if( sidx == rhs.length && tidx == remainRHS.length ) {
										// Exact match
										if( i == s.size() && j == t.size() )
											return nextNRules;
										dsmatrix[ i ][ j ].add( new Submatch( null, false, 0, nextNRules ) );
									}
									// rhs is fully matched (!remainS)
									else if( sidx == rhs.length )
										dsmatrix[ i ][ j ].add( new Submatch( match.rule, false, tidx, nextNRules ) );
									// remainRHS is fully matched (remainS)
									else
										dtmatrix[ i ][ j ].add( new Submatch( rule, true, sidx, nextNRules ) );
								}
							}
						}
					}
				}
				/**
				 * Applicable rules of suffixes of t[1..j]
				 */
				if( j != 0 ) {
					int valid = 1;
					if( useEE ) {
						// Check if we can skip evaluating current entry
						short searchrange = t.getSearchRange( j - 1 );
						int jp = Math.max( 0, j - searchrange );
						valid = dtP[ i ][ j - 1 ];
						if( jp > 0 ) {
							valid -= dtP[ i ][ jp - 1 ];
							if( i > 0 )
								valid += dtP[ i - 1 ][ jp - 1 ];
						}
						if( i > 0 )
							valid -= dtP[ i - 1 ][ j - 1 ];
					}
					if( valid == 0 )
						t_skipped = true;
					else {
						Rule[] rules = t.getSuffixApplicableRules( j - 1 );
						for( Rule rule : rules ) {
							++niterrules;
							int[] lhs = rule.getFrom();
							int[] rhs = rule.getTo();
							HashSet<Submatch> prevmatches = (HashSet<Submatch>) dtmatrix[ i ][ j - lhs.length ];
							if( prevmatches.isEmpty() )
								continue;
							for( Submatch match : prevmatches ) {
								++nitermatches;
								int nextNRules = match.nAppliedRules + ( StaticFunctions.isSelfRule( rule ) ? 0 : 1 );
								// If previous match is 'equals', do not apply this rule
								// since a rule of s is always applied first.
								if( match.rule == null )
									continue;
								// Do not append additional rule if remaining string of previous
								// match is from t.
								else if( !match.remainS )
									continue;
								int[] remainRHS = match.rule.getTo();
								int sidx = match.idx;
								int tidx = 0;
								boolean expandable = true;
								while( sidx < remainRHS.length && tidx < rhs.length && expandable ) {
									++nitertokens;
									if( rhs[ tidx ] != remainRHS[ sidx ] )
										expandable = false;
									++sidx;
									++tidx;
								}
								if( expandable ) {
									if( tidx == rhs.length && sidx == remainRHS.length ) {
										// Exact match
										if( i == s.size() && j == t.size() )
											return nextNRules;
										dsmatrix[ i ][ j ].add( new Submatch( null, false, 0, nextNRules ) );
									}
									// rhs is fully matched (remainS)
									else if( tidx == rhs.length )
										dtmatrix[ i ][ j ].add( new Submatch( match.rule, true, sidx, nextNRules ) );
									// remainRHS is fully matched (!remainS)
									else
										dsmatrix[ i ][ j ].add( new Submatch( rule, false, tidx, nextNRules ) );
								}
							}
						}
					}
				}
				if( s_skipped && t_skipped )
					++earlyevaled;
				if( !dsmatrix[ i ][ j ].isEmpty() )
					++sQ;
				if( !dtmatrix[ i ][ j ].isEmpty() )
					++tQ;
				if( i == 0 ) {
					dsP[ i ][ j ] = sQ;
					dtP[ i ][ j ] = tQ;
				}
				else {
					dsP[ i ][ j ] = dsP[ i - 1 ][ j ] + sQ;
					dtP[ i ][ j ] = dtP[ i - 1 ][ j ] + tQ;
				}

				/* Claim 2
				 * Let i' = i - smaxsearchrange and j' = j - tmaxsearchrange.
				 * dsP[i, j] - dsP[i', j] = 0
				 * and
				 * dtP[i, j] - dsP[i, j'] = 0
				 * <==> s cannot be transformed to t */
				if( useEP ) {
					int valid = dsP[ i ][ j ] + dtP[ i ][ j ];
					if( i >= smaxsearchrange )
						valid -= dsP[ i - smaxsearchrange ][ j ];
					if( j >= tmaxsearchrange )
						valid -= dtP[ i ][ j - tmaxsearchrange ];
					if( valid == 0 ) {
						++earlystopped;
						return -1;
					}
				}
			}
		}
		return -1;
	}

}
