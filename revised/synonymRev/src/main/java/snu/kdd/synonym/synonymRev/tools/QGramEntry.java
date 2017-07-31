package snu.kdd.synonym.synonymRev.tools;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import snu.kdd.synonym.synonymRev.data.Rule;

public class QGramEntry {
	private Rule[] ruleList;
	public int length;
	public int rightMostIndex;
	public int bothSize;
	public int builtPosition;
	public boolean eof = false;

	public QGramEntry( int q, Rule r, int idx ) {
		ruleList = new Rule[ 1 ];
		ruleList[ 0 ] = r;
		length = r.rightSize();
		rightMostIndex = idx + r.leftSize();

		bothSize = length * 2;
		builtPosition = 0;
	}

	public QGramEntry( QGramEntry entry, Rule r ) {
		int ruleCount = entry.ruleList.length + 1;
		ruleList = new Rule[ ruleCount ];
		for( int i = 0; i < ruleCount - 1; i++ ) {
			ruleList[ i ] = entry.ruleList[ i ];
		}
		ruleList[ ruleCount - 1 ] = r;
		length = entry.length + r.rightSize();
		rightMostIndex = entry.rightMostIndex + r.leftSize();

		bothSize = ruleList[ 0 ].rightSize() + r.rightSize();
		builtPosition = entry.builtPosition;
	}

	@Override
	public String toString() {
		StringBuilder bld = new StringBuilder();

		bld.append( "[" );
		for( int i = 0; i < ruleList.length; i++ ) {
			int[] to = ruleList[ i ].getRight();
			for( int j = 0; j < to.length; j++ ) {
				bld.append( to[ j ] );
				bld.append( " " );
			}
			bld.append( "/" );
		}
		bld.append( "], r: " );
		bld.append( rightMostIndex );

		return bld.toString();
	}

	public int getBothRHSLength() {
		return bothSize;
	}

	public void generateQGram( int q, List<List<QGram>> qgrams, int min, int max ) {
		if( !eof && length < q ) {
			return;
		}

		Rule firstRule = ruleList[ 0 ];

		int[] to = firstRule.getRight();
		int firstRuleToSize = to.length;

		int lastSize;

		if( eof ) {
			lastSize = firstRuleToSize;
		}
		else {
			lastSize = Integer.min( length - q + 1, firstRuleToSize );
		}

		int i = builtPosition;
		for( ; i < lastSize; i++ ) {

			int[] qgram = new int[ q ];
			int idx = 0;
			boolean stop = false;

			// set first rule part
			for( int p = i; p < firstRuleToSize; p++ ) {
				qgram[ idx++ ] = to[ p ];
				if( idx == q ) {
					addQGram( new QGram( qgram ), qgrams, min, max, i );
					stop = true;
					break;
				}
			}

			for( int r = 1; !stop && r < ruleList.length; r++ ) {
				Rule otherRule = ruleList[ r ];

				int[] otherRuleTo = otherRule.getRight();
				int otherRuleToSize = otherRuleTo.length;

				for( int p = 0; p < otherRuleToSize; p++ ) {
					qgram[ idx++ ] = otherRuleTo[ p ];
					if( idx == q ) {
						addQGram( new QGram( qgram ), qgrams, min, max, i );
						stop = true;
						break;
					}
				}
			}

			if( !stop ) {
				for( ; idx < q; idx++ ) {
					qgram[ idx ] = Integer.MAX_VALUE;
				}
				addQGram( new QGram( qgram ), qgrams, min, max, i );
			}
		}
		builtPosition = i;
	}

	public void generateQGramWithRange( int q, Object2ObjectOpenHashMap<QGram, List<QGramRange>> qgrams, int min, int max ) {
		if( !eof && length < q ) {
			return;
		}

		Rule firstRule = ruleList[ 0 ];

		int[] to = firstRule.getRight();
		int firstRuleToSize = to.length;

		int lastSize;

		if( eof ) {
			lastSize = firstRuleToSize;
		}
		else {
			lastSize = Integer.min( length - q + 1, firstRuleToSize );
		}

		int i = builtPosition;
		for( ; i < lastSize; i++ ) {

			int[] qgram = new int[ q ];
			int idx = 0;
			boolean stop = false;

			// set first rule part
			for( int p = i; p < firstRuleToSize; p++ ) {
				qgram[ idx++ ] = to[ p ];
				if( idx == q ) {
					addQGramWithRange( new QGram( qgram ), qgrams, min, max, i );
					stop = true;
					break;
				}
			}

			for( int r = 1; !stop && r < ruleList.length; r++ ) {
				Rule otherRule = ruleList[ r ];

				int[] otherRuleTo = otherRule.getRight();
				int otherRuleToSize = otherRuleTo.length;

				for( int p = 0; p < otherRuleToSize; p++ ) {
					qgram[ idx++ ] = otherRuleTo[ p ];
					if( idx == q ) {
						addQGramWithRange( new QGram( qgram ), qgrams, min, max, i );
						stop = true;
						break;
					}
				}
			}

			if( !stop ) {
				for( ; idx < q; idx++ ) {
					qgram[ idx ] = Integer.MAX_VALUE;
				}
				addQGramWithRange( new QGram( qgram ), qgrams, min, max, i );
			}
		}
		builtPosition = i;
	}

