package util.statistics;

import illinoisParser.BackPointer;
import illinoisParser.CCGbankTrainer;
import illinoisParser.Chart;
import illinoisParser.CoarseChartItem;
import illinoisParser.CoarseLexicalCategoryChartItem;
import illinoisParser.Grammar;
import illinoisParser.LexicalToken;
import illinoisParser.Sentence;
import illinoisParser.Rule;
import illinoisParser.Rule_Type;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

public class GrammarStatistics {

	private Grammar grammar;
	private FrequencyList<Rule> ruleCounts;
	private FrequencyList<String> categoryCounts;
	private FrequencyList<String> lexicalCategoryCounts;
	private HashMap<String, FrequencyList<String>> wordToLexicalCategoryCounts;
	private HashMap<String, FrequencyList<String>> lexicalCategoryToWordCounts;
	private FrequencyList<String> wordCounts;
	private FrequencyList<String> posCounts;

	private GrammarStatistics() {
		grammar = new Grammar();
		ruleCounts = new FrequencyList<Rule>();
		lexicalCategoryCounts = new FrequencyList<String>();
		categoryCounts = new FrequencyList<String>();
		wordCounts = new FrequencyList<String>();
		posCounts = new FrequencyList<String>();
		wordToLexicalCategoryCounts = new HashMap<String, FrequencyList<String>>();
		lexicalCategoryToWordCounts= new HashMap<String, FrequencyList<String>>();
	}

	public static GrammarStatistics readStatistics(Collection<Sentence> corpus) {
		GrammarStatistics stats = new GrammarStatistics();
		for(Sentence sen : corpus) {
			stats.addCountsFromSentence(sen);
		}
		return stats;
	}

	private void addCountsFromSentence(Sentence sen) {
		for(LexicalToken lt : sen.getTokens()) {
			this.wordCounts.addCount(lt.getWord());
			this.posCounts.addCount(lt.getPOS().toString());
		}
		try {
			Chart chart = CCGbankTrainer.getCoarseChartFromAutoParse(grammar, sen);
			this.addCountsRecurse(chart.coarseRoot(), sen);
		}
		catch(Exception e) {
			System.out.println("Failed to get stats for sentence: "+sen);
			e.printStackTrace();
		}
	}

	private void addCountsRecurse(CoarseChartItem ci, Sentence sen) {
		this.categoryCounts.addCount(grammar.getCatFromID(ci.category()));
		if(ci instanceof CoarseLexicalCategoryChartItem) {
			this.lexicalCategoryCounts.addCount(grammar.getCatFromID(ci.category()));
			int realWordID = grammar.getWordOrPOSID(sen.getTokens()[ci.X()]);
			Rule prodRule = grammar.getProductionRule(ci.category(), realWordID);
			this.ruleCounts.addCount(prodRule);
			String word = sen.getTokens()[ci.X()].getWord();
			String cat = grammar.getCatFromID(ci.category());
			if(!this.wordToLexicalCategoryCounts.containsKey(word)) {
				this.wordToLexicalCategoryCounts.put(word, new FrequencyList<String>());
			}
			if(!this.lexicalCategoryToWordCounts.containsKey(cat)) {
				this.lexicalCategoryToWordCounts.put(cat, new FrequencyList<String>());
			}
			this.lexicalCategoryToWordCounts.get(cat).addCount(word);
			this.wordToLexicalCategoryCounts.get(word).addCount(cat);
			return;
		}
		for(BackPointer bp : ci.children) {
			this.ruleCounts.addCount(bp.r);
			addCountsRecurse(bp.B(), sen);
			if(!bp.isUnary()) {
				addCountsRecurse(bp.C(), sen);
			}
		}
	}

	public Grammar grammar() {
		return grammar;
	}

	public FrequencyList<Rule> getRuleCounts() {
		return this.ruleCounts;
	}

	public FrequencyList<Rule> getRuleCounts(Rule_Type... types) {
		HashSet<Rule_Type> ts = new HashSet<Rule_Type>();
		for(Rule_Type t : types) {
			ts.add(t);
		}
		FrequencyList<Rule> list = new FrequencyList<Rule>();
		for(FrequencyListEntry<Rule> entry : this.ruleCounts.sortedList()) {
			if(ts.contains(entry.value().getType())) {
				list.addEntry(entry);
			}
		}
		return list;
	}

	public FrequencyList<String> getRuleCountsAsString(Rule_Type... types) {
		return this.getRuleCountsAsString(this.getRuleCounts(types));
	}
	
	private FrequencyList<String> getRuleCountsAsString(FrequencyList<Rule> ruleList) {
		FrequencyList<String> list = new FrequencyList<String>();
		for(FrequencyListEntry<Rule> entry : ruleList.sortedList()) {
			list.addEntry(new FrequencyListEntry<String>(entry.value().toString(grammar), entry.frequency()));
		}
		return list;
	}

	public FrequencyList<String> getCategoryCounts() {
		return this.categoryCounts;
	}

	public FrequencyList<String> getLexicalCategoryCounts() {
		return this.lexicalCategoryCounts;
	}

	public FrequencyList<String> getWordCounts() {
		return this.wordCounts;
	}

	public FrequencyList<String> getPOSCounts() {
		return this.posCounts;
	}

	public FrequencyList<String> getRuleCountsAsString() {
		return this.getRuleCountsAsString(this.getRuleCounts());
	}
	
	public FrequencyList<String> getWordsForLexicalCategory(String category) {
		return this.lexicalCategoryToWordCounts.get(category);
	}
	
	public FrequencyList<String> getLexicalCategoriesForWord(String word) {
		return this.wordToLexicalCategoryCounts.get(word);
	}
}
