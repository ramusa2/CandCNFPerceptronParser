package illinoisParser;

import java.io.Serializable;

/**
 * A tuple for tieing ccg categories with their history used to produce them
 * 
 * @author bisk1
 */
class CategoryTuple implements Serializable {

  /**
   * CCG Category
   */
  final Integer cat;
  /**
   * Rule Type used to produce it
   */
  final Rule_Type type;
  private static final int P = 96589, P2 = 96731;
  private final int hashcode;

  /**
   * Create tuple with cat and type
   * 
   * @param symb
   * @param t
   */
  CategoryTuple(Integer c, Rule_Type t) {
    this.cat = c;
    this.type = t;
    this.hashcode = Long.valueOf(this.cat * P + this.type.ordinal() * P2).hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    CategoryTuple o = (CategoryTuple) obj;
    return o.cat == this.cat && o.type == this.type;
  }

  @Override
  public String toString() {
    return this.cat + "|" + this.type;
  }

  @Override
  public int hashCode() {
    return this.hashcode;
  }

}
