package snu.kdd.synonym.estimation;

public class CountEntry {
	public int count[];
	public int total;

	public CountEntry() {
		// 0 : 1 ~ 10
		// 1 : 11 ~ 100
		// 2 : 101 ~ infinity
		count = new int[ 3 ];
	}

	public void increase( long exp ) {
		count[ getIndex( exp ) ]++;
		total++;
	}

	private int getIndex( long number ) {
		if( number <= 10 ) {
			return 0;
		}
		else if( number <= 100 ) {
			return 1;
		}
		else {
			return 2;
		}
	}

}
