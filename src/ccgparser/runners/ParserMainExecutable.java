package ccgparser.runners;

import ccgparser.core.config.ExecutableRoutine;

/**
 * This class is the entry point for training or parsing with the 
 * Illinois CCG parsing models.
 * 
 * @author ramusa2
 *
 */
public class ParserMainExecutable {
	
	private static ExecutableRoutine routine;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		readArgs(args);
		executeRoutine();
	}

	private static void readArgs(String[] args) {
		// Get routine to execute
		if(args.length == 0) {
			System.out.println("ERROR: No arguments provided (the parser doesn't know what you want it to do).");
			printExecutableRoutines();
			System.exit(0);
		}
		try {
			routine = ExecutableRoutine.valueOf(args[0].toLowerCase());
		}
		catch(Exception e) {
			System.out.println("ERROR: Argument '"+args[0]+"' isn't a valid routine (the parser doesn't know what you want it to do).");
			printExecutableRoutines();
			System.exit(0);
		}
		// Read additional routine-specific arguments
		
	}
	
	private static void printExecutableRoutines() {
		System.out.println("Please run the executable with one of the following routines as the first argument:\n");
		System.out.println("  create        Initializes the file structure for the supertagging and parsing models.");
		System.out.println("  train         Learns supertagger and parsing model parameters.");
		System.out.println("  supertag      Assigns lexical category distributions to input text.");
		System.out.println("  parse         Assigns parse trees to supertagged text.");
		System.out.println("  evaluate      Supertags and parses input text and evaluate against gold derivations.");
	}

	private static void executeRoutine() {
		// TODO Auto-generated method stub
		switch(routine) {
		case create:
			break;
		case train:
			break;
		case supertag:
			break;
		case evaluate:
			break;
		case parse:
			break;
		default:
			break;
		}
	}

}
