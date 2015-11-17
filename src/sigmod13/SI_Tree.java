package sigmod13;

import java.util.ArrayList;
import java.util.HashSet;

import sigmod13.filter.ITF_Filter;
import tools.IntegerMap;

public class SI_Tree {
  /**
   * A root entry of S-Directory (level 0) <br/>
   * Key for a fence entry is <b>u</b>.
   */
  private IntegerMap<FenceEntry> root;
  /**
   * Global order
   */
  private final ITF_Filter       filter;
  /**
   * Join threshold used in this tree
   */
  private final double           theta;
  /**
   * The number of records in this tree
   */
  private int                    size;
  /**
   * Flag to designate similarity function
   */
  public static boolean          exactAnswer = false;

  /**
   * Number of fence entries
   */
  public long                    FEsize      = 0;
  /**
   * Number of leaf entries
   */
  public long                    LEsize      = 0;
  /**
   * Number of signatures
   */
  public long                    sigsize     = 0;

  /**
   * Construct a SI-Tree
   */
  private SI_Tree(double theta, ITF_Filter filter) {
    root = new IntegerMap<FenceEntry>();
    this.theta = theta;
    this.filter = filter;
  }

  public SI_Tree(double theta, ITF_Filter filter, ArrayList<SIRecord> records) {
    this(theta, filter);
    for (SIRecord rec : records)
      add(rec);
  }

  /**
   * Add an entry into this SI-Tree
   * 
   * @param rec
   *          The record to add
   */
  public void add(SIRecord rec) {
    int u = rec.getTokens().size();
    if (!root.containsKey(u)) root.put(u, new FenceEntry(u));
    root.get(u).add(rec);
    ++size;
  }

  /**
   * Algorithm 3 in the paper <br/>
   * Retrieve all the candidate pairs
   */
  public HashSet<SIRecordPair> getCandidates(SI_Tree o, double threshold) {
    // Line 1 : Initialize
    HashSet<SIRecordPair> results = new HashSet<SIRecordPair>();

    // Line 2 : For all the combinations of fence entries
    for (FenceEntry fe_this : root.values()) {
      for (FenceEntry fe_other : o.root.values()) {
        // Line 3 : Check if this fence entry pair can generate any
        // candidate pair
        double cut = threshold * Math.max(fe_this.u, fe_other.u);
        if (Math.min(fe_this.v, fe_other.v) < cut) continue;

        // Line 4 : For all the combinations of leaf entries
        for (LeafEntry le_this : fe_this.P.values()) {
          for (LeafEntry le_other : fe_other.P.values()) {
            // Line 5 : Check if this leaf entry pair can generate
            // any candidate pair
            if (Math.min(le_this.t, le_other.t) < cut) continue;

            // Line 6 : Find all the overlapping signatures
            for (int sig : le_this.P.keySet()) {
              if (!le_other.P.containsKey(sig)) continue;

              // Line 7 : get L_s
              ArrayList<SIRecord> Ls = le_this.P.get(sig);
              // Line 8 : get L_t
              ArrayList<SIRecord> Lt = le_other.P.get(sig);

              // Line 9~10 : Add candidate pair
              for (SIRecord rec1 : Ls)
                for (SIRecord rec2 : Lt)
                  results.add(new SIRecordPair(rec1, rec2));
            }
          }
        }
      }
    }

    return results;
  }

