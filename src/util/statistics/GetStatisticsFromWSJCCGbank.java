package util.statistics;

import java.io.File;
import java.util.Collection;
import illinoisParser.CCGbankReader;
import illinoisParser.Rule_Type;
import illinoisParser.Sentence;

public class GetStatisticsFromWSJCCGbank {

	public static void main(String[] args) {
		int lowSec = 2;
		int highSec = 21;
		String autoDir = "data/CCGbank/AUTO/";
		(new File("documentation/notes/grammar_statistics")).mkdirs();
		Collection<Sentence> corpus = CCGbankReader.getCCGbankData(lowSec, highSec, autoDir);
		GrammarStatistics stats = GrammarStatistics.readStatistics(corpus);
		writeStats(stats);
	}

	private static void writeStats(GrammarStatistics stats) {
		FrequencyList<String> allList = stats.getRuleCountsAsString();
		allList.writeToFile(new File("documentation/notes/grammar_statistics/rule_freqs.txt"));
		Rule_Type[] types = Rule_Type.values();
		for(Rule_Type type : types) {
			FrequencyList<String> list = stats.getRuleCountsAsString(type);
			if(list.size() > 0) {
				list.writeToFile(new File("documentation/notes/grammar_statistics/rule_freqs."+type+".txt"));
			}
		}
		FrequencyList<String> lcList = stats.getLexicalCategoryCounts();
		lcList.writeToFile(new File("documentation/notes/grammar_statistics/lexical_category_freqs.txt"));
	}

}
