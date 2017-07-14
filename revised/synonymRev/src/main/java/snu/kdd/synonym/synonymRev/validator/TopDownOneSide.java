package snu.kdd.synonym.synonymRev.validator;

import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;

public class TopDownOneSide extends Validator {

	@Override
	public int isEqual( Record x, Record y ) {
		++checked;
		if( areSameString( x, y ) )
			return 0;
		// DEBUG
		// System.out.println( "x " + x );
		// System.out.println( "y " + y );

		boolean[][] isValid = new boolean[ x.getTokenCount() ][ y.getTokenCount() ];
		boolean[][] isEquiv = new boolean[ x.getTokenCount() ][ y.getTokenCount() ];

		boolean isEqual = getMyEqual( x, y, x.getTokenCount() - 1, y.getTokenCount() - 1, isValid, isEquiv );
		if( isEqual ) {
			return 1;
		}
		else {
			return -1;
		}
	}

	/**
	 * Get the value of M_x[i][j][""].<br/>
	 * If the value is not computed, compute and then return the value.
	 */
	private boolean getMyEqual( Record x, Record y, int xIdx, int yIdx, boolean[][] isValid, boolean[][] isEquiv ) {
		++recursivecalls;
		if( xIdx == -1 && yIdx == -1 ) {
			return true;
		}
		if( xIdx == -1 || yIdx == -1 ) {
			return false;
		}

		// If this value is already computed, simply return the computed value.
		if( isValid[ xIdx ][ yIdx ] ) {
			return isEquiv[ xIdx ][ yIdx ];
		}

		// Check every rule which is applicable to a suffix of x[1..i]

		Rule[] rules = x.getSuffixApplicableRules( xIdx );

		for( int ridx = 0; ridx < rules.length; ++ridx ) {
			++niterrules;
			Rule rule = rules[ ridx ];

			int[] lhs = rule.getLeft();
			int[] rhs = rule.getRight();

			int[] yTokens = y.getTokens();

			boolean candidate = true;
			int baseIdx = yIdx - rhs.length + 1;
			if( baseIdx < 0 ) {
				candidate = false;
			}
			else {
				for( int i = 0; i < rhs.length; i++ ) {
					if( yTokens[ baseIdx + i ] != rhs[ i ] ) {
						candidate = false;
						break;
					}
				}
			}

			if( candidate ) {
				// System.out.println( "Cand rule: " + rule.toTextString( Record.strlist ) );

				boolean equiv = getMyEqual( x, y, xIdx - lhs.length, yIdx - rhs.length, isValid, isEquiv );
				if( equiv ) {
					isValid[ xIdx ][ yIdx ] = true;
					isEquiv[ xIdx ][ yIdx ] = true;
					return true;
				}
			}

		}

		isValid[ xIdx ][ yIdx ] = true;
		isEquiv[ xIdx ][ yIdx ] = false;

		return false;
	}
}
