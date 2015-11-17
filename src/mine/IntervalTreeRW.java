package mine;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * Implement with binary search tree <br/>
 * Red-Black tree is implemented
 */
public class IntervalTreeRW<K extends Comparable<K>, V> {
  private final ITNode nil  = new ITNode();
  private ITNode       root = nil;
  private int          size = 0;

  /**
   * Insert a node with given low, high key and given value
   * 
   * @param low
   *          The lower key
   * @param high
   *          The higher key
   * @param value
   *          The value
   */
  public void insert(K low, K high, V value) {
    ITNode node = new ITNode(low, high, value);
    if (root == nil)
      root = node;
    else {
      root.insert(node);

      // Fix the tree
      ITNode z = node;
      while (z.parent.color == Color.RED) {
        if (z.parent == z.parent.parent.left) {
          ITNode y = z.parent.parent.right;
          if (y.color == Color.RED) {
            z.parent.color = Color.BLACK;
            y.color = Color.BLACK;
            z.parent.parent.color = Color.RED;
            z = z.parent.parent;
          } else {
            if (z == z.parent.right) {
              z = z.parent;
              z.leftRotate();
            }
            z.parent.color = Color.BLACK;
            z.parent.parent.color = Color.RED;
            z.parent.parent.rightRotate();
          }
        } else {
          ITNode y = z.parent.parent.left;
          if (y.color == Color.RED) {
            z.parent.color = Color.BLACK;
            y.color = Color.BLACK;
            z.parent.parent.color = Color.RED;
            z = z.parent.parent;
          } else {
            if (z == z.parent.left) {
              z = z.parent;
              z.rightRotate();
            }
            z.parent.color = Color.BLACK;
            z.parent.parent.color = Color.RED;
            z.parent.parent.leftRotate();
          }
        }
      }
    }
    root.color = Color.BLACK;
    ++size;
  }

  /**
   * Find all nodes which overlaps with a key value
   * 
   * @param key
   *          The key
   */
  public ArrayList<V> search(K key) {
    return search(key, key);
  }

  /**
   * Find all nodes which overlaps with a given interval
   * 
   * @param min
   *          The minimum key
   * @param max
   *          The maximum key
   */
  public ArrayList<V> search(K min, K max) {
    ArrayList<V> rslt = new ArrayList<V>();
    if (root != nil) root.find(rslt, min, max);
    return rslt;
  }

  public int size() {
    return size;
  }

  public boolean validate() {
    int count = 0;
    Queue<ITNode> queue = new LinkedList<ITNode>();
    queue.add(root);
    while (!queue.isEmpty()) {
      ITNode node = queue.poll();
      if (node == nil) continue;
      ++count;
      queue.add(node.left);
      queue.add(node.right);
    }
    return count == size;
  }

  public ITIterator iterator() {
    return new ITIterator();
  }

  private enum Color {
    RED, BLACK
  };

  private class ITNode {
    ITNode parent = nil;
    ITNode left   = nil;
    ITNode right  = nil;
    K      low;
    K      high;
    K      max;
    V      value;
    Color  color  = Color.RED;

    ITNode() {
      color = Color.BLACK;
    }

    ITNode(K low, K high, V value) {
      this.low = low;
      max = this.high = high;
      this.value = value;
    }

    void insert(ITNode node) {
      // Update max
      if (max.compareTo(node.max) < 0) max = node.max;

      if (low.compareTo(node.low) <= 0) {
        if (right == nil) {
          right = node;
          node.parent = this;
        } else
          right.insert(node);
      } else {
        if (left == nil) {
          left = node;
          node.parent = this;
        } else
          left.insert(node);
      }
    }

    void leftRotate() {
      ITNode y = right;
      this.right = y.left;
      if (y.left != nil) y.left.parent = this;
      y.parent = parent;
      if (parent == nil)
        root = y;
      else if (this == parent.left)
        parent.left = y;
      else
        parent.right = y;
      y.left = this;
      parent = y;

      // Fix max
      y.max = max;
      max = high;
      if (left != nil && max.compareTo(left.max) < 0) max = left.max;
      if (right != nil && max.compareTo(right.max) < 0) max = right.max;
    }

    void rightRotate() {
      ITNode x = left;
      left = x.right;
      if (x.right != nil) x.right.parent = this;
      x.parent = parent;
      if (parent == nil)
        root = x;
      else if (this == parent.right)
        parent.right = x;
      else
        parent.left = x;
      x.right = this;
      parent = x;

      // Fix max
      x.max = max;
      max = high;
      if (left != nil && max.compareTo(left.max) < 0) max = left.max;
      if (right != nil && max.compareTo(right.max) < 0) max = right.max;
    }

    void find(ArrayList<V> rslt, K min, K max) {
      // If the given interval overlaps with current node, return this
      // node
      if (min.compareTo(high) <= 0 && low.compareTo(max) <= 0) rslt.add(value);
      // If left child exists and may overlaps with the given
      // interval, recur for left child.
      if (left != nil && min.compareTo(left.max) <= 0)
        left.find(rslt, min, max);
      // If right child exists and may overlaps with the given
      // interval, recur for the child.
      if (right != nil && low.compareTo(max) <= 0) right.find(rslt, min, max);
    }

    public String toString() {
      if (value == null) return "Nil";
      return color.toString();
    }
  }

  public class ITIterator implements Iterator<V> {
    private ITNode next;

    ITIterator() {
      next = root;
      if (next == nil) return;
      while (next.left != nil)
        next = next.left;
    }

    @Override
    public boolean hasNext() {
      return next != nil;
    }

    @Override
    public V next() {
      if (!hasNext()) throw new NoSuchElementException();
      V r = next.value;
      if (next.right != nil) {
        next = next.right;
        while (next.left != nil)
          next = next.left;
        return r;
      } else
        while (true) {
          if (next.parent == nil) {
            next = nil;
            return r;
          } else if (next.parent.left == next) {
            next = next.parent;
            return r;
          }
          next = next.parent;
        }
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }
}
