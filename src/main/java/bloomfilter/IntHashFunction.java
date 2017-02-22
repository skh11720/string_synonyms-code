package bloomfilter;

import java.util.Random;

/**
 * Maps each integer to another integer within a certain range randomly.
 * Utilizes 2-wise universal hash.
 */
public class IntHashFunction {
	private final int m;
	private static final long LARGEPRIME = 29996224275833L;
	private final long a;
	private final long b;

	IntHashFunction( int m ) {
		this.m = m;
		a = (long) ( Math.random() * ( LARGEPRIME - 1 ) ) + 1;
		b = (long) ( Math.random() * LARGEPRIME );
	}

	IntHashFunction( int m, Random rand ) {
		this.m = m;
		a = rand.nextLong() % ( LARGEPRIME - 1 ) + 1;
		b = rand.nextLong() % LARGEPRIME;
	}

	int hash( int i ) {
		long tmp = ( a * i + b ) % LARGEPRIME;
		return (int) ( tmp % m );
	}
}
