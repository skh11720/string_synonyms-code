package mine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import tools.Algorithm;
import tools.IntIntRecordTriple;
import tools.IntegerPair;
import tools.IntegerSet;
import tools.Rule;
import tools.RuleTrie;
import tools.Rule_ACAutomata;
import tools.StaticFunctions;
import tools.WYK_HashMap;
import tools.WYK_HashSet;

public class JoinHNoIntervalTree extends Algorithm {
  static boolean                                     useAutomata  = true;
  static boolean                                     skipChecking = false;
  static int                                         maxIndex     = Integer.MAX_VALUE;
  static boolean                                     compact      = false;
  static boolean                                     singleside   = false;
  static boolean                                     earlyprune   = false;
  static boolean                                     useMatrix    = false;
  static boolean                                     useTDMatrix  = false;
  static boolean                                     naiveCheck   = false;
  static int                                         naiveThreshold;

  ArrayList<Record>                                  tableR;
  ArrayList<Record>                                  tableS;
  ArrayList<Rule>                                    rulelist;
  RecordIDComparator                                 idComparator;
  RuleTrie                                           ruletrie;

  static String                                      outputfile;

  /**
   * Key: (token, index) pair<br/>
   * Value: (min, max, record) triple
   */
  WYK_HashMap<IntegerPair, List<IntIntRecordTriple>> idx;

