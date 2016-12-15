package util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;

import illinoisParser.CCGbankReader;
import illinoisParser.CCGbankTrainer;
import illinoisParser.Chart;
import illinoisParser.FineChartItem;
import illinoisParser.Grammar;
import illinoisParser.Model;
import illinoisParser.Sentence;
import illinoisParser.SupervisedParsingConfig;
import illinoisParser.SupervisedTrainingConfig;
import illinoisParser.Tree;
import illinoisParser.Util;
import illinoisParser.models.HWDepModel;

public class TopKParseGenerator {

	private static final int K = 100;
	private static final int MAX_LENGTH = 40;
	private static final String OUTPUT = "wsj2-21_max40.topk.txt";
	private static String DELIMITER = "####";

	public static void main(String[] args) throws FileNotFoundException {
		// Get training data
		int[] secs = new int[20];
		for(int s=2; s<=21; s++) {
			secs[s-2] = s;
		}
		String autoDir = "data/CCGbank/AUTO";
		Collection<Sentence> sentences = CCGbankReader.getCCGbankData(secs, autoDir);

		// Initialize model
		Grammar g = new Grammar();
		Model m = new HWDepModel(g);
		SupervisedTrainingConfig c = SupervisedTrainingConfig.getDefaultConfig();
		try {
			CCGbankTrainer.readGrammarAndTrainModel(sentences, g, m, c);
		}
		catch(Exception e) {
			e.printStackTrace();
		}

		// Output
		PrintWriter pw = new PrintWriter(new File(OUTPUT));
		// Parse sentences
		int parsed = 1;
		for(Sentence sen : sentences) {
			if(sen.length() <= MAX_LENGTH) {
				try {
					SupervisedParsingConfig cp = SupervisedParsingConfig.getDefaultConfig();
					Chart chart = new Chart(sen, g);
					chart.coarseParse(g, cp);
					if(chart.successfulCoarseParse()) {
						chart.fineParse(m, cp);
						if(chart.successfulFineParse()) {
							ArrayList<Tree<? extends FineChartItem>> topK = chart.fineRoot().getKBestTrees(m, K);
							String entry = sen.getCCGbankParse()+"\n"
									+sen.toString()+"\n";
							for(Tree<? extends FineChartItem> tree : topK) {
								chart.fsRecurse(tree);
								StringBuilder sb = new StringBuilder();
								Util.buildAUTORecurse(sb, sen, m, tree);
								String line = tree.probability()+" "+sb.toString()+"\n";
								entry += line;
							}
							entry += DELIMITER;
							pw.println(entry);
						}
					}
				}
				catch(Exception e) {
					e.printStackTrace();
					pw.println(sen.getCCGbankParse());
					pw.println(sen.toString());
					pw.println(DELIMITER);
				}
			}
			parsed++;
			if(parsed % 100 == 0) {
				System.out.println("Tagged "+parsed+" out of "+sentences.size()+" sentences.");
			}
		}
		pw.close();
	}


}
