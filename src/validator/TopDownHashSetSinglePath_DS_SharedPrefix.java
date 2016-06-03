package validator;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import mine.Record;
import tools.Rule;

public class TopDownHashSetSinglePath_DS_SharedPrefix extends Validator {
  private static final Submatch BASIS     = new Submatch(0, 0, null, 0);
  private static long           timestamp = 0;

  /**
   * Represents a Submatch x[1..i] ==> z + str , y[1..j] ==> z
   * (or vice versa)
   */
  private static class Submatch {
    /**
     * Index of x
     */
    final int                i;
    /**
     * Index of y
     */
    final int                j;
    /**
     * Represents a substring rule.rhs[idx..*]
     */
    final Rule               rule;
    final int                idx;
    final int                hash;

    private static final int bigprime = 1645333507;

    Submatch(int i, int j, Rule rule, int idx) {
      // Set values
      this.i = i;
      this.j = j;
      this.rule = rule;
      this.idx = idx;

      // Compute hash value
      long tmp = 0;
      tmp = ((long) i) << 32 + j;
      tmp %= bigprime;
      tmp = (tmp << 32) + idx;
      tmp %= bigprime;
      if (rule != null) {
        int[] s = rule.getTo();
        for (int k = 0; k < idx; ++k) {
          tmp = (tmp << 32) + s[k];
          tmp %= bigprime;
        }
      }

      // Set hash value
      this.hash = (int) (tmp % Integer.MAX_VALUE);
    }

    @Override
    public boolean equals(Object o) {
      if (o.getClass() != Submatch.class) return false;
      Submatch os = (Submatch) o;
      if (hash != os.hash)
        return false;
      else if (i != os.i || j != os.j) return false;
      if (rule == null) {
        if (os.rule == null)
          return true;
        else
          return false;
      } else if (os.rule == null)
        return false;
      else if (idx != os.idx) return false;
      int[] s1 = rule.getTo();
      int[] s2 = os.rule.getTo();
      for (int i = 0; i < idx; ++i)
        if (s1[i] != s2[i]) return false;
      return true;
    }

    @Override
    public int hashCode() {
      return hash;
    }

    @Override
    public String toString() {
      if (rule != null)
        return String.format("%d:%d:%s:%d", i, j, rule.toString(), idx);
      else
        return String.format("%d:%d:null:0", i, j);
    }
  }

  private static class Mark {
    final long    timestamp;
    final boolean b;

    Mark(boolean b) {
      this.timestamp = TopDownHashSetSinglePath_DS_SharedPrefix.timestamp;
      this.b = b;
    }
  }

  private static final boolean       countEffectivePrevEntry = true;
  public static long                 effectivePrevEntryCount = 0;
  public static long                 prevEntryCount          = 0;

  /**
   * Temporary matrix to save match result of doubleside equivalence check.</br>
   * Mx[i][j] stores all sub-matches for x[1..i]=>z + str and y[1..j]=>z </br>
   */
  private static Map<Submatch, Mark> Mx                      = new HashMap<Submatch, Mark>();
  private static Map<Submatch, Mark> prevMx                  = new HashMap<Submatch, Mark>();
  /**
   * Temporary matrix to save match result of doubleside equivalence check.</br>
   * My[i][j] stores all sub-matches for x[1..i]=>z and y[1..j]=>z + str</br>
   */
  private static Map<Submatch, Mark> My                      = new HashMap<Submatch, Mark>();
  private static Map<Submatch, Mark> prevMy                  = new HashMap<Submatch, Mark>();

  private static Record              prevX                   = null;
  private static Record              prevY                   = null;

  public int isEqual(Record x, Record y) {
    ++checked;
    timestamp = System.nanoTime();
    /**
     * If two strings are exactly the same, return true;
     */
    if (areSameString(x, y)) return 0;

    /**
     * Compute usableBoundIdx
     */
    copyUsableSubmatches(x, y);

    /**
     * Check if two strings are equivalent
     */
    Submatch basis = new Submatch(x.size(), y.size(), null, 0);
    boolean isEqual = getMyEqual(x, y, basis);

    /**
     * Change prevMx and prevMy with Mx and My
     */
    cleanup();

    /**
     * Return the result;
     */
    if (isEqual)
      return 1;
    else
      return -1;
  }

