package mine.hybrid;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mine.JoinH2GramNoIntervalTree;
import mine.Naive1;
import mine.Record;
import mine.RecordIDComparator;
import tools.Algorithm;
import tools.IntIntRecordTriple;
import tools.IntegerPair;
import tools.Parameters;
import tools.Rule;
import tools.RuleTrie;
import tools.StaticFunctions;
import tools.WYK_HashMap;
import validator.Validator;
import wrapped.WrappedInteger;

/**
 * Given threshold, if a record has more than 'threshold' 1-expandable strings,
 * use an index to store them.
 * Otherwise, generate all 1-expandable strings and then use them to check
 * if two strings are equivalent.
 * It first build JoinMin(JoinH2Gram) index and then change threshold / modify
 * index in order to find the best execution time.
 */
public class Hybrid2GramWithOptTheta2 extends Algorithm {
  static boolean                                           useAutomata   = true;
  static boolean                                           skipChecking  = false;
  static int                                               maxIndex      = Integer.MAX_VALUE;
  static boolean                                           compact       = false;
  static boolean                                           singleside;
  static Validator                                         checker;

  RecordIDComparator                                       idComparator;
  RuleTrie                                                 ruletrie;

  static String                                            outputfile;

  int                                                      joinThreshold = 0;

  double                                                   alpha;
  double                                                   beta;
  double                                                   gamma;
  double                                                   delta;
  double                                                   epsilon;

  /**
   * Key: (token, index) pair<br/>
   * Value: (min, max, record) triple
   */
  /**
   * Index of the records in S for the strings in T which has less or equal to
   * 'threshold' 1-expandable strings
   * (SL x TH)
   */
  Map<Integer, Map<IntegerPair, List<IntIntRecordTriple>>> SL_TH_idx;
  /**
   * Index of the records in S for the strings in T which has more than
   * 'threshold' 1-expandable strings
   * (SH x T)
   */
  Map<Integer, Map<IntegerPair, List<IntIntRecordTriple>>> SH_T_idx;

  // Frequency counts
  Map<Integer, Map<IntegerPair, WrappedInteger>>           SH_T_invokes;
  Map<Integer, Map<IntegerPair, WrappedInteger>>           SL_TH_invokes;
  // Frequency counts
  Map<Integer, Map<IntegerPair, WrappedInteger>>           SH_T_idx_count;
  Map<Integer, Map<IntegerPair, WrappedInteger>>           SL_TH_idx_count;
  /**
   * List of 1-expandable strings
   */
  Map<Record, List<Integer>>                               setR;
  /**
   * Estimated number of comparisons
   */
  long                                                     est_SH_T_cmps;
  long                                                     est_SL_TH_cmps;

  protected Hybrid2GramWithOptTheta2(String rulefile, String Rfile,
      String Sfile) throws IOException {
    super(rulefile, Rfile, Sfile);
    idComparator = new RecordIDComparator();
    ruletrie = new RuleTrie(rulelist);
  }

