package tools;

import java.util.Arrays;
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

		String[] tpstr = null;
		try {
			tpstr = pstr[ 1 ].trim().split( " " );
		}
		catch( Exception e ) {
			e.printStackTrace();
			System.out.println( str );
			System.exit( 1 );
		}

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
}
