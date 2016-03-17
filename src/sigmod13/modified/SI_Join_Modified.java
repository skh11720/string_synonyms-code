package sigmod13.modified;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import mine.Record;
import sigmod13.SI_Tree;
import tools.Algorithm;
import tools.Pair;
import tools.Rule;
import tools.Rule_ACAutomata;

public class SI_Join_Modified extends Algorithm {
  static boolean    compact = true;
  ArrayList<Record> tableR;
  ArrayList<Record> tableS;
  ArrayList<Rule>   rulelist;

  public SI_Join_Modified(String DBR_file, String DBS_file, String rulefile)
      throws IOException {
    super(rulefile, DBR_file, DBS_file);
    int size = 1000000;
    readRules(rulefile);
    Record.setStrList(strlist);
    tableR = readRecords(DBR_file, size);
    tableS = readRecords(DBS_file, size);
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

    long currentTime = System.currentTimeMillis();
    // Preprocess each records in R
    for (Record rec : tableR) {
      rec.preprocessRules(automata, false);
    }
    long time = System.currentTimeMillis() - currentTime;
    System.out.println("Preprocess rules : " + time);

    currentTime = System.currentTimeMillis();
    for (Record rec : tableR) {
      rec.preprocessLengths();
    }
    time = System.currentTimeMillis() - currentTime;
    System.out.println("Preprocess lengths: " + time);

    currentTime = System.currentTimeMillis();
    for (Record rec : tableR) {
      rec.preprocessAvailableTokens(1);
    }
    time = System.currentTimeMillis() - currentTime;
    System.out.println("Preprocess tokens: " + time);

    currentTime = System.currentTimeMillis();
    for (Record rec : tableR) {
      rec.preprocessEstimatedRecords();
    }
    time = System.currentTimeMillis() - currentTime;
    System.out.println("Preprocess est records: " + time);

    currentTime = System.currentTimeMillis();
    for (Record rec : tableR) {
      rec.preprocessSearchRanges();
      rec.preprocessSuffixApplicableRules();
    }
    time = System.currentTimeMillis() - currentTime;
    System.out.println("Preprocess for early pruning: " + time);

    // Preprocess each records in S
    for (Record rec : tableS) {
      rec.preprocessRules(automata, false);
      rec.preprocessLengths();
      rec.preprocessAvailableTokens(1);
      rec.preprocessEstimatedRecords();
      rec.preprocessSearchRanges();
      rec.preprocessSuffixApplicableRules();
    }
  }

  public void run() throws IOException {
    preprocess();
    // BufferedReader br = new BufferedReader(new
    // InputStreamReader(System.in));
    // br.readLine();

    long startTime = System.currentTimeMillis();

    SI_Tree<Record> treeR = new SI_Tree<Record>(1, null, tableR);
    SI_Tree<Record> treeS = new SI_Tree<Record>(1, null, tableS);
    System.out.println("Node size : " + (treeR.FEsize + treeR.LEsize));
    System.out.println("Sig size : " + treeR.sigsize);

    System.out.print("Building SI-Tree finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));

    // br.readLine();

    join(treeR, treeS, 1);
  }

  public void join(SI_Tree<Record> treeR, SI_Tree<Record> treeS,
      double threshold) {
    long startTime = System.currentTimeMillis();

    List<Pair<Record>> candidates = treeR.join(treeS, threshold);
    // long counter = treeR.join(treeS, threshold);
    System.out.print("Retrieveing candidates finished");

    System.out.println(" " + (System.currentTimeMillis() - startTime));
    System.out.println("Candidates : " + candidates.size());

    startTime = System.currentTimeMillis();

    System.out.print("Validating finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));
    System.out.println("Similar pairs : " + candidates.size());

    try {
      BufferedWriter bw = new BufferedWriter(new FileWriter("rslt.txt"));
      for (Pair<Record> ip : candidates) {
        if (ip.rec1.getID() != ip.rec2.getID())
          bw.write(ip.rec1.toString(strlist) + "\t==\t"
              + ip.rec2.toString(strlist) + "\n");
      }
      bw.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  /**
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws IOException {
    String[] remainingArgs = parse(args);
    if (remainingArgs.length != 3) {
      printUsage();
      return;
    }
    String Rfile = remainingArgs[0];
    String Sfile = remainingArgs[1];
    String Rulefile = remainingArgs[2];

    long startTime = System.currentTimeMillis();
    SI_Join_Modified inst = new SI_Join_Modified(Rfile, Sfile, Rulefile);
    System.out.print("Constructor finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));
    inst.run();
  }

  private static void printUsage() {
    System.out.println("Usage : <R file> <S file> <Rule file>");
  }

  private static String[] parse(String[] args) {
    Options options = buildOptions();
    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = null;
    try {
      cmd = parser.parse(options, args);
    } catch (ParseException e) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("JoinH", options, true);
      System.exit(1);
    }
    SI_Tree.skipEquiCheck = cmd.hasOption("skipequiv");
    return cmd.getArgs();
  }

  private static Options buildOptions() {
    Options options = new Options();
    options.addOption("skipequiv", false, "Skip equivalency check");
    return options;
  }
}
