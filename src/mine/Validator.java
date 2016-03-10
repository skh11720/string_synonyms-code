package mine;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import tools.Rule;
import tools.StaticFunctions;
import tools.WYK_HashSet;

public class Validator {
  public static long filtered     = 0;
  public static long niterentry   = 0;
  public static long checked      = 0;
  public static long niterrules   = 0;
  public static long nitermatches = 0;

  /**
   * @param difflen
   *          Residual string length : <br/>
   *          (modified r string length) - (modified t string length)
   * @param r
   *          Record r
   * @param idx1
   *          Last matched index of r
   * @param t
   *          Record t
   * @param idx2
   *          Last matched index of t
   * @return
   */
  public static boolean checkLengthFilter(int difflen, Record r, int idx1,
      Record t, int idx2) {
    int[] rCandidateLengths = r.getCandidateLengths(r.size() - 1).clone();
    int[] tCandidateLengths = t.getCandidateLengths(t.size() - 1).clone();
    rCandidateLengths[0] -= r.getCandidateLengths(idx1)[0];
    rCandidateLengths[1] -= r.getCandidateLengths(idx1)[1];
    tCandidateLengths[0] -= t.getCandidateLengths(idx2)[0];
    tCandidateLengths[1] -= t.getCandidateLengths(idx2)[1];
    if (difflen > 0) {
      rCandidateLengths[0] += difflen;
      rCandidateLengths[1] += difflen;
    } else if (difflen < 0) {
      tCandidateLengths[0] -= difflen;
      tCandidateLengths[1] -= difflen;
    }
    boolean rslt = StaticFunctions.overlap(rCandidateLengths,
        tCandidateLengths);
    if (!rslt) filtered++;
    return rslt;
  }

