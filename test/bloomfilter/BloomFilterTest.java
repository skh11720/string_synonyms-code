package bloomfilter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BloomFilterTest {

  @Test
  public void test() {
    BloomFilter bf = new BloomFilter(32, 1, 0);
    Signature sig1 = bf.add(1).add(10).add(8).add(60).create();
    Signature sig2 = bf.add(1).add(60).create();
    Signature sig3 = bf.create();
    assertTrue(sig1.contains(sig2));
    assertFalse(sig3.contains(sig2));
  }

}
