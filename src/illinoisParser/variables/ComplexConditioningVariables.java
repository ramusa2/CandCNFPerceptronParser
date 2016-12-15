package illinoisParser.variables;

public class ComplexConditioningVariables extends ConditioningVariables {
	private final ConditioningVariables backoff, remainder;
	private final int hash;

	public ComplexConditioningVariables(ConditioningVariables backoffVars, 
			ConditioningVariables remainderVars) {
		backoff = backoffVars;
		remainder = remainderVars;
		hash = this.hash();
	}
	
	public ConditioningVariables getBackoff() {
		return backoff;
	}

	@Override
	public int hashCode() {
		return hash;
	}
	
	@Override
	public String toString() {
		return backoff.toString()+" # "+remainder.toString();
	}

	private final int hash() {
		int result = 17;
		result = result * 31 + backoff.hashCode();
		result = result * 31 + remainder.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object oth) {
		if(this == oth) {
			return true;
		}
		if (oth == null || !(oth instanceof ComplexConditioningVariables)) {
			return false;
		}
		ComplexConditioningVariables o = (ComplexConditioningVariables) oth;
		return o.remainder.equals(this.remainder) && o.backoff.equals(this.backoff);
	}

	@Override
	public int getVariableValue(int varIndex) {
		if(varIndex < backoff.length()) {
			return backoff.getVariableValue(varIndex);
		}
		return remainder.getVariableValue(varIndex-backoff.length());
	}
	
	@Override
	protected int length() {
		return backoff.length() + remainder.length();
	}
	
	@Override
	public String saveToString() {
		// TODO Auto-generated method stub
		return null;
	}

	public static ConditioningVariables loadFromString(String input, boolean cacheVariables) {
		int split = input.lastIndexOf("#");
		String backoffInput = input.substring(0, split).trim();
		ConditioningVariables backoff = ConditioningVariables.loadFromString(backoffInput, cacheVariables);
		String remainderInput = input.substring(split+1).trim();
		ConditioningVariables remainder = ConditioningVariables.loadFromString(remainderInput, cacheVariables);
		if(cacheVariables) {
			return VariablesFactory.get(backoff, remainder);
		}
		return VariablesFactory.cache(backoff, remainder);
	}
}
