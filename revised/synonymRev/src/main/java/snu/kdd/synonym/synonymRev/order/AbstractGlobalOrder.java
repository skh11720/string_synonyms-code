package snu.kdd.synonym.synonymRev.order;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang.ArrayUtils;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import snu.kdd.synonym.synonymRev.data.Query;
import snu.kdd.synonym.synonymRev.data.Record;
import snu.kdd.synonym.synonymRev.data.Rule;
import snu.kdd.synonym.synonymRev.tools.PosQGram;
import snu.kdd.synonym.synonymRev.tools.QGram;

abstract public class AbstractGlobalOrder {

	public static enum Ordering { PF, TF, FF, }
	
	protected Object2IntOpenHashMap<?> counter = null;
	protected Object2IntOpenHashMap<?> orderMap = null;
	protected final int qgramSize;
	protected int nEntry;
	protected int max_pos = 0;
	
	public AbstractGlobalOrder( int qgramSize) {
		this.qgramSize = qgramSize;
		if ( qgramSize == 1 ) {
			counter = new Object2IntOpenHashMap<Integer>();
			counter.defaultReturnValue( 0 );
			orderMap = new Object2IntOpenHashMap<Integer>();
		}
		else {
			counter = new Object2IntOpenHashMap<QGram>();
			counter.defaultReturnValue( 0 );
			orderMap = new Object2IntOpenHashMap<QGram>();
		}
	}
	
	public void initializeForSequence( Query query, boolean indexByOrder ) {
		count( query.searchedSet.recordList, true );
		if ( !query.selfJoin ) count( query.indexedSet.recordList, false );
//		try {
//			BufferedWriter bw = new BufferedWriter( new FileWriter( "tmp/counter.txt" ) );
//			for ( Object key : counter.keySet() ) {
//				bw.write( key.toString() +": "+counter.getInt( key )+"\n");
//			}
//			bw.flush();
//		}
//		catch ( IOException e ) {}
		buildOrderMap();
		if ( indexByOrder ) {
			IntOpenHashSet converted = new IntOpenHashSet();
			indexByOrder( query.searchedSet.recordList, true, converted );
			if ( !query.selfJoin ) indexByOrder( query.indexedSet.recordList, false, converted );
		}
	}

	public void initializeForSet( Query query ) {
		count( query.searchedSet.recordList, true );
		if ( !query.selfJoin ) count( query.indexedSet.recordList, true );
		buildOrderMap();
		IntOpenHashSet converted = new IntOpenHashSet();
		indexByOrder( query.searchedSet.recordList, true, converted );
		if ( !query.selfJoin ) indexByOrder( query.indexedSet.recordList, true, converted );
	}

	protected void indexByOrder( List<Record> recordList, boolean expand, IntOpenHashSet converted ) {
		if ( qgramSize > 1 ) throw new RuntimeException("Unexpected error");
		for ( Record rec : recordList ) {
			max_pos = Math.max( max_pos, rec.getMaxTransLength() );
//			Boolean debug = false;
//			if ( rec.getID() == 11487 ) debug = true;
//			if (debug) System.out.println( "ID: "+rec.getID() );
//			if (debug) System.out.println( "record, before: "+Arrays.toString( rec.getTokensArray() ) );
			int[] tokens = rec.getTokensArray();
			for ( int i=0; i<tokens.length; i++ ) {
//				if (debug) System.out.println( "convert token: "+tokens[i]+", "+orderMap.get( tokens[i] ) );
				if ( expand ) {
					for ( Rule rule : rec.getSuffixApplicableRules( i ) ) {
//						if (debug) System.out.println( "rule, before: "+rule.toString() );
						if ( converted.contains( rule.id ) ) continue;
						int[] lhs = rule.getLeft();
						for ( int j=0; j<lhs.length; j++ ) lhs[j] = orderMap.getInt( lhs[j] );
						int[] rhs = rule.getRight();
						for ( int j=0; j<rhs.length; j++ ) rhs[j] = orderMap.getInt( rhs[j] );
//						if (debug) System.out.println( "rule, after: "+rule.toString() );
						
	//					if ( convertedRuleSet.contains( rule ) ) {
	//						System.out.println( Arrays.toString( recS.getTokensArray() ) );
	//						System.out.println( Arrays.toString( rule.getLeft()) );
	//						System.out.println( Arrays.toString( rule.getRight()) );
	//						System.exit( 1 );
	//					}
						converted.add( rule.id );
					}
				}
				tokens[i] = orderMap.getInt( tokens[i] );
			} // end for i
//			if(debug) System.out.println( "record, after: "+Arrays.toString( rec.getTokensArray() ) );
//			if ( debug && rec.getID() > 10 ) System.exit( 1 );
		} // end for record
	}
	
