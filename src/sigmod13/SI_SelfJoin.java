package sigmod13;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import sigmod13.filter.ITF1;
import sigmod13.filter.ITF2;
import sigmod13.filter.ITF3;
import sigmod13.filter.ITF4;
import sigmod13.filter.ITF_Filter;
import tools.Algorithm;
import tools.Rule;
import tools.Rule_ACAutomata;

public class SI_SelfJoin extends Algorithm {
  ArrayList<SIRecord> table;
  ArrayList<Rule>     rulelist;

  public SI_SelfJoin(String DBR_file, String rulefile) throws IOException {
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

  private ArrayList<SIRecord> readRecords(String DBfile, int num)
      throws IOException {
    Rule_ACAutomata ruleAC = new Rule_ACAutomata(rulelist);
    ArrayList<SIRecord> rslt = new ArrayList<SIRecord>();
    BufferedReader br = new BufferedReader(new FileReader(DBfile));
    String line;
    while ((line = br.readLine()) != null && num != 0) {
      rslt.add(new SIRecord(rslt.size(), line, str2int, ruleAC));
      --num;
    }
    br.close();
    long count = 0;
    int max = 0;
    for(SIRecord rec : rslt) {
      int tmp = rec.getApplicableRules().size();
      count += tmp;
      max = Math.max(max, tmp);
    }
    System.out.println("Avg rules : " + ((double)count / rslt.size()));
    System.out.println("Max rules : " + max);
    return rslt;
  }

  public void run(double threshold, int filterType) throws IOException {
    // BufferedReader br = new BufferedReader(new
    // InputStreamReader(System.in));
    // br.readLine();

    long startTime = System.currentTimeMillis();

    ITF_Filter filter = null;

    switch (filterType) {
      case 1:
        filter = new ITF1(table, rulelist);
        break;
      case 2:
        filter = new ITF2(table, rulelist);
        break;
      case 3:
        filter = new ITF3(table, rulelist);
        break;
      case 4:
        filter = new ITF4(table, rulelist);
        break;
      default:
    }
    SI_Tree tree = new SI_Tree(threshold, filter, table);
    System.out.println("Node size : " + (tree.FEsize + tree.LEsize));
    System.out.println("Sig size : " + tree.sigsize);

    System.out.print("Building SI-Tree finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));

    // br.readLine();

    selfjoin(tree, threshold);
  }

  public void selfjoin(SI_Tree treeR, double threshold) {
    long startTime = System.currentTimeMillis();

    HashSet<SIRecordPair> candidates = treeR.selfjoin(threshold);
    // long counter = treeR.join(treeS, threshold);
    System.out.print("Retrieveing candidates finished");

    System.out.println(" " + (System.currentTimeMillis() - startTime));
    System.out.println("Candidates : " + candidates.size());

    startTime = System.currentTimeMillis();

    long similar = 0;

    // for(SIRecordPair pair : candidates)
    // System.out.println(pair.rec1 + "\t" + pair.rec2);

    for (SIRecordPair pair : candidates)
      if (SimilarityFunc.selectiveExp(pair.rec1, pair.rec2) >= threshold)
        ++similar;

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
    SI_Tree.exactAnswer = (args.length == 5);

    long startTime = System.currentTimeMillis();
    SI_SelfJoin inst = new SI_SelfJoin(Rfile, Rulefile);
    System.out.print("Constructor finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));
    inst.run(threshold, filterNo);
  }

  private static void printUsage() {
    System.out.println("Usage : <R file> <Rule file> <Threshold> <Filter No.>");
  }
}
