package illinoisParser;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import util.statistics.FrequencyList;
import util.statistics.FrequencyListEntry;
import util.statistics.GrammarStatistics;

/**
 * CCGbankTrainer is a static utility class for training an empty model/grammar on raw 
 * CCGbank data (stored as Sentences). Subtasks include counting word frequencies, generating 
 * gold parses from an AUTO string, adding words/categories/rules to the grammar, etc. 
 * 
 * @author ramusa2
 *
 */
public abstract class CCGbankTrainer {

	public static void readGrammarAndTrainModel(Collection<Sentence> sentences, Grammar grammar, 
			Model model, SupervisedTrainingConfig config) throws Exception {
		CCGbankTrainer.setKnownWords(sentences, grammar, 
				config.getMaxSentenceLength(), config.getKnownWordFreqCutoff());
		//Util.logln("Training on "+sentences.size()+" sentences:");
		for (Sentence sen : sentences) {
			Util.stat(sen.getID());
			if(sen.length() <= config.getMaxSentenceLength()) {
				CCGbankTrainer.trainOnSentence(grammar, model, sen);
			}
		}
		grammar.buildLeftToRightMap();
		if(model != null) {
			model.finalizeProbabilityDistributions();
		}
	}
	

	public static Grammar readGrammar(Collection<Sentence> sentences, int wordFreqMin) throws Exception {
		Grammar grammar = new Grammar();
		CCGbankTrainer.setKnownWords(sentences, grammar, wordFreqMin);
		for (Sentence sen : sentences) {
			CCGbankTrainer.getCoarseChartFromAutoParse(grammar, sen);
		}
		grammar.buildLeftToRightMap();
		return grammar;
	}

	public static Grammar readGrammarWithRuleFrequencyCutoff(
			Collection<Sentence> sentences, int minRuleFreq, int wordFreqMin) throws Exception {
		Grammar grammar = new Grammar();
		CCGbankTrainer.setKnownWords(sentences, grammar, wordFreqMin);
		// Train full grammar
		for (Sentence sen : sentences) {
			CCGbankTrainer.getCoarseChartFromAutoParse(grammar, sen);
		}
		// Prune out rules that don't occur frequently enough
		GrammarStatistics stats = GrammarStatistics.readStatistics(sentences);
		FrequencyList<Rule> rules = stats.getRuleCounts();
		HashSet<Rule> allowedRules = new HashSet<Rule>();
		for(FrequencyListEntry<Rule> entry : rules.sortedList()) {
			if(entry.frequency() >= minRuleFreq) {
				allowedRules.add(entry.value());
			}
			else {
				break;
			}
		}
		grammar.pruneRules(allowedRules);
		return grammar;
	}

	private static void setKnownWords(Collection<Sentence> sentences,
			Grammar grammar, int knownWordFreqCutoff) {
		setKnownWords(sentences, grammar, 10000,  knownWordFreqCutoff);
	}


	private static void setKnownWords(Collection<Sentence> sentences, Grammar grammar,
			int maxSenLength, int knownWordFreqCutoff) {
		HashMap<String, Integer> freqs = new HashMap<String, Integer>();
		for(Sentence sen : sentences ) {
			if(sen.length() <= maxSenLength) {
				for(LexicalToken lt : sen.getTokens()) {
					String w = lt.getWord();
					Integer f = freqs.get(w);
					f = (f == null) ? 1 : f+1;
					freqs.put(w, f);
				}
			}
		}
		for(Entry<String, Integer> entry : freqs.entrySet()) {
			if(entry.getValue() >= knownWordFreqCutoff) {
				grammar.addKnownWord(entry.getKey());
			}
		}
	}

	public final static Chart getCoarseChartFromAutoParse(Grammar grammar, Sentence sentence) throws Exception {
		AutoDecoder decoder = new AutoDecoder(sentence, sentence.getCCGbankParse());
		return decoder.getCoarseChart(grammar);
	}

	public final static void trainOnSentence(Grammar grammar, Model model, Sentence sentence) throws Exception {
		// Get the coarse chart (also adds rules to grammar)
		Chart coarseChart = CCGbankTrainer.getCoarseChartFromAutoParse(grammar, sentence);
		if(coarseChart == null) {
			Util.logln("Failed to train on sentence (bad CCGcat combine?): "+sentence.asWords());
			return;
		}
		coarseChart.computeWordsAndTagsWithUnk(grammar);
		if(model != null) {
			CCGbankTrainer.addCountsToModelRecurse(coarseChart.coarseRoot, model, coarseChart);
		}
	}

	private final static void addCountsToModelRecurse(CoarseChartItem ci, Model model, Chart coarseChart) throws Exception {
		if (ci instanceof CoarseLexicalCategoryChartItem) {
			FineChartItem fineLexicalCI = model.getFineLexicalChartItem((CoarseLexicalCategoryChartItem)ci, coarseChart);
			model.addCountsFromFineLexicalChartItem(fineLexicalCI, coarseChart);
			fineLexicalCI = ci.addFineChartItem(fineLexicalCI);
			return;
		}
		for (BackPointer bp : ci.children) {
			if(bp.isUnary()) {
				CCGbankTrainer.addCountsToModelRecurse(bp.B(), model, coarseChart);
				for(FineChartItem fineChildCI : bp.B.fineItems()) {
					FineChartItem fineParentCI = model.getFineUnaryChartItem(ci, fineChildCI);
					fineParentCI = ci.addFineChartItem(fineParentCI);
					model.addCountsFromFineUnaryChartItem(fineParentCI, fineChildCI, coarseChart);
				}
			}
			else {
				CCGbankTrainer.addCountsToModelRecurse(bp.B(), model, coarseChart);
				CCGbankTrainer.addCountsToModelRecurse(bp.C(), model, coarseChart);
				for(FineChartItem fineLeftChildCI : bp.B.fineItems()) {
					for(FineChartItem fineRightChildCI : bp.C.fineItems()) {
						FineChartItem fineParentCI = model.getFineBinaryChartItem(ci, bp,
								fineLeftChildCI, fineRightChildCI);
						fineParentCI = ci.addFineChartItem(fineParentCI);
						FineBackPointer fineBP = new FineBackPointer((Binary) bp.r, fineLeftChildCI, fineRightChildCI);
						model.addCountsFromFineBinaryChartItem(fineParentCI, fineBP, coarseChart);
					}
				}
			}
		}
	}
}
