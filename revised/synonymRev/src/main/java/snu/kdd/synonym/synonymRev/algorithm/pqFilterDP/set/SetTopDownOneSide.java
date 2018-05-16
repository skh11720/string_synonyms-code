package snu.kdd.synonym.synonymRev.algorithm.pqFilterDP.set;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;

public class SetTopDownOneSide extends AbstractSetValidator {
	
	private Object2BooleanOpenHashMap<MemKey> isEquiv = new Object2BooleanOpenHashMap<MemKey>();
	private Record leftRec, rightRec;
	private IntOpenHashSet rightTokenSet;
	
	public SetTopDownOneSide( Boolean selfJoin ) {
		super( selfJoin );
	}

	@Override
	protected int isEqualOneSide( Record x, Record y ) {
		// Check whether x -> y

		// DEBUG
		// System.out.println( "x " + x );
		// System.out.println( "y " + y );
		
		this.leftRec = x;
		this.rightRec = y;
		this.rightTokenSet = new IntOpenHashSet(y.getTokensArray());
		isEquiv.clear();

		// DEBUG
//		debug = false;
//		if ( leftRec.getID() == 7706 && rightRec.getID() == 7707 ) debug = true;
//		if ( rightRec.getID() == 7706 && leftRec.getID() == 7707 ) debug = true;
//		if (debug) System.out.println( leftRec.toString() + ", "+ Arrays.toString( leftRec.getTokensArray() ) );
//		if (debug) System.out.println( rightRec.toString() + ", "+ Arrays.toString( rightRec.getTokensArray() ) );
		
		boolean isEqual = getMyEqual( new IntOpenHashSet(), x.size() );
		if( isEqual ) {
			return 1;
		}
		else {
			return -1;
		}
	}

	/**
	 * pos starts from 1.
	 */
	private boolean getMyEqual( IntOpenHashSet generated, int pos ) {
		++recursivecalls;

		// DEBUG
//		if (debug) System.out.println( generated.toString()+"\t"+pos );
//		if (debug && pos == 0) {
//			System.out.println( "RESULT" );
//			System.out.println( (new IntOpenHashSet( rightRec.getTokensArray() )).toString() );
//			System.out.println( generated.toString() );
//			System.out.println( generated.equals( new IntOpenHashSet( rightRec.getTokensArray()) ) );
//		}
		/////////////

		if ( generated.equals( new IntOpenHashSet( rightRec.getTokensArray()) ) && pos <= 0 ) return true;
		if ( pos == 0 ) return false;

		// If this value is already computed, simply return the computed value.
		MemKey key = new MemKey( generated.toIntArray(), pos );
		if( isEquiv.containsKey( key ) ) return isEquiv.getBoolean( key );

		// recursion
		for ( Rule rule : leftRec.getSuffixApplicableRules( pos-1 ) ) {
			++niterrules;
			// check whether all tokens in rhs are in y.
			Boolean isValidRule = true;
			for ( int token : rule.getRight() )
				if ( !rightTokenSet.contains( token ) ) {
					isValidRule = false;
					break;
				}

			// DEBUG
//			if ( debug ) System.out.println( rule.toString()+", "+isValidRule );

			if ( !isValidRule ) continue;
			
			// make the union set of generated tokens by applying the current rule.
			IntOpenHashSet generatedMore = generated.clone();
			for ( int token : rule.getRight() ) generatedMore.add( token );
			if ( getMyEqual( generatedMore, pos-rule.leftSize() ) ) {
				isEquiv.put( key, true );
				return true;
			}
		}
		isEquiv.put( key, false );
		return false;
	}

	@Override
	public String getName() {
		return "SetTopDownOneSide";
	}
	
	private class MemKey {
		
		int[] arr;
		int n;
		int hash;
		
		// TODO: test the case with an IntegerSet and an integer as the key.
		public MemKey( int[] arr, int n) {
			this.arr = arr;
			this.n = n;
			for ( int i=0; i<arr.length; i++ ) hash = 0x1f1f1f1f ^ hash + arr[i];
			hash = 0x1f1f1f1f ^ hash + n;
		}

		@Override
		public boolean equals( Object obj ) {
			if ( obj == null ) return false;
			if (obj == this ) return true;

			MemKey o = (MemKey)obj;
			if ( this.arr.length != o.arr.length ) return false;
			if ( this.n != o.n ) return false;
			for ( int i=0; i<arr.length; i++ ) {
				if ( this.arr[i] != o.arr[i] ) return false;
			}
			return true;
		}
		
		@Override
		public int hashCode() {
			return hash;
		}
	}
}
