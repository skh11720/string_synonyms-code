package tools;

import java.util.Random;

public class RandHash {
	private static final long largePrime = 22801768001L;
	private static final Random rand = new Random();
	private final long a;
	private final long b;

	public RandHash() {
		a = 1 + ( rand.nextLong() % ( largePrime - 1 ) );
		b = rand.nextLong() % largePrime;
	}

	public int get( int id ) {
		return (int) ( ( ( a * id + b ) % largePrime ) % Integer.MAX_VALUE );
	}
}