	protected void buildOrderMap() {
		Stream<?> stream = counter.entrySet().stream().sorted( Map.Entry.comparingByValue() );
		Iterator<Entry<?, Integer>> iter = (Iterator<Entry<?, Integer>>) stream.iterator();
//		System.out.println( "Counter size: "+counter.size() );
		for ( int i=0; i<counter.size(); i++ ) {
			Entry<?, Integer> entry = iter.next();
			if( qgramSize == 1 ) ((Object2IntOpenHashMap<Integer>)orderMap).put( (Integer)entry.getKey(), i );
			else ((Object2IntOpenHashMap<QGram>)orderMap).put( (QGram)entry.getKey(), i );
		}
	}
	
	protected void count( List<Record> recordList, boolean expand ) {
		if ( qgramSize == 1 ) countTokens( recordList, expand );
		else countQGrams( recordList, expand );
	}

	protected void countQGrams( List<Record> recordList, boolean expand ) {
		Object2IntOpenHashMap<QGram> _counter = (Object2IntOpenHashMap<QGram>)counter;
		for ( Record rec : recordList ) {
			List<List<QGram>> selfQGrams = rec.getSelfQGrams( qgramSize, rec.size() );
			for ( int i=0; i<rec.size(); i++ ) {
				if ( expand ) {
					for ( Rule rule : rec.getSuffixApplicableRules( i ) ) {
						int[] rhs = rule.getRight();
						for ( int j=0; j<rhs.length+1-qgramSize; j++ ) {
							QGram qgram = new QGram( Arrays.copyOfRange( rhs, j, j+qgramSize ));
							_counter.put( qgram, _counter.getInt( qgram )+1 );
						}
					}
				}
				else {
					for ( QGram qgram : selfQGrams.get( i ) ) _counter.put( qgram, _counter.getInt( qgram )+1 );
				}
			}
		}
	}

	protected void countTokens( List<Record> recordList, boolean expand ) {
		Object2IntOpenHashMap<Integer> _counter = (Object2IntOpenHashMap<Integer>)counter;
		for ( Record rec : recordList ) {
			int[] tokens = rec.getTokensArray();
			for ( int i=0; i<rec.size(); i++ ) {
				if ( expand ) {
					for ( Rule rule : rec.getSuffixApplicableRules( i ) ) {
						int[] rhs = rule.getRight();
						for ( int j=0; j<rhs.length; j++ ) {
							_counter.put( Integer.valueOf( rhs[j] ), _counter.getInt( rhs[j] )+1 );
						}
					}
				}
				else _counter.put( Integer.valueOf( tokens[i] ), _counter.getInt( tokens[i] )+1 );
			}
		}
	}
	
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

