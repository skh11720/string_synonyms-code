package tools;

import java.lang.reflect.Array;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Specialized implementation of HashSet&lt;Integer, T&gt;.
 */
public class WYK_HashMap<K, V> implements Map<K, V> {
	private Entry[] array;
	private int size;
	private double factor;
	private int nextExpandSize;

	public long getCount = 0;
	public long getIterCount = 0;

	public WYK_HashMap() {
		this( 10 );
	}

	public WYK_HashMap( int initialCapacity ) {
		factor = 0.5;
		clear( initialCapacity );
	}

	@SuppressWarnings( "unchecked" )
	public WYK_HashMap( Map<? extends K, ? extends V> c ) {
		this();
		int size = Math.max( 10, (int) Math.ceil( c.size() / factor ) );
		array = (Entry[]) Array.newInstance( Entry.class, size );
		for( Map.Entry<? extends K, ? extends V> e : c.entrySet() )
			put( e.getKey(), e.getValue() );
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public boolean containsKey( Object o ) {
		K key = (K) o;
		int hash = key.hashCode();
		return containsKey( hash, key );
	}

	private boolean containsKey( int hash, K key ) {
		Entry curr = array[ getIdx( hash ) ];
		while( curr != null ) {
			if( curr.hash == hash && curr.key.equals( key ) )
				return true;
			curr = curr.next;
		}
		return false;
	}

	@Override
	@SuppressWarnings( "unchecked" )
	public V get( Object o ) {
		++getCount;
		K key = (K) o;
		int hash = key.hashCode();
		Entry curr = array[ getIdx( hash ) ];
		while( curr != null ) {
			++getIterCount;
			if( curr.hash == hash && curr.key.equals( key ) )
				return curr.value;
			curr = curr.next;
		}
		return null;
	}

	@Override
	public V put( K key, V value ) {
		V removedValue = remove( key );

		int hash = key.hashCode();
		int idx = getIdx( hash );
		Entry curr = new Entry( hash, key, value );
		Entry next = array[ idx ];
		curr.next = next;
		array[ idx ] = curr;

		// Expand array
		if( ++size >= nextExpandSize ) {
			resize( (int) ( array.length * 1.7 ) );
		}

		return removedValue;
	}

	@Override
	@SuppressWarnings( "unchecked" )
	public V remove( Object o ) {
		K key = (K) o;

		int hash = key.hashCode();
		int idx = getIdx( hash );
		Entry prev = null;
		Entry curr = array[ idx ];
		while( curr != null ) {
			if( curr.hash == hash && curr.key.equals( key ) ) {
				// Current entry is the first element
				if( prev == null )
					array[ idx ] = curr.next;
				else
					prev.next = curr.next;
				--size;
				return curr.value;
			}
			prev = curr;
			curr = curr.next;
		}
		return null;
	}

	@Override
	public int size() {
		return size;
	}

	/**
	 * Print <br/>
	 * 1) Collision ratio (except empty entry)
	 * 2) Avg chain length (except empty entry)
	 */
	public void printStat() {
		int collisionCounter = 0;
		int nonemptyCounter = 0;
		int maxchainlength = 0;
		int maxidx = -1;
		for( int idx = 0; idx < array.length; ++idx ) {
			Entry e = array[ idx ];
			if( e == null ) {
				continue;
			}
			++nonemptyCounter;
			if( e.next != null ) {
				++collisionCounter;
			}
			Entry curr = e;
			int chainlength = 0;
			while( curr != null ) {
				curr = curr.next;
				++chainlength;
			}
			if( maxchainlength < chainlength ) {
				maxchainlength = chainlength;
				maxidx = idx;
			}
		}
		double ratio1 = (double) collisionCounter / nonemptyCounter;
		double ratio2 = (double) size() / nonemptyCounter;
		double ratio3 = (double) nonemptyCounter / array.length;
		System.out.println( "Collision ratio w/o empty : " + ratio1 );
		System.out.println( "avg chain length w/o empty : " + ratio2 );
		System.out.println( "max chain length : " + maxchainlength );
		System.out.println( "max chain length entry : " + maxidx );
		System.out.println( "occupied cell ratio : " + ratio3 );
		System.out.println( "Iters in get() : " + getIterCount );
		System.out.println( "Invocation of get() : " + getCount );
	}

	/**
	 * Expand the array with the given size
	 * 
	 * @param nextSize
	 */
	@SuppressWarnings( "unchecked" )
	private void resize( int nextSize ) {
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
		return hash % array.length;
	}

	@Override
	public String toString() {
		EntryIterator it = new EntryIterator();
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

	private final class Entry implements Map.Entry<K, V> {
		private int hash;
		private K key;
		private V value;
		private Entry next;

		Entry( int hash, K key, V value ) {
			this.hash = hash;
			this.key = key;
			this.value = value;
		}

		@Override
		public String toString() {
			return key.toString() + ":" + value.toString();
		}

		@Override
		public K getKey() {
			return key;
		}

		@Override
		public V getValue() {
			return value;
		}

		@Override
		public V setValue( V value ) {
			V prevValue = this.value;
			this.value = value;
			return prevValue;
		}
	}

	private final class KeyIterator implements Iterator<K> {
		private EntryIterator it;

		KeyIterator() {
			it = new EntryIterator();
		}

		@Override
		public boolean hasNext() {
			return it.hasNext();
		}

		@Override
		public K next() {
			return it.next().getKey();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	private final class KeySet extends AbstractSet<K> {
		@Override
		public Iterator<K> iterator() {
			return new KeyIterator();
		}

		@Override
		public int size() {
			return size;
		}
	}

	private final class ValueIterator implements Iterator<V> {
		private EntryIterator it;

		ValueIterator() {
			it = new EntryIterator();
		}

		@Override
		public boolean hasNext() {
			return it.hasNext();
		}

		@Override
		public V next() {
			return it.next().getValue();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	private final class ValueCollection extends AbstractCollection<V> {
		@Override
		public Iterator<V> iterator() {
			return new ValueIterator();
		}

		@Override
		public int size() {
			return size;
		}
	}

	private final class EntryIterator implements Iterator<Map.Entry<K, V>> {
		int curridx;
		Entry curr;

		EntryIterator() {
			curridx = 0;
			while( curridx < array.length && ( curr = array[ curridx ] ) == null )
				++curridx;
		}

		@Override
		public boolean hasNext() {
			return ( curr != null );
		}

		@Override
		public Entry next() {
			Entry nextEntry = curr;
			curr = curr.next;
			if( curr == null ) {
				++curridx;
				while( curridx < array.length && ( curr = array[ curridx ] ) == null )
					++curridx;
			}
			return nextEntry;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	private final class EntrySet extends AbstractSet<Map.Entry<K, V>> {
		@Override
		public Iterator<Map.Entry<K, V>> iterator() {
			return new EntryIterator();
		}

		@Override
		public int size() {
			return size;
		}
	}

	@Override
	public void clear() {
		Arrays.fill( array, null );
		size = 0;
	}

	@SuppressWarnings( "unchecked" )
	private void clear( int initialCapacity ) {
		array = (Entry[]) Array.newInstance( Entry.class, initialCapacity );
		size = 0;
		nextExpandSize = (int) ( array.length * factor );
	}

	@Override
	public boolean containsValue( Object arg0 ) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		return new EntrySet();
	}

	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	@Override
	public Set<K> keySet() {
		return new KeySet();
	}

	@Override
	public void putAll( Map<? extends K, ? extends V> m ) {
		for( Map.Entry<? extends K, ? extends V> e : m.entrySet() )
			put( e.getKey(), e.getValue() );
	}

	@Override
	public Collection<V> values() {
		return new ValueCollection();
	}
}
