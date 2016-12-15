package illinoisParser;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * A CYK backpointer
 * 
 * @author bisk1
 * @param <G>
 */
public class BackPointer implements java.io.Externalizable {

	// IF YOU EXTEND THIS CLASS YOU MUST CHANGE THE FUNCTION IN MODEL:
	// BackPointer newBackPointer(Rule r, ChartItem ... Children){

	/**
	 * Rule used
	 */
	public Rule r;
	/**
	 * Rule Type: Left/Right/Unary/Lex/Top
	 */
	protected int type;
	/**
	 * Left Child
	 */
	protected CoarseChartItem B;
	/**
	 * Right Child
	 */
	protected CoarseChartItem C;
	private int hashcode;

	/**
	 * Default constructor for Externalizing
	 */
	public BackPointer() {}

	/**
	 * Create backpointer based on children and rule used for combining
	 * 
	 * @param rule
	 *          Rule
	 * @param b
	 *          Left child
	 * @param c
	 *          Right child
	 */
	BackPointer(Binary rule, CoarseChartItem b, CoarseChartItem c) {
		this.r = rule;
		this.B = b;
		this.C = c;
		this.hashcode = r.hashCode() + b.hashCode() + c.hashCode();
	}

	/**
	 * Create Unary backpointer based on rule and child chart-item
	 * 
	 * @param rule
	 * @param b
	 */
	BackPointer(Unary rule, CoarseChartItem b) {
		this.C = null;
		this.r = rule;
		this.B = b;
		this.hashcode = r.hashCode() + b.hashCode();
	}

	@Override
	public String toString() {
		if (this.C != null) {
			return this.r + "|" + this.B + "|" + this.C;
		}
		return this.r + "|" + this.B;
	}

	@Override
	public int hashCode() {
		return this.hashcode;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		@SuppressWarnings("unchecked")
		BackPointer o = (BackPointer) obj;
		return this.r.equals(o.r) && this.type == o.type && this.B.equals(o.B)
				&& ((this.C == o.C) || (this.C.equals(o.C)));
	}

	/**
	 * Backpointer for unary rule
	 * @return if Unary rule was used
	 */
	public final boolean isUnary() {
		return this.C == null;
	}

	/**
	 * Left child getter
	 * @return Left Child
	 */
	public CoarseChartItem B() {
		return this.B;
	}

	/**
	 * Right child getter
	 * @return Right child
	 */
	public CoarseChartItem C() {
		return this.C;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void readExternal(ObjectInput oi) throws IOException,
	ClassNotFoundException {
		this.r = (Rule) oi.readObject();
		this.type = oi.readInt();
		this.B = (CoarseChartItem) oi.readObject();
		this.C = (CoarseChartItem) oi.readObject();
		this.hashcode = oi.readInt();
	}

	@Override
	public void writeExternal(ObjectOutput oo) throws IOException {
		oo.writeObject(this.r);
		oo.writeInt(this.type);
		oo.writeObject(this.B);
		oo.writeObject(this.C);
		oo.writeInt(this.hashcode);
	}

	public Rule_Direction direction() {
		if(r instanceof Binary) {
			return ((Binary) r).head;
		}
		return Rule_Direction.None;
	}
}
