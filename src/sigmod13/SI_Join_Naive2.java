package sigmod13;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import sigmod13.filter.ITF1;
import sigmod13.filter.ITF2;
import sigmod13.filter.ITF3;
import sigmod13.filter.ITF4;
import sigmod13.filter.ITF_Filter;
import tools.Algorithm;
import tools.IntegerMap;
import tools.Pair;
import tools.Rule;

/**
 * SI-Join algorithm which does not miss answer.
 * Generate all expanded records and map them to the original record.
 */
public class SI_Join_Naive2 extends Algorithm {
  IntegerMap<HashSet<SIRecordExpanded>>        idxR;
  IntegerMap<HashSet<SIRecordExpanded>>        idxS;
  HashMap<SIRecordExpanded, HashSet<SIRecord>> mapR;
  HashMap<SIRecordExpanded, HashSet<SIRecord>> mapS;
  ArrayList<SIRecord>                          tableR;
  ArrayList<SIRecord>                          tableS;
  ArrayList<Rule>                              rulelist;

  public SI_Join_Naive2(String DBR_file, String DBS_file, String rulefile)
      throws IOException {
    super(rulefile, DBR_file, DBS_file);
  }

  private void buildMap() {
    mapR = new HashMap<SIRecordExpanded, HashSet<SIRecord>>();
    mapS = new HashMap<SIRecordExpanded, HashSet<SIRecord>>();
    for (SIRecord rec : tableR) {
      for (SIRecordExpanded exp : rec.generateAll()) {
        if (!mapR.containsKey(exp)) mapR.put(exp, new HashSet<SIRecord>());
        mapR.get(exp).add(rec);
      }
    }
    for (SIRecord rec : tableS) {
      for (SIRecordExpanded exp : rec.generateAll()) {
        if (!mapS.containsKey(exp)) mapS.put(exp, new HashSet<SIRecord>());
        mapS.get(exp).add(rec);
      }
    }
  }

  public void run(double threshold, int filterType) throws IOException {
    buildMap();
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
    idxR = new IntegerMap<HashSet<SIRecordExpanded>>();
    idxS = new IntegerMap<HashSet<SIRecordExpanded>>();
    for (SIRecordExpanded exp : mapR.keySet()) {
      int prefix_size = exp.size() - (int) Math.ceil(threshold * exp.size())
          + 1;
      HashSet<Integer> sigset = filterR.filter(exp, prefix_size);
      for (Integer sig : sigset) {
        if (!idxR.containsKey(sig))
          idxR.put(sig, new HashSet<SIRecordExpanded>());
        idxR.get(sig).add(exp);
      }
    }
    for (SIRecordExpanded exp : mapS.keySet()) {
      int prefix_size = exp.size() - (int) Math.ceil(threshold * exp.size())
          + 1;
      HashSet<Integer> sigset = filterS.filter(exp, prefix_size);
      for (Integer sig : sigset) {
        if (!idxS.containsKey(sig))
          idxS.put(sig, new HashSet<SIRecordExpanded>());
        idxS.get(sig).add(exp);
      }
    }

    System.out.print("Building Index finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));

    join(threshold);
  }

  public void join(double threshold) {
    long startTime = System.currentTimeMillis();

    HashSet<Pair<SIRecord>> candidates = new HashSet<Pair<SIRecord>>();
    for (Map.Entry<Integer, HashSet<SIRecordExpanded>> entry : idxR
        .entrySet()) {
      if (!idxS.containsKey(entry.getKey())) continue;
      HashSet<SIRecordExpanded> iidxR = entry.getValue();
      HashSet<SIRecordExpanded> iidxS = idxS.get(entry.getKey());
      for (SIRecordExpanded expR : iidxR)
        for (SIRecordExpanded expS : iidxS) {
          if (expR.jaccard(expS) >= threshold) {
            HashSet<SIRecord> recRs = mapR.get(expR);
            HashSet<SIRecord> recSs = mapS.get(expS);
            for (SIRecord recR : recRs)
              for (SIRecord recS : recSs)
                candidates.add(new Pair<SIRecord>(recR, recS));
          }
        }
    }
    System.out.print("Validating finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));
    System.out.println("Similar pairs : " + candidates.size());
    for (Pair<SIRecord> pair : candidates)
      System.out.println(pair.rec1.toString() + " == " + pair.rec2.toString());
  }

  /**
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws IOException {
    if (args.length != 5) {
      printUsage();
      return;
    }
    String Rfile = args[0];
    String Sfile = args[1];
    String Rulefile = args[2];
    double threshold = Double.parseDouble(args[3]);
    int filterNo = Integer.parseInt(args[4]);

    long startTime = System.currentTimeMillis();
    SI_Join_Naive2 inst = new SI_Join_Naive2(Rfile, Sfile, Rulefile);
    System.out.print("Constructor finished");
    System.out.println(" " + (System.currentTimeMillis() - startTime));
    inst.run(threshold, filterNo);
  }

  private static void printUsage() {
    System.out.println(
        "Usage : <R file> <S file> <Rule file> <Threshold> <Filter No.>");
  }
}
