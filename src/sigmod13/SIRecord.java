package sigmod13;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import sigmod13.filter.ITF_Filter;
import tools.IntegerSet;
import tools.Rule;
import tools.Rule_ACAutomata;

public class SIRecord implements RecordInterface, Comparable<SIRecord> {
  private final int                  id;
  private final IntegerSet           tokens;
  final IntegerSet                   fullExpanded;
  final HashSet<Rule>                applicableRules;
  private static final HashSet<Rule> emptyRules = new HashSet<Rule>();

  /**
   * Create a record and preprocess applicable rules
   */
  public SIRecord(int id, String str, HashMap<String, Integer> str2int,
      Rule_ACAutomata automata) {
    this.id = id;
    String[] pstr = str.split("( |\t)+");
    int[] tokens = new int[pstr.length];
    this.tokens = new IntegerSet();
    for (int i = 0; i < pstr.length; ++i) {
      tokens[i] = str2int.get(pstr[i]);
      this.tokens.add(tokens[i]);
    }

    // Rules
    applicableRules = new HashSet<Rule>();
    for (Rule rule : automata.applicableRules(tokens))
      applicableRules.add(rule);

    // Full expand
    fullExpanded = this.tokens.copy();
    for (Rule rule : applicableRules)
      for (int s : rule.getTo())
        fullExpanded.add(s);
  }

  public SIRecord(SIRecord rec) {
    id = -1;
    tokens = rec.tokens.copy();
    // Applicable rules does not change
    applicableRules = rec.applicableRules;
    fullExpanded = rec.fullExpanded;
  }

  public SIRecord(SIRecordExpanded rec) {
    id = -1;
    tokens = new IntegerSet(rec.getOriginalTokens());
    tokens.addAll(rec.getExpandedTokens());
    fullExpanded = tokens;
    applicableRules = emptyRules;
  }

  public int getID() {
    return id;
  }

  public final HashSet<Rule> getApplicableRules() {
    return applicableRules;
  }

  /**
   * Generate all the possible expanded sets
   */
  @Override
  public HashSet<SIRecordExpanded> generateAll() {
    try {
      Queue<SIRecordExpanded> queue = new LinkedList<SIRecordExpanded>();
      queue.add(new SIRecordExpanded(this));

      Queue<SIRecordExpanded> bufferQueue = new LinkedList<SIRecordExpanded>();
      for (Rule rule : applicableRules) {
        if (rule.getFrom().length == 1 && rule.getTo().length == 1
            && rule.getFrom()[0] == rule.getTo()[0])
          continue;
        while (!queue.isEmpty()) {
          SIRecordExpanded curr = queue.poll();
          SIRecordExpanded expanded = new SIRecordExpanded(curr);
          expanded.applyRule(rule);
          bufferQueue.add(curr);
          bufferQueue.add(expanded);
        }
        Queue<SIRecordExpanded> tmpqueue = bufferQueue;
        bufferQueue = queue;
        queue = tmpqueue;
      }
      HashSet<SIRecordExpanded> rslt = new HashSet<SIRecordExpanded>(queue);
      return rslt;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public void applyRule(Rule rule) throws Exception {
    for (int s : rule.getFrom())
      if (!tokens.contains(s)) throw new Exception("Not applicable rule");
    for (int s : rule.getTo())
      tokens.add(s);
  }

  public void applyRule(HashSet<Rule> rules) throws Exception {
    for (Rule rule : rules) {
      for (int s : rule.getFrom())
        if (!tokens.contains(s)) throw new Exception("Not applicable rule");
      for (int s : rule.getTo())
        tokens.add(s);
    }
  }

  public double calcJaccard(SIRecord o) {
    int cupsize = 0;
    for (Integer str : tokens)
      if (o.tokens.contains(str)) ++cupsize;
    return (double) cupsize
        / (double) (tokens.size() + o.tokens.size() - cupsize);
  }

  public double calcFullJaccard(SIRecord o) {
    int cupsize = 0;
    for (Integer str : fullExpanded)
      if (o.fullExpanded.contains(str)) ++cupsize;
    return (double) cupsize
        / (double) (fullExpanded.size() + o.fullExpanded.size() - cupsize);
  }

  public boolean contains(int token) {
    return tokens.contains(token);
  }

  public boolean fullExpandedContains(int token) {
    return fullExpanded.contains(token);
  }

  @Override
  public int hashCode() {
    return tokens.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    return id == ((SIRecord) o).id;
  }

  @Override
  public String toString() {
    return tokens.toString();
  }

  @Override
  public int getMinLength() {
    return tokens.size();
  }

  @Override
  public int getMaxLength() {
    return fullExpanded.size();
  }

  @Override
  public int size() {
    return tokens.size();
  }

  @Override
  public Collection<Integer> getTokens() {
    return tokens;
  }

  @Override
  public Set<Integer> getSignatures(ITF_Filter filter, double theta) {
    IntegerSet signature = new IntegerSet();
    if (theta == 1) {
      signature.addAll(filter.filter(new SIRecordExpanded(this), 1));
      return signature;
    }
    HashSet<SIRecordExpanded> expanded = generateAll();
    for (SIRecordExpanded exp : expanded) {
      // In the paper the number of signature is states as belows.
      // int cut = (int) Math.ceil((1.0 - theta) * exp.size());
      // However, it should be
      int cut = 1 + exp.size() - (int) Math.ceil(theta * exp.size());
      HashSet<Integer> sig = filter.filter(exp, cut);
      signature.addAll(sig);
    }
    return signature;
  }

  @Override
  public double similarity(RecordInterface rec) {
    if (rec.getClass() != SIRecord.class) return 0;
    return SimilarityFunc.fullExp2(this, (SIRecord) rec);
  }

  @Override
  public int compareTo(SIRecord o) {
    return Integer.compare(id, o.id);
  }
}
