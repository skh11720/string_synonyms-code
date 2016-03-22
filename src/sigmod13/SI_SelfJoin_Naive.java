package sigmod13;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import sigmod13.filter.ITF1;
import sigmod13.filter.ITF2;
import sigmod13.filter.ITF3;
import sigmod13.filter.ITF4;
import sigmod13.filter.ITF_Filter;
import tools.Algorithm;
import tools.Pair;
import tools.Rule;

public class SI_SelfJoin_Naive extends Algorithm {
  ArrayList<SIRecord> table;
  ArrayList<Rule>     rulelist;

  public SI_SelfJoin_Naive(String DBR_file, String rulefile)
      throws IOException {
    super(rulefile, DBR_file, DBR_file);
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
    SI_Tree<SIRecord> tree = new SI_Tree<SIRecord>(threshold, filter, table);
    System.out.println("Node size : " + (tree.FEsize + tree.LEsize));
    System.out.println("Sig size : " + tree.sigsize);

    System.out.print("Building SI-Tree finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));

    // br.readLine();

    naiveselfjoin(tree, threshold);
  }

  public void naiveselfjoin(SI_Tree<SIRecord> treeR, double threshold) {
    long startTime = System.currentTimeMillis();

    HashSet<Pair<SIRecord>> candidates = treeR.naivejoin(table, true);
    // long counter = treeR.join(treeS, threshold);
    System.out.print("Retrieveing candidates finished");

    System.out.println(" " + (System.currentTimeMillis() - startTime));
    System.out.println("Candidates : " + candidates.size());

    startTime = System.currentTimeMillis();

    long similar = 0;

    // for(Pair<SIRecord> pair : candidates)
    // System.out.println(pair.rec1 + "\t" + pair.rec2);

    // for (Pair<SIRecord> pair : candidates)
    // if (SimilarityFunc.selectiveExp(pair.rec1, pair.rec2) >= threshold)
    // ++similar;

    System.out.print("Validating finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));
    System.out.println("Similar pairs : " + similar);
  }

  /**
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws IOException {
    if (args.length != 4) {
      printUsage();
      return;
    }
    String Rfile = args[0];
    String Rulefile = args[1];
    double threshold = Double.parseDouble(args[2]);
    int filterNo = Integer.parseInt(args[3]);

    long startTime = System.currentTimeMillis();
    SI_SelfJoin_Naive inst = new SI_SelfJoin_Naive(Rfile, Rulefile);
    System.out.print("Constructor finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));
    inst.run(threshold, filterNo);
  }

  private static void printUsage() {
    System.out.println("Usage : <R file> <Rule file> <Threshold> <Filter No.>");
  }
}
