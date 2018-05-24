package snu.kdd.synonym.synonymRev.data;

import java.util.Arrays;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import snu.kdd.synonym.synonymRev.tools.StaticFunctions;

public class Rule implements Comparable<Rule> {
	int[] lefths;
	int[] righths;
	public final int id;

	private final int hash;
	boolean isSelfRule = false;

	private static Int2ObjectOpenHashMap<Rule> selfRuleMap = new Int2ObjectOpenHashMap<Rule>();
	
	private static int count = 0;

	protected static final Rule[] EMPTY_RULE = new Rule[ 0 ];

	public static Rule getSelfRule( int item ) {
		Rule r = selfRuleMap.get( item );

		if( r == null ) {
			r = new Rule( item, item );
			r.isSelfRule = true;
			selfRuleMap.put( item, r );
		}
		return r;
	}

	public Rule( String str, TokenIndex tokenIndex ) {
		int hash = 0;
		String[] pstr = str.split( ", " );
		String[] fpstr = pstr[ 0 ].trim().split( " " );

		lefths = new int[ fpstr.length ];
		for( int i = 0; i < fpstr.length; ++i ) {
			lefths[ i ] = tokenIndex.getID( fpstr[ i ] );
			hash = 0x1f1f1f1f ^ hash + lefths[ i ];
		}

		String[] tpstr = pstr[ 1 ].trim().split( " " );

		righths = new int[ tpstr.length ];
		for( int i = 0; i < tpstr.length; ++i ) {
			righths[ i ] = tokenIndex.getID( tpstr[ i ] );
			hash = 0x1f1f1f1f ^ hash + righths[ i ];
		}
		this.hash = hash;
		id = count++;
	}

	// needed?
	public Rule( int[] from, int[] to ) {
		int hash = 0;
		this.lefths = from;
		this.righths = to;
		for( int i = 0; i < from.length; ++i )
			hash = 0x1f1f1f1f ^ hash + from[ i ];
		for( int i = 0; i < to.length; ++i )
			hash = 0x1f1f1f1f ^ hash + to[ i ];
		this.hash = hash;
		id = count++;
	}

	// mostly used for self rule
	public Rule( int from, int to ) {
		int hash = 0;
		this.lefths = new int[ 1 ];
		this.lefths[ 0 ] = from;
		hash = 0x1f1f1f1f ^ hash + this.lefths[ 0 ];
		this.righths = new int[ 1 ];
		this.righths[ 0 ] = to;
		hash = 0x1f1f1f1f ^ hash + this.righths[ 0 ];
		this.hash = hash;
		id = count++;
	}

	public boolean isSelfRule() {
		return isSelfRule;
	}

	public int[] getLeft() {
		return lefths;
	}

	public int[] getRight() {
		return righths;
	}

	public int leftSize() {
		return lefths.length;
	}

	public int rightSize() {
		return righths.length;
	}

	@Override
	public int compareTo( Rule o ) {
		// only compares lhs
		return StaticFunctions.compare( lefths, o.lefths );
	}

	@Override
	public boolean equals( Object o ) {
		if( o == null ) {
			return false;
		}

		if( this == o ) {
			return true;
		}
		Rule ro = (Rule) o;
		if( lefths.length == ro.lefths.length && righths.length == ro.righths.length ) {
			for( int i = 0; i < leftSize(); ++i ) {
				if( lefths[ i ] != ro.lefths[ i ] ) {
					return false;
				}
			}
			for( int i = 0; i < rightSize(); ++i ) {
				if( righths[ i ] != ro.righths[ i ] ) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return hash;
	}

	@Override
	public String toString() {
		return Arrays.toString( lefths ) + " -> " + Arrays.toString( righths );
	}

	public String toOriginalString( TokenIndex tokenIndex ) {
		StringBuilder bld = new StringBuilder();
		for( int i = 0; i < lefths.length; i++ ) {
			bld.append( tokenIndex.getToken( lefths[ i ] ) + " " );
		}

		bld.append( "-> " );

		for( int i = 0; i < righths.length; i++ ) {
			bld.append( tokenIndex.getToken( righths[ i ] ) + " " );
		}

		return bld.toString();
	}
}
