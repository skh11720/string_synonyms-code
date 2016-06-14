package mine.hybrid;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
import tools.WYK_HashSet;
import validator.Validator;

/**
 * Given threshold, if a record has more than 'threshold' 1-expandable strings,
 * use an index to store them.
 * Otherwise, generate all 1-expandable strings and then use them to check
 * if two strings are equivalent.
 * Use a basic model.
 */
public class Hybrid2GramWithOptTheta extends Algorithm {
  static boolean                                           useAutomata  = true;
  static boolean                                           skipChecking = false;
  static int                                               maxIndex     = Integer.MAX_VALUE;
  static boolean                                           compact      = false;
  static int                                               joinThreshold;
  static boolean                                           singleside;
  static Validator                                         checker;

  RecordIDComparator                                       idComparator;
  RuleTrie                                                 ruletrie;

  static String                                            outputfile;

  /**
   * Key: (token, index) pair<br/>
   * Value: (min, max, record) triple
   */
  /**
   * Index of the records in R for the strings in S which has less or equal to
   * 'threshold' 1-expandable strings
   */
  Map<Integer, Map<IntegerPair, List<IntIntRecordTriple>>> short_idx;
  /**
   * Index of the records in R for the strings in S which has more than
   * 'threshold' 1-expandable strings
   */
  Map<Integer, Map<IntegerPair, List<IntIntRecordTriple>>> long_idx;
  /**
   * List of 1-expandable strings
   */
  Map<Record, List<Integer>>                               setR;

  protected Hybrid2GramWithOptTheta(String rulefile, String Rfile, String Sfile)
      throws IOException {
    super(rulefile, Rfile, Sfile);
    idComparator = new RecordIDComparator();
    ruletrie = new RuleTrie(rulelist);
  }

