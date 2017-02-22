package validator;

import java.util.HashSet;

import mine.Record;
import tools.Rule;

public class BottomUpHashSetSinglePath_DS extends Validator {
	private static final Submatch BASIS = new Submatch( 0, 0, null, 0 );

	/**
	 * Represents a Submatch x[1..i] ==> z + str , y[1..j] ==> z
	 * (or vice versa)
	 */
	private static class Submatch {
		/**
		 * Index of x
		 */
		final int i;
		/**
		 * Index of y
		 */
		final int j;
		/**
		 * Represents a substring rule.rhs[idx..*]
		 */
		final Rule rule;
		final int idx;
		final int hash;

		Submatch( int i, int j, Rule rule, int idx ) {
			this.i = i;
			this.j = j;
			this.rule = rule;
			this.idx = idx;
			int hash = i + j;
			if( rule != null ) {
				int[] s = rule.getTo();
				for( int k = idx; k < s.length; ++k )
					hash += s[ k ];
				hash += idx;
			}
			this.hash = hash;
		}

		@Override
		public boolean equals( Object o ) {
			if( o.getClass() != Submatch.class )
				return false;
			Submatch os = (Submatch) o;
			if( hash != os.hash )
				return false;
			else if( i != os.i || j != os.j )
				return false;
			if( rule == null ) {
				if( os.rule == null )
					return true;
				else
					return false;
			}
			else if( os.rule == null )
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
	private static HashSet<Submatch> Mx = new HashSet<Submatch>();
	/**
	 * Temporary matrix to save match result of doubleside equivalence check.</br>
	 * My[i][j] stores all sub-matches for x[1..i]=>z and y[1..j]=>z + str</br>
	 */
	private static HashSet<Submatch> My = new HashSet<Submatch>();

	public int isEqual( Record x, Record y ) {
		++checked;
		if( areSameString( x, y ) )
			return 0;
		Mx.clear();
		My.clear();
		boolean isEqual = expandMy( x, y, BASIS );
		if( isEqual )
			return 1;
		else
			return -1;
	}

	/**
	 * Expand current entry M_x[i][j][remain].
	 */
	private static boolean expandMx( Record x, Record y, Submatch match ) {
		++recursivecalls;
		// Every EQUALMATCH is saved in M_y matrix
		assert ( match.rule != null );
		assert ( match.i <= x.size() );
		assert ( match.j <= y.size() );
		// If there is no applicable rule to y (Reached the end of y), return false.
		if( match.j == y.size() ) {
			Mx.add( match );
			return false;
		}
		// If this entry is already extended and evaluated, return false.
		else if( Mx.contains( match ) )
			return false;
		++niterentry;
		// Check every rule which is applicable to a prefix of y[j..*]
		Rule[] rules = y.getApplicableRules( match.j );
		assert ( rules != null );
		assert ( rules.length != 0 );
		int[] str = match.rule.getTo();
		for( int ridx = 0; ridx < rules.length; ++ridx ) {
			++niterrules;
			Rule rule = rules[ ridx ];
			int[] rhs = rule.getTo();
			int lhslength = rule.getFrom().length;
			int rhsidx = 0;
			int remainidx = match.idx;
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
				Submatch nextmatch = null;
				if( rhsidx < rhs.length )
					nextmatch = new Submatch( match.i, match.j + lhslength, rule, rhsidx );
				else
					nextmatch = new Submatch( match.i, match.j + lhslength, null, 0 );
				// Expand My[i][j + |r.lhs|][r.rhs - remain]
				boolean isEqual = expandMy( x, y, nextmatch );
				if( isEqual )
					return true;
			}
			// rhsidx reached the end of string first: r.rhs is a prefix of remain
			else {
				assert ( rhsidx == rhs.length );
				Submatch nextmatch = new Submatch( match.i, match.j + lhslength, match.rule, remainidx );
				// Expand Mx[i][j + |r.lhs|][remain - r.rhs]
				boolean isEqual = expandMx( x, y, nextmatch );
				if( isEqual )
					return true;
			}
		}
		// If there is no match, return false
		Mx.add( match );
		return false;
	}

	/**
	 * Expand the match in M_y[i][j][remain].<br/>
	 */
	private static boolean expandMy( Record x, Record y, Submatch match ) {
		++recursivecalls;
		if( match.rule == null )
			return expandEqualMy( x, y, match );
		assert ( match.i <= x.size() );
		assert ( match.j <= y.size() );
		// If there is no applicable rule to x (Reached the end of x), return false.
		if( match.i == x.size() )
			return false;
		// If this entry is already extended and evaluated, return false.
		else if( My.contains( match ) )
			return false;
		++niterentry;
		// Check every rule which is applicable to a prefix of x[i..*]
		Rule[] rules = x.getApplicableRules( match.i );
		assert ( rules != null );
		assert ( rules.length != 0 );
		int[] str = match.rule.getTo();
		for( int ridx = 0; ridx < rules.length; ++ridx ) {
			++niterrules;
			Rule rule = rules[ ridx ];
			int[] rhs = rule.getTo();
			int lhslength = rule.getFrom().length;
			int rhsidx = 0;
			int remainidx = match.idx;
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
				Submatch nextmatch = null;
				if( remainidx < str.length )
					nextmatch = new Submatch( match.i + lhslength, match.j, match.rule, remainidx );
				else
					nextmatch = new Submatch( match.i + lhslength, match.j, null, 0 );
				// Retrieve My[i + |r.lhs|][j][remain - r.rhs]
				boolean isEqual = expandMy( x, y, nextmatch );
				if( isEqual )
					return true;
			}
			// remain is shorter than r.rhs
			else {
				assert ( remainidx == str.length );
				Submatch nextmatch = new Submatch( match.i + lhslength, match.j, rule, rhsidx );
				// Expand Mx[i + |r.lhs|][j][r.rhs - remain]
				boolean isEqual = expandMx( x, y, nextmatch );
				if( isEqual )
					return true;
			}
		}
		// If there is no match, return false
		My.add( match );
		return false;
	}

	private static boolean expandEqualMy( Record x, Record y, Submatch match ) {
		assert ( match.i <= x.size() );
		assert ( match.j <= y.size() );
		// If there is no applicable rule to x (Reached the end of x),
		// If we reached the end of y, return true. otherwise, return false.
		if( match.i == x.size() )
			return match.j == y.size();
		// If this entry is already extended and evaluated, return false.
		else if( My.contains( match ) )
			return false;
		++niterentry;
		// Check every rule which is applicable to a prefix of x[i..*]
		Rule[] rules = x.getApplicableRules( match.i );
		assert ( rules != null );
		assert ( rules.length != 0 );
		// Always do expandMx
		for( int ridx = 0; ridx < rules.length; ++ridx ) {
			++niterrules;
			Rule rule = rules[ ridx ];
			int lhslength = rule.getFrom().length;
			Submatch nextmatch = new Submatch( match.i + lhslength, match.j, rule, 0 );
			// Expand Mx[i + |r.lhs|][j][r.rhs]
			boolean isEqual = expandMx( x, y, nextmatch );
			if( isEqual )
				return true;
		}
		// If there is no match, return false
		My.add( match );
		return false;
	}
}
