package validator;

import java.util.HashMap;

import mine.Record;
import tools.Rule;

public class BottomUpMatrixSinglePath_DS extends Validator {
	private static final Submatch EQUALMATCH = new Submatch( null, 0 );

	/**
	 * Represents a substring rule.rhs[idx..*]
	 */
	private static class Submatch {
		Rule rule;
		int idx;
		int hash;

		Submatch( Rule rule, int idx ) {
			this.rule = rule;
			this.idx = idx;
			if( rule != null ) {
				int[] s = rule.getTo();
				for( int i = idx; i < s.length; ++i )
					hash += s[ i ];
				hash += idx;
			}
		}

		@Override
		public boolean equals( Object o ) {
			if( o.getClass() != Submatch.class )
				return false;
			Submatch os = (Submatch) o;
			if( hash != os.hash )
				return false;
			int[] s1 = rule.getTo();
			int[] s2 = os.rule.getTo();
			int idx1 = idx, idx2 = os.idx;
			while( idx1 < s1.length && idx2 < s2.length )
				if( s1[ idx1++ ] != s2[ idx2++ ] )
					return false;
			return idx1 == s1.length && idx2 == s2.length;
		}

		@Override
		public int hashCode() {
			return hash;
		}
	}

	/**
	 * Temporary matrix to save match result of doubleside equivalence check.</br>
	 * Mx[i][j] stores all sub-matches for x[1..i]=>z + str and y[1..j]=>z </br>
	 */
	@SuppressWarnings( { "unchecked" } )
	private static HashMap<Submatch, Boolean>[][] Mx = new HashMap[ 1 ][ 0 ];
	/**
	 * Temporary matrix to save match result of doubleside equivalence check.</br>
	 * My[i][j] stores all sub-matches for x[1..i]=>z and y[1..j]=>z + str</br>
	 */
	@SuppressWarnings( "unchecked" )
	private static HashMap<Submatch, Boolean>[][] My = new HashMap[ 1 ][ 0 ];

	@SuppressWarnings( { "rawtypes", "unchecked" } )
	private static void enlargeMatrices( int slen, int tlen ) {
		if( slen >= Mx.length || tlen >= Mx[ 0 ].length ) {
			int rows = Math.max( slen + 1, Mx.length );
			int cols = Math.max( tlen + 1, Mx[ 0 ].length );
			Mx = new HashMap[ rows ][ cols ];
			My = new HashMap[ rows ][ cols ];
			for( int i = 0; i < Mx.length; ++i )
				for( int j = 0; j < Mx[ 0 ].length; ++j ) {
					Mx[ i ][ j ] = new HashMap();
					My[ i ][ j ] = new HashMap();
				}
		}
	}

	public int isEqual( Record x, Record y ) {
		++checked;
		if( areSameString( x, y ) )
			return 0;
		/**
		 * If temporary matrix size is not enough, enlarge the space
		 */
		enlargeMatrices( x.size(), y.size() );
		clearMatrix( x, y );
		boolean isEqual = expandMy( x, y, 0, 0, EQUALMATCH );
		if( isEqual )
			return 1;
		else
			return -1;
	}

	private static void clearMatrix( Record x, Record y ) {
		for( int i = 0; i <= x.size(); ++i )
			for( int j = 0; j <= y.size(); ++j ) {
				Mx[ i ][ j ].clear();
				My[ i ][ j ].clear();
			}
	}

	/**
	 * Expand current entry M_x[i][j][remain].
	 */
	private static boolean expandMx( Record x, Record y, int i, int j, Submatch remain ) {
		++recursivecalls;
		assert ( i <= x.size() );
		assert ( j <= y.size() );
		// Every EQUALMATCH is saved in M_y matrix
		assert ( remain != EQUALMATCH );
		// If there is no applicable rule to y (Reached the end of y), return false.
		if( j == y.size() ) {
			Mx[ i ][ j ].put( remain, true );
			return false;
		}
		// If this entry is already extended and evaluated, return false.
		else if( Mx[ i ][ j ].containsKey( remain ) )
			return false;
		++niterentry;
		// Check every rule which is applicable to a prefix of y[j..*]
		Rule[] rules = y.getApplicableRules( j );
		assert ( rules != null );
		assert ( rules.length != 0 );
		int[] str = remain.rule.getTo();
		for( int ridx = 0; ridx < rules.length; ++ridx ) {
			++niterrules;
			Rule rule = rules[ ridx ];
			int[] rhs = rule.getTo();
			int lhslength = rule.getFrom().length;
			int rhsidx = 0;
			int remainidx = remain.idx;
			// Check if one is a prefix of another
			boolean isPrefix = true;
			while( isPrefix && rhsidx < rhs.length && remainidx < str.length ) {
				if( rhs[ rhsidx ] != str[ remainidx ] )
					isPrefix = false;
				++rhsidx;
				++remainidx;
			}
			// If r.rhs is not a prefix of remain (or vice versa), skip using this
			// rule
			if( !isPrefix )
				continue;
			// remainidx reached the end of string first: remain is a prefix of r.rhs
			if( remainidx == str.length ) {
				assert ( rhsidx <= rhs.length );
				Submatch nextmatch = EQUALMATCH;
				if( rhsidx < rhs.length )
					nextmatch = new Submatch( rule, rhsidx );
				// Expand My[i][j + |r.lhs|][r.rhs - remain]
				boolean isEqual = expandMy( x, y, i, j + lhslength, nextmatch );
				if( isEqual )
					return true;
			}
			// rhsidx reached the end of string first: r.rhs is a prefix of remain
			else {
				assert ( rhsidx == rhs.length );
				Submatch nextmatch = new Submatch( remain.rule, remainidx );
				// Expand Mx[i][j + |r.lhs|][remain - r.rhs]
				boolean isEqual = expandMx( x, y, i, j + lhslength, nextmatch );
				if( isEqual )
					return true;
			}
		}
		// If there is no match, return false
		Mx[ i ][ j ].put( remain, true );
		return false;
	}