  protected JoinHNoIntervalTree(String rulefile, String Rfile, String Sfile)
      throws IOException {
    super(rulefile, Rfile, Sfile);
    int size = -1;

    readRules(rulefile);
    Record.setStrList(strlist);
    tableR = readRecords(Rfile, size);
    tableS = readRecords(Sfile, size);
    idComparator = new RecordIDComparator();
    ruletrie = new RuleTrie(rulelist);
    Record.setRuleTrie(ruletrie);
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
    long applicableRules = 0;
    for (Record rec : tableR) {
      rec.preprocessRules(automata, useAutomata);
      applicableRules += rec.getNumApplicableRules();
    }
    long time = System.currentTimeMillis() - currentTime;
    System.out.println("Preprocess rules : " + time);
    System.out.println(
        "Avg applicable rules : " + applicableRules + "/" + tableR.size());

    currentTime = System.currentTimeMillis();
    for (Record rec : tableR) {
      rec.preprocessLengths();
    }
    time = System.currentTimeMillis() - currentTime;
    System.out.println("Preprocess lengths: " + time);

    if (!compact) {
      currentTime = System.currentTimeMillis();
      for (Record rec : tableR) {
        rec.preprocessAvailableTokens(maxIndex);
      }
      time = System.currentTimeMillis() - currentTime;
      System.out.println("Preprocess tokens: " + time);
    }

    currentTime = System.currentTimeMillis();
    for (Record rec : tableR) {
      rec.preprocessEstimatedRecords();
    }
    time = System.currentTimeMillis() - currentTime;
    System.out.println("Preprocess est records: " + time);

    currentTime = System.currentTimeMillis();
    for (Record rec : tableR) {
      rec.preprocessSearchRanges();
      rec.preprocessSuffixApplicableRules();
    }
    time = System.currentTimeMillis() - currentTime;
    System.out.println("Preprocess for early pruning: " + time);

    // Preprocess each records in S
    for (Record rec : tableS) {
      rec.preprocessRules(automata, useAutomata);
      rec.preprocessLengths();
      if (!compact) rec.preprocessAvailableTokens(maxIndex);
      rec.preprocessEstimatedRecords();
      rec.preprocessSearchRanges();
      rec.preprocessSuffixApplicableRules();
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
      int searchmax = Math.min(availableTokens.length, maxIndex);
      for (int i = 0; i < searchmax; ++i) {
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

    idx = new WYK_HashMap<IntegerPair, List<IntIntRecordTriple>>();
    for (Record rec : tableR) {
      IntegerSet[] availableTokens = rec.getAvailableTokens();
      int[] range = rec.getCandidateLengths(rec.size() - 1);
      int minIdx = -1;
      int minInvokes = Integer.MAX_VALUE;
      int searchmax = Math.min(range[0], maxIndex);
      for (int i = 0; i < searchmax; ++i) {
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
        List<IntIntRecordTriple> list = idx.get(ip);
        if (list == null) {
          list = new ArrayList<IntIntRecordTriple>();
          idx.put(ip, list);
        }
        list.add(new IntIntRecordTriple(range[0], range[1], rec));
      }
      elements += availableTokens[minIdx].size();
    }
    System.out.println("Predict : " + predictCount);
    System.out.println("Idx size : " + elements);

    ///// Statistics
    int sum = 0;
    long count = 0;
    for (List<IntIntRecordTriple> list : idx.values()) {
      if (list.size() == 1) continue;
      sum++;
      count += list.size();
    }
    System.out.println("iIdx size : " + count);
    System.out.println("Rec per idx : " + ((double) count) / sum);
  }

  private WYK_HashSet<IntegerPair> join() {
    WYK_HashSet<IntegerPair> rslt = new WYK_HashSet<IntegerPair>();

    long appliedRules_sum = 0;
    for (Record recS : tableS) {
      IntegerSet[] availableTokens = recS.getAvailableTokens();
      int[] range = recS.getCandidateLengths(recS.size() - 1);
      int searchmax = Math.min(availableTokens.length, maxIndex);
      for (int i = 0; i < searchmax; ++i) {
        List<List<Record>> candidatesList = new ArrayList<List<Record>>();
        for (int token : availableTokens[i]) {
          IntegerPair ip = new IntegerPair(token, i);
          List<IntIntRecordTriple> tree = idx.get(ip);

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
        List<Record> expandedS = naiveCheck
            && recS.getEstNumRecords() < naiveThreshold ? recS.expandAll()
                : null;
        for (Record recR : candidates) {
          int compare = -1;
          // Use naive method
          if (naiveCheck) {
            if (recR.getEstNumRecords() < naiveThreshold
                && recS.getEstNumRecords() < naiveThreshold) {
              assert (expandedS != null);
              assert (expandedS.size() > 0);
              compare = Validator.NaiveDoubleSide(expandedS, recR);
            }
            // If cannot expand every record, simply use DP_A_Matrix
            else
              compare = Validator.DP_A_MatrixwithEarlyPruning(recR, recS);
          }
          // Use Top-down matrix
          else if (useTDMatrix)
            compare = Validator.DP_A_TopdownMatrix(recR, recS);
          // Use matrix
          else if (useMatrix) {
            if (earlyprune)
              compare = Validator.DP_A_MatrixwithEarlyPruning(recR, recS);
            else
              compare = Validator.DP_A_Matrix(recR, recS);
          }
          // Utilize pre-built per-record automata
          else if (useAutomata)
            compare = Validator.DP_A_Queue_useACAutomata(recR, recS, true);
          // Do not utilize any extra resource except list of applicable rules
          else
            compare = Validator.DP_A_Queue(recR, recS, true);
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
    // Count Invokes per each (token, loc) pair
    WYK_HashMap<IntegerPair, Integer> invokes = new WYK_HashMap<IntegerPair, Integer>();
    for (Record rec : tableS) {
      int[] tokens = rec.getTokenArray();
      for (int i = 0; i < tokens.length; ++i) {
        IntegerPair ip = new IntegerPair(tokens[i], i);
        Integer count = invokes.get(ip);
        if (count == null)
          count = 1;
        else
          count += 1;
        invokes.put(ip, count);
      }
    }

    idx = new WYK_HashMap<IntegerPair, List<IntIntRecordTriple>>();
    for (Record rec : tableR) {
      IntegerSet[] availableTokens = rec.getAvailableTokens();
      int[] range = rec.getCandidateLengths(rec.size() - 1);
      int minIdx = -1;
      int minInvokes = Integer.MAX_VALUE;
      int searchmax = Math.min(range[0], maxIndex);
      for (int i = 0; i < searchmax; ++i) {
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
        List<IntIntRecordTriple> list = idx.get(ip);
        if (list == null) {
          list = new ArrayList<IntIntRecordTriple>();
          idx.put(ip, list);
        }
        list.add(new IntIntRecordTriple(range[0], range[1], rec));
      }
      elements += availableTokens[minIdx].size();
    }
    System.out.println("Predict : " + predictCount);
    System.out.println("Idx size : " + elements);

    ///// Statistics
    int sum = 0;
    long count = 0;
    for (List<IntIntRecordTriple> list : idx.values()) {
      if (list.size() == 1) continue;
      sum++;
      count += list.size();
    }
    System.out.println("iIdx size : " + count);
    System.out.println("Rec per idx : " + ((double) count) / sum);
  }

  private WYK_HashSet<IntegerPair> joinSingleSide() {
    WYK_HashSet<IntegerPair> rslt = new WYK_HashSet<IntegerPair>();

    long appliedRules_sum = 0;
    for (Record recS : tableS) {
      int[] tokens = recS.getTokenArray();
      int minlength = recS.getMinLength();
      int maxlength = recS.getMaxLength();
      for (int i = 0; i < tokens.length; ++i) {
        List<Record> candidatesList = new ArrayList<Record>();
        IntegerPair ip = new IntegerPair(tokens[i], i);
        List<IntIntRecordTriple> tree = idx.get(ip);

        if (tree == null) continue;
        for (IntIntRecordTriple e : tree)
          if (StaticFunctions.overlap(e.min, e.max, minlength, maxlength))
            candidatesList.add(e.rec);
        if (skipChecking) continue;
        for (Record recR : candidatesList) {
          int compare = -1;
          if (earlyprune)
            compare = Validator.DP_SingleSidewithEarlyPruning(recR, recS);
          else
            compare = Validator.DP_SingleSide(recR, recS);
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
    long startTime = System.currentTimeMillis();
    preprocess();
    System.out.print("Preprocess finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));

    // Retrieve statistics
    statistics();

    startTime = System.currentTimeMillis();
    if (singleside)
      buildIndexSingleSide();
    else
      buildIndex();
    System.out.print("Building Index finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));

    startTime = System.currentTimeMillis();
    WYK_HashSet<IntegerPair> rslt = (singleside ? joinSingleSide() : join());
    System.out.print("Join finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));
    System.out.println(rslt.size());
    System.out.println("Comparisons: " + Validator.checked);
    System.out.println("Total iter entries: " + Validator.niterentry);
    System.out.println("Total iter rules: " + Validator.niterrules);
    System.out.println("Total iter matches: " + Validator.nitermatches);
    System.out.println("Total iter tokens: " + Validator.nitertokens);
    System.out.println("Early evaled: " + Validator.earlyevaled);
    System.out.println("Early stopped: " + Validator.earlystopped);

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
    String[] remainingArgs = parse(args);
    if (remainingArgs.length != 4) {
      printUsage();
      return;
    }
    String Rfile = remainingArgs[0];
    String Sfile = remainingArgs[1];
    String Rulefile = remainingArgs[2];
    outputfile = remainingArgs[3];

    long startTime = System.currentTimeMillis();
    JoinHNoIntervalTree inst = new JoinHNoIntervalTree(Rulefile, Rfile, Sfile);
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
      formatter.printHelp("JoinH", options, true);
      System.exit(1);
    }
    useAutomata = !cmd.hasOption("noautomata");
    skipChecking = cmd.hasOption("skipequiv");
    compact = cmd.hasOption("compact");
    if (compact) useAutomata = false;
    singleside = cmd.hasOption("singleside");
    if (cmd.hasOption("n"))
      maxIndex = Integer.parseInt(cmd.getOptionValue("n"));
    earlyprune = cmd.hasOption("earlyprune");
    useMatrix = cmd.hasOption("matrix");
    useTDMatrix = cmd.hasOption("tdmatrix");
    if (cmd.hasOption("naivecheck")) {
      naiveCheck = true;
      naiveThreshold = Integer.parseInt(cmd.getOptionValue("naivecheck"));
    }
    return cmd.getArgs();
  }

  private static Options buildOptions() {
    Options options = new Options();
    options.addOption("n", true, "Maximum index to find minimum point");
    options.addOption("noautomata", false,
        "Do not use automata to check equivalency");
    options.addOption("skipequiv", false, "Skip equivalency check");
    options.addOption("compact", false, "Use memory-compact version");
    options.addOption("singleside", false,
        "Use single-side equiv check algorithm");
    options.addOption("earlyprune", false, "Use early pruning strategies");
    options.addOption("matrix", false, "Use matrix in equivalence check");
    options.addOption("tdmatrix", false,
        "Use Top-down matrix in equivalence check");
    options.addOption("naivecheck", true,
        "<N> Check string equivalence by expanding every strings"
            + "which have less than N expanded records");
    return options;
  }

  private static void printUsage() {
    System.out.println("Usage : <R file> <S file> <Rule file>");
  }
}
