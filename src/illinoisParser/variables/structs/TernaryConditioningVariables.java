package illinoisParser.variables.structs;

import illinoisParser.variables.ConditioningVariables;

public class TernaryConditioningVariables extends ConditioningVariables {

	private final int v1, v2, v3;
	private final int hash;

	public TernaryConditioningVariables(int a, int b, int c) {
		v1 = a;
		v2 = b;
		v3 = c;
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
		return "["+v1+", "+v2+", "+v3+"]";
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
		if (oth == null || !(oth instanceof TernaryConditioningVariables)) {
			return false;
		}
		TernaryConditioningVariables o = (TernaryConditioningVariables) oth;
		return o.v1 == this.v1 && o.v2 == this.v2 && o.v3 == this.v3;
	}

	@Override
	public int getVariableValue(int i) {
		if(i==0) {
			return v1;
		}
		else if(i==1) {
			return v2;
		}
		else if(i==2) {
			return v3;
		}
		return -1;
	}

	@Override
	protected int length() {
		return 3;
	}

	@Override
	public String saveToString() {
		return v1+" "+v2+" "+v3;
	}
}
