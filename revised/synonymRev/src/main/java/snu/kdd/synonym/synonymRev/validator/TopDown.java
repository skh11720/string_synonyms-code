package snu.kdd.synonym.synonymRev.validator;

import java.util.HashMap;
import java.util.Map;

import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;

public class TopDown extends Validator {
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

		private static final int bigprime = 1645333507;

		Submatch( int i, int j, Rule rule, int idx ) {
			// Set values
			this.i = i;
			this.j = j;
			this.rule = rule;
			this.idx = idx;

			// Compute hash value
			long tmp = i;
			tmp = ( tmp << 32 ) + j;
			tmp %= bigprime;
			tmp = ( tmp << 32 ) + idx;
			tmp %= bigprime;
			if( rule != null ) {
				int[] s = rule.getRight();
				for( int k = 0; k < idx; ++k ) {
					tmp = ( tmp << 32 ) + s[ k ];
					tmp %= bigprime;
				}
			}

			// Set hash value
			this.hash = (int) ( tmp % Integer.MAX_VALUE );
		}

		@Override
		public boolean equals( Object o ) {
			if( o == null ) {
				return false;
			}
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
			else if( idx != os.idx )
				return false;
			int[] s1 = rule.getRight();
			int[] s2 = os.rule.getRight();
			for( int i = 0; i < idx; ++i )
				if( s1[ i ] != s2[ i ] )
					return false;
			return true;
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
	private static Map<Submatch, Boolean> Mx = new HashMap<Submatch, Boolean>( 1000 );
	/**
	 * Temporary matrix to save match result of doubleside equivalence check.</br>
	 * My[i][j] stores all sub-matches for x[1..i]=>z and y[1..j]=>z + str</br>
	 */
	private static Map<Submatch, Boolean> My = new HashMap<Submatch, Boolean>( 1000 );

	@Override
	public int isEqual( Record x, Record y ) {
		++checked;
		if( areSameString( x, y ) )
			return 0;
		/**
		 * If temporary matrix size is not enough, enlarge the space
		 */
		Mx.clear();
		My.clear();
		My.put( BASIS, true );
		Submatch basis = new Submatch( x.getTokenCount(), y.getTokenCount(), null, 0 );
		boolean isEqual = getMyEqual( x, y, basis );
		if( isEqual )
			return 1;
		else
			return -1;
	}

	/**
	 * Get the value of M_x[i][j][remain].<br/>
	 * If the value is not computed, compute and then return the value.
	 */
	private static boolean getMx( Record x, Record y, Submatch match ) {
		++recursivecalls;
		// If this value is already computed, simply return the computed value.
		Boolean rslt = Mx.get( match );
		if( rslt != null )
			return rslt;
		// Check every rule which is applicable to a suffix of x[1..i]
		++niterentry;
		if( match.i == 0 )
			return false;
		Rule[] rules = x.getSuffixApplicableRules( match.i - 1 );
		assert ( rules != null );
		assert ( rules.length != 0 );
		int[] str = match.rule.getRight();
		for( int ridx = 0; ridx < rules.length; ++ridx ) {
			Rule rule = rules[ ridx ];
			++niterrules;
			int[] rhs = rule.getRight();
			int lhslength = rule.getLeft().length;
			int rhsidx = rhs.length - 1;
			int remainidx = match.idx - 1;
			// Check if one is a suffix of another
			boolean isSuffix = true;
			while( isSuffix && rhsidx >= 0 && remainidx >= 0 ) {
				if( rhs[ rhsidx ] != str[ remainidx ] )
					isSuffix = false;
				--rhsidx;
				--remainidx;
			}
			// If r.rhs is not a suffix of remain (or vice versa), skip using this
			// rule
			if( !isSuffix )
				continue;
			boolean prevM = false;
			// r.rhs is shorter than remain
			if( rhs.length < match.idx ) {
				assert ( remainidx >= 0 );
				assert ( rhsidx == -1 );
				Submatch prevmatch = new Submatch( match.i - lhslength, match.j, match.rule, match.idx - rhs.length );
				// Retrieve Mx[i-|r.lhs|][j][remain - r.rhs]
				prevM = getMx( x, y, prevmatch );
			}
			// remain is shorter than r.rhs
			else if( rhs.length > match.idx ) {
				assert ( remainidx == -1 );
				assert ( rhsidx >= 0 );
				Submatch prevmatch = new Submatch( match.i - lhslength, match.j, rule, rhs.length - match.idx );
				// Retrieve My[i-|r.lhs|][j][remain - r.rhs]
				prevM = getMy( x, y, prevmatch );
			}
			// r.rhs == remain
			else {
				assert ( remainidx == -1 );
				assert ( rhsidx == -1 );
				Submatch prevmatch = new Submatch( match.i - lhslength, match.j, null, 0 );
				prevM = getMyEqual( x, y, prevmatch );
			}
			// If there exists a valid match, return true
			if( prevM ) {
				Mx.put( match, true );
				return true;
			}
		}
		// If there is no match, return false
		Mx.put( match, false );
		return false;
	}

	/**
	 * Get the value of M_y[i][j][remain].<br/>
	 * If the value is not computed, compute and then return the value.
	 */
	private static boolean getMy( Record x, Record y, Submatch match ) {
		++recursivecalls;
		// Every exact match is handled by getMyEqual(x, y, match).
		assert ( match.rule != null );
		// If this value is already computed, simply return the computed value.
		Boolean rslt = My.get( match );
		if( rslt != null )
			return rslt;
		// Check every rule which is applicable to a suffix of y[1..j]
		++niterentry;
		if( match.j == 0 )
			return false;
		Rule[] rules = y.getSuffixApplicableRules( match.j - 1 );
		assert ( rules != null );
		assert ( rules.length != 0 );
		int[] str = match.rule.getRight();
		for( int ridx = 0; ridx < rules.length; ++ridx ) {
			Rule rule = rules[ ridx ];
			++niterrules;
			int[] rhs = rule.getRight();
			int lhslength = rule.getLeft().length;
			int rhsidx = rhs.length - 1;
			int remainidx = match.idx - 1;
			// Check if one is a suffix of another
			boolean isSuffix = true;
			while( isSuffix && rhsidx >= 0 && remainidx >= 0 ) {
				if( rhs[ rhsidx ] != str[ remainidx ] )
					isSuffix = false;
				--rhsidx;
				--remainidx;
			}
			// If r.rhs is not a suffix of remain (or vice versa), skip using this
			// rule
			if( !isSuffix )
				continue;
			boolean prevM = false;
			// r.rhs is shorter than remain
			if( rhs.length < match.idx ) {
				assert ( remainidx >= 0 );
				assert ( rhsidx == -1 );
				Submatch prevmatch = new Submatch( match.i, match.j - lhslength, match.rule, match.idx - rhs.length );
				// Retrieve My[i][j-|r.lhs|][remain - r.rhs]
				prevM = getMy( x, y, prevmatch );
			}
			// remain is shorter than r.rhs
			else if( rhs.length > match.idx ) {
				assert ( remainidx == -1 );
				assert ( rhsidx >= 0 );
				Submatch prevmatch = new Submatch( match.i, match.j - lhslength, rule, rhs.length - match.idx );
				// Retrieve Mx[i][j-|r.lhs|][remain - r.rhs]
				prevM = getMx( x, y, prevmatch );
			}
			// r.rhs == remain
			else {
				assert ( remainidx == -1 );
				assert ( rhsidx == -1 );
				Submatch prevmatch = new Submatch( match.i, match.j - lhslength, null, 0 );
				prevM = getMyEqual( x, y, prevmatch );
			}
			// If there exists a valid match, return true
			if( prevM ) {
				My.put( match, true );
				return true;
			}
		}
		// If there is no match, return false
		My.put( match, false );
		return false;
	}

	/**
	 * Get the value of M_x[i][j][""].<br/>
	 * If the value is not computed, compute and then return the value.
	 */
	private static boolean getMyEqual( Record x, Record y, Submatch match ) {
		++recursivecalls;
		// If this value is already computed, simply return the computed value.
		Boolean rslt = My.get( match );
		if( rslt != null )
			return rslt;
		// Check every xule which is applicable to a suffix of y[1..j]
		++niterentry;
		if( match.j == 0 )
			return match.i == 0;
		Rule[] rules = y.getSuffixApplicableRules( match.j - 1 );
		assert ( rules != null );
		assert ( rules.length != 0 );
		for( int ridx = 0; ridx < rules.length; ++ridx ) {
			Rule rule = rules[ ridx ];
			++niterrules;
			boolean prevM = false;
			Submatch prevmatch = new Submatch( match.i, match.j - rule.getLeft().length, rule, rule.getRight().length );
			prevM = getMx( x, y, prevmatch );
			// If there exists a valid match, return true
			if( prevM ) {
				My.put( match, true );
				return true;
			}
		}
		// If there is no match, return false
		My.put( match, false );
		return false;
	}
}
