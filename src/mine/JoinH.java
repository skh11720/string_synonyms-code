package mine;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tools.Algorithm;
import tools.IntegerPair;
import tools.IntegerSet;
import tools.Parameters;
import tools.StaticFunctions;
import tools.WYK_HashMap;
import tools.WYK_HashSet;
import validator.Validator;

public class JoinH extends Algorithm {
  static boolean                                            useAutomata  = true;
  static boolean                                            skipChecking = false;
  static boolean                                            compact      = false;
  static String                                             outputfile;
  RecordIDComparator                                        idComparator;
  static int                                                maxIndex     = 3;
  static Validator                                          checker;

  /**
   * Key: (token, index) pair<br/>
   * Value IntervalTree Key: length of record (min, max)
   * Value IntervalTree Value: record
   */
  WYK_HashMap<IntegerPair, IntervalTreeRW<Integer, Record>> idx;

  protected JoinH(String rulefile, String Rfile, String Sfile)
      throws IOException {
    super(rulefile, Rfile, Sfile);
    idComparator = new RecordIDComparator();
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
          int compare = checker.isEqual(recR, recS);
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
    preprocess(compact, Integer.MAX_VALUE, useAutomata);
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
    compact = params.isCompact();
    checker = params.getValidator();

    long startTime = System.currentTimeMillis();
    JoinH inst = new JoinH(Rulefile, Rfile, Sfile);
    System.out.print("Constructor finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));
    inst.run();

    Validator.printStats();
  }
}
