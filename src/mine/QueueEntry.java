package mine;

import java.util.Arrays;

public class QueueEntry {
  final int        idx1;
  final int        idx2;
  final int        type;
  final int[]      residual;
  final QueueEntry prev;
  final int        hash;

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