	/**
	 * Expand the match in M_y[i][j][remain].<br/>
	 */
	private static boolean expandMy( Record x, Record y, int i, int j, Submatch remain ) {
		++recursivecalls;
		if( remain == EQUALMATCH )
			return expandEqualMy( x, y, i, j );
		assert ( i <= x.size() );
		assert ( j <= y.size() );
		// If there is no applicable rule to x (Reached the end of x), return false.
		if( i == x.size() )
			return false;
		// If this entry is already extended and evaluated, return false.
		else if( My[ i ][ j ].containsKey( remain ) )
			return false;
		++niterentry;
		// Check every rule which is applicable to a prefix of x[i..*]
		Rule[] rules = x.getApplicableRules( i );
		assert ( rules != null );
		assert ( rules.length != 0 );
		int[] str = remain.rule.getTo();
		for( int ridx = 0; ridx < rules.length; ++ridx ) {
			++niterrules;
			Rule rule = rules[ ridx ];
			int[] rhs = rule.getTo();
			int lhslength = rule.getFrom().length;
			int rhsidx = 0;
			int remainidx = remain.idx;
			// Check if one is a prefix of another
			boolean isPrefix = true;
			while( isPrefix && rhsidx < rhs.length && remainidx < str.length ) {
				if( rhs[ rhsidx ] != str[ remainidx ] )
					isPrefix = false;
				++rhsidx;
				++remainidx;
			}
			// If r.rhs is not a prefix of remain (or vice versa), skip using this
			// rule
			if( !isPrefix )
				continue;
			// rhsidx reached the end of string first: r.rhs is a prefix of remain
			if( rhsidx == rhs.length ) {
				assert ( remainidx <= str.length );
				Submatch nextmatch = EQUALMATCH;
				if( remainidx < str.length )
					nextmatch = new Submatch( remain.rule, remainidx );
				// Retrieve My[i + |r.lhs|][j][remain - r.rhs]
				boolean isEqual = expandMy( x, y, i + lhslength, j, nextmatch );
				if( isEqual )
					return true;
			}
			// remain is shorter than r.rhs
			else {
				assert ( remainidx == str.length );
				Submatch prevmatch = new Submatch( rule, rhsidx );
				// Expand Mx[i + |r.lhs|][j][r.rhs - remain]
				boolean isEqual = expandMx( x, y, i + lhslength, j, prevmatch );
				if( isEqual )
					return true;
			}
		}
		// If there is no match, return false
		My[ i ][ j ].put( remain, true );
		return false;
	}

	private static boolean expandEqualMy( Record x, Record y, int i, int j ) {
		assert ( i <= x.size() );
		assert ( j <= y.size() );
		// If there is no applicable rule to x (Reached the end of x),
		// If we reached the end of y, return true. otherwise, return false.
		if( i == x.size() )
			return j == y.size();
		// If this entry is already extended and evaluated, return false.
		else if( My[ i ][ j ].containsKey( EQUALMATCH ) )
			return false;
		++niterentry;
		// Check every rule which is applicable to a prefix of x[i..*]
		Rule[] rules = x.getApplicableRules( i );
		assert ( rules != null );
		assert ( rules.length != 0 );
		// Always do expandMx
		for( int ridx = 0; ridx < rules.length; ++ridx ) {
			++niterrules;
			Rule rule = rules[ ridx ];
			int lhslength = rule.getFrom().length;
			Submatch prevmatch = new Submatch( rule, 0 );
			// Expand Mx[i + |r.lhs|][j][r.rhs]
			boolean isEqual = expandMx( x, y, i + lhslength, j, prevmatch );
			if( isEqual )
				return true;
		}
		// If there is no match, return false
		My[ i ][ j ].put( EQUALMATCH, true );
		return false;
	}
}
