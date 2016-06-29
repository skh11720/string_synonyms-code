package mine.hybrid;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import mine.Record;
import mine.RecordIDComparator;
import tools.Algorithm;
import tools.IntegerPair;
import tools.Parameters;
import tools.RandHash;
import tools.Rule;
import tools.RuleTrie;
import tools.StaticFunctions;
import tools.WYK_HashMap;
import validator.Validator;

/**
 * Given threshold, if a record has more than 'threshold' 1-expandable strings,
 * use an index to store them.
 * Otherwise, generate all 1-expandable strings and then use them to check
 * if two strings are equivalent.
 * Join (SH x TL), (S x TH) and (SL x TL)
 */
public class Hybrid2GramA1_2 extends Algorithm {
  static boolean                               useAutomata    = true;
  static boolean                               skipChecking   = false;
  static int                                   maxIndex       = Integer.MAX_VALUE;
  static boolean                               compact        = false;
  static int                                   joinThreshold;
  static boolean                               singleside;
  static Validator                             checker;

  RecordIDComparator                           idComparator;
  RuleTrie                                     ruletrie;

  static String                                outputfile;

  /**
   * Key: (token, index) pair<br/>
   * Value: (min, max, record) triple
   */
  /**
   * Index of the records in R for the strings in S which has less or equal to
   * 'threshold' 1-expandable strings
   */
  Map<Integer, Map<IntegerPair, List<Record>>> SH_TL_idx;
  /**
   * Index of the records in R for the strings in S which has more than
   * 'threshold' 1-expandable strings
   */
  Map<Integer, Map<IntegerPair, List<Record>>> S_TH_idx;
  /**
   * List of 1-expandable strings
   */
  Map<Record, List<Integer>>                   setR;

  public static double                         dirsampleratio = 0.01;
  public static int                            mhsize         = 30;
  RandHash[]                                   rhfunc;

