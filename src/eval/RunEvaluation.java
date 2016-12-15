package eval;

public class RunEvaluation {
	
	private static String goldAuto;
	private static String guessAuto;
	private static String outputDir;
	
	public static void main(String[] args) {
		processArgs(args);
		evaluate();
	}

	private static void processArgs(String[] args) {
		if(args.length != 3) {
			printArgsMessageAndExit();
		}
		for (String arg : args) {
			if(arg.startsWith("gold-auto=")) {
				goldAuto = arg.split("=")[1];
			}
			else if(arg.startsWith("guess-auto=")) {
				guessAuto = arg.split("=")[1];
			}
			else if(arg.startsWith("outputdir=")) {
				outputDir = arg.split("=")[1];
			}
			else {
				printArgsMessageAndExit();
			}
		}
		checkArgs();
	}

	private static void checkArgs() {
		if(goldAuto == null) {
			System.out.println("Failed to set gold-auto; please give a valid .auto filename");
			printArgsMessageAndExit();
		}
		if(guessAuto == null) {
			System.out.println("Failed to set guess-auto; please give a valid .auto filename");
			printArgsMessageAndExit();
		}
		if(outputDir == null) {
			System.out.println("Failed to set output directory");
			printArgsMessageAndExit();
		}
	}

	private static void printArgsMessageAndExit() {
		System.out.println("Please provide three arguments: " +
				"\ngold-auto=xxx.auto" +
				"\nguess-auto=yyy.auto" +
				"\noutputdir=z/z/z/");
		System.exit(1);
	}

	private static void evaluate() {
		Evaluator eval = new Evaluator(goldAuto, guessAuto);
		eval.runEval();
		eval.writeResultsToDir(outputDir);
		System.out.println("Done evaluating.");
	}

}