  /**
   * Algorithm 3 in the paper <br/>
   * Almost same as {@link #getCandidates(SI_Tree, double) getCandidates}
   * function, but additionally do join to reduce memory consumption.
   */
  public HashSet<SIRecordPair> join(SI_Tree o, double threshold) {
    // Line 1 : Initialize
    HashSet<SIRecordPair> results = new HashSet<SIRecordPair>();
    HashSet<SIRecordPair> evaled = new HashSet<SIRecordPair>();
    long count = 0;
    long duplicate_results = 0;

    // Line 2 : For all the combinations of fence entries
    for (FenceEntry fe_this : root.values()) {
      for (FenceEntry fe_other : o.root.values()) {
        // Line 3 : Check if this fence entry pair can generate any
        // candidate pair
        double cut = threshold * Math.max(fe_this.u, fe_other.u);
        if (Math.min(fe_this.v, fe_other.v) < cut) continue;

        // Line 4 : For all the combinations of leaf entries
        for (LeafEntry le_this : fe_this.P.values()) {
          for (LeafEntry le_other : fe_other.P.values()) {
            // Line 5 : Check if this leaf entry pair can generate
            // any candidate pair
            if (Math.min(le_this.t, le_other.t) < cut) continue;

            // Line 6 : Find all the overlapping signatures
            for (int sig : le_this.P.keySet()) {
              if (!le_other.P.containsKey(sig)) continue;

              // Line 7 : get L_s
              ArrayList<SIRecord> Ls = le_this.P.get(sig);
              // Line 8 : get L_t
              ArrayList<SIRecord> Lt = le_other.P.get(sig);

              // if(Ls.size() != 1 && Lt.size() != 1)
              // System.out.println(Ls.size() + "*" + Lt.size());
              // count += Ls.size() * Lt.size();

              // // Line 9~10 : Add candidate pair
              for (SIRecord rec1 : Ls) {
                HashSet<SIRecordExpanded> exp1 = null;
                if (exactAnswer) exp1 = rec1.generateAll();
                for (SIRecord rec2 : Lt) {
                  SIRecordPair sirp = new SIRecordPair(rec1, rec2);
                  if (evaled.contains(sirp)) ++duplicate_results;
                  // else
                  // evaled.add(sirp);
                  // Similarity check
                  double sim = 0;
                  if (exactAnswer) {
                    HashSet<SIRecordExpanded> exp2 = rec2.generateAll();
                    for (SIRecordExpanded exp1R : exp1)
                      for (SIRecordExpanded exp2R : exp2)
                        sim = Math.max(sim, exp1R.jaccard(exp2R));
                  } else
                    sim = SimilarityFunc.selectiveExp(rec1, rec2);
                  if (sim >= threshold) results.add(sirp);
                  ++count;
                }
              }
            }
          }
        }
      }
    }

    System.out.println("Comparisons : " + count);
    System.out.println("Duplicate results : " + duplicate_results);
    return results;
  }

  /**
   * Self-join version of Algorithm 3 in the paper <br/>
   * Almost same as {@link #getCandidates(SI_Tree, double) getCandidates}
   * function, but additionally do join to reduce memory consumption.
   */
  public HashSet<SIRecordPair> selfjoin(double threshold) {
    // Line 1 : Initialize
    HashSet<SIRecordPair> results = new HashSet<SIRecordPair>();
    HashSet<SIRecordPair> evaled = new HashSet<SIRecordPair>();
    long count = 0;
    long duplicate_results = 0;

    // Line 2 : For all the combinations of fence entries
    for (FenceEntry fe_this : root.values()) {
      for (FenceEntry fe_other : root.values()) {
        // Line 3 : Check if this fence entry pair can generate any
        // candidate pair
        double cut = threshold * Math.max(fe_this.u, fe_other.u);
        if (Math.min(fe_this.v, fe_other.v) < cut) continue;

        // Line 4 : For all the combinations of leaf entries
        for (LeafEntry le_this : fe_this.P.values()) {
          for (LeafEntry le_other : fe_other.P.values()) {
            // Line 5 : Check if this leaf entry pair can generate
            // any candidate pair
            if (Math.min(le_this.t, le_other.t) < cut) continue;

            // Line 6 : Find all the overlapping signatures
            for (int sig : le_this.P.keySet()) {
              if (!le_other.P.containsKey(sig)) continue;

              // Line 7 : get L_s
              ArrayList<SIRecord> Ls = le_this.P.get(sig);
              // Line 8 : get L_t
              ArrayList<SIRecord> Lt = le_other.P.get(sig);

              // if(Ls.size() != 1 && Lt.size() != 1)
              // System.out.println(Ls.size() + "*" + Lt.size());
              // count += Ls.size() * Lt.size();

              // // Line 9~10 : Add candidate pair
              for (SIRecord rec1 : Ls) {
                int id1 = rec1.getID();
                HashSet<SIRecordExpanded> exp1 = null;
                if (exactAnswer) exp1 = rec1.generateAll();
                for (SIRecord rec2 : Lt) {
                  int id2 = rec2.getID();
                  if (id1 <= id2) break;
                  SIRecordPair sirp = new SIRecordPair(rec1, rec2);
                  if (evaled.contains(sirp)) ++duplicate_results;
                  // else
                  // evaled.add(sirp);
                  // Similarity check
                  double sim = 0;
                  if (exactAnswer) {
                    HashSet<SIRecordExpanded> exp2 = rec2.generateAll();
                    for (SIRecordExpanded exp1R : exp1)
                      for (SIRecordExpanded exp2R : exp2)
                        sim = Math.max(sim, exp1R.jaccard(exp2R));
                  } else
                    sim = SimilarityFunc.selectiveExp(rec1, rec2);
                  if (sim >= threshold) results.add(sirp);
                  ++count;
                }
              }
            }
          }
        }
      }
    }

    System.out.println("Comparisons : " + count);
    System.out.println("Duplicate results : " + duplicate_results);
    return results;
  }

