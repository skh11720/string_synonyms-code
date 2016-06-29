package mine;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import tools.Algorithm;
import tools.IntegerPair;
import tools.RuleTrie;
import tools.Rule_ACAutomata;
import tools.StaticFunctions;
import tools.WYK_HashMap;

/**
 * Expand from both sides
 */
public class Naive1 extends Algorithm {
  /**
   * Store the original index from expanded string
   */
  Map<Record, ArrayList<Integer>> rec2idx;
  Rule_ACAutomata                 automata;
  RuleTrie                        ruletrie;

  public static long              threshold = Long.MAX_VALUE;
  public static boolean           skipequiv = false;

  public long                     buildIndexTime;
  public long                     joinTime;
  public double                   alpha;
  public double                   beta;

  protected Naive1(String rulefile, String Rfile, String Sfile)
      throws IOException {
    super(rulefile, Rfile, Sfile);
    automata = new Rule_ACAutomata(rulelist);
    ruletrie = new RuleTrie(rulelist);
  }

  public Naive1(Algorithm o) {
    super(o);
    automata = new Rule_ACAutomata(rulelist);
    ruletrie = new RuleTrie(rulelist);
  }

  private void preprocess() {
    for (Record r : tableR) {
      r.preprocessRules(automata, false);
      r.preprocessEstimatedRecords();
    }
    for (Record s : tableS) {
      s.preprocessRules(automata, false);
      s.preprocessEstimatedRecords();
    }
  }

  private void buildIndex() {
    rec2idx = new WYK_HashMap<Record, ArrayList<Integer>>(1000000);
    long starttime = System.nanoTime();
    long totalExpSize = 0;
    int count = 0;
    for (int i = 0; i < tableR.size(); ++i) {
      Record recR = tableR.get(i);
      long est = recR.getEstNumRecords();
      if (est > threshold) continue;
      List<Record> expanded = recR.expandAll(ruletrie);
      assert (expanded.size() <= threshold);
      totalExpSize += expanded.size();
      for (Record exp : expanded) {
        ArrayList<Integer> list = rec2idx.get(exp);
        if (list == null) {
          list = new ArrayList<Integer>(5);
          rec2idx.put(exp, list);
        }
        // If current list already contains current record, skip adding
        if (!list.isEmpty() && list.get(list.size() - 1) == i) continue;
        list.add(i);
      }
      ++count;
    }
    long idxsize = 0;
    for (List<Integer> list : rec2idx.values())
      idxsize += list.size();
    System.out.println(count + " records are indexed");
    System.out.println("Total index size: " + idxsize);
    // ((WYK_HashMap<Record, ArrayList<Integer>>) rec2idx).printStat();
    long duration = System.nanoTime() - starttime;
    alpha = ((double) duration) / totalExpSize;
  }

  private class IntegerComparator implements Comparator<Integer> {
    @Override
    public int compare(Integer o1, Integer o2) {
      return o1.compareTo(o2);
    }
  }

  private List<IntegerPair> join() {
    List<IntegerPair> rslt = new ArrayList<IntegerPair>();
    long starttime = System.nanoTime();
    long totalExpSize = 0;

    for (int idxS = 0; idxS < tableS.size(); ++idxS) {
      Record recS = tableS.get(idxS);
      long est = recS.getEstNumRecords();
      if (est > threshold) continue;
      List<Record> expanded = recS.expandAll(ruletrie);
      totalExpSize += expanded.size();
      List<List<Integer>> candidates = new ArrayList<List<Integer>>(
          expanded.size() * 2);
      for (Record exp : expanded) {
        List<Integer> overlapidx = rec2idx.get(exp);
        if (overlapidx == null) continue;
        candidates.add(overlapidx);
      }
      if (!skipequiv) {
        List<Integer> union = StaticFunctions.union(candidates,
            new IntegerComparator());
        for (Integer idx : union)
          rslt.add(new IntegerPair(idx, idxS));
      }
    }

    long duration = System.nanoTime() - starttime;
    beta = ((double) duration) / totalExpSize;

    return rslt;
  }

  public void run() {
    long startTime = System.nanoTime();
    preprocess();
    buildIndexTime = System.nanoTime() - startTime;
    System.out.println("Preprocess finished " + buildIndexTime);

    List<RecordPair> list = runWithoutPreprocess();
    try {
      BufferedWriter bw = new BufferedWriter(new FileWriter("rslt.txt"));
      for (RecordPair rp : list) {
        Record s = rp.record1;
        Record t = rp.record2;
        if (!s.equals(t)) bw.write(s.toString() + " == " + t.toString() + "\n");
      }
      bw.close();
    } catch (IOException e) {
    }
  }

  public List<RecordPair> runWithoutPreprocess() {
    long startTime = System.nanoTime();
    buildIndex();
    buildIndexTime = System.nanoTime() - startTime;
    System.out.println("Building Index finished " + buildIndexTime);
    startTime = System.nanoTime();
    List<IntegerPair> rslt = join();
    joinTime = System.nanoTime() - startTime;
    System.out.println("Join finished " + joinTime + " ns");
    System.out.println(rslt.size());
    System.out.println("Union counter: " + StaticFunctions.union_cmp_counter);
    System.out
        .println("Equals counter: " + StaticFunctions.compare_cmp_counter);

    List<RecordPair> rlist = new ArrayList<RecordPair>();
    for (IntegerPair ip : rslt) {
      Record r = tableR.get(ip.i1);
      Record s = tableS.get(ip.i2);
      rlist.add(new RecordPair(r, s));
    }
    Collections.sort(rlist);
    return rlist;
  }

  public void clearIndex() {
    if (rec2idx != null) rec2idx.clear();
    rec2idx = null;
  }

  public static void main(String[] args) throws IOException {
    if (args.length != 4) {
      printUsage();
      return;
    }
    String Rfile = args[0];
    String Sfile = args[1];
    String Rulefile = args[2];
    Naive1.threshold = Integer.valueOf(args[3]);

    long startTime = System.currentTimeMillis();
    Naive1 inst = new Naive1(Rulefile, Rfile, Sfile);
    System.out.print("Constructor finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));
    inst.run();
  }

  private static void printUsage() {
    System.out.println("Usage : <R file> <S file> <Rule file> <exp threshold>");
  }
}
