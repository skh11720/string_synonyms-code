package snu.kdd.synonym.synonymRev.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

/**
 * Specialized implementation of HashSet&lt;Integer&gt;.
 */
public class IntegerSet implements Iterable<Integer>, Set<Integer> {
	private Entry[] array;
	private int size;
	private double factor;
	private int nextExpandSize;
	private int hash;

	public static void main( String[] args ) {
		// Testing
		int testsize = 100000;
		Random rand = new Random();
		ArrayList<Integer> tmp = new ArrayList<Integer>();
		for( int i = 0; i < testsize; ++i )
			tmp.add( rand.nextInt() );

		IntegerSet set = new IntegerSet( tmp );
		HashSet<Integer> refset = new HashSet<Integer>( tmp );
		System.out.println( set.size() == refset.size() );
	}

	public IntegerSet() {
		factor = 0.5;
		clear();
		hash = 0;
	}

	public IntegerSet( Collection<? extends Integer> c ) {
		this();
		int size = Math.max( 10, (int) Math.ceil( c.size() / factor ) );
		array = new Entry[ size ];
		for( Integer i : c )
			add( i );
	}

	public IntegerSet( int[] records ) {
		this();
		int size = Math.max( 10, (int) Math.ceil( records.length / factor ) );
		array = new Entry[ size ];
		for( int i : records )
			add( i );
	}

	public IntegerSet copy() {
		IntegerSet replica = new IntegerSet();
		replica.array = new Entry[ array.length ];
		for( int i = 0; i < array.length; ++i )
			if( array[ i ] != null )
				replica.array[ i ] = new Entry( array[ i ] );
		replica.size = size;
		replica.factor = factor;
		replica.nextExpandSize = nextExpandSize;
		replica.hash = hash;
		return replica;
	}

	public boolean containsI( int key ) {
		Entry curr = array[ getIdx( key ) ];
		while( curr != null ) {
			if( curr.record == key )
				return true;
			curr = curr.next;
		}
		return false;
	}

	public boolean addI( int key ) {
		// e is already in this set
		if( contains( key ) )
			return false;

		int idx = getIdx( key );
		Entry curr = new Entry( key );
		Entry next = array[ idx ];
		curr.next = next;
		array[ idx ] = curr;
		hash += key;

		// Expand array
		if( ++size >= nextExpandSize )
			resize( (int) ( array.length * 1.7 ) );
		return true;
	}

	public boolean removeI( int key ) {
		int idx = getIdx( key );
		Entry prev = null;
		Entry curr = array[ idx ];
		while( curr != null ) {
			if( curr.record == key ) {
				// Current entry is the first element
				if( prev == null )
					array[ idx ] = curr.next;
				else
					prev.next = curr.next;
				--size;
				hash -= key;
				return true;
			}
			prev = curr;
			curr = curr.next;
		}
		return false;
	}

	@Override
	public int size() {
		return size;
	}

	/**
	 * Expand the array with the given size
	 *
	 * @param nextSize
	 */
	private void resize( int nextSize ) {
		nextSize = Math.max( 10, nextSize );
		Entry[] temp = array;
		array = new Entry[ nextSize ];
		nextExpandSize = (int) ( array.length * factor );

		// Move entries in temp to new array
		for( int curridx = 0; curridx < temp.length; ++curridx ) {
			Entry curr;
			while( ( curr = temp[ curridx ] ) != null ) {
				temp[ curridx ] = curr.next;

				int movedIdx = getIdx( curr.record );
				curr.next = array[ movedIdx ];
				array[ movedIdx ] = curr;
			}
		}
	}

	private int getIdx( int key ) {
		key = Math.abs( key );
		if( array.length == 0 )
			System.out.println( "???" );
		return key % array.length;
	}

	@Override
	public boolean add( Integer key ) {
		if( key == null )
			throw new NullPointerException();
		return addI( key.intValue() );
	}

	@Override
	public boolean addAll( Collection<? extends Integer> c ) {
		int nextSize = c.size() + size;
		if( nextSize >= nextExpandSize )
			resize( (int) Math.ceil( nextSize / factor / factor ) );
		for( Integer i : c )
			addI( i.intValue() );
		return true;
	}

	@Override
	public void clear() {
		array = new Entry[ 10 ];
		size = 0;
		nextExpandSize = (int) ( array.length * factor );
	}

	@Override
	public boolean contains( Object o ) {
		if( o == null )
			throw new NullPointerException();
		return containsI( ( (Integer) o ).intValue() );
	}

	@Override
	public boolean containsAll( Collection<?> c ) {
		boolean rslt = true;
		Iterator<?> it = c.iterator();
		while( rslt && it.hasNext() ) {
			Integer curr = (Integer) it.next();
			rslt = rslt && containsI( curr.intValue() );
		}
		return rslt;
	}

	@Override
	public boolean isEmpty() {
		return ( size == 0 );
	}

	@Override
	public Iterator<Integer> iterator() {
		return new IntegerSetIterator();
	}

	@Override
	public boolean remove( Object o ) {
		if( o == null )
			throw new NullPointerException();
		return removeI( ( (Integer) o ).intValue() );
	}

	@Override
	public boolean removeAll( Collection<?> c ) {
		boolean rslt = false;
		Iterator<?> it = c.iterator();
		while( it.hasNext() ) {
			Integer curr = (Integer) it.next();
			rslt = rslt || removeI( curr.intValue() );
		}
		return rslt;
	}

	@Override
	public boolean retainAll( Collection<?> c ) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object[] toArray() {
		Object[] rslt = new Object[ size ];
		IntegerSetIterator it = new IntegerSetIterator();
		int idx = 0;
		while( it.hasNext() )
			rslt[ idx++ ] = it.next();
		return rslt;
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public <T> T[] toArray( T[] a ) {
		if( a.length < size )
			a = (T[]) new Integer[ size ];
		IntegerSetIterator it = new IntegerSetIterator();
		int idx = 0;
		while( it.hasNext() )
			a[ idx++ ] = (T) it.next();
		return a;
	}

	@Override
	public String toString() {
		IntegerSetIterator it = new IntegerSetIterator();
		boolean first = true;
		StringBuilder sb = new StringBuilder();
		sb.append( '[' );
		while( it.hasNext() ) {
			if( !first )
				sb.append( ", " );
			first = false;
			sb.append( it.next() );
		}
		sb.append( ']' );
		return sb.toString();
	}

	@Override
	public boolean equals( Object o ) {
		if( o == null ) {
			return false;
		}

		IntegerSet is = (IntegerSet) o;
		if( is.size != size )
			return false;
		if( is.containsAll( this ) )
			return true;
		else
			return false;
	}

	@Override
	public int hashCode() {
		return hash;
	}

	private final class Entry {
		int record;
		Entry next;

		Entry( int record ) {
			this.record = record;
		}

		Entry( Entry o ) {
			this.record = o.record;
			if( o.next != null )
				next = new Entry( o.next );
		}

		@Override
		public String toString() {
			return Integer.toString( record );
		}
	}

	private final class IntegerSetIterator implements Iterator<Integer> {
		private int curridx;
		private Entry curr;

		IntegerSetIterator() {
			curridx = 0;
			while( curridx < array.length && ( curr = array[ curridx ] ) == null )
				++curridx;
		}

		@Override
		public boolean hasNext() {
			return ( curr != null );
		}

		@Override
		public Integer next() {
			Integer nextValue = curr.record;
			curr = curr.next;
			if( curr == null ) {
				++curridx;
				while( curridx < array.length && ( curr = array[ curridx ] ) == null )
					++curridx;
			}
			return nextValue;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