  /**
   * Returns the number of applied rules if matched. -1 otherwise.
   */
  public static int DP_A_Queue(Record r, Record t, boolean UseLengthFilter) {
    // Increase counter
    ++checked;
    // Check if interval of two given strings overlap or not
    if (UseLengthFilter) {
      int[] rCandidateLengths = r.getCandidateLengths(r.size() - 1);
      int[] tCandidateLengths = t.getCandidateLengths(t.size() - 1);
      if (!StaticFunctions.overlap(rCandidateLengths, tCandidateLengths))
        return -1;
    }

    // Initialize queue
    Queue<QueueEntry> queue = new LinkedList<QueueEntry>();
    // Initialize a hashset for storing discovered entries
    WYK_HashSet<QueueEntry> discovered = new WYK_HashSet<QueueEntry>(100);

    // Variable for next candidate entry
    QueueEntry next;

    // Create initial matching results
    queue.add(new QueueEntry(-1, -1, null, 0, null));

    // Begin
    while (!queue.isEmpty()) {
      ++niterentry;
      QueueEntry qe = queue.poll();
      int i = qe.idx1;
      int j = qe.idx2;
      int difflen = 0;
      // Both r[0..i] and t[0..j] can be sub-matched into qe.matched
      // Case l=0
      if (qe.type == 0)
        for (Rule rule1 : r.getApplicableRules(i + 1)) {
          // If this rule is applied, r[0...idx1] will be sub-matched
          int idx1 = i + rule1.fromSize();
          for (Rule rule2 : t.getApplicableRules(j + 1)) {
            ++niterrules;
            next = null;
            // If this rule is applied, t[0...idx2] will be
            // sub-matched
            int idx2 = j + rule2.fromSize();
            // Calculate the length difference of two transformed
            // strings
            difflen = rule1.toSize() - rule2.toSize();
            // Case b=d
            if (difflen == 0
                && StaticFunctions.compare(rule1.getTo(), rule2.getTo()) == 0) {
              next = new QueueEntry(idx1, idx2, qe, 0, null);
              if (rule1.fromSize() == 1
                  && rule1.getFrom()[0] == rule2.getFrom()[0])
                ;
              else
                next.appliedRules = qe.appliedRules + 1;
            }
            // Case d is a prefix of b
            else if (difflen > 0
                && StaticFunctions.isPrefix(rule1.getTo(), rule2.getTo())) {
              int[] residual = StaticFunctions.copyIntegerArray(rule1.getTo(),
                  rule2.toSize());
              next = new QueueEntry(idx1, idx2, qe, 1, residual);
            }
            // Case b is a prefix of d
            else if (difflen < 0
                && StaticFunctions.isPrefix(rule2.getTo(), rule1.getTo())) {
              int[] residual = StaticFunctions.copyIntegerArray(rule2.getTo(),
                  rule1.toSize());
              next = new QueueEntry(idx1, idx2, qe, 2, residual);
            }

            // Check if there exists a sub-match
            if (next == null)
              continue;
            // Check if the next sub-match can be used further
            else if (UseLengthFilter && !Validator.checkLengthFilter(difflen, r,
                next.idx1, t, next.idx2))
              continue;
            // Check if we found an answer
            else if (next.idx1 == (r.size() - 1) && next.idx2 == (t.size() - 1)
                && next.type == 0)
              return next.appliedRules;
            // Check if the next sub-match is already discovered
            else if (!discovered.contains(next)) {
              queue.add(next);
              discovered.add(next);
            }
          }
        }
      // Case l=1
      else if (qe.type == 1)
        for (Rule rule : t.getApplicableRules(j + 1)) {
          ++niterrules;
          next = null;
          // If this rule is applied, t[0...idx2] will be sub-matched
          int idx2 = j + rule.fromSize();
          // Calculate the length difference of two transformed
          // strings
          difflen = qe.residual.length - rule.toSize();
          // Case e=b
          if (difflen == 0
              && StaticFunctions.compare(qe.residual, rule.getTo()) == 0) {
            next = new QueueEntry(i, idx2, qe, 0, null);
          }
          // Case b is a prefix of e
          else if (difflen > 0
              && StaticFunctions.isPrefix(qe.residual, rule.getTo())) {
            int[] residual = StaticFunctions.copyIntegerArray(qe.residual,
                rule.toSize());
            next = new QueueEntry(i, idx2, qe, 1, residual);
          }
          // Case e is a prefix of b
          else if (difflen < 0
              && StaticFunctions.isPrefix(rule.getTo(), qe.residual)) {
            int[] residual = StaticFunctions.copyIntegerArray(rule.getTo(),
                qe.residual.length);
            next = new QueueEntry(i, idx2, qe, 2, residual);
          }

          // Check if there exists a sub-match
          if (next == null)
            continue;
          // Check if the next sub-match can be used further
          else if (UseLengthFilter && !Validator.checkLengthFilter(difflen, r,
              next.idx1, t, next.idx2))
            continue;
          if (rule.fromSize() == 1 && rule.toSize() == 1
              && rule.getFrom()[0] == rule.getTo()[0])
            ;
          else
            next.appliedRules = qe.appliedRules + 1;
          // Check if we found an answer
          if (next.idx1 == (r.size() - 1) && next.idx2 == (t.size() - 1)
              && next.type == 0)
            return next.appliedRules;
          // Check if the next sub-match is already discovered
          else if (!discovered.contains(next)) {
            queue.add(next);
            discovered.add(next);
          }
        }
      // Case l=2
      else
        for (Rule rule : r.getApplicableRules(i + 1)) {
          ++niterrules;
          next = null;
          // If this rule is applied, r[0...idx1] will be sub-matched
          int idx1 = i + rule.fromSize();
          // Calculate the length difference of two transformed
          // strings
          difflen = rule.toSize() - qe.residual.length;
          // Case e=b
          if (difflen == 0
              && StaticFunctions.compare(qe.residual, rule.getTo()) == 0) {
            next = new QueueEntry(idx1, j, qe, 0, null);
          }
          // Case b is a prefix of e
          else if (difflen < 0
              && StaticFunctions.isPrefix(qe.residual, rule.getTo())) {
            int[] residual = StaticFunctions.copyIntegerArray(qe.residual,
                rule.toSize());
            next = new QueueEntry(idx1, j, qe, 2, residual);
          } else if (difflen > 0
              && StaticFunctions.isPrefix(rule.getTo(), qe.residual)) {
            int[] residual = StaticFunctions.copyIntegerArray(rule.getTo(),
                qe.residual.length);
            next = new QueueEntry(idx1, j, qe, 1, residual);
          }

          // Check if there exists a sub-match
          if (next == null)
            continue;
          // Check if the next sub-match can be used further
          else if (UseLengthFilter && !Validator.checkLengthFilter(difflen, r,
              next.idx1, t, next.idx2))
            continue;
          if (rule.fromSize() == 1 && rule.toSize() == 1
              && rule.getFrom()[0] == rule.getTo()[0])
            ;
          else
            next.appliedRules = qe.appliedRules + 1;
          // Check if we found an answer
          if (next.idx1 == (r.size() - 1) && next.idx2 == (t.size() - 1)
              && next.type == 0)
            return next.appliedRules;
          // Check if the next sub-match is already discovered
          else if (!discovered.contains(next)) {
            queue.add(next);
            discovered.add(next);
          }
        }
    }
    return -1;
  }

