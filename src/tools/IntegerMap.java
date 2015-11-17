package tools;

import java.lang.reflect.Array;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Specialized implementation of HashSet&lt;Integer, T&gt;.
 */
public class IntegerMap<T> implements Map<Integer, T> {
  private Entry[] array;
  private int     size;
  private double  factor;
  private int     nextExpandSize;

  public static void main(String[] args) {
    ArrayList<Integer> keylist = new ArrayList<Integer>();
    HashMap<Integer, Integer> refmap = new HashMap<Integer, Integer>();
    IntegerMap<Integer> map = new IntegerMap<Integer>();
    int testsize = 100000;
    Random rand = new Random();

    for (int i = 0; i < testsize; ++i) {
      int key = rand.nextInt();
      int value = rand.nextInt();
      keylist.add(key);
      refmap.put(key, value);
      map.putI(key, value);
    }

    System.out.println(map.size == refmap.size());

    while (true) {
      int idx = rand.nextInt(keylist.size());
      int key = keylist.get(idx);
      Integer refval = refmap.get(key);
      Integer val = map.get(key);
      if (Integer.compare(refval, val) != 0) System.out.println(key);
    }
  }

  public IntegerMap() {
    factor = 0.5;
    clear();
  }

  @SuppressWarnings("unchecked")
  public IntegerMap(Map<? extends Integer, ? extends T> c) {
    this();
    int size = Math.max(10, (int) Math.ceil(c.size() / factor));
    array = (Entry[]) Array.newInstance(Entry.class, size);
    for (Map.Entry<? extends Integer, ? extends T> e : c.entrySet())
      put(e.getKey(), e.getValue());
  }

  public boolean containsKeyI(int key) {
    Entry curr = array[getIdx(key)];
    while (curr != null) {
      if (curr.key == key) return true;
      curr = curr.next;
    }
    return false;
  }

  public T getI(int key) {
    Entry curr = array[getIdx(key)];
    while (curr != null) {
      if (curr.key == key) return curr.value;
      curr = curr.next;
    }
    return null;
  }

  public T putI(int key, T value) {
    T removedValue = remove(key);

    int idx = getIdx(key);
    Entry curr = new Entry(key, value);
    Entry next = array[idx];
    curr.next = next;
    array[idx] = curr;

    // Expand array
    if (++size >= nextExpandSize) resize((int) (array.length * 1.7));

    return removedValue;
  }

  public T removeI(int key) {
    int idx = getIdx(key);
    Entry prev = null;
    Entry curr = array[idx];
    while (curr != null) {
      if (curr.key == key) {
        // Current entry is the first element
        if (prev == null)
          array[idx] = curr.next;
        else
          prev.next = curr.next;
        --size;
        return curr.value;
      }
      prev = curr;
      curr = curr.next;
    }
    return null;
  }

  @Override
  public int size() {
    return size;
  }

  /**
   * Expand the array with the given size
   * 
   * @param nextSize
   */
  @SuppressWarnings("unchecked")
  private void resize(int nextSize) {
    nextSize = Math.max(10, nextSize);
    Entry[] temp = array;
    array = (Entry[]) Array.newInstance(Entry.class, nextSize);
    nextExpandSize = (int) (array.length * factor);

    // Move entries in temp to new array
    for (int curridx = 0; curridx < temp.length; ++curridx) {
      Entry curr;
      while ((curr = temp[curridx]) != null) {
        temp[curridx] = curr.next;

        int movedIdx = getIdx(curr.key);
        curr.next = array[movedIdx];
        array[movedIdx] = curr;
      }
    }
  }

  private int getIdx(int e) {
    e = Math.abs(e);
    if (array.length == 0) System.out.println("???");
    return e % array.length;
  }

  @Override
  public String toString() {
    EntryIterator it = new EntryIterator();
    boolean first = true;
    StringBuilder sb = new StringBuilder();
    sb.append('[');
    while (it.hasNext()) {
      if (!first) sb.append(", ");
      first = false;
      sb.append(it.next());
    }
    sb.append(']');
    return sb.toString();
  }

  private final class Entry implements Map.Entry<Integer, T> {
    private int   key;
    private T     value;
    private Entry next;

    Entry(int record, T value) {
      this.key = record;
      this.value = value;
    }

    public String toString() {
      return Integer.toString(key) + ":" + value.toString();
    }

    @Override
    public Integer getKey() {
      return key;
    }

    @Override
    public T getValue() {
      return value;
    }

    @Override
    public T setValue(T value) {
      T prevValue = this.value;
      this.value = value;
      return prevValue;
    }
  }

  private final class KeyIterator implements Iterator<Integer> {
    private EntryIterator it;

    KeyIterator() {
      it = new EntryIterator();
    }

    @Override
    public boolean hasNext() {
      return it.hasNext();
    }

    @Override
    public Integer next() {
      return it.next().getKey();
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  private final class KeySet extends AbstractSet<Integer> {
    @Override
    public Iterator<Integer> iterator() {
      return new KeyIterator();
    }

    @Override
    public int size() {
      return size;
    }
  }

  private final class ValueIterator implements Iterator<T> {
    private EntryIterator it;

    ValueIterator() {
      it = new EntryIterator();
    }

    @Override
    public boolean hasNext() {
      return it.hasNext();
    }

    @Override
    public T next() {
      return it.next().getValue();
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  private final class ValueCollection extends AbstractCollection<T> {
    @Override
    public Iterator<T> iterator() {
      return new ValueIterator();
    }

    @Override
    public int size() {
      return size;
    }
  }

  private final class EntryIterator implements Iterator<Map.Entry<Integer, T>> {
    int   curridx;
    Entry curr;

    EntryIterator() {
      curridx = 0;
      while (curridx < array.length && (curr = array[curridx]) == null)
        ++curridx;
    }

    @Override
    public boolean hasNext() {
      return (curr != null);
    }

    @Override
    public Entry next() {
      Entry nextEntry = curr;
      curr = curr.next;
      if (curr == null) {
        ++curridx;
        while (curridx < array.length && (curr = array[curridx]) == null)
          ++curridx;
      }
      return nextEntry;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  private final class EntrySet extends AbstractSet<Map.Entry<Integer, T>> {
    @Override
    public Iterator<Map.Entry<Integer, T>> iterator() {
      return new EntryIterator();
    }

    @Override
    public int size() {
      return size;
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void clear() {
    array = (Entry[]) Array.newInstance(Entry.class, 10);
    size = 0;
    nextExpandSize = (int) (array.length * factor);
  }

  @Override
  public boolean containsKey(Object o) {
    Integer key = (Integer) o;
    return containsKeyI(key.intValue());
  }

  @Override
  public boolean containsValue(Object arg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<Map.Entry<Integer, T>> entrySet() {
    return new EntrySet();
  }

  @Override
  public T get(Object o) {
    Integer key = (Integer) o;
    return getI(key.intValue());
  }

  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  @Override
  public Set<Integer> keySet() {
    return new KeySet();
  }

  @Override
  public T put(Integer key, T value) {
    return putI(key.intValue(), value);
  }

  @Override
  public void putAll(Map<? extends Integer, ? extends T> m) {
    for (Map.Entry<? extends Integer, ? extends T> e : m.entrySet())
      putI(e.getKey().intValue(), e.getValue());
  }

  @Override
  public T remove(Object o) {
    Integer key = (Integer) o;
    return removeI(key.intValue());
  }

  @Override
  public Collection<T> values() {
    return new ValueCollection();
  }
}
