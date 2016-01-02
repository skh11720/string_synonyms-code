package mine;

import java.util.Comparator;

public class RecordIDComparator implements Comparator<Record> {
  @Override
  public int compare(Record o1, Record o2) {
    return Integer.compare(o1.getID(), o2.getID());
  }
}
