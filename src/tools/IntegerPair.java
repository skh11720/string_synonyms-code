package tools;

public class IntegerPair {
	public int i1;
	public int i2;
	// private static final long bigprime = 32416190071L;
	private static final long bigprime = 179428399;

	public IntegerPair( int i1, int i2 ) {
		this.i1 = i1;
		this.i2 = i2;
	}

	public boolean equals( Object o ) {
		IntegerPair oip = (IntegerPair) o;
		return ( i1 == oip.i1 ) && ( i2 == oip.i2 );
	}

	public int hashCode() {
		long merged = ( (long) i1 << 32 ) + i2;
		merged %= bigprime;
		return (int) ( merged % Integer.MAX_VALUE );
	}

	public String toString() {
		return String.format( "%d,%d", i1, i2 );
	}
}
