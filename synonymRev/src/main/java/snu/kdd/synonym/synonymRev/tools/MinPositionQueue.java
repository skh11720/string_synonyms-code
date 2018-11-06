package snu.kdd.synonym.synonymRev.tools;

import java.util.Comparator;
import java.util.PriorityQueue;

public class MinPositionQueue {

	PriorityQueue<MinPosition> pq;
	public static final MinPositionComparator comp = new MinPositionComparator();
	int nIndex;
	public double minInvokes = Integer.MAX_VALUE;

	public MinPositionQueue( int nIndex ) {
		pq = new PriorityQueue<MinPosition>( nIndex, comp );
		this.nIndex = nIndex;
	}

	public void add( int index, double invokes ) {
		MinPosition mp = new MinPosition( index, invokes );

		if( pq.size() < nIndex || comp.compare( mp, pq.peek() ) > 0 ) {
			if( pq.size() == nIndex ) {
				pq.poll();
			}
			pq.add( mp );

			if( minInvokes > invokes ) {
				minInvokes = invokes;
			}
		}
	}

	public boolean isEmpty() {
		return pq.isEmpty();
	}

	public MinPosition poll() {
		return pq.poll();
	}

	public int size() {
		return pq.size();
	}

	private static class MinPositionComparator implements Comparator<MinPosition> {
		@Override
		public int compare( MinPosition x, MinPosition y ) {
			if( x.candidateCount > y.candidateCount ) {
				return -1;
			}
			if( x.candidateCount < y.candidateCount ) {
				return 1;
			}
			return 0;
		}

	}

	public static class MinPosition {
		public int positionIndex;
		public double candidateCount;

		public MinPosition( int pos, double invokes ) {
			positionIndex = pos;
			candidateCount = invokes;
		}

		@Override
		public String toString() {
			return positionIndex + " " + candidateCount;
		}

	}

	// Main function to test priorirty queue
	public static void main( String args[] ) {

		PriorityQueue<MinPosition> pq = new PriorityQueue<MinPosition>( 3, comp );

		MinPosition mp = new MinPosition( 1, 10 );

		if( pq.size() < 3 || comp.compare( mp, pq.peek() ) > 0 ) {
			if( pq.size() == 3 ) {
				pq.poll();
			}
			pq.add( mp );
		}

		mp = new MinPosition( 2, 6 );
		if( pq.size() < 3 || comp.compare( mp, pq.peek() ) > 0 ) {
			if( pq.size() == 3 ) {
				pq.poll();
			}
			pq.add( mp );
		}

		mp = new MinPosition( 3, 16 );
		if( pq.size() < 3 || comp.compare( mp, pq.peek() ) > 0 ) {
			if( pq.size() == 3 ) {
				pq.poll();
			}
			pq.add( mp );
		}

		mp = new MinPosition( 4, 18 );
		if( pq.size() < 3 || comp.compare( mp, pq.peek() ) > 0 ) {
			if( pq.size() == 3 ) {
				pq.poll();
			}
			pq.add( mp );
		}

		mp = new MinPosition( 5, 7 );
		if( pq.size() < 3 || comp.compare( mp, pq.peek() ) > 0 ) {
			if( pq.size() == 3 ) {
				pq.poll();
			}
			pq.add( mp );
		}

		while( !pq.isEmpty() ) {
			MinPosition pos = pq.poll();

			System.out.println( pos );
		}
	}
}
