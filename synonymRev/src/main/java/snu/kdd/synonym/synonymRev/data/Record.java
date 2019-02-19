package snu.kdd.synonym.synonymRev.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import sigmod13.RecordInterface;
import sigmod13.filter.ITF_Filter;
import snu.kdd.synonym.synonymRev.tools.DEBUG;
import snu.kdd.synonym.synonymRev.tools.IntegerSet;
import snu.kdd.synonym.synonymRev.tools.QGram;
import snu.kdd.synonym.synonymRev.tools.QGramEntry;
import snu.kdd.synonym.synonymRev.tools.QGramRange;
import snu.kdd.synonym.synonymRev.tools.StaticFunctions;
import snu.kdd.synonym.synonymRev.tools.Util;
import snu.kdd.synonym.synonymRev.tools.WYK_HashSet;
import snu.kdd.synonym.synonymRev.validator.Validator;

public class Record implements Comparable<Record>, RecordInterface, RecordInterface.Expanded {
	public static int expandAllCount = 0;

	private int[] tokens;
	private final int id;
	private final int num_dist_tokens;

	public Rule[][] applicableRules = null;
	protected int[][] transformLengths = null;

	private long[] estTrans;

	private boolean validHashValue = false;
	private int hashValue;

	public static TokenIndex tokenIndex = null;
	protected Rule[][] suffixApplicableRules = null;

	public int getQGramCount = 0;
	
	public static void initStatic() {
		expandAllCount = 0;
		tokenIndex = null;
	}

	public Record( int id, String str, TokenIndex tokenIndex ) {
		this.id = id;
		String[] pstr = str.split( "( |\t)+" );
		tokens = new int[ pstr.length ];
		for( int i = 0; i < pstr.length; ++i ) {
			tokens[ i ] = tokenIndex.getID( pstr[ i ] );
		}
		
		num_dist_tokens = new IntOpenHashSet( tokens ).size();
	}

	public Record( int[] tokens ) {
		this.id = -1;
		this.tokens = tokens;
		num_dist_tokens = new IntOpenHashSet( tokens ).size();
	}

	@Override
	public int compareTo( Record o ) {
		if( tokens.length != o.tokens.length ) {
			return tokens.length - o.tokens.length;
		}

		int idx = 0;
		while( idx < tokens.length ) {
			int cmp = Integer.compare( tokens[ idx ], o.tokens[ idx ] );
			if( cmp != 0 ) {
				return cmp;
			}
			++idx;
		}
		return 0;
	}

	public int[] getTokensArray() {
		return tokens;
	}

	public int getTokenCount() {
		return tokens.length;
	}

	public int getDistinctTokenCount() {
		return num_dist_tokens;
	}

	/**
	 * Set applicable rules
	 */

	public void preprocessApplicableRules( ACAutomataR automata ) {
		applicableRules = automata.applicableRules( tokens );
	}

	public Rule[][] getApplicableRules() {
		return applicableRules;
	}

	public Rule[] getApplicableRules( int k ) {
		if( applicableRules == null ) {
			return null;
		}
		else if( k < applicableRules.length ) {
			return applicableRules[ k ];
		}
		else {
			return Rule.EMPTY_RULE;
		}
	}

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

	/**
	 * Set and return estimated number of transformed strings of the string
	 */

	public void preprocessEstimatedRecords() {
		@SuppressWarnings( "unchecked" )
		ArrayList<Rule>[] tmpAppRules = new ArrayList[ tokens.length ];
		for( int i = 0; i < tokens.length; ++i )
			tmpAppRules[ i ] = new ArrayList<Rule>();

		for( int i = 0; i < tokens.length; ++i ) {
			for( Rule rule : applicableRules[ i ] ) {
				int eidx = i + rule.leftSize() - 1;
				tmpAppRules[ eidx ].add( rule );
			}
		}

		long[] est = new long[ tokens.length ];
		estTrans = est;
		for( int i = 0; i < est.length; ++i ) {
			est[ i ] = Long.MAX_VALUE;
		}

		for( int i = 0; i < tokens.length; ++i ) {
			long size = 0;
			for( Rule rule : tmpAppRules[ i ] ) {
				int sidx = i - rule.leftSize() + 1;
				if( sidx == 0 ) {
					size += 1;
				}
				else {
					size += est[ sidx - 1 ];
				}

				if( size < 0 ) {
					Util.printLog( "Too many generalizations: " + id + " size " + size );

					return;
				}
			}
			est[ i ] = size;
		}
		if( DEBUG.EstTooManyWarningON ) {
			if( est[ tokens.length - 1 ] > DEBUG.EstTooManyThreshold ) {
				Util.printLog( "Too many generalizations: " + id + " size " + est[ tokens.length - 1 ] );
			}
		}
	}

