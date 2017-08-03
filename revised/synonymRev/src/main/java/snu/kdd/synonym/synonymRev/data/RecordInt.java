package snu.kdd.synonym.synonymRev.data;

public class RecordInt {
	public Record record;
	public int index;

	public RecordInt( Record r, int m ) {
		this.record = r;
		this.index = m;
	}

	@Override
	public int hashCode() {
		return record.hashCode() + this.index;
	}
}
