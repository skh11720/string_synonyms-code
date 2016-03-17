package tools;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import mine.Record;
import mine.Validator;

public class ValidatorTest {
  private static Map<String, Integer> str2int;
  private static List<String>         int2str;
  private static Record               s;
  private static Record               t;

  @BeforeClass
  public static void beforeclass() {
    str2int = new HashMap<String, Integer>();
    int2str = new ArrayList<String>();
  }

  @Before
  public void before() {
    str2int.clear();
    int2str.clear();
    clearStats();
  }

  @Test
  public void testNotEqualStringSS() {
    String str1 = "A B C D";
    String str2 = "a b c d";
    String[] rulearray = new String[] { "A, a", "C, c", "B C, x" };
    build(str1, str2, rulearray);
    checkSSEP(10, 7, 6, true, -1);
    clearStats();
    checkSS(16, 28, -1);
  }

  @Test
  public void testEqualStringSS() {
    String str1 = "A B C D";
    String str2 = "a b c d";
    String[] rulearray = new String[] { "A, a", "B, x", "B C, b c", "D, d" };
    build(str1, str2, rulearray);
    checkSSEP(16, 11, 10, false, 3);
    clearStats();
    checkSS(16, 32, 3);
  }

  @Test
  public void testNotEqualStringDS() {
    String str1 = "A B C D";
    String str2 = "a b c d";
    String[] rulearray = new String[] { "A, a", "D, d", "B C, x" };
    build(str1, str2, rulearray);
    checkDSEP(17, 11, 8, 8, true, -1);
    clearStats();
    checkDS(24, 55, 8, -1);
  }

  @Test
  public void testEqualStringDS() {
    String str1 = "A B C D";
    String str2 = "a b c d";
    String[] rulearray = new String[] { "A, a", "B, x", "B C, b c", "D, d" };
    build(str1, str2, rulearray);
    checkDSEP(24, 15, 13, 15, false, 3);
    clearStats();
    checkDS(24, 60, 15, 3);
  }

  @Ignore
  public void testDS() {
    String str1 = "A B C D";
    String str2 = "a b c d";
    String[] rulearray = new String[] { "A, a", "B, x", "D, d" };
    build(str1, str2, rulearray);
    Validator.DP_A_MatrixwithEarlyPruning(s, t);
  }

  @Test
  public void testEqualStringTopDown1() {
    String str1 = "A B C D";
    String str2 = "a b c d";
    String[] rulearray = new String[] { "A, a", "B, x", "B C, b c", "D, d" };
    build(str1, str2, rulearray);
    int isEqual = Validator.DP_A_TopdownMatrix(s, t);
    assert (isEqual >= 0);
  }

  @Test
  public void testEqualStringTopDown2() {
    String str1 = "A B C D";
    String str2 = "a b c d";
    String[] rulearray = new String[] { "A, a", "B C, x y", "D, w", "b, x",
        "c d, y, w" };
    build(str1, str2, rulearray);
    int isEqual = Validator.DP_A_TopdownMatrix(s, t);
    assert (isEqual >= 0);
  }

  @Test
  public void testEqualStringTopDown3() {
    String str1 = "A B C D E F";
    String str2 = "a b c d e";
    String[] rulearray = new String[] { "A, ㄱ", "B C, ㄴ ㄷ", "D, ㄹ", "E F, ㅁ ㅂ",
        "a b, ㄱ ㄴ", "c d, ㄷ ㄹ ㅁ", "e, ㅂ" };
    build(str1, str2, rulearray);
    int isEqual = Validator.DP_A_TopdownMatrix(s, t);
    assert (isEqual >= 0);
  }

  @Test
  public void testNotEqualStringTopDown1() {
    String str1 = "A B C D";
    String str2 = "a b c d";
    String[] rulearray = new String[] { "B, x", "B C, b c", "D, d" };
    build(str1, str2, rulearray);
    int isEqual = Validator.DP_A_TopdownMatrix(s, t);
    assert (isEqual < 0);
  }

