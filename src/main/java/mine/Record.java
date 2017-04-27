package mine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Stack;

import sigmod13.RecordInterface;
import sigmod13.filter.ITF_Filter;
import snu.kdd.synonym.algorithm.JoinHybridOpt_Q.CountEntry;
import tools.DEBUG;
import tools.IntegerPair;
import tools.IntegerSet;
import tools.LongIntPair;
import tools.QGram;
import tools.Rule;
import tools.RuleTrie;
import tools.Rule_ACAutomata;
import tools.Rule_InverseTrie;
import tools.StaticFunctions;
import tools.WYK_HashSet;
import validator.DefaultValidator;
import validator.Validator;

public class Record implements Comparable<Record>, RecordInterface, RecordInterface.Expanded {
	public static List<String> strlist;
	protected static RuleTrie atm;
	protected int id;
	/**
	 * For fast hashing
	 */
	protected boolean validHashValue = false;
	protected int hashValue;
	private static final int bigprime = 1645333507;

	public static int expandAllCount = 0;
	public static int expandAllIterCount = 0;
	public static int getQGramCount = 0;
	public static int get2GramCount = 0;

	/**
	 * Actual tokens
	 */
	protected int[] tokens;
	/**
	 * For DynamicMatch.
	 * applicableRules[i] contains all the rules which can be applied to the
	 * prefix of str[i].
	 */
	protected Rule[][] applicableRules = null;
	/**
	 * For DynamicMatch.
	 * applicableRules[i] contains all the rules which can be applied to the
	 * suffix of str[i].
	 */
	protected Rule[][] suffixApplicableRules = null;
	protected Rule_InverseTrie applicableRulesTrie = null;
	/**
	 * For {@link algorithm.dynamic.DynamicMatch06_C}
	 */
	protected IntegerSet[] availableTokens = null;
	/**
	 * For Length filter
	 * transformedLengths[ i - 1 ][ 0 ] = l_{min}(s[1,i],R)
	 * transformedLengths[ i - 1 ][ 1 ] = l_{Max}(s[1,i],R)
	 */
	protected int[][] transformedLengths = null;

	protected static final Rule[] EMPTY_RULE = new Rule[ 0 ];
	/**
	 * Estimate the number of equivalent records
	 */
	protected long[] estimated_equivs = null;
	/**
	 * For early pruning of one-side equivalence check.<br/>
	 * Suppose that we are computing M[i,j].<br/>
	 * If searchrange[i] = l, we may search M[i-l..i,*] only.
	 */
	protected short[] searchrange = null;
	protected short maxsearchrange = 1;
	/**
	 * For early pruning of one-side equivalence check.<br/>
	 * Suppose that we are computing M[i,j].<br/>
	 * If invsearchrange[i] = l, we may search M[*,j-l..j] only.
	 */
	protected short[] invsearchrange = null;
	protected short maxinvsearchrange = 1;

	protected static final Validator checker = new DefaultValidator();

	// public HashMap<Integer, IntegerPair> lastTokenMap;

	private Record() {
		id = -1;
	}

	public static void setStrList( List<String> int2str ) {
		Record.strlist = int2str;
	}

	public static void setRuleTrie( RuleTrie atm ) {
		Record.atm = atm;
	}

	public static RuleTrie getRuleTrie() {
		return atm;
	}

	public Record( int[] tokens ) {
		this.id = -1;
		this.tokens = tokens;
	}

	public Record( int id, String str, Map<String, Integer> str2int ) {
		this.id = id;
		String[] pstr = str.split( "( |\t)+" );
		tokens = new int[ pstr.length ];
		for( int i = 0; i < pstr.length; ++i ) {
			int token = str2int.get( pstr[ i ] );
			tokens[ i ] = token;
		}
	}

	public Record( Record o ) {
		id = -1;
		tokens = new int[ o.tokens.length ];
		for( int i = 0; i < tokens.length; ++i ) {
			tokens[ i ] = o.tokens[ i ];
		}
	}

	public void preprocessRules( Rule_ACAutomata automata, boolean buildtrie ) {
		applicableRules = automata.applicableRules( tokens, 0 );
		if( buildtrie ) {
			applicableRulesTrie = new Rule_InverseTrie( applicableRules );
		}
	}

	/**
	 * preprocessLengths(), addSelfTokenRules() and preprocessRules() should be
	 * called before this method is called
	 */
	// Interval tree를 이용해서 available token set을 저장할 수도 있음
	public void preprocessAvailableTokens( int maxlength ) {
		assert ( maxlength > 0 );
		maxlength = Math.min( maxlength, transformedLengths[ tokens.length - 1 ][ 1 ] );
		availableTokens = new IntegerSet[ maxlength ];
		for( int i = 0; i < maxlength; ++i )
			availableTokens[ i ] = new IntegerSet();
		for( int i = 0; i < tokens.length; ++i ) {
			Rule[] rules = applicableRules[ i ];
			int[] range;
			if( i == 0 )
				range = new int[] { 0, 0 };
			else
				range = transformedLengths[ i - 1 ];
			int from = range[ 0 ];
			int to = range[ 1 ];
			for( Rule rule : rules ) {
				int[] tokens = rule.getTo();
				for( int j = 0; j < tokens.length; ++j ) {
					for( int k = from; k <= to; ++k ) {
						if( k + j >= maxlength )
							break;
						availableTokens[ k + j ].add( tokens[ j ] );
					}
				}
			}
		}
	}

