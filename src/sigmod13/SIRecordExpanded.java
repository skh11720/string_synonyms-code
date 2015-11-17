package sigmod13;

import tools.IntegerSet;
import tools.Rule;

/**
 * Expanded record
 */
public class SIRecordExpanded {
  private IntegerSet originalTokens;
  private IntegerSet expandedTokens;

  public SIRecordExpanded(SIRecord rec) {
    originalTokens = rec.getTokens();
    expandedTokens = new IntegerSet();
  }

  public SIRecordExpanded(SIRecordExpanded rec) {
    originalTokens = new IntegerSet(rec.originalTokens);
    expandedTokens = new IntegerSet(rec.expandedTokens);
  }

  public IntegerSet getOriginalTokens() {
    return originalTokens;
  }

  public IntegerSet getExpandedTokens() {
    return expandedTokens;
  }

  public int getSize() {
    return originalTokens.size() + expandedTokens.size();
  }

  public void applyRule(Rule rule) throws Exception {
    for (int s : rule.getFrom())
      if (!originalTokens.contains(s))
        throw new Exception("Not applicable rule");
    for (int s : rule.getTo())
      if (!originalTokens.contains(s)) expandedTokens.add(s);
  }

  @Override
  public String toString() {
    return originalTokens.toString() + " + " + expandedTokens.toString();
  }

  @Override
  public boolean equals(Object o) {
    SIRecordExpanded sire = (SIRecordExpanded) o;
    if (hashCode() != sire.hashCode()) return false;
    return originalTokens.equals(sire.originalTokens)
        && expandedTokens.equals(sire.expandedTokens);
  }

  @Override
  public int hashCode() {
    return originalTokens.hashCode() + expandedTokens.hashCode();
  }

  public double jaccard(SIRecordExpanded o) {
    int intersection = 0;
    for (Integer token : originalTokens) {
      if (o.originalTokens.contains(token))
        ++intersection;
      else if (o.expandedTokens.contains(token)) ++intersection;
    }
    for (Integer token : expandedTokens) {
      if (o.originalTokens.contains(token))
        ++intersection;
      else if (o.expandedTokens.contains(token)) ++intersection;
    }
    int union = originalTokens.size() + expandedTokens.size()
        + o.originalTokens.size() + o.expandedTokens.size() - intersection;
    return (double) intersection / union;
  }
}
