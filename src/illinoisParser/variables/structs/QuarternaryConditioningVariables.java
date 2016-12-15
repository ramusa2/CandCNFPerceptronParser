package illinoisParser.variables.structs;

import illinoisParser.variables.ConditioningVariables;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.concurrent.ConcurrentHashMap;

public class QuarternaryConditioningVariables extends ConditioningVariables {

	private final int v1, v2, v3, v4;
	private final int hash;

	public QuarternaryConditioningVariables(int a, int b, int c, int d) {
		v1 = a;
		v2 = b;
		v3 = c;
		v4 = d;
		hash = this.hash();
	}

	private final int hash() {
		int result = 17;
		result = result * 31 + v1;
		result = result * 31 + v2;
		result = result * 31 + v3;
		result = result * 31 + v4;
		return result;
	}

	@Override
	public String toString() {
		return "["+v1+", "+v2+", "+v3+", "+v4+"]";
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
		if (oth == null || !(oth instanceof QuarternaryConditioningVariables)) {
			return false;
		}
		QuarternaryConditioningVariables o = (QuarternaryConditioningVariables) oth;
		return o.v1 == this.v1 && o.v2 == this.v2 && o.v3 == this.v3 && o.v4 == this.v4;
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
		else if(i==3) {
			return v4;
		}
		return -1;
	}

	@Override
	protected int length() {
		return 4;
	}

	@Override
	public String saveToString() {
		return v1+" "+v2+" "+v3+" "+v4;
	}
}
