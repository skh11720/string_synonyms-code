package sigmod13.filter;

import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;

import sigmod13.SIRecord;
import sigmod13.SIRecordExpanded;
import tools.Rule;

public abstract class ITF_Filter {
	public ITF_Filter( List<SIRecord> tableT, List<Rule> rulelist ) {
	};

	/**
	 * Compare two terms with their ITF value.<br/>
	 * If the return value is positive, it means that t1 precedes t2. <br/>
	 * Else if the return value is negative, it means that h2 precedes t1. <br/>
	 * Otherwise, t1 is equal to t2.
	 */
	abstract public int compare( int t1, boolean t1_from_record, int t2, boolean t2_from_record );

	/**
	 * Find the first <b>k</b> tokens from the given record
	 */
	public HashSet<Integer> filter( SIRecordExpanded rec, int k ) {
		PriorityQueue<PQEntry> pqueue = new PriorityQueue<PQEntry>();
		for( int token : rec.getOriginalTokens() ) {
			PQEntry e = new PQEntry( token, true );
			if( pqueue.size() < k )
				pqueue.add( e );
			else {
				PQEntry max = pqueue.peek();
				if( max.compareTo( e ) < 0 ) {
					pqueue.poll();
					pqueue.add( e );
				}
			}
		}

		for( int token : rec.getExpandedTokens() ) {
			PQEntry e = new PQEntry( token, false );
			if( pqueue.size() < k )
				pqueue.add( e );
			else {
				PQEntry max = pqueue.peek();
				if( max.compareTo( e ) < 0 ) {
					pqueue.poll();
					pqueue.add( e );
				}
			}
		}

		HashSet<Integer> rslt = new HashSet<Integer>();
		for( PQEntry e : pqueue )
			rslt.add( e.token );
		return rslt;
	}

	private class PQEntry implements Comparable<PQEntry> {
		int token;
		boolean from_record;

		PQEntry( int token, boolean from_record ) {
			this.token = token;
			this.from_record = from_record;
		}

		@Override
		public String toString() {
			return token + "/" + from_record;
		}

		@Override
		public int compareTo( PQEntry o ) {
			return -compare( token, from_record, o.token, o.from_record );
		}
	}
}
