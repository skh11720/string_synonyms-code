package mine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import tools.IntegerMap;
import tools.Pair;
import tools.StaticFunctions;
import tools.WYK_HashMap;
import validator.Validator;

/**
 * @param <K>
 *          Signature type
 */
public class SI_Tree_JoinH<K> {
  public static boolean            skipEquiCheck = false;
  /**
   * A root entry of S-Directory (level 0) <br/>
   * Key for a fence entry is <b>u</b>.
   */
  private IntegerMap<FenceEntry>   root;
  /**
   * The number of records in this tree
   */
  private int                      size;
  /**
   * Number of fence entries
   */
  public long                      FEsize        = 0;
  /**
   * Number of leaf entries
   */
  public long                      LEsize        = 0;
  /**
   * Number of signatures
   */
  public long                      sigsize       = 0;
  private final Comparator<Record> RecordComparator;
  private Validator                checker;

  /**
   * Construct a SI-Tree
   */
  SI_Tree_JoinH(Validator checker) {
    root = new IntegerMap<FenceEntry>();
    RecordComparator = new Comparator<Record>() {
      public int compare(Record o1, Record o2) {
        return Integer.compare(o1.getID(), o2.getID());
      }
    };
    this.checker = checker;
  }

  /**
   * Add an entry into this SI-Tree
   *
   * @param rec
   *          The record to add
   */
  public void add(Record rec, Iterable<K> sigs) {
    int u = rec.getMinLength();
    if (!root.containsKey(u)) root.put(u, new FenceEntry(u));
    root.get(u).add(rec, sigs);
    ++size;
  }

  /**
   * Algorithm 3 in the paper <br/>
   * Retrieve all the candidate pairs
   */
  public HashSet<Pair<Record>> getCandidates(SI_Tree_JoinH<K> o,
      double threshold) {
    // Line 1 : Initialize
    HashSet<Pair<Record>> results = new HashSet<Pair<Record>>();

    // Line 2 : For all the combinations of fence entries
    for (FenceEntry fe : root.values()) {
      for (FenceEntry fe_other : o.root.values()) {
        // Line 3 : Check if this fence entry pair can generate any
        // candidate pair
        double cut = threshold * Math.max(fe.u, fe_other.u);
        if (Math.min(fe.v, fe_other.v) < cut) continue;

        // Line 4 : For all the combinations of leaf entries
        for (LeafEntry le : fe.P.values()) {
          for (LeafEntry le_other : fe_other.P.values()) {
            // Line 5 : Check if this leaf entry pair can generate
            // any candidate pair
            if (Math.min(le.t, le_other.t) < cut) continue;

            // Line 6 : Find all the overlapping signatures
            for (K sig : le.P.keySet()) {
              if (!le_other.P.containsKey(sig)) continue;

              // Line 7 : get L_s
              ArrayList<Record> Ls = le.P.get(sig);
              // Line 8 : get L_t
              ArrayList<Record> Lt = le_other.P.get(sig);

              // Line 9~10 : Add candidate pair
              for (Record rec1 : Ls)
                for (Record rec2 : Lt)
                  results.add(new Pair<Record>(rec1, rec2));
            }
          }
        }
      }
    }

    return results;
  }

  static final boolean verbose = false;

  /**
   * Algorithm 3 in the paper <br/>
   * Almost same as {@link #getCandidates(SI_Tree_JoinH, double)
   * getCandidates}
   * function, but additionally do join to reduce memory consumption.
   * This function enumerate signatures for each record in R and then find
   * overlapping records.
   */
  public List<Record> search(Record r, Iterable<K> sigs, boolean skipChecking) {
    // Line 1 : Initialize
    List<Record> results = new ArrayList<Record>();
    List<List<Record>> candidates = new ArrayList<List<Record>>();

    // For all the fence entries
    for (FenceEntry fe : root.values()) {
      // Line 3 : Check if this fence entry may contain equivalent length string
      if (!StaticFunctions.overlap(fe.u, fe.v, r.getMinLength(),
          r.getMaxLength()))
        continue;

      // For all the leaf entries
      for (LeafEntry le : fe.P.values()) {
        // Line 3 : Check if this fence entry may contain equivalent length
        // string
        if (!StaticFunctions.overlap(fe.u, le.t, r.getMinLength(),
            r.getMaxLength()))
          continue;

        // For each signature of r, find all the related records in this index
        for (K sig : sigs) {
          if (!le.P.containsKey(sig)) continue;
          List<Record> Ls = le.P.get(sig);
          candidates.add(Ls);
        }
      }
    }

    List<Record> union = StaticFunctions.union(candidates, RecordComparator);
    if (!skipChecking) {
      for (Record s : union) {
        int cmp = checker.isEqual(r, s);
        if (cmp >= 0) results.add(s);
      }
    }
    return results;
  }

  public int size() {
    return size;
  }

  /**
   * Fence-entry for S-Directory (level 1)<br/>
   * Contains three fields &lt;u, v, P&gt;<br/>
   * u : The minimum number of tokens in the expanded strings<br/>
   * v : The maximal number of tokens in the expanded strings
   * whose minimum length is u<br/>
   * P : A set of pointers to the leaf nodes
   */
  private class FenceEntry {
    /**
     * The number of tokens of a string
     */
    private final int             u;
    /**
     * The maximal number of tokens in the full expanded sets of strings
     * whose length is u
     */
    private int                   v;
    /**
     * A set of pointers to the leaf nodes
     */
    private IntegerMap<LeafEntry> P;

    FenceEntry(int u) {
      this.u = v = u;
      P = new IntegerMap<LeafEntry>();
      ++FEsize;
    }

    /**
     * Add a record to some leaf node under this fence entry
     *
     * @param rec
     */
    void add(Record rec, Iterable<K> sigs) {
      int v = rec.getMaxLength();
      this.v = Math.max(v, this.v);
      if (!P.containsKey(v)) P.put(v, new LeafEntry(v));
      P.get(v).add(rec, sigs);
    }
  }

  /**
   * Leaf-entry for S-Directory (level 2)<br/>
   * Contains two fields &lt;t, P&gt;<br/>
   * t : An integer to denote the number of the tokens in the full expanded
   * set of a string <br/>
   * P : A pointer to an inverted list
   */
  private class LeafEntry {
    /**
     * An integer to denote the number of the tokens in the full expanded
     * set of a string
     */
    private int                       t;
    /**
     * A pointer to an inverted index
     */
    private Map<K, ArrayList<Record>> P;

    LeafEntry(int v) {
      t = v;
      P = new WYK_HashMap<K, ArrayList<Record>>();
      ++LEsize;
    }

    /**
     * Add a record to this leaf node.
     *
     * @param rec
     */
    void add(Record rec, Iterable<K> sigs) {
      // Add to inverted indices
      for (K sig : sigs) {
        if (!P.containsKey(sig)) P.put(sig, new ArrayList<Record>(5));
        P.get(sig).add(rec);
        ++sigsize;
      }
    }
  }
}
