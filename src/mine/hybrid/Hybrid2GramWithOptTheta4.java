package mine.hybrid;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mine.JoinH2GramNoIntervalTree2;
import mine.Naive1;
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
 * It first build JoinMin(JoinH2Gram) index and then change threshold / modify
 * index in order to find the best execution time.
 * Same as Hybrid2GramWithOptTheta2, but
 * 1) utilizes minhash to estimate execution and
 * 2) use Hybrid2GramA1_2 instead of A1
 * time.
 */
public class Hybrid2GramWithOptTheta4 extends Algorithm {
  static boolean                               useAutomata           = true;
  static boolean                               skipChecking          = false;
  static int                                   maxIndex              = Integer.MAX_VALUE;
  static boolean                               compact               = false;
  static boolean                               singleside;
  static Validator                             checker;

  RecordIDComparator                           idComparator;
  RuleTrie                                     ruletrie;

  static String                                outputfile;

  int                                          joinThreshold         = -1;

  double                                       alpha;
  double                                       beta;
  double                                       gamma;
  double                                       delta;
  double                                       epsilon;
  double                                       zeta;
  public static double                         dirsampleratio        = 0.01;

  public static double                         idx_count_sampleratio = 0.01;
  public static int                            mhsize                = 30;
  RandHash[]                                   rhfunc;

  private static final int                     RECORD_CLASS_BYTES    = 64;

  /*
   * private int intarrbytes(int len) {
   * // Accurate bytes in 64bit machine is:
   * // ceil(4 * len / 8) * 8 + 16
   * return len * 4 + 16;
   * }
   */

  /**
   * Key: (token, index) pair<br/>
   * Value: (min, max, record) triple
   */
  /**
   * Index of the records in S for the strings in T which has less or equal to
   * 'threshold' 1-expandable strings
   * (SL x TH)
   */
  Map<Integer, Map<IntegerPair, List<Record>>> S_TH_idx;
  /**
   * Index of the records in S for the strings in T which has more than
   * 'threshold' 1-expandable strings
   * (SH x T)
   */
  Map<Integer, Map<IntegerPair, List<Record>>> SH_TL_idx;

  // Frequency counts
  Map<Integer, Map<IntegerPair, Directory>>    TH_invokes;
  Map<Integer, Map<IntegerPair, Directory>>    TL_invokes;
  /**
   * List of 1-expandable strings
   */
  Map<Record, List<Integer>>                   setR;
  /**
   * Estimated number of comparisons
   */
  long                                         est_S_TH_cmps;
  long                                         est_SH_TL_cmps;

  /**
   * Estimated cost of expanding records in S
   */
  long                                         expcostS;
  /**
   * Estimated cost of expanding records in T
   */
  long                                         expcostT;
  long                                         maxtheta;

  int                                          minSHidx;
  int                                          minTHidx;

  List<Record>                                 sampleR;

  protected Hybrid2GramWithOptTheta4(String rulefile, String Rfile,
      String Sfile) throws IOException {
    super(rulefile, Rfile, Sfile);
    idComparator = new RecordIDComparator();
    ruletrie = new RuleTrie(rulelist);
    rhfunc = new RandHash[mhsize];
    for (int i = 0; i < mhsize; ++i)
      rhfunc[i] = new RandHash();
  }

  class Directory {
    List<Record> list;
    int[]        minhash;

    Directory() {
      minhash = new int[mhsize];
      for (int i = 0; i < mhsize; ++i)
        minhash[i] = Integer.MAX_VALUE;
      list = new ArrayList<Record>(2);
    }

    // Add a record
    void add(Record rec) {
      list.add(rec);
      for (int i = 0; i < mhsize; ++i) {
        int hash = rhfunc[i].get(rec.getID());
        minhash[i] = Math.min(minhash[i], hash);
      }
    }

    // Remove the last element with the given hash values
    void removeLast(int[] hash) {
      list.remove(list.size() - 1);
      for (int i = 0; i < mhsize; ++i) {
        if (hash[i] == minhash[i]) {
          minhash[i] = Integer.MAX_VALUE;
          for (Record rec : list) {
            int rechash = rhfunc[i].get(rec.getID());
            minhash[i] = Math.min(rechash, minhash[i]);
          }
        }
      }
    }
  }

