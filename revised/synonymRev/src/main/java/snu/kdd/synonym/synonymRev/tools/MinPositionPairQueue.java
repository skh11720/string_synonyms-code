package snu.kdd.synonym.synonymRev.tools;

import java.util.Comparator;
import java.util.PriorityQueue;

public class MinPositionPairQueue {

	PriorityQueue<MinPosition> pq;
	public static final MinPositionComparator comp = new MinPositionComparator();
	int nIndex;
	public double minInvokes = Integer.MAX_VALUE;

	public MinPositionPairQueue( int nIndex ) {
		pq = new PriorityQueue<MinPosition>( nIndex, comp );
		this.nIndex = nIndex;
	}

	public void add( int index, double overlap, double invokes ) {
		MinPosition mp = new MinPosition( index, overlap, invokes );

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

	public int pollIndex() {
		return pq.poll().positionIndex;
	}

	private static class MinPositionComparator implements Comparator<MinPosition> {
		@Override
		public int compare( MinPosition x, MinPosition y ) {
			if( x.overlapCount > y.overlapCount ) {
				return -1;
			}
			else if( x.overlapCount < y.overlapCount ) {
				return 1;
			}
			else if( x.candidateCount > y.candidateCount ) {
				return -1;
			}
			else if( x.candidateCount < y.candidateCount ) {
				return 1;
			}
			return 0;
		}

	}

	private static class MinPosition {
		int positionIndex;
		double overlapCount;
		double candidateCount;

		public MinPosition( int pos, double overlap, double invokes ) {
			positionIndex = pos;
			overlapCount = overlap;
			candidateCount = invokes;
		}

		@Override
		public String toString() {
			return positionIndex + " " + overlapCount + " " + candidateCount;
		}
	}

	// Main function to test priorirty queue
	public static void main( String args[] ) {

		PriorityQueue<MinPosition> pq = new PriorityQueue<MinPosition>( 3, comp );

		MinPosition mp = new MinPosition( 1, 10, 10 );

		if( pq.size() < 3 || comp.compare( mp, pq.peek() ) > 0 ) {
			if( pq.size() == 3 ) {
				pq.poll();
			}
			pq.add( mp );
		}

		mp = new MinPosition( 2, 10, 6 );
		if( pq.size() < 3 || comp.compare( mp, pq.peek() ) > 0 ) {
			if( pq.size() == 3 ) {
				pq.poll();
			}
			pq.add( mp );
		}

		mp = new MinPosition( 3, 16, 16 );
		if( pq.size() < 3 || comp.compare( mp, pq.peek() ) > 0 ) {
			if( pq.size() == 3 ) {
				pq.poll();
			}
			pq.add( mp );
		}

		mp = new MinPosition( 4, 18, 18 );
		if( pq.size() < 3 || comp.compare( mp, pq.peek() ) > 0 ) {
			if( pq.size() == 3 ) {
				pq.poll();
			}
			pq.add( mp );
		}

		mp = new MinPosition( 5, 17, 7 );
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
