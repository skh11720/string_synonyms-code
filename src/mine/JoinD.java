package mine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import tools.Algorithm;
import tools.IntegerMap;
import tools.IntegerSet;
import tools.Rule;
import tools.Rule_ACAutomata;
import tools.StaticFunctions;
import tools.WYK_HashSet;

public class JoinD extends Algorithm {
  boolean                                                skipChecking = false;
  public static int                                      a            = 3;
  ArrayList<Record>                                      tableR;
  ArrayList<Record>                                      tableS;
  ArrayList<Rule>                                        rulelist;
  RecordIDComparator                                     idComparator;

  /**
   * Key: token<br/>
   * Value IntervalTree Key: length of record (min, max)<br/>
   * Value IntervalTree Value: record
   */
  ArrayList<IntegerMap<IntervalTreeRW<Integer, Record>>> idx;

  protected JoinD(String rulefile, String Rfile, String Sfile)
      throws IOException {
    super(rulefile, Rfile, Sfile);
    int size = 1000000;

    readRules(rulefile);
    Record.setStrList(strlist);
    tableR = readRecords(Rfile, size);
    tableS = readRecords(Sfile, size);
    idComparator = new RecordIDComparator();
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

  private void preprocess() {
    Rule_ACAutomata automata = new Rule_ACAutomata(rulelist);

    long currentTime = System.currentTimeMillis();
    // Preprocess each records in R
    for (Record rec : tableR) {
      rec.preprocessRules(automata, true);
    }
    long time = System.currentTimeMillis() - currentTime;
    System.out.println("Preprocess rules : " + time);

    currentTime = System.currentTimeMillis();
    for (Record rec : tableR) {
      rec.preprocessLengths();
    }
    time = System.currentTimeMillis() - currentTime;
    System.out.println("Preprocess lengths: " + time);

    currentTime = System.currentTimeMillis();
    for (Record rec : tableR) {
      rec.preprocessAvailableTokens(a);
    }
    time = System.currentTimeMillis() - currentTime;
    System.out.println("Preprocess available tokens: " + time);

    currentTime = System.currentTimeMillis();
    for (Record rec : tableR) {
      rec.preprocessEstimatedRecords();
    }
    time = System.currentTimeMillis() - currentTime;
    System.out.println("Preprocess est records: " + time);

    // Preprocess each records in S
    for (Record rec : tableS) {
      rec.preprocessRules(automata, true);
      rec.preprocessLengths();
      rec.preprocessAvailableTokens(a);
      rec.preprocessEstimatedRecords();
    }
  }

  private void buildIndex() {
    long elements = 0;
    // Build an index

    idx = new ArrayList<IntegerMap<IntervalTreeRW<Integer, Record>>>();
    for (int i = 0; i < a; ++i)
      idx.add(new IntegerMap<IntervalTreeRW<Integer, Record>>());
    for (Record rec : tableR) {
      IntegerSet[] availableTokens = rec.getAvailableTokens();
      int[] range = rec.getCandidateLengths(rec.size() - 1);
      int boundary = Math.min(range[1], a);
      for (int i = 0; i < boundary; ++i) {
        IntegerMap<IntervalTreeRW<Integer, Record>> map = idx.get(i);
        for (int token : availableTokens[i]) {
          IntervalTreeRW<Integer, Record> list = map.get(token);
          if (list == null) {
            list = new IntervalTreeRW<Integer, Record>();
            map.put(token, list);
          }
          list.insert(range[0], range[1], rec);
        }
        elements += availableTokens[i].size();
      }
    }
    System.out.println("Idx size : " + elements);

    // Statistics
    int sum = 0;
    long count = 0;
    for (IntegerMap<IntervalTreeRW<Integer, Record>> map : idx) {
      for (IntervalTreeRW<Integer, Record> list : map.values()) {
        if (list.size() == 1) continue;
        sum++;
        count += list.size();
      }
    }
    System.out.println("iIdx size : " + count);
    System.out.println("Rec per idx : " + ((double) count) / sum);
  }

  private WYK_HashSet<IntegerPair> join() {
    WYK_HashSet<IntegerPair> rslt = new WYK_HashSet<IntegerPair>();
    int count = 0;

    for (Record recS : tableS) {
      List<List<Record>> candidatesList = new ArrayList<List<Record>>();
      IntegerSet[] availableTokens = recS.getAvailableTokens();
      int[] range = recS.getCandidateLengths(recS.size() - 1);
      int boundary = Math.min(range[0], a);
      for (int i = 0; i < boundary; ++i) {
        List<List<Record>> ithCandidates = new ArrayList<List<Record>>();
        IntegerMap<IntervalTreeRW<Integer, Record>> map = idx.get(i);
        for (int token : availableTokens[i]) {
          IntervalTreeRW<Integer, Record> tree = map.get(token);
          if (tree == null) continue;
          List<Record> candidates = tree.search(range[0], range[1]);
          Collections.sort(candidates, idComparator);
          ithCandidates.add(candidates);
        }
        candidatesList.add(StaticFunctions.union(ithCandidates, idComparator));
      }
      List<Record> candidates = StaticFunctions.intersection(candidatesList,
          idComparator);
      count += candidates.size();

      if(skipChecking) continue;
      for (Record recR : candidates) {
        int compare = Validator.DP_A_Queue_useACAutomata(recR, recS, true);
        if (compare >= 0) rslt.add(new IntegerPair(recR.getID(), recS.getID()));
      }

    }
    System.out.println("comparisions : " + count);

    return rslt;
  }

  @SuppressWarnings("unused")
  private List<Record> mergeCandidatesWithHashSet(
      List<ArrayList<Record>> list) {
    IntegerMap<Record> set = new IntegerMap<Record>();
    for (ArrayList<Record> candidates : list)
      for (Record rec : candidates)
        set.put(rec.getID(), rec);
    List<Record> candidates = new ArrayList<Record>();
    for (Record rec : set.values())
      candidates.add(rec);
    return candidates;
  }

  public void run() {
    long startTime = System.currentTimeMillis();
    preprocess();
    System.out.print("Preprocess finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));

    startTime = System.currentTimeMillis();
    buildIndex();
    System.out.print("Building Index finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));

    startTime = System.currentTimeMillis();
    WYK_HashSet<IntegerPair> rslt = join();
    System.out.print("Join finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));
    System.out.println(rslt.size());
    System.out.println("Comparisons: " + Validator.checked);
    System.out.println("Filtered: " + Validator.filtered);

    try {
      BufferedWriter bw = new BufferedWriter(new FileWriter("rslt.txt"));
      HashMap<Integer, ArrayList<Record>> tmp = new HashMap<Integer, ArrayList<Record>>();
      for (IntegerPair ip : rslt) {
        if (!tmp.containsKey(ip.i1)) tmp.put(ip.i1, new ArrayList<Record>());
        if (ip.i1 != ip.i2) tmp.get(ip.i1).add(tableS.get(ip.i2));
      }
      for (int i = 0; i < tableR.size(); ++i) {
        if (!tmp.containsKey(i) || tmp.get(i).size() == 0) continue;
        bw.write(tableR.get(i).toString() + "\t");
        bw.write(tmp.get(i).toString() + "\n");
      }
      bw.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public static void main(String[] args) throws IOException {
    if (args.length != 4 && args.length != 5) {
      printUsage();
      return;
    }
    String Rfile = args[0];
    String Sfile = args[1];
    String Rulefile = args[2];
    JoinD.a = Integer.parseInt(args[3]);
    boolean skipChecking = args.length == 5;

    long startTime = System.currentTimeMillis();
    JoinD inst = new JoinD(Rulefile, Rfile, Sfile);
    inst.skipChecking = skipChecking;
    System.out.print("Constructor finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));
    inst.run();
  }

  private static void printUsage() {
    System.out.println("Usage : <R file> <S file> <Rule file> <a>");
  }
}
