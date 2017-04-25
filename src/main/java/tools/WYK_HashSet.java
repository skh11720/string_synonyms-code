package tools;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class WYK_HashSet<T> implements Iterable<T>, Collection<T>, Set<T> {
	private Entry[] array;
	private int size;
	private double factor;
	private int nextExpandSize;

	public static long collision = 0;
	public static long resize = 0;

	public static boolean DEBUG = false;

	public WYK_HashSet() {
		factor = 0.5;
		clear( 10 );
	}

	public WYK_HashSet( int initialCapacity ) {
		factor = 0.5;
		clear( initialCapacity );
	}

	@SuppressWarnings( "unchecked" )
	public WYK_HashSet( Collection<? extends T> c ) {
		this();
		int size = Math.max( 10, (int) Math.ceil( c.size() / factor ) );
		array = (Entry[]) Array.newInstance( Entry.class, size );
		for( T i : c )
			add( i );
	}

	@SuppressWarnings( "unchecked" )
	public WYK_HashSet( T[] records ) {
		this();
		int size = Math.max( 10, (int) Math.ceil( records.length / factor ) );
		array = (Entry[]) Array.newInstance( Entry.class, size );
		for( T i : records )
			add( i );
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public boolean contains( Object o ) {
		if( o == null )
			throw new NullPointerException();
		T key = (T) o;
		int hash = key.hashCode();
		return contains( hash, key );
	}

	private boolean contains( int hash, T key ) {
		Entry curr = array[ getIdx( hash ) ];
		while( curr != null ) {
			if( curr.hash == hash && curr.record.equals( key ) )
				return true;
			curr = curr.next;
			collision++;
		}
		return false;
	}

	@Override
	public boolean add( T key ) {
		if( key == null )
			throw new NullPointerException();
		// e is already in this set
		int hash = key.hashCode();
		if( contains( hash, key ) )
			return false;

		int idx = getIdx( hash );
		Entry curr = new Entry( hash, key );
		Entry next = array[ idx ];
		curr.next = next;
		array[ idx ] = curr;

		// Expand array
		if( ++size >= nextExpandSize )
			resize( (int) ( array.length * 1.7 ) );
		return true;
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public boolean remove( Object o ) {
		T key = (T) o;
		int hash = key.hashCode();
		int idx = getIdx( hash );
		Entry prev = null;
		Entry curr = array[ idx ];
		while( curr != null ) {
			if( curr.hash == hash && curr.record.equals( key ) ) {
				// Current entry is the first element
				if( prev == null )
					array[ idx ] = curr.next;
				else
					prev.next = curr.next;
				--size;
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
	@SuppressWarnings( "unchecked" )
	private void resize( int nextSize ) {
		if( DEBUG ) {
			System.out.println( "DEBUG: resize from " + nextExpandSize + "(" + size + ") to " + nextSize );
		}

		resize++;

		nextSize = Math.max( 10, nextSize );
		Entry[] temp = array;
		array = (Entry[]) Array.newInstance( Entry.class, nextSize );
		nextExpandSize = (int) ( array.length * factor );

		// Move entries in temp to new array
		for( int curridx = 0; curridx < temp.length; ++curridx ) {
			Entry curr;
			while( ( curr = temp[ curridx ] ) != null ) {
				temp[ curridx ] = curr.next;

				int movedIdx = getIdx( curr.hash );
				curr.next = array[ movedIdx ];
				array[ movedIdx ] = curr;
			}
		}
	}

	private int getIdx( int hash ) {
		return Math.abs( hash % array.length );
	}

	@Override
	public boolean addAll( Collection<? extends T> c ) {
		int nextSize = c.size() + size;
		if( nextSize >= nextExpandSize )
			resize( (int) Math.ceil( nextSize / factor / factor ) );
		for( T i : c )
			add( i );
		return true;
	}

	@Override
	public void clear() {
		clear( 10 );
	}

	@SuppressWarnings( "unchecked" )
	private void clear( int size ) {
		array = (Entry[]) Array.newInstance( Entry.class, size );
		this.size = 0;

		if( DEBUG ) {
			System.out.println( "ArrayLength " + array.length + " " + factor + " " + (int) ( array.length * factor ) );
		}

		nextExpandSize = (int) ( array.length * factor );
	}

	public void emptyAll() {
		Arrays.fill( array, null );
		this.size = 0;
	}

	@Override
	public boolean containsAll( Collection<?> c ) {
		boolean rslt = true;
		Iterator<?> it = c.iterator();
		while( rslt && it.hasNext() ) {
			Integer curr = (Integer) it.next();
			rslt = rslt && contains( curr.intValue() );
		}
		return rslt;
	}

	@Override
	public boolean isEmpty() {
		return ( size == 0 );
	}

	@Override
	public Iterator<T> iterator() {
		return new SetIterator();
	}

	@Override
	public boolean removeAll( Collection<?> c ) {
		boolean rslt = false;
		Iterator<?> it = c.iterator();
		while( it.hasNext() ) {
			Integer curr = (Integer) it.next();
			rslt = rslt || remove( curr );
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
		SetIterator it = new SetIterator();
		int idx = 0;
		while( it.hasNext() )
			rslt[ idx++ ] = it.next();
		return rslt;
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public <U> U[] toArray( U[] a ) {
		if( a.length < size )
			a = (U[]) Array.newInstance( a[ 0 ].getClass(), size );
		SetIterator it = new SetIterator();
		int idx = 0;
		while( it.hasNext() )
			a[ idx++ ] = (U) it.next();
		return a;
	}

	@Override
	public String toString() {
		SetIterator it = new SetIterator();
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

	private final class Entry {
		int hash;
		T record;
		Entry next;

		Entry( int hash, T record ) {
			this.hash = hash;
			this.record = record;
		}

		@Override
		public String toString() {
			return record.toString();
		}
	}

	private final class SetIterator implements Iterator<T> {
		private int curridx;
		private Entry curr;

		SetIterator() {
			curridx = 0;
			while( curridx < array.length && ( curr = array[ curridx ] ) == null )
				++curridx;
		}

		@Override
		public boolean hasNext() {
			return ( curr != null );
		}

		@Override
		public T next() {
			T nextValue = curr.record;
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