  private void computeInvocationCounts() {
    // Build an index
    // Count Invokes per each (token, loc) pair
    TL_invokes = new WYK_HashMap<Integer, Map<IntegerPair, Directory>>();
    TH_invokes = new WYK_HashMap<Integer, Map<IntegerPair, Directory>>();
    // Actually, tableT
    for (Record rec : tableS) {
      if (Math.random() > dirsampleratio) continue;
      List<Set<IntegerPair>> available2Grams = rec.get2Grams();
      int searchmax = Math.min(available2Grams.size(), maxIndex);
      // Every record is SH/TH record at the beginning
      boolean is_TH_Record = rec.getEstNumRecords() > maxtheta;
      for (int i = 0; i < searchmax; ++i) {
        Map<IntegerPair, Directory> curr_invokes = null;
        if (is_TH_Record) {
          curr_invokes = TH_invokes.get(i);
          if (curr_invokes == null) {
            curr_invokes = new WYK_HashMap<IntegerPair, Directory>();
            TH_invokes.put(i, curr_invokes);
          }
        } else {
          curr_invokes = TL_invokes.get(i);
          if (curr_invokes == null) {
            curr_invokes = new WYK_HashMap<IntegerPair, Directory>();
            TL_invokes.put(i, curr_invokes);
          }
        }
        for (IntegerPair twogram : available2Grams.get(i)) {
          Directory count = curr_invokes.get(twogram);
          if (count == null) {
            count = new Directory();
            curr_invokes.put(twogram, count);
          }
          count.add(rec);
        }
      }
    }
    System.out.println("Bigram retrieval : " + Record.exectime);
  }

  /**
   * If theta == -1, find the maximum theta as well.
   * Else, use the given theta.
   */
  private void estNaiveExecTime(int theta) {
    Runtime runtime = Runtime.getRuntime();
    System.out.println(
        (runtime.totalMemory() - runtime.freeMemory()) / 1048576 + "MB used");
    long memlimit = theta < 0 ? runtime.freeMemory() / 2 : Long.MAX_VALUE;
    if (theta >= 0) maxtheta = theta;

    minSHidx = minTHidx = 0;
    expcostS = expcostT = 0;
    long currexpanded = 0;
    long currexpcost = 0;
    long memcost = 0;
    for (Record s : tableR) {
      long expanded = s.getEstNumRecords();
      if (theta >= 0) {
        if (expanded > theta) break;
        ++minSHidx;
        expcostS += expanded;
      } else if (expanded != currexpanded) {
        minSHidx = s.getID();
        currexpanded = expanded;
        expcostS += currexpcost;
        currexpcost = 0;
      }
      currexpcost += expanded;
      memcost += memcost(s.getEstExpandCost(), expanded);
      if (memcost > memlimit) break;
    }
    if (theta < 0) {
      if (memcost > memlimit)
        maxtheta = currexpanded - 1;
      else {
        maxtheta = currexpanded;
        minSHidx = tableR.size();
      }
    }

    currexpanded = 0;
    currexpcost = 0;
    memcost = 0;
    for (Record t : tableS) {
      long expanded = t.getEstNumRecords();
      if (theta >= 0) {
        if (expanded > theta) break;
        ++minTHidx;
        expcostT += expanded;
      } else if (expanded != currexpanded) {
        minTHidx = t.getID();
        currexpanded = expanded;
        expcostT += currexpcost;
        currexpcost = 0;
      }
      currexpcost += expanded;
      memcost += memcost(t.getEstExpandCost(), expanded);
      if (memcost > memlimit) break;
    }
    if (theta < 0) {
      if (memcost > memlimit)
        maxtheta = Math.max(maxtheta, currexpanded - 1);
      else {
        maxtheta = Math.max(maxtheta, currexpanded);
        minTHidx = tableS.size();
      }
    }
  }

