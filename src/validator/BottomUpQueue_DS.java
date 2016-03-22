package validator;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import mine.Record;
import tools.Rule;
import tools.StaticFunctions;
import tools.WYK_HashSet;

public class BottomUpQueue_DS extends Validator {
  private final boolean useLengthFilter;

  public BottomUpQueue_DS(boolean useLengthFilter) {
    this.useLengthFilter = useLengthFilter;
  }

  private static class QueueEntry {
    final int        idx1;
    final int        idx2;
    final int        type;
    final int[]      residual;
    @SuppressWarnings("unused")
    final QueueEntry prev;
    final int        hash;
    // Temporary variable to track the number of rule applications
    int              appliedRules = 0;

    QueueEntry(int idx1, int idx2, QueueEntry prev, int type, int[] residual) {
      this.idx1 = idx1;
      this.idx2 = idx2;
      this.type = type;
      this.residual = residual;
      this.prev = prev;
      int tmpHash = (idx1 ^ 0x1f1f1f1f + idx2) ^ 0x1f1f1f1f + type;
      if (residual != null) for (int r : residual)
        tmpHash = tmpHash ^ 0x1f1f1f1f + r;
      hash = tmpHash;
    }

    @Override
    public String toString() {
      String str = String.format("(%d,%d):(%d,%s)", idx1, idx2, type,
          Arrays.toString(residual));
      return str;
    }

    @Override
    public int hashCode() {
      return hash;
    }

    @Override
    public boolean equals(Object o) {
      QueueEntry me = (QueueEntry) o;
      if (me.idx1 != idx1 || me.idx2 != idx2)
        return false;
      else if (me.type == type) {
        if (type == 0)
          return true;
        else if (me.residual.length != residual.length) return false;
        boolean rslt = true;
        for (int i = 0; i < residual.length; ++i)
          rslt = rslt && (residual[i] == me.residual[i]);
      }
      return false;
    }
  }

  /**
   * Returns the number of applied rules if matched. -1 otherwise.
   */
  public int isEqual(Record r, Record t) {
    // Increase counter
    ++checked;
    // Check if interval of two given strings overlap or not
    if (useLengthFilter) {
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
            else if (useLengthFilter
                && !checkLengthFilter(difflen, r, next.idx1, t, next.idx2))
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
          else if (useLengthFilter
              && !checkLengthFilter(difflen, r, next.idx1, t, next.idx2))
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
          else if (useLengthFilter
              && !checkLengthFilter(difflen, r, next.idx1, t, next.idx2))
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

}
