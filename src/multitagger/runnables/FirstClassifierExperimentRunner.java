package multitagger.runnables;

import java.io.File;
import java.util.Scanner;

import multitagger.FirstClassifierExperiment;

public class FirstClassifierExperimentRunner {
	
	public static void main(String[] args) throws Exception {
		if(args.length != 2) {
			System.out.println("Mandatory arguments: DIR CONFIG_FILE");
			return;
		}
		String directory = args[0];
		//runExperiment(directory, null);
		String configFile = args[1];
		runExperiment(directory, new File(configFile));
	}

	private static void runExperiment(String directory, File configFile) throws Exception {
		FirstClassifierExperiment exp = new FirstClassifierExperiment(directory);
		if(configFile != null) {
			Scanner sc = new Scanner(configFile);
			String line;
			while(sc.hasNextLine()) {
				if(!(line = sc.nextLine().trim()).isEmpty()) {
					if(line.contains("=")) {
						int splitIndex = line.indexOf("=");
						String arg = line.substring(0, splitIndex);
						String val = line.substring(splitIndex+1);
						exp.setArgument(arg, val);
					}
					else {
						System.out.println("Failed to parse argument: "+line);
					}
				}
			}
			sc.close();
		}
		exp.runExperiment();
	}

}
