package tools;

/**
 * Class for storing T pair
 */
public class Pair<T extends Comparable<T>> implements Comparable<Pair<T>> {
	public final T rec1;
	public final T rec2;
	/**
	 * Pre-calculate hashcode for faster comparison
	 */
	final int hash;

	public Pair( T rec1, T rec2 ) {
		this.rec1 = rec1;
		this.rec2 = rec2;
		hash = rec1.hashCode() + rec2.hashCode() ^ 0x1f1f1f1f;
	}

	@SuppressWarnings( "unchecked" )
	public boolean equals( Object o ) {
		Pair<T> sirp = (Pair<T>) o;
		if( hash != sirp.hash )
			return false;
		if( rec1.equals( sirp.rec1 ) && rec2.equals( sirp.rec2 ) )
			return true;
		return false;
	}

	public int hashCode() {
		return hash;
	}

	@Override
	public int compareTo( Pair<T> o ) {
		int cmp = rec1.compareTo( o.rec1 );
		if( cmp == 0 )
			return rec2.compareTo( o.rec2 );
		else
			return cmp;
	}

	@Override
	public String toString() {
		return "(" + rec1.toString() + ", " + rec2.toString() + ")";
	}
}
