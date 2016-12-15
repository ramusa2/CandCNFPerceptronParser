package illinoisParser.variables.structs;

import illinoisParser.variables.ConditioningVariables;

public class BinaryConditioningVariables extends ConditioningVariables {

	private final int v1, v2;
	private final int hash;

	public BinaryConditioningVariables(int a, int b) {
		v1 = a;
		v2 = b;
		hash = this.hash();
	}

	private final int hash() {
		int result = 17;
		result = result * 31 + v1;
		result = result * 31 + v2;
		return result;
	}

	@Override
	public String toString() {
		return "["+v1+", "+v2+"]";
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
		if (oth == null || !(oth instanceof BinaryConditioningVariables)) {
			return false;
		}
		BinaryConditioningVariables o = (BinaryConditioningVariables) oth;
		return o.v1 == this.v1 && o.v2 == this.v2;
	}

	@Override
	public int getVariableValue(int i) {
		if(i==0) {
			return v1;
		}
		else if(i==1) {
			return v2;
		}
		return -1;
	}

	@Override
	protected int length() {
		return 2;
	}

	@Override
	public String saveToString() {
		return v1+" "+v1;
	}
}
