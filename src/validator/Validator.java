package validator;

import mine.Record;

public abstract class Validator {
  public static int checked;
  public static int niterentry;
  public static int niterrules;
  public static int nitermatches;
  public static int nitertokens;
  public static int earlyevaled;
  public static int earlystopped;
  public static int filtered;

  public abstract int isEqual(Record x, Record y);

  protected static boolean areSameString(Record s, Record t) {
    if (s.size() != t.size()) return false;
    int[] si = s.getTokenArray();
    int[] ti = t.getTokenArray();
    for (int i = 0; i < s.size(); ++i)
      if (si[i] != ti[i]) return false;
    return true;
  }
}
