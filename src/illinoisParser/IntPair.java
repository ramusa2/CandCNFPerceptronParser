package illinoisParser;

import java.io.Serializable;

/**
 * An immutable data-structure for pairs of integers
 * 
 * @author bisk1
 */
public class IntPair implements Serializable, Comparable<Object> {
  private static final long serialVersionUID = 5162012L;
  private final int A, B;
  private final int hash;

  /**
   * Constructor for 2 ints ( e.g. binary production )
   * 
   * @param a
   * @param b
   */
  IntPair(int a, int b) {
    A = a;
    B = b;
    hash = 7919 * a + b;
  }

  /**
   * Constructor for 1 int ( e..g unary production )
   * 
   * @param a
   */
  public IntPair(int a) {
    A = a;
    B = -1;
    hash = 7919 * a;
  }

  @Override
  public int hashCode() {
    return hash;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    IntPair obj = (IntPair) o;
    return obj.A == A && obj.B == B;
  }

  @Override
  public String toString() {
    return A + " " + B;
  }

  /**
   * Only uses the first item for comparison
   */
  public int compareTo(Object o) {
    IntPair other = (IntPair) o;
    return (int) (other.A - A);
  }

  /**
   * Return the first item
   * @return first item
   */
  int first() {return A;}
  /**
   * Return the second item
   * @return second item
   */
  int second() {return B;}
}
