package validator;

import java.util.HashMap;

import mine.Record;
import tools.Rule;

public class TopDownMatrix_DS extends Validator {
  private static final Submatch EQUALMATCH = new Submatch(null, 0);

  private static class Submatch {
    Rule rule;
    int  idx;
    int  hash;

    Submatch(Rule rule, int idx) {
      this.rule = rule;
      this.idx = idx;
      if (rule != null) {
        int[] s = rule.getTo();
        for (int i = 0; i < idx; ++i)
          hash += s[i];
        hash += idx;
      }
    }

    Submatch(Submatch o) {
      this.rule = o.rule;
      this.idx = o.idx;
    }

    @Override
    public boolean equals(Object o) {
      if (o.getClass() != Submatch.class) return false;
      Submatch os = (Submatch) o;
      if (idx != os.idx)
        return false;
      else if (hash != os.hash) return false;
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
  }

  /**
   * Temporary matrix to save match result of doubleside equivalence check.</br>
   * dsmatrix[i][j] stores all sub-matches for s[1..i]=>x1,
   * t[1..j]=>x2 where x1 is sub/superstring of x2 and |x1|-|x2|<=0 </br>
   * Every rule applied to s must use this matrix to retrieve previous
   * matches.</br>
   * This matrix will be used to equivalence check with top-down manner.
   */
  @SuppressWarnings({ "unchecked" })
  private static HashMap<Submatch, Boolean>[][] Mx = new HashMap[1][0];
  /**
   * Temporary matrix to save match result of doubleside equivalence check.</br>
   * dtmatrix[i][j] stores all sub-matches for s[1..i]=>x1,
   * t[1..j]=>x2 where x1 is sub/superstring of x2 and |x1|-|x2|>0
   * Every rule applied to t must use this matrix to retrieve previous
   * matches.</br>
   * This matrix will be used to equivalence check with top-down manner.
   */
  @SuppressWarnings("unchecked")
  private static HashMap<Submatch, Boolean>[][] My = new HashMap[1][0];

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private static void enlargeDSTDMatrix(int slen, int tlen) {
    if (slen >= Mx.length || tlen >= Mx[0].length) {
      int rows = Math.max(slen + 1, Mx.length);
      int cols = Math.max(tlen + 1, Mx[0].length);
      Mx = new HashMap[rows][cols];
      My = new HashMap[rows][cols];
      for (int i = 0; i < Mx.length; ++i)
        for (int j = 0; j < Mx[0].length; ++j) {
          Mx[i][j] = new HashMap();
          My[i][j] = new HashMap();
        }
      My[0][0].put(EQUALMATCH, true);
    }
  }

  public int isEqual(Record x, Record y) {
    ++checked;
    if (areSameString(x, y)) return 0;
    /**
     * If temporary matrix size is not enough, enlarge the space
     */
    enlargeDSTDMatrix(x.size(), y.size());
    clearMatrix(x, y);
    assert (My[0][0].size() == 1);
    boolean isEqual = getMyEqual(x, y, x.size(), y.size());
    if (isEqual)
      return 1;
    else
      return -1;
  }

  private static void clearMatrix(Record x, Record y) {
    for (int i = 0; i <= x.size(); ++i)
      for (int j = 0; j <= y.size(); ++j) {
        Mx[i][j].clear();
        if (i == 0 && j == 0) continue;
        My[i][j].clear();
      }
  }

