package tools;

import java.util.List;

public class QGram {
	private static final int SHIFT_VAL = 314159;

	public int[] qgram;

	public QGram( int[] qgram ) {
		this.qgram = qgram;
	}

	@Override
	public boolean equals( Object o ) {
		QGram oip = (QGram) o;

		if( qgram.length == oip.qgram.length ) {
			for( int i = 0; i < qgram.length; i++ ) {
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

	@Override
	public int hashCode() {
		int hc = qgram.length;
		for( int i = 0; i < qgram.length; i++ ) {
			hc = hc * SHIFT_VAL + qgram[ i ];
		}
		return hc;
	}

	@Override
	public String toString() {
		String str = "";
		for( int i = 0; i < qgram.length; i++ ) {
			str += qgram[ i ] + " ";
		}

		return str;
	}

	public String toStrString( List<String> strlist ) {
		String str = "";
		for( int i = 0; i < qgram.length; i++ ) {
			str += strlist.get( qgram[ i ] ) + " ";
		}

		return str;
	}
}
