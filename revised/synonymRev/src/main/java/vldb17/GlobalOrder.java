package vldb17;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;

public class GlobalOrder {

	public static enum Ordering {
		PF,
		TF,
		FF,
	}
	
	private final Ordering mode;
	private Int2IntOpenHashMap orderMap = null;
	
	public GlobalOrder( String mode ) {
		this.mode = Ordering.valueOf( mode );
	}
	
	public void initOrder( Query query ) {
		if ( mode != Ordering.FF ) return;
		Int2IntOpenHashMap counter = new Int2IntOpenHashMap();
		counter.defaultReturnValue( 0 );
		for ( Record recS : query.searchedSet.recordList ) {
			for ( int i=0; i<recS.size(); i++ ) {
				for ( Rule rule : recS.getSuffixApplicableRules( i ) ) {
					int[] rhs = rule.getRight();
					for ( int j=0; j<rhs.length; j++ ) {
						counter.put( rhs[j], counter.get( rhs[j] )+1 );
					}
				}
			}
		}

		orderMap = new Int2IntOpenHashMap( counter.size() );
		Stream<Entry<Integer, Integer>> stream = counter.entrySet().stream().sorted( Map.Entry.comparingByValue() );
		Iterator<Entry<Integer, Integer>> iter = stream.iterator();
		for ( int i=0; i<counter.size(); i++ ) {
			Entry<Integer, Integer> entry = iter.next();
			orderMap.put( entry.getKey().intValue(), i );
		}
	}
	
	public int compareQGrams( int[] qgram0, int[] qgram1 ) {
		if ( qgram0.length == 1 && qgram1.length == 1 ) return compareTokens( qgram0[0], qgram1[0] );
		int len = Math.min( qgram0.length, qgram1.length );
		int res = Integer.MAX_VALUE;
		for (int i=0; i<len; i++) {
			res = Integer.compare( qgram0[i], qgram1[i] );
			if (res != 0) return res;
		}
		if (qgram0.length > len) return 1;
		else if (qgram1.length > len) return -1;
		else return 0;
	}
	
	public int comparePosQGrams(int[] qgram0, int pos0, int[] qgram1, int pos1 ) {
		int res = Integer.MAX_VALUE;
		switch (mode) {
		case PF:
			res = Integer.compare( pos0, pos1 );
			if (res != 0 ) return res;
			else res = compareQGrams( qgram0, qgram1 );
			break;

		case TF:
			res = compareQGrams( qgram0, qgram1 );
			if (res != 0 ) return res;
			else res = Integer.compare( pos0, pos1 );
			break;

		default:
			throw new RuntimeException("UNIMPLEMENTED CASE");
		}
		assert res != Integer.MAX_VALUE;
		return res;
	}
	
	public int compareTokens(int token0, int token1) {
		switch (mode) {
		case TF: return Integer.compare( token0, token1 );
		case FF: return Integer.compare( orderMap.get( token0 ), orderMap.get( token1 ) );
		default: throw new RuntimeException("Unexpected error");
		}
	}
	
	public Ordering getMode() {
		return mode;
	}
}
