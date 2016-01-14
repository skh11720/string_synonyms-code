package mine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import tools.Algorithm;
import tools.Rule;
import tools.Rule_ACAutomata;

public class FilterLargeEquivs extends Algorithm {

  protected FilterLargeEquivs(String rulefile, String Rfile)
      throws IOException {
    super(rulefile, Rfile, Rfile);
  }

  public static void main(String[] args) throws Exception {
    String inputfile = args[0];
    String rulefile = args[1];
    String outputfile = args[2];
    int threshold = Integer.valueOf(args[3]);
    FilterLargeEquivs inst = new FilterLargeEquivs(rulefile, inputfile);
    List<Rule> rules = inst.readRules(rulefile);
    List<Record> records = inst.readRecords(inputfile, -1);
    BufferedWriter bw = new BufferedWriter(new FileWriter(outputfile));
    Rule_ACAutomata automata = new Rule_ACAutomata(rules);
    Record.setStrList(inst.strlist);
    for (Record rec : records) {
      rec.preprocessRules(automata, false);
      rec.preprocessEstimatedRecords();
      if (rec.getEstNumRecords() > threshold) continue;
      bw.write(rec.toString() + "\n");
    }
    bw.close();
  }

  private List<Rule> readRules(String Rulefile) throws IOException {
    List<Rule> rulelist = new ArrayList<Rule>();
    BufferedReader br = new BufferedReader(new FileReader(Rulefile));
    String line;
    while ((line = br.readLine()) != null) {
      rulelist.add(new Rule(line, str2int));
    }
    br.close();

    // Add Self rule
    for (int token : str2int.values())
      rulelist.add(new Rule(token, token));
    return rulelist;
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

}