  /**
   * Returns the number of applied rules if matched. -1 otherwise.
   */
  public static int DP_A_Queue_useACAutomata(Record r, Record t,
      boolean UseLengthFilter) {
    // Increase counter
    ++checked;
    // Check if interval of two given strings overlap or not
    if (UseLengthFilter) {
      int[] rCandidateLengths = r.getCandidateLengths(r.size() - 1);
      int[] tCandidateLengths = t.getCandidateLengths(t.size() - 1);
      if (!StaticFunctions.overlap(rCandidateLengths, tCandidateLengths))
        return -1;
    }

    // Initialize queue
    Queue<QueueEntry> queue = new LinkedList<QueueEntry>();
    // Initialize a hashset for storing discovered entries
    WYK_HashSet<QueueEntry> discovered = new WYK_HashSet<QueueEntry>(100);

    // Variable for next candidate entry
    QueueEntry next;

    // Create initial matching results
    queue.add(new QueueEntry(-1, -1, null, 0, null));

    // Begin
    while (!queue.isEmpty()) {
      ++niterentry;
      QueueEntry qe = queue.poll();
      int i = qe.idx1;
      int j = qe.idx2;
      int difflen = 0;
      // Both r[0..i] and t[0..j] can be sub-matched into qe.matched
      // Case l=0
      if (qe.type == 0) {
        Rule[] Rrules = r.getApplicableRules(i + 1);
        Rule[] Trules = t.getApplicableRules(j + 1);
        int long_idx = i;
        int short_idx = j;
        Record long_record = r;
        Rule[] short_rules = Trules;
        boolean R_is_shorter_than_T = false;
        if (Rrules.length < Trules.length) {
          R_is_shorter_than_T = true;
          long_record = t;
          short_rules = Rrules;
          long_idx = j;
          short_idx = i;
        }
        for (Rule rule1 : short_rules) {
          // If this rule is applied, r[0...idx1] will be sub-matched
          int idx1 = short_idx + rule1.fromSize();
          List<Rule> candidates = long_record.getMatched(rule1.getTo(),
              long_idx + 1);
          for (Rule rule2 : candidates) {
            ++niterrules;
            next = null;
            // If this rule is applied, t[0...idx2] will be
            // sub-matched
            int idx2 = long_idx + rule2.fromSize();
            // Calculate the length difference of two transformed
            // strings
            difflen = rule1.toSize() - rule2.toSize();
            // Case b=d
            if (difflen == 0
                && StaticFunctions.compare(rule1.getTo(), rule2.getTo()) == 0) {
              if (R_is_shorter_than_T)
                next = new QueueEntry(idx1, idx2, qe, 0, null);
              else
                next = new QueueEntry(idx2, idx1, qe, 0, null);
              if (rule1.fromSize() == 1
                  && rule1.getFrom()[0] == rule2.getFrom()[0])
                ;
              else
                next.appliedRules = qe.appliedRules + 1;
            }
            // Case d is a prefix of b
            else if (difflen > 0
                && StaticFunctions.isPrefix(rule1.getTo(), rule2.getTo())) {
              int[] residual = StaticFunctions.copyIntegerArray(rule1.getTo(),
                  rule2.toSize());
              if (R_is_shorter_than_T)
                next = new QueueEntry(idx1, idx2, qe, 1, residual);
              else
                next = new QueueEntry(idx2, idx1, qe, 2, residual);
            }
            // Case b is a prefix of d
            else if (difflen < 0
                && StaticFunctions.isPrefix(rule2.getTo(), rule1.getTo())) {
              int[] residual = StaticFunctions.copyIntegerArray(rule2.getTo(),
                  rule1.toSize());
              if (R_is_shorter_than_T)
                next = new QueueEntry(idx1, idx2, qe, 2, residual);
              else
                next = new QueueEntry(idx2, idx1, qe, 1, residual);
            }

            // Check if there exists a sub-match
            if (next == null)
              continue;
            // Check if the next sub-match can be used further
            else if (UseLengthFilter) {
              int tmpdifflen = R_is_shorter_than_T ? difflen : -difflen;
              if (!Validator.checkLengthFilter(tmpdifflen, r, next.idx1, t,
                  next.idx2))
                continue;
            }
            // Check if we found an answer
            if (next.idx1 == (r.size() - 1) && next.idx2 == (t.size() - 1)
                && next.type == 0)
              return next.appliedRules;
            // Check if the next sub-match is already discovered
            else if (!discovered.contains(next)) {
              queue.add(next);
              discovered.add(next);
            }
          }
        }
      }
      // Case l=1
      else if (qe.type == 1) {
        List<Rule> candidates = t.getMatched(qe.residual, j + 1);
        for (Rule rule : candidates) {
          ++niterrules;
          next = null;
          // If this rule is applied, t[0...idx2] will be sub-matched
          int idx2 = j + rule.fromSize();
          // Calculate the length difference of two transformed
          // strings
          difflen = qe.residual.length - rule.toSize();
          // Case e=b
          if (difflen == 0
              && StaticFunctions.compare(qe.residual, rule.getTo()) == 0) {
            next = new QueueEntry(i, idx2, qe, 0, null);
          }
          // Case b is a prefix of e
          else if (difflen > 0
              && StaticFunctions.isPrefix(qe.residual, rule.getTo())) {
            int[] residual = StaticFunctions.copyIntegerArray(qe.residual,
                rule.toSize());
            next = new QueueEntry(i, idx2, qe, 1, residual);
          }
          // Case e is a prefix of b
          else if (difflen < 0
              && StaticFunctions.isPrefix(rule.getTo(), qe.residual)) {
            int[] residual = StaticFunctions.copyIntegerArray(rule.getTo(),
                qe.residual.length);
            next = new QueueEntry(i, idx2, qe, 2, residual);
          }

          // Check if there exists a sub-match
          if (next == null)
            continue;
          // Check if the next sub-match can be used further
          else if (UseLengthFilter && !Validator.checkLengthFilter(difflen, r,
              next.idx1, t, next.idx2))
            continue;
          if (rule.fromSize() == 1 && rule.toSize() == 1
              && rule.getFrom()[0] == rule.getTo()[0])
            ;
          else
            next.appliedRules = qe.appliedRules + 1;
          // Check if we found an answer
          if (next.idx1 == (r.size() - 1) && next.idx2 == (t.size() - 1)
              && next.type == 0)
            return next.appliedRules;
          // Check if the next sub-match is already discovered
          else if (!discovered.contains(next)) {
            queue.add(next);
            discovered.add(next);
          }
        }
      }
      // Case l=2
      else {
        List<Rule> candidates = r.getMatched(qe.residual, i + 1);
        for (Rule rule : candidates) {
          ++niterrules;
          next = null;
          // If this rule is applied, r[0...idx1] will be sub-matched
          int idx1 = i + rule.fromSize();
          // Calculate the length difference of two transformed
          // strings
          difflen = rule.toSize() - qe.residual.length;
          // Case e=b
          if (difflen == 0
              && StaticFunctions.compare(qe.residual, rule.getTo()) == 0) {
            next = new QueueEntry(idx1, j, qe, 0, null);
          }
          // Case b is a prefix of e
          else if (difflen < 0
              && StaticFunctions.isPrefix(qe.residual, rule.getTo())) {
            int[] residual = StaticFunctions.copyIntegerArray(qe.residual,
                rule.toSize());
            next = new QueueEntry(idx1, j, qe, 2, residual);
          } else if (difflen > 0
              && StaticFunctions.isPrefix(rule.getTo(), qe.residual)) {
            int[] residual = StaticFunctions.copyIntegerArray(rule.getTo(),
                qe.residual.length);
            next = new QueueEntry(idx1, j, qe, 1, residual);
          }

          // Check if there exists a sub-match
          if (next == null)
            continue;
          // Check if the next sub-match can be used further
          else if (UseLengthFilter && !Validator.checkLengthFilter(difflen, r,
              next.idx1, t, next.idx2))
            continue;
          if (rule.fromSize() == 1 && rule.toSize() == 1
              && rule.getFrom()[0] == rule.getTo()[0])
            ;
          else
            next.appliedRules = qe.appliedRules + 1;
          // Check if we found an answer
          if (next.idx1 == (r.size() - 1) && next.idx2 == (t.size() - 1)
              && next.type == 0)
            return next.appliedRules;
          // Check if the next sub-match is already discovered
          else if (!discovered.contains(next)) {
            queue.add(next);
            discovered.add(next);
          }
        }
      }
    }
    return -1;
  }