  private static int usableBoundIdx = 0;

  private static void copyUsableSubmatches(Record x, Record y) {
    if (y == prevY) {
      // Compute the maximum index which can be used
      int[] tokensX = x.getTokenArray();
      int[] tokensPrevX = prevX.getTokenArray();
      int bound = Math.min(tokensX.length, tokensPrevX.length);
      usableBoundIdx = 0;
      for (; usableBoundIdx < bound; ++usableBoundIdx) {
        if (tokensX[usableBoundIdx] != tokensPrevX[usableBoundIdx]) break;
      }

      // Retrieve all submatches which can be utilized
      for (Entry<Submatch, Mark> e : prevMx.entrySet()) {
        Submatch m = e.getKey();
        if (m.i <= usableBoundIdx) {
          Mx.put(m, e.getValue());
          ++prevEntryCount;
        }
      }
      for (Entry<Submatch, Mark> e : prevMy.entrySet()) {
        Submatch m = e.getKey();
        if (m.i <= usableBoundIdx) {
          My.put(m, e.getValue());
          ++prevEntryCount;
        }
      }
    }
    prevX = x;
    prevY = y;
  }

  private static void cleanup() {
    Map<Submatch, Mark> tmp = prevMx;
    prevMx = Mx;
    Mx = tmp;
    Mx.clear();
    tmp = prevMy;
    prevMy = My;
    My = tmp;
    My.clear();
    My.put(BASIS, new Mark(true));
  }

  /**
   * Get the value of M_x[i][j][remain].<br/>
   * If the value is not computed, compute and then return the value.
   */
  private static boolean getMx(Record x, Record y, Submatch match) {
    ++recursivecalls;
    // If this value is already computed, simply return the computed value.
    Mark rslt = Mx.get(match);
    if (rslt != null) {
      if (countEffectivePrevEntry && rslt.timestamp < timestamp)
        ++effectivePrevEntryCount;
      return rslt.b;
    }
    // Check every rule which is applicable to a suffix of x[1..i]
    ++niterentry;
    if (match.i == 0) return false;
    Rule[] rules = x.getSuffixApplicableRules(match.i - 1);
    assert (rules != null);
    assert (rules.length != 0);
    int[] str = match.rule.getTo();
    for (int ridx = 0; ridx < rules.length; ++ridx) {
      Rule rule = rules[ridx];
      ++niterrules;
      int[] rhs = rule.getTo();
      int lhslength = rule.getFrom().length;
      int rhsidx = rhs.length - 1;
      int remainidx = match.idx - 1;
      // Check if one is a suffix of another
      boolean isSuffix = true;
      while (isSuffix && rhsidx >= 0 && remainidx >= 0) {
        if (rhs[rhsidx] != str[remainidx]) isSuffix = false;
        --rhsidx;
        --remainidx;
      }
      // If r.rhs is not a suffix of remain (or vice versa), skip using this
      // rule
      if (!isSuffix) continue;
      boolean prevM = false;
      // r.rhs is shorter than remain
      if (rhs.length < match.idx) {
        assert (remainidx >= 0);
        assert (rhsidx == -1);
        Submatch prevmatch = new Submatch(match.i - lhslength, match.j,
            match.rule, match.idx - rhs.length);
        // Retrieve Mx[i-|r.lhs|][j][remain - r.rhs]
        prevM = getMx(x, y, prevmatch);
      }
      // remain is shorter than r.rhs
      else if (rhs.length > match.idx) {
        assert (remainidx == -1);
        assert (rhsidx >= 0);
        Submatch prevmatch = new Submatch(match.i - lhslength, match.j, rule,
            rhs.length - match.idx);
        // Retrieve My[i-|r.lhs|][j][remain - r.rhs]
        prevM = getMy(x, y, prevmatch);
      }
      // r.rhs == remain
      else {
        assert (remainidx == -1);
        assert (rhsidx == -1);
        Submatch prevmatch = new Submatch(match.i - lhslength, match.j, null,
            0);
        prevM = getMyEqual(x, y, prevmatch);
      }
      // If there exists a valid match, return true
      if (prevM) {
        Mx.put(match, new Mark(true));
        return true;
      }
    }
    // If there is no match, return false
    Mx.put(match, new Mark(false));
    return false;
  }

