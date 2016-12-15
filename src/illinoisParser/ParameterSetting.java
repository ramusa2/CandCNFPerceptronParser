package illinoisParser;

/**
 * When setting parameters for a {@link SupervisedConfiguration}, the value may
 * be of type String, Double, Integer, etc.
 * 
 * This class supports parameterization for the different types while allowing
 * an instance of SupervisedConfig to maintain a single map of all of the values.
 * 
 * @author ramusa2
 *
 * @param <T> the type of the parameter's value
 */
public class ParameterSetting<T> {
	
	/**
	 * The ParameterSetting's value
	 */
	private T value;
	
	/**
	 * Default constructor (sets initial value to val)
	 */
	public ParameterSetting(T val) {
		this.setValue(val);
	}
	
	/**
	 * Sets the parameter's value
	 * @param val the new value
	 */
	public void setValue(T val) {
		value = val;
	}
	
	/**
	 * Returns the parameter's value
	 * @return the parameter value, with appropriate type
	 */
	public T getValue() {
		return value;
	}

	@Override
	public boolean equals(Object obj) {
		return (obj==this) || (obj instanceof ParameterSetting && ((ParameterSetting)obj).value.equals(this.value)); 
	}
	
	@Override
	public int hashCode() {
		return value.hashCode();
	}
	
	@Override
	public String toString() {
		return value.toString();
	}
}
