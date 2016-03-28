package tools;

public class LongIntPair {
  public long l;
  public int  i;

  public LongIntPair(long twogram, int i) {
    this.l = twogram;
    this.i = i;
  }

  public boolean equals(Object o) {
    LongIntPair oip = (LongIntPair) o;
    return (l == oip.l) && (i == oip.i);
  }

  public int hashCode() {
    return ((int) l) ^ 0x1f1f1f1f + i;
  }

  public String toString() {
    return String.format("%d,%d", l, i);
  }
}
