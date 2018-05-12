package snu.kdd.synonym.synonymRev.algorithm.pqFilterDP;

import java.util.Arrays;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class SetGreedyOneSide extends Validator {
	
	private Record x, y;
	
	protected static boolean areSameString( Record s, Record t ) {
		if( s.getTokenCount() != t.getTokenCount() ) {
			return false;
		}

		int[] si = s.getTokensArray();
		int[] ti = t.getTokensArray();
		Arrays.sort( si );
		Arrays.sort( ti );
		for( int i = 0; i < si.length; ++i ) {
			if( si[ i ] != ti[ i ] ) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int isEqual( Record x, Record y ) {
		// Check whether x -> y

		++checked;
		if( areSameString( x, y ) )
			return 0;
		// DEBUG
		// System.out.println( "x " + x );
		// System.out.println( "y " + y );
		
		this.x = x;
		this.y = y;
		IntOpenHashSet ySet = new IntOpenHashSet(y.getTokensArray());
		IntOpenHashSet remaining = ySet.clone();
		int pos = x.size();
		
//		boolean isEqual = getMyEqual( , x.size() );
		
		while ( pos > 0 ) {
			float bestScore = -1;
			Rule bestRule = null;
			for ( Rule rule : x.getSuffixApplicableRules( pos-1 ) ) {
				++niterrules;
				// check whether all tokens in rhs are in y.
				float score = 0;
				Boolean isValidRule = true;
				for ( int token : rule.getRight() ) {
					if ( !ySet.contains( token ) ) {
						isValidRule = false;
						break;
					}
					if ( remaining.contains( token ) ) ++score;
				}
				if ( isValidRule ) {
					score /= rule.rightSize();
					if ( score > bestScore ) {
						bestScore = score;
						bestRule = rule;
					}
				}
			}
			
			// If there is no valid rule, return -1.
			if ( bestRule == null ) return -1;
			
			// apply the best rule.
			for ( int token : bestRule.getRight() ) {
				remaining.remove( token );
			pos -= bestRule.leftSize();
			}
		} // end while
		
		if ( remaining.size() == 0 ) return 1;
		else return -1;
	}

	@Override
	public String getName() {
		return "SetGreedyOneSide";
	}
}
