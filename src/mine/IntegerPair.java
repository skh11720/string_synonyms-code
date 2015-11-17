package mine;

public class IntegerPair {
	int i1;
	int i2;

	IntegerPair(int i1, int i2) {
		this.i1 = i1;
		this.i2 = i2;
	}

	public boolean equals(Object o) {
		IntegerPair oip = (IntegerPair) o;
		return (i1 == oip.i1) && (i2 == oip.i2);
	}

	public int hashCode() {
		return i1 ^ 0x1f1f1f1f + i2;
	}
	
	public String toString() {
		return String.format("%d,%d", i1, i2);
	}
}
