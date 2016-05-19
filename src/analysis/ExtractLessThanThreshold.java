package analysis;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import mine.Record;
import tools.Algorithm;
import tools.RuleTrie;
import tools.Rule_ACAutomata;

public class ExtractLessThanThreshold extends Algorithm {
  Rule_ACAutomata ruleatm;
  RuleTrie        trie;

  protected ExtractLessThanThreshold(String rulefile, String Rfile,
      String Sfile) throws IOException {
    super(rulefile, Rfile, Sfile);
    ruleatm = new Rule_ACAutomata(rulelist);
    trie = new RuleTrie(rulelist);
    Record.setRuleTrie(trie);
  }

  public static void main(String[] args) throws IOException {
    if (args.length != 4) {
      printUsage();
      System.exit(0);
    }
    ExtractLessThanThreshold inst = new ExtractLessThanThreshold(args[1],
        args[0], args[0]);
    int threshold = Integer.parseInt(args[2]);
    inst.extract(threshold, args[3]);
  }

  private static void printUsage() {
    System.out.println("Usage : [Data] [Rule] [Threshold] [Output]");
  }

  private void extract(int threshold, String outputfile) throws IOException {
    BufferedWriter bw = new BufferedWriter(new FileWriter(outputfile));
    for (Record str : tableR) {
      str.preprocessRules(ruleatm, false);
      str.preprocessEstimatedRecords();
      long est = str.getEstNumRecords();
      if (est <= threshold) bw.write(str.toString(strlist) + "\n");
    }
    bw.close();
  }
}
