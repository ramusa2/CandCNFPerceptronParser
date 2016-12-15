package ccgparser.runners.test;

import illinoisParser.Grammar;
import illinoisParser.Rule;
import illinoisParser.SupervisedParsingConfig;
import illinoisParser.variables.ConditioningVariables;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import perceptron.parser.ccnormalform.NormalFormPerceptronParser;

import ccgparser.util.OfficialCandCFeature;
import ccgparser.util.OfficialCandCReader;

public class OfficialCandCReaderTester {

	static String parserDir = "candc-1.00/models/parser/";

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		Grammar grammar = Grammar.load(new File("grammar"));
		NormalFormPerceptronParser parser = OfficialCandCReader.loadPretrainedCandCNormalFormParser(parserDir, grammar);
		//Grammar grammar = parser.grammar();
		
		/*
		HashMap<String, Integer> categories = testCategoryReading();
		ArrayList<Rule> rules = testRuleReading();
		HashMap<String, Integer> lexicon = testWordAndPOSReading();
		ArrayList<OfficialCandCFeature> features = testFeatureReading();
		ArrayList<Double> weights = testWeightReading();
		System.out.println("Read "+categories.size()+" categories.");
		System.out.println("Read "+lexicon.size()+" vocabulary entries (words/POS tags).");
		System.out.println("Read "+rules.size()+" rules.");
		System.out.println("Read "+features.size()+" features.");
		System.out.println("Read "+weights.size()+" feature weights.");
		//printUsedFeatureTemplates(features);
		ArrayList<ConditioningVariables> variables = testConvertFeatures(features, grammar);
		System.out.println("Converted "+variables.size()+" features.");
		
		
		NormalFormPerceptronParser parser = new NormalFormPerceptronParser(grammar, SupervisedParsingConfig.getDefaultConfig());
		parser.setFeatureWeights(variables, weights);		
		*/
	}

	private static ArrayList<ConditioningVariables> testConvertFeatures(ArrayList<OfficialCandCFeature> features,
			Grammar grammar) {
		ArrayList<ConditioningVariables> converted = new ArrayList<ConditioningVariables>();
		for(OfficialCandCFeature feature : features) {
			converted.add(feature.convertToOurNormalForm(grammar));
		}
		System.out.println("Successfully converted features (but still verify equivalence).");
		return converted;
	}

	private static void printUsedFeatureTemplates(
			ArrayList<OfficialCandCFeature> features) {
		HashSet<Character> used = new HashSet<Character>();
		for(OfficialCandCFeature feature : features) {
			used.add(feature.getTemplateID());
		}
		ArrayList<Character> sorted = new ArrayList<Character>();
		sorted.addAll(used);
		Collections.sort(sorted);
		System.out.println("Used feature templates:");
		for(Character c : sorted) {
			System.out.println("\t"+c);
		}
	}

	private static ArrayList<OfficialCandCFeature> testFeatureReading() {
		ArrayList<OfficialCandCFeature> features = OfficialCandCReader.readFeatures(parserDir);
		return features;
	}

	private static ArrayList<Double> testWeightReading() {
		ArrayList<Double> weights = OfficialCandCReader.readWeights(parserDir);
		return weights;
	}

	private static HashMap<String, Integer> testWordAndPOSReading() {
		HashMap<String, Integer> lexicon = OfficialCandCReader.readWordsAndPOS(parserDir);

		return lexicon;
	}

	private static HashMap<String, Integer> testCategoryReading() {
		HashMap<String, Integer> categories = OfficialCandCReader.readCategories(parserDir);

		return categories;
	}

	private static ArrayList<Rule> testRuleReading() {
		ArrayList<Rule> rules = OfficialCandCReader.readRules(parserDir);

		return rules;
	}

}
