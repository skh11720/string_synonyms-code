package mine;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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

public class JoinH2GramNoIntervalTree extends Algorithm {
  public static boolean                                    useAutomata  = false;
  public static boolean                                    skipChecking = false;
  public static int                                        maxIndex     = Integer.MAX_VALUE;
  public static boolean                                    compact      = true;
  public static boolean                                    singleside   = false;
  public static boolean                                    exact2grams  = false;

  RecordIDComparator                                       idComparator;
  RuleTrie                                                 ruletrie;

  public static String                                     outputfile;

  public static Validator                                  checker;
  /**
   * Key: (2gram, index) pair<br/>
   * Value: (min, max, record) triple
   */
  Map<Integer, Map<IntegerPair, List<IntIntRecordTriple>>> idx;
  // Map<IntegerPairIntPair, List<IntIntRecordTriple>> idx;

  public long                                              buildIndexTime;
  public long                                              joinTime;

  public JoinH2GramNoIntervalTree(String rulefile, String Rfile, String Sfile)
      throws IOException {
    super(rulefile, Rfile, Sfile);

    Record.setStrList(strlist);
    idComparator = new RecordIDComparator();
    ruletrie = new RuleTrie(rulelist);
    Record.setRuleTrie(ruletrie);
  }

  public JoinH2GramNoIntervalTree(Algorithm o) {
    super(o);

    Record.setStrList(strlist);
    idComparator = new RecordIDComparator();
    ruletrie = new RuleTrie(rulelist);
    Record.setRuleTrie(ruletrie);
  }

