package mine;

public class IndexEntry {
  int    min;
  int    max;
  Record rec;

  IndexEntry(int min, int max, Record rec) {
    this.min = min;
    this.max = max;
    this.rec = rec;
  }
}
