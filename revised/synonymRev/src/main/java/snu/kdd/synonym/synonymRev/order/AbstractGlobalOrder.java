package snu.kdd.synonym.synonymRev.order;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.tools.PosQGram;
import snu.kdd.synonym.synonymRev.tools.QGram;

abstract public class AbstractGlobalOrder {

	public static enum Ordering { PF, TF, FF, }
	
	protected Map<?, Integer> orderMap = null;
	protected final int qgramSize;
	protected int nEntry;
	
	public AbstractGlobalOrder( int qgramSize) {
		this.qgramSize = qgramSize;
	}
	
	public void initOrder( Query query ) {
		if ( qgramSize == 1 ) orderMap = orderTokens( query );
		else orderMap = orderQGrams( query );
		nEntry = orderMap.size();
	}
	
	abstract public Ordering getMode();

	// PosQGram
	abstract public int getOrder( PosQGram o );
	abstract public int getOrder( int token, int pos );
	abstract public int compare( PosQGram o1, PosQGram o2 );
	abstract public int compare( int[] qgram1, int pos1, int[] qgram2, int pos2 );
	abstract public int compare( int token1, int pos1, int token2, int pos2 );
	
	// QGram
	abstract public int getOrder( QGram o );
	abstract public int compare( QGram o1, QGram o2 );
	abstract public int compare( int[] qgram1, int[] qgram2 );
	
	// Token
	abstract public int getOrder( int token );
	abstract public int compare( int token1, int token2 );

	protected Object2IntOpenHashMap<QGram> orderQGrams( Query query ) {
		Object2IntOpenHashMap<QGram> counter = new Object2IntOpenHashMap<QGram>();
		counter.defaultReturnValue( 0 );
		for ( Record recS : query.searchedSet.recordList ) {
			for ( int i=0; i<recS.size(); i++ ) {
				for ( Rule rule : recS.getSuffixApplicableRules( i ) ) {
					int[] rhs = rule.getRight();
					for ( int j=0; j<rhs.length; j++ ) {
						QGram qgram = new QGram( Arrays.copyOfRange( rhs, j, j+qgramSize ));
						counter.put( qgram, counter.getInt( qgram )+1 );
					}
				}
			}
		}
		
		if ( !query.selfJoin ) {
			for ( Record recT : query.indexedSet.recordList ) {
				for ( int i=0; i<recT.size(); i++ ) {
					for ( Rule rule : recT.getSuffixApplicableRules( i ) ) {
						int[] rhs = rule.getRight();
						for ( int j=0; j<rhs.length; j++ ) {
							QGram qgram = new QGram( Arrays.copyOfRange( rhs, j, j+qgramSize ));
							counter.put( qgram, counter.getInt( qgram )+1 );
						}
					}
				}
			}
		}
		if ( !query.selfJoin ) {
			for ( Record recT : query.indexedSet.recordList ) {
				for ( int i=0; i<recT.size(); i++ ) {
					for ( Rule rule : recT.getSuffixApplicableRules( i ) ) {
						List<K> keyList = parseRule( rule, i );
						for ( K key : keyList ) counter.put( key, counter.getInt( key )+1 );
					}
				}
			}
		}

		Object2IntOpenHashMap<QGram> orderMap = new Object2IntOpenHashMap<QGram>( counter.size() );
		Stream<Entry<QGram, Integer>> stream = counter.entrySet().stream().sorted( Map.Entry.comparingByValue() );
		Iterator<Entry<QGram, Integer>> iter = stream.iterator();
		for ( int i=0; i<counter.size(); i++ ) {
			Entry<QGram, Integer> entry = iter.next();
			orderMap.put( entry.getKey(), i );
		}
		return orderMap;
	}
	
	protected Int2IntOpenHashMap orderTokens( Query query ) {
		Int2IntOpenHashMap counter = new Int2IntOpenHashMap();
		counter.defaultReturnValue( 0 );
		for ( Record recS : query.searchedSet.recordList ) {
			for ( int i=0; i<recS.size(); i++ ) {
				for ( Rule rule : recS.getSuffixApplicableRules( i ) ) {
					int[] rhs = rule.getRight();
					for ( int j=0; j<rhs.length; j++ ) counter.put( rhs[j], counter.get( rhs[j] )+1 );
				}
			}
		}
		
		if ( !query.selfJoin ) {
			for ( Record recT : query.indexedSet.recordList ) {
				for ( int i=0; i<recT.size(); i++ ) {
					for ( Rule rule : recT.getSuffixApplicableRules( i ) ) {
						int[] rhs = rule.getRight();
						for ( int j=0; j<rhs.length; j++ ) counter.put( rhs[j], counter.get( rhs[j] )+1 );
					}
				}
			}
		}

		Int2IntOpenHashMap orderMap = new Int2IntOpenHashMap( counter.size() );
		Stream<Entry<Integer, Integer>> stream = counter.entrySet().stream().sorted( Map.Entry.comparingByValue() );
		Iterator<Entry<Integer, Integer>> iter = stream.iterator();
		for ( int i=0; i<counter.size(); i++ ) {
			Entry<Integer, Integer> entry = iter.next();
			orderMap.put( entry.getKey().intValue(), i );
		}
		return orderMap;
	}
}