  private void buildJoinMinIndex() {
    long SH_T_elements = 0;
    long SL_TH_elements = 0;
    est_SH_T_cmps = 0;
    est_SL_TH_cmps = 0;
    // Build an index
    // Count Invokes per each (token, loc) pair
    SH_T_invokes = new WYK_HashMap<Integer, Map<IntegerPair, WrappedInteger>>();
    SL_TH_invokes = new WYK_HashMap<Integer, Map<IntegerPair, WrappedInteger>>();
    // Count records in each index
    SH_T_idx_count = new WYK_HashMap<Integer, Map<IntegerPair, WrappedInteger>>();
    SL_TH_idx_count = new WYK_HashMap<Integer, Map<IntegerPair, WrappedInteger>>();
    // Actually, tableT
    for (Record rec : tableS) {
      List<Set<IntegerPair>> available2Grams = rec.get2Grams();
      int searchmax = Math.min(available2Grams.size(), maxIndex);
      // Every record is SH/TH record at the beginning
      boolean is_TH_Record = true;// rec.getEstNumRecords() > joinThreshold
      for (int i = 0; i < searchmax; ++i) {
        Map<IntegerPair, WrappedInteger> curr_SH_T_invokes = SH_T_invokes
            .get(i);
        Map<IntegerPair, WrappedInteger> curr_SL_TH_invokes = SL_TH_invokes
            .get(i);
        if (curr_SH_T_invokes == null) {
          curr_SH_T_invokes = new WYK_HashMap<IntegerPair, WrappedInteger>();
          curr_SL_TH_invokes = new WYK_HashMap<IntegerPair, WrappedInteger>();
          SH_T_invokes.put(i, curr_SH_T_invokes);
          SL_TH_invokes.put(i, curr_SL_TH_invokes);
        }
        for (IntegerPair twogram : available2Grams.get(i)) {
          WrappedInteger count = curr_SH_T_invokes.get(twogram);
          if (count == null) {
            count = new WrappedInteger(1);
            curr_SH_T_invokes.put(twogram, count);
          } else
            count.increment();
          if (is_TH_Record) {
            count = curr_SL_TH_invokes.get(twogram);
            if (count == null) {
              count = new WrappedInteger(1);
              curr_SL_TH_invokes.put(twogram, count);
            } else
              count.increment();
          }
        }
      }
    }

    // Build an index for the strings in S which has more than 'threshold'
    // 1-expandable strings
    // SH_T_idx = new WYK_HashMap<Integer, Map<IntegerPair,
    // List<IntIntRecordTriple>>>();
    // SL_TH_idx = new WYK_HashMap<Integer, Map<IntegerPair,
    // List<IntIntRecordTriple>>>();

    // Actually, tableS
    for (Record rec : tableR) {
      List<Set<IntegerPair>> available2Grams = rec.get2Grams();
      int[] range = rec.getCandidateLengths(rec.size() - 1);
      int minIdx = -1;
      int minInvokes = Integer.MAX_VALUE;
      int searchmax = Math.min(range[0], maxIndex);
      boolean is_SH_record = true;// rec.getEstNumRecords() > joinThreshold;

      Map<Integer, Map<IntegerPair, WrappedInteger>> invokes = is_SH_record
          ? SH_T_invokes : SL_TH_invokes;
      Map<Integer, Map<IntegerPair, WrappedInteger>> idx_count = is_SH_record
          ? SH_T_idx_count : SL_TH_idx_count;

      for (int i = 0; i < searchmax; ++i) {
        Map<IntegerPair, WrappedInteger> curr_invokes = invokes.get(i);
        if (curr_invokes == null) {
          minIdx = i;
          minInvokes = 0;
          break;
        }
        int invoke = 0;
        for (IntegerPair twogram : available2Grams.get(i)) {
          WrappedInteger count = curr_invokes.get(twogram);
          if (count != null) invoke += count.get();
        }
        if (invoke < minInvokes) {
          minIdx = i;
          minInvokes = invoke;
        }
      }

      Map<IntegerPair, WrappedInteger> curr_idx_count = idx_count.get(minIdx);
      if (curr_idx_count == null) {
        curr_idx_count = new WYK_HashMap<IntegerPair, WrappedInteger>();
        idx_count.put(minIdx, curr_idx_count);
      }
      for (IntegerPair twogram : available2Grams.get(minIdx)) {
        WrappedInteger count = curr_idx_count.get(twogram);
        if (count == null) {
          count = new WrappedInteger(1);
          curr_idx_count.put(twogram, count);
        } else
          count.increment();
      }
      int elements = available2Grams.get(minIdx).size();
      if (is_SH_record) {
        est_SH_T_cmps += minInvokes;
        SH_T_elements += elements;
      } else {
        est_SL_TH_cmps += minInvokes;
        SL_TH_elements += elements;
      }
    }
    System.out.println("SH_T predict : " + est_SH_T_cmps);
    System.out.println("SH_T idx size : " + SH_T_elements);
    System.out.println("SL_TH predict : " + est_SL_TH_cmps);
    System.out.println("SL_TH idx size : " + SL_TH_elements);
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

    long time1 = System.currentTimeMillis();
    for (Record s : tableS) {
      if (s.getEstNumRecords() > joinThreshold)
        appliedRules_sum += searchEquivsByDynamicIndex(s, SH_T_idx, rslt);
    }
    time1 = System.currentTimeMillis() - time1;

    long time2 = System.currentTimeMillis();
    for (Record s : tableS) {
      if (s.getEstNumRecords() > joinThreshold)
        continue;
      else
        appliedRules_sum += searchEquivsByDynamicIndex(s, SL_TH_idx, rslt);
    }
    time2 = System.currentTimeMillis() - time2;

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
      Map<Integer, Map<IntegerPair, List<IntIntRecordTriple>>> idx,
      List<IntegerPair> rslt) {
    int appliedRules_sum = 0;
    List<Set<IntegerPair>> available2Grams = s.get2Grams();
    int[] range = s.getCandidateLengths(s.size() - 1);
    int searchmax = Math.min(available2Grams.size(), maxIndex);
    for (int i = 0; i < searchmax; ++i) {
      Map<IntegerPair, List<IntIntRecordTriple>> curr_idx = idx.get(i);
      if (curr_idx == null) continue;
      List<List<Record>> candidatesList = new ArrayList<List<Record>>();
      for (IntegerPair twogram : available2Grams.get(i)) {
        List<IntIntRecordTriple> tree = curr_idx.get(twogram);

        if (tree == null) continue;
        List<Record> list = new ArrayList<Record>();
        for (IntIntRecordTriple e : tree)
          if (StaticFunctions.overlap(e.min, e.max, range[0], range[1]))
            list.add(e.rec);
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
    for (int i = 0; i < tableR.size(); ++i)
      tableR.get(i).setID(i);
    for (int i = 0; i < tableS.size(); ++i)
      tableS.get(i).setID(i);
  }

  public void run(double sampleratio) {
    long startTime = System.currentTimeMillis();
    preprocess(compact, maxIndex, useAutomata);
    System.out.print("Preprocess finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));

    // Retrieve statistics
    statistics();

    // Estimate constants
    findConstants(sampleratio);

    startTime = System.currentTimeMillis();
    buildJoinMinIndex();
    System.out.print("Building Index finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));

    // Modify index to get optimal theta
    startTime = System.currentTimeMillis();
    findTheta();
    System.out.print("Estimation finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));
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
    tableS = sampleRlist;

    System.out.println(sampleRlist.size() + " R records are sampled");
    System.out.println(sampleSlist.size() + " S records are sampled");

    // Infer alpha and beta
    Naive1 naiveinst = new Naive1(this);
    Naive1.threshold = 100;
    naiveinst.runWithoutPreprocess();
    alpha = naiveinst.alpha;
    beta = naiveinst.beta;

    // Infer gamma, delta and epsilon
    JoinH2GramNoIntervalTree joinmininst = new JoinH2GramNoIntervalTree(this);
    joinmininst.useAutomata = useAutomata;
    joinmininst.skipChecking = skipChecking;
    joinmininst.maxIndex = maxIndex;
    joinmininst.compact = compact;
    joinmininst.checker = checker;
    joinmininst.outputfile = outputfile;
    try {
      joinmininst.runWithoutPreprocess();
    } catch (Exception e) {
    }
    gamma = joinmininst.gamma;
    delta = joinmininst.delta;
    epsilon = joinmininst.epsilon;
    System.out.println("Bigram computation time : " + Record.exectime);
    Validator.printStats();

    // Restore
    tableR = tmpR;
    tableS = tmpS;

    System.out.println("Alpha : " + alpha);
    System.out.println("Beta : " + beta);
    System.out.println("Gamma : " + gamma);
    System.out.println("Delta : " + delta);
    System.out.println("Epsilon : " + epsilon);
  }

  private void findTheta() {
    // Find the best threshold
    long starttime = System.nanoTime();
    int best_theta = 0;
    long best_esttime = Long.MAX_VALUE;
    long[] best_esttimes = null;
    // Indicates the minimum indices which have more that 'theta' expanded
    // records
    int sidx = 0;
    int tidx = 0;

    // Prefix sums
    long currSLExpSize = 0;
    long currTLExpSize = 0;
    while (sidx < tableR.size() || tidx < tableS.size()) {
      // Find the next t
      Record s = sidx == tableR.size() ? null : tableR.get(sidx);
      Record t = tidx == tableS.size() ? null : tableS.get(tidx);
      long theta = 0;
      if (s == null)
        theta = t.getEstNumRecords();
      else if (t == null)
        theta = s.getEstNumRecords();
      else
        theta = Math.min(s.getEstNumRecords(), t.getEstNumRecords());
      if (theta > Integer.MAX_VALUE) break;

      // Estimate new running time
      // Modify SL_TH_invokes, SL_TH_idx
      while (t != null) {
        long expSize = t.getEstNumRecords();
        if (expSize > theta) break;
        currTLExpSize += expSize;
        t = tableS.get(++tidx);
        List<Set<IntegerPair>> twograms = t.get2Grams();
        for (int i = 0; i < t.getMaxLength(); ++i) {
          Map<IntegerPair, WrappedInteger> curr_invokes = SL_TH_invokes.get(i);
          if (curr_invokes == null) {
            curr_invokes = new WYK_HashMap<IntegerPair, WrappedInteger>();
            SL_TH_invokes.put(i, curr_invokes);
          }
          Map<IntegerPair, WrappedInteger> curr_idx_count = SL_TH_idx_count
              .get(i);
          for (IntegerPair curr_twogram : twograms.get(i)) {
            WrappedInteger count = curr_invokes.get(curr_twogram);
            if (count != null && count.get() > 0) count.decrement();
            if (curr_idx_count != null) {
              count = curr_idx_count.get(curr_twogram);
              if (count != null) est_SL_TH_cmps -= count.get();
            }
          }
        }
      }
      // Modify both indexes
      while (s != null) {
        long expSize = s.getEstNumRecords();
        if (expSize > theta) break;
        currSLExpSize += expSize;
        s = tableR.get(++sidx);
        // Count the reduced invocation counts
        List<Set<IntegerPair>> twograms = s.get2Grams();
        int SH_T_min_invokes = Integer.MAX_VALUE;
        int SL_TH_min_invokes = Integer.MAX_VALUE;
        int SL_TH_min_index = -1;
        for (int i = 0; i < s.getMinLength(); ++i) {
          int SH_T_count = 0;
          int SL_TH_count = 0;
          for (IntegerPair curr_twogram : twograms.get(i)) {
            WrappedInteger count = SH_T_invokes.get(i).get(curr_twogram);
            if (count != null) SH_T_count += count.get();
            count = SL_TH_invokes.get(i).get(curr_twogram);
            if (count != null) SL_TH_count += count.get();
          }
          if (SH_T_count < SH_T_min_invokes) SH_T_min_invokes = SH_T_count;
          if (SL_TH_count < SL_TH_min_invokes) {
            SL_TH_min_invokes = SL_TH_count;
            SL_TH_min_index = i;
          }
        }
        // Modify SH_T_idx
        // Modify SL_TH_idx
        Map<IntegerPair, WrappedInteger> curr_idx_count = SL_TH_idx_count
            .get(SL_TH_min_index);
        if (curr_idx_count == null) {
          curr_idx_count = new WYK_HashMap<IntegerPair, WrappedInteger>();
          SL_TH_idx_count.put(SL_TH_min_index, curr_idx_count);
        }
        for (IntegerPair curr_twogram : twograms.get(SL_TH_min_index)) {
          WrappedInteger count = curr_idx_count.get(curr_twogram);
          if (count == null) {
            count = new WrappedInteger(1);
            curr_idx_count.put(curr_twogram, count);
          } else
            count.increment();
        }
        est_SH_T_cmps -= SH_T_min_invokes;
        est_SL_TH_cmps += SL_TH_min_invokes;
      }

      long[] esttimes = new long[4];
      esttimes[0] = (long) (alpha * currSLExpSize);
      esttimes[1] = (long) (beta * currTLExpSize);
      esttimes[2] = (long) (epsilon * est_SH_T_cmps);
      esttimes[3] = (long) (epsilon * est_SL_TH_cmps);
      long esttime = esttimes[0] + esttimes[1] + esttimes[2] + esttimes[3];
      if (esttime < best_esttime) {
        best_theta = (int) theta;
        best_esttime = esttime;
        best_esttimes = esttimes;
      }
      if (theta == 10 || theta == 30 || theta == 100 || theta == 300
          || theta == 1000 || theta == 3000) {
        System.out.println("T=" + theta + " : " + esttime);
        System.out.println(Arrays.toString(esttimes));
      }
    }
    System.out.print("Best threshold : " + best_theta);
    System.out.println(" with running time " + best_esttime);
    System.out.println(Arrays.toString(best_esttimes));
    long duration = System.nanoTime() - starttime;
    System.out.println("Find theta with " + duration + "ns");
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
    Hybrid2GramWithOptTheta2 inst = new Hybrid2GramWithOptTheta2(Rulefile,
        Rfile, Sfile);
    System.out.print("Constructor finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));
    inst.run(params.getSampleRatio());
    Validator.printStats();
  }
}
