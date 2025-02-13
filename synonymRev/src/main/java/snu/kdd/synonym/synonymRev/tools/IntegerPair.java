package snu.kdd.synonym.synonymRev.tools;

public class IntegerPair {
	public final int i1;
	public final int i2;
	// private static final long bigprime = 32416190071L;
	private static final long bigprime = 179428399;

	public IntegerPair( int i1, int i2 ) {
		this.i1 = i1;
		this.i2 = i2;
	}

	public IntegerPair( int[] integers ) {
		this.i1 = integers[ 0 ];
		this.i2 = integers[ 1 ];
	}

	@Override
	public boolean equals( Object o ) {
		if( o == null ) {
			return false;
		}

		IntegerPair oip = (IntegerPair) o;
		return ( i1 == oip.i1 ) && ( i2 == oip.i2 );
	}

	@Override
	public int hashCode() {
		long merged = ( (long) i1 << 32 ) + i2;
		merged %= bigprime;
		return (int) ( merged % Integer.MAX_VALUE );
	}

	@Override
	public String toString() {
		return String.format( "%d %d ", i1, i2 );
	}
	
	public IntegerPair ordered() {
		if ( i2 < i1 )
			return new IntegerPair( i2, i1 );
		else
			return this;
	}
	
	public IntegerPair swap() {
		return new IntegerPair( i2, i1 );
	}
}
