package snu.kdd.synonym.algorithm.revised;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tools.IntegerPair;
import tools.Rule;
import tools.RuleTrie;
import tools.Rule_ACAutomata;
import tools.StaticFunctions;
import tools.WYK_HashSet;

public class RevRecord {
	public int id;
	protected int[] tokens;

	/**
	 * For DynamicMatch.
	 * applicableRules[i] contains all the rules which can be applied to the
	 * prefix of str[i].
	 */
	protected Rule[][] applicableRules = null;

	protected static final Rule[] EMPTY_RULE = new Rule[ 0 ];

	/**
	 * For Length filter
	 * transformedLengths[ i - 1 ][ 0 ] = l_{min}(s[1,i],R)
	 * transformedLengths[ i - 1 ][ 1 ] = l_{Max}(s[1,i],R)
	 */
	protected int[][] transformedLengths = null;

	/**
	 * Estimate the number of equivalent records
	 */
	protected long[] estimated_equivs = null;

	public RevRecord( int id, String str, Map<String, Integer> str2int ) {
		this.id = id;
		String[] pstr = str.split( "[ |\t]+" );
		tokens = new int[ pstr.length ];
		for( int i = 0; i < pstr.length; ++i ) {
			tokens[ i ] = str2int.get( pstr[ i ] );
		}
	}

	public RevRecord() {
		this.id = -1;
	}

	public void preprocessRules( Rule_ACAutomata automata, boolean buildtrie ) {
		applicableRules = automata.applicableRules( tokens, 0 );
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

	public void preprocessEstimatedRecords() {
		@SuppressWarnings( "unchecked" )
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
					System.out.println( "Too many generalizations: " + id + " size " + size );
					return;
				}
			}
			est[ i ] = size;
		}
	}

	/**
	 * Returns all available (actually, superset) 2 grams
	 */
	public List<Set<IntegerPair>> get2Grams() {
		/* There are two type of 2 grams:
		 * 1) two tokens are derived from different rules.
		 * 2) two tokens are generated from the same rule. */

		// twograms.get( k ) returns all positional two-grams each of whose position is k
		List<Set<IntegerPair>> twograms = new ArrayList<Set<IntegerPair>>();
		int[] range = getCandidateLengths( size() - 1 );

		int maxRange = Integer.max( range[ 1 ] - 1, 1 ); // if Maximum length is 1, we need to generate (token, EOL) twogram
		for( int i = 0; i < maxRange; ++i ) { // generates all positional two-grams with k = 1, ..., l_{Max}(s, R) - 1
			twograms.add( new WYK_HashSet<IntegerPair>( 30 ) );
		}
		add2GramsFromDiffRules( twograms );
		add2GramsFromSameRule( twograms );
		return twograms;
	}

	// maxStartPos exclusive
	public List<Set<IntegerPair>> get2GramsWithBound( int maxStartPos ) {
		/* There are two type of 2 grams:
		 * 1) two tokens are derived from different rules.
		 * 2) two tokens are generated from the same rule. */

		// twograms.get( k ) returns all positional two-grams each of whose position is k
		List<Set<IntegerPair>> twograms = new ArrayList<Set<IntegerPair>>();
		int[] range = getCandidateLengths( size() - 1 );

		// to include maxLength
		int max = Integer.min( maxStartPos, Integer.max( 1, range[ 1 ] - 1 ) );

		for( int i = 0; i < max; ++i ) { // generates all positional two-grams with k = 1, ..., l_{Max}(s, R)
			twograms.add( new WYK_HashSet<IntegerPair>( 30 ) );
		}
		add2GramsFromDiffRulesWithBound( twograms, maxStartPos );
		add2GramsFromSameRuleWithBound( twograms, maxStartPos );
		return twograms;
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

					// TODO DEBUG
					// for( int idx = min; idx <= max; ++idx ) {
					for( int idx = Integer.min( 1, max ); idx >= min; idx-- ) {
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
					// TODO DEBUG
					// for( int idx = min; idx <= max; ++idx ) {
					for( int idx = Integer.min( 1, max ); idx >= min; idx-- ) {
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

	public Rule[] getApplicableRules( int k ) {
		if( applicableRules == null )
			return null;
		else if( k < applicableRules.length )
			return applicableRules[ k ];
		else
			return EMPTY_RULE;
	}

	public int[] getCandidateLengths( int k ) {
		if( transformedLengths == null )
			return null;
		return transformedLengths[ k ];
	}

	public int getID() {
		return id;
	}

	public void setID( int id ) {
		this.id = id;
	}

	public int size() {
		return tokens.length;
	}

	public int compareTo( RevRecord o ) {
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
	 * Expand this record with given rule trie
	 */
	public ArrayList<RevRecord> expandAll( RuleTrie atm ) {
		ArrayList<RevRecord> rslt = new ArrayList<RevRecord>();
		expandAll( rslt, atm, 0 );
		return rslt;
	}

	/**
	 * @param rslt
	 *            Result records
	 * @param tidx
	 *            Transformed location index
	 */
	private void expandAll( ArrayList<RevRecord> rslt, RuleTrie atm, int idx ) {
		if( idx == tokens.length ) {
			rslt.add( this );
			return;
		}
		ArrayList<Rule> rules = atm.applicableRules( tokens, idx );
		for( Rule rule : rules ) {
			RevRecord new_rec = this;
			if( !StaticFunctions.isSelfRule( rule ) ) {
				new_rec = applyRule( rule, idx );
			}
			int new_idx = idx + rule.toSize();
			new_rec.expandAll( rslt, atm, new_idx );
		}
	}

	private RevRecord applyRule( Rule rule, int idx ) {
		RevRecord rslt = new RevRecord();
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
}
