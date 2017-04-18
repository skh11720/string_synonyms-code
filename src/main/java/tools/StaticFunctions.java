package tools;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import mine.Record;

public class StaticFunctions {
	/**
	 * Compare two integer arrays str1[from1..from1+length),
	 * str2[from2..from2+length)
	 */
	public static int compare( int[] str1, int from1, int[] str2, int from2, int length ) {
		int idx1 = from1;
		int idx2 = from2;
		for( int i = 0; i < length; ++i ) {
			// Check if length bound is satisfied
			if( str1.length == idx1 ) {
				if( str2.length == idx2 )
					return 0;
				else
					return -1;
			}
			else if( str2.length == idx2 )
				return 1;
			int cmp = Integer.compare( str1[ idx1 ], str2[ idx2 ] );
			if( cmp != 0 )
				return cmp;
			++idx1;
			++idx2;
		}
		return 0;
	}

	public static long compare_cmp_counter = 0;

	/**
	 * Compare two integer arrays
	 */
	public static int compare( int[] str1, int[] str2 ) {
		if( str1.length == 0 || str2.length == 0 )
			return str1.length - str2.length;
		int idx = 0;
		int lastcmp = 0;
		while( idx < str1.length && idx < str2.length && ( lastcmp = Integer.compare( str1[ idx ], str2[ idx ] ) ) == 0 )
			++idx;
		compare_cmp_counter += ( idx + 1 );
		if( lastcmp != 0 )
			return lastcmp;
		else if( str1.length == str2.length )
			return 0;
		else if( idx == str1.length )
			return -1;
		else
			return 1;
	}

	/**
	 * Check if a given pattern is a prefix of given string
	 *
	 * @param str
	 *            The string
	 * @param pattern
	 *            The pattern
	 */
	public static boolean isPrefix( int[] str, int[] pattern ) {
		if( str.length < pattern.length )
			return false;
		for( int i = 0; i < pattern.length; ++i )
			if( str[ i ] != pattern[ i ] )
				return false;
		return true;
	}

	/**
	 * Check if a given pattern is a suffix of given string
	 *
	 * @param str
	 *            The string
	 * @param pattern
	 *            The pattern
	 */
	public static boolean isSuffix( int[] str, int[] pattern ) {
		if( str.length < pattern.length )
			return false;
		for( int i = 1; i <= pattern.length; ++i )
			if( str[ str.length - i ] != pattern[ pattern.length - i ] )
				return false;
		return true;
	}

	/**
	 * Check if two intervals overlap or not
	 */
	public static boolean overlap( int[] i1, int[] i2 ) {
		if( i1[ 0 ] > i2[ 1 ] || i1[ 1 ] < i2[ 0 ] )
			return false;
		return true;
	}

	public static boolean overlap( int min1, int max1, int min2, int max2 ) {
		if( min1 > max2 || max1 < min2 )
			return false;
		return true;
	}

	/**
	 * Copy a given string starts from given 'from' value
	 *
	 * @param src
	 *            The source string
	 * @param from
	 *            The starting index
	 */
	public static int[] copyIntegerArray( int[] src, int from ) {
		int[] rslt = new int[ src.length - from ];
		for( int i = from; i < src.length; ++i )
			rslt[ i - from ] = src[ i ];
		return rslt;
	}

	/**
	 * Copy a given string starts from given 'from' value
	 *
	 * @param src
	 *            The source string
	 * @param from
	 *            The starting index (inclusive)
	 * @param to
	 *            The last index (exclusive)
	 */
	public static int[] copyIntegerArray( int[] src, int from, int to ) {
		assert ( to < src.length );
		assert ( from <= to );
		int[] rslt = new int[ to - from ];
		for( int i = from; i < to; ++i )
			rslt[ i - from ] = src[ i ];
		return rslt;
	}

	private static class RecordIntTriple<T> {
		T rec;
		int i1;
		int i2;

