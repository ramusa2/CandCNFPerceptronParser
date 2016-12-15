package illinoisParser;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Defines a Binary rule
 * 
 * @author bisk1
 */
public class Binary extends Rule {

	/**
	 * Rule Arity ( 0 for B<sup>0</sup>, 1 for B<sup>1</sup>, ... )
	 */
	public int arity;
	/**
	 * Right child category of the parent
	 */
	public Integer C;
	/**
	 * Specifies if left or right child is the functor or modifier
	 */
	public Rule_Direction head;

	/**
	 * Instantiate a Binary rule. Constructor only used when externalizing
	 */
	public Binary() {}

	/**
	 * Create a Binary rule.
	 * 
	 * @param a Parent
	 * @param b Left child
	 * @param c Right child
	 * @param t Rule Type
	 * @param ar Rule Arity
	 * @param h Head direction
	 */
	public Binary(Integer a, Integer b, Integer c, Rule_Type t, int ar, Rule_Direction h) {
		this.A = a;
		this.B = b;
		this.C = c;
		this.Type = t;
		this.arity = ar;
		this.head = h;
		this.N = 2;
	}

	@Override
	public int hashCode() {
		if(hashCode == 0) {
			hashCode = super.hashCode();
			hashCode = hashCode *31 + C;
		}
		return hashCode;
	}

	@Override
	public boolean equals(Object o) {
		boolean s = super.equals(o);
		if (s) {
			Binary b = (Binary) o;
			return this.arity == b.arity && this.C.equals(b.C) && this.head == b.head;
		}
		return false;
	}

	@Override
	public String toString(Grammar g, double P) {
		return toString(g) + "\t" + P;
	}

	@Override
	public String toString(Grammar g) {
		// TODO: get a good format for this string (too wide)
		return String.format("#BIN# %-12s %5d %-15s -> %-15s %-15s",
				this.getType(), this.arity, g.prettyCat(this.A), g.prettyCat(this.B),
				g.prettyCat(this.C));
		
		/*
		return String.format("#BINARY#  %-15s %-40s -> %-40s %-40s %-5d %-5s",
				this.getType(), g.prettyCat(this.A), g.prettyCat(this.B),
				g.prettyCat(this.C), this.arity, this.head.toString());
				*/
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
	ClassNotFoundException {
		super.readExternal(in);
		this.C = in.readInt();
		this.arity = in.readInt();
		this.head = (Rule_Direction) in.readObject();

	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeInt(this.C);
		out.writeInt(this.arity);
		out.writeObject(this.head);
	}

}
