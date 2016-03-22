package sigmod13;

import java.io.IOException;
import java.util.HashSet;

import sigmod13.filter.ITF1;
import sigmod13.filter.ITF2;
import sigmod13.filter.ITF3;
import sigmod13.filter.ITF4;
import sigmod13.filter.ITF_Filter;
import tools.Pair;

public class SI_SelfJoin extends SIAlgorithm {

  public SI_SelfJoin(String DBR_file, String rulefile) throws IOException {
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
        filter = new ITF1(tableR, rulelist);
        break;
      case 2:
        filter = new ITF2(tableR, rulelist);
        break;
      case 3:
        filter = new ITF3(tableR, rulelist);
        break;
      case 4:
        filter = new ITF4(tableR, rulelist);
        break;
      default:
    }
    SI_Tree<SIRecord> tree = new SI_Tree<SIRecord>(threshold, filter, tableR);
    System.out.println("Node size : " + (tree.FEsize + tree.LEsize));
    System.out.println("Sig size : " + tree.sigsize);

    System.out.print("Building SI-Tree finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));

    // br.readLine();

    selfjoin(tree, threshold);
  }

  public void selfjoin(SI_Tree<SIRecord> treeR, double threshold) {
    long startTime = System.currentTimeMillis();

    HashSet<Pair<SIRecord>> candidates = treeR.selfjoin(threshold);
    // long counter = treeR.join(treeS, threshold);
    System.out.print("Retrieveing candidates finished");

    System.out.println(" " + (System.currentTimeMillis() - startTime));
    System.out.println("Candidates : " + candidates.size());

    startTime = System.currentTimeMillis();

    long similar = 0;

    // for(Pair<SIRecord> pair : candidates)
    // System.out.println(pair.rec1 + "\t" + pair.rec2);

    for (Pair<SIRecord> pair : candidates)
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
    SI_Tree.exactAnswer = false;// (args.length == 5);

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
