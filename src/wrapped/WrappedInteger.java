package wrapped;

public class WrappedInteger implements Comparable<WrappedInteger> {
  private int i;

  public WrappedInteger(int i) {
    this.i = i;
  }

  public void increment() {
    ++i;
  }

  public void increment(int delta) {
    i += delta;
  }

  public void decrement() {
    --i;
  }

  public int compareTo(WrappedInteger o) {
    return Integer.compare(i, o.i);
  }

  public int get() {
    return i;
  }

  @Override
  public boolean equals(Object o) {
    return (o.getClass() == WrappedInteger.class)
        && (i == ((WrappedInteger) o).i);
  }

  @Override
  public String toString() {
    return Integer.toString(i);
  }

  @Override
  public int hashCode() {
    return i;
  }
}