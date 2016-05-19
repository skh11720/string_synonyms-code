package tools;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

public class StaticFunctionsTest {
  private static Comparator<Integer> intcmp;

  @BeforeClass
  public static void beforeClass() {
    intcmp = new Comparator<Integer>() {
      public int compare(Integer o1, Integer o2) {
        return o1.compareTo(o2);
      }
    };
  }

  public static <T> List<T> interswitch(List<List<T>> lists,
      Comparator<T> cmp) {
    return StaticFunctions.intersection(lists, cmp);
  }

  @Test
  public void intersection2Test1() {
    Integer[][] arr = new Integer[][] { { 1, 2, 3 }, { 2, 6 } };
    List<List<Integer>> lists = transform(arr);
    List<Integer> inter = interswitch(lists, intcmp);
    assertEquals(1, inter.size());
    assertEquals(2, inter.get(0).intValue());
  }

  @Test
  public void intersection2Test2() {
    Integer[][] arr = new Integer[][] { { 1, 2, 3 }, { 2, 3, 6 } };
    List<List<Integer>> lists = transform(arr);
    List<Integer> inter = interswitch(lists, intcmp);
    assertEquals(2, inter.size());
    assertEquals(2, inter.get(0).intValue());
    assertEquals(3, inter.get(1).intValue());
  }

  @Test
  public void intersection2Test3() {
    Integer[][] arr = new Integer[][] { { 1, 2, 3 }, { 4 } };
    List<List<Integer>> lists = transform(arr);
    List<Integer> inter = interswitch(lists, intcmp);
    assertEquals(0, inter.size());
  }

  @Test
  public void intersection2Test4() {
    Integer[][] arr = new Integer[][] { { 1, 2, 3, 4 }, { 1 } };
    List<List<Integer>> lists = transform(arr);
    List<Integer> inter = interswitch(lists, intcmp);
    assertEquals(1, inter.size());
    assertEquals(1, inter.get(0).intValue());
  }

  @Test
  public void intersection2Test5() {
    Integer[][] arr = new Integer[][] { { 1, 2 }, { 1, 2 } };
    List<List<Integer>> lists = transform(arr);
    List<Integer> inter = interswitch(lists, intcmp);
    assertEquals(2, inter.size());
    assertEquals(1, inter.get(0).intValue());
    assertEquals(2, inter.get(1).intValue());
  }

  @Test
  public void intersection3Test1() {
    Integer[][] arr = new Integer[][] { { 1, 2, 3 }, { 2, 6 }, { 2, 5 } };
    List<List<Integer>> lists = transform(arr);
    List<Integer> inter = interswitch(lists, intcmp);
    assertEquals(1, inter.size());
    assertEquals(2, inter.get(0).intValue());
  }

  @Test
  public void intersection3Test2() {
    Integer[][] arr = new Integer[][] { { 1, 2, 3 }, { 2, 3, 6 }, { 3, 7 } };
    List<List<Integer>> lists = transform(arr);
    List<Integer> inter = interswitch(lists, intcmp);
    assertEquals(1, inter.size());
    assertEquals(3, inter.get(0).intValue());
  }

  @Test
  public void intersection3Test3() {
    Integer[][] arr = new Integer[][] { { 1, 2, 3 }, { 4 }, { 1 } };
    List<List<Integer>> lists = transform(arr);
    List<Integer> inter = interswitch(lists, intcmp);
    assertEquals(0, inter.size());
  }

  @Test
  public void intersection3Test4() {
    Integer[][] arr = new Integer[][] { { 1, 2, 3, 4 }, { 1 }, { 1, 2 } };
    List<List<Integer>> lists = transform(arr);
    List<Integer> inter = interswitch(lists, intcmp);
    assertEquals(1, inter.size());
    assertEquals(1, inter.get(0).intValue());
  }

  @Test
  public void intersection3Test5() {
    Integer[][] arr = new Integer[][] { { 1 }, { 1 }, { 1 } };
    List<List<Integer>> lists = transform(arr);
    List<Integer> inter = interswitch(lists, intcmp);
    assertEquals(1, inter.size());
    assertEquals(1, inter.get(0).intValue());
  }

  private static <T> List<List<T>> transform(T[][] arr) {
    List<List<T>> lists = new ArrayList<List<T>>();
    for (int i = 0; i < arr.length; ++i) {
      List<T> list = new ArrayList<T>();
      for (T item : arr[i])
        list.add(item);
      lists.add(list);
    }
    return lists;
  }
}
