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

  public static void printStats() {
    System.out.println("Comparisons: " + Validator.checked);
    System.out.println("Total iter entries: " + Validator.niterentry);
    System.out.println("Total iter rules: " + Validator.niterrules);
    System.out.println("Total iter matches: " + Validator.nitermatches);
    System.out.println("Total iter tokens: " + Validator.nitertokens);
    System.out.println("Early evaled: " + Validator.earlyevaled);
    System.out.println("Early stopped: " + Validator.earlystopped);
    System.out.println("Length filtered: " + Validator.filtered);
  }
}
