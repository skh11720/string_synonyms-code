package bloomfilter;

public class Signature {
	private static final int INTSIZE = 32;
	int[] bitmap;

	Signature( int m ) {
		int arrsize = (int) Math.ceil( ( (double) m ) / INTSIZE );
		bitmap = new int[ arrsize ];
	}

	void add( int idx ) {
		int blockidx = ( idx / INTSIZE );
		int withinblockidx = idx % INTSIZE;
		bitmap[ blockidx ] |= 1 << withinblockidx;
	}

	/**
	 * Check whether a set owns this signature contains
	 * every item in another set
	 */
	public boolean contains( Signature o ) {
		assert ( bitmap.length == o.bitmap.length );
		for( int i = 0; i < bitmap.length; ++i ) {
			int intersection = bitmap[ i ] & o.bitmap[ i ];
			if( intersection != o.bitmap[ i ] )
				return false;
		}
		return true;
	}
}