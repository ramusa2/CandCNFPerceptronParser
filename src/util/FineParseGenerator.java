package util;

import perceptron.core.representations.parsing.PackedFeatureForest;
import perceptron.parser.PerceptronChart;
import perceptron.parser.PerceptronParser;
import util.serialization.SerializedData;
import illinoisParser.Grammar;

public class FineParseGenerator {

	public static void generateFineParses(Grammar grammar,
			SerializedData<PerceptronChart> coarseParseCache, 
			PerceptronParser parser,
			SerializedData<PackedFeatureForest> fineParseCache) {
		PerceptronChart chart = null;
		while((chart = coarseParseCache.next()) != null) {
			PackedFeatureForest forest = buildPackedFeatureForest(parser, chart);
			fineParseCache.addObject(forest);
		}
	}

	private static PackedFeatureForest buildPackedFeatureForest(
			PerceptronParser parser, PerceptronChart chart) {
		chart.fineParseWithPerceptronModel(parser, false);
		return new PackedFeatureForest(chart, parser, true);
	}
}