		RecordIntTriple( T rec, int i1, int i2 ) {
			this.rec = rec;
			this.i1 = i1;
			this.i2 = i2;
		}
	}

	private static class RecordIntTripleComparator<T> implements Comparator<RecordIntTriple<T>> {
		Comparator<T> cmp;

		RecordIntTripleComparator( Comparator<T> cmp ) {
			this.cmp = cmp;
		}

		@Override
		public int compare( RecordIntTriple<T> o1, RecordIntTriple<T> o2 ) {
			return cmp.compare( o1.rec, o2.rec );
		}
	}

	public static long union_item_counter = 0;
	public static long union_cmp_counter = 0;

	public static <T> List<T> union( List<? extends List<T>> list, Comparator<T> cmp ) {
		if( list.size() == 0 ) {
			// return empty list
			return new ArrayList<T>();
		}
		else if( list.size() == 1 ) {
			// return list itself
			return list.get( 0 );
		}
		for( List<T> l : list ) {
			union_item_counter += l.size();
		}

		// Merge candidates
		RecordIntTripleComparator<T> ritCom = new RecordIntTripleComparator<T>( cmp );

		// sort list
		PriorityQueue<RecordIntTriple<T>> pq = new PriorityQueue<RecordIntTriple<T>>( list.size(), ritCom );
		for( int i = 0; i < list.size(); ++i ) {
			List<T> candidates = list.get( i );
			if( !candidates.isEmpty() ) {
				pq.add( new RecordIntTriple<T>( candidates.get( 0 ), i, 0 ) );
			}
		}

		List<T> candidates = new ArrayList<T>();
		T last = null;
		while( !pq.isEmpty() ) {
			RecordIntTriple<T> p = pq.poll();
			if( last != null ) {
				++union_cmp_counter;
			}
			if( last == null || cmp.compare( last, p.rec ) != 0 ) {
				last = p.rec;
				candidates.add( p.rec );
			}
			List<T> origin = list.get( p.i1 );
			++p.i2;
			if( origin.size() > p.i2 ) {
				p.rec = origin.get( p.i2 );
				pq.add( p );
			}
		}
		return candidates;
	}

	public static <T> List<T> union( List<? extends List<T>> list ) {
		if( list.size() == 0 )
			return new ArrayList<T>();
		else if( list.size() == 1 )
			return list.get( 0 );
		Set<T> set = new WYK_HashSet<T>();
		for( List<T> l : list )
			union_item_counter += l.size();
		for( List<T> currlist : list ) {
			set.addAll( currlist );
		}
		List<T> union = new ArrayList<T>( set );
		return union;
	}

	public static long inter_item_counter = 0;
	public static long inter_cmp_counter = 0;

	public static <T> List<T> intersection( List<? extends List<T>> list, Comparator<T> cmp ) {
		LinkedList<T> intersection = new LinkedList<T>();
		if( list.size() == 0 ) {
			return intersection;
		}
		else if( list.size() == 1 ) {
			return list.get( 0 );
		}
		for( List<T> l : list ) {
			inter_item_counter += l.size();
		}
		intersection.addAll( list.get( 0 ) );
		for( int i = 1; i < list.size(); ++i ) {
			List<T> src = list.get( i );
			if( src.size() == 0 || intersection.size() == 0 ) {
				intersection.clear();
				break;
			}
			Iterator<T> iter1 = intersection.iterator();
			Iterator<T> iter2 = src.iterator();
			T v1 = iter1.next();
			T v2 = iter2.next();
			while( v1 != null && v2 != null ) {
				int compare = cmp.compare( v1, v2 );
				++inter_cmp_counter;
				if( compare == 0 ) {
					v1 = iter1.hasNext() ? iter1.next() : null;
					v2 = iter2.hasNext() ? iter2.next() : null;
				}
				else if( compare < 0 ) {
					iter1.remove();
					v1 = iter1.hasNext() ? iter1.next() : null;
				}
				else
					v2 = iter2.hasNext() ? iter2.next() : null;
			}
			if( v1 != null && v2 == null ) {
				while( true ) {
					iter1.remove();
					if( iter1.hasNext() )
						iter1.next();
					else
						break;
				}
			}
		}
		return intersection;
	}