  @Test
  public void testNotEqualStringTopDown2() {
    String str1 = "A B C D";
    String str2 = "a b c d";
    String[] rulearray = new String[] { "A, a", "B C, u y", "D, w", "b, x",
        "c d, y, w" };
    build(str1, str2, rulearray);
    int isEqual = Validator.DP_A_TopdownMatrix(s, t);
    assert (isEqual < 0);
  }

  @Test
  public void testNotEqualStringTopDown3() {
    String str1 = "A B C D E F";
    String str2 = "a b c d e f";
    String[] rulearray = new String[] { "A, ㄱ", "B C, ㄴ ㄷ", "D, ㄹ", "E F, ㅁ ㅂ",
        "a b, ㄱ ㄴ", "c d, ㄷ ㄹ ㅁ", "e, ㅂ" };
    build(str1, str2, rulearray);
    int isEqual = Validator.DP_A_TopdownMatrix(s, t);
    assert (isEqual < 0);
  }

  /**
   * Check if the number of evaluations / early-evaluated are matched and
   * procedure is early-pruned
   * 
   * @param evaled
   * @param earlyed
   * @param ep
   */
  private void checkSSEP(long evaledentry, long earlyed, long evaledrules,
      boolean ep, int answer) {
    int result = Validator.DP_SingleSidewithEarlyPruning(s, t);
    assertEquals(evaledentry, Validator.niterentry);
    assertEquals(earlyed, Validator.earlyevaled);
    assertEquals(evaledrules, Validator.niterrules);
    assertEquals(ep, Validator.earlystopped == 1);
    assertEquals(answer, result);
  }

  private void checkSS(long evaledentry, long evaledrules, int answer) {
    int result = Validator.DP_SingleSide(s, t);
    assertEquals(evaledentry, Validator.niterentry);
    assertEquals(evaledrules, Validator.niterrules);
    assertEquals(answer, result);
  }

  private void checkDSEP(long evaledentry, long earlyed, long evaledrules,
      long evaledmatches, boolean ep, int answer) {
    int result = Validator.DP_A_MatrixwithEarlyPruning(s, t);
    assertEquals(evaledentry, Validator.niterentry);
    assertEquals(earlyed, Validator.earlyevaled);
    assertEquals(evaledrules, Validator.niterrules);
    assertEquals(evaledmatches, Validator.nitermatches);
    assertEquals(ep, Validator.earlystopped == 1);
    assertEquals(answer, result);
  }

  private void checkDS(long evaledentry, long evaledrules, long evaledmatches,
      int answer) {
    int result = Validator.DP_A_Matrix(s, t);
    assertEquals(evaledentry, Validator.niterentry);
    assertEquals(evaledrules, Validator.niterrules);
    assertEquals(evaledmatches, Validator.nitermatches);
    assertEquals(answer, result);
  }

  private void clearStats() {
    Validator.niterentry = 0;
    Validator.earlyevaled = 0;
    Validator.earlystopped = 0;
    Validator.niterrules = 0;
    Validator.nitermatches = 0;
  }

  /**
   * Build test case (generate records/strint-to-integer map and preprocess
   * records)
   */
  private void build(String str1, String str2, String[] rulearray) {
    str2int.clear();
    int2str.clear();
    add(str1);
    add(str2);
    for (String rule : rulearray)
      add(rule);

    // Transform to s,t, and rule_automata
    s = new Record(0, str1, str2int);
    t = new Record(1, str2, str2int);
    List<Rule> rulelist = new ArrayList<Rule>();
    for (String rule : rulearray)
      rulelist.add(new Rule(rule, str2int));
    for (Integer token : str2int.values())
      rulelist.add(new Rule(token, token));
    Rule_ACAutomata rac = new Rule_ACAutomata(rulelist);

    // Preprocess records
    preprocess(s, rac);
    preprocess(t, rac);
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
