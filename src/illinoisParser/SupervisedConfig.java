package illinoisParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Scanner;

/**
 * Stores the parameters for a supervised parser on CCGbank; has subclasses
 * for specific parameter sets for {@link SupervisedTrainingConfig training}
 * and {@link SupervisedParsingConfig parsing}.
 * 
 * @author ramusa2
 */
public abstract class SupervisedConfig {
	
	/**
	 * TODO: FIXME: This version of SupervisedConfig sacrifices type safety for ease of adding
	 * parameters (in the subclasses). Once the set of configuration options is static, we should
	 * migrate to a type-safe SupervisedConfig class where each subclass sets (explicitly-typed) 
	 * private instance variables for each of their parameter values.
	 */

	/**
	 * A HashMap storing parameter keys with their ParameterSetting values
	 */
	private HashMap<String, ParameterSetting> parameterMap; 
	// ^ Using raw type allows us to specify a type for each value, but is NOT TYPESAFE

	/**
	 * Protected default constructor used by factory method(s)
	 * Sets the default parameters for this Configuration
	 */
	protected SupervisedConfig() {
		this.parameterMap = new HashMap<String, ParameterSetting>();
		this.setDefaultParameterValues();
	}
	
	/**
	 * Each subclass needs to override this method to add a default value for each of its
	 * parameters. Furthermore, if a parameter does *not* have a default value set,
	 * then readFromFile() will fail if the text file attempts to set a value for
	 * an undefined parameter.
	 */
	protected abstract void setDefaultParameterValues();
	
	/**
	 * 
	 * Default factory method (called with a default instance of a subclass); 
	 * constructs a subclass of SupervisedConfig by reading in parameters from a file
	 * 
	 * @param filename	path to the configuration text file
	 * @param config	a SupervisedConfig-subclass object with default parameters
	 * @throws FileNotFoundException
	 * @throws IllegalArgumentException
	 */
	protected static void readParametersFromFile(SupervisedConfig config, String filename) 
			throws FileNotFoundException, IllegalArgumentException {
		Scanner sc = new Scanner(new File(filename)); 
		// throws FileNotFoundException if no file at filename
		while(sc.hasNextLine()) {
			String line = sc.nextLine().trim();
			try {
				config.setExistingParameter(line);
			}
			catch(IllegalArgumentException e) {
				sc.close();
				throw new IllegalArgumentException("Cannot read " +
						"training configuration file; illegal parameter setting: "+line);
			}
		}
		sc.close();
	}

	/**
	 * Saves this configuration object to a text file
	 * @param filename path to the target save file
	 * @throws FileNotFoundException 
	 */
	public void saveParametersToFile(String filename) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(new File(filename)); 
		for(String param : this.parameterMap.keySet()) {
			String line = param;
			while(line.length() < 30) {
			    line += " ";
			}
			pw.println(line+this.parameterMap.get(param).toString());
		}
		pw.close();
	}

	/**
	 * Associates a value with an existing parameter. Attempts to infer the type of that value.
	 * @param parameterAndValue a parameter-value pair, stored as a whitespace-split delimited String 
	 */
	protected void setExistingParameter(String parameterAndValue) {
		String parameter = parameterAndValue.split("\\s+")[0];
		String value = parameterAndValue.split("\\s+")[1];
		try {
			Double val = Double.parseDouble(value);
			this.checkAndSetExistingParameter(parameter, new ParameterSetting(val));
		}
		catch(NumberFormatException e) {
			// FIXME: dictating flow by catching exceptions is bad practice
			try {
				Integer val = Integer.parseInt(value);
				this.checkAndSetExistingParameter(parameter, new ParameterSetting(val));
			}
			catch(NumberFormatException e2) {
				// FIXME: dictating flow by catching exceptions is bad practice
				if(value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
					Boolean val = Boolean.parseBoolean(value);
					this.checkAndSetExistingParameter(parameter, new ParameterSetting(val));
				}
				else {
					this.checkAndSetExistingParameter(parameter, new ParameterSetting(value));
				}
			}
		}
	}	

	/**
	 * Synchronized method to associated an existing parameter with a new value.
	 * @param parameter
	 * @param value
	 */
	private synchronized void checkAndSetExistingParameter(String parameter,
			ParameterSetting value) throws IllegalArgumentException {
		if(this.parameterMap.containsKey(parameter)) {
			this.parameterMap.put(parameter, value);
		}
		else {
			throw new IllegalArgumentException("This configuration doesn't include a parameter called: "+parameter+".\n" +
					"Use addNewParameter() to set the default vlaue of a new parameter.");
		}
	}
	
	/**
	 * Adds a new parameter, provided that one of this name does not already exist.
	 * Throws IllegalArgumentException if a parameter of this name already exists.
	 * @param parameter
	 * @param value
	 */
	protected void addNewParameter(String parameter, ParameterSetting value) 
												throws IllegalArgumentException {
		if(!this.parameterMap.containsKey(parameter)) {
			this.parameterMap.put(parameter, value);
		}
		else {
			throw new IllegalArgumentException("This configuration already includes a parameter called: "+parameter+".\n" +
					"Use checkAndSetExistingParameter() to overwrite the value of an existing parameter.");
		}
	}
	
	/**
	 * Returns the ParameterSetting associated with the given parameter, or null if no setting exists
	 * @param parameter the parameter String key to look up
	 * @return the ParameterSetting value for parameter if one exists, else null
	 */
	protected ParameterSetting lookup(String parameter) {
		return this.parameterMap.get(parameter);
	}
}
