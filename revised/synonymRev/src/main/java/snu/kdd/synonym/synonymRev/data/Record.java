package snu.kdd.synonym.synonymRev.data;

import java.util.ArrayList;

import snu.kdd.synonym.synonymRev.tools.StaticFunctions;
import snu.kdd.synonym.synonymRev.tools.Util;

public class Record {
	public static int expandAllCount = 0;

	private int[] tokens;
	private int id;

	protected Rule[][] applicableRules = null;
	protected int[][] transformLengths = null;

	private long[] estTrans;

	private boolean validHashValue = false;
	private int hashValue;

	public static TokenIndex tokenIndex = null;

	public Record( int id, String str, TokenIndex tokenIndex ) {
		if( Record.tokenIndex == null ) {
			Record.tokenIndex = tokenIndex;
		}

		this.id = id;
		String[] pstr = str.split( "( |\t)+" );
		tokens = new int[ pstr.length ];
		for( int i = 0; i < pstr.length; ++i ) {
			tokens[ i ] = tokenIndex.getID( pstr[ i ] );
		}
	}

	public Record( int[] tokens ) {
		this.id = -1;
		this.tokens = tokens;
	}

	public int getTokenCount() {
		return tokens.length;
	}

	/**
	 * Set applicable rules
	 */

	public void preprocessRules( ACAutomataR automata ) {
		applicableRules = automata.applicableRules( tokens );
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

	public String toString() {
		if( Record.tokenIndex != null ) {
			return toString( Record.tokenIndex );
		}
		else {
			return "";
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
			if( id == orec.id ) {
				if( id == -1 ) {
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
}
