package mine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tools.Algorithm;
import tools.IntegerPair;
import tools.IntegerSet;
import tools.Rule;
import tools.Rule_ACAutomata;
import tools.StaticFunctions;
import tools.WYK_HashMap;
import tools.WYK_HashSet;

public class JoinH extends Algorithm {
  boolean                                                   skipChecking = false;
  boolean                                                   useAutomata  = true;
  ArrayList<Record>                                         tableR;
  ArrayList<Record>                                         tableS;
  ArrayList<Rule>                                           rulelist;
  RecordIDComparator                                        idComparator;

  /**
   * Key: (token, index) pair<br/>
   * Value IntervalTree Key: length of record (min, max)
   * Value IntervalTree Value: record
   */
  WYK_HashMap<IntegerPair, IntervalTreeRW<Integer, Record>> idx;

  protected JoinH(String rulefile, String Rfile, String Sfile)
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
      rec.preprocessRules(automata, useAutomata);
    }
    long time = System.currentTimeMillis() - currentTime;
    System.out.println("Preprocess rules : " + time);

    currentTime = System.currentTimeMillis();
    for (Record rec : tableR) {
      rec.preprocessLengths();
    }
    time = System.currentTimeMillis() - currentTime;
    System.out.println("Preprocess rules : " + time);

    currentTime = System.currentTimeMillis();
    for (Record rec : tableR) {
      rec.preprocessAvailableTokens(Integer.MAX_VALUE);
    }
    time = System.currentTimeMillis() - currentTime;
    System.out.println("Preprocess rules : " + time);

    currentTime = System.currentTimeMillis();
    for (Record rec : tableR) {
      rec.preprocessEstimatedRecords();
    }
    time = System.currentTimeMillis() - currentTime;
    System.out.println("Preprocess rules : " + time);

    // Preprocess each records in S
    for (Record rec : tableS) {
      rec.preprocessRules(automata, useAutomata);
      rec.preprocessLengths();
      rec.preprocessAvailableTokens(Integer.MAX_VALUE);
      rec.preprocessEstimatedRecords();
    }
  }

  private void buildIndex() {
    long elements = 0;
    long predictCount = 0;
    // Build an index
    // Count Invokes per each (token, loc) pair
    WYK_HashMap<IntegerPair, Integer> invokes = new WYK_HashMap<IntegerPair, Integer>();
    for (Record rec : tableS) {
      IntegerSet[] availableTokens = rec.getAvailableTokens();
      for (int i = 0; i < availableTokens.length; ++i) {
        for (int token : availableTokens[i]) {
          IntegerPair ip = new IntegerPair(token, i);
          Integer count = invokes.get(ip);
          if (count == null)
            count = 1;
          else
            count += 1;
          invokes.put(ip, count);
        }
      }
    }

    idx = new WYK_HashMap<IntegerPair, IntervalTreeRW<Integer, Record>>();
    for (Record rec : tableR) {
      IntegerSet[] availableTokens = rec.getAvailableTokens();
      int[] range = rec.getCandidateLengths(rec.size() - 1);
      int minlength = range[0];
      int minIdx = -1;
      int minInvokes = Integer.MAX_VALUE;
      for (int i = 0; i < minlength; ++i) {
        int invoke = 0;
        for (int token : availableTokens[i]) {
          IntegerPair ip = new IntegerPair(token, i);
          Integer count = invokes.get(ip);
          if (count != null) invoke += count;
        }
        if (invoke < minInvokes) {
          minIdx = i;
          minInvokes = invoke;
        }
      }

      predictCount += minInvokes;

      for (int token : availableTokens[minIdx]) {
        IntegerPair ip = new IntegerPair(token, minIdx);
        IntervalTreeRW<Integer, Record> list = idx.get(ip);
        if (list == null) {
          list = new IntervalTreeRW<Integer, Record>();
          idx.put(ip, list);
        }
        list.insert(range[0], range[1], rec);
      }
      elements += availableTokens[minIdx].size();
    }
    System.out.println("Predict : " + predictCount);
    System.out.println("Idx size : " + elements);

    ///// Statistic
    System.out.println("iIdx key-value pairs: " + idx.size());
    int sum = 0;
    int singlelistsize = 0;
    long count = 0;
    long sqsum = 0;
    for (IntervalTreeRW<Integer, Record> list : idx.values()) {
      sqsum += list.size() * list.size();
      if (list.size() == 1) {
        ++singlelistsize;
        continue;
      }
      sum++;
      count += list.size();
    }
    System.out.println("Single value list size : " + singlelistsize);
    System.out.println("iIdx size : " + count);
    System.out.println("Rec per idx : " + ((double) count) / sum);
    System.out.println("Sqsum : " + sqsum);
  }

  private WYK_HashSet<IntegerPair> join() {
    WYK_HashSet<IntegerPair> rslt = new WYK_HashSet<IntegerPair>();

    long cand_sum = 0;
    int count_cand = 0;
    int count_empty = 0;
    long sum = 0;
    long appliedRules_sum = 0;
    for (Record recS : tableS) {
      IntegerSet[] availableTokens = recS.getAvailableTokens();
      for (IntegerSet set : availableTokens) {
        sum += set.size();
      }

      int[] range = recS.getCandidateLengths(recS.size() - 1);
      for (int i = 0; i < availableTokens.length; ++i) {
        List<ArrayList<Record>> candidatesList = new ArrayList<ArrayList<Record>>();
        for (int token : availableTokens[i]) {
          IntegerPair ip = new IntegerPair(token, i);
          IntervalTreeRW<Integer, Record> tree = idx.get(ip);

          if (tree == null) {
            ++count_empty;
            continue;
          }
          ++count_cand;
          cand_sum += tree.size();
          if (range.length > 0) continue;
          ArrayList<Record> candidates = tree.search(range[0], range[1]);
          Collections.sort(candidates, idComparator);
          candidatesList.add(candidates);
        }
        List<Record> candidates = StaticFunctions.union(candidatesList,
            idComparator);
        if (skipChecking) continue;
        for (Record recR : candidates) {
          int compare = Validator.DP_A_Queue_useACAutomata(recR, recS, true);
          if (compare >= 0) {
            rslt.add(new IntegerPair(recR.getID(), recS.getID()));
            appliedRules_sum += compare;
          }
        }
      }
    }
    System.out.println("th Key membership check : " + sum);
    System.out.println("Avg candidates : " + cand_sum + "/" + count_cand);
    System.out.println("Empty candidates : " + count_empty);
    System.out
        .println("Avg applied rules : " + appliedRules_sum + "/" + rslt.size());
    return rslt;
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
    if (args.length != 3 && args.length != 4) {
      printUsage();
      return;
    }
    String Rfile = args[0];
    String Sfile = args[1];
    String Rulefile = args[2];
    boolean skipChecking = args.length == 4;

    long startTime = System.currentTimeMillis();
    JoinH inst = new JoinH(Rulefile, Rfile, Sfile);
    inst.skipChecking = skipChecking;
    System.out.print("Constructor finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));
    inst.run();
  }

  private static void printUsage() {
    System.out.println("Usage : <R file> <S file> <Rule file>");
  }
}
