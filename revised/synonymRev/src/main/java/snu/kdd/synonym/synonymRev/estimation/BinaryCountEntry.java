package snu.kdd.synonym.synonymRev.estimation;

public class BinaryCountEntry {
	public int smallListSize;
	public int largeListSize;

	public BinaryCountEntry() {
		smallListSize = 0;
		largeListSize = 0;
	}

	public BinaryCountEntry( int small, int large ) {
		smallListSize = small;
		largeListSize = large;
	}

	public void increaseLarge() {
		largeListSize++;
	}

	public void increaseSmall() {
		smallListSize++;
	}

	public void fromLargeToSmall() {
		largeListSize--;
		smallListSize++;
	}
}
