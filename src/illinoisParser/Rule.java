package illinoisParser;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Specifies a Rule object
 * 
 * @author bisk1
 */
public abstract class Rule implements Externalizable, Comparable<Rule> {

	/**
	 * Parent
	 */
	public Integer A;
	/**
	 * Type
	 */
	protected Rule_Type Type;
	/**
	 * Child
	 */
	public Integer B;

	/**
	 * Number of children
	 */
	protected int N;

	protected int hashCode = 0;

	/**
	 * Pretty print for a rule with its probability
	 * 
	 * @param g
	 *          Grammar
	 * @param P
	 *          Probability
	 * @return String
	 */
	public abstract String toString(Grammar g, double P);

	/**
	 * Pretty print for a rule
	 * 
	 * @param g
	 *          Grammar
	 * @return String
	 */
	public abstract String toString(Grammar g);

	@Override
	public int hashCode() {
		if(hashCode == 0) {
			hashCode = 17;
			hashCode = hashCode*31 + A;
			hashCode = hashCode*31 + B;
			//hashCode = hashCode *31 + Type.hashCode();
		}
		return hashCode;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		Rule r = (Rule) o;
		return A.equals(r.A) && getType().equals(r.getType()) && B.equals(r.B) && N == r.N;
	}

	public void readExternal(ObjectInput in) throws IOException,
	ClassNotFoundException {
		A = in.readInt();
		B = in.readInt();
		Type = (Rule_Type) in.readObject();
		N = in.readInt();
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(A);
		out.writeInt(B);
		out.writeObject(getType());
		out.writeInt(N);
	}

	public Rule_Type getType() {
		return Type;
	}

	@Override
	public int compareTo(Rule o) {
		return o.A - this.A;
	}
}
