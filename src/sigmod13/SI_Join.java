package sigmod13;

import java.io.IOException;
import java.util.List;

import sigmod13.filter.ITF1;
import sigmod13.filter.ITF2;
import sigmod13.filter.ITF3;
import sigmod13.filter.ITF4;
import sigmod13.filter.ITF_Filter;
import tools.Pair;

public class SI_Join extends SIAlgorithm {

  public SI_Join(String DBR_file, String DBS_file, String rulefile)
      throws IOException {
    super(rulefile, DBR_file, DBS_file);
  }

  public void run(double threshold, int filterType) throws IOException {
    // BufferedReader br = new BufferedReader(new
    // InputStreamReader(System.in));
    // br.readLine();

    long startTime = System.currentTimeMillis();

    ITF_Filter filterR = null;
    ITF_Filter filterS = null;

    switch (filterType) {
      case 1:
        filterR = new ITF1(tableR, rulelist);
        filterS = new ITF1(tableS, rulelist);
        break;
      case 2:
        filterR = new ITF2(tableR, rulelist);
        filterS = new ITF2(tableS, rulelist);
        break;
      case 3:
        filterR = new ITF3(tableR, rulelist);
        filterS = new ITF3(tableS, rulelist);
        break;
      case 4:
        filterR = new ITF4(tableR, rulelist);
        filterS = new ITF4(tableS, rulelist);
        break;
      default:
    }
    SI_Tree<SIRecord> treeR = new SI_Tree<SIRecord>(threshold, filterR, tableR);
    SI_Tree<SIRecord> treeS = new SI_Tree<SIRecord>(threshold, filterS, tableS);
    System.out.println("Node size : " + (treeR.FEsize + treeR.LEsize));
    System.out.println("Sig size : " + treeR.sigsize);

    System.out.print("Building SI-Tree finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));

    // br.readLine();

    join(treeR, treeS, threshold);
  }

  public void join(SI_Tree<SIRecord> treeR, SI_Tree<SIRecord> treeS,
      double threshold) {
    long startTime = System.currentTimeMillis();

    List<Pair<SIRecord>> candidates = treeR.join(treeS, threshold);
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
    if (args.length != 5 && args.length != 6) {
      printUsage();
      return;
    }
    String Rfile = args[0];
    String Sfile = args[1];
    String Rulefile = args[2];
    double threshold = Double.parseDouble(args[3]);
    int filterNo = Integer.parseInt(args[4]);
    SI_Tree.exactAnswer = (args.length == 6);

    long startTime = System.currentTimeMillis();
    SI_Join inst = new SI_Join(Rfile, Sfile, Rulefile);
    System.out.print("Constructor finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));
    inst.run(threshold, filterNo);
  }

  private static void printUsage() {
    System.out.println(
        "Usage : <R file> <S file> <Rule file> <Threshold> <Filter No.> [T=exact]");
  }
}