  /**
   * Temporary matrix to save match result of DP_SingleSide
   */
  private static int matrix[][] = new int[1][0];

  /**
   * Check if s can be transformed to t
   *
   * @param s
   * @param t
   * @return true if s can be transformed to t
   */
  public static int DP_SingleSide(Record s, Record t) {
    ++checked;
    /**
     * If temporary matrix size is not enough, enlarge the space
     */
    if (s.size() >= matrix.length || t.size() >= matrix[0].length) {
      int rows = Math.max(s.size() + 1, matrix.length);
      int cols = Math.max(t.size() + 1, matrix[0].length);
      matrix = new int[rows][cols];
      matrix[0][0] = 1;
    }
    for (int i = 1; i <= s.size(); ++i) {
      for (int j = 1; j <= t.size(); ++j) {
        ++niterentry;
        matrix[i][j] = 0;
        /*
         * compute matrix[i][j]
         */
        Rule[] rules = s.getSuffixApplicableRules(i - 1);
        for (Rule rule : rules) {
          ++niterrules;
          int[] lhs = rule.getFrom();
          int[] rhs = rule.getTo();
          if (j - rhs.length < 0)
            continue;
          else if (matrix[i - lhs.length][j - rhs.length] == 0)
            continue;
          else if (StaticFunctions.compare(rhs, 0, t.getTokenArray(),
              j - rhs.length, rhs.length) == 0) {
            matrix[i][j] = matrix[i - lhs.length][j - rhs.length];
            if (!isSelfRule(rule)) ++matrix[i][j];
            break;
          }
        }
      }
    }
    return matrix[s.size()][t.size()] - 1;
  }

