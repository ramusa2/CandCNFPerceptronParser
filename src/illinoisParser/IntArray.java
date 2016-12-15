package illinoisParser;

import java.io.Serializable;
import java.util.Arrays;

/**
 * An integer array
 * 
 * @author bisk1
 */
public strictfp class IntArray implements Serializable {
  private static final long serialVersionUID = 5142012L;

  transient private int hashcode = 0;
  /**
   * Underlying integer array
   */
  long[] ia;// = new int[]{};
  private int ptr = 0;
  private static final int INCREMENT = 2;

  /**
   * Empty array
   */
  public IntArray() {}

  /**
   * Create array from input, with the option to pre-compute the hash function
   * 
   * @param is
   * @param makeHash
   */
  public IntArray(long[] is, boolean makeHash) {
    ia = is; // Arrays.copyOf(is, is.length);
    ptr = ia.length;
    if (makeHash) {
      hashcode = Arrays.hashCode(ia);
    }
  }

  /**
   * Create empty array of length s
   * 
   * @param s
   */
  public IntArray(int s) {
    ia = new long[s];
    Arrays.fill(ia, 0);
    ptr = s;
  }

  private void set(int i, long v) throws Exception {
    if (i >= ia.length) {
      int cur = ia.length;
      if (ia != null) {
        ia = Arrays.copyOf(ia, i + INCREMENT);
      } else {
        ia = new long[i + INCREMENT];
      }
      Arrays.fill(ia, cur, ia.length, 0);
    }
    ia[i] = v;
    ptr = i + 1;
  }

  /**
   * Adding new item. Does not necessarily require array resizing
   * 
   * @param v
   * @throws Exception
   */
  void append(int v) throws Exception {
    set(ptr, v);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    IntArray obj = (IntArray) o;
    return Arrays.equals(ia, obj.ia);
  }

  /**
   * Number of elements in the array, not the absolute underlying size
   * 
   * @return int
   */
  public final int size() {
    return ptr;
  }

  @Override
  public int hashCode() {
    if (hashcode != 0 || ia.length == 0) {
      return hashcode;
    }
    hashcode = Arrays.hashCode(ia);
    return hashcode;
  }

  @Override
  public String toString() {
    return Arrays.toString(Arrays.copyOf(ia, ptr));
  }

  /**
   * Get index i
   * 
   * @param i
   *          index
   * @return int
   */
  public long get(int i) {
    return ia[i];
  }

  /**
   * Adds v to the array unless it's already in the array
   * 
   * @param v
   * @throws Exception
   */
  public final void union(int v) throws Exception {
    if (ia == null) {
      ia = new long[INCREMENT];
      set(0, v);
      return;
    }
    for (long val : array()) {
      if (val == v) {
        return;
      }
    }
    set(ptr, v);
  }

  /**
   * Return underlying array
   * 
   * @return int[]
   */
  final long[] array() {
    return Arrays.copyOf(ia, ptr);
  }

  /**
   * Check if array is empty
   * 
   * @return boolean
   */
  public boolean isEmpty() {
    return ia.length == 0;
  }

  /**
   * Check if array contains j
   * 
   * @param j
   * @return boolean
   */
  public final boolean contains(int j) {
    for (int i = 0; i < ptr; i++) {
      if (ia[i] == j) {
        return true;
      }
    }
    return false;
  }

  /**
   * Updates hashcode
   */
  final void makeHash() {
    hashCode();
  }

  /**
   * Increments index or creates index if not present
   * 
   * @param index
   * @throws Exception
   */
  public final void addIncrement(int index) throws Exception {
    if (index >= ia.length) {
      set(index, 1);
    } else {
      set(index, get(index) + 1);
    }
  }

  /**
   * Returns subset of array
   * 
   * @param backoffCutoff
   * @return IntArray
   */
  public IntArray getBackoff(int backoffCutoff) {
    return new IntArray(Arrays.copyOf(ia, backoffCutoff), true);
  }

  /**
   * Check if all indices have a positive value
   * 
   * @return boolean
   */
  public boolean isFull() {
    for (long i : ia) {
      if (i == -1) {
        return false;
      }
    }
    return true;
  }
}
