package tools;

import mine.Record;

public class IntegerPair {
	public int i1;
	public int i2;
	// private static final long bigprime = 32416190071L;
	private static final long bigprime = 179428399;

	public IntegerPair( int i1, int i2 ) {
		this.i1 = i1;
		this.i2 = i2;
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
		return String.format( "%d,%d", i1, i2 );
	}

	public String toStrString() {
		String s1;

		if( i1 != Integer.MAX_VALUE ) {
			s1 = Record.strlist.get( i1 );
		}
		else {
			s1 = "EOL";
		}

		String s2;
		if( i2 != Integer.MAX_VALUE ) {
			s2 = Record.strlist.get( i2 );
		}
		else {
			s2 = "EOL";
		}
		return s1 + " " + s2;
	}
}