  /**
   * Temporary matrix to save sum of matrix values
   */
  private static int[][] P            = new int[1][0];

  public static long     earlyevaled  = 0;
  public static long     earlystopped = 0;

  /**
   * Check if s can be transformed to t
   *
   * @param s
   * @param t
   * @return true if s can be transformed to t
   */
  public static int DP_SingleSidewithEarlyPruning(Record s, Record t) {
    ++checked;
    /**
     * If temporary matrix size is not enough, enlarge the space
     */
    if (s.size() >= matrix.length || t.size() >= matrix[0].length) {
      int rows = Math.max(s.size() + 1, matrix.length);
      int cols = Math.max(t.size() + 1, matrix[0].length);
      matrix = new int[rows][cols];
      matrix[0][0] = 1;
    }
    if (s.size() >= P.length || t.size() >= P[0].length) {
      int rows = Math.max(s.size() + 1, P.length);
      int cols = Math.max(t.size() + 1, P[0].length);
      P = new int[rows][cols];
      P[0][0] = 1;
      for (int i = 1; i < rows; ++i)
        P[i][0] = 1;
      for (int i = 1; i < cols; ++i)
        P[0][i] = 1;
    }
    short maxsearchrange = s.getMaxSearchRange();
    short maxinvsearchrange = s.getMaxInvSearchRange();
    for (int i = 1; i <= s.size(); ++i) {
      /*
       * P[i][j]: sum_{i'=0}^i sum_{j'=0}^j M[i',j']
       */
      int Q = 0;
      short searchrange = s.getSearchRange(i - 1);
      short invsearchrange = s.getInvSearchRange(i - 1);

      int ip = Math.max(0, i - searchrange);
      for (int j = 1; j <= t.size(); ++j) {
        /*
         * Q: sum_{j'=0}^{j-1} M[i,j']
         */
        ++niterentry;
        matrix[i][j] = 0;
        /*
         * Claim 2
         * Let i' = i - maxsearchrange and j' = j - maxinvsearchrange.
         * P[i-1, j] + Q - P[i', j'] = 0
         * <==> s cannot be transformed to t
         */
        int valid = P[i - 1][j] + Q;
        if (i > maxsearchrange && j > maxinvsearchrange)
          valid -= P[i - maxsearchrange - 1][j - maxinvsearchrange - 1];
        if (valid == 0) {
          ++earlystopped;
          return -1;
        }
        int jp = Math.max(0, j - invsearchrange);
        /*
         * Claim 1
         * Let i' = i - searchrange and j' = j - invsearchrange.
         * M[i,j] = 1
         * ==> M[i'..i, j'..j] has at least one 1 (except M[i,j])
         * <==> P[i-1, j] + Q - P[i'-1, j-1] - P[i-1, j'-1] + P[i'-1, j'-1] \neq
         * 0
         * If there is no empty string in lhs/rhs of a rule,
         * <==> P[i-1, j-1] - P[i'-1, j-1] - P[i-1, j'-1] + P[i'-1, j'-1] \neq 0
         */
        valid = P[i - 1][j - 1];
        if (ip > 0) {
          valid -= P[ip - 1][j - 1];
          if (jp > 0) valid += P[ip - 1][jp - 1];
        }
        if (jp > 0) valid -= P[i - 1][jp - 1];
        if (valid == 0) {
          P[i][j] = P[i - 1][j] + Q;
          ++earlyevaled;
          continue;
        }
        /*
         * compute matrix[i][j], P[i][j] and Q.
         * Note that P[i][j] = P[i-1][j] + Q + matrix[i][j].
         */
        Rule[] rules = s.getSuffixApplicableRules(i - 1);
        for (Rule rule : rules) {
          ++niterrules;
          int[] lhs = rule.getFrom();
          int[] rhs = rule.getTo();
          if (j - rhs.length < 0)
            continue;
          else if (matrix[i - lhs.length][j - rhs.length] == 0)
            continue;
          else if (StaticFunctions.compare(rhs, 0, t.getTokenArray(),
              j - rhs.length, rhs.length) == 0) {
            matrix[i][j] = matrix[i - lhs.length][j - rhs.length];
            if (!isSelfRule(rule)) ++matrix[i][j];
            break;
          }
        }
        Q += (matrix[i][j] == 0 ? 0 : 1);
        /*
         * Q is updated. (Q = sum_{j'=1}^j M[i,j'])
         * Thus, P[i][j] = P[i-1][j] + Q.
         */
        P[i][j] = P[i - 1][j] + Q;
      }
    }
    return matrix[s.size()][t.size()] - 1;
  }

