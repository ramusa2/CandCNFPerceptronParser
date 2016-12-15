package ccgparser.util;

import illinoisParser.Grammar;
import illinoisParser.Rule;
import illinoisParser.SupervisedParsingConfig;
import illinoisParser.variables.ConditioningVariables;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import perceptron.parser.ccnormalform.NormalFormPerceptronParser;

public class OfficialCandCReader {

	public static NormalFormPerceptronParser loadPretrainedCandCNormalFormParser(String parserDir, Grammar grammar) {
		//Grammar grammar = loadGrammar(parserDir);
		ArrayList<OfficialCandCFeature> features = OfficialCandCReader.readFeatures(parserDir);
		ArrayList<Double> weights = OfficialCandCReader.readWeights(parserDir);
		NormalFormPerceptronParser parser = new NormalFormPerceptronParser(grammar, SupervisedParsingConfig.getDefaultConfig());
		parser.setFeatureWeights(convertFeatures(features, grammar), weights);	
		System.out.println("Parser has retained "+parser.features().size()+" out of "+features.size()+" features.");
		return parser;
	}

	private static ArrayList<ConditioningVariables> convertFeatures(
			ArrayList<OfficialCandCFeature> features, Grammar grammar) {
		ArrayList<ConditioningVariables> converted = new ArrayList<ConditioningVariables>();
		for(OfficialCandCFeature feature : features) {
			ConditioningVariables feat = feature.convertToOurNormalForm(grammar);
			if(feat != null) {
				converted.add(feat);
			}
			else {
				if(!feature.desc.contains("X")) {
					System.out.println(feature.desc);
				}			
			}
		}
		return converted;
	}

	public static Grammar loadGrammar(String parserDir) {
		HashMap<String, Integer> categories = readCategories(parserDir);
		ArrayList<Rule> rules = readRules(parserDir);
		HashMap<String, Integer> lexicon = readWordsAndPOS(parserDir);
		// TODO: build Grammar object
		return null;
	}

	private static HashMap<String, Integer> readCategoriesHelper(String filename) {
		HashMap<String, Integer> categories = new HashMap<String, Integer>();
		int index = 0;
		Scanner sc = getScanner(filename);
		if(sc != null) {
			String line;
			while(sc.hasNextLine()) {
				line = sc.nextLine();
				if(!(line.isEmpty() || line.startsWith("#") || line.startsWith(" ") || line.startsWith("\t"))) {
					categories.put(line, index++);
				}
			}
			sc.close();
		}
		return categories;
	}

	private static HashMap<String, Integer> readWordsAndPOSHelper(String filename) {
		HashMap<String, Integer> lexicon = new HashMap<String, Integer>();
		int index = 0;
		Scanner sc = getScanner(filename);
		if(sc != null) {
			String line;
			while(sc.hasNextLine()) {
				line = sc.nextLine();
				if(!(line.isEmpty() || line.startsWith("#") || line.startsWith(" ") || line.startsWith("\t"))) {
					lexicon.put(line.split("\\s+")[0], index++);
				}
			}
			sc.close();
		}
		return lexicon;
	}

	private static ArrayList<Rule> readRulesHelper(String filename, HashMap<String, Integer> categoryIndices) {
		ArrayList<Rule> rules = new ArrayList<Rule>();
		Scanner sc = getScanner(filename);
		if(sc != null) {
			String line;
			while(sc.hasNextLine()) {
				line = sc.nextLine();
				if(!(line.isEmpty() || line.startsWith("#") || line.startsWith(" ") || line.startsWith("\t"))) {
					String[] rhs = line.trim().split("\\s+");
					// TODO: implement this? (maybe can re-use code from somewhere else, e.g. CCGcat.combine()?)
					Rule rule = null;
					rules.add(rule);
				}
			}
			sc.close();
		}
		return rules;
	}

	private static ArrayList<OfficialCandCFeature> readFeaturesHelper(
			String filename) {
		return OfficialCandCFeature.readFeaturesFromFile(new File(filename));
	}

	private static ArrayList<Double> readWeightsHelper(String filename) {
		ArrayList<Double> weights = new ArrayList<Double>();
		Scanner sc = getScanner(filename);
		if(sc != null) {
			String line;
			while(sc.hasNextLine()) {
				line = sc.nextLine();
				if(!(line.isEmpty() || line.startsWith("#") || line.startsWith(" ") || line.startsWith("\t"))) {
					weights.add(Double.parseDouble(line.trim()));
				}
			}
			sc.close();
		}
		return weights;
	}

	public static HashMap<String, Integer> readCategories(String parserDir) {
		return readCategoriesHelper(validateParserDir(parserDir)+"cats/markedup");
	}

	public static HashMap<String, Integer> readWordsAndPOS(String parserDir) {
		return readWordsAndPOSHelper(validateParserDir(parserDir)+"lexicon");
	}

	public static ArrayList<Rule> readRules(String parserDir) {
		return readRulesHelper(validateParserDir(parserDir)+"rules", readCategories(parserDir));
	}

	public static ArrayList<OfficialCandCFeature> readFeatures(String parserDir) {
		return readFeaturesHelper(validateParserDir(parserDir)+"features");
	}

	public static ArrayList<Double> readWeights(String parserDir) {
		return readWeightsHelper(validateParserDir(parserDir)+"weights");
	}

	private static String validateParserDir(String parserDir) {
		if(!parserDir.endsWith(File.separator)) {
			parserDir = parserDir+File.separator;
		}
		return parserDir;
	}

	private static Scanner getScanner(String filename) {
		try {
			return new Scanner(new File(filename));
		}
		catch(FileNotFoundException e) {
			System.err.println("Failed to open file while loading official C&C parser: "+filename);
		}
		return null;
	}

}
