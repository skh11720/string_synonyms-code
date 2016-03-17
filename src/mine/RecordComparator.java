package mine;

import java.util.Comparator;

/**
 * Sort string with dictionary order
 */
public class RecordComparator implements Comparator<Record> {
  @Override
  public int compare(Record o1, Record o2) {
    int[] tokens1 = o1.tokens, tokens2 = o2.tokens;
    for (int idx = 0; idx < o1.size() && idx < o2.size(); ++idx) {
      int cmp = Integer.compare(tokens1[idx], tokens2[idx]);
      if (cmp != 0) return cmp;
    }
    return Integer.compare(tokens1.length, tokens2.length);
  }
}
