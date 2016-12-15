package illinoisParser;

import java.util.Collection;

/**
 * A Parser has:
 * 
 *   - a configuration (an assignment of the various parsing parameters)
 *   - a grammar (including known words, nonterminal categories, and rules)
 *   - a probability model (used to choose the most-likely parse)
 *   
 * @author ramusa2
 *
 */
public class Parser {

	private Grammar grammar;

	private Model model;

	private SupervisedParsingConfig config;


	// Constructor
	public Parser(SupervisedParsingConfig c, Grammar g, Model m) {
		config = c;
		grammar = g;
		model = m;
	}

	/**
	 * Return the parser's configuration object
	 */
	public SupervisedParsingConfig getConfig() {
		return config;
	}

	/**
	 * Return the parser's grammar
	 */
	public Grammar getGrammar() {
		return grammar;
	}

	/**
	 * Return the parser's probability model
	 */
	public Model getModel() {
		return model;
	}

	/**
	 * Given a sentence, the parse method first generates a coarse parse forest from the grammar
	 * and removes chart items that are not part of a successful parse. Then, a fine parse forest
	 * over the equivalence classes defined by the probability model is produced (with an optional
	 * beam for efficiency), and the highest-scoring parse(s) are returned.
	 * @throws Exception 
	 */
	public ParseResult parse(Sentence sen, SupervisedParsingConfig config) throws Exception {
		if (sen.length() <= config.getMaxSentenceLength()) {
			try {
				Chart chart = new Chart(sen, grammar);
				chart.coarseParse(grammar, config);
				if(chart.successfulCoarseParse()) {
					chart.fineParse(model, config);
					if(chart.successfulFineParse()) {
						Tree viterbi = chart.getViterbiParse();
						if(viterbi == null) {
							Util.logln("Failed to build feature structure: "+sen);
						}
						else {
							// Dependency structure failure
							return new ParseResult(sen, viterbi, false);
						}
					}
					else {
						// Fine parse failure
						Util.logln("Failed to fine parse sentence: "+sen);
						return new ParseResult(sen, null, false);
					}
				}
				else {
					// Coarse Parse Failure
					Util.logln("Failed to coarse parse sentence: "+sen);
					checkCoarseParseFailure(chart, sen);
					return new ParseResult(sen, null, true);
				}
			}
			catch(Exception e) {
				Util.logln("Exception during parsing (not just a normal parse failure).");
			}
		}
		return new ParseResult(sen, null, true);
	}

	private void checkCoarseParseFailure(Chart chart, Sentence sen) throws Exception {
		Util.logln("Checking coarse parse failure for: "+sen.toString());
		int maxRule = grammar.Rules.size();
		Chart goldChart = CCGbankTrainer.getCoarseChartFromAutoParse(grammar, sen);
		if(goldChart == null) {
			Util.logln("Failed to check gold parse (bad CCGcat combine?): "+sen.asWords());
			return;
		}
		checkCoarseParseFailureRecurse(goldChart.coarseRoot, goldChart, chart, sen, maxRule);
	}

	private boolean checkCoarseParseFailureRecurse(CoarseChartItem goldItem,
			Chart goldChart, Chart chart, Sentence sen, int maxRule) {
		Util.logln("["+goldItem.X()+", "+goldItem.Y()+"] Checking gold item: "+grammar.getCatFromID(goldItem.category()));
		// Check if gold item is in parser's chart
		if(goldItem instanceof CoarseLexicalCategoryChartItem) {
			boolean here = itemPresent(goldItem, chart);
			if(!here) {
				Util.logln("*** Missing coarse gold lexical category in parse failure: "+grammar.getCatFromID(goldItem.category())+" -> "+sen.asWords(goldItem.X(),  goldItem.Y()));
			}
			return here;
		}
		else {
			BackPointer bp = goldItem.children.get(0);			
			// Recurse
			boolean childrenHere = this.checkCoarseParseFailureRecurse(bp.B(), goldChart, chart, sen, maxRule);
			if(bp.C() != null) {
				childrenHere = childrenHere && this.checkCoarseParseFailureRecurse(bp.C(), goldChart, chart, sen, maxRule);
			}
			boolean here = itemPresent(goldItem, chart);
			if(!here) {
				if(childrenHere) {
					Util.logln("*** Missing coarse gold item in parse failure: "+grammar.getCatFromID(goldItem.category())+" -> "+sen.asWords(goldItem.X(),  goldItem.Y()));			
					Util.logln("*** Missing rule: "+grammar.prettyRule(bp.r));
					Collection<Rule> rules;
					if(bp.C() != null) {
						rules = grammar.getRules(new IntPair(bp.B().category(), bp.C().category()));
						if(rules.isEmpty()) {
							Util.logln("    (No rules "+grammar.getCatFromID(goldItem.category())+" -> "+grammar.getCatFromID(bp.B().category())+" "+grammar.getCatFromID(bp.C().category())+"in grammar)");
						}
						else {
							Util.logln("    Rules in grammar:");
							for(Rule r : rules) {
								int ruleID = grammar.Rules.checkID(r);
								if(ruleID < maxRule) {
									Util.logln("      "+grammar.prettyRule(r));
									if(r instanceof Binary) {
										Collection<Integer> possible_c_cats = grammar.rightCats(bp.B().category());
										if(possible_c_cats == null || possible_c_cats.isEmpty()) {
											Util.logln("    ---> No possible_c_cats");
										}
										else {
											if(!possible_c_cats.contains(bp.C().category())) {
												Util.logln("    ---> possible_c_cats missing "+grammar.getCatFromID(bp.C().category()));
											}
											/*
											else if(!NF.binaryNF(r.Type, ((Binary)r).arity, bp.B().type(), bp.B().arity(),
													bp.C().type(), bp.C().arity())) {
												Util.logln("    ---> failed combine check ");
											}
											*/
										}
									}
								}
							}
						}
					}
					else {
						rules = grammar.getRules(new IntPair(bp.B().category()));
						if(rules.isEmpty()) {
							Util.logln("    (No rules "+grammar.getCatFromID(goldItem.category())+" -> "+grammar.getCatFromID(bp.B().category())+"in grammar)");
						}
						else {
							Util.logln("    Rules in grammar:");
							for(Rule r : rules) {
								int ruleID = grammar.Rules.checkID(r);
								if(ruleID < maxRule) {
									Util.logln("      "+grammar.prettyRule(r));
								}
							}
						}
					}
				}
				else {
					Util.logln("    Missing coarse gold item in parse failure, but child missing too: "+grammar.getCatFromID(goldItem.category())+" -> "+sen.asWords(goldItem.X(),  goldItem.Y()));			
				}
			}
			return here;
		}
	}

	private boolean itemPresent(CoarseChartItem item, Chart chart) {
		return chart.getCoarseCell(item.X(),  item.Y()).getCat(item) != null;
	}
}
