package sigmod13;

import java.util.HashSet;

import tools.Rule;

public class SimilarityFunc {
  public static long invoked = 0;

  /**
   * Calculate Full-expanded Jaccard similarity value
   */
  public static double fullExp(SIRecord rec1, SIRecord rec2) {
    SIRecord erec1 = new SIRecord(rec1);
    SIRecord erec2 = new SIRecord(rec2);
    try {
      for (Rule rule : erec1.applicableRules)
        erec1.applyRule(rule);
      for (Rule rule : erec2.applicableRules)
        erec2.applyRule(rule);
    } catch (Exception e) {
      // This should never be happen
      e.printStackTrace();
    }
    return erec1.calcJaccard(erec2);
  }

  /**
   * Calculate Full-expanded Jaccard similarity value <br/>
   * Use their pre-expanded record
   */
  public static double fullExp2(SIRecord rec1, SIRecord rec2) {
    return rec1.calcFullJaccard(rec2);
  }

  public static double exactSelectiveExp(SIRecord rec1, SIRecord rec2) {
    double max = 0;
    HashSet<SIRecordExpanded> expanded = rec1.generateAll();
    for (SIRecordExpanded exp2 : rec2.generateAll())
      for (SIRecordExpanded exp : expanded) {
        double sim = exp2.jaccard(exp);
        max = Math.max(sim, max);
      }
    return max;
  }

  /**
   * Calculate Selective-expanded Jaccard similarity value <br/>
   * Algorithm 2 in the paper
   */
  public static double selectiveExp(SIRecord rec1, SIRecord rec2) {
    ++invoked;

    // Line 1 : Calcualte candidate rule set
    // Procedure findCandidateRuleSet(), line 4
    // Line 4 : Calcualte candidate rule set
    HashSet<Rule> C1 = new HashSet<Rule>();
    for (Rule rule : rec1.applicableRules)
      if (ruleGain(rule, rec1, rec2) > 0) C1.add(rule);
    HashSet<Rule> C2 = new HashSet<Rule>();
    for (Rule rule : rec2.applicableRules)
      if (ruleGain(rule, rec2, rec1) > 0) C2.add(rule);

    // Line 5 : repeat until no rule can be removed
    // TODO : Bottle neck
    boolean removed = false;
    do {
      removed = false;
      SIRecord erec1 = new SIRecord(rec1);
      SIRecord erec2 = new SIRecord(rec2);
      try {
        // Line 6 : Calculate S'_1
        erec1.applyRule(C1);
        // Line 7 : Calculate S'_2
        erec2.applyRule(C2);
      } catch (Exception e) {
        // This should never be happen
        e.printStackTrace();
      }
      // Line 8 : calculate current Jaccard similarity
      double theta = erec1.calcJaccard(erec2);
      double threshold = theta / (1 + theta);

      // Line 9 ~ 11
      // Java HashSet throws ConcurrentModificationException if trying to
      // modify while iterating. Therefore, we have use some buffer
      // HashSet.
      HashSet<Rule> buffer = new HashSet<Rule>();
      for (Rule rule : C1)
        if (ruleGain(rule, rec1, rec2) < threshold)
          removed = true;
        else
          buffer.add(rule);
      C1 = buffer;
      buffer = new HashSet<Rule>();

      for (Rule rule : C2)
        if (ruleGain(rule, rec2, rec1) < threshold)
          removed = true;
        else
          buffer.add(rule);
      C2 = buffer;
    } while (removed);

    // Line 2 : calculate \theta
    // Procedure expand(), line 13
    // Line 13 : Initialize S'_1
    SIRecord erec1 = new SIRecord(rec1);
    // Line 14 : Initialize S'_2
    SIRecord erec2 = new SIRecord(rec2);
    // Line 15 : repeat until there is no applicable rule
    while (C1.size() != 0 || C2.size() != 0) {
      // Line 16 : find the current most gain-effective rule
      Rule best_rule = null;
      double max_gain = Double.NEGATIVE_INFINITY;
      boolean best_from_1 = true;
      for (Rule rule : C1) {
        double gain = ruleGain(rule, erec1, erec2);
        if (gain > max_gain) {
          best_rule = rule;
          max_gain = gain;
        }
      }
      for (Rule rule : C2) {
        double gain = ruleGain(rule, erec2, erec1);
        if (gain > max_gain) {
          best_from_1 = false;
          best_rule = rule;
          max_gain = gain;
        }
      }

      // Line 17 : Check the best rule gain
      if (max_gain > 0) {
        // Line 18 : Expand
        try {
          if (best_from_1)
            erec1.applyRule(best_rule);
          else
            erec2.applyRule(best_rule);
        } catch (Exception e) {
          // This should never be happen
          e.printStackTrace();
        }
      }

      // Line 19 : remove the best rule
      if (best_from_1)
        C1.remove(best_rule);
      else
        C2.remove(best_rule);

    }

    // Line 20 : return the similarity
    return Math.max(erec1.calcJaccard(erec2), rec1.calcJaccard(rec2));
  }

  /**
   * Calculate rule gain
   * 
   * @param rule
   *          An applicable rule of rec1
   */
  private static double ruleGain(Rule rule, SIRecord rec1, SIRecord rec2) {
    // Line 1 : Calculate |U| instead of U
    int sizeU = 0;
    for (int str : rule.getTo())
      if (!rec1.getTokens().contains(str)) ++sizeU;

    // Line 2-3
    if (sizeU == 0) return 0;

    // Line 4
    // S'_2 is already calculated

    // Line 5 : Calculate G
    HashSet<Integer> G = new HashSet<Integer>();
    for (int str : rule.getTo())
      if (rec2.fullExpanded.contains(str)) G.add(str);
    G.removeAll(rec1.getTokens());

    // Line 6
    return (double) G.size() / sizeU;
  }
}
