package mine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import tools.Algorithm;
import tools.IntegerSet;
import tools.Rule;
import tools.Rule_ACAutomata;
import tools.WYK_HashMap;
import tools.WYK_HashSet;

public class Mine extends Algorithm {
  ArrayList<Record> tableR;
  ArrayList<Record> tableS;
  ArrayList<Rule>   rulelist;

  WYK_HashMap<IntegerPair, ArrayList<Record>> idx;

  protected Mine(String rulefile, String Rfile, String Sfile)
      throws IOException {
    super(rulefile, Rfile, Sfile);
    int size = 1000000;

    readRules(rulefile);
    Record.setStrList(strlist);
    tableR = readRecords(Rfile, size);
    tableS = readRecords(Sfile, size);
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
    // Preprocess each records in R
    for (Record rec : tableR) {
      rec.preprocessRules(automata);
      rec.preprocessLengths();
      rec.preprocessAvailableTokens();
      rec.preprocessEstimatedRecords();
    }

    // Preprocess each records in S
    for (Record rec : tableS) {
      rec.preprocessRules(automata);
      rec.preprocessLengths();
      rec.preprocessAvailableTokens();
      rec.preprocessEstimatedRecords();
    }
  }

  private void buildIndex() {
    long elements = 0;
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

    idx = new WYK_HashMap<IntegerPair, ArrayList<Record>>();
    for (Record rec : tableR) {
      IntegerSet[] availableTokens = rec.getAvailableTokens();
      int minlength = rec.getCandidateLengths(rec.size() - 1)[0];
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

      for (int token : availableTokens[minIdx]) {
        IntegerPair ip = new IntegerPair(token, minIdx);
        ArrayList<Record> list = idx.get(ip);
        if (list == null) {
          list = new ArrayList<Record>();
          list.add(rec);
          idx.put(ip, list);
        } else
          list.add(rec);
      }
      elements += availableTokens[minIdx].size();
    }
    System.out.println("Elements in the index : " + elements);
  }

  private WYK_HashSet<IntegerPair> join() {
    WYK_HashSet<IntegerPair> rslt = new WYK_HashSet<IntegerPair>();

    for (Record recS : tableS) {
      IntegerSet[] availableTokens = recS.getAvailableTokens();
      for (int i = 0; i < availableTokens.length; ++i) {
        for (int token : availableTokens[i]) {
          IntegerPair ip = new IntegerPair(token, i);
          ArrayList<Record> candidates = idx.get(ip);

          if (candidates == null) continue;
          for (Record recR : candidates) {
            boolean compare = Validator.DP_A_Queue_useACAutomata(recR, recS,
                true);
            if (compare) rslt.add(new IntegerPair(recR.getID(), recS.getID()));
          }
        }
      }
    }

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
    if (args.length != 3) {
      printUsage();
      return;
    }
    String Rfile = args[0];
    String Sfile = args[1];
    String Rulefile = args[2];

    long startTime = System.currentTimeMillis();
    Mine inst = new Mine(Rulefile, Rfile, Sfile);
    System.out.print("Constructor finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));
    inst.run();
  }

  private static void printUsage() {
    System.out.println("Usage : <R file> <S file> <Rule file>");
  }
}
