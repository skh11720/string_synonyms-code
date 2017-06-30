package tools;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;

import mine.Record;

public class JoinMinCandidateSet {
	int nIndex;

	WYK_HashMap<Record, Integer> appearingMap = null;

	public JoinMinCandidateSet( int nIndex, Record rec ) {
		this.nIndex = nIndex;

		if( rec.predictedInvokesJoinMin < 10 ) {
			appearingMap = new WYK_HashMap<Record, Integer>( 10 );
		}
		else {
			appearingMap = new WYK_HashMap<Record, Integer>( rec.predictedInvokesJoinMin * 2 );
		}
	}

	public void add( WYK_HashSet<Record> set ) {
		for( Record r : set ) {
			Integer count = appearingMap.get( r );

			if( count == null ) {
				appearingMap.put( r, 1 );
			}
			else {
				appearingMap.put( r, count + 1 );
			}
		}
	}

	public ArrayList<Record> getCandSet() {
		ArrayList<Record> list = new ArrayList<Record>( appearingMap.size() );

		Iterator<Entry<Record, Integer>> iter = appearingMap.entrySet().iterator();
		while( iter.hasNext() ) {
			Entry<Record, Integer> entry = iter.next();

			Record r = entry.getKey();
			if( r.indexedCountJoinMin == entry.getValue() ) {
				list.add( r );
			}
		}

		return list;
	}
}
