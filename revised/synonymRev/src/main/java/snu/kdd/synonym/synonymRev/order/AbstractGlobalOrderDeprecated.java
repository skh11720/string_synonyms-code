package snu.kdd.synonym.synonymRev.order;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.order.AbstractGlobalOrder.Ordering;

abstract public class AbstractGlobalOrderDeprecated<K> {

	protected final Ordering mode;
	protected Object2LongOpenHashMap<K> orderMap = null;
	
	public AbstractGlobalOrderDeprecated(String mode) {
		this.mode = Ordering.valueOf( mode );
	}

	abstract protected List<K> parseRule(Rule rule, int pos);
	abstract public int compare( K o1, K o2 );
	abstract public long getOrder( K o );
	
	public void initOrder( Query query ) {
		if ( mode != Ordering.FF ) return;
		Object2IntOpenHashMap<K> counter = new Object2IntOpenHashMap<K>();
		counter.defaultReturnValue( 0 );
		for ( Record recS : query.searchedSet.recordList ) {
			for ( int i=0; i<recS.size(); i++ ) {
				for ( Rule rule : recS.getSuffixApplicableRules( i ) ) {
					List<K> keyList = parseRule( rule, i );
					if ( keyList == null ) continue;
					for ( K key : keyList ) counter.put( key, counter.getInt( key )+1 );
				}
			}
		}

		orderMap = new Object2LongOpenHashMap<K>( counter.size() );
		Stream<Entry<K, Integer>> stream = counter.entrySet().stream().sorted( Map.Entry.comparingByValue() );
		Iterator<Entry<K, Integer>> iter = stream.iterator();
		for ( int i=0; i<counter.size(); i++ ) {
			Entry<K, Integer> entry = iter.next();
			orderMap.put( entry.getKey(), i );
		}
	}
	
	public Ordering getMode() {
		return mode;
	}
}
