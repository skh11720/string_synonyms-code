package snu.kdd.synonym.synonymRev.data;

import java.util.ArrayList;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerPair;
import snu.kdd.synonym.synonymRev.tools.Util;

public class Dataset_Split {
	String name;
	Object2ObjectOpenHashMap<IntegerPair, ObjectArrayList<Record>> recordListMap;
	ArrayList<IntegerPair> keySet;
	int nRecord;

	public Dataset_Split( Dataset ds, boolean oneSideJoin ) {
		recordListMap = new Object2ObjectOpenHashMap<>();
		keySet = new ArrayList<>();
		nRecord = ds.nRecord;

		for( Record r : ds.recordList ) {
			IntegerPair pair = null;
			if( !oneSideJoin ) {
				int[] range = r.getTransLengths();
				pair = new IntegerPair( range[ 0 ], range[ 1 ] );
			}
			else {
				int count = r.getTokenCount();
				pair = new IntegerPair( count, count );
			}

			ObjectArrayList<Record> recordList = recordListMap.get( pair );

			if( recordList == null ) {
				recordList = new ObjectArrayList<>();

				recordListMap.put( pair, recordList );
				keySet.add( pair );
			}

			recordList.add( r );
		}

		if( DEBUG.JoinBKON ) {
			for( Entry<IntegerPair, ObjectArrayList<Record>> entry : recordListMap.object2ObjectEntrySet() ) {
				Util.printLog( entry.getKey() + " " + entry.getValue().size() );
			}
		}
	}

	public int keySetSize() {
		return keySet.size();
	}

	public ObjectArrayList<Record> getSplitData( int i ) {
		IntegerPair key = keySet.get( i );

		return recordListMap.get( key );
	}

	public IntegerPair getKey( int i ) {
		return keySet.get( i );
	}
}
