package mine;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import tools.Algorithm;
import tools.IntegerPair;
import tools.IntegerSet;
import tools.Parameters;
import tools.RuleTrie;
import tools.WYK_HashMap;
import tools.WYK_HashSet;
import validator.Validator;

public class Mine_Qgram extends Algorithm {
  WYK_HashMap<IndexKey, ArrayList<Record>> idx;
  static boolean                           useAutomata  = true;
  static boolean                           skipChecking = false;
  static int                               maxIndex     = Integer.MAX_VALUE;
  static boolean                           compact      = false;
  static boolean                           singleside   = false;
  RecordIDComparator                       idComparator;
  RuleTrie                                 ruletrie;
  static String                            outputfile;
  static Validator                         checker;

  protected Mine_Qgram(String rulefile, String Rfile, String Sfile)
      throws IOException {
    super(rulefile, Rfile, Sfile);
  }

  private void buildIndex() {
    // Build an index
    // Count Invokes per each (token, loc) pair
    WYK_HashMap<IndexKey, Integer> invokes = new WYK_HashMap<IndexKey, Integer>();
    for (Record rec : tableS) {
      IntegerSet[] availableTokens = rec.getAvailableTokens();
      // Add empty character
      if (availableTokens.length == 1) {
        for (int token : availableTokens[0]) {
          IndexKey key = new IndexKey(0, new int[] { token, 0 });
          Integer count = invokes.get(key);
          if (count == null)
            count = 1;
          else
            count += 1;
          invokes.put(key, count);
        }
      } else {
        for (int i = 0; i < availableTokens.length - 1; ++i) {
          for (int token1 : availableTokens[i]) {
            for (int token2 : availableTokens[i + 1]) {
              IndexKey key = new IndexKey(i, new int[] { token1, token2 });
              Integer count = invokes.get(key);
              if (count == null)
                count = 1;
              else
                count += 1;
              invokes.put(key, count);
            }
          }
        }
      }
    }

    idx = new WYK_HashMap<IndexKey, ArrayList<Record>>();
    for (Record rec : tableR) {
      IntegerSet[] availableTokens = rec.getAvailableTokens();
      int minlength = rec.getCandidateLengths(rec.size() - 1)[0];
      int minIdx = -1;
      int minInvokes = Integer.MAX_VALUE;
      if (minlength == 1) {
        minIdx = 0;
      } else {
        for (int i = 0; i < minlength - 1; ++i) {
          int invoke = 0;
          for (int token1 : availableTokens[i]) {
            for (int token2 : availableTokens[i + 1]) {
              IndexKey key = new IndexKey(i, new int[] { token1, token2 });
              Integer count = invokes.get(key);
              if (count != null) invoke += count;
            }
          }
          if (invoke < minInvokes) {
            minIdx = i;
            minInvokes = invoke;
          }
        }
      }

      if (minlength == 1) {
        for (int token : availableTokens[minIdx]) {
          IndexKey key = new IndexKey(0, new int[] { token, 0 });
          ArrayList<Record> list = idx.get(key);
          if (list == null) {
            list = new ArrayList<Record>();
            list.add(rec);
            idx.put(key, list);
          } else
            list.add(rec);
        }
      } else {
        for (int token1 : availableTokens[minIdx]) {
          for (int token2 : availableTokens[minIdx + 1]) {
            IndexKey key = new IndexKey(0, new int[] { token1, token2 });
            ArrayList<Record> list = idx.get(key);
            if (list == null) {
              list = new ArrayList<Record>();
              list.add(rec);
              idx.put(key, list);
            } else
              list.add(rec);
          }
        }
      }
    }
  }

  private WYK_HashSet<IntegerPair> join() {
    WYK_HashSet<IntegerPair> rslt = new WYK_HashSet<IntegerPair>();

    for (Record recS : tableS) {
      IntegerSet[] availableTokens = recS.getAvailableTokens();
      if (availableTokens.length == 1) {
        for (int token : availableTokens[0]) {
          IndexKey key = new IndexKey(0, new int[] { token, 0 });
          ArrayList<Record> candidates = idx.get(key);

          if (candidates == null) continue;
          for (Record recR : candidates) {
            int compare = checker.isEqual(recR, recS);
            if (compare >= 0)
              rslt.add(new IntegerPair(recR.getID(), recS.getID()));
          }
        }
      } else {
        for (int i = 0; i < availableTokens.length - 1; ++i) {
          for (int token1 : availableTokens[i]) {
            for (int token2 : availableTokens[i + 1]) {
              IndexKey key = new IndexKey(0, new int[] { token1, token2 });
              ArrayList<Record> candidates = idx.get(key);

              if (candidates == null) continue;
              for (Record recR : candidates) {
                int compare = checker.isEqual(recR, recS);
                if (compare >= 0)
                  rslt.add(new IntegerPair(recR.getID(), recS.getID()));
              }
            }
          }
        }
      }
    }

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
    Mine_Qgram inst = new Mine_Qgram(Rulefile, Rfile, Sfile);
    System.out.print("Constructor finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));
    inst.run();
    
    Validator.printStats();
  }
}

class IndexKey {
  final int   idx;
  final int[] qgram;
  final int   hash;

  IndexKey(int idx, int[] qgram) {
    this.idx = idx;
    this.qgram = qgram;
    int hash = idx;
    for (int str : qgram)
      hash = hash ^ 0x1f1f1f1f + str;
    this.hash = hash;
  }

  @Override
  public int hashCode() {
    return hash;
  }
}