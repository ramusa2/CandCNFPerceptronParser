package illinoisParser.variables;

public abstract class ConditioningVariables {

	public abstract int getVariableValue(int varIndex);

	protected abstract int length();
	
	public abstract String saveToString();
	
	public static ConditioningVariables loadFromString(String input, boolean cacheVariables) {
		if(input.contains("#")) {
			return ComplexConditioningVariables.loadFromString(input, cacheVariables);
		}
		if(input.trim().isEmpty()) {
			return VariablesFactory.getEmpty();
		}
		String[] strToks = input.trim().split("\\s+");
		int[] vars = new int[strToks.length];
		for(int v=0;v<vars.length;v++) {
			vars[v] = Integer.parseInt(strToks[v]);
		}
		if(cacheVariables) {
			return VariablesFactory.cache(vars);
		}
		return VariablesFactory.get(vars);
	}
}