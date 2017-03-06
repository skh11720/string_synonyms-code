package mine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import sigmod13.RecordInterface;
import sigmod13.filter.ITF_Filter;
import tools.IntegerPair;
import tools.IntegerSet;
import tools.Rule;
import tools.RuleTrie;
import tools.Rule_ACAutomata;
import tools.Rule_InverseTrie;
import tools.StaticFunctions;
import tools.WYK_HashSet;
import validator.DefaultValidator;
import validator.Validator;

public class Record implements Comparable<Record>, RecordInterface, RecordInterface.Expanded {
	protected static List<String> strlist;
	protected static RuleTrie atm;
	protected int id;
	/**
	 * For fast hashing
	 */
	protected boolean validHashValue = false;
	protected int hashValue;
	private static final int bigprime = 1645333507;

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
	 */
	protected int[][] candidateLengths = null;
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
		String[] pstr = str.split( "[ |\t]+" );
		tokens = new int[ pstr.length ];
		for( int i = 0; i < pstr.length; ++i )
			tokens[ i ] = str2int.get( pstr[ i ] );
	}

	public Record( Record o ) {
		id = -1;
		tokens = new int[ o.tokens.length ];
		for( int i = 0; i < tokens.length; ++i )
			tokens[ i ] = o.tokens[ i ];
	}

	public void preprocessRules( Rule_ACAutomata automata, boolean buildtrie ) {
		applicableRules = automata.applicableRules( tokens, 0 );
		if( buildtrie )
			applicableRulesTrie = new Rule_InverseTrie( applicableRules );
	}

	/**
	 * preprocessLengths(), addSelfTokenRules() and preprocessRules() should be
	 * called before this method is called
	 */
	// Interval tree를 이용해서 available token set을 저장할 수도 있음
	public void preprocessAvailableTokens( int maxlength ) {
		assert ( maxlength > 0 );
		maxlength = Math.min( maxlength, candidateLengths[ tokens.length - 1 ][ 1 ] );
		availableTokens = new IntegerSet[ maxlength ];
		for( int i = 0; i < maxlength; ++i )
			availableTokens[ i ] = new IntegerSet();
		for( int i = 0; i < tokens.length; ++i ) {
			Rule[] rules = applicableRules[ i ];
			int[] range;
			if( i == 0 )
				range = new int[] { 0, 0 };
			else
				range = candidateLengths[ i - 1 ];
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
		IntegerSet[] availableTokens = new IntegerSet[ candidateLengths[ tokens.length - 1 ][ 1 ] ];
		for( int i = 0; i < availableTokens.length; ++i )
			availableTokens[ i ] = new IntegerSet();
		for( int i = 0; i < tokens.length; ++i ) {
			Rule[] rules = applicableRules[ i ];
			int[] range;
			if( i == 0 )
				range = new int[] { 0, 0 };
			else
				range = candidateLengths[ i - 1 ];
			int from = range[ 0 ];
			int to = range[ 1 ];
			for( Rule rule : rules ) {
				int[] tokens = rule.getTo();
				for( int j = 0; j < tokens.length; ++j ) {
					for( int k = from; k <= to; ++k )
						availableTokens[ k + j ].add( tokens[ j ] );
				}
			}
		}
		return availableTokens;
	}

	/**
	 * preprocessRules() should be called before this method is called
	 */
	public void preprocessLengths() {
		candidateLengths = new int[ tokens.length ][ 2 ];
		for( int i = 0; i < tokens.length; ++i )
			candidateLengths[ i ][ 0 ] = candidateLengths[ i ][ 1 ] = i + 1;

		for( Rule rule : applicableRules[ 0 ] ) {
			int fromSize = rule.fromSize();
			int toSize = rule.toSize();
			if( fromSize == toSize )
				continue;
			candidateLengths[ fromSize - 1 ][ 0 ] = Math.min( candidateLengths[ fromSize - 1 ][ 0 ], toSize );
			candidateLengths[ fromSize - 1 ][ 1 ] = Math.max( candidateLengths[ fromSize - 1 ][ 1 ], toSize );
		}
		for( int i = 1; i < tokens.length; ++i ) {
			candidateLengths[ i ][ 0 ] = Math.min( candidateLengths[ i ][ 0 ], candidateLengths[ i - 1 ][ 0 ] + 1 );
			candidateLengths[ i ][ 1 ] = Math.max( candidateLengths[ i ][ 1 ], candidateLengths[ i - 1 ][ 1 ] + 1 );
			for( Rule rule : applicableRules[ i ] ) {
				int fromSize = rule.fromSize();
				int toSize = rule.toSize();
				candidateLengths[ i + fromSize - 1 ][ 0 ] = Math.min( candidateLengths[ i + fromSize - 1 ][ 0 ],
						candidateLengths[ i - 1 ][ 0 ] + toSize );
				candidateLengths[ i + fromSize - 1 ][ 1 ] = Math.max( candidateLengths[ i + fromSize - 1 ][ 1 ],
						candidateLengths[ i - 1 ][ 1 ] + toSize );
			}
		}
	}

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
		for( int i = 0; i < est.length; ++i )
			est[ i ] = Long.MAX_VALUE;
		for( int i = 0; i < tokens.length; ++i ) {
			long size = 0;
			for( Rule rule : tmpAppRules[ i ] ) {
				int sidx = i - rule.fromSize() + 1;
				if( sidx == 0 )
					size += 1;
				else
					size += est[ sidx - 1 ];
				if( size < 0 || size > Integer.MAX_VALUE ) {
					return;
				}
			}
			est[ i ] = size;
		}
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

	/**
	 * Returns all available (actually, superset) 2 grams
	 */
	public List<Set<IntegerPair>> get2Grams() {
		long start = System.nanoTime();
		/* There are two type of 2 grams:
		 * 1) two tokens are derived from different rules.
		 * 2) two tokens are generated from the same rule. */
		List<Set<IntegerPair>> twograms = new ArrayList<Set<IntegerPair>>();
		int[] range = getCandidateLengths( size() - 1 );
		for( int i = 0; i < range[ 1 ]; ++i )
			twograms.add( new WYK_HashSet<IntegerPair>() );
		add2GramsFromDiffRules( twograms );
		add2GramsFromSameRule( twograms );
		exectime += System.nanoTime() - start;
		return twograms;
	}

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
			if( i == 0 )
				range = new int[] { 0, 0 };
			else
				range = getCandidateLengths( i - 1 );
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
					for( int idx = min; idx <= max; ++idx )
						twograms.get( idx ).add( twogram );
				}
				else {
					assert ( tailrules.length > 0 );
					for( Rule tailrule : tailrules ) {
						// Generate twogram
						IntegerPair twogram = new IntegerPair( headstr[ headstr.length - 1 ], tailrule.getTo()[ 0 ] );
						for( int idx = min; idx <= max; ++idx )
							twograms.get( idx ).add( twogram );
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
	 * Returns all available (actually, superset) 2 grams
	 * which appear in str[idx..idx+1].
	 *
	 * @param idx:
	 *            starting index of 2 grams. (starting from 0)
	 */
	public Set<IntegerPair> get2Grams( int idx ) {
		/* There are two type of 2 grams:
		 * 1) two tokens are derived from different rules.
		 * 2) two tokens are generated from the same rule. */
		Set<IntegerPair> twograms = new WYK_HashSet<IntegerPair>();
		add2GramsFromDiffRules( idx, twograms );
		add2GramsFromSameRule( idx, twograms );
		return twograms;
	}

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
	private void add2GramsFromDiffRules( int idx, Set<IntegerPair> twograms ) {
		// iterate on prefix rules: it is easier
		for( int i = 0; i < size(); ++i ) {
			Rule[] rules = getApplicableRules( i );
			int[] range = getCandidateLengths( i );
			// range[0] is the minimum index of token generated by this rule.
			// If it exceeds idx, every rule will not generate a 2-gram starting from
			// a token within them
			if( range[ 0 ] > idx )
				break;
			for( @SuppressWarnings( "unused" )
			Rule rule : rules ) {
			}
		}
	}

	/**
	 * Add every 2 grams which may appear in str[idx..idx+1] where
	 * str[idx] and str[idx + 1] are derived from the same rule
	 *
	 * @param idx
	 *            starting index of 2 grams.
	 * @param twograms
	 *            set which stores 2 grams
	 */
	private void add2GramsFromSameRule( int idx, Set<IntegerPair> twograms ) {

	}

	public int getNumApplicableRules() {
		int count = 0;
		for( int i = 0; i < applicableRules.length; ++i ) {
			for( Rule rule : applicableRules[ i ] ) {
				if( rule.getFrom().length == 1 && rule.getFrom().length == 1 && rule.getFrom()[ 0 ] == rule.getTo()[ 0 ] )
					continue;
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
		if( candidateLengths == null )
			return null;
		return candidateLengths[ k ];
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
				if( StaticFunctions.isSelfRule( rule ) )
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
	@Deprecated
	public ArrayList<Record> expandAll() {
		ArrayList<Record> rslt = new ArrayList<Record>();
		expandAll( rslt, 0 );
		return rslt;
	}

	private void expandAll( ArrayList<Record> rslt, int idx ) {
		if( idx == tokens.length ) {
			rslt.add( this );
			return;
		}
		Rule[] rules = applicableRules[ idx ];
		for( Rule rule : rules ) {
			Record new_rec = this;
			if( !StaticFunctions.isSelfRule( rule ) )
				new_rec = applyRule( rule, idx );
			int new_idx = idx + rule.toSize();
			new_rec.expandAll( rslt, new_idx );
		}
	}

	/**
	 * Expand this record with given rule trie
	 */
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
	private void expandAll( ArrayList<Record> rslt, RuleTrie atm, int idx ) {
		if( idx == tokens.length ) {
			rslt.add( this );
			return;
		}
		ArrayList<Rule> rules = atm.applicableRules( tokens, idx );
		for( Rule rule : rules ) {
			Record new_rec = this;
			if( !StaticFunctions.isSelfRule( rule ) ) {
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
		Record orec = (Record) o;
		return StaticFunctions.compare( tokens, orec.tokens ) == 0;
	}

	@Override
	public int getMinLength() {
		return candidateLengths[ candidateLengths.length - 1 ][ 0 ];
	}

	@Override
	public int getMaxLength() {
		return candidateLengths[ candidateLengths.length - 1 ][ 1 ];
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
}