  private void buildIndex() {
    long longelements = 0;
    long longpredictCount = 0;
    long shortelements = 0;
    long shortpredictCount = 0;
    // Build an index
    // Count Invokes per each (token, loc) pair
    Map<Integer, Map<IntegerPair, Integer>> long_invokes = new WYK_HashMap<Integer, Map<IntegerPair, Integer>>();
    Map<Integer, Map<IntegerPair, Integer>> short_invokes = new WYK_HashMap<Integer, Map<IntegerPair, Integer>>();
    for (Record rec : tableS) {
      List<Set<IntegerPair>> available2Grams = rec.get2Grams();
      int searchmax = Math.min(available2Grams.size(), maxIndex);
      boolean isIntegerPairRecord = rec.getEstNumRecords() > joinThreshold;
      for (int i = 0; i < searchmax; ++i) {
        Map<IntegerPair, Integer> curr_long_invokes = long_invokes.get(i);
        Map<IntegerPair, Integer> curr_short_invokes = short_invokes.get(i);
        if (curr_long_invokes == null) {
          curr_long_invokes = new WYK_HashMap<IntegerPair, Integer>();
          curr_short_invokes = new WYK_HashMap<IntegerPair, Integer>();
          long_invokes.put(i, curr_long_invokes);
          short_invokes.put(i, curr_short_invokes);
        }
        for (IntegerPair twogram : available2Grams.get(i)) {
          if (isIntegerPairRecord) {
            Integer count = curr_long_invokes.get(twogram);
            if (count == null)
              count = 1;
            else
              count += 1;
            curr_long_invokes.put(twogram, count);
          } else {
            Integer count = curr_short_invokes.get(twogram);
            if (count == null)
              count = 1;
            else
              count += 1;
            curr_short_invokes.put(twogram, count);
          }
        }
      }
    }

    // Build an index for the strings in S which has more than 'threshold'
    // 1-expandable strings
    long_idx = new WYK_HashMap<Integer, Map<IntegerPair, List<IntIntRecordTriple>>>();
    short_idx = new WYK_HashMap<Integer, Map<IntegerPair, List<IntIntRecordTriple>>>();

    for (Record rec : tableR) {
      List<Set<IntegerPair>> available2Grams = rec.get2Grams();
      int[] range = rec.getCandidateLengths(rec.size() - 1);
      int minIdx = -1;
      int minInvokes = Integer.MAX_VALUE;
      int searchmax = Math.min(range[0], maxIndex);
      for (int i = 0; i < searchmax; ++i) {
        Map<IntegerPair, Integer> curr_long_invokes = long_invokes.get(i);
        if (curr_long_invokes == null) {
          minIdx = i;
          minInvokes = 0;
          break;
        }
        int invoke = 0;
        for (IntegerPair twogram : available2Grams.get(i)) {
          Integer count = curr_long_invokes.get(twogram);
          if (count != null) invoke += count;
        }
        if (invoke < minInvokes) {
          minIdx = i;
          minInvokes = invoke;
        }
      }
      longpredictCount += minInvokes;

      Map<IntegerPair, List<IntIntRecordTriple>> curr_long_idx = long_idx
          .get(minIdx);
      if (curr_long_idx == null) {
        curr_long_idx = new WYK_HashMap<IntegerPair, List<IntIntRecordTriple>>();
        long_idx.put(minIdx, curr_long_idx);
      }
      for (IntegerPair twogram : available2Grams.get(minIdx)) {
        List<IntIntRecordTriple> list = curr_long_idx.get(twogram);
        if (list == null) {
          list = new ArrayList<IntIntRecordTriple>();
          curr_long_idx.put(twogram, list);
        }
        list.add(new IntIntRecordTriple(range[0], range[1], rec));
      }
      longelements += available2Grams.get(minIdx).size();

      // Build an index for the strings in S which has less or equal to
      // 'threshold' 1-expandable strings

      if (rec.getEstNumRecords() <= joinThreshold) continue;
      minIdx = -1;
      minInvokes = Integer.MAX_VALUE;
      for (int i = 0; i < searchmax; ++i) {
        Map<IntegerPair, Integer> curr_short_invokes = short_invokes.get(i);
        if (curr_short_invokes == null) {
          minIdx = i;
          minInvokes = 0;
          break;
        }
        int invoke = 0;
        for (IntegerPair twogram : available2Grams.get(i)) {
          Integer count = curr_short_invokes.get(twogram);
          if (count != null) invoke += count;
        }
        if (invoke < minInvokes) {
          minIdx = i;
          minInvokes = invoke;
        }
      }
      shortpredictCount += minInvokes;

      Map<IntegerPair, List<IntIntRecordTriple>> curr_short_idx = short_idx
          .get(minIdx);
      if (curr_short_idx == null) {
        curr_short_idx = new HashMap<IntegerPair, List<IntIntRecordTriple>>();
        short_idx.put(minIdx, curr_short_idx);
      }
      for (IntegerPair twogram : available2Grams.get(minIdx)) {
        List<IntIntRecordTriple> list = curr_short_idx.get(twogram);
        if (list == null) {
          list = new ArrayList<IntIntRecordTriple>();
          curr_short_idx.put(twogram, list);
        }
        list.add(new IntIntRecordTriple(range[0], range[1], rec));
      }
      shortelements += available2Grams.get(minIdx).size();
    }
    System.out.println("IntegerPair predict : " + longpredictCount);
    System.out.println("IntegerPair idx size : " + longelements);
    System.out.println("Short predict : " + shortpredictCount);
    System.out.println("Short idx size : " + shortelements);

    ///// Statistics
    int sum = 0;
    long count = 0;
    for (Map<IntegerPair, List<IntIntRecordTriple>> curr_long_idx : long_idx
        .values()) {
      for (List<IntIntRecordTriple> list : curr_long_idx.values()) {
        if (list.size() == 1) continue;
        sum++;
        count += list.size();
      }
    }
    System.out.println("long iIdx size : " + count);
    System.out.println("long Rec per idx : " + ((double) count) / sum);

    ///// Statistics
    sum = 0;
    count = 0;
    for (Map<IntegerPair, List<IntIntRecordTriple>> curr_short_idx : short_idx
        .values()) {
      for (List<IntIntRecordTriple> list : curr_short_idx.values()) {
        if (list.size() == 1) continue;
        sum++;
        count += list.size();
      }
    }
    System.out.println("short iIdx size : " + count);
    System.out.println("short Rec per idx : " + ((double) count) / sum);

    // Build 1-expanded set for every record in R
    count = 0;
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

    long time1 = System.currentTimeMillis();
    for (Record s : tableS) {
      if (s.getEstNumRecords() > joinThreshold)
        appliedRules_sum += searchEquivsByDynamicIndex(s, long_idx, rslt);
    }
    time1 = System.currentTimeMillis() - time1;

    long time2 = System.currentTimeMillis();
    for (Record s : tableS) {
      if (s.getEstNumRecords() > joinThreshold)
        continue;
      else
        appliedRules_sum += searchEquivsByDynamicIndex(s, short_idx, rslt);
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

  private void buildIndexSingleSide() {
    long elements = 0;
    long predictCount = 0;
    // Build an index
    // Count Invokes per each (twogram, loc) pair
    Map<Integer, Map<IntegerPair, Integer>> invokes = new WYK_HashMap<Integer, Map<IntegerPair, Integer>>();
    for (Record rec : tableS) {
      for (int i = 0; i < rec.size(); ++i) {
        Map<IntegerPair, Integer> curridx_invokes = invokes.get(i);
        if (curridx_invokes == null) {
          curridx_invokes = new WYK_HashMap<IntegerPair, Integer>();
          invokes.put(i, curridx_invokes);
        }
        IntegerPair twogram = rec.getOriginal2Gram(i);
        Integer count = curridx_invokes.get(twogram);
        if (count == null)
          count = 1;
        else
          count += 1;
        curridx_invokes.put(twogram, count);
      }
    }

    long_idx = new WYK_HashMap<Integer, Map<IntegerPair, List<IntIntRecordTriple>>>();
    for (Record rec : tableR) {
      List<Set<IntegerPair>> available2Grams = rec.get2Grams();
      int[] range = rec.getCandidateLengths(rec.size() - 1);
      int minIdx = -1;
      int minInvokes = Integer.MAX_VALUE;
      int searchmax = Math.min(range[0], maxIndex);
      for (int i = 0; i < searchmax; ++i) {
        int invoke = 0;
        Map<IntegerPair, Integer> curridx_invokes = invokes.get(i);
        // There is no invocation count: this is the minimum point
        if (curridx_invokes == null) {
          minIdx = i;
          minInvokes = 0;
          break;
        }
        for (IntegerPair twogram : available2Grams.get(i)) {
          Integer count = curridx_invokes.get(twogram);
          if (count != null) invoke += count;
        }
        if (invoke < minInvokes) {
          minIdx = i;
          minInvokes = invoke;
        }
      }

      predictCount += minInvokes;

      Map<IntegerPair, List<IntIntRecordTriple>> curridx = long_idx.get(minIdx);
      if (curridx == null) {
        curridx = new WYK_HashMap<IntegerPair, List<IntIntRecordTriple>>();
        long_idx.put(minIdx, curridx);
      }
      for (IntegerPair twogram : available2Grams.get(minIdx)) {
        List<IntIntRecordTriple> list = curridx.get(twogram);
        if (list == null) {
          list = new ArrayList<IntIntRecordTriple>();
          curridx.put(twogram, list);
        }
        list.add(new IntIntRecordTriple(range[0], range[1], rec));
      }
      elements += available2Grams.get(minIdx).size();
    }
    System.out.println("Predict : " + predictCount);
    System.out.println("Idx size : " + elements);

    ///// Statistics
    int sum = 0;
    long count = 0;
    for (Map<IntegerPair, List<IntIntRecordTriple>> curridx : long_idx
        .values()) {
      for (List<IntIntRecordTriple> list : curridx.values()) {
        if (list.size() == 1) continue;
        sum++;
        count += list.size();
      }
    }
    System.out.println("iIdx size : " + count);
    System.out.println("Rec per idx : " + ((double) count) / sum);
  }

  private WYK_HashSet<IntegerPair> joinSingleSide() {
    WYK_HashSet<IntegerPair> rslt = new WYK_HashSet<IntegerPair>();

    long appliedRules_sum = 0;
    for (Record recS : tableS) {
      int minlength = recS.getMinLength();
      int maxlength = recS.getMaxLength();
      for (int i = 0; i < recS.size(); ++i) {
        Map<IntegerPair, List<IntIntRecordTriple>> curridx = long_idx.get(i);
        if (curridx == null) continue;
        IntegerPair twogram = recS.getOriginal2Gram(i);
        List<Record> candidatesList = new ArrayList<Record>();
        List<IntIntRecordTriple> tree = curridx.get(twogram);

        if (tree == null) continue;
        for (IntIntRecordTriple e : tree)
          if (StaticFunctions.overlap(e.min, e.max, minlength, maxlength))
            candidatesList.add(e.rec);
        if (skipChecking) continue;
        for (Record recR : candidatesList) {
          int compare = checker.isEqual(recR, recS);
          if (compare >= 0) {
            rslt.add(new IntegerPair(recR.getID(), recS.getID()));
            appliedRules_sum += compare;
          }
        }
      }
    }
    System.out
        .println("Avg applied rules : " + appliedRules_sum + "/" + rslt.size());

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

  public void run(double sampleratio) {
    long startTime = System.currentTimeMillis();
    preprocess(compact, maxIndex, useAutomata);
    System.out.print("Preprocess finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));

    // Retrieve statistics
    statistics();

    // Compute optimal theta
    startTime = System.currentTimeMillis();
    findTheta(sampleratio);
    System.out.print("Estimation finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));
    System.exit(1);

    startTime = System.currentTimeMillis();
    if (singleside)
      buildIndexSingleSide();
    else
      buildIndex();
    System.out.print("Building Index finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));

    startTime = System.currentTimeMillis();
    Collection<IntegerPair> rslt = (singleside ? joinSingleSide() : join());
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
  private void findTheta(double sampleratio) {
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

    // Infer alpha and beta
    Naive1 naiveinst = new Naive1(this);
    Naive1.threshold = 1000;
    // naiveinst.runWithoutPreprocess();
    double alpha = naiveinst.alpha;
    double beta = naiveinst.beta;

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
    double gamma = joinmininst.gamma;
    double delta = joinmininst.delta;
    double epsilon = joinmininst.epsilon;
    System.out.println("Bigram computation time : " + Record.exectime);

    // Restore
    tableR = tmpR;
    tableS = tmpS;

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

    System.out.println("Alpha : " + alpha);
    System.out.println("Beta : " + beta);
    System.out.println("Gamma : " + gamma);
    System.out.println("Delta : " + delta);
    System.out.println("Epsilon : " + epsilon);

    // Find the best threshold
    int best_t = 0;
    long best_esttime = Long.MAX_VALUE;
    long[] best_esttimes = null;
    // Indicates the minimum indices which have more that 't' expanded records
    int ridx = 0;
    int sidx = 0;
    long totalSSigSize = 0;
    for (Record s : tableS) {
      List<Set<IntegerPair>> available2Grams = s.get2Grams();
      for (Set<IntegerPair> set : available2Grams)
        totalSSigSize += set.size();
    }
    // Prefix sums
    long currRLExpSize = 0;
    long currSLExpSize = 0;
    int currRHSize = tableR.size();
    int currRLSize = 0;
    long currSHSigSize = totalSSigSize;
    while (ridx < tableR.size() || sidx < tableS.size()) {
      // Find the next t
      Record r = ridx == tableR.size() ? null : tableR.get(ridx);
      Record s = sidx == tableS.size() ? null : tableS.get(sidx);
      long t = 0;
      if (r == null)
        t = s.getEstNumRecords();
      else if (s == null)
        t = r.getEstNumRecords();
      else
        t = Math.min(r.getEstNumRecords(), s.getEstNumRecords());
      if (t > Integer.MAX_VALUE) break;

      // Compute estimated time (with R)
      while (r != null) {
        long expSize = r.getEstNumRecords();
        if (expSize > t) break;
        currRLExpSize += expSize;
        --currRHSize;
        ++currRLSize;
        r = tableR.get(++ridx);
      }
      while (s != null) {
        long expSize = s.getEstNumRecords();
        if (expSize > t) break;
        currSLExpSize += expSize;
        List<Set<IntegerPair>> available2Grams = s.get2Grams();
        for (Set<IntegerPair> set : available2Grams)
          currSHSigSize -= set.size();
        s = tableS.get(++sidx);
      }
      long[] esttimes = new long[5];
      esttimes[0] = (long) (alpha * currRLExpSize);
      esttimes[1] = (long) (beta * currSLExpSize);
      esttimes[2] = (long) (gamma * currSHSigSize);
      esttimes[3] = (long) (epsilon * currRHSize * totalSSigSize);
      esttimes[4] = (long) (epsilon * currRLSize * currSHSigSize);
      long esttime = esttimes[0] + esttimes[1] + esttimes[2] + esttimes[3]
          + esttimes[4];
      if (esttime < best_esttime) {
        best_t = (int) t;
        best_esttime = esttime;
        best_esttimes = esttimes;
      }
      if (t == 10 || t == 100) {
        System.out.println("T=" + t + " : " + esttime);
        System.out.println(Arrays.toString(esttimes));
      }
    }
    System.out.print("Best threshold : " + best_t);
    System.out.println(" with running time " + best_esttime);
    System.out.println(Arrays.toString(best_esttimes));
    joinThreshold = best_t;
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
    Hybrid2GramWithOptTheta inst = new Hybrid2GramWithOptTheta(Rulefile, Rfile,
        Sfile);
    System.out.print("Constructor finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));
    inst.run(params.getSampleRatio());
    Validator.printStats();
  }
}