  /**
   * Get the value of M_x[i][j][remain].<br/>
   * If the value is not computed, compute and then return the value.
   */
  private static boolean getMx(Record x, Record y, int i, int j,
      Submatch remain) {
    ++recursivecalls;
    // If this value is already computed, simply return the computed value.
    Boolean rslt = Mx[i][j].get(remain);
    if (rslt != null) return rslt;
    // Check every rule which is applicable to a suffix of x[1..i]
    ++niterentry;
    if (i == 0) return false;
    Rule[] rules = x.getSuffixApplicableRules(i - 1);
    assert (rules != null);
    assert (rules.length != 0);
    int[] str = remain.rule.getTo();
    for (int ridx = 0; ridx < rules.length; ++ridx) {
      Rule rule = rules[ridx];
      ++niterrules;
      int[] rhs = rule.getTo();
      int lhslength = rule.getFrom().length;
      int rhsidx = rhs.length - 1;
      int remainidx = remain.idx - 1;
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
      if (rhs.length < remain.idx) {
        assert (remainidx >= 0);
        assert (rhsidx == -1);
        Submatch prevmatch = new Submatch(remain);
        prevmatch.idx -= rhs.length;
        // Retrieve Mx[i-|r.lhs|][j][remain - r.rhs]
        prevM = getMx(x, y, i - lhslength, j, prevmatch);
      }
      // remain is shorter than r.rhs
      else if (rhs.length > remain.idx) {
        assert (remainidx == -1);
        assert (rhsidx >= 0);
        Submatch prevmatch = new Submatch(rule, rhs.length - remain.idx);
        // Retrieve My[i-|r.lhs|][j][remain - r.rhs]
        prevM = getMy(x, y, i - lhslength, j, prevmatch);
      }
      // r.rhs == remain
      else {
        assert (remainidx == -1);
        assert (rhsidx == -1);
        prevM = getMyEqual(x, y, i - lhslength, j);
      }
      // If there exists a valid match, return true
      if (prevM) {
        Mx[i][j].put(remain, true);
        return true;
      }
    }
    // If there is no match, return false
    Mx[i][j].put(remain, false);
    return false;
  }

  /**
   * Get the value of M_y[i][j][remain].<br/>
   * If the value is not computed, compute and then return the value.
   */
  private static boolean getMy(Record x, Record y, int i, int j,
      Submatch remain) {
    ++recursivecalls;
    // If this value is already computed, simply return the computed value.
    Boolean rslt = My[i][j].get(remain);
    if (rslt != null) return rslt;
    // Check every rule which is applicable to a suffix of y[1..j]
    ++niterentry;
    if (j == 0) return false;
    Rule[] rules = y.getSuffixApplicableRules(j - 1);
    assert (rules != null);
    assert (rules.length != 0);
    int[] str = remain.rule.getTo();
    for (int ridx = 0; ridx < rules.length; ++ridx) {
      Rule rule = rules[ridx];
      ++niterrules;
      int[] rhs = rule.getTo();
      int lhslength = rule.getFrom().length;
      int rhsidx = rhs.length - 1;
      int remainidx = remain.idx - 1;
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
      if (rhs.length < remain.idx) {
        assert (remainidx >= 0);
        assert (rhsidx == -1);
        Submatch prevmatch = new Submatch(remain);
        prevmatch.idx -= rhs.length;
        // Retrieve My[i][j-|r.lhs|][remain - r.rhs]
        prevM = getMy(x, y, i, j - lhslength, prevmatch);
      }
      // remain is shorter than r.rhs
      else if (rhs.length > remain.idx) {
        assert (remainidx == -1);
        assert (rhsidx >= 0);
        Submatch prevmatch = new Submatch(rule, rhs.length - remain.idx);
        // Retrieve Mx[i][j-|r.lhs|][remain - r.rhs]
        prevM = getMx(x, y, i, j - lhslength, prevmatch);
      }
      // r.rhs == remain
      else {
        assert (remainidx == -1);
        assert (rhsidx == -1);
        prevM = getMyEqual(x, y, i, j - lhslength);
      }
      // If there exists a valid match, return true
      if (prevM) {
        My[i][j].put(remain, true);
        return true;
      }
    }
    // If there is no match, return false
    My[i][j].put(remain, false);
    return false;
  }

  /**
   * Get the value of M_x[i][j][""].<br/>
   * If the value is not computed, compute and then return the value.
   */
  private static boolean getMyEqual(Record x, Record y, int i, int j) {
    ++recursivecalls;
    // If this value is already computed, simply return the computed value.
    Boolean rslt = My[i][j].get(EQUALMATCH);
    if (rslt != null) return rslt;
    // Check every xule which is applicable to a suffix of y[1..j]
    ++niterentry;
    if (j == 0) return i == 0;
    Rule[] rules = y.getSuffixApplicableRules(j - 1);
    assert (rules != null);
    assert (rules.length != 0);
    for (int ridx = 0; ridx < rules.length; ++ridx) {
      Rule rule = rules[ridx];
      ++niterrules;
      boolean prevM = false;
      Submatch prevmatch = new Submatch(rule, rule.getTo().length);
      prevM = getMx(x, y, i, j - rule.getFrom().length, prevmatch);
      // If there exists a valid match, return true
      if (prevM) {
        My[i][j].put(EQUALMATCH, true);
        return true;
      }
    }
    // If there is no match, return false
    My[i][j].put(EQUALMATCH, false);
    return false;
  }
}
