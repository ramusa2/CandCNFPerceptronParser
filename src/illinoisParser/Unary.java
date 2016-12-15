package illinoisParser;

/**
 * Defines a Unary rule
 * 
 * @author bisk1
 */
public class Unary extends Rule {

  /**
   * Constructor used during serialization
   */
  public Unary() {}

  /**
   * Define a rule
   * 
   * @param a Parent
   * @param b Child
   * @param type Type
   */
  public Unary(Integer a, Integer b, Rule_Type type) {
    A = a;
    B = b;
    Type = type;
    N = 1;
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    return super.equals(o);
  }

  @Override
  public String toString(Grammar g, double P) {
    return toString(g) + "\t" + P;
  }

  @Override
  public String toString(Grammar g) {
		// TODO: get a good format for this string (too wide)
		return String.format("#UN#  %-12s %-15s -> %-15s",
				this.getType(), g.prettyCat(this.A), g.prettyCat(this.B));
		/*
    return String.format("#UNARY#   %-15s %-40s -> %-20s",
        this.getType(), g.prettyCat(A), g.prettyCat(B));
        */
  }

}
