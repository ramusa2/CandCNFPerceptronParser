package util;

import illinoisParser.CCGbankReader;
import illinoisParser.Sentence;

import java.io.File;
import java.io.PrintWriter;
import java.util.Collection;

import util.statistics.FrequencyList;
import util.statistics.FrequencyListEntry;
import util.statistics.GrammarStatistics;

/**
 * Given an optional integer argument k (default k=10), prints a list of categories appearing
 * at least k times in sections 2-21 to categories_freq=k.txt
 * @author ramusa2
 *
 */
public class GenerateSupertaggerCategoryList {
	
	
	public static void main(String[] args) throws Exception {
		int k = 1;
		if(args.length > 0) {
			k = Integer.parseInt(args[0]);
		}
		int lowSec = 2;
		int highSec = 21;
		String autoDir = "data/CCGbank/AUTO/";
		Collection<Sentence> corpus = CCGbankReader.getCCGbankData(lowSec, highSec, autoDir);
		GrammarStatistics stats = GrammarStatistics.readStatistics(corpus);
		PrintWriter pw = new PrintWriter(new File("categories_freq="+k+".txt"));
		FrequencyList<String> counts = stats.getLexicalCategoryCounts();
		for(FrequencyListEntry<String> entry : counts.sortedList()) {
			if(entry.frequency() >= k) {
				pw.println(entry.value());
			}
		}
		pw.close();
	}

}
