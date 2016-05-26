package mine;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import tools.Algorithm;
import tools.IntIntRecordTriple;
import tools.IntegerPair;
import tools.Parameters;
import tools.StaticFunctions;
import tools.WYK_HashMap;
import tools.WYK_HashSet;
import validator.Validator;

/**
 * Extended JoinBNoIntervalTree
 */
public class Join2GramNoIntervalTree extends Algorithm {
  static boolean                                     useAutomata  = false;
  static boolean                                     skipChecking = false;
  static int                                         maxIndex     = Integer.MAX_VALUE;
  static boolean                                     compact      = false;
  static boolean                                     singleside   = false;
  static boolean                                     exact2grams  = false;
  static Validator                                   checker;

  RecordIDComparator                                 idComparator;

  static String                                      outputfile;

  /**
   * Key: 2gram<br/>
   * Value: (min, max, record) triple
   */
  WYK_HashMap<IntegerPair, List<IntIntRecordTriple>> idx;

  protected Join2GramNoIntervalTree(String rulefile, String Rfile, String Sfile)
      throws IOException {
    super(rulefile, Rfile, Sfile);
    idComparator = new RecordIDComparator();
  }

  private void buildIndex() {
    long elements = 0;

    idx = new WYK_HashMap<IntegerPair, List<IntIntRecordTriple>>();
    for (Record rec : tableR) {
      List<Set<IntegerPair>> available2Grams = exact2grams
          ? rec.getExact2Grams() : rec.get2Grams();
      Set<IntegerPair> twoGrams = available2Grams.get(0);
      for (IntegerPair twoGram : twoGrams) {
        List<IntIntRecordTriple> list = idx.get(twoGram);
        if (list == null) {
          list = new ArrayList<IntIntRecordTriple>();
          idx.put(twoGram, list);
        }
        list.add(new IntIntRecordTriple(rec.getMinLength(), rec.getMaxLength(),
            rec));
      }
      elements += twoGrams.size();
    }
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

    long comparisons = 0;
    long appliedRules_sum = 0;
    for (Record recS : tableS) {
      int[] range = recS.getCandidateLengths(recS.size() - 1);
      List<Set<IntegerPair>> available2Grams = exact2grams
          ? recS.getExact2Grams() : recS.get2Grams();
      Set<IntegerPair> twoGrams = available2Grams.get(0);
      List<List<Record>> candidatesList = new ArrayList<List<Record>>();
      for (IntegerPair twoGram : twoGrams) {
        List<IntIntRecordTriple> tree = idx.get(twoGram);

        if (tree == null) continue;
        List<Record> list = new ArrayList<Record>();
        for (IntIntRecordTriple e : tree)
          if (StaticFunctions.overlap(e.min, e.max, range[0], range[1]))
            list.add(e.rec);
        candidatesList.add(list);
      }
      List<Record> candidates = StaticFunctions.union(candidatesList,
          idComparator);
      if (skipChecking) {
        comparisons += candidates.size();
        continue;
      }
      for (Record recR : candidates) {
        int compare = checker.isEqual(recR, recS);
        if (compare >= 0) {
          rslt.add(new IntegerPair(recR.getID(), recS.getID()));
          appliedRules_sum += compare;
        }
      }
    }
    System.out
        .println("Avg applied rules : " + appliedRules_sum + "/" + rslt.size());
    if (skipChecking) System.out.println("Candidates : " + comparisons);

    return rslt;
  }

  public void run() {
    long startTime = System.currentTimeMillis();
    preprocess(compact, maxIndex, useAutomata);
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
    exact2grams = params.isExact2Grams();
    checker = params.getValidator();

    long startTime = System.currentTimeMillis();
    Join2GramNoIntervalTree inst = new Join2GramNoIntervalTree(Rulefile, Rfile,
        Sfile);
    System.out.print("Constructor finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));
    inst.run();

    Validator.printStats();
  }
}