  /**
   * Temporary matrix to save match result of doubleside equivalence check.</br>
   * dsmatrix[i][j] stores all sub-matches for s[1..i]=>x1,
   * t[1..j]=>x2 where x1 is sub/superstring of x2 and |x1|-|x2|<=0 </br>
   * Every rule applied to s must use this matrix to retrieve previous matches.
   */
  @SuppressWarnings({ "unchecked" })
  private static HashSet<Submatch>[][] dsmatrix = new HashSet[1][0];
  /**
   * Temporary matrix to save match result of doubleside equivalence check.</br>
   * dtmatrix[i][j] stores all sub-matches for s[1..i]=>x1,
   * t[1..j]=>x2 where x1 is sub/superstring of x2 and |x1|-|x2|>0
   * Every rule applied to t must use this matrix to retrieve previous matches.
   */
  @SuppressWarnings("unchecked")
  private static HashSet<Submatch>[][] dtmatrix = new HashSet[1][0];
  private static final int             maxRHS   = 10;

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private static void enlargeDSMatchMatrix(int slen, int tlen) {
    if (slen >= dsmatrix.length || tlen >= dsmatrix[0].length) {
      int rows = Math.max(slen + 1, dsmatrix.length);
      int cols = Math.max(tlen + 1, dsmatrix[0].length);
      dsmatrix = new HashSet[rows][cols];
      dtmatrix = new HashSet[rows][cols];
      for (int i = 0; i < dsmatrix.length; ++i)
        for (int j = 0; j < dsmatrix[0].length; ++j) {
          dsmatrix[i][j] = new HashSet();
          dtmatrix[i][j] = new HashSet();
        }
      dsmatrix[0][0].add(EQUALMATCH);
    }
  }

  public static int DP_A_Matrix(Record s, Record t) {
    ++checked;
    /**
     * If temporary matrix size is not enough, enlarge the space
     */
    enlargeDSMatchMatrix(s.size(), t.size());
    for (int i = 0; i <= s.size(); ++i) {
      for (int j = 0; j <= t.size(); ++j) {
        if (i == 0 && j == 0) continue;
        dsmatrix[i][j].clear();
        dtmatrix[i][j].clear();
        ++niterentry;
        /*
         * Applicable rules of suffixes of s[1..i]
         */
        if (i != 0) {
          Rule[] rules = s.getSuffixApplicableRules(i - 1);
          for (Rule rule : rules) {
            ++niterrules;
            int[] lhs = rule.getFrom();
            int[] rhs = rule.getTo();
            HashSet<Submatch> prevmatches = (HashSet<Submatch>) dsmatrix[i
                - lhs.length][j];
            if (prevmatches.isEmpty()) continue;
            for (Submatch match : prevmatches) {
              ++nitermatches;
              int nextNRules = match.nAppliedRules + (isSelfRule(rule) ? 0 : 1);
              // If previous match is 'equals', simply add current rule
              if (match.rule == null) {
                dtmatrix[i][j].add(new Submatch(rule, true, 0, nextNRules));
                continue;
              }
              // Do not append additional rule if remaining string of previous
              // match is from s.
              else if (match.remainS) continue;
              int[] remainRHS = match.rule.getTo();
              int sidx = 0;
              int tidx = match.idx;
              boolean expandable = true;
              while (tidx < remainRHS.length && sidx < rhs.length
                  && expandable) {
                if (rhs[sidx] != remainRHS[tidx]) expandable = false;
                ++sidx;
                ++tidx;
              }
              if (expandable) {
                if (sidx == rhs.length && tidx == remainRHS.length) {
                  // Exact match
                  if (i == s.size() && j == t.size()) return nextNRules;
                  dsmatrix[i][j].add(new Submatch(null, false, 0, nextNRules));
                }
                // rhs is fully matched (!remainS)
                else if (sidx == rhs.length)
                  dsmatrix[i][j]
                      .add(new Submatch(match.rule, false, tidx, nextNRules));
                // remainRHS is fully matched (remainS)
                else
                  dtmatrix[i][j]
                      .add(new Submatch(rule, true, sidx, nextNRules));
              }
            }
          }
        }
        /**
         * Applicable rules of suffixes of t[1..j]
         */
        if (j != 0) {
          Rule[] rules = t.getSuffixApplicableRules(j - 1);
          for (Rule rule : rules) {
            ++niterrules;
            int[] lhs = rule.getFrom();
            int[] rhs = rule.getTo();
            boolean selfrule = (lhs.length == 1 && rhs.length == 1
                && lhs[0] == rhs[0]);
            HashSet<Submatch> prevmatches = (HashSet<Submatch>) dtmatrix[i][j
                - lhs.length];
            if (prevmatches.isEmpty()) continue;
            for (Submatch match : prevmatches) {
              ++nitermatches;
              int nextNRules = match.nAppliedRules + (selfrule ? 0 : 1);
              // If previous match is 'equals', do not apply this rule
              // since a rule of s is always applied first.
              if (match.rule == null)
                continue;
              // Do not append additional rule if remaining string of previous
              // match is from t.
              else if (!match.remainS) continue;
              int[] remainRHS = match.rule.getTo();
              int sidx = match.idx;
              int tidx = 0;
              boolean expandable = true;
              while (sidx < remainRHS.length && tidx < rhs.length
                  && expandable) {
                if (rhs[tidx] != remainRHS[sidx]) expandable = false;
                ++sidx;
                ++tidx;
              }
              if (expandable) {
                if (tidx == rhs.length && sidx == remainRHS.length) {
                  // Exact match
                  if (i == s.size() && j == t.size()) return nextNRules;
                  dsmatrix[i][j].add(new Submatch(null, false, 0, nextNRules));
                }
                // rhs is fully matched (remainS)
                else if (tidx == rhs.length)
                  dtmatrix[i][j]
                      .add(new Submatch(match.rule, true, sidx, nextNRules));
                // remainRHS is fully matched (!remainS)
                else
                  dsmatrix[i][j]
                      .add(new Submatch(rule, false, tidx, nextNRules));
              }
            }
          }
        }
      }
    }
    return -1;
  }

