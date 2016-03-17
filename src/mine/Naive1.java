package mine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import tools.Algorithm;
import tools.IntegerPair;
import tools.Rule;
import tools.RuleTrie;
import tools.Rule_ACAutomata;
import tools.StaticFunctions;

/**
 * Expand from both sides
 */
public class Naive1 extends Algorithm {
  ArrayList<Record>                   tableR;
  /**
   * Store the original index from expanded string
   */
  HashMap<Record, ArrayList<Integer>> rec2idx;
  ArrayList<Record>                   tableS;
  ArrayList<Rule>                     rulelist;
  Rule_ACAutomata                     automata;
  RuleTrie                            ruletrie;

  static int                          threshold = 1000;

  protected Naive1(String rulefile, String Rfile, String Sfile)
      throws IOException {
    super(rulefile, Rfile, Sfile);
    int size = -1;

    readRules(rulefile);
    Record.setStrList(strlist);
    tableR = readRecords(Rfile, size);
    tableS = readRecords(Sfile, size);
    automata = new Rule_ACAutomata(rulelist);
    ruletrie = new RuleTrie(rulelist);
  }

  private void Init() {
    rec2idx = new HashMap<Record, ArrayList<Integer>>();
    int count = 0;
    for (int i = 0; i < tableR.size(); ++i) {
      Record recR = tableS.get(i);
      recR.preprocessRules(automata, false);
      recR.preprocessEstimatedRecords();
      long est = recR.getEstNumRecords();
      if (est > threshold) continue;
      List<Record> expanded = recR.expandAll(ruletrie);
      assert (expanded.size() <= threshold);
      for (Record exp : expanded) {
        if (!rec2idx.containsKey(exp))
          rec2idx.put(exp, new ArrayList<Integer>(5));
        ArrayList<Integer> list = rec2idx.get(exp);
        assert (list != null);
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
  }

  private void readRules(String Rulefile) throws IOException {
    rulelist = new ArrayList<Rule>();
    BufferedReader br = new BufferedReader(new FileReader(Rulefile));
    String line;
    while ((line = br.readLine()) != null) {
      rulelist.add(new Rule(line, str2int));
    }
    br.close();

    // Add Self rule
    for (int token : str2int.values())
      rulelist.add(new Rule(token, token));
  }

  private ArrayList<Record> readRecords(String DBfile, int num)
      throws IOException {
    ArrayList<Record> rslt = new ArrayList<Record>();
    BufferedReader br = new BufferedReader(new FileReader(DBfile));
    String line;
    while ((line = br.readLine()) != null && num != 0) {
      rslt.add(new Record(rslt.size(), line, str2int));
      --num;
    }
    br.close();
    return rslt;
  }

  private class IntegerComparator implements Comparator<Integer> {
    @Override
    public int compare(Integer o1, Integer o2) {
      return o1.compareTo(o2);
    }
  }

  private List<IntegerPair> join() {
    ArrayList<IntegerPair> rslt = new ArrayList<IntegerPair>();

    for (int idxS = 0; idxS < tableS.size(); ++idxS) {
      Record recS = tableS.get(idxS);
      recS.preprocessRules(automata, false);
      recS.preprocessEstimatedRecords();
      long est = recS.getEstNumRecords();
      if (est > threshold) continue;
      ArrayList<Record> expanded = recS.expandAll(ruletrie);
      ArrayList<List<Integer>> candidates = new ArrayList<List<Integer>>();
      for (Record exp : expanded) {
        if (!rec2idx.containsKey(exp)) continue;
        ArrayList<Integer> overlapidx = rec2idx.get(exp);
        candidates.add(overlapidx);
      }
      List<Integer> union = StaticFunctions.union(candidates,
          new IntegerComparator());
      for (Integer idx : union)
        if (idx != idxS) rslt.add(new IntegerPair(idx, idxS));
    }

    return rslt;
  }

  public void run() {
    long startTime = System.currentTimeMillis();
    Init();
    System.out.print("Building Index finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));
    startTime = System.currentTimeMillis();
    List<IntegerPair> rslt = join();
    System.out.print("Join finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));
    System.out.println(rslt.size());

    try {
      BufferedWriter bw = new BufferedWriter(new FileWriter("rslt.txt"));
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
