package bloomfilter;

import java.util.Random;

public class BloomFilter {
  private final int               m;
  private final IntHashFunction[] f;
  private final Random            rand;
  private Signature               currsig;

  /**
   * Generate a bloom filter with m bits and k hash functions
   * 
   * @param m
   *          Number of bits
   * @param k
   *          Number of hash functions
   */
  public BloomFilter(int m, int k, long seed) {
    this.m = m;
    f = new IntHashFunction[k];
    rand = new Random(seed);
    for (int i = 0; i < k; ++i)
      f[i] = new IntHashFunction(m, rand);
  }

  /**
   * Adds an item i to the currently generated signature
   */
  public BloomFilter add(int i) {
    if (currsig == null) currsig = new Signature(m);
    for (IntHashFunction currf : f) {
      int h = currf.hash(i);
      assert (h < m);
      currsig.add(h);
    }
    return this;
  }

  /**
   * Releases the currently generated signature
   */
  public Signature create() {
    if (currsig == null) currsig = new Signature(m);
    Signature tmp = currsig;
    currsig = null;
    return tmp;
  }
}