  /**
   * Temporary matrix to save sum of matrix values
   */
  private static int[][] dsP = new int[1][0];
  private static int[][] dtP = new int[1][0];

  private static void enlargeDSPMatrix(int slen, int tlen) {
    if (slen >= dsP.length || tlen >= dsP[0].length) {
      int rows = Math.max(slen + 1, dsP.length);
      int cols = Math.max(tlen + 1, dsP[0].length);
      dsP = new int[rows][cols];
      dtP = new int[rows][cols];
      dsP[0][0] = 1;
    }
  }

  public static int DP_A_MatrixwithEarlyPruning(Record s, Record t) {
    ++checked;
    /**
     * If temporary matrix size is not enough, enlarge the space
     */
    enlargeDSMatchMatrix(s.size(), t.size());
    enlargeDSPMatrix(s.size(), t.size());

    short smaxsearchrange = s.getMaxSearchRange();
    short tmaxsearchrange = t.getMaxSearchRange();
    for (int i = 0; i <= s.size(); ++i) {
      int sQ = 0;
      int tQ = 0;
      for (int j = 0; j <= t.size(); ++j) {
        if (i == 0 && j == 0) {
          ++sQ;
          continue;
        }
        dsmatrix[i][j].clear();
        dtmatrix[i][j].clear();
        ++niterentry;
        /*
         * Claim 2
         * Let i' = i - smaxsearchrange and j' = j - tmaxsearchrange.
         * P[i-1, j] + Q - P[i', j'] = 0
         * <==> s cannot be transformed to t
         */
        int valid = sQ + tQ;
        if (i > 0) {
          valid += dsP[i - 1][j];
          valid += dtP[i - 1][j];
        }
        if (i > smaxsearchrange && j > tmaxsearchrange) {
          valid -= dsP[i - smaxsearchrange - 1][j - tmaxsearchrange - 1];
          valid -= dtP[i - smaxsearchrange - 1][j - tmaxsearchrange - 1];
        }
        if (valid == 0) {
          ++earlystopped;
          return -1;
        }

        boolean s_skipped = i == 0;
        boolean t_skipped = j == 0;
        /*
         * Applicable rules of suffixes of s[1..i]
         */
        if (i != 0) {
          // Check if we can skip evaluating current entry
          short searchrange = s.getSearchRange(i - 1);
          int ip = Math.max(0, i - searchrange);
          valid = dsP[i - 1][j];
          if (ip > 0) {
            valid -= dsP[ip - 1][j];
            if (j > 0) valid += dsP[ip - 1][j - 1];
          }
          if (j > 0) valid -= dsP[i - 1][j - 1];

          if (valid == 0)
            s_skipped = true;
          else {
            Rule[] rules = s.getSuffixApplicableRules(i - 1);
            for (Rule rule : rules) {
              ++niterrules;
              int[] lhs = rule.getFrom();
              int[] rhs = rule.getTo();
              HashSet<Submatch> prevmatches = (HashSet<Submatch>) dsmatrix[i
                  - lhs.length][j];
              if (prevmatches.isEmpty()) continue;
              for (Submatch match : prevmatches) {
                ++nitermatches;
                int nextNRules = match.nAppliedRules
                    + (isSelfRule(rule) ? 0 : 1);
                // If previous match is 'equals', simply add current rule
                if (match.rule == null) {
                  dtmatrix[i][j].add(new Submatch(rule, true, 0, nextNRules));
                  continue;
                }
                // Do not append additional rule if remaining string of previous
                // match is from s.
                else if (match.remainS) continue;
                int[] remainRHS = match.rule.getTo();
                int sidx = 0;
                int tidx = match.idx;
                boolean expandable = true;
                while (tidx < remainRHS.length && sidx < rhs.length
                    && expandable) {
                  if (rhs[sidx] != remainRHS[tidx]) expandable = false;
                  ++sidx;
                  ++tidx;
                }
                if (expandable) {
                  if (sidx == rhs.length && tidx == remainRHS.length) {
                    // Exact match
                    if (i == s.size() && j == t.size()) return nextNRules;
                    dsmatrix[i][j]
                        .add(new Submatch(null, false, 0, nextNRules));
                  }
                  // rhs is fully matched (!remainS)
                  else if (sidx == rhs.length)
                    dsmatrix[i][j]
                        .add(new Submatch(match.rule, false, tidx, nextNRules));
                  // remainRHS is fully matched (remainS)
                  else
                    dtmatrix[i][j]
                        .add(new Submatch(rule, true, sidx, nextNRules));
                }
              }
            }
          }
        }
        /**
         * Applicable rules of suffixes of t[1..j]
         */
        if (j != 0) {
          // Check if we can skip evaluating current entry
          short searchrange = t.getSearchRange(j - 1);
          int jp = Math.max(0, j - searchrange);
          valid = dtP[i][j - 1];
          if (jp > 0) {
            valid -= dtP[i][jp - 1];
            if (i > 0) valid += dtP[i - 1][jp - 1];
          }
          if (i > 0) valid -= dtP[i - 1][j - 1];
          if (valid == 0)
            t_skipped = true;
          else {
            Rule[] rules = t.getSuffixApplicableRules(j - 1);
            for (Rule rule : rules) {
              ++niterrules;
              int[] lhs = rule.getFrom();
              int[] rhs = rule.getTo();
              HashSet<Submatch> prevmatches = (HashSet<Submatch>) dtmatrix[i][j
                  - lhs.length];
              if (prevmatches.isEmpty()) continue;
              for (Submatch match : prevmatches) {
                ++nitermatches;
                int nextNRules = match.nAppliedRules + 1;
                // If previous match is 'equals', do not apply this rule
                // since a rule of s is always applied first.
                if (match.rule == null)
                  continue;
                // Do not append additional rule if remaining string of previous
                // match is from t.
                else if (!match.remainS) continue;
                int[] remainRHS = match.rule.getTo();
                int sidx = match.idx;
                int tidx = 0;
                boolean expandable = true;
                while (sidx < remainRHS.length && tidx < rhs.length
                    && expandable) {
                  if (rhs[tidx] != remainRHS[sidx]) expandable = false;
                  ++sidx;
                  ++tidx;
                }
                if (expandable) {
                  if (tidx == rhs.length && sidx == remainRHS.length) {
                    // Exact match
                    if (i == s.size() && j == t.size()) return nextNRules;
                    dsmatrix[i][j]
                        .add(new Submatch(null, false, 0, nextNRules));
                  }
                  // rhs is fully matched (remainS)
                  else if (tidx == rhs.length)
                    dtmatrix[i][j]
                        .add(new Submatch(match.rule, true, sidx, nextNRules));
                  // remainRHS is fully matched (!remainS)
                  else
                    dsmatrix[i][j]
                        .add(new Submatch(rule, false, tidx, nextNRules));
                }
              }
            }
          }
        }
        if (s_skipped && t_skipped) ++earlyevaled;
        if (!dsmatrix[i][j].isEmpty()) ++sQ;
        if (!dtmatrix[i][j].isEmpty()) ++tQ;
        if (i == 0) {
          dsP[i][j] = sQ;
          dtP[i][j] = tQ;
        } else {
          dsP[i][j] = dsP[i - 1][j] + sQ;
          dtP[i][j] = dtP[i - 1][j] + tQ;
        }
      }
    }
    return -1;
  }

  private static Submatch EQUALMATCH = new Validator.Submatch(null, false, 0,
      0);

  private static boolean isSelfRule(Rule rule) {
    return rule.getFrom()[0] == rule.getTo()[0] && rule.getFrom().length == 1
        && rule.getTo().length == 1;
  }

  /**
   * Represents a remaining substring rule.rhs[idx..|rule.rhs|]
   */
  private static class Submatch {
    Rule    rule;
    boolean remainS;
    short   idx;
    short   nAppliedRules;

    Submatch(Rule rule, boolean remainS, int idx, int nAppliedRules) {
      this.rule = rule;
      this.remainS = remainS;
      this.idx = (short) idx;
      this.nAppliedRules = (short) nAppliedRules;
    }

    @Override
    public boolean equals(Object o) {
      if (o.getClass() != Submatch.class) return false;
      Submatch os = (Submatch) o;
      return (rule == os.rule && remainS == os.remainS && idx == os.idx);
    }

    @Override
    public int hashCode() {
      if (rule == null) return maxRHS;
      return (rule.hashCode() * maxRHS) + idx;
    }
  }
}
