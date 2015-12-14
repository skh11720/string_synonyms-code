package tools;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import mine.Record;

public class StaticFunctions {
  /**
   * Compare two integer array
   */
  public static int compare(int[] str1, int[] str2) {
    if (str1.length == 0 || str2.length == 0) return str1.length - str2.length;
    int idx = 0;
    int lastcmp = 0;
    while (idx < str1.length && idx < str2.length
        && (lastcmp = Integer.compare(str1[idx], str2[idx])) == 0)
      ++idx;
    if (lastcmp != 0)
      return lastcmp;
    else if (str1.length == str2.length)
      return 0;
    else if (idx == str1.length)
      return -1;
    else
      return 1;
  }

  /**
   * Check if a given pattern is a prefix of given string
   *
   * @param str
   *          The string
   * @param pattern
   *          The pattern
   */
  public static boolean isPrefix(int[] str, int[] pattern) {
    if (str.length < pattern.length) return false;
    for (int i = 0; i < pattern.length; ++i)
      if (str[i] != pattern[i]) return false;
    return true;
  }

  /**
   * Check if two intervals overlap or not
   */
  public static boolean overlap(int[] i1, int[] i2) {
    if (i1[0] > i2[1] || i1[1] < i2[0]) return false;
    return true;
  }

  /**
   * Copy a given string starts from given 'from' value
   *
   * @param src
   *          The source string
   * @param from
   *          The starting index
   */
  public static int[] copyIntegerArray(int[] src, int from) {
    int[] rslt = new int[src.length - from];
    for (int i = from; i < src.length; ++i)
      rslt[i - from] = src[i];
    return rslt;
  }

  private static class RecordIntTriple implements Comparable<RecordIntTriple> {
    Record rec;
    int    i1;
    int    i2;

    RecordIntTriple(Record rec, int i1, int i2) {
      this.rec = rec;
      this.i1 = i1;
      this.i2 = i2;
    }

    @Override
    public int compareTo(RecordIntTriple o) {
      return Integer.compare(rec.getID(), o.rec.getID());
    }
  }

  public static List<Record> union(List<? extends List<Record>> list) {
    // Merge candidates
    PriorityQueue<RecordIntTriple> pq = new PriorityQueue<RecordIntTriple>();
    for (int i = 0; i < list.size(); ++i) {
      List<Record> candidates = list.get(i);
      if (!candidates.isEmpty())
        pq.add(new RecordIntTriple(candidates.get(0), i, 0));
    }
    List<Record> candidates = new ArrayList<Record>();
    Record last = null;
    while (!pq.isEmpty()) {
      RecordIntTriple p = pq.poll();
      if (last == null || last.getID() != p.rec.getID()) {
        last = p.rec;
        candidates.add(p.rec);
      }
      List<Record> origin = list.get(p.i1);
      ++p.i2;
      if (origin.size() > p.i2) {
        p.rec = origin.get(p.i2);
        pq.add(p);
      }
    }
    return candidates;
  }

  public static List<Record> intersection(List<? extends List<Record>> list) {
    if(list.size() == 1) return list.get(0);
    PriorityQueue<RecordIntTriple> pq = new PriorityQueue<RecordIntTriple>();
    for (int i = 0; i < list.size(); ++i) {
      List<Record> candidates = list.get(i);
      if (!candidates.isEmpty())
        pq.add(new RecordIntTriple(candidates.get(0), i, 0));
    }
    List<Record> candidates = new ArrayList<Record>();
    Record last = null;
    int count = 0;
    while (!pq.isEmpty()) {
      RecordIntTriple p = pq.poll();
      if (last == null || last.getID() != p.rec.getID()) {
        last = p.rec;
        count = 1;
      } else if (++count == list.size()) candidates.add(last);
      List<Record> origin = list.get(p.i1);
      ++p.i2;
      if (origin.size() > p.i2) {
        p.rec = origin.get(p.i2);
        pq.add(p);
      }
    }
    return candidates;
  }

  /**
   * Merge two sorted ArrrayLists
   */
  public static List<Record> union(ArrayList<Record> al1,
      ArrayList<Record> al2) {
    ArrayList<Record> rslt = new ArrayList<Record>();
    int idx1 = 0, idx2 = 0;
    while (idx1 < al1.size() && idx2 < al2.size()) {
      Record r1 = al1.get(idx1);
      Record r2 = al2.get(idx2);
      int cmp = r1.compareTo(r2);
      if (cmp == 0) {
        rslt.add(r1);
        ++idx1;
        ++idx2;
      } else if (cmp < 0) {
        rslt.add(r1);
        ++idx1;
      } else {
        rslt.add(r2);
        ++idx2;
      }
    }
    while (idx1 < al1.size())
      rslt.add(al1.get(idx1++));
    while (idx2 < al2.size())
      rslt.add(al2.get(idx2++));
    return rslt;
  }

  /**
   * Do intersection between two sorted ArrrayLists
   */
  public static List<Record> intersection(ArrayList<Record> al1,
      ArrayList<Record> al2) {
    ArrayList<Record> rslt = new ArrayList<Record>();
    int idx1 = 0, idx2 = 0;
    while (idx1 < al1.size() && idx2 < al2.size()) {
      Record r1 = al1.get(idx1);
      Record r2 = al2.get(idx2);
      int cmp = r1.compareTo(r2);
      if (cmp == 0) {
        rslt.add(r1);
        ++idx1;
        ++idx2;
      } else if (cmp < 0)
        ++idx1;
      else
        ++idx2;
    }
    return rslt;
  }
}