	public static <T> List<T> intersection2( List<? extends List<T>> list, Comparator<T> cmp ) {
		if( list.size() == 0 )
			return new ArrayList<T>();
		else if( list.size() == 1 )
			return list.get( 0 );
		for( List<T> l : list )
			inter_item_counter += l.size();
		RecordIntTripleComparator<T> ritCom = new RecordIntTripleComparator<T>( cmp );
		PriorityQueue<RecordIntTriple<T>> pq = new PriorityQueue<RecordIntTriple<T>>( list.size(), ritCom );
		for( int i = 0; i < list.size(); ++i ) {
			List<T> candidates = list.get( i );
			if( !candidates.isEmpty() )
				pq.add( new RecordIntTriple<T>( candidates.get( 0 ), i, 0 ) );
		}
		List<T> candidates = new ArrayList<T>();
		T last = null;
		int count = 0;
		while( !pq.isEmpty() ) {
			RecordIntTriple<T> p = pq.poll();
			if( last != null )
				++inter_cmp_counter;
			if( last == null || cmp.compare( last, p.rec ) != 0 ) {
				last = p.rec;
				count = 1;
				if( pq.size() != list.size() - 1 )
					break;
			}
			else if( ++count == list.size() )
				candidates.add( last );
			List<T> origin = list.get( p.i1 );
			++p.i2;
			if( origin.size() > p.i2 ) {
				p.rec = origin.get( p.i2 );
				pq.add( p );
			}
		}
		return candidates;
	}

	/**
	 * Merge two sorted ArrrayLists
	 */
	public static List<Record> union( ArrayList<Record> al1, ArrayList<Record> al2 ) {
		ArrayList<Record> rslt = new ArrayList<Record>();
		int idx1 = 0, idx2 = 0;
		while( idx1 < al1.size() && idx2 < al2.size() ) {
			Record r1 = al1.get( idx1 );
			Record r2 = al2.get( idx2 );
			int cmp = r1.compareTo( r2 );
			if( cmp == 0 ) {
				rslt.add( r1 );
				++idx1;
				++idx2;
			}
			else if( cmp < 0 ) {
				rslt.add( r1 );
				++idx1;
			}
			else {
				rslt.add( r2 );
				++idx2;
			}
		}
		while( idx1 < al1.size() )
			rslt.add( al1.get( idx1++ ) );
		while( idx2 < al2.size() )
			rslt.add( al2.get( idx2++ ) );
		return rslt;
	}

	/**
	 * Do intersection between two sorted ArrrayLists
	 */
	public static List<Record> intersection( ArrayList<Record> al1, ArrayList<Record> al2 ) {
		ArrayList<Record> rslt = new ArrayList<Record>();
		int idx1 = 0, idx2 = 0;
		while( idx1 < al1.size() && idx2 < al2.size() ) {
			Record r1 = al1.get( idx1 );
			Record r2 = al2.get( idx2 );
			int cmp = r1.compareTo( r2 );
			if( cmp == 0 ) {
				rslt.add( r1 );
				++idx1;
				++idx2;
			}
			else if( cmp < 0 )
				++idx1;
			else
				++idx2;
		}
		return rslt;
	}

	// public static boolean isSelfRule( Rule rule ) {
	// return rule.getFrom()[ 0 ] == rule.getTo()[ 0 ] && rule.getFrom().length == 1 && rule.getTo().length == 1;
	// }

	public static <T> void write2file( List<T> list, String filename ) throws IOException {
		BufferedWriter bw = new BufferedWriter( new FileWriter( filename ) );
		for( T rec : list )
			bw.write( rec.toString() + "\n" );
		bw.close();
	}
}