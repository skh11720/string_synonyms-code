package mine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import tools.Algorithm;
import tools.IntIntRecordTriple;
import tools.IntegerMap;
import tools.IntegerPair;
import tools.IntegerSet;
import tools.Rule;
import tools.Rule_ACAutomata;
import tools.StaticFunctions;
import tools.WYK_HashSet;

public class JoinBNoIntervalTree extends Algorithm {
  static boolean                    skipChecking = false;
  static boolean                    useAutomata  = true;
  static boolean                    compact      = false;
  ArrayList<Record>                 tableR;
  ArrayList<Record>                 tableS;
  ArrayList<Rule>                   rulelist;
  RecordIDComparator                idComparator;

  /**
   * Key: token<br/>
   * Value IntervalTree Key: length of record (min, max)<br/>
   * Value IntervalTree Value: record
   */
  IntegerMap<ArrayList<IntIntRecordTriple>> idx;

  protected JoinBNoIntervalTree(String rulefile, String Rfile, String Sfile)
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
    System.out.println("Preprocess lengths: " + time);

    currentTime = System.currentTimeMillis();
    for (Record rec : tableR) {
      rec.preprocessAvailableTokens(1);
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
      rec.preprocessRules(automata, useAutomata);
      rec.preprocessLengths();
      rec.preprocessAvailableTokens(1);
      rec.preprocessEstimatedRecords();
    }
  }

  private void buildIndex() {
    long elements = 0;
    // Build an index

    idx = new IntegerMap<ArrayList<IntIntRecordTriple>>();
    for (Record rec : tableR) {
      // All available tokens at the first position
      IntegerSet[] availableTokens = rec.getAvailableTokens();
      // All available equivalent string lengths
      int[] range = rec.getCandidateLengths(rec.size() - 1);
      for (int token : availableTokens[0]) {
        ArrayList<IntIntRecordTriple> list = idx.get(token);
        if (list == null) {
          list = new ArrayList<IntIntRecordTriple>();
          idx.put(token, list);
        }
        list.add(new IntIntRecordTriple(range[0], range[1], rec));
      }
      // Number of replicas of current record
      elements += availableTokens[0].size();
    }
    System.out.println("Idx size : " + elements);

    // Statistics
    System.out.println("iIdx key-value pairs: " + idx.size());
    int sum = 0;
    int singlelistsize = 0;
    long count = 0;
    for (ArrayList<IntIntRecordTriple> list : idx.values()) {
      if (list.size() == 1) {
        ++singlelistsize;
        continue;
      }
      sum++;
      count += list.size();
    }
    System.out.println("Single value list size : " + singlelistsize);
    System.out.println("iIdx size : " + count);
    System.out.println("Rec per key-value pair : " + ((double) count) / sum);
  }

  private WYK_HashSet<IntegerPair> join() {
    WYK_HashSet<IntegerPair> rslt = new WYK_HashSet<IntegerPair>();

    // Union하는 set의 평균 개수 및 동시에 union하는 set의 개수
    long set_union_count = 0;
    long set_union_sum = 0;
    long set_union_setsize_sum = 0;
    // inverted index의 key 조회횟수의 합
    long sum = 0;
    for (Record recS : tableS) {
      List<List<Record>> candidatesList = new ArrayList<List<Record>>();

      IntegerSet[] availableTokens = recS.getAvailableTokens();
      int asdf = availableTokens[0].size();
      sum += asdf;
      int[] range = recS.getCandidateLengths(recS.size() - 1);
      for (int token : availableTokens[0]) {
        ArrayList<IntIntRecordTriple> tree = idx.get(token);

        if (tree == null) continue;
        List<Record> list = new ArrayList<Record>();
        for (IntIntRecordTriple e : tree)
          if (StaticFunctions.overlap(e.min, e.max, range[0], range[1]))
            list.add(e.rec);
        set_union_setsize_sum += list.size();
        candidatesList.add(list);
      }

      List<Record> candidates = StaticFunctions.union(candidatesList,
          idComparator);
      set_union_sum = candidatesList.size();
      ++set_union_count;

      if (skipChecking) continue;
      for (Record recR : candidates) {
        int compare = -1;
        if (useAutomata)
          compare = Validator.DP_A_Queue_useACAutomata(recR, recS, true);
        else
          compare = Validator.DP_A_Queue(recR, recS, true);
        if (compare >= 0) rslt.add(new IntegerPair(recR.getID(), recS.getID()));
      }
    }
    System.out.println("Key membership check : " + sum);
    System.out.println("set_union_count: " + set_union_count);
    System.out.println("set_union_sum: " + set_union_sum);
    System.out.println("set_union_setsize_sum: " + set_union_setsize_sum);

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
    System.out.println("Total iters: " + Validator.niterentry);

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
    String[] remainingArgs = parse(args);
    if (remainingArgs.length != 3) {
      printUsage();
      return;
    }
    String Rfile = remainingArgs[0];
    String Sfile = remainingArgs[1];
    String Rulefile = remainingArgs[2];

    long startTime = System.currentTimeMillis();
    JoinBNoIntervalTree inst = new JoinBNoIntervalTree(Rulefile, Rfile, Sfile);
    System.out.print("Constructor finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));
    inst.run();
  }

  private static String[] parse(String[] args) {
    Options options = buildOptions();
    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = null;
    try {
      cmd = parser.parse(options, args);
    } catch (ParseException e) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("JoinD", options, true);
      System.exit(1);
    }
    useAutomata = !cmd.hasOption("noautomata");
    skipChecking = cmd.hasOption("skipequiv");
    compact = cmd.hasOption("compact");
    return cmd.getArgs();
  }

  private static Options buildOptions() {
    Options options = new Options();
    options.addOption("noautomata", false,
        "Do not use automata to check equivalency");
    options.addOption("skipequiv", false, "Skip equivalency check");
    options.addOption("compact", false, "Use memory-compact version");
    return options;
  }

  private static void printUsage() {
    System.out.println("Usage : <R file> <S file> <Rule file>");
  }
}