	public long getEstNumTransformed() {
		return estTrans[ estTrans.length - 1 ];
	}

	/**
	 * Expand this record with preprocessed rules
	 */

	public ArrayList<Record> expandAll() {
		ArrayList<Record> rslt = new ArrayList<Record>();
		expandAll( rslt, 0, this.tokens );
		return rslt;
	}

	// applicableRules should be previously computed
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
				int newSize = t.length - rule.leftSize() + rule.rightSize();

				int[] new_rec = new int[ newSize ];

				int rightSize = tokens.length - idx;
				int rightMostSize = rightSize - rule.leftSize();

				int[] rhs = rule.getRight();

				int k = 0;
				for( int i = 0; i < t.length - rightSize; i++ ) {
					new_rec[ k++ ] = t[ i ];
				}
				for( int i = 0; i < rhs.length; i++ ) {
					new_rec[ k++ ] = rhs[ i ];
				}
				for( int i = t.length - rightMostSize; i < t.length; i++ ) {
					new_rec[ k++ ] = t[ i ];
				}

				int new_idx = idx + rule.leftSize();
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
	 * Transformed lengths
	 */

	public void preprocessTransformLength() {
		transformLengths = new int[ tokens.length ][ 2 ];
		for( int i = 0; i < tokens.length; ++i )
			transformLengths[ i ][ 0 ] = transformLengths[ i ][ 1 ] = i + 1;

		for( Rule rule : applicableRules[ 0 ] ) {
			int fromSize = rule.leftSize();
			int toSize = rule.rightSize();
			if( fromSize > toSize ) {
				transformLengths[ fromSize - 1 ][ 0 ] = Math.min( transformLengths[ fromSize - 1 ][ 0 ], toSize );
			}
			else if( fromSize < toSize ) {
				transformLengths[ fromSize - 1 ][ 1 ] = Math.max( transformLengths[ fromSize - 1 ][ 1 ], toSize );
			}
		}
		for( int i = 1; i < tokens.length; ++i ) {
			transformLengths[ i ][ 0 ] = Math.min( transformLengths[ i ][ 0 ], transformLengths[ i - 1 ][ 0 ] + 1 );
			transformLengths[ i ][ 1 ] = Math.max( transformLengths[ i ][ 1 ], transformLengths[ i - 1 ][ 1 ] + 1 );
			for( Rule rule : applicableRules[ i ] ) {
				int fromSize = rule.leftSize();
				int toSize = rule.rightSize();
				if( fromSize > toSize ) {
					transformLengths[ i + fromSize - 1 ][ 0 ] = Math.min( transformLengths[ i + fromSize - 1 ][ 0 ],
							transformLengths[ i - 1 ][ 0 ] + toSize );
				}
				else if( fromSize < toSize ) {
					transformLengths[ i + fromSize - 1 ][ 1 ] = Math.max( transformLengths[ i + fromSize - 1 ][ 1 ],
							transformLengths[ i - 1 ][ 1 ] + toSize );
				}

			}
		}
	}
	
	public int[][] getTransLengthsAll() {
		return transformLengths;
	}

	public int[] getTransLengths() {
		return transformLengths[ tokens.length - 1 ];
	}

	public int getMaxTransLength() {
		return transformLengths[ tokens.length - 1 ][ 1 ];
	}

	public int getMinTransLength() {
		return transformLengths[ tokens.length - 1 ][ 0 ];
	}

	public String toString() {
		if( Record.tokenIndex != null ) {
			return toString( Record.tokenIndex );
		}
		else {
			String rslt = "";
			for( int id : tokens ) {
				if( rslt.length() != 0 ) {
					rslt += " ";
				}
				rslt += id;
			}
			return rslt;
		}
	}