	/**
	 * preprocessLengths(), addSelfTokenRules() and preprocessRules() should be
	 * called before this method is called
	 */
	// Interval tree를 이용해서 available token set을 저장할 수도 있음
	public IntegerSet[] computeAvailableTokens() {
		IntegerSet[] availableTokens = new IntegerSet[ transformedLengths[ tokens.length - 1 ][ 1 ] ];
		for( int i = 0; i < availableTokens.length; ++i )
			availableTokens[ i ] = new IntegerSet();
		for( int i = 0; i < tokens.length; ++i ) {
			Rule[] rules = applicableRules[ i ];
			int[] range;
			if( i == 0 )
				range = new int[] { 0, 0 };
			else
				range = transformedLengths[ i - 1 ];
			int from = range[ 0 ];
			int to = range[ 1 ];
			for( Rule rule : rules ) {
				int[] tokens = rule.getTo();
				for( int j = 0; j < tokens.length; ++j ) {
					for( int k = from; k <= to; ++k ) {
						availableTokens[ k + j ].add( tokens[ j ] );
					}
				}
			}
		}
		return availableTokens;
	}

	/**
	 * preprocessRules() should be called before this method is called
	 */
	public void preprocessLengths() {
		transformedLengths = new int[ tokens.length ][ 2 ];
		for( int i = 0; i < tokens.length; ++i )
			transformedLengths[ i ][ 0 ] = transformedLengths[ i ][ 1 ] = i + 1;

		for( Rule rule : applicableRules[ 0 ] ) {
			int fromSize = rule.fromSize();
			int toSize = rule.toSize();
			if( fromSize > toSize ) {
				transformedLengths[ fromSize - 1 ][ 0 ] = Math.min( transformedLengths[ fromSize - 1 ][ 0 ], toSize );
			}
			else if( fromSize < toSize ) {
				transformedLengths[ fromSize - 1 ][ 1 ] = Math.max( transformedLengths[ fromSize - 1 ][ 1 ], toSize );
			}
		}
		for( int i = 1; i < tokens.length; ++i ) {
			transformedLengths[ i ][ 0 ] = Math.min( transformedLengths[ i ][ 0 ], transformedLengths[ i - 1 ][ 0 ] + 1 );
			transformedLengths[ i ][ 1 ] = Math.max( transformedLengths[ i ][ 1 ], transformedLengths[ i - 1 ][ 1 ] + 1 );
			for( Rule rule : applicableRules[ i ] ) {
				int fromSize = rule.fromSize();
				int toSize = rule.toSize();
				if( fromSize > toSize ) {
					transformedLengths[ i + fromSize - 1 ][ 0 ] = Math.min( transformedLengths[ i + fromSize - 1 ][ 0 ],
							transformedLengths[ i - 1 ][ 0 ] + toSize );
				}
				else if( fromSize < toSize ) {
					transformedLengths[ i + fromSize - 1 ][ 1 ] = Math.max( transformedLengths[ i + fromSize - 1 ][ 1 ],
							transformedLengths[ i - 1 ][ 1 ] + toSize );
				}

			}
		}
	}

	// public void preprocessLastToken() {
	// lastTokenMap = new HashMap<Integer, IntegerPair>();
	// for( int i = 0; i < tokens.length; i++ ) {
	// for( Rule rule : applicableRules[ i ] ) {
	// int fromSize = rule.fromSize();
	// int toSize = rule.toSize();
	// if( i + fromSize == tokens.length ) {
	// // rule generates the last token
	// int min;
	// int max;
	// if( i != 0 ) {
	// min = transformedLengths[ i - 1 ][ 0 ] + toSize;
	// max = transformedLengths[ i - 1 ][ 1 ] + toSize;
	// }
	// else {
	// min = toSize;
	// max = toSize;
	// }
	//
	// int lastToken = rule.getTo()[ toSize - 1 ];
	// IntegerPair pair = lastTokenMap.get( lastToken );
	// if( pair == null ) {
	// lastTokenMap.put( lastToken, new IntegerPair( min, max ) );
	// }
	// else {
	// pair.i1 = Integer.min( pair.i1, min );
	// pair.i2 = Integer.max( pair.i2, max );
	// }
	// }
	// }
	// }
	// }

	@SuppressWarnings( "unchecked" )
	public void preprocessEstimatedRecords() {
		ArrayList<Rule>[] tmpAppRules = new ArrayList[ tokens.length ];
		for( int i = 0; i < tokens.length; ++i )
			tmpAppRules[ i ] = new ArrayList<Rule>();

		for( int i = 0; i < tokens.length; ++i ) {
			for( Rule rule : applicableRules[ i ] ) {
				int eidx = i + rule.fromSize() - 1;
				tmpAppRules[ eidx ].add( rule );
			}
		}

		long[] est = new long[ tokens.length ];
		estimated_equivs = est;
		for( int i = 0; i < est.length; ++i ) {
			est[ i ] = Long.MAX_VALUE;
		}

		for( int i = 0; i < tokens.length; ++i ) {
			long size = 0;
			for( Rule rule : tmpAppRules[ i ] ) {
				int sidx = i - rule.fromSize() + 1;
				if( sidx == 0 ) {
					size += 1;
				}
				else {
					size += est[ sidx - 1 ];
				}

				if( size < 0 ) {
					if( DEBUG.ON ) {
						System.out.println( "Too many generalizations: " + id + " size " + size );
					}

					return;
				}
			}
			est[ i ] = size;
		}

		// if( est[ est.length - 1 ] > 10000 ) {
		// System.out.println( "[Warning] Many generalizations: " + id + " size " + est[ est.length - 1 ] );
		// System.out.println( this.toString() );
		// for( int i = 0; i < tokens.length; ++i ) {
		// for( Rule rule : applicableRules[ i ] ) {
		// System.out.println( rule.toTextString( strlist ) );
		// }
		// }
		// }
	}

