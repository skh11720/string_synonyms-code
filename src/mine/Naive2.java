package mine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import tools.Algorithm;
import tools.Rule;
import tools.RuleTrie;
import tools.Rule_ACAutomata;
import tools.WYK_HashSet;

public class Naive2 extends Algorithm {
  ArrayList<Record>        tableR;
  /**
   * Map each record to its own index
   */
  HashMap<Record, Integer> rec2idx;
  ArrayList<Record>        tableS;
  ArrayList<Rule>          rulelist;
  Rule_ACAutomata          automata;
  RuleTrie                 ruletrie;

  static int               threshold = 1000;

  protected Naive2(String rulefile, String Rfile, String Sfile)
      throws IOException {
    super(rulefile, Rfile, Sfile);
    int size = 1000000;

    readRules(rulefile);
    Record.setStrList(strlist);
    tableR = readRecords(Rfile, size);
    tableS = readRecords(Sfile, size);
    rec2idx = new HashMap<Record, Integer>();
    for (int i = 0; i < tableR.size(); ++i)
      rec2idx.put(tableR.get(i), i);
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

  private List<IntegerPair> join() {
    automata = new Rule_ACAutomata(rulelist);
    ruletrie = new RuleTrie(rulelist);
    List<IntegerPair> rslt = new ArrayList<IntegerPair>();

    for (int idxS = 0; idxS < tableS.size(); ++idxS) {
      Record recS = tableS.get(idxS);
      recS.preprocessRules(automata, false);
      recS.preprocessEstimatedRecords();
      long est = recS.getEstNumRecords();
      if (est >= threshold) continue;
      ArrayList<Record> expanded = recS.expandAll(ruletrie);
      for (Record exp : expanded) {
        ArrayList<Record> double_expanded = exp.expandAll(ruletrie);
        WYK_HashSet<Integer> candidates = new WYK_HashSet<Integer>();
        for (Record dexp : double_expanded) {
          Integer idx = rec2idx.get(dexp);
          if (idx == null) continue;
          candidates.add(idx);
        }
        for (Integer idx : candidates) {
          rslt.add(new IntegerPair(idx, idxS));
        }
      }
    }

    return rslt;
  }

  public void run() {
    long startTime = System.currentTimeMillis();
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
    Naive2.threshold = Integer.valueOf(args[3]);

    long startTime = System.currentTimeMillis();
    Naive2 inst = new Naive2(Rulefile, Rfile, Sfile);
    System.out.print("Constructor finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));
    inst.run();
  }

  private static void printUsage() {
    System.out.println("Usage : <R file> <S file> <Rule file> <exp threshold>");
  }
}
