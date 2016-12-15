package illinoisParser;

/**
 * @author bisk1
 * Class for comparing objects which have a score/value which nees to be sorted
 * @param <T>   Type of the object/contents
 */
class ObjectDoublePair<T extends Object> implements Comparable<Object> {
  private final T contents;
  private final double value;
  
  /**
   * Create an immutable object which sorts by the double's value
   * @param object
   * @param val
   */
  ObjectDoublePair(T object, double val) {
    contents = object;
    value = val;
  }
  
  /**
   * Returns the sorted content
   * @return  object
   */
  T content() {
    return contents;
  }
  
  /**
   * Value of the object
   * @return value
   */
  double value() {
    return value;
  }
  
  @SuppressWarnings("unchecked")
  public int compareTo(Object arg0) {
    return (int) Math.signum(((ObjectDoublePair<T>) arg0).value - value);
  }
}