	// range : inclusive
	public void generateQGram( int q, List<List<QGram>> qgrams, int min, int max, int range ) {
		if( !eof && length < q ) {
			return;
		}

		Rule firstRule = ruleList[ 0 ];

		int[] to = firstRule.getRight();
		int firstRuleToSize = to.length;

		int lastSize;

		if( eof ) {
			lastSize = firstRuleToSize;
		}
		else {
			lastSize = Integer.min( length - q + 1, firstRuleToSize );
		}

		int i = builtPosition;
		for( ; i < lastSize; i++ ) {
			if( i + min >= range ) {
				break;
			}

			int[] qgram = new int[ q ];
			int idx = 0;
			boolean stop = false;

			// set first rule part
			for( int p = i; p < firstRuleToSize; p++ ) {
				qgram[ idx++ ] = to[ p ];
				if( idx == q ) {
					addQGram( new QGram( qgram ), qgrams, min, max, i, range );
					stop = true;
					break;
				}
			}

			if( !stop ) {
				for( int r = 1; r < ruleList.length; r++ ) {
					Rule otherRule = ruleList[ r ];

					int[] otherRuleTo = otherRule.getRight();
					int otherRuleToSize = otherRuleTo.length;

					for( int p = 0; p < otherRuleToSize; p++ ) {
						qgram[ idx++ ] = otherRuleTo[ p ];
						if( idx == q ) {
							addQGram( new QGram( qgram ), qgrams, min, max, i, range );
							stop = true;
							break;
						}
					}
				}
			}

			if( !stop ) {
				for( ; idx < q; idx++ ) {
					qgram[ idx ] = Integer.MAX_VALUE;
				}
				addQGram( new QGram( qgram ), qgrams, min, max, i, range );
			}
		}
		builtPosition = i;
	}

	public void generateQGramCount( int q, List<Integer> qgramCount, int min, int max, int range ) {
		if( !eof && length < q ) {
			return;
		}

		Rule firstRule = ruleList[ 0 ];

		int[] to = firstRule.getRight();
		int firstRuleToSize = to.length;

		int lastSize;

		if( eof ) {
			lastSize = firstRuleToSize;
		}
		else {
			lastSize = Integer.min( length - q + 1, firstRuleToSize );
		}

		int i = builtPosition;
		for( ; i < lastSize; i++ ) {
			if( i + min >= range ) {
				break;
			}

			int idx = 0;
			boolean stop = false;

			// set first rule part
			for( int p = i; p < firstRuleToSize; p++ ) {
				if( idx == q ) {
					addQGramCount( qgramCount, min, max, i, range );
					stop = true;
					break;
				}
			}

			if( !stop ) {
				for( int r = 1; r < ruleList.length; r++ ) {
					Rule otherRule = ruleList[ r ];

					int[] otherRuleTo = otherRule.getRight();
					int otherRuleToSize = otherRuleTo.length;

					for( int p = 0; p < otherRuleToSize; p++ ) {
						if( idx == q ) {
							addQGramCount( qgramCount, min, max, i, range );
							stop = true;
							break;
						}
					}
				}
			}

			if( !stop ) {
				addQGramCount( qgramCount, min, max, i, range );
			}
		}
		builtPosition = i;
	}

	public void addQGramCount( List<Integer> qgramCount, int min, int max, int i, int range ) {
		int iterMinIndex = min + i;
		int iterMaxIndex = max + i;

		for( int p = iterMinIndex; p <= iterMaxIndex; p++ ) {
			if( p >= range ) {
				break;
			}

			qgramCount.add( p, qgramCount.get( p ) + 1 );
		}
	}

	public void addQGram( QGram qgram, List<List<QGram>> qgrams, int min, int max, int i, int range ) {
		int iterMinIndex = min + i;
		int iterMaxIndex = max + i;

		for( int p = iterMinIndex; p <= iterMaxIndex; p++ ) {
			if( p >= range ) {
				break;
			}

			qgrams.get( p ).add( qgram );
		}
	}

	public void addQGram( QGram qgram, List<List<QGram>> qgrams, int min, int max, int i ) {
		int iterMinIndex = min + i;
		int iterMaxIndex = max + i;

		for( int p = iterMinIndex; p <= iterMaxIndex; p++ ) {
			qgrams.get( p ).add( qgram );
		}
	}

	public void addQGramWithRange( QGram qgram, Object2ObjectOpenHashMap<QGram, List<QGramRange>> qgramsMap, int min, int max,
			int i ) {
		int iterMinIndex = min + i;
		int iterMaxIndex = max + i;

		List<QGramRange> list = qgramsMap.get( qgram );

		int mergeMin = iterMinIndex;
		int mergeMax = iterMaxIndex;

		System.out.println( "Adding " + qgram + " at [" + min + ", " + max + "]" );

		if( list != null ) {
			Iterator<QGramRange> iter = list.iterator();
			while( iter.hasNext() ) {
				QGramRange otherRange = iter.next();
				if( min > otherRange.max || max < otherRange.min ) {
					continue;
				}

				if( otherRange.max > mergeMax ) {
					mergeMax = otherRange.max;
				}
				if( otherRange.min < mergeMin ) {
					mergeMin = otherRange.min;
				}
				iter.remove();
			}
		}
		else {
			list = new ArrayList<QGramRange>();
			qgramsMap.put( qgram, list );
		}

		QGramRange qgramRange = new QGramRange( qgram, mergeMin, mergeMax );
		list.add( qgramRange );
	}
}