	public void preprocessSearchRanges() {
		searchrange = new short[ tokens.length ];
		invsearchrange = new short[ tokens.length ];
		// Assumption : no lhs/rhs of a rule is empty
		for( int i = 0; i < tokens.length; ++i )
			searchrange[ i ] = invsearchrange[ i ] = 1;
		for( int i = 0; i < tokens.length; ++i ) {
			for( Rule r : applicableRules[ i ] ) {
				int[] from = r.getFrom();
				int[] to = r.getTo();
				// suffix index : i + |from| - 1
				int suffixidx = i + from.length - 1;
				searchrange[ suffixidx ] = (short) Math.max( searchrange[ suffixidx ], from.length );
				maxsearchrange = (short) Math.max( maxsearchrange, searchrange[ suffixidx ] );
				invsearchrange[ suffixidx ] = (short) Math.max( invsearchrange[ suffixidx ], to.length );
				maxinvsearchrange = (short) Math.max( maxinvsearchrange, invsearchrange[ suffixidx ] );
			}
		}
	}

	public void preprocessSuffixApplicableRules() {
		List<List<Rule>> tmplist = new ArrayList<List<Rule>>();
		for( int i = 0; i < tokens.length; ++i )
			tmplist.add( new ArrayList<Rule>() );
		for( int i = tokens.length - 1; i >= 0; --i ) {
			for( Rule rule : applicableRules[ i ] ) {
				int suffixidx = i + rule.getFrom().length - 1;
				tmplist.get( suffixidx ).add( rule );
			}
		}
		suffixApplicableRules = new Rule[ tokens.length ][];
		for( int i = 0; i < tokens.length; ++i )
			suffixApplicableRules[ i ] = tmplist.get( i ).toArray( new Rule[ 0 ] );
	}

	public IntegerSet[] getAvailableTokens() {
		if( availableTokens == null )
			return computeAvailableTokens();
		return availableTokens;
	}

	/**
	 * Returns 2grams which can be obtained by using at least 2 different rules
	 */
	public Set<Long> getFirstCrossing2Grams() {
		Set<Long> result = new HashSet<Long>();
		Rule[] firstRules = applicableRules[ 0 ];
		// For each rule, find the last token.
		for( Rule rule : firstRules ) {
			int[] from = rule.getFrom();
			int[] to = rule.getTo();
			// If there is no more string to apply a rule, it simply return the last
			// token.
			if( from.length == tokens.length ) {
				result.add( (long) to[ to.length - 1 ] );
				continue;
			}
			// Find another rule that can applied right next to the current rule
			Rule[] nextRules = applicableRules[ from.length ];
			long tmpgram = ( (long) to[ to.length - 1 ] ) * ( (long) Integer.MAX_VALUE );
			for( Rule nextrule : nextRules ) {
				int[] nextto = nextrule.getTo();
				long gram = tmpgram + nextto[ 0 ];
				result.add( gram );
			}
		}
		return result;
	}

	public IntegerPair getOriginal2Gram( int idx ) {
		int token = tokens[ idx ];
		if( idx != size() - 1 )
			return new IntegerPair( token, tokens[ idx + 1 ] );
		else
			return new IntegerPair( token, Integer.MAX_VALUE );
	}

	public static long exectime = 0;

	public List<List<QGram>> getQGrams( int q ) {
		getQGramCount++;
		List<List<QGram>> positionalQGram = new ArrayList<List<QGram>>();

		int maxLength = getMaxLength();
		for( int i = 0; i < maxLength; i++ ) {
			positionalQGram.add( new ArrayList<QGram>( 30 ) );
		}

		for( int t = 0; t < tokens.length; t++ ) {
			Rule[] rules = applicableRules[ t ];

			int minIndex;
			int maxIndex;

			if( t == 0 ) {
				minIndex = 0;
				maxIndex = 0;
			}
			else {
				minIndex = transformedLengths[ t - 1 ][ 0 ];
				maxIndex = transformedLengths[ t - 1 ][ 1 ];
			}

			// try {
			for( int r = 0; r < rules.length; r++ ) {
				Rule startRule = rules[ r ];

				Stack<QGramEntry> stack = new Stack<QGramEntry>();

				stack.add( new QGramEntry( q, startRule, t ) );

				while( !stack.isEmpty() ) {
					QGramEntry entry = stack.pop();

					if( entry.length >= q + entry.getBothRHSLength() - 2 ) {
						entry.generateQGram( q, positionalQGram, minIndex, maxIndex );
					}
					else {
						if( entry.rightMostIndex < tokens.length ) {
							// append

							entry.generateQGram( q, positionalQGram, minIndex, maxIndex );

							for( Rule nextRule : applicableRules[ entry.rightMostIndex ] ) {
								stack.add( new QGramEntry( entry, nextRule ) );
							}

						}
						else {
							// add EOF
							entry.eof = true;
							entry.generateQGram( q, positionalQGram, minIndex, maxIndex );
						}
					}
				}
			}
			// }
			// catch( Exception e ) {
			// e.printStackTrace();
			// System.out.println( "Record " + this + " id " + this.id + " " + getMaxLength() );
			// }
		}

		List<List<QGram>> resultQGram = new ArrayList<List<QGram>>();

		int maxSize = 0;
		for( int i = 0; i < maxLength; i++ ) {
			int size = positionalQGram.get( i ).size();
			if( maxSize < size ) {
				maxSize = size;
			}
		}

		// WYK_HashSet.DEBUG = true;
		WYK_HashSet<QGram> sQGram = new WYK_HashSet<QGram>( maxSize * 2 + 2 );

		for( int i = 0; i < positionalQGram.size(); i++ ) {
			sQGram.emptyAll();

			List<QGram> pQGram = positionalQGram.get( i );
			List<QGram> lQGram = new ArrayList<QGram>( pQGram.size() + 1 );

			for( QGram qgram : pQGram ) {
				boolean added = sQGram.add( qgram );
				if( added ) {
					lQGram.add( qgram );
				}
			}

			resultQGram.add( lQGram );
		}
		// WYK_HashSet.DEBUG = false;

		return resultQGram;
	}

