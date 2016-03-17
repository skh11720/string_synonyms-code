package mine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import tools.Rule;
import tools.RuleTrie;
import tools.Rule_ACAutomata;

public class RecordTest {
  private static Map<String, Integer> str2int;
  private static List<String>         int2str;
  private static RuleTrie             trie;
  private static Rule_ACAutomata      atm;
  private static Record               s;

  @BeforeClass
  public static void beforeclass() {
    str2int = new HashMap<String, Integer>();
    int2str = new ArrayList<String>();
  }

  @Before
  public void before() {
    str2int.clear();
    int2str.clear();
  }

  @Test
  public void testExpand() {
    String str = "A B C D";
    String[] rulearray = new String[] { "A, a", "C, c", "B C, x" };
    build(str, rulearray);
    List<Record> expanded = s.expandAll();
    Set<String> set = new HashSet<String>();
    for (Record exp : expanded)
      set.add(exp.toString());
    assertEquals(6, expanded.size());
    assertTrue(set.contains("A B C D "));
    assertTrue(set.contains("a B C D "));
    assertTrue(set.contains("A B c D "));
    assertTrue(set.contains("a B c D "));
    assertTrue(set.contains("A x D "));
    assertTrue(set.contains("a x D "));
  }

  @Test
  public void testExpand2() {
    String str = "A B C D";
    String[] rulearray = new String[] { "A, a", "C, c", "B, b" };
    build(str, rulearray);
    List<Record> expanded = s.expandAll();
    Set<String> set = new HashSet<String>();
    for (Record exp : expanded)
      set.add(exp.toString());
    assertEquals(8, expanded.size());
    assertTrue(set.contains("A B C D "));
    assertTrue(set.contains("a B C D "));
    assertTrue(set.contains("A b C D "));
    assertTrue(set.contains("a b C D "));
    assertTrue(set.contains("A B c D "));
    assertTrue(set.contains("a B c D "));
    assertTrue(set.contains("A b c D "));
    assertTrue(set.contains("a b c D "));
  }

  @Test
  public void testExpand3() {
    String str = "anchor chain";
    String[] rulearray = new String[] { "anchor, keystone", "anchor, lime" };
    build(str, rulearray);
    List<Record> expanded = s.expandAll();
    Set<String> set = new HashSet<String>();
    for (Record exp : expanded)
      set.add(exp.toString());
    assertEquals(3, expanded.size());
    assertTrue(set.contains("anchor chain "));
    assertTrue(set.contains("keystone chain "));
    assertTrue(set.contains("lime chain "));
  }

  /**
   * Build test case (generate records/strint-to-integer map and preprocess
   * records)
   */
  private void build(String str, String[] rulearray) {
    str2int.clear();
    int2str.clear();
    add(str);
    for (String rule : rulearray)
      add(rule);

    // Transform to s,t, and rule_automata
    s = new Record(0, str, str2int);
    List<Rule> rulelist = new ArrayList<Rule>();
    for (String rule : rulearray)
      rulelist.add(new Rule(rule, str2int));
    for (Integer token : str2int.values())
      rulelist.add(new Rule(token, token));
    atm = new Rule_ACAutomata(rulelist);
    trie = new RuleTrie(rulelist);
    Record.setRuleTrie(trie);
    Record.setStrList(int2str);

    // Preprocess records
    preprocess(s, atm);
  }

  private static void preprocess(Record rec, Rule_ACAutomata rac) {
    rec.preprocessRules(rac, false);
    rec.preprocessLengths();
    rec.preprocessSearchRanges();
    rec.preprocessSuffixApplicableRules();
  }

  /**
   * Add this string to string-to-integer map
   */
  private void add(String str) {
    String[] pstr = str.split("[ ,\t]+");
    for (String token : pstr) {
      if (str2int.containsKey(token)) continue;
      str2int.put(token, str2int.size());
      int2str.add(token);
    }
  }
}
