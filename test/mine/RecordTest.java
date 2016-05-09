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

  private static List<Set<String>> toString(List<Set<Long>> twograms) {
    List<Set<String>> twogramstrings = new ArrayList<Set<String>>();
    for (int i = 0; i < twograms.size(); ++i) {
      Set<String> twogramstri = new HashSet<String>();
      for (Long twogram : twograms.get(i))
        twogramstri.add(Record.twoGram2String(twogram));
      twogramstrings.add(twogramstri);
    }
    return twogramstrings;
  }

  @Test
  public void test2Grams1() {
    String str = "A B C D";
    String[] rulearray = new String[] { "A, a", "C, c", "B C, x" };
    build(str, rulearray);
    List<Set<Long>> twograms = s.get2Grams();
    List<Set<String>> twogramstrings = toString(twograms);

    assertEquals(4, twogramstrings.size());
    Set<String> first2grams = twogramstrings.get(0);
    assertEquals(4, first2grams.size());
    first2grams.contains("A B");
    first2grams.contains("a B");
    first2grams.contains("A x");
    first2grams.contains("a x");
    Set<String> second2grams = twogramstrings.get(1);
    assertEquals(3, second2grams.size());
    second2grams.contains("B C");
    second2grams.contains("B x");
    second2grams.contains("x D");
    Set<String> third2grams = twogramstrings.get(2);
    assertEquals(3, third2grams.size());
    third2grams.contains("C D");
    third2grams.contains("x D");
    third2grams.contains("D");
    Set<String> fourth2grams = twogramstrings.get(3);
    assertEquals(1, fourth2grams.size());
    fourth2grams.contains("D");
  }

  @Test
  public void test2Grams2() {
    String str = "A B C D";
    String[] rulearray = new String[] { "A, a a", "C, c X", "B, b" };
    build(str, rulearray);
    List<Set<Long>> twograms = s.get2Grams();
    List<Set<String>> twogramstrings = toString(twograms);

    assertEquals(6, twogramstrings.size());
    Set<String> first2grams = twogramstrings.get(0);
    assertEquals(3, first2grams.size());
    first2grams.contains("a a");
    first2grams.contains("A b");
    first2grams.contains("A B");
    Set<String> second2grams = twogramstrings.get(1);
    assertEquals(6, second2grams.size());
    second2grams.contains("B C");
    second2grams.contains("a B");
    second2grams.contains("b C");
    second2grams.contains("a b");
    second2grams.contains("B c");
    second2grams.contains("b c");
    Set<String> third2grams = twogramstrings.get(2);
    assertEquals(6, second2grams.size());
    third2grams.contains("C D");
    third2grams.contains("B C");
    third2grams.contains("b C");
    third2grams.contains("c X");
    third2grams.contains("B c");
    third2grams.contains("b c");
    Set<String> fourth2grams = twogramstrings.get(3);
    assertEquals(4, fourth2grams.size());
    fourth2grams.contains("C D");
    fourth2grams.contains("X D");
    fourth2grams.contains("c X");
    fourth2grams.contains("D");
    Set<String> fifth2grams = twogramstrings.get(4);
    assertEquals(2, fifth2grams.size());
    fifth2grams.contains("X D");
    fifth2grams.contains("D");
    Set<String> sixth2grams = twogramstrings.get(5);
    assertEquals(1, sixth2grams.size());
    sixth2grams.contains("D");
  }

  @Test
  public void test2Grams3() {
    String str = "A B C D";
    String[] rulearray = new String[] { "A, a", "B, b x y", "C, c" };
    build(str, rulearray);
    List<Set<Long>> twograms = s.get2Grams();
    List<Set<String>> twogramstrings = toString(twograms);

    assertEquals(6, twogramstrings.size());
    Set<String> first2grams = twogramstrings.get(0);
    assertEquals(4, first2grams.size());
    first2grams.contains("A B");
    first2grams.contains("a B");
    first2grams.contains("A b");
    first2grams.contains("a b");
    Set<String> second2grams = twogramstrings.get(1);
    assertEquals(3, second2grams.size());
    second2grams.contains("B C");
    second2grams.contains("b x");
    second2grams.contains("B c");
    Set<String> third2grams = twogramstrings.get(2);
    assertEquals(3, third2grams.size());
    third2grams.contains("C D");
    third2grams.contains("x y");
    third2grams.contains("c D");
    Set<String> fourth2grams = twogramstrings.get(3);
    assertEquals(5, fourth2grams.size());
    fourth2grams.contains("y C");
    fourth2grams.contains("y c");
    fourth2grams.contains("C D");
    fourth2grams.contains("c D");
    fourth2grams.contains("D");
    Set<String> fifth2grams = twogramstrings.get(4);
    assertEquals(3, fifth2grams.size());
    fifth2grams.contains("C D");
    fifth2grams.contains("c D");
    fifth2grams.contains("D");
    Set<String> sixth2grams = twogramstrings.get(5);
    assertEquals(1, sixth2grams.size());
    sixth2grams.contains("D");
  }

  @Test
  public void testExact2Grams1() {
    String str = "A B C D";
    String[] rulearray = new String[] { "A, a", "B, b x y", "C, c" };
    build(str, rulearray);
    List<Set<Long>> twograms = s.getExact2Grams();
    List<Set<String>> twogramstrings = toString(twograms);

    assertEquals(6, twogramstrings.size());
    Set<String> first2grams = twogramstrings.get(0);
    assertEquals(4, first2grams.size());
    first2grams.contains("A B");
    first2grams.contains("a B");
    first2grams.contains("A b");
    first2grams.contains("a b");
    Set<String> second2grams = twogramstrings.get(1);
    assertEquals(3, second2grams.size());
    second2grams.contains("B C");
    second2grams.contains("b x");
    second2grams.contains("B c");
    Set<String> third2grams = twogramstrings.get(2);
    assertEquals(3, third2grams.size());
    third2grams.contains("C D");
    third2grams.contains("x y");
    third2grams.contains("c D");
    Set<String> fourth2grams = twogramstrings.get(3);
    assertEquals(3, fourth2grams.size());
    fourth2grams.contains("y C");
    fourth2grams.contains("y c");
    fourth2grams.contains("D");
    Set<String> fifth2grams = twogramstrings.get(4);
    assertEquals(2, fifth2grams.size());
    fifth2grams.contains("C D");
    fifth2grams.contains("c D");
    Set<String> sixth2grams = twogramstrings.get(5);
    assertEquals(1, sixth2grams.size());
    sixth2grams.contains("D");
  }

  @Test
  public void testExact2Grams2() {
    String str = "A B C D";
    String[] rulearray = new String[] { "A, a a", "C, c X", "B, b" };
    build(str, rulearray);
    List<Set<Long>> twograms = s.get2Grams();
    List<Set<String>> twogramstrings = toString(twograms);

    assertEquals(6, twogramstrings.size());
    Set<String> first2grams = twogramstrings.get(0);
    assertEquals(3, first2grams.size());
    first2grams.contains("A B");
    first2grams.contains("A b");
    first2grams.contains("a a");
    Set<String> second2grams = twogramstrings.get(1);
    assertEquals(6, second2grams.size());
    second2grams.contains("B C");
    second2grams.contains("B c");
    second2grams.contains("b C");
    second2grams.contains("b c");
    second2grams.contains("a B");
    second2grams.contains("a b");
    Set<String> third2grams = twogramstrings.get(2);
    assertEquals(6, second2grams.size());
    third2grams.contains("C D");
    third2grams.contains("c X");
    third2grams.contains("B C");
    third2grams.contains("B c");
    third2grams.contains("b C");
    third2grams.contains("b c");
    Set<String> fourth2grams = twogramstrings.get(3);
    assertEquals(4, fourth2grams.size());
    fourth2grams.contains("D");
    fourth2grams.contains("X D");
    fourth2grams.contains("C D");
    fourth2grams.contains("c X");
    Set<String> fifth2grams = twogramstrings.get(4);
    assertEquals(2, fifth2grams.size());
    fifth2grams.contains("X D");
    fifth2grams.contains("D");
    Set<String> sixth2grams = twogramstrings.get(5);
    assertEquals(1, sixth2grams.size());
    sixth2grams.contains("D");
  }

  @Test
  public void testExact2Grams3() {
    String str = "A B C D";
    String[] rulearray = new String[] { "A, a a w", "B C, c X", "B, b y z" };
    build(str, rulearray);
    List<Set<Long>> twograms = s.getExact2Grams();
    List<Set<String>> twogramstrings = toString(twograms);

    assertEquals(8, twogramstrings.size());
    Set<String> first2grams = twogramstrings.get(0);
    assertEquals(4, first2grams.size());
    first2grams.contains("A B");
    first2grams.contains("A b");
    first2grams.contains("A c");
    first2grams.contains("a a");
    Set<String> second2grams = twogramstrings.get(1);
    assertEquals(4, second2grams.size());
    second2grams.contains("B C");
    second2grams.contains("b y");
    second2grams.contains("c X");
    second2grams.contains("a w");
    Set<String> third2grams = twogramstrings.get(2);
    assertEquals(6, third2grams.size());
    third2grams.contains("C D");
    third2grams.contains("y z");
    third2grams.contains("X D");
    third2grams.contains("w B");
    third2grams.contains("w b");
    third2grams.contains("w c");
    Set<String> fourth2grams = twogramstrings.get(3);
    assertEquals(5, fourth2grams.size());
    fourth2grams.contains("D");
    fourth2grams.contains("z C");
    fourth2grams.contains("B C");
    fourth2grams.contains("b y");
    fourth2grams.contains("c X");
    Set<String> fifth2grams = twogramstrings.get(4);
    assertEquals(3, fifth2grams.size());
    fifth2grams.contains("C D");
    fifth2grams.contains("y z");
    fifth2grams.contains("X D");
    Set<String> sixth2grams = twogramstrings.get(5);
    assertEquals(2, sixth2grams.size());
    sixth2grams.contains("D");
    sixth2grams.contains("z c");
    Set<String> seventh2grams = twogramstrings.get(6);
    assertEquals(1, seventh2grams.size());
    seventh2grams.contains("c D");
    Set<String> eighth2grams = twogramstrings.get(7);
    assertEquals(1, eighth2grams.size());
    eighth2grams.contains("D");
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