  protected Hybrid2GramA1_2(String rulefile, String Rfile, String Sfile)
      throws IOException {
    super(rulefile, Rfile, Sfile);
    idComparator = new RecordIDComparator();
    ruletrie = new RuleTrie(rulelist);
    rhfunc = new RandHash[mhsize];
    for (int i = 0; i < mhsize; ++i)
      rhfunc[i] = new RandHash();
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private void buildJoinMinIndex() {
    // BufferedWriter bw = new BufferedWriter(new FileWriter("asdf"));
    Runtime runtime = Runtime.getRuntime();

    long SH_TL_elements = 0;
    long S_TH_elements = 0;
    long est_invokes = 0;
    double est_unions = 0;
    // Build an index
    // Count Invokes per each (token, loc) pair
    Map<Integer, Map<IntegerPair, Directory>> SH_TL_invokes = new WYK_HashMap<Integer, Map<IntegerPair, Directory>>();
    Map<Integer, Map<IntegerPair, Directory>> S_TH_invokes = new WYK_HashMap<Integer, Map<IntegerPair, Directory>>();

    SH_TL_idx = new WYK_HashMap<Integer, Map<IntegerPair, List<Record>>>();
    S_TH_idx = new WYK_HashMap<Integer, Map<IntegerPair, List<Record>>>();

    // Actually, tableT
    for (Record rec : tableS) {
      List<Set<IntegerPair>> available2Grams = rec.get2Grams();
      int searchmax = Math.min(available2Grams.size(), maxIndex);
      boolean is_TH_Record = rec.getEstNumRecords() > joinThreshold;
      for (int i = 0; i < searchmax; ++i) {
        Map<IntegerPair, Directory> curr_invokes;
        if (is_TH_Record) {
          curr_invokes = S_TH_invokes.get(i);
          if (curr_invokes == null) {
            curr_invokes = new WYK_HashMap<IntegerPair, Directory>();
            S_TH_invokes.put(i, curr_invokes);
          }
        } else {
          curr_invokes = SH_TL_invokes.get(i);
          if (curr_invokes == null) {
            curr_invokes = new WYK_HashMap<IntegerPair, Directory>();
            SH_TL_invokes.put(i, curr_invokes);
          }
        }
        for (IntegerPair twogram : available2Grams.get(i)) {
          if (Math.random() > dirsampleratio) continue;
          Directory count = curr_invokes.get(twogram);
          if (count == null) {
            count = new Directory();
            curr_invokes.put(twogram, count);
          }
          count.add(rec);
        }
      }
    }

    // bw.close();
    System.out.println("Bigram retrieval : " + Record.exectime);
    System.out.println(
        (runtime.totalMemory() - runtime.freeMemory()) / 1048576 + "MB used");

    final Map[] invokes = { S_TH_invokes, SH_TL_invokes };
    final Map[] indices = { S_TH_idx, SH_TL_idx };

    // Actually, tableS
    int[] union_sig = new int[mhsize];
    for (Record rec : tableR) {
      List<Set<IntegerPair>> available2Grams = rec.get2Grams();
      int[] range = rec.getCandidateLengths(rec.size() - 1);
      int searchmax = Math.min(range[0], maxIndex);
      boolean is_SH_record = rec.getEstNumRecords() > joinThreshold;

      for (int mapidx = 0; mapidx < 2; ++mapidx) {
        // SL record : skip building the second index
        if (mapidx == 1 && !is_SH_record)
          continue;
        else if (invokes[mapidx].size() == 0) continue;

        // Find the best location with the least number of candidates
        int minIdx = -1;
        int minInvokes = Integer.MAX_VALUE;
        double minUnion = Integer.MAX_VALUE;
        double[] unionarr = new double[searchmax];

        for (int i = 0; i < searchmax; ++i) {
          // prepare for the union signature
          for (int j = 0; j < mhsize; ++j)
            union_sig[j] = Integer.MAX_VALUE;
          int maxsize = 0;
          Directory maxsizedir = null;

          // Find every directory with the bigrams which this record can
          // generate
          Map<IntegerPair, Directory> curr_invokes = (Map<IntegerPair, Directory>) invokes[mapidx]
              .get(i);
          if (curr_invokes == null) {
            minIdx = i;
            minInvokes = 0;
            minUnion = 0;
            break;
          }
          int invoke = 0;
          for (IntegerPair twogram : available2Grams.get(i)) {
            Directory dir = curr_invokes.get(twogram);
            if (dir == null)
              continue;
            else if (dir.count > maxsize) {
              maxsize = dir.count;
              maxsizedir = dir;
            }
            for (int j = 0; j < mhsize; ++j)
              union_sig[j] = Math.min(union_sig[j], dir.minhash[j]);
            invoke += dir.count;
          }

          // Compute the estimated union size
          int inter = 0;
          double union = 0;
          if (maxsizedir != null) {
            assert (maxsize != 0);
            for (int j = 0; j < mhsize; ++j)
              if (union_sig[j] == maxsizedir.minhash[j]) ++inter;
            union = ((double) maxsizedir.count * mhsize) / inter;
          }
          unionarr[i] = union;

          // Update minUnion
          if (union < minUnion) {
            minIdx = i;
            minInvokes = invoke;
            minUnion = union;
          }
        }

        // Add record to index
        Map<IntegerPair, List<Record>> curr_idx = (Map<IntegerPair, List<Record>>) indices[mapidx]
            .get(minIdx);
        if (curr_idx == null) {
          curr_idx = new WYK_HashMap<IntegerPair, List<Record>>();
          indices[mapidx].put(minIdx, curr_idx);
        }
        for (IntegerPair twogram : available2Grams.get(minIdx)) {
          List<Record> list = curr_idx.get(twogram);
          if (list == null) {
            list = new ArrayList<Record>();
            curr_idx.put(twogram, list);
          }
          list.add(rec);
        }
        int elements = available2Grams.get(minIdx).size();
        if (mapidx == 1)
          S_TH_elements += elements;
        else
          SH_TL_elements += elements;

        est_unions += minUnion;
        est_invokes += minInvokes;
      }
    }
    System.out.println("Bigram retrieval : " + Record.exectime);
    System.out.println(
        (runtime.totalMemory() - runtime.freeMemory()) / 1048576 + "MB used");

    System.out.println("SH_TL idx size : " + SH_TL_elements);
    System.out.println("S_TH idx size : " + S_TH_elements);
    System.out.println("Est invokes : " + est_invokes / dirsampleratio);
    System.out.println("Est unions : " + est_unions / dirsampleratio);
    for (Entry<Integer, Map<IntegerPair, List<Record>>> e : SH_TL_idx
        .entrySet()) {
      System.out.println(e.getKey() + " : " + e.getValue().size());
    }
  }

  private void clearJoinMinIndex() {
    for (Map<IntegerPair, List<Record>> map : S_TH_idx.values())
      map.clear();
    S_TH_idx.clear();
    for (Map<IntegerPair, List<Record>> map : SH_TL_idx.values())
      map.clear();
    SH_TL_idx.clear();
  }

  private void buildNaiveIndex() {
    // Build 1-expanded set for every record in R
    int count = 0;
    setR = new HashMap<Record, List<Integer>>();
    for (int i = 0; i < tableR.size(); ++i) {
      Record rec = tableR.get(i);
      assert (rec != null);
      if (rec.getEstNumRecords() > joinThreshold) continue;
      List<Record> expanded = rec.expandAll(ruletrie);
      assert (expanded.size() <= joinThreshold);
      assert (!expanded.isEmpty());
      for (Record expR : expanded) {
        if (!setR.containsKey(expR)) setR.put(expR, new ArrayList<Integer>(5));
        List<Integer> list = setR.get(expR);
        assert (list != null);
        if (!list.isEmpty() && list.get(list.size() - 1) == i) continue;
        list.add(i);
      }
      ++count;
    }
    long idxsize = 0;
    for (List<Integer> list : setR.values())
      idxsize += list.size();
    System.out.println(count + " records are 1-expanded and indexed");
    System.out.println("Total index size: " + idxsize);
  }

  /**
   * Although this implementation is not efficient, we did like this to measure
   * the execution time of each part more accurate.
   * 
   * @return
   */
  private ArrayList<IntegerPair> join() {
    ArrayList<IntegerPair> rslt = new ArrayList<IntegerPair>();
    long appliedRules_sum = 0;

    long startTime = System.currentTimeMillis();
    buildJoinMinIndex();
    System.out.print("Building JoinMin Index finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));
    long time1 = 0;
    long time2 = 0;

    for (Record s : tableS) {
      boolean is_TH_record = s.getEstNumRecords() > joinThreshold;
      long time = System.currentTimeMillis();
      if (is_TH_record) {
        appliedRules_sum += searchEquivsByDynamicIndex(s, S_TH_idx, rslt);
        time1 += System.currentTimeMillis() - time;
      } else {
        appliedRules_sum += searchEquivsByDynamicIndex(s, SH_TL_idx, rslt);
        time2 += System.currentTimeMillis() - time;
      }
    }
    System.out.print("JoinMin part finished");
    System.out.print(Validator.checked + " cmps");

    clearJoinMinIndex();

    startTime = System.currentTimeMillis();
    buildNaiveIndex();
    System.out.print("Building Naive Index finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));

    long time3 = System.currentTimeMillis();
    for (Record s : tableS) {
      if (s.getEstNumRecords() > joinThreshold)
        continue;
      else
        searchEquivsByNaive1Expansion(s, rslt);
    }
    time3 = System.currentTimeMillis() - time3;

    System.out
        .println("Avg applied rules : " + appliedRules_sum + "/" + rslt.size());
    System.out.println("S_TH : " + time1);
    System.out.println("SH_TL : " + time2);
    System.out.println("SL_TL : " + time3);

    return rslt;
  }

  private int searchEquivsByDynamicIndex(Record s,
      Map<Integer, Map<IntegerPair, List<Record>>> idx,
      List<IntegerPair> rslt) {
    int appliedRules_sum = 0;
    List<Set<IntegerPair>> available2Grams = s.get2Grams();
    int[] range = s.getCandidateLengths(s.size() - 1);
    int searchmax = Math.min(available2Grams.size(), maxIndex);
    for (int i = 0; i < searchmax; ++i) {
      Map<IntegerPair, List<Record>> curr_idx = idx.get(i);
      if (curr_idx == null) continue;
      List<List<Record>> candidatesList = new ArrayList<List<Record>>();
      for (IntegerPair twogram : available2Grams.get(i)) {
        List<Record> tree = curr_idx.get(twogram);

        if (tree == null) continue;
        List<Record> list = new ArrayList<Record>();
        for (Record rec : tree)
          if (StaticFunctions.overlap(rec.getMinLength(), rec.getMaxLength(),
              range[0], range[1]))
            list.add(rec);
        candidatesList.add(list);
      }
      List<Record> candidates = StaticFunctions.union(candidatesList,
          idComparator);
      if (skipChecking) continue;
      for (Record recR : candidates) {
        int compare = checker.isEqual(recR, s);
        if (compare >= 0) {
          rslt.add(new IntegerPair(recR.getID(), s.getID()));
          appliedRules_sum += compare;
        }
      }
    }
    return appliedRules_sum;
  }

  private class IntegerComparator implements Comparator<Integer> {
    @Override
    public int compare(Integer o1, Integer o2) {
      return o1.compareTo(o2);
    }
  }

  /*
   * 
   * private int intarrbytes(int len) {
   * // Accurate bytes in 64bit machine is:
   * // ceil(4 * len / 8) * 8 + 16
   * return len * 4 + 16;
   * }
   */

  class Directory {
    int   count;
    int[] minhash;

    Directory() {
      count = 0;
      minhash = new int[mhsize];
      for (int i = 0; i < mhsize; ++i)
        minhash[i] = Integer.MAX_VALUE;
    }

    // Add a record
    void add(Record rec) {
      ++count;
      for (int i = 0; i < mhsize; ++i) {
        int hash = rhfunc[i].get(rec.getID());
        minhash[i] = Math.min(minhash[i], hash);
      }
    }
  }

  private void searchEquivsByNaive1Expansion(Record s, List<IntegerPair> rslt) {
    ArrayList<List<Integer>> candidates = new ArrayList<List<Integer>>();
    ArrayList<Record> expanded = s.expandAll(ruletrie);
    for (Record exp : expanded) {
      List<Integer> list = setR.get(exp);
      if (list == null) continue;
      candidates.add(list);
    }
    List<Integer> union = StaticFunctions.union(candidates,
        new IntegerComparator());
    for (Integer idx : union)
      rslt.add(new IntegerPair(idx, s.getID()));
  }

  public void statistics() {
    long strlengthsum = 0;
    long strmaxinvsearchrangesum = 0;
    int strs = 0;
    int maxstrlength = 0;

    long rhslengthsum = 0;
    int rules = 0;
    int maxrhslength = 0;

    for (Record rec : tableR) {
      strmaxinvsearchrangesum += rec.getMaxInvSearchRange();
      int length = rec.getTokenArray().length;
      ++strs;
      strlengthsum += length;
      maxstrlength = Math.max(maxstrlength, length);
    }
    for (Record rec : tableS) {
      strmaxinvsearchrangesum += rec.getMaxInvSearchRange();
      int length = rec.getTokenArray().length;
      ++strs;
      strlengthsum += length;
      maxstrlength = Math.max(maxstrlength, length);
    }

    for (Rule rule : rulelist) {
      int length = rule.getTo().length;
      ++rules;
      rhslengthsum += length;
      maxrhslength = Math.max(maxrhslength, length);
    }

    System.out.println("Average str length: " + strlengthsum + "/" + strs);
    System.out.println(
        "Average maxinvsearchrange: " + strmaxinvsearchrangesum + "/" + strs);
    System.out.println("Maximum str length: " + maxstrlength);
    System.out.println("Average rhs length: " + rhslengthsum + "/" + rules);
    System.out.println("Maximum rhs length: " + maxrhslength);
  }

  public void run() {
    long startTime = System.currentTimeMillis();
    preprocess(compact, maxIndex, useAutomata);
    System.out.print("Preprocess finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));

    // Retrieve statistics
    statistics();

    startTime = System.currentTimeMillis();
    Collection<IntegerPair> rslt = join();
    System.out.print("Join finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));
    System.out.println(rslt.size());
    System.out.println("Union counter: " + StaticFunctions.union_cmp_counter);

    try {
      BufferedWriter bw = new BufferedWriter(new FileWriter(outputfile));
      for (IntegerPair ip : rslt) {
        if (ip.i1 != ip.i2) bw.write(tableR.get(ip.i1).toString(strlist)
            + "\t==\t" + tableR.get(ip.i2).toString(strlist) + "\n");
      }
      bw.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public static void main(String[] args) throws IOException {
    Parameters params = Parameters.parseArgs(args);
    String Rfile = params.getInputX();
    String Sfile = params.getInputY();
    String Rulefile = params.getInputRules();
    outputfile = params.getOutput();

    // Setup parameters
    useAutomata = params.isUseACAutomata();
    skipChecking = params.isSkipChecking();
    maxIndex = params.getMaxIndex();
    compact = params.isCompact();
    joinThreshold = params.getJoinThreshold();
    singleside = params.isSingleside();
    checker = params.getValidator();

    long startTime = System.currentTimeMillis();
    Hybrid2GramA1_2 inst = new Hybrid2GramA1_2(Rulefile, Rfile, Sfile);
    System.out.print("Constructor finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));
    inst.run();
    Validator.printStats();
  }
}
