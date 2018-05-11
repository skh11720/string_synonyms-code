package snu.kdd.synonym.synonymRev.algorithm.pqFilterDP;

import java.util.Arrays;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class SetTopDownOneSide extends Validator {
	
	private Object2BooleanOpenHashMap<MemKey> isEquiv = new Object2BooleanOpenHashMap<MemKey>();
	private Record x, y;
	private IntOpenHashSet ySet;
	
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
		this.ySet = new IntOpenHashSet(y.getTokensArray());
		isEquiv.clear();

		
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
		if ( Arrays.equals( y.getTokensArray(), generated.toIntArray() ) && pos <= 0 ) return true;
		if ( pos == 0 ) return false;

		// If this value is already computed, simply return the computed value.
		MemKey key = new MemKey( generated.toIntArray(), pos );
		if( isEquiv.containsKey( key ) ) return isEquiv.getBoolean( key );

		// recursion
		for ( Rule rule : x.getSuffixApplicableRules( pos-1 ) ) {
			++niterrules;
			// check whether all tokens in rhs are in y.
			Boolean isValidRule = true;
			for ( int token : rule.getRight() )
				if ( !ySet.contains( token ) ) {
					isValidRule = false;
					break;
				}
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

	public String getName() {
		return "TopDownOneSide";
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