	public List<List<QGram>> getQGrams( int q, int range ) {
		getQGramCount++;
		List<List<QGram>> positionalQGram = new ArrayList<List<QGram>>();

		int maxLength = Integer.min( range, getMaxLength() );
		for( int i = 0; i < maxLength; i++ ) {
			positionalQGram.add( new ArrayList<QGram>( 30 ) );
		}

		for( int t = 0; t < tokens.length; t++ ) {
			Rule[] rules = applicableRules[ t ];

			int minIndex;
			int maxIndex;

			if( t == 0 ) {
				minIndex = 0;
				maxIndex = 0;
			}
			else {
				minIndex = transformedLengths[ t - 1 ][ 0 ];
				maxIndex = transformedLengths[ t - 1 ][ 1 ];
			}

			if( minIndex >= range ) {
				continue;
			}

			// try {
			for( int r = 0; r < rules.length; r++ ) {
				Rule startRule = rules[ r ];

				Stack<QGramEntry> stack = new Stack<QGramEntry>();

				stack.add( new QGramEntry( q, startRule, t ) );

				while( !stack.isEmpty() ) {
					QGramEntry entry = stack.pop();

					if( entry.length >= q + entry.getBothRHSLength() - 2 ) {
						entry.generateQGram( q, positionalQGram, minIndex, maxIndex, range );
					}
					else {
						if( entry.rightMostIndex < tokens.length ) {
							// append

							entry.generateQGram( q, positionalQGram, minIndex, maxIndex, range );

							if( minIndex + entry.builtPosition < range ) {
								for( Rule nextRule : applicableRules[ entry.rightMostIndex ] ) {

									QGramEntry newEntry = new QGramEntry( entry, nextRule );

									if( newEntry.length >= q + newEntry.getBothRHSLength() - 2 ) {
										newEntry.generateQGram( q, positionalQGram, minIndex, maxIndex, range );
									}
									else {
										stack.add( newEntry );
									}
								}
							}

						}
						else {
							// add EOF
							entry.eof = true;
							entry.generateQGram( q, positionalQGram, minIndex, maxIndex, range );
						}
					}
				}
			}
			// }
			// catch( Exception e ) {
			// e.printStackTrace();
			// System.out.println( "Record " + this + " id " + this.id + " " + getMaxLength() );
			// }
		}

		List<List<QGram>> resultQGram = new ArrayList<List<QGram>>();

		int maxSize = 0;
		for( int i = 0; i < maxLength; i++ ) {
			int size = positionalQGram.get( i ).size();
			if( maxSize < size ) {
				maxSize = size;
			}
		}

		WYK_HashSet<QGram> sQGram = new WYK_HashSet<QGram>( maxSize * 2 + 2 );

		for( int i = 0; i < positionalQGram.size(); i++ ) {
			sQGram.emptyAll();
			List<QGram> pQGram = positionalQGram.get( i );

			List<QGram> lQGram = new ArrayList<QGram>( pQGram.size() + 1 );

			for( QGram qgram : pQGram ) {
				boolean added = sQGram.add( qgram );
				if( added ) {
					lQGram.add( qgram );
				}
			}

			resultQGram.add( lQGram );
		}

		return resultQGram;
	}

	public static class QGramEntry {
		private Rule[] ruleList;
		private int length;
		private int rightMostIndex;
		private int bothSize;
		private int builtPosition;
		private boolean eof = false;

		public QGramEntry( int q, Rule r, int idx ) {
			ruleList = new Rule[ 1 ];
			ruleList[ 0 ] = r;
			length = r.toSize();
			rightMostIndex = idx + r.fromSize();

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
			length = entry.length + r.toSize();
			rightMostIndex = entry.rightMostIndex + r.fromSize();

			bothSize = ruleList[ 0 ].toSize() + r.toSize();
			builtPosition = entry.builtPosition;
		}

