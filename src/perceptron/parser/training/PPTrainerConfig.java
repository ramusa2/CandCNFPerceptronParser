package perceptron.parser.training;

import illinoisParser.Grammar;
import illinoisParser.SupervisedParsingConfig;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Scanner;

import perceptron.parser.PerceptronParser;
import perceptron.parser.ccnormalform.NormalFormPerceptronParser;

/**
 * The PPTrainerConfig class stores the parameters used by a {@link PPTrainer} pipeline.
 * 
 * @author ramusa2
 *
 */
public class PPTrainerConfig {
	
	/**
	 * Number of iterations through the training data
	 */
	private int numTrainingIterations = 10;
	
	/**
	 * Length of the longest sentence allowed in the training data
	 */
	private int maxSentenceLength = 40;
	
	/**
	 * Default/empty constructor, used in factory methods.
	 */
	private PPTrainerConfig() {}

	/**
	 * Returns the default configuration object (default parameters).
	 */
	public static PPTrainerConfig getDefaultConfig() {
		PPTrainerConfig config =  new PPTrainerConfig();		
		return config;
	}
	
	/**
	 * Loads an existing training pipeline from disk.
	 * 
	 * @param file	the top-level directory
	 * @return		the loaded training pipeline
	 */
	public static PPTrainerConfig load(File file) {
		PPTrainerConfig config = new PPTrainerConfig();
		Scanner sc = null;
		try {
			sc = new Scanner(file);
			String line;
			while(sc.hasNextLine()) {
				line = sc.nextLine();
				if(line.contains("#")) {
					line = line.substring(0, line.indexOf("#"));
				}
				line = line.trim();
				if(!line.isEmpty()) {
					String[] argVal = line.split("\\s+");
					if(argVal.length >= 2) {
						String arg = argVal[0];
						String val = argVal[1];
						if(arg.equalsIgnoreCase("numTrainingIterations")) {
							config.numTrainingIterations = Integer.parseInt(val);
						}
						else if(arg.equalsIgnoreCase("maxSentenceLengths")) {
							config.maxSentenceLength = Integer.parseInt(val);
						}
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		if(sc != null) {
			sc.close();
		}
		return config;
	}
	
	/**
	 * Saves a configuration to disk.
	 * 
	 * @param file 	output file
	 */
	public void save(File file) {
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(file);
			pw.println("numTrainingIterations\t\t"+this.numTrainingIterations);
			pw.println("maxSentenceLength    \t\t"+this.maxSentenceLength);				
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		if(pw != null) {
			pw.close();
		}
	}

	/**
	 * Returns a new {@link perceptron.parser.ccnormalform.NormalFormPerceptronParser} that uses the specified grammar and config.
	 */
	public PerceptronParser getNewParser(Grammar grammar, SupervisedParsingConfig config) {
		// TODO: make this depend on a variable
		return new NormalFormPerceptronParser(grammar, config);
	}

	/**
	 * Returns the number of iterations through the training data performed during learning.
	 */
	public int getNumTrainingIterations() {
		return this.numTrainingIterations;
	}

	/**
	 * Returns the length of the longest sentence that would be allowed to be parsed. 
	 */
	public int getMaxSentenceLength() {
		return this.maxSentenceLength;
	}
}
