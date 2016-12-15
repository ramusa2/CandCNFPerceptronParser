package illinoisParser.variables.structs;

import illinoisParser.variables.ConditioningVariables;

/**
 * Class for storing a single integer conditioning variable 
 * (e.g. for conditional probability distributions, features, etc.)
 *  
 * @author ramusa2
 *
 */
public class UnaryConditioningVariables extends ConditioningVariables {

	private final int v1;
	private final int hash;

	public UnaryConditioningVariables(int a) {
		v1 = a;
		hash = this.hash();
	}

	private final int hash() {
		int result = 17;
		result = result * 31 + v1;
		return result;
	}

	@Override
	public String toString() {
		return "["+v1+"]";
	}

	@Override
	public final int hashCode() {
		return hash;
	}

	@Override
	public final boolean equals(Object oth) {
		if(this == oth) {
			return true;
		}
		if (oth == null || !(oth instanceof UnaryConditioningVariables)) {
			return false;
		}
		UnaryConditioningVariables o = (UnaryConditioningVariables) oth;
		return o.v1 == this.v1;
	}

	@Override
	public int getVariableValue(int i) {
		if(i==0) {
			return v1;
		}
		return -1;
	}

	@Override
	protected int length() {
		return 1;
	}

	@Override
	public String saveToString() {
		return v1+"";
	}
}
