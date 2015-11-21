package sigmod13;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

import tools.IntegerSet;
import tools.Rule;
import tools.Rule_ACAutomata;

public class SIRecord {
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
    fullExpanded = new IntegerSet(this.tokens);
    for (Rule rule : applicableRules)
      for (int s : rule.getTo())
        fullExpanded.add(s);
  }

  public SIRecord(SIRecord rec) {
    id = -1;
    tokens = new IntegerSet(rec.tokens);
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

  public IntegerSet getTokens() {
    return tokens;
  }

  public final HashSet<Rule> getApplicableRules() {
    return applicableRules;
  }

  /**
   * Generate all the possible expanded sets
   */
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
}
