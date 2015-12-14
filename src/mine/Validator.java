package mine;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import tools.Rule;
import tools.StaticFunctions;
import tools.WYK_HashSet;

public class Validator {
  public static long filtered = 0;
  public static long checked  = 0;

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

  public static boolean DP_A_Queue(Record r, Record t,
      boolean UseLengthFilter) {
    // Increase counter
    ++checked;
    // Check if interval of two given strings overlap or not
    if (UseLengthFilter) {
      int[] rCandidateLengths = r.getCandidateLengths(r.size() - 1);
      int[] tCandidateLengths = t.getCandidateLengths(t.size() - 1);
      if (!StaticFunctions.overlap(rCandidateLengths, tCandidateLengths))
        return false;
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
              return true;
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
          // Check if we found an answer
          else if (next.idx1 == (r.size() - 1) && next.idx2 == (t.size() - 1)
              && next.type == 0)
            return true;
          // Check if the next sub-match is already discovered
          else if (!discovered.contains(next)) {
            queue.add(next);
            discovered.add(next);
          }
        }
      // Case l=2
      else
        for (Rule rule : r.getApplicableRules(i + 1)) {
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
          // Check if we found an answer
          else if (next.idx1 == (r.size() - 1) && next.idx2 == (t.size() - 1)
              && next.type == 0)
            return true;
          // Check if the next sub-match is already discovered
          else if (!discovered.contains(next)) {
            queue.add(next);
            discovered.add(next);
          }
        }
    }
    return false;
  }

  public static boolean DP_A_Queue_useACAutomata(Record r, Record t,
      boolean UseLengthFilter) {
    // Increase counter
    ++checked;
    // Check if interval of two given strings overlap or not
    if (UseLengthFilter) {
      int[] rCandidateLengths = r.getCandidateLengths(r.size() - 1);
      int[] tCandidateLengths = t.getCandidateLengths(t.size() - 1);
      if (!StaticFunctions.overlap(rCandidateLengths, tCandidateLengths))
        return false;
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
              return true;
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
          // Check if we found an answer
          else if (next.idx1 == (r.size() - 1) && next.idx2 == (t.size() - 1)
              && next.type == 0)
            return true;
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
          // Check if we found an answer
          else if (next.idx1 == (r.size() - 1) && next.idx2 == (t.size() - 1)
              && next.type == 0)
            return true;
          // Check if the next sub-match is already discovered
          else if (!discovered.contains(next)) {
            queue.add(next);
            discovered.add(next);
          }
        }
      }
    }
    return false;
  }
}
