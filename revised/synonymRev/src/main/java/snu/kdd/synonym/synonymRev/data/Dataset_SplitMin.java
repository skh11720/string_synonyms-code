package snu.kdd.synonym.synonymRev.data;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.Util;

public class Dataset_SplitMin {
	String name;
	Int2ObjectOpenHashMap<ObjectArrayList<Record>> recordListMap;
	IntArrayList keySet;
	Int2IntOpenHashMap maxLengthMap;
	int nRecord;

	public Dataset_SplitMin( Dataset ds, boolean oneSideJoin ) {
		recordListMap = new Int2ObjectOpenHashMap<>();
		keySet = new IntArrayList();
		if( !oneSideJoin ) {
			maxLengthMap = new Int2IntOpenHashMap();
		}
		nRecord = ds.nRecord;

		for( Record r : ds.recordList ) {
			int key;
			if( !oneSideJoin ) {
				int[] range = r.getTransLengths();
				key = range[ 0 ];
			}
			else {
				key = r.getTokenCount();
			}

			ObjectArrayList<Record> recordList = recordListMap.get( key );

			if( recordList == null ) {
				recordList = new ObjectArrayList<>();

				recordListMap.put( key, recordList );
				keySet.add( key );

			}

			if( oneSideJoin ) {
				int[] range = r.getTransLengths();
				int max = maxLengthMap.get( key );
				if( max < range[ 1 ] ) {
					maxLengthMap.put( key, range[ 1 ] );
				}
			}

			recordList.add( r );
		}

		if( DEBUG.JoinBKON ) {
			for( Entry<ObjectArrayList<Record>> entry : recordListMap.int2ObjectEntrySet() ) {
				Util.printLog( entry.getIntKey() + " " + entry.getValue().size() );
			}
		}
	}

	public int keySetSize() {
		return keySet.size();
	}

	public ObjectArrayList<Record> getSplitData( int i ) {
		int key = keySet.getInt( i );

		return recordListMap.get( key );
	}

	public int getKey( int i ) {
		return keySet.getInt( i );
	}

	public int getMaxLength( int key ) {
		return maxLengthMap.get( key );
	}
}
