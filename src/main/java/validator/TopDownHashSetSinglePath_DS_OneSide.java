package validator;

import mine.Record;
import tools.Rule;

public class TopDownHashSetSinglePath_DS_OneSide extends Validator {

	@Override
	public int isEqual( Record x, Record y ) {
		// DEBUG
		System.out.println( x );
		System.out.println( y );

		boolean[][] isValid = new boolean[ x.size() ][ y.size() ];
		boolean[][] isEquiv = new boolean[ x.size() ][ y.size() ];

		boolean isEqual = getMyEqual( x, y, x.size() - 1, y.size() - 1, isValid, isEquiv );
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
	private static boolean getMyEqual( Record x, Record y, int xIdx, int yIdx, boolean[][] isValid, boolean[][] isEquiv ) {

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
			Rule rule = rules[ ridx ];

			int[] lhs = rule.getFrom();
			int[] rhs = rule.getTo();

			int[] yTokens = y.getTokenArray();

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
				System.out.println( "Cand rule: " + rule.toTextString( Record.strlist ) );

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
