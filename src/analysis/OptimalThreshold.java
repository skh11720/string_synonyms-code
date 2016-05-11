package analysis;

import java.io.IOException;
import java.util.List;

import mine.Record;
import tools.Algorithm;
import tools.RuleTrie;
import tools.Rule_ACAutomata;

public class OptimalThreshold extends Algorithm {
  Rule_ACAutomata ruleatm;
  RuleTrie        trie;

  protected OptimalThreshold(String rulefile, String Rfile, String Sfile)
      throws IOException {
    super(rulefile, Rfile, Sfile);
    ruleatm = new Rule_ACAutomata(rulelist);
    trie = new RuleTrie(rulelist);
    Record.setRuleTrie(trie);
  }

  public static void main(String[] args) throws IOException {
    OptimalThreshold inst = new OptimalThreshold(args[2], args[0], args[1]);
    inst.measureExpandTime();
  }

  private void measureExpandTime() {
    long totaltime = 0;
    long expandedsize = 0;
    long totalexpandedsize = 0;
    long cost = 0;
    long sizesum = 0;
    for (Record rec : tableR) {
      rec.preprocessRules(ruleatm, false);
      rec.preprocessSuffixApplicableRules();
      rec.preprocessEstimatedRecords();
      if (rec.getEstNumRecords() <= 1E5) {
        long starttime = System.nanoTime();
        List<Record> expanded = rec.expandAll(trie);
        long duration = System.nanoTime() - starttime;
        expandedsize += expanded.size();
        cost += rec.getEstExpandCost();
        totaltime += duration;
        sizesum += rec.size();
      }
      totalexpandedsize += rec.getEstNumRecords();
    }
    System.out.println("Total " + totalexpandedsize + " exp recs");
    System.out.println("Total(<\\theta) " + expandedsize + " exp recs");
    System.out.println("Total " + cost + " cost");
    System.out.println("Total " + sizesum + " sizesum");
    System.out.println(totaltime + "ns");
    System.out.println("Avg " + (totaltime / cost) + " ns");
  }
}
