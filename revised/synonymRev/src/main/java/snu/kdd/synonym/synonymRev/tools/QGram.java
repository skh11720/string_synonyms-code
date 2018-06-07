package snu.kdd.synonym.synonymRev.tools;

import snu.kdd.synonym.synonymRev.data.TokenIndex;

public class QGram {
	/*
	 * non-negative integer: normal tokens
	 * Integer.MAX_VALUE: EOF
	 * -1: wildcard
	 */
	// private static final int SHIFT_VAL = 314159;
	private static final int SHIFT_VAL = 199807;
	// private static final int SHIFT_VAL = 314161;

	public final int hash;

	public final int[] qgram;

	public QGram( int[] qgram ) {
		this.qgram = qgram;

		int hc = qgram.length;
		for( int i = 0; i < qgram.length; i++ ) {
			hc = hc * SHIFT_VAL + qgram[ i ];
		}

		hash = Math.abs( hc );
	}

	@Override
	public boolean equals( Object o ) {
		if( o == null ) {
			return false;
		}

		QGram oip = (QGram) o;

		if( hashCode() != oip.hashCode() ) {
			return false;
		}

		if( qgram.length == oip.qgram.length ) {
			for( int i = 0; i < qgram.length; i++ ) {
				if ( qgram[i] == -1 || oip.qgram[i] == -1 ) continue; // wildcard
				if( qgram[ i ] != oip.qgram[ i ] ) {
					return false;
				}
			}
			return true;
		}
		else {
			return false;
		}
	}
	
	public boolean equals( int[] o ) {
		if (o == null ) return false;
		if (qgram.length == o.length) {
			for (int i=0; i<qgram.length; i++) {
				if ( qgram[i] == -1 || o[i] == -1 ) continue; // wildcard
				if ( qgram[i] != o[i] ) return false;
			}
			return true;
		}
		else return false;
	}

	@Override
	public int hashCode() {
		return hash;
	}

	@Override
	public String toString() {
		String str = "";
		for( int i = 0; i < qgram.length; i++ ) {
			str += qgram[ i ] + " ";
		}

		return str;
	}

	public String toString( TokenIndex tokenIndex ) {
		String str = "";
		for( int i = 0; i < qgram.length; i++ ) {
			int id = qgram[ i ];

			if( id == Integer.MAX_VALUE ) {
				str += "EOF ";
			}
			else if ( id == -1 ) {
				str += "* ";
			}
			else {
				str += tokenIndex.getToken( qgram[ i ] ) + " ";
			}
		}

		return str;
	}
}
