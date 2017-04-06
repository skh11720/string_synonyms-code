package tools;

import mine.Record;

public class IntIntRecordTriple {
	public int min;
	public int max;
	public Record rec;

	public IntIntRecordTriple( int min, int max, Record rec ) {
		this.min = min;
		this.max = max;
		this.rec = rec;
	}

	@Override
	public String toString() {
		return this.min + " " + this.max + " " + rec.getID();
	}
}
