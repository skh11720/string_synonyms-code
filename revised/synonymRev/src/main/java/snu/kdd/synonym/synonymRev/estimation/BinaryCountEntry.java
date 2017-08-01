package snu.kdd.synonym.synonymRev.estimation;

public class BinaryCountEntry {
	public int smallListSize;
	public int largeListSize;

	BinaryCountEntry() {
		smallListSize = 0;
		largeListSize = 0;
	}

	BinaryCountEntry( int small, int large ) {
		smallListSize = small;
		largeListSize = large;
	}

	void increaseLarge() {
		largeListSize++;
	}

	void increaseSmall() {
		smallListSize++;
	}

	void fromLargeToSmall() {
		largeListSize--;
		smallListSize++;
	}
}
