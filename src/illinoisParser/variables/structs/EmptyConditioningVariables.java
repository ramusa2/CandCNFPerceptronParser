package illinoisParser.variables.structs;

import illinoisParser.variables.ConditioningVariables;

public class EmptyConditioningVariables extends ConditioningVariables {

	private static EmptyConditioningVariables  EMPTY;

	private EmptyConditioningVariables () {}

	@Override
	public String toString() {
		return "[]";
	}
	
	@Override
	public int hashCode() {
		return 0;
	}

	@Override
	public boolean equals(Object oth) {
		return oth instanceof EmptyConditioningVariables;
	}

	public final static EmptyConditioningVariables  getEmpty() {
		return EMPTY;
	}
	
	public final static void setEmpty(EmptyConditioningVariables empty) {
		EMPTY = empty;
	}

	@Override
	public int getVariableValue(int i) {
		return -1;
	}

	@Override
	protected int length() {
		return 0;
	}

	@Override
	public String saveToString() {
		return "[]";
	}
}
