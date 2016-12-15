package illinoisParser;

import java.io.FileNotFoundException;

/**
 * An executable class for training and evaluating supervised CCGbank parsers. <br>
 * Options: <br>
 * 
 * 		train {@link SupervisedTrainingConfig training_config_file} <br> 
 * 	    save model_dir <br>
 *  	load model_dir <br>
 *  	parse {@link SupervisedParsingConfig parsing_config_file} <br>
 *  
 * @author ramusa2
 * @see illinoisParser.SupervisedParser SupervisedParser 
 */
public class SupervisedParserMain {
	
	/**
	 * The list of valid arguments for this routine
	 */
	protected static final String[] validArgs;
	/**
	 * Argument for training a new supervised parser (parameters are specified in a training configuration file)
	 */
	protected static final String TRAIN; 
	/**
	 * Argument for saving the current parser to disk (i.e., the specified directory)
	 */
	protected static final String SAVE;
	/**
	 * Argument for loading a supervised parser from disk (i.e., the specified directory)
	 */
	protected static final String LOAD;
	/**
	 * Argument for parsing data with the current parser (data is specified in a parsing configuration file)
	 */
	protected static final String PARSE;
	
	// Initialize validArgs
	static {
		TRAIN = "train";
		SAVE = "save";
		LOAD = "load";
		PARSE = "parse";
		validArgs = new String[]{TRAIN, SAVE, LOAD, PARSE};
	}

	/**
	 * Main runtime method, called with one or more {@link SupervisedParserMain#validArgs arguments}
	 * @param args:  
	 * 		train {@link SupervisedTrainingConfig training_config_file}
	 * 		save model_dir
	 *  	load model_dir	
	 *  	parse {@link SupervisedParsingConfig parsing_config_file}
	 */
	public static void main(String[] args) {
		// Note: must provide an argument
		if(args.length == 0) {
			System.out.println("Must provide at least one valid argument/parameter. System exiting.");
			return;
		}
		
		// TODO: initialize Util for printing logs, and change these System.out calls to Util.log
		
		Model model = null;
		int argIndex = 0;
		String arg = "";
		try {
			while(argIndex < args.length) {
				// Get next arg and increment counter
				arg = args[argIndex++].toLowerCase();
				if(arg.equals(TRAIN)) {
					String confFile = args[argIndex++];
					SupervisedTrainingConfig config = SupervisedTrainingConfig.readFromFile(confFile);
					model = SupervisedParser.trainModel(config);
				}
				else if(arg.equals(SAVE)) {
					if(model == null) {
						throw new IllegalArgumentException("Attempting to save null model; " +
								"call train or load before saving.");
					}
					String saveDir = args[argIndex++];
					SupervisedParser.saveModel(model, saveDir);
				}
				else if(arg.equals(LOAD)) {
					String loadDir = args[argIndex++];
					if(model != null) {
						System.out.println("Warning: overwriting current model; loading from: "+loadDir);
					}
					model = SupervisedParser.loadModel(loadDir);
				}
				else if(arg.equals(PARSE)) {
					if(model == null) {
						throw new IllegalArgumentException("Attempting to parse with null model; " +
								"call train or load before parsing.");
					}
					String confFile = args[argIndex++];
					SupervisedParsingConfig config = SupervisedParsingConfig.readFromFile(confFile);
					SupervisedParser.parse(model, config);
				}
				else {
					throw new IllegalArgumentException();
				}
			}
		}
		catch(IllegalArgumentException e) {
			// If the user provides an invalid argument, indicate this and enumerate the valid arguments
			String msg = "Illegal SupervisedParserMain argument: "+arg+"\nLegal arguments:\n";
			for(String validArg : validArgs) {
				msg += "    "+validArg+"\n";
			}
			System.out.println(msg);
			System.out.println("Exiting...");
			System.exit(1);
		}
		catch(ArrayIndexOutOfBoundsException e) {
			// If the user fails to provide a parameter, explain which parameter is missing
			String msg = "Malformed SupervisedParserMain argument ("+arg+"); parameter required.\n";
			if(arg.equals(TRAIN)) {
				msg += "   Usage: "+arg+" training_config_file";
			}
			else if(arg.equals(SAVE)) {
				msg += "   Usage: "+arg+" model_dir";
			}
			else if(arg.equals(LOAD)) {
				msg += "   Usage: "+arg+" model_dir";
			}
			else if(arg.equals(PARSE)) {
				msg += "   Usage: "+arg+" parsing_config_file";
			}
			System.out.println(msg);
			System.out.println("Exiting...");
			System.exit(1);
		}
		catch(FileNotFoundException e) {
			String msg = "No configuration file found at: ("+arg+")\n";
			System.out.println(msg);
			System.out.println("Exiting...");
			System.exit(1);
		}
	}
}