  /**
   * Get the value of M_y[i][j][remain].<br/>
   * If the value is not computed, compute and then return the value.
   */
  private static boolean getMy(Record x, Record y, Submatch match) {
    ++recursivecalls;
    // Every exact match is handled by getMyEqual(x, y, match).
    assert (match.rule != null);
    // If this value is already computed, simply return the computed value.
    Mark rslt = My.get(match);
    if (rslt != null) {
      if (countEffectivePrevEntry && rslt.timestamp < timestamp)
        ++effectivePrevEntryCount;
      return rslt.b;
    }
    // Check every rule which is applicable to a suffix of y[1..j]
    ++niterentry;
    if (match.j == 0) return false;
    Rule[] rules = y.getSuffixApplicableRules(match.j - 1);
    assert (rules != null);
    assert (rules.length != 0);
    int[] str = match.rule.getTo();
    for (int ridx = 0; ridx < rules.length; ++ridx) {
      Rule rule = rules[ridx];
      ++niterrules;
      int[] rhs = rule.getTo();
      int lhslength = rule.getFrom().length;
      int rhsidx = rhs.length - 1;
      int remainidx = match.idx - 1;
      // Check if one is a suffix of another
      boolean isSuffix = true;
      while (isSuffix && rhsidx >= 0 && remainidx >= 0) {
        if (rhs[rhsidx] != str[remainidx]) isSuffix = false;
        --rhsidx;
        --remainidx;
      }
      // If r.rhs is not a suffix of remain (or vice versa), skip using this
      // rule
      if (!isSuffix) continue;
      boolean prevM = false;
      // r.rhs is shorter than remain
      if (rhs.length < match.idx) {
        assert (remainidx >= 0);
        assert (rhsidx == -1);
        Submatch prevmatch = new Submatch(match.i, match.j - lhslength,
            match.rule, match.idx - rhs.length);
        // Retrieve My[i][j-|r.lhs|][remain - r.rhs]
        prevM = getMy(x, y, prevmatch);
      }
      // remain is shorter than r.rhs
      else if (rhs.length > match.idx) {
        assert (remainidx == -1);
        assert (rhsidx >= 0);
        Submatch prevmatch = new Submatch(match.i, match.j - lhslength, rule,
            rhs.length - match.idx);
        // Retrieve Mx[i][j-|r.lhs|][remain - r.rhs]
        prevM = getMx(x, y, prevmatch);
      }
      // r.rhs == remain
      else {
        assert (remainidx == -1);
        assert (rhsidx == -1);
        Submatch prevmatch = new Submatch(match.i, match.j - lhslength, null,
            0);
        prevM = getMyEqual(x, y, prevmatch);
      }
      // If there exists a valid match, return true
      if (prevM) {
        My.put(match, new Mark(true));
        return true;
      }
    }
    // If there is no match, return false
    My.put(match, new Mark(false));
    return false;
  }

  /**
   * Get the value of M_x[i][j][""].<br/>
   * If the value is not computed, compute and then return the value.
   */
  private static boolean getMyEqual(Record x, Record y, Submatch match) {
    ++recursivecalls;
    // If this value is already computed, simply return the computed value.
    Mark rslt = My.get(match);
    if (rslt != null) {
      if (countEffectivePrevEntry && rslt.timestamp < timestamp)
        ++effectivePrevEntryCount;
      return rslt.b;
    }
    // Check every xule which is applicable to a suffix of y[1..j]
    ++niterentry;
    if (match.j == 0) return match.i == 0;
    Rule[] rules = y.getSuffixApplicableRules(match.j - 1);
    assert (rules != null);
    assert (rules.length != 0);
    for (int ridx = 0; ridx < rules.length; ++ridx) {
      Rule rule = rules[ridx];
      ++niterrules;
      boolean prevM = false;
      Submatch prevmatch = new Submatch(match.i,
          match.j - rule.getFrom().length, rule, rule.getTo().length);
      prevM = getMx(x, y, prevmatch);
      // If there exists a valid match, return true
      if (prevM) {
        My.put(match, new Mark(true));
        return true;
      }
    }
    // If there is no match, return false
    My.put(match, new Mark(false));
    return false;
  }
}
