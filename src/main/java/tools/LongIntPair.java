package tools;

public class LongIntPair {
	public long l;
	public int i;

	public LongIntPair( long l, int i ) {
		this.l = l;
		this.i = i;
	}

	@Override
	public boolean equals( Object o ) {
		if( o != null ) {
			LongIntPair oip = (LongIntPair) o;
			return ( l == oip.l ) && ( i == oip.i );
		}
		return false;
	}

	@Override
	public int hashCode() {
		return ( (int) l ) ^ 0x1f1f1f1f + i;
	}

	@Override
	public String toString() {
		return String.format( "%d,%d", l, i );
	}
}