  /**
   * Naive version of SI-join. <br/>
   * Only one record set is indexed and records in the other set is expanded
   * in runtime. <br/>
   * Note that this method should be used if threshold == 1
   */
  public HashSet<SIRecordPair> naivejoin(ArrayList<SIRecord> records,
      boolean is_selfjoin) {
    HashSet<SIRecordPair> results = new HashSet<SIRecordPair>();
    long count = 0;
    for (SIRecord rec : records) {
      int id1 = rec.getID();
      HashSet<SIRecordExpanded> expanded = rec.generateAll();
      for (SIRecordExpanded exp : expanded) {
        SIRecord rec1 = new SIRecord(exp);
        int cut = 1 + exp.getSize() - (int) Math.ceil(theta * exp.getSize());
        HashSet<Integer> sig = filter.filter(exp, cut);
        // Number of sig must be 1
        if (sig.size() != 1) throw new RuntimeException();
        // For all fence entries
        for (FenceEntry fe : root.values()) {
          // Check length condition
          if (fe.v < exp.getSize() || exp.getSize() < fe.u) continue;
          // For all leaf entries
          for (LeafEntry le : fe.P.values()) {
            // Check length condition
            if (le.t < exp.getSize()) continue;
            Integer key = sig.iterator().next();
            ArrayList<SIRecord> values = le.P.getI(key);
            if (values == null) continue;
            // Check if similarity equals to 1
            for (SIRecord rec2 : values) {
              int id2 = rec2.getID();
              if (is_selfjoin && id1 <= id2) break;
              // Similarity check
              double sim = SimilarityFunc.fullExp2(rec1, rec2);
              if (sim == 1) {
                SIRecordPair sirp = new SIRecordPair(rec, rec2);
                results.add(sirp);
              }
              ++count;
            }
          }
        }
      }
    }

    System.out.println("Comparisons : " + count);
    return results;
  }

  public int size() {
    return size;
  }

  /**
   * Fence-entry for S-Directory (level 1)<br/>
   * Contains three fields &lt;u, v, P&gt;<br/>
   * u : The number of tokens of a string<br/>
   * v : The maximal number of tokens in the full expanded sets of strings
   * whose length is u<br/>
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
    void add(SIRecord rec) {
      int v = rec.fullExpanded.size();
      this.v = Math.max(v, this.v);
      if (!P.containsKey(v)) P.put(v, new LeafEntry(v));
      P.get(v).add(rec);
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
    private int                             t;
    /**
     * A pointer to an inverted index
     */
    private IntegerMap<ArrayList<SIRecord>> P;

    LeafEntry(int v) {
      t = v;
      P = new IntegerMap<ArrayList<SIRecord>>();
      ++LEsize;
    }

    /**
     * Add a record to this leaf node.
     * 
     * @param rec
     */
    void add(SIRecord rec) {
      HashSet<Integer> signature;
      // Special code for theta == 1 and filter == 1
      if (theta == 1) {
        SIRecordExpanded exp = new SIRecordExpanded(rec);
        signature = filter.filter(exp, 1);
      } else {
        // Retrieve all the signatures
        HashSet<SIRecordExpanded> expanded = rec.generateAll();
        signature = new HashSet<Integer>();
        for (SIRecordExpanded exp : expanded) {
          // In the paper the number of signature is states as belows.
          // int cut = (int) Math.ceil((1.0 - theta) * exp.getSize());
          // However, it should be
          int cut = 1 + exp.getSize() - (int) Math.ceil(theta * exp.getSize());
          HashSet<Integer> sig = filter.filter(exp, cut);
          signature.addAll(sig);
        }
      }

      // Add to inverted indices
      for (int sig : signature) {
        if (!P.containsKey(sig)) P.put(sig, new ArrayList<SIRecord>(5));
        P.get(sig).add(rec);
      }
      sigsize += signature.size();
    }
  }
}

/**
 * Class for storing SIRecord pair
 */
class SIRecordPair {
  final SIRecord rec1;
  final SIRecord rec2;
  /**
   * Pre-calculate hashcode for faster comparison
   */
  final int      hash;

  SIRecordPair(SIRecord rec1, SIRecord rec2) {
    this.rec1 = rec1;
    this.rec2 = rec2;
    hash = rec1.hashCode() + rec2.hashCode() ^ 0x1f1f1f1f;
  }

  public boolean equals(Object o) {
    SIRecordPair sirp = (SIRecordPair) o;
    if (hash != sirp.hash) return false;
    if (rec1.equals(sirp.rec1) && rec2.equals(sirp.rec2)) return true;
    return false;
  }

  public int hashCode() {
    return hash;
  }
}