  private int memcost(long lengthsum, long exps) {
    int memcost = 0;
    // Size for the integer arrays
    memcost += 4 * lengthsum + 16 * exps;
    // Size for the Record instance
    memcost += RECORD_CLASS_BYTES * exps;
    // Pointers in the inverted index
    memcost += 8 * exps;
    // Pointers in the Hashmap (in worst case)
    // Our hashmap filling ratio is 0.5: 24 / 0.5 = 48
    memcost += 48 * exps;

    return memcost;
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

    long time1 = 0;
    long time2 = 0;
    for (Record s : tableS) {
      long time = System.currentTimeMillis();
      if (s.getEstNumRecords() > joinThreshold) {
        appliedRules_sum += searchEquivsByDynamicIndex(s, S_TH_idx, rslt);
        time1 += System.currentTimeMillis() - time;
      } else {
        appliedRules_sum += searchEquivsByDynamicIndex(s, SH_TL_idx, rslt);
        time2 += System.currentTimeMillis() - time;
      }
    }

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
    System.out.println("large S : " + time1);
    System.out.println("small S + large R : " + time2);
    System.out.println("small S + small S: " + time3);

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
        for (Record r : tree)
          if (StaticFunctions.overlap(r.getMinLength(), r.getMaxLength(),
              range[0], range[1]))
            list.add(r);
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

  @Override
  protected void preprocess(boolean compact, int maxIndex,
      boolean useAutomata) {
    super.preprocess(compact, maxIndex, useAutomata);

    // Sort R and S with expanded sizes
    Comparator<Record> cmp = new Comparator<Record>() {
      @Override
      public int compare(Record o1, Record o2) {
        long est1 = o1.getEstNumRecords();
        long est2 = o2.getEstNumRecords();
        return Long.compare(est1, est2);
      }
    };
    Collections.sort(tableR, cmp);
    Collections.sort(tableS, cmp);

    // Reassign ID
    long maxSEstNumRecords = 0;
    long maxTEstNumRecords = 0;
    for (int i = 0; i < tableR.size(); ++i) {
      Record s = tableR.get(i);
      s.setID(i);
      maxSEstNumRecords = Math.max(maxSEstNumRecords, s.getEstNumRecords());
    }
    for (int i = 0; i < tableS.size(); ++i) {
      Record t = tableS.get(i);
      t.setID(i);
      maxTEstNumRecords = Math.max(maxTEstNumRecords, t.getEstNumRecords());
    }

    System.out.println("Max S expanded size : " + maxSEstNumRecords);
    System.out.println("Max T expanded size : " + maxTEstNumRecords);
  }

  public void run(double sampleratio) {
    long startTime = System.currentTimeMillis();
    preprocess(compact, maxIndex, useAutomata);
    System.out.print("Preprocess finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime) + "ms");

    // Retrieve statistics
    statistics();

    // Estimate constants
    findConstants(sampleratio);

    startTime = System.currentTimeMillis();
    estNaiveExecTime(joinThreshold);
    computeInvocationCounts();
    // checkLongestIndex();
    System.out.print("Building Index finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime) + "ms");

    // Modify index to get optimal theta
    startTime = System.currentTimeMillis();
    findTheta();
    System.out.print("Estimation finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime) + "ms");
    System.exit(1);

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

  @SuppressWarnings("static-access")
  private void findConstants(double sampleratio) {
    // Sample
    List<Record> sampleRlist = new ArrayList<Record>();
    List<Record> sampleSlist = new ArrayList<Record>();
    for (Record r : tableR)
      if (Math.random() < sampleratio) sampleRlist.add(r);
    for (Record s : tableS)
      if (Math.random() < sampleratio) sampleSlist.add(s);
    List<Record> tmpR = tableR;
    tableR = sampleRlist;
    List<Record> tmpS = tableS;
    tableS = sampleSlist;
    sampleR = sampleRlist;

    System.out.println(sampleRlist.size() + " R records are sampled");
    System.out.println(sampleSlist.size() + " S records are sampled");
    estNaiveExecTime(-1);

    // Infer alpha and beta
    Naive1 naiveinst = new Naive1(this);
    Naive1.threshold = (maxtheta / 2);
    naiveinst.runWithoutPreprocess();
    alpha = naiveinst.alpha;
    beta = naiveinst.beta;
    naiveinst = null;
    System.gc();

    // Infer gamma, delta and epsilon
    JoinH2GramNoIntervalTree2 joinmininst = new JoinH2GramNoIntervalTree2(this);
    joinmininst.useAutomata = useAutomata;
    joinmininst.skipChecking = skipChecking;
    joinmininst.maxIndex = maxIndex;
    joinmininst.compact = compact;
    joinmininst.checker = checker;
    joinmininst.outputfile = outputfile;
    joinmininst.runWithoutPreprocess();
    gamma = joinmininst.gamma;
    delta = joinmininst.delta;
    epsilon = joinmininst.epsilon;
    zeta = joinmininst.zeta;
    System.out.println("Bigram computation time : " + Record.exectime);
    joinmininst = null;
    System.gc();
    Validator.printStats();

    // Restore
    tableR = tmpR;
    tableS = tmpS;

    System.out.println("Alpha : " + alpha);
    System.out.println("Beta : " + beta);
    System.out.println("Gamma : " + gamma);
    System.out.println("Delta : " + delta);
    System.out.println("Epsilon : " + epsilon);
    System.out.println("Zeta : " + zeta);
  }

  private void findTheta() {
    // Find the best threshold
    long starttime = System.nanoTime();
    long best_theta = maxtheta;
    long best_esttime = Long.MAX_VALUE;
    long[] best_esttimes = null;

    // Indicates the minimum indices which have more that 'theta' expanded
    // records
    int sidx = 0;
    int tidx = 0;
    long theta = maxtheta;

    System.out.println("Max theta : " + theta);
    estTime();
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private long estTime() {
    // Compute Naive part
    // 1) Build index
    long naivebldidxtime = (long) (alpha * expcostS);
    // 2) Access index
    long naiveaccessidxtime = (long) (beta * expcostT);
    long naivetime = naivebldidxtime + naiveaccessidxtime;

    // Compute index part
    long[] retrievetime = new long[2];
    long[] equivchecktime = new long[2];
    Map[] invokes = { TL_invokes, TH_invokes };
    int[] minhash = new int[mhsize];
    for (int i = 0; i < tableR.size(); ++i) {
      if (Math.random() > idx_count_sampleratio) continue;
      Record s = tableR.get(i);
      boolean is_SH_record = i >= minSHidx;
      assert (is_SH_record == (s.getEstNumRecords() > joinThreshold));
      int searchmax = Math.min(s.getMinLength(), maxIndex);
      List<Set<IntegerPair>> twograms = s.get2Grams();

      // 0) Find the best position
      for (int mapidx = 0; mapidx < 2; ++mapidx) {
        if (!is_SH_record && mapidx == 0) continue;
        int bestUnionSize = Integer.MAX_VALUE;
        double bestCandSize = Double.POSITIVE_INFINITY;
        for (int j = 0; j < searchmax; ++j) {
          // If there is no invocations on the current position,
          // this position is the best position.
          Map<IntegerPair, Directory> curr_invokes = (Map<IntegerPair, Directory>) invokes[mapidx]
              .get(j);
          if (curr_invokes == null) {
            bestUnionSize = 0;
            bestCandSize = 0;
            break;
          }
          int unionsize = 0;
          double candsize = 0;
          int maxsize = 0;
          int[] maxsizehash = null;
          for (int k = 0; k < mhsize; ++k)
            minhash[k] = Integer.MAX_VALUE;
          Set<IntegerPair> curr_twograms = twograms.get(j);
          for (IntegerPair twogram : curr_twograms) {
            Directory dir = curr_invokes.get(twogram);
            if (dir == null) continue;
            if (maxsize < dir.list.size()) {
              maxsize = dir.list.size();
              maxsizehash = dir.minhash;
            }
            unionsize += dir.list.size();
            for (int k = 0; k < mhsize; ++k) {
              int hash = dir.minhash[k];
              minhash[k] = Math.min(minhash[k], hash);
            }
          }
          // Estimate union size
          if (maxsizehash != null) {
            int inter = 0;
            assert (maxsize != 0);
            for (int k = 0; k < mhsize; ++k)
              if (minhash[k] == maxsizehash[k]) ++inter;
            if (inter == 0)
              candsize = Math.min(tableS.size(), unionsize);
            else
              candsize = ((double) maxsize * mhsize) / inter;
          }
          if (bestCandSize > candsize) {
            bestUnionSize = unionsize;
            bestCandSize = candsize;
          }
        }

        // 1) Retrieve candidates
        retrievetime[mapidx] += (long) (zeta * bestUnionSize);

        // 2) Equivalence check
        equivchecktime[mapidx] += (long) (epsilon * bestCandSize);
      }
    }
    retrievetime[0] /= idx_count_sampleratio * dirsampleratio;
    retrievetime[1] /= idx_count_sampleratio * dirsampleratio;
    equivchecktime[0] /= idx_count_sampleratio * dirsampleratio;
    equivchecktime[1] /= idx_count_sampleratio * dirsampleratio;
    long STHtime = retrievetime[1] + equivchecktime[1];
    long SHTLtime = retrievetime[0] + equivchecktime[0];

    System.out.println("Naive : " + naivebldidxtime + " + " + naiveaccessidxtime
        + " = " + naivetime);
    System.out.println("SH x TL : " + retrievetime[0] + " + "
        + equivchecktime[0] + " = " + SHTLtime);
    System.out.println("S x TH : " + retrievetime[1] + " + " + equivchecktime[1]
        + " = " + STHtime);
    return naivetime + STHtime + SHTLtime;
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
    singleside = params.isSingleside();
    checker = params.getValidator();

    long startTime = System.currentTimeMillis();
    Hybrid2GramWithOptTheta4 inst = new Hybrid2GramWithOptTheta4(Rulefile,
        Rfile, Sfile);
    inst.joinThreshold = params.getJoinThreshold();
    System.out.print("Constructor finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));
    inst.run(params.getSampleRatio());
    Validator.printStats();
  }
}
