package mine;

public class RecordPair implements Comparable<RecordPair> {
  public Record record1;
  public Record record2;

  RecordPair(Record rec1, Record rec2) {
    record1 = rec1;
    record2 = rec2;
  }

  @Override
  public int compareTo(RecordPair o) {
    int cmp = record1.compareTo(o.record1);
    if (cmp != 0) return cmp;
    return record2.compareTo(o.record2);
  }

  @Override
  public String toString() {
    return record1.toString() + "," + record2.toString();
  }
}
