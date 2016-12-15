package util;

import illinoisParser.Grammar;
import illinoisParser.SupervisedParsingConfig;

import perceptron.core.representations.parsing.PackedFeatureForest;
import perceptron.parser.PerceptronParser;
import perceptron.parser.ccnormalform.NormalFormPerceptronParser;
import util.serialization.SerializedData;

public class TrainPerceptronParserFromCharts {
	
	public static void trainPerceptronParserOnPackedForests(Grammar grammar, SerializedData<PackedFeatureForest> data, int maxSenLength, int numIters, 
			String saveDirectory, String saveFileName) {

		// Train parser
		SupervisedParsingConfig c2 = SupervisedParsingConfig.getDefaultConfig();
		PerceptronParser parser = new NormalFormPerceptronParser(grammar, c2);
		
		parser.setSaveDirectory(saveDirectory);
		parser.trainOnPackedForests(data, numIters, maxSenLength);
		parser.save(saveFileName);
	}

}