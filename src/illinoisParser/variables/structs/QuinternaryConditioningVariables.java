package illinoisParser.variables.structs;

import illinoisParser.variables.ConditioningVariables;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.concurrent.ConcurrentHashMap;

public class QuinternaryConditioningVariables extends ConditioningVariables {

	private final int v1, v2, v3, v4, v5;
	private final int hash;

	public QuinternaryConditioningVariables(int a, int b, int c, int d, int e) {
		v1 = a;
		v2 = b;
		v3 = c;
		v4 = d;
		v5 = e;
		hash = this.hash();
	}

	private final int hash() {
		int result = 17;
		result = result * 31 + v1;
		result = result * 31 + v2;
		result = result * 31 + v3;
		result = result * 31 + v4;
		result = result * 31 + v5;
		return result;
	}

	@Override
	public String toString() {
		return "["+v1+", "+v2+", "+v3+", "+v4+", "+v5+"]";
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
		if (oth == null || !(oth instanceof QuinternaryConditioningVariables)) {
			return false;
		}
		QuinternaryConditioningVariables o = (QuinternaryConditioningVariables) oth;
		return o.v1 == this.v1 && o.v2 == this.v2 && o.v3 == this.v3 && o.v4 == this.v4 && o.v5 == this.v5;
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
		else if(i==4) {
			return v5;
		}
		return -1;
	}

	@Override
	protected int length() {
		return 5;
	}

	@Override
	public String saveToString() {
		return v1+" "+v2+" "+v3+" "+v4+" "+v5;
	}
}