  private void buildIndex() throws IOException {
    long elements = 0;
    long predictCount = 0;
    long starttime = System.currentTimeMillis();
    // Build an index
    // Count Invokes per each (token, loc) pair
    Map<Integer, Map<IntegerPair, Integer>> invokes = new WYK_HashMap<Integer, Map<IntegerPair, Integer>>();
    // Map<IntegerPairIntPair, Integer> invokes = new
    // HashMap<IntegerPairIntPair, Integer>();
    for (Record rec : tableS) {
      List<Set<IntegerPair>> available2Grams = exact2grams
          ? rec.getExact2Grams() : rec.get2Grams();
      int searchmax = Math.min(available2Grams.size(), maxIndex);
      for (int i = 0; i < searchmax; ++i) {
        Map<IntegerPair, Integer> curridx_invokes = invokes.get(i);
        if (curridx_invokes == null) {
          curridx_invokes = new WYK_HashMap<IntegerPair, Integer>();
          invokes.put(i, curridx_invokes);
        }
        for (IntegerPair twogram : available2Grams.get(i)) {
          Integer count = curridx_invokes.get(twogram);
          if (count == null)
            count = 1;
          else
            count += 1;
          curridx_invokes.put(twogram, count);
        }
      }
    }

    long duration = System.currentTimeMillis() - starttime;
    System.out.println("Step 1 : " + duration);
    starttime = System.currentTimeMillis();

    idx = new WYK_HashMap<Integer, Map<IntegerPair, List<IntIntRecordTriple>>>();
    // idx = new HashMap<Integer, Map<IntegerPair, List<IntIntRecordTriple>>>();

    // BufferedOutputStream bw = new BufferedOutputStream(
    // new FileOutputStream("asdf"));
    for (Record rec : tableR) {
      List<Set<IntegerPair>> available2Grams = exact2grams
          ? rec.getExact2Grams() : rec.get2Grams();
      int[] range = rec.getCandidateLengths(rec.size() - 1);
      int minIdx = -1;
      int minInvokes = Integer.MAX_VALUE;
      int searchmax = Math.min(range[0], maxIndex);
      for (int i = 0; i < searchmax; ++i) {
        if (available2Grams.get(i).isEmpty()) continue;
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

      Map<IntegerPair, List<IntIntRecordTriple>> curridx = idx.get(minIdx);
      if (curridx == null) {
        curridx = new WYK_HashMap<IntegerPair, List<IntIntRecordTriple>>(
            1000000);
        // curridx = new HashMap<IntegerPair, List<IntIntRecordTriple>>();
        idx.put(minIdx, curridx);
      }
      for (IntegerPair twogram : available2Grams.get(minIdx)) {
        // write2File(bw, minIdx, twogram, rec.getID());
        if (true) {
          List<IntIntRecordTriple> list = curridx.get(twogram);
          if (list == null) {
            list = new ArrayList<IntIntRecordTriple>();
            curridx.put(twogram, list);
          }
          list.add(new IntIntRecordTriple(range[0], range[1], rec));
        }
      }
      elements += available2Grams.get(minIdx).size();
    }
    // bw.close();
    System.out.println("Predict : " + predictCount);
    System.out.println("Idx size : " + elements);
    duration = System.currentTimeMillis() - starttime;
    System.out.println("Step 2 : " + duration);

    int sum = 0;
    int ones = 0;
    long count = 0;
    ///// Statistics
    for (Map<IntegerPair, List<IntIntRecordTriple>> curridx : idx.values()) {
      WYK_HashMap<IntegerPair, List<IntIntRecordTriple>> tmp = (WYK_HashMap<IntegerPair, List<IntIntRecordTriple>>) curridx;
      if (sum == 0) tmp.printStat();
      for (List<IntIntRecordTriple> list : curridx.values()) {
        if (list.size() == 1) {
          ++ones;
          continue;
        }
        sum++;
        count += list.size();
      }
    }
    System.out.println("key-value pairs(all) : " + (sum + ones));
    System.out.println("iIdx size(all) : " + (count + ones));
    System.out.println(
        "Rec per idx(all) : " + ((double) (count + ones)) / (sum + ones));
    System.out.println("key-value pairs(w/o 1) : " + sum);
    System.out.println("iIdx size(w/o 1) : " + count);
    System.out.println("Rec per idx(w/o 1) : " + ((double) count) / sum);
    System.out.println("2Gram retrieval: " + Record.exectime);
    ///// Statistics
    sum = 0;
    ones = 0;
    count = 0;
    for (Map<IntegerPair, Integer> curridx : invokes.values()) {
      WYK_HashMap<IntegerPair, Integer> tmp = (WYK_HashMap<IntegerPair, Integer>) curridx;
      if (sum == 0) tmp.printStat();
      for (Entry<IntegerPair, Integer> list : curridx.entrySet()) {
        if (list.getValue() == 1) {
          ++ones;
          continue;
        }
        sum++;
        count += list.getValue();
      }
    }
    System.out.println("key-value pairs(all) : " + (sum + ones));
    System.out.println("iIdx size(all) : " + (count + ones));
    System.out.println(
        "Rec per idx(all) : " + ((double) (count + ones)) / (sum + ones));
    System.out.println("key-value pairs(w/o 1) : " + sum);
    System.out.println("iIdx size(w/o 1) : " + count);
    System.out.println("Rec per idx(w/o 1) : " + ((double) count) / sum);
  }

  static ByteBuffer buffer = ByteBuffer.allocate(16);

  private static void write2File(BufferedOutputStream bos, int idx,
      IntegerPair twogram, int id) throws IOException {
    buffer.clear();
    buffer.putInt(idx);
    buffer.putInt(twogram.i1);
    buffer.putInt(twogram.i2);
    buffer.putInt(id);
    bos.write(buffer.array());
  }

  private List<IntegerPair> join() {
    List<IntegerPair> rslt = new ArrayList<IntegerPair>();

    long appliedRules_sum = 0;
    for (Record recS : tableS) {
      List<Set<IntegerPair>> available2Grams = exact2grams
          ? recS.getExact2Grams() : recS.get2Grams();
      int[] range = recS.getCandidateLengths(recS.size() - 1);
      int searchmax = Math.min(available2Grams.size(), maxIndex);
      for (int i = 0; i < searchmax; ++i) {
        Map<IntegerPair, List<IntIntRecordTriple>> curridx = idx.get(i);
        if (curridx == null) continue;
        List<List<Record>> candidatesList = new ArrayList<List<Record>>();
        for (IntegerPair twogram : available2Grams.get(i)) {
          List<IntIntRecordTriple> tree = curridx.get(twogram);

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

    idx = new WYK_HashMap<Integer, Map<IntegerPair, List<IntIntRecordTriple>>>();
    for (Record rec : tableR) {
      List<Set<IntegerPair>> available2Grams = exact2grams
          ? rec.getExact2Grams() : rec.get2Grams();
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

      Map<IntegerPair, List<IntIntRecordTriple>> curridx = idx.get(minIdx);
      if (curridx == null) {
        curridx = new WYK_HashMap<IntegerPair, List<IntIntRecordTriple>>();
        idx.put(minIdx, curridx);
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
    for (Map<IntegerPair, List<IntIntRecordTriple>> curridx : idx.values()) {
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
        Map<IntegerPair, List<IntIntRecordTriple>> curridx = idx.get(i);
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
    long startTime = System.nanoTime();
    preprocess(compact, maxIndex, useAutomata);
    System.out.print("Preprocess finished");
    System.out.println(" " + (System.nanoTime() - startTime));

    // Retrieve statistics
    statistics();

    startTime = System.nanoTime();
    if (singleside)
      buildIndexSingleSide();
    else
      try {
        buildIndex();
      } catch (Exception e) {
        e.printStackTrace();
        System.exit(0);
      }
    buildIndexTime = System.nanoTime() - startTime;
    System.out.println("Building Index finished " + buildIndexTime);

    startTime = System.nanoTime();
    Collection<IntegerPair> rslt = (singleside ? joinSingleSide() : join());
    joinTime = System.nanoTime() - startTime;
    System.out.println("Join finished " + joinTime + " ns");
    System.out.println(rslt.size());

    try {
      BufferedWriter bw = new BufferedWriter(new FileWriter(outputfile));
      for (IntegerPair ip : rslt) {
        Record r = tableR.get(ip.i1);
        Record s = tableS.get(ip.i2);
        if (!r.equals(s)) bw.write(tableR.get(ip.i1).toString(strlist)
            + "\t==\t" + tableS.get(ip.i2).toString(strlist) + "\n");
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
    compact = params.isCompact();
    checker = params.getValidator();
    exact2grams = params.isExact2Grams();

    long startTime = System.currentTimeMillis();
    JoinH2GramNoIntervalTree inst = new JoinH2GramNoIntervalTree(Rulefile,
        Rfile, Sfile);
    System.out.print("Constructor finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));
    inst.run();

    Validator.printStats();
  }
}
