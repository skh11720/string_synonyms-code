package snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.set;

import java.util.Arrays;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;

public class SetGreedyOneSide extends AbstractSetValidator {
	
	public SetGreedyOneSide( Boolean selfJoin ) {
		super( selfJoin	 );
	}

	@Override
	protected int isEqualOneSide( Record x, Record y ) {
		// check whether x -> y
		
		// DEBUG
//		Boolean debug = false;
//		if ( x.getID() == 4199 && y.getID() == 4566 ) debug = true;
//		if ( y.getID() == 4199 && x.getID() == 4566 ) debug = true;
//		if (debug) {
//			System.out.println( x.toString()+", "+Arrays.toString( x.getTokensArray() ) );
//			System.out.println( y.toString()+", "+Arrays.toString( y.getTokensArray() ) );
//		}
		
		IntOpenHashSet ySet = new IntOpenHashSet(y.getTokensArray());
		IntOpenHashSet remaining = ySet.clone();
		int pos = x.size();
		
//		boolean isEqual = getMyEqual( , x.size() );
		
		while ( pos > 0 ) {
			float bestScore = -1;
			Rule bestRule = null;
			for ( Rule rule : x.getSuffixApplicableRules( pos-1 ) ) {

//				if (debug) System.out.println( ""+(pos-1)+", "+rule.toString() );

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

			// DEBUG
//			if ( debug ) System.out.println( "best: "+bestRule.toString() );
			
			// apply the best rule.
			for ( int token : bestRule.getRight() ) {
				remaining.remove( token );
			}
			pos -= bestRule.leftSize();
		} // end while
		
//		if ( debug ) System.out.println( "result: "+(remaining.size() == 0) );
		
		if ( remaining.size() == 0 ) return 1;
		else return -1;
	}

	@Override
	public String getName() {
		return "SetGreedyOneSide";
	}
}
