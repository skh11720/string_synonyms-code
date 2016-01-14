package sigmod13.modified;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import mine.Record;
import sigmod13.SI_Tree;
import tools.Algorithm;
import tools.Pair;
import tools.Rule;
import tools.Rule_ACAutomata;

public class SI_SelfJoin_Modified extends Algorithm {
  ArrayList<Record> table;
  ArrayList<Rule>   rulelist;

  public SI_SelfJoin_Modified(String DBR_file, String rulefile)
      throws IOException {
    super(rulefile, DBR_file, DBR_file);
    int size = -1;
    readRules(rulefile);
    table = readRecords(DBR_file, size);
  }

  private void readRules(String Rulefile) throws IOException {
    rulelist = new ArrayList<Rule>();
    BufferedReader br = new BufferedReader(new FileReader(Rulefile));
    String line;
    while ((line = br.readLine()) != null) {
      rulelist.add(new Rule(line, str2int));
    }
    br.close();
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
    for (Record rec : table) {
      rec.preprocessRules(automata, false);
    }
    long time = System.currentTimeMillis() - currentTime;
    System.out.println("Preprocess rules : " + time);

    currentTime = System.currentTimeMillis();
    for (Record rec : table) {
      rec.preprocessLengths();
    }
    time = System.currentTimeMillis() - currentTime;
    System.out.println("Preprocess lengths: " + time);

    currentTime = System.currentTimeMillis();
    for (Record rec : table) {
      rec.preprocessAvailableTokens(1);
    }
    time = System.currentTimeMillis() - currentTime;
    System.out.println("Preprocess available tokens: " + time);

    currentTime = System.currentTimeMillis();
    for (Record rec : table) {
      rec.preprocessEstimatedRecords();
    }
    time = System.currentTimeMillis() - currentTime;
    System.out.println("Preprocess est records: " + time);
  }

  public void run(double threshold, int filterType) throws IOException {
    // BufferedReader br = new BufferedReader(new
    // InputStreamReader(System.in));
    // br.readLine();

    preprocess();

    long startTime = System.currentTimeMillis();

    SI_Tree<Record> tree = new SI_Tree<Record>(threshold, null, table);
    System.out.println("Node size : " + (tree.FEsize + tree.LEsize));
    System.out.println("Sig size : " + tree.sigsize);

    System.out.print("Building SI-Tree finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));

    // br.readLine();

    selfjoin(tree, threshold);
  }

  public void selfjoin(SI_Tree<Record> treeR, double threshold) {
    long startTime = System.currentTimeMillis();

    HashSet<Pair<Record>> candidates = treeR.selfjoin(threshold);
    // long counter = treeR.join(treeS, threshold);
    System.out.print("Retrieveing candidates finished");

    System.out.println(" " + (System.currentTimeMillis() - startTime));
    System.out.println("Candidates : " + candidates.size());

    startTime = System.currentTimeMillis();

    long similar = 0;

    for (Pair<Record> pair : candidates) {
      System.out.println(pair.rec1 + "\t" + pair.rec2);
      ++similar;
    }

    System.out.print("Validating finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));
    System.out.println("Similar pairs : " + similar);
  }

  /**
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws IOException {
    if (args.length != 4 && args.length != 5) {
      printUsage();
      return;
    }
    String Rfile = args[0];
    String Rulefile = args[1];
    double threshold = Double.parseDouble(args[2]);
    int filterNo = Integer.parseInt(args[3]);
    SI_Tree.exactAnswer = false;// (args.length == 5);

    long startTime = System.currentTimeMillis();
    SI_SelfJoin_Modified inst = new SI_SelfJoin_Modified(Rfile, Rulefile);
    System.out.print("Constructor finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));
    inst.run(threshold, filterNo);
  }

  private static void printUsage() {
    System.out.println("Usage : <R file> <Rule file> <Threshold> <Filter No.>");
  }
}