	public String toString( TokenIndex tokenIndex ) {
		String rslt = "";
		for( int id : tokens ) {
			rslt += tokenIndex.getToken( id ) + " ";
		}
		return rslt;
	}

	public int getID() {
		return id;
	}

	@Override
	public int hashCode() {
		if( !validHashValue ) {
			long tmp = 0;
			for( int token : tokens ) {
                tmp = ( tmp << 32 ) + token;
//                tmp = 0x1f1f1f1f ^ tmp + token;
				tmp = tmp % Util.bigprime;
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
			if( id == orec.id || id == -1 || orec.id == -1 ) {
				if( id == -1 || orec.id == -1 ) {
					return StaticFunctions.compare( tokens, orec.tokens ) == 0;
				}
				return true;
			}

			return false;
		}
		else {
			return false;
		}
	}

	/* Get/set suffix applicable rules for validators */

	public void preprocessSuffixApplicableRules() {
		List<List<Rule>> tmplist = new ArrayList<List<Rule>>();

		for( int i = 0; i < tokens.length; ++i ) {
			tmplist.add( new ArrayList<Rule>() );
		}

		for( int i = tokens.length - 1; i >= 0; --i ) {
			for( Rule rule : applicableRules[ i ] ) {
				int suffixidx = i + rule.getLeft().length - 1;
				tmplist.get( suffixidx ).add( rule );
			}
		}

		suffixApplicableRules = new Rule[ tokens.length ][];
		for( int i = 0; i < tokens.length; ++i ) {
			suffixApplicableRules[ i ] = tmplist.get( i ).toArray( new Rule[ 0 ] );
		}
	}

	public Rule[] getSuffixApplicableRules( int k ) {
		if( suffixApplicableRules == null ) {
			return null;
		}
		else if( k < suffixApplicableRules.length ) {
			return suffixApplicableRules[ k ];
		}
		else {
			return Rule.EMPTY_RULE;
		}
	}

	/* Get positional qgrams */

	public List<List<QGram>> getQGrams( int q ) {
		getQGramCount++;
		int maxLength = getMaxTransLength();
		List<List<QGram>> positionalQGram = new ArrayList<List<QGram>>( maxLength );

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
				minIndex = transformLengths[ t - 1 ][ 0 ];
				maxIndex = transformLengths[ t - 1 ][ 1 ];
			}

			// try {
			for( int r = 0; r < rules.length; r++ ) {
				Rule startRule = rules[ r ];

				// System.out.println( "Start Rule: " + startRule );

				Stack<QGramEntry> stack = new Stack<QGramEntry>();

				stack.add( new QGramEntry( q, startRule, t ) );

				while( !stack.isEmpty() ) {
					QGramEntry entry = stack.pop();
					// System.out.println( "Entry: " + entry );

					if( entry.length - entry.getLeftRHSLength() >= q - 1 ) {
						entry.generateQGram( q, positionalQGram, minIndex, maxIndex );
					}
					else {
						if( entry.rightMostIndex < tokens.length ) {
							// append
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
		}

		List<List<QGram>> resultQGram = new ArrayList<List<QGram>>( getMaxTransLength() );

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
		int maxLength = Integer.min( range, getMaxTransLength() );
		List<List<QGram>> positionalQGram = new ArrayList<List<QGram>>( maxLength );

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
				minIndex = transformLengths[ t - 1 ][ 0 ];
				maxIndex = transformLengths[ t - 1 ][ 1 ];
			}

			if( minIndex >= range ) {
				continue;
			}

			for( int r = 0; r < rules.length; r++ ) {
				Rule startRule = rules[ r ];
				// System.out.println( "Start Rule: " + startRule );

				Stack<QGramEntry> stack = new Stack<QGramEntry>();

				stack.add( new QGramEntry( q, startRule, t ) );

				while( !stack.isEmpty() ) {
					QGramEntry entry = stack.pop();

					// System.out.println( "Entry: " + entry );
					if( entry.length - entry.getLeftRHSLength() >= q - 1 ) {
						entry.generateQGram( q, positionalQGram, minIndex, maxIndex, range );
					}
					else {
						if( entry.rightMostIndex < tokens.length ) {
							// append
							if( minIndex + entry.builtPosition < range ) {
								for( Rule nextRule : applicableRules[ entry.rightMostIndex ] ) {
									QGramEntry newEntry = new QGramEntry( entry, nextRule );

									stack.add( newEntry );
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
		}

		int maxSize = 0;
		for( int i = 0; i < maxLength; i++ ) {
			int size = positionalQGram.get( i ).size();
			if( maxSize < size ) {
				maxSize = size;
			}
		}

		List<List<QGram>> resultQGram = new ArrayList<List<QGram>>( maxLength );
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

	public List<QGramRange> getQGramRange( int q ) {
		getQGramCount++;
		Object2ObjectOpenHashMap<QGram, List<QGramRange>> positionalQGram = new Object2ObjectOpenHashMap<>();

		for( int t = 0; t < tokens.length; t++ ) {
			Rule[] rules = applicableRules[ t ];

			int minIndex;
			int maxIndex;

			if( t == 0 ) {
				minIndex = 0;
				maxIndex = 0;
			}
			else {
				minIndex = transformLengths[ t - 1 ][ 0 ];
				maxIndex = transformLengths[ t - 1 ][ 1 ];
			}

			// try {
			for( int r = 0; r < rules.length; r++ ) {
				Rule startRule = rules[ r ];

				Stack<QGramEntry> stack = new Stack<QGramEntry>();

				stack.add( new QGramEntry( q, startRule, t ) );

				while( !stack.isEmpty() ) {
					QGramEntry entry = stack.pop();

					if( entry.length - entry.getLeftRHSLength() >= q - 1 ) {
						entry.generateQGramRange( q, positionalQGram, minIndex, maxIndex );
					}
					else {
						if( entry.rightMostIndex < tokens.length ) {
							// append more rules

							for( Rule nextRule : applicableRules[ entry.rightMostIndex ] ) {
								stack.add( new QGramEntry( entry, nextRule ) );
							}
						}
						else {
							// add EOF
							entry.eof = true;
							entry.generateQGramRange( q, positionalQGram, minIndex, maxIndex );
						}
					}
				}
			}
		}

		ArrayList<QGramRange> resultList = new ArrayList<>();
		ObjectIterator<Entry<QGram, List<QGramRange>>> iter = positionalQGram.entrySet().iterator();
		while( iter.hasNext() ) {
			resultList.addAll( iter.next().getValue() );
		}

		return resultList;
	}
	
	public List<List<QGram>> getSelfQGrams( int q ) {
		return getSelfQGrams( q, tokens.length );
	}

	public List<List<QGram>> getSelfQGrams( int q, int range ) {
		getQGramCount++;
		List<List<QGram>> positionalQGram = new ArrayList<List<QGram>>();

		int maxLength = Integer.min( range, tokens.length );
		for( int i = 0; i < maxLength; i++ ) {
			positionalQGram.add( new ArrayList<QGram>( 1 ) );
		}

		int i = 0;
		for( ; i < tokens.length - q + 1; i++ ) {

			if( i >= maxLength )
				break;

			int[] qgramArray = new int[ q ];

			for( int j = 0; j < q; j++ ) {
				qgramArray[ j ] = tokens[ i + j ];
			}

			QGram qgram = new QGram( qgramArray );

			List<QGram> list = positionalQGram.get( i );

			list.add( qgram );
		}

		for( ; i < maxLength; i++ ) {
			int[] qgramArray = new int[ q ];
			for( int j = 0; j < q; j++ ) {
				if( i + j < tokens.length ) {
					qgramArray[ j ] = tokens[ i + j ];
				}
				else {
					qgramArray[ j ] = Integer.MAX_VALUE;
				}
			}

			QGram qgram = new QGram( qgramArray );

			List<QGram> list = positionalQGram.get( i );

			list.add( qgram );
		}

		return positionalQGram;
	}

	public List<QGramRange> getSelfQGramsWithRange( int q ) {
		getQGramCount++;
		List<QGramRange> positionalQGram = new ArrayList<QGramRange>();

		int maxLength = tokens.length;

		int i = 0;
		for( ; i < tokens.length - q + 1; i++ ) {

			if( i >= maxLength )
				break;

			int[] qgramArray = new int[ q ];

			for( int j = 0; j < q; j++ ) {
				qgramArray[ j ] = tokens[ i + j ];
			}

			QGram qgram = new QGram( qgramArray );
			QGramRange qgramRange = new QGramRange( qgram, i, i );

			positionalQGram.add( qgramRange );
		}

		for( ; i < maxLength; i++ ) {
			int[] qgramArray = new int[ q ];
			for( int j = 0; j < q; j++ ) {
				if( i + j < tokens.length ) {
					qgramArray[ j ] = tokens[ i + j ];
				}
				else {
					qgramArray[ j ] = Integer.MAX_VALUE;
				}
			}

			QGram qgram = new QGram( qgramArray );
			QGramRange qgramRange = new QGramRange( qgram, i, i );

			positionalQGram.add( qgramRange );
		}

		return positionalQGram;
	}

	@Override
	public RecordInterface toRecord() {
		return this;
	}

	@Override
	public double similarity( Expanded rec, Validator checker ) {
		if( rec.getClass() != Record.class )
			return 0;
		int compare = checker.isEqual( this, (Record) rec );
		if( compare >= 0 )
			return 1;
		else
			return 0;
	}

	@Override
	public int getMinLength() {
		return transformLengths[ transformLengths.length - 1 ][ 0 ];
	}

	@Override
	public int getMaxLength() {
		return transformLengths[ transformLengths.length - 1 ][ 1 ];
	}

	@Override
	public int size() {
		return tokens.length;
	}

	@Override
	public Collection<Integer> getTokens() {
		List<Integer> list = new ArrayList<Integer>();
		for( int i : tokens )
			list.add( i );
		return list;
	}

	@Override
	public double similarity( RecordInterface rec, Validator checker ) {
		if( rec.getClass() != Record.class )
			return 0;
		int compare = checker.isEqual( this, (Record) rec );
		if( compare >= 0 )
			return 1;
		else
			return 0;
	}

	protected IntegerSet[] availableTokens = null;

	public void preprocessAvailableTokens( int maxlength ) {
		assert ( maxlength > 0 );
		maxlength = Math.min( maxlength, transformLengths[ tokens.length - 1 ][ 1 ] );
		availableTokens = new IntegerSet[ maxlength ];
		for( int i = 0; i < maxlength; ++i )
			availableTokens[ i ] = new IntegerSet();
		for( int i = 0; i < tokens.length; ++i ) {
			Rule[] rules = applicableRules[ i ];
			int[] range;
			if( i == 0 )
				range = new int[] { 0, 0 };
			else
				range = transformLengths[ i - 1 ];
			int from = range[ 0 ];
			int to = range[ 1 ];
			for( Rule rule : rules ) {
				int[] tokens = rule.getRight();
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

	private IntegerSet sigS = null;
	private IntegerSet sigT = null;
	
	@Override
	public Set<Integer> getSignatures( ITF_Filter filter, double theta ) {
		/*
		 * theta is not used. 
		 * In fact, theta is supposed to be 1.
		 * Thus, sig is the set of the first token of every transformed record of this record.
		 * 
		 * In order to preserve the code in package sigmod13,
		 * we use the unused theta as a flag which is 100 if this record is expandable (i.e., this record is from the searchedSet),
		 * and -100 otherwise (i.e., this record is from the indexedSet).
		 * 
		 * the signatures are generated in the same way with the original paper.
		 */
		if ( theta > 10 ) {
			if ( sigS != null ) return sigS;
			sigS = new IntegerSet();
			for ( Record exp : this.expandAll() ) {
				sigS.add( Arrays.stream( exp.getTokensArray() ).min().getAsInt() );
			}
			return sigS;
		}
		else if (theta < -10 ) {
			if ( sigT != null ) return sigT;
			sigT = new IntegerSet();
			sigT.add( Arrays.stream( tokens ).min().getAsInt() );
			return sigT;
		}
		else return null;
	}

	@Override
	public Set<? extends Expanded> generateAll() {
		return null;
	}
}
