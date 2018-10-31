package snu.kdd.synonym.synonymRev.tools;

import it.unimi.dsi.fastutil.longs.LongArrayList;

public class Histogram {
	private final LongArrayList list;
	private final String name;

	private long maxValue = Long.MIN_VALUE;
	private long minValue = Long.MAX_VALUE;
	private long totalValue = 0;

	public Histogram( String name ) {
		this.name = name;
		list = new LongArrayList();
	}

	public String getName() {
		return name;
	}

	public Histogram( String name, int initialSize ) {
		this.name = name;
		list = new LongArrayList( initialSize );
	}

	public void add( long value ) {
		list.add( value );
		totalValue += value;

		if( maxValue < value ) {
			maxValue = value;
		}
		if( minValue > value ) {
			minValue = value;
		}
	}

	public void printZero() {
		long count = 0;
		for( int i = 0; i < list.size(); i++ ) {
			if( list.getLong( i ) == 0 ) {
				count++;
			}
		}
		Util.printLog( "Zero: " + count );
	}

	public void print() {
		Util.printLog( "Histogram: " + name );

		if( list.size() == 0 ) {
			Util.printLog( "Nothing is added" );
		}
		else {
			Util.printLog( "Total: " + totalValue + " avg: " + ( (double) totalValue / list.size() ) );
			Util.printLog( "Min: " + minValue + ", Max: " + maxValue );
		}
	}

	public int[] getPositiveLogStat() {
		if( list.size() == 0 ) {
			return null;
		}

		int exponent = -1;
		while( maxValue != 0 ) {
			maxValue = maxValue / 10;
			exponent++;
		}

		int[] histogram = new int[ exponent + 2 ];

		int zero = 0;

		for( int i = 0; i < list.size(); i++ ) {
			long v = list.getLong( i );

			if( v == 0 ) {
				zero++;
			}
			else {
				int exp = -1;
				while( v != 0 ) {
					v = v / 10;
					exp++;
				}

				if( list.getLong( i ) > 0 ) {
					histogram[ exp + 1 ]++;
				}
			}
		}
		histogram[ 0 ] = zero;

		return histogram;
	}

	public void printLogHistogram() {

		if( list.size() == 0 ) {
			return;
		}

		int exponent = -1;
		while( maxValue != 0 ) {
			maxValue = maxValue / 10;
			exponent++;
		}

		int negExponent = -1;
		if( minValue < 0 ) {
			while( minValue != 0 ) {
				minValue = minValue / 10;
				negExponent++;
			}
		}

		int[] histogram = new int[ exponent + 1 ];
		int[] negHistogram = new int[ negExponent + 1 ];

		int zero = 0;

		for( int i = 0; i < list.size(); i++ ) {
			long v = list.getLong( i );

			if( v == 0 ) {
				zero++;
			}
			else {
				int exp = -1;
				while( v != 0 ) {
					v = v / 10;
					exp++;
				}

				if( list.getLong( i ) > 0 ) {
					histogram[ exp ]++;
				}
				else {
					negHistogram[ exp ]++;
				}
			}
		}

		if( minValue < 0 ) {
			for( int i = 0; i <= negExponent; i++ ) {
				Util.printLog( String.format( "hist[%d](%1.0e ~ %1.0e): %d", i, -Math.pow( 10, i ), -Math.pow( 10, i + 1 ),
						negHistogram[ i ] ) );
			}
		}

		Util.printLog( "zero                  : " + zero );

		for( int i = 0; i <= exponent; i++ ) {
			Util.printLog(
					String.format( "hist[%d](%1.0e ~ %1.0e): %d", i, Math.pow( 10, i ), Math.pow( 10, i + 1 ), histogram[ i ] ) );
		}
	}

}