		Object2IntOpenHashMap<QGram> orderMap = new Object2IntOpenHashMap<QGram>( counter.size() );
		orderMap.defaultReturnValue( Integer.MAX_VALUE );
		Stream<Entry<QGram, Integer>> stream = counter.entrySet().stream().sorted( Map.Entry.comparingByValue() );
		Iterator<Entry<QGram, Integer>> iter = stream.iterator();
		for ( int i=0; i<counter.size(); i++ ) {
			Entry<QGram, Integer> entry = iter.next();
			orderMap.put( entry.getKey(), i );
		}
		return orderMap;
	}
	
	public void writeToFile() {
		BufferedWriter bw;
		// counter
		try {
			bw = new BufferedWriter( new FileWriter( "./tmp/counter.txt" ) );
			Stream<?> stream = counter.entrySet().stream().sorted( Map.Entry.comparingByValue() );
			Iterator<Entry<?, Integer>> iter = (Iterator<Entry<?, Integer>>) stream.iterator();
			for ( int i=0; i<counter.size(); i++ ) {
				Entry<?, Integer> entry = iter.next();
				bw.write( entry.getKey()+": "+entry.getValue()+"\n" );
			}
			bw.flush();
			bw.close();
		}
		catch ( IOException e ) {}
		
		// orderMap
		try {
			bw = new BufferedWriter( new FileWriter( "./tmp/orderMap.txt" ) );
			for ( Entry<?, Integer> entry : orderMap.entrySet() ) {
				bw.write( entry.getKey()+": "+entry.getValue()+"\n" );
			}
			bw.flush();
			bw.close();
		}
		catch ( IOException e ) {}
//		orderMap.entrySet()
//		Stream<?> stream = counter.entrySet().stream().sorted( Map.Entry.comparingByValue() );
//		Iterator<Entry<?, Integer>> iter = (Iterator<Entry<?, Integer>>) stream.iterator();
////		System.out.println( "Counter size: "+counter.size() );
//		for ( int i=0; i<counter.size(); i++ ) {
//			Entry<?, Integer> entry = iter.next();
//			if( qgramSize == 1 ) ((Object2IntOpenHashMap<Integer>)orderMap).put( (Integer)entry.getKey(), i );
//			else ((Object2IntOpenHashMap<QGram>)orderMap).put( (QGram)entry.getKey(), i );
//		}
	}
	
//	protected Object2IntOpenHashMap orderTokens( Query query ) {
//		Int2IntOpenHashMap counter = new Int2IntOpenHashMap();
//		counter.defaultReturnValue( 0 );
//		for ( Record recS : query.searchedSet.recordList ) {
//			for ( int i=0; i<recS.size(); i++ ) {
//				for ( Rule rule : recS.getSuffixApplicableRules( i ) ) {
//					int[] rhs = rule.getRight();
//					for ( int j=0; j<rhs.length; j++ ) counter.put( rhs[j], counter.get( rhs[j] )+1 );
//				}
//			}
//		}
//		
//		for ( Record recT : query.indexedSet.recordList ) {
//			for ( int i=0; i<recT.size(); i++ ) {
//				int[] tokens = recT.getTokensArray();
//				if ( !query.selfJoin ) {
//					for ( Rule rule : recT.getSuffixApplicableRules( i ) ) {
//						int[] rhs = rule.getRight();
//						for ( int j=0; j<rhs.length; j++ ) counter.put( rhs[j], counter.get( rhs[j] )+1 );
//					}
//				}
//				// Note that self rules are in the set of applicable rules.
//				// 
//				else counter.put( tokens[i], counter.get( tokens[i] )+1 );
//			}
//		}
//
//		Int2IntOpenHashMap orderMap = new Int2IntOpenHashMap( counter.size() );
//		orderMap.defaultReturnValue( Integer.MAX_VALUE );
//		Stream<Entry<Integer, Integer>> stream = counter.entrySet().stream().sorted( Map.Entry.comparingByValue() );
//		Iterator<Entry<Integer, Integer>> iter = stream.iterator();
//		for ( int i=0; i<counter.size(); i++ ) {
//			Entry<Integer, Integer> entry = iter.next();
//			orderMap.put( entry.getKey().intValue(), i );
//		}
//		return orderMap;
//	}
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

}