		@Override
		public String toString() {
			StringBuilder bld = new StringBuilder();

			bld.append( "[" );
			for( int i = 0; i < ruleList.length; i++ ) {
				int[] to = ruleList[ i ].getTo();
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

			int[] to = firstRule.getTo();
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

					int[] otherRuleTo = otherRule.getTo();
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

		// range : inclusive
		public void generateQGram( int q, List<List<QGram>> qgrams, int min, int max, int range ) {
			if( !eof && length < q ) {
				return;
			}

			Rule firstRule = ruleList[ 0 ];

			int[] to = firstRule.getTo();
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

						int[] otherRuleTo = otherRule.getTo();
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
	}

	public LongIntPair getMinimumIndexSize( List<Map<QGram, CountEntry>> positionalQCountMap, long threshold, int q ) {

		boolean isLarge = this.getEstNumRecords() > threshold;
		List<List<QGram>> positionalQGrams = this.getQGrams( q );

		int minIndex = 0;
		long minCount = Long.MAX_VALUE;

		for( int i = 0; i < positionalQGrams.size(); i++ ) {
			List<QGram> qgrams = positionalQGrams.get( i );
			Map<QGram, CountEntry> currentCountMap = positionalQCountMap.get( i );

			long count = 0;
			for( QGram qgram : qgrams ) {
				CountEntry entry = currentCountMap.get( qgram );

				if( isLarge ) {
					count += entry.largeListSize + entry.smallListSize;
				}
				else {
					count += entry.largeListSize;
				}
			}

			if( minCount > count ) {
				minCount = count;
				minIndex = i;
			}
		}

		return new LongIntPair( minCount, minIndex );
	}

	/**
	 * Returns all available (actually, superset) 2 grams
	 */
	@Deprecated
	public List<Set<IntegerPair>> get2Grams() {
		get2GramCount++;
		long start = System.nanoTime();
		/* There are two type of 2 grams:
		 * 1) two tokens are derived from different rules.
		 * 2) two tokens are generated from the same rule. */

		// twograms.get( k ) returns all positional two-grams each of whose position is k
		List<Set<IntegerPair>> twograms = new ArrayList<Set<IntegerPair>>();
		int[] range = getCandidateLengths( size() - 1 );

		for( int i = 0; i < range[ 1 ]; ++i ) { // generates all positional two-grams with k = 1, ..., l_{Max}(s, R)
			twograms.add( new WYK_HashSet<IntegerPair>( 30 ) );
		}
		add2GramsFromDiffRules( twograms );
		add2GramsFromSameRule( twograms );
		exectime += System.nanoTime() - start;
		return twograms;
	}

	// maxStartPos exclusive
	@Deprecated
	public List<Set<IntegerPair>> get2GramsWithBound( int maxStartPos ) {
		get2GramCount++;
		long start = System.nanoTime();
		/* There are two type of 2 grams:
		 * 1) two tokens are derived from different rules.
		 * 2) two tokens are generated from the same rule. */

		// twograms.get( k ) returns all positional two-grams each of whose position is k
		List<Set<IntegerPair>> twograms = new ArrayList<Set<IntegerPair>>();
		int[] range = getCandidateLengths( size() - 1 );

		// to include maxLength
		int max = Integer.min( maxStartPos, range[ 1 ] );

		for( int i = 0; i < max; ++i ) { // generates all positional two-grams with k = 1, ..., l_{Max}(s, R)
			twograms.add( new WYK_HashSet<IntegerPair>( 30 ) );
		}
		add2GramsFromDiffRulesWithBound( twograms, maxStartPos );
		add2GramsFromSameRuleWithBound( twograms, maxStartPos );
		exectime += System.nanoTime() - start;
		return twograms;
	}

	@Deprecated
	public List<Set<IntegerPair>> getExact2Grams() {
		/* There are two type of 2 grams:
		 * 1) two tokens are derived from different rules.
		 * 2) two tokens are generated from the same rule. */
		List<Set<IntegerPair>> twograms = new ArrayList<Set<IntegerPair>>();
		long[] availrangebitmap = new long[ size() ];
		for( int i = 0; i < getMaxLength(); ++i )
			twograms.add( new WYK_HashSet<IntegerPair>() );
		// Compute exact available ranges
		for( int i = 0; i < size(); ++i ) {
			long range = 1;
			if( i != 0 )
				range = availrangebitmap[ i - 1 ];
			// For each rule r applicable to the prefix of x[i..],
			// range[i + r.lhs] contains range[i - 1] + r.rhs
			for( Rule rule : getApplicableRules( i ) )
				availrangebitmap[ i + rule.fromSize() - 1 ] |= range << rule.toSize();
		}

		// By using the exact available ranges, compute the exact 2 grams
		for( int i = 0; i < size(); ++i ) {
			long range = 1;
			if( i != 0 )
				range = availrangebitmap[ i - 1 ];
			List<Integer> availrangelist = transform( range );
			for( Rule headrule : getApplicableRules( i ) ) {
				int[] headstr = headrule.getTo();

				// Add 2grams from different rules
				Rule[] tailrules = getApplicableRules( i + headrule.fromSize() );
				if( tailrules == EMPTY_RULE ) {
					assert ( i + headrule.getFrom().length == size() );
					IntegerPair twogram = new IntegerPair( headstr[ headstr.length - 1 ], Integer.MAX_VALUE );
					for( int idx : availrangelist )
						if( idx + headstr.length < 64 )
							twograms.get( idx + headstr.length - 1 ).add( twogram );
				}
				else {
					assert ( tailrules.length > 0 );
					for( Rule tailrule : tailrules ) {
						// Generate twogram
						IntegerPair twogram = new IntegerPair( headstr[ headstr.length - 1 ], tailrule.getTo()[ 0 ] );
						for( int idx : availrangelist )
							if( idx + headstr.length < 64 )
								twograms.get( idx + headstr.length - 1 ).add( twogram );
					}
				}

				// Add 2grams from the same rule
				// If this rule generates one token only, skip generating 2 grams
				if( headstr.length < 2 )
					continue;
				for( int idx = 0; idx < headstr.length - 1 && idx < 64; ++idx ) {
					// Generate twogram
					IntegerPair twogram = new IntegerPair( headstr[ idx ], headstr[ idx + 1 ] );
					for( int jdx : availrangelist )
						if( jdx + idx < 64 )
							twograms.get( jdx + idx ).add( twogram );
				}
			}
		}
		return twograms;
	}

	private List<Integer> transform( long bitmap ) {
		bitmap &= 0x7fffffffffffffffL;
		List<Integer> list = new ArrayList<Integer>();
		if( bitmap > 0 ) {
			for( int i = 0; i < 64; ++i ) {
				if( bitmap % 2 == 1 )
					list.add( i );
				bitmap /= 2;
			}
		}
		return list;
	}

	/**
	 * Add every 2 grams derived from different rules.
	 * Also add 2 gram which contains EOL character.
	 *
	 * @param twograms
	 *            set which stores 2 grams
	 */
	private void add2GramsFromDiffRules( List<Set<IntegerPair>> twograms ) {
		// iterate on prefix rules: it is easier
		for( int i = 0; i < size(); ++i ) {
			// Every rules are applicable to a prefix of str[i..*]
			Rule[] headrules = getApplicableRules( i );
			int[] range = null;
			if( i == 0 ) {
				range = new int[] { 0, 0 };
			}
			else {
				range = getCandidateLengths( i - 1 );
			}

			for( Rule headrule : headrules ) {
				// Retrieve another prefix rules
				Rule[] tailrules = getApplicableRules( i + headrule.getFrom().length );
				int[] headstr = headrule.getTo();
				// The minimum index of the last token
				int min = range[ 0 ] + headstr.length - 1;
				int max = range[ 1 ] + headstr.length - 1;
				// If there is no applicable rule, (== end of string reached)
				// it adds 2 grams with EOL character.
				if( tailrules == EMPTY_RULE ) {
					assert ( i + headrule.getFrom().length == size() );
					IntegerPair twogram = new IntegerPair( headstr[ headstr.length - 1 ], Integer.MAX_VALUE );

					for( int idx = min; idx <= max; ++idx ) {
						twograms.get( idx ).add( twogram );
					}
				}
				else {
					assert ( tailrules.length > 0 );
					for( Rule tailrule : tailrules ) {
						// Generate twogram
						IntegerPair twogram = new IntegerPair( headstr[ headstr.length - 1 ], tailrule.getTo()[ 0 ] );
						for( int idx = min; idx <= max; ++idx ) {
							twograms.get( idx ).add( twogram );
						}
					}
				}
			}
		}
	}

	/**
	 * Add every 2 grams derived from different rules.
	 * Also add 2 gram which contains EOL character.
	 *
	 * @param twograms
	 *            set which stores 2 grams
	 * @param maxRange
	 *            maximum range of position of 2 grams
	 */
	private void add2GramsFromDiffRulesWithBound( List<Set<IntegerPair>> twograms, int maxStartPos ) {
		// iterate on prefix rules: it is easier
		for( int i = 0; i < size(); ++i ) {
			// Every rules are applicable to a prefix of str[i..*]
			Rule[] headrules = getApplicableRules( i );
			int[] range = null;
			if( i == 0 ) {
				range = new int[] { 0, 0 };
			}
			else {
				range = getCandidateLengths( i - 1 );
			}
			for( Rule headrule : headrules ) {
				// Retrieve another prefix rules
				Rule[] tailrules = getApplicableRules( i + headrule.getFrom().length );
				int[] headstr = headrule.getTo();
				// The minimum index of the last token
				int min = range[ 0 ] + headstr.length - 1;
				int max = range[ 1 ] + headstr.length - 1;

				max = Integer.min( max, maxStartPos - 1 );
				// If there is no applicable rule, (== end of string reached)
				// it adds 2 grams with EOL character.
				if( tailrules == EMPTY_RULE ) {
					assert ( i + headrule.getFrom().length == size() );
					IntegerPair twogram = new IntegerPair( headstr[ headstr.length - 1 ], Integer.MAX_VALUE );

					for( int idx = min; idx <= max; ++idx ) {
						twograms.get( idx ).add( twogram );
					}
				}
				else {
					assert ( tailrules.length > 0 );
					for( Rule tailrule : tailrules ) {
						// Generate twogram
						IntegerPair twogram = new IntegerPair( headstr[ headstr.length - 1 ], tailrule.getTo()[ 0 ] );
						for( int idx = min; idx <= max; ++idx ) {
							twograms.get( idx ).add( twogram );
						}
					}
				}
			}
		}
	}

	/**
	 * Add every 2 grams derived from the same rule
	 *
	 * @param twograms
	 *            set which stores 2 grams
	 */
	private void add2GramsFromSameRule( List<Set<IntegerPair>> twograms ) {
		// iterate on prefix rules: it is easier
		for( int i = 0; i < size(); ++i ) {
			// Every rules are applicable to a prefix of str[i..*]
			Rule[] rules = getApplicableRules( i );
			int[] range = null;
			if( i == 0 )
				range = new int[] { 0, 0 };
			else
				range = getCandidateLengths( i - 1 );
			for( Rule headrule : rules ) {
				int[] str = headrule.getTo();
				// If this rule generates one token only, skip generating 2 grams
				if( str.length < 2 )
					continue;
				for( int idx = 0; idx < str.length - 1; ++idx ) {
					// Generate twogram
					IntegerPair twogram = new IntegerPair( str[ idx ], str[ idx + 1 ] );
					for( int jdx = range[ 0 ] + idx; jdx <= range[ 1 ] + idx; ++jdx ) {
						twograms.get( jdx ).add( twogram );
					}
				}
			}
		}
	}

	/**
	 * Add every 2 grams derived from the same rule
	 *
	 * @param twograms
	 *            set which stores 2 grams
	 */
	private void add2GramsFromSameRuleWithBound( List<Set<IntegerPair>> twograms, int maxStartPos ) {
		// iterate on prefix rules: it is easier
		for( int i = 0; i < size(); ++i ) {
			// Every rules are applicable to a prefix of str[i..*]
			Rule[] rules = getApplicableRules( i );
			int[] range = null;
			if( i == 0 ) {
				range = new int[] { 0, 0 };
			}
			else {
				range = getCandidateLengths( i - 1 );
			}
			for( Rule headrule : rules ) {
				int[] str = headrule.getTo();
				// If this rule generates one token only, skip generating 2 grams
				if( str.length < 2 ) {
					continue;
				}
				for( int idx = 0; idx < str.length - 1; ++idx ) {
					// Generate twogram
					IntegerPair twogram = new IntegerPair( str[ idx ], str[ idx + 1 ] );
					for( int jdx = range[ 0 ] + idx; jdx <= range[ 1 ] + idx; ++jdx ) {
						if( jdx >= maxStartPos ) {
							break;
						}
						twograms.get( jdx ).add( twogram );
					}
				}
			}
		}
	}

	/**
	 * Returns all available (actually, superset) 2 grams
	 * which appear in str[idx..idx+1].
	 *
	 * @param idx:
	 *            starting index of 2 grams. (starting from 0)
	 */
	// public Set<IntegerPair> get2Grams( int idx ) {
	// /* There are two type of 2 grams:
	// * 1) two tokens are derived from different rules.
	// * 2) two tokens are generated from the same rule. */
	// Set<IntegerPair> twograms = new WYK_HashSet<IntegerPair>();
	// add2GramsFromDiffRules( idx, twograms );
	// add2GramsFromSameRule( idx, twograms );
	// return twograms;
	// }

	/**
	 * @TODO: Finish
	 *        Add every 2 grams which may appear in str[idx..idx+1] where
	 *        str[idx] and str[idx + 1] are derived from different rules
	 *
	 * @param idx
	 *            starting index of 2 grams.
	 * @param twograms
	 *            set which stores 2 grams
	 */
	// private void add2GramsFromDiffRules( int idx, Set<IntegerPair> twograms ) {
	// // iterate on prefix rules: it is easier
	// for( int i = 0; i < size(); ++i ) {
	// Rule[] rules = getApplicableRules( i );
	// int[] range = getCandidateLengths( i );
	// // range[0] is the minimum index of token generated by this rule.
	// // If it exceeds idx, every rule will not generate a 2-gram start)ing from
	// // a token within them
	// if( range[ 0 ] > idx )
	// break;
	// for( @SuppressWarnings( "unused" )
	// Rule rule : rules ) {
	// }
	// }
	// }

	/**
	 * Add every 2 grams which may appear in str[idx..idx+1] where
	 * str[idx] and str[idx + 1] are derived from the same rule
	 *
	 * @param idx
	 *            starting index of 2 grams.
	 * @param twograms
	 *            set which stores 2 grams
	 */
	// private void add2GramsFromSameRule( int idx, Set<IntegerPair> twograms ) {
	//
	// }

	public int getNumApplicableRules() {
		int count = 0;
		for( int i = 0; i < applicableRules.length; ++i ) {
			for( Rule rule : applicableRules[ i ] ) {
				if( rule.isSelfRule() ) {
					continue;
				}
				++count;
			}
		}
		return count;
	}

	public Rule[] getApplicableRules( int k ) {
		if( applicableRules == null )
			return null;
		else if( k < applicableRules.length )
			return applicableRules[ k ];
		else
			return EMPTY_RULE;
	}

	public Rule[] getSuffixApplicableRules( int k ) {
		if( suffixApplicableRules == null )
			return null;
		else if( k < suffixApplicableRules.length )
			return suffixApplicableRules[ k ];
		else
			return EMPTY_RULE;
	}

	public short getSearchRange( int k ) {
		return searchrange[ k ];
	}

	public short getMaxSearchRange() {
		return maxsearchrange;
	}

	public short getInvSearchRange( int k ) {
		return invsearchrange[ k ];
	}

	public short getMaxInvSearchRange() {
		return maxinvsearchrange;
	}

	public List<Rule> getMatched( int[] residual, int sidx ) {
		if( applicableRulesTrie == null )
			throw new RuntimeException();
		return applicableRulesTrie.applicableRules( residual, sidx );
	}

	public int[] getCandidateLengths( int k ) {
		if( transformedLengths == null )
			return null;
		return transformedLengths[ k ];
	}

	public long getEstNumRecords() {
		return estimated_equivs[ estimated_equivs.length - 1 ];
	}

	public long getEstExpandCost() {
		// cost[i] : expand cost for x[1..i].
		long[] costs = new long[ size() + 1 ];
		for( int i = 1; i <= size(); ++i ) {
			Rule[] rules = getSuffixApplicableRules( i - 1 );
			for( Rule rule : rules ) {
				if( rule.isSelfRule() )
					continue;
				int deltalen = rule.getTo().length - rule.getFrom().length;
				int previdx = i - rule.getFrom().length - 1;
				long equivs = previdx >= 0 ? estimated_equivs[ previdx ] : 1;
				long newcost = costs[ i - rule.getFrom().length ] + deltalen * ( equivs - 1 ) + size() + deltalen;
				costs[ i ] += newcost;
			}
			costs[ i ] += costs[ i - 1 ];
		}
		return costs[ size() ];
	}

	public int getRuleCount() {
		int count = 0;
		for( Rule[] rules : applicableRules )
			count += rules.length;
		return count;
	}

	public int getFirstRuleCount() {
		return suffixApplicableRules[ size() - 1 ].length;
	}

	@Override
	public int getID() {
		return id;
	}

	public void setID( int id ) {
		this.id = id;
	}

	@Override
	public int size() {
		return tokens.length;
	}

	@Override
	public int compareTo( Record o ) {
		if( tokens.length != o.tokens.length )
			return tokens.length - o.tokens.length;
		int idx = 0;
		while( idx < tokens.length ) {
			int cmp = Integer.compare( tokens[ idx ], o.tokens[ idx ] );
			if( cmp != 0 )
				return cmp;
			++idx;
		}
		return 0;
	}

	/**
	 * Expand this record with preprocessed rules
	 */
	public ArrayList<Record> expandAll() {
		ArrayList<Record> rslt = new ArrayList<Record>();
		expandAll( rslt, 0, this.tokens );
		return rslt;
	}

	private void expandAll( ArrayList<Record> rslt, int idx, int[] t ) {
		expandAllCount++;

		Rule[] rules = applicableRules[ idx ];

		for( Rule rule : rules ) {
			if( rule.isSelfRule() ) {
				if( idx + 1 != tokens.length ) {
					expandAll( rslt, idx + 1, t );
				}
				else {
					rslt.add( new Record( t ) );
				}
			}
			else {
				int newSize = t.length - rule.fromSize() + rule.toSize();
				expandAllIterCount += newSize;

				int[] new_rec = new int[ newSize ];

				int rightSize = tokens.length - idx;
				int rightMostSize = rightSize - rule.fromSize();

				int k = 0;
				for( int i = 0; i < t.length - rightSize; i++ ) {
					new_rec[ k++ ] = t[ i ];
				}
				for( int i = 0; i < rule.toSize(); i++ ) {
					new_rec[ k++ ] = rule.getTo()[ i ];
				}
				for( int i = t.length - rightMostSize; i < t.length; i++ ) {
					new_rec[ k++ ] = t[ i ];
				}

				int new_idx = idx + rule.fromSize();
				if( new_idx == tokens.length ) {
					rslt.add( new Record( new_rec ) );
				}
				else {
					expandAll( rslt, new_idx, new_rec );
				}
			}
		}
	}

	/**
	 * Expand this record with given rule trie
	 */
	@Deprecated
	public ArrayList<Record> expandAll( RuleTrie atm ) {
		ArrayList<Record> rslt = new ArrayList<Record>();
		expandAll( rslt, atm, 0 );
		return rslt;
	}

	/**
	 * @param rslt
	 *            Result records
	 * @param tidx
	 *            Transformed location index
	 */
	@Deprecated
	private void expandAll( ArrayList<Record> rslt, RuleTrie atm, int idx ) {
		if( idx == tokens.length ) {
			rslt.add( this );
			return;
		}
		ArrayList<Rule> rules = atm.applicableRules( tokens, idx );
		for( Rule rule : rules ) {
			Record new_rec = this;
			if( !rule.isSelfRule() ) {
				new_rec = applyRule( rule, idx );
			}
			int new_idx = idx + rule.toSize();
			new_rec.expandAll( rslt, atm, new_idx );
		}
	}

	private Record applyRule( Rule rule, int idx ) {
		Record rslt = new Record();
		int shift = rule.toSize() - rule.fromSize();
		int length = this.size() + shift;
		rslt.tokens = new int[ length ];
		for( int i = 0; i < idx; ++i )
			rslt.tokens[ i ] = this.tokens[ i ];
		// Applied
		for( int i = 0; i < rule.toSize(); ++i )
			rslt.tokens[ idx + i ] = rule.getTo()[ i ];
		for( int i = idx + rule.fromSize(); i < this.tokens.length; ++i )
			rslt.tokens[ i + shift ] = this.tokens[ i ];
		return rslt;
	}

	public static String twoGram2String( IntegerPair twogram ) {
		int token1 = twogram.i1;
		int token2 = twogram.i2;
		String rslt = strlist.get( token1 );
		if( token2 != Integer.MAX_VALUE )
			rslt = rslt + " " + strlist.get( token2 );
		return rslt;
	}

	@Override
	public String toString() {
		String rslt = "";
		for( int token : tokens ) {
			rslt += strlist.get( token ) + " ";
		}
		return rslt;
	}

	public String toString( List<String> strlist2 ) {
		String rslt = "";
		for( int token : tokens ) {
			rslt += strlist2.get( token ) + " ";
		}
		return rslt;
	}

	@Override
	public int hashCode() {
		if( !validHashValue ) {
			long tmp = 0;
			for( int token : tokens ) {
				tmp = ( tmp << 32 ) + token;
				tmp = tmp % bigprime;
			}
			hashValue = (int) ( tmp % Integer.MAX_VALUE );
			validHashValue = true;
		}
		return hashValue;
	}

	@Override
	public boolean equals( Object o ) {
		if( o != null ) {
			Record orec = (Record) o;

			if( this == orec ) {
				return true;
			}

			return StaticFunctions.compare( tokens, orec.tokens ) == 0;
		}
		else {
			return false;
		}
	}

	@Override
	public int getMinLength() {
		return transformedLengths[ transformedLengths.length - 1 ][ 0 ];
	}

	@Override
	public int getMaxLength() {
		return transformedLengths[ transformedLengths.length - 1 ][ 1 ];
	}

	@Override
	public Collection<Integer> getTokens() {
		List<Integer> list = new ArrayList<Integer>();
		for( int i : tokens )
			list.add( i );
		return list;
	}

	public int[] getTokenArray() {
		return tokens;
	}

	@Override
	public double similarity( RecordInterface rec ) {
		if( rec.getClass() != Record.class )
			return 0;
		int compare = checker.isEqual( this, (Record) rec );
		if( compare >= 0 )
			return 1;
		else
			return 0;
	}

	@Override
	public Set<Integer> getSignatures( ITF_Filter filter, double theta ) {
		IntegerSet sig = new IntegerSet();
		if( availableTokens != null )
			for( int token : availableTokens[ 0 ] )
				sig.add( token );
		else
			sig.addAll( this.getAvailableTokens()[ 0 ] );
		return sig;
	}

	@Override
	public Set<? extends Expanded> generateAll() {
		List<Record> list = this.expandAll( atm );
		Set<Record> set = new WYK_HashSet<Record>( list );
		return set;
	}

	@Override
	public RecordInterface toRecord() {
		return this;
	}

	@Override
	public double similarity( Expanded rec ) {
		if( rec.getClass() != Record.class )
			return 0;
		int compare = checker.isEqual( this, (Record) rec );
		if( compare >= 0 )
			return 1;
		else
			return 0;
	}

	public Record randomTransform( Rule_ACAutomata atm, Random rand ) {
		List<Integer> list = new ArrayList<Integer>();
		Rule[][] rules = atm.applicableRules( tokens, 0 );
		int idx = 0;
		while( idx < tokens.length ) {
			int ruleidx = rand.nextInt( rules[ idx ].length );
			Rule rule = rules[ idx ][ ruleidx ];
			for( int token : rule.getTo() )
				list.add( token );
			idx += rule.getFrom().length;
		}
		int[] transformed = new int[ list.size() ];
		for( idx = 0; idx < list.size(); ++idx )
			transformed[ idx ] = list.get( idx );
		return new Record( transformed );
	}

	// public boolean shareLastToken( Record e ) {
	// Map<Integer, IntegerPair> smallMap = null;
	// Map<Integer, IntegerPair> otherMap = null;
	//
	// // iterate small map
	// // if( e.lastTokenMap.size() < lastTokenMap.size() ) {
	// smallMap = lastTokenMap;
	// otherMap = e.lastTokenMap;
	// // }
	// // else {
	// // smallMap = e.lastTokenMap;
	// // otherMap = lastTokenMap;
	// // }
	//
	// for( Map.Entry<Integer, IntegerPair> entry : smallMap.entrySet() ) {
	// IntegerPair range = otherMap.get( entry.getKey() );
	//
	// if( range == null ) {
	// continue;
	// }
	//
	// IntegerPair thisRange = entry.getValue();
	// if( StaticFunctions.overlap( range.i1, range.i2, thisRange.i1, thisRange.i2 ) ) {
	// return true;
	// }
	// }
	// return false;
	// }

	public long getEstimatedEquiv() {
		return estimated_equivs[ estimated_equivs.length - 1 ];
	}
}
