package tools;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Rule implements Comparable<Rule> {
	private final int hash;
	int[] from;
	int[] to;

	public Rule( String str, Map<String, Integer> str2int ) {
		int hash = 0;
		String[] pstr = str.split( "," );
		String[] fpstr = pstr[ 0 ].trim().split( " " );
		from = new int[ fpstr.length ];
		for( int i = 0; i < fpstr.length; ++i ) {
			from[ i ] = str2int.get( fpstr[ i ] );
			hash = 0x1f1f1f1f ^ hash + from[ i ];
		}

		String[] tpstr = pstr[ 1 ].trim().split( " " );

		to = new int[ tpstr.length ];
		for( int i = 0; i < tpstr.length; ++i ) {
			to[ i ] = str2int.get( tpstr[ i ] );
			hash = 0x1f1f1f1f ^ hash + to[ i ];
		}
		this.hash = hash;
	}

	public Rule( int[] from, int[] to ) {
		int hash = 0;
		this.from = from;
		this.to = to;
		for( int i = 0; i < from.length; ++i )
			hash = 0x1f1f1f1f ^ hash + from[ i ];
		for( int i = 0; i < to.length; ++i )
			hash = 0x1f1f1f1f ^ hash + to[ i ];
		this.hash = hash;
	}

	public Rule( int from, int to ) {
		int hash = 0;
		this.from = new int[ 1 ];
		this.from[ 0 ] = from;
		hash = 0x1f1f1f1f ^ hash + this.from[ 0 ];
		this.to = new int[ 1 ];
		this.to[ 0 ] = to;
		hash = 0x1f1f1f1f ^ hash + this.to[ 0 ];
		this.hash = hash;
	}

	private static HashMap<Integer, Rule> selfRuleMap = new HashMap<Integer, Rule>();

	public static Rule getSelfRule( int item ) {
		Rule r = selfRuleMap.get( item );
		if( r == null ) {
			r = new Rule( item, item );
			selfRuleMap.put( item, r );
		}
		return r;
	}

	public int[] getFrom() {
		return from;
	}

	public int[] getTo() {
		return to;
	}

	public int fromSize() {
		return from.length;
	}

	public int toSize() {
		return to.length;
	}

	@Override
	public int compareTo( Rule o ) {
		return StaticFunctions.compare( from, o.from );
	}

	@Override
	public boolean equals( Object o ) {
		if( o == null ) {
			return false;
		}

		if( this == o )
			return true;
		Rule ro = (Rule) o;
		if( from.length == ro.from.length && to.length == ro.to.length ) {
			for( int i = 0; i < fromSize(); ++i )
				if( Integer.compare( from[ i ], ro.from[ i ] ) != 0 )
					return false;
			for( int i = 0; i < toSize(); ++i )
				if( Integer.compare( to[ i ], ro.to[ i ] ) != 0 )
					return false;
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
		return Arrays.toString( from ) + " -> " + Arrays.toString( to );
	}

	public String toTextString( List<String> strlist ) {
		StringBuilder bld = new StringBuilder();
		for( int i = 0; i < from.length; i++ ) {
			bld.append( strlist.get( from[ i ] ) + " " );
		}
		bld.append( "-> " );
		for( int i = 0; i < to.length; i++ ) {
			bld.append( strlist.get( to[ i ] ) + " " );
		}
		return bld.toString();
	}
}
