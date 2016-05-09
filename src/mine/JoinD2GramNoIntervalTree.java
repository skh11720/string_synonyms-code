package mine;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tools.Algorithm;
import tools.IntIntRecordTriple;
import tools.IntegerPair;
import tools.Parameters;
import tools.StaticFunctions;
import tools.WYK_HashMap;
import tools.WYK_HashSet;
import validator.Validator;

public class JoinD2GramNoIntervalTree extends Algorithm {
  static boolean                            useAutomata  = true;
  static boolean                            skipChecking = false;
  static boolean                            compact      = false;
  static boolean                            exact2grams  = false;
  static String                             outputfile;
  RecordIDComparator                        idComparator;
  static int                                maxIndex     = 3;
  static Validator                          checker;
  /**
   * Key: twogram<br/>
   * Value IntervalTree Key: length of record (min, max)<br/>
   * Value IntervalTree Value: record
   */
  List<Map<Long, List<IntIntRecordTriple>>> idx;

  protected JoinD2GramNoIntervalTree(String rulefile, String Rfile,
      String Sfile) throws IOException {
    super(rulefile, Rfile, Sfile);
    idComparator = new RecordIDComparator();
  }

  private void buildIndex() {
    long elements = 0;
    // Build an index

    idx = new ArrayList<Map<Long, List<IntIntRecordTriple>>>();
    for (int i = 0; i < maxIndex; ++i)
      idx.add(new WYK_HashMap<Long, List<IntIntRecordTriple>>());
    for (Record rec : tableR) {
      List<Set<Long>> available2Grams = exact2grams ? rec.getExact2Grams()
          : rec.get2Grams();
      int[] range = rec.getCandidateLengths(rec.size() - 1);
      int boundary = Math.min(range[1], maxIndex);
      for (int i = 0; i < boundary; ++i) {
        Map<Long, List<IntIntRecordTriple>> map = idx.get(i);
        for (long twogram : available2Grams.get(i)) {
          List<IntIntRecordTriple> list = map.get(twogram);
          if (list == null) {
            list = new ArrayList<IntIntRecordTriple>();
            map.put(twogram, list);
          }
          list.add(new IntIntRecordTriple(range[0], range[1], rec));
        }
        elements += available2Grams.get(i).size();
      }
    }
    System.out.println("Idx size : " + elements);

    for (int i = 0; i < maxIndex; ++i) {
      Map<Long, List<IntIntRecordTriple>> ithidx = idx.get(i);
      System.out.println(i + "th iIdx key-value pairs: " + ithidx.size());
      // Statistics
      int sum = 0;
      int singlelistsize = 0;
      long count = 0;
      long sqsum = 0;
      for (List<IntIntRecordTriple> list : ithidx.values()) {
        sqsum += list.size() * list.size();
        if (list.size() == 1) {
          ++singlelistsize;
          continue;
        }
        sum++;
        count += list.size();
      }
      System.out.println(i + "th Single value list size : " + singlelistsize);
      System.out.println(i + "th iIdx size : " + count);
      System.out.println(i + "th Rec per idx : " + ((double) count) / sum);
      System.out.println(i + "th Sqsum : " + sqsum);
    }
  }

  private WYK_HashSet<IntegerPair> join() {
    WYK_HashSet<IntegerPair> rslt = new WYK_HashSet<IntegerPair>();
    long count = 0;

    long cand_sum[] = new long[maxIndex];
    int count_cand[] = new int[maxIndex];
    int count_empty[] = new int[maxIndex];
    long[] sum = new long[maxIndex];
    for (Record recS : tableS) {
      List<List<Record>> candidatesList = new ArrayList<List<Record>>();
      List<Set<Long>> available2Grams = exact2grams ? recS.getExact2Grams()
          : recS.get2Grams();

      int[] range = recS.getCandidateLengths(recS.size() - 1);
      int boundary = Math.min(range[0], maxIndex);
      for (int i = 0; i < boundary; ++i) {
        long asdf = available2Grams.get(i).size();
        sum[i] += asdf;
      }
      for (int i = 0; i < boundary; ++i) {
        List<List<Record>> ithCandidates = new ArrayList<List<Record>>();
        Map<Long, List<IntIntRecordTriple>> map = idx.get(i);
        for (long twogram : available2Grams.get(i)) {
          List<IntIntRecordTriple> tree = map.get(twogram);
          if (tree == null) {
            ++count_empty[i];
            continue;
          }
          cand_sum[i] += tree.size();
          ++count_cand[i];
          List<Record> list = new ArrayList<Record>();
          for (IntIntRecordTriple e : tree)
            if (StaticFunctions.overlap(e.min, e.max, range[0], range[1]))
              list.add(e.rec);
          ithCandidates.add(list);
        }
        candidatesList.add(StaticFunctions.union(ithCandidates, idComparator));
      }
      List<Record> candidates = StaticFunctions.intersection(candidatesList,
          idComparator);
      count += candidates.size();

      if (skipChecking) continue;
      for (Record recR : candidates) {
        int compare = checker.isEqual(recR, recS);
        if (compare >= 0) rslt.add(new IntegerPair(recR.getID(), recS.getID()));
      }
    }
    for (int i = 0; i < maxIndex; ++i) {
      System.out.println(i + "th Key membership check : " + sum[i]);
      System.out
          .println("Avg candidates : " + cand_sum[i] + "/" + count_cand[i]);
      System.out.println("Empty candidates : " + count_empty[i]);
    }
    System.out.println("comparisions : " + count);

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

    System.out.println("Set union iters:" + StaticFunctions.counter);
    System.out.println("Set inter iters:" + StaticFunctions.inter_counter);

    try {
      BufferedWriter bw = new BufferedWriter(new FileWriter(outputfile));
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
    Parameters params = Parameters.parseArgs(args);
    String Rfile = params.getInputX();
    String Sfile = params.getInputY();
    String Rulefile = params.getInputRules();
    outputfile = params.getOutput();
    maxIndex = params.getMaxIndex();

    // Setup parameters
    useAutomata = params.isUseACAutomata();
    skipChecking = params.isSkipChecking();
    compact = params.isCompact();
    checker = params.getValidator();
    exact2grams = params.isExact2Grams();

    long startTime = System.currentTimeMillis();
    JoinD2GramNoIntervalTree inst = new JoinD2GramNoIntervalTree(Rulefile,
        Rfile, Sfile);
    System.out.print("Constructor finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));
    inst.run();

    Validator.printStats();
  }
}
