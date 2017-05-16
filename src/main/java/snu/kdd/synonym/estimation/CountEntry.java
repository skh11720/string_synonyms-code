package snu.kdd.synonym.estimation;

public class CountEntry {
	public int count[];
	public int total;

	public static int countMax = 4;

	public CountEntry() {
		// 0 : 1 ~ 10
		// 1 : 11 ~ 100
		// 2 : 101 ~ infinity
		count = new int[ countMax ];
	}

	public void increase( long exp ) {
		count[ getIndex( exp ) ]++;
		total++;
	}

	public static int getIndex( long number ) {
		int powerOf10 = 10;

		for( int i = 0; i < countMax - 1; i++ ) {
			if( number <= powerOf10 ) {
				return i;
			}
			powerOf10 *= 10;
		}
		return countMax - 1;
	}

}
