package snu.kdd.synonym.synonymRev.tools;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class StaticFunctions {
	/**
	 * Compare two integer arrays
	 */
	public static int compare( int[] str1, int[] str2 ) {
		if( str1.length == 0 || str2.length == 0 ) {
			return str1.length - str2.length;
		}

		int idx = 0;
		int lastcmp = 0;

		while( idx < str1.length && idx < str2.length && ( lastcmp = Integer.compare( str1[ idx ], str2[ idx ] ) ) == 0 ) {
			++idx;
		}

		if( lastcmp != 0 ) {
			return lastcmp;
		}
		else if( str1.length == str2.length ) {
			return 0;
		}
		else if( idx == str1.length ) {
			return -1;
		}
		else {
			return 1;
		}
	}

	public static boolean overlap( int min1, int max1, int min2, int max2 ) {
		if( min1 > max2 || max1 < min2 ) {
			return false;
		}
		return true;
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

	/**
	 * Merge two sorted ArrrayLists
	 */

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
}
