package util;

import illinoisParser.Binary;
import illinoisParser.CCGbankReader;
import illinoisParser.CCGbankTrainer;
import illinoisParser.Grammar;
import illinoisParser.Rule;
import illinoisParser.Rule_Type;
import illinoisParser.Sentence;
import illinoisParser.Unary;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;

import util.statistics.FrequencyList;
import util.statistics.FrequencyListEntry;
import util.statistics.GrammarStatistics;

public class ReadPrunedGrammar {
	
	/** Minimum frequency cutoff for type-changing rules 
	    (TYPE_CHANGE, FW_PUNCT_TC, BW_PUNCT_TC, FW_CONJOIN_CT, BW_CONJOIN_TC) **/
	static int tcMin = 10; 

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		readCandCGrammar();
		//readTCRestrictedGrammar();
	}
	
	private static void readCandCGrammar() throws Exception {
		int lowSec = 2;
		int highSec = 21;
		String autoDir = "data/CCGbank/AUTO/";
		Collection<Sentence> corpus = CCGbankReader.getCCGbankData(lowSec, highSec, autoDir);
		int unknownWordMin = 0;
		Grammar grammar = CCGbankTrainer.readGrammar(corpus, unknownWordMin);
		
		grammar.pruneRules(candcRulesToRemove(corpus));
		
		File saveDir = new File("grammar_candc");
		
		saveDir.mkdirs();
		grammar.save(saveDir);	
	}

	private static HashSet<Rule> candcRulesToRemove(Collection<Sentence> corpus) {
		HashSet<Rule> disallowed = new HashSet<Rule>();
		GrammarStatistics stats = GrammarStatistics.readStatistics(corpus);
		FrequencyList<Rule> rules = stats.getRuleCounts();	
		for(FrequencyListEntry<Rule> entry : rules.sortedList()) {
			Rule r = entry.value();
			double f = entry.frequency();
			
			if(!retainCandCRule(stats, r, f)) {
				disallowed.add(entry.value());
			}
		}		
		return disallowed;
	}

	private static boolean retainCandCRule(GrammarStatistics stats, Rule r, double f) {
		Grammar grammar = stats.grammar();
		Rule_Type type = r.getType();
		String parent = grammar.getCatFromID(r.A);
		
		// Unary rules
		if(r instanceof Unary) {
			if(type == Rule_Type.PRODUCTION) {
				return true; // Don't prune any lexical rules; supertagger will take care of this anyway
			}
			String child = grammar.getCatFromID(r.B);
			if(type == Rule_Type.FW_TYPERAISE) {
				if(child.equals("NP")) {
					return 		parent.equals("NP/(NP\\NP)")
							|| 	parent.equals("S/(S\\NP)")
							|| 	parent.equals("S/(S/NP)");
				}
				return false;
			}
			if(type == Rule_Type.BW_TYPERAISE) {
				if(child.equals("NP")) {
					return 		parent.equals("(S\\NP)\\((S\\NP)/NP)")
							|| 	parent.equals("((S\\NP)/NP)\\(((S\\NP)/NP)/NP)") // May be missing from CCGbank
							|| 	parent.equals("((S\\NP)/(S[to]\\NP))\\(((S\\NP)/(S[to]\\NP))/NP)")
							|| 	parent.equals("((S\\PP)/NP)\\(((S\\NP)/PP)/NP)")
							|| 	parent.equals("((S\\NP)/(S[adj]\\NP))\\(((S\\NP)/(S[adj]\\NP))/NP)");
				}
				else if(child.equals("PP")) {
					return parent.equals("(S\\NP)\\((S\\NP)/PP)");
				}
				else if(child.equals("S[adj]\\NP")) {
					return parent.equals("(S\\NP)\\((S\\NP)/(S[adj]\\NP))");
				}
				return false;
			}
			if(type == Rule_Type.TYPE_CHANGE) {
				if(parent.equals("NP")) {
					return 		child.equals("N")
							||	child.equals("S[ng]\\NP");
				}
				else if(parent.equals("NP\\NP")) {
					return 		child.equals("S[dcl]\\NP")
							||	child.equals("S[pss]\\NP")
							||	child.equals("S[ng]\\NP")
							||	child.equals("S[adj]\\NP")
							||	child.equals("S[to]\\NP")
							||	child.equals("(S[to]\\NP)/NP")
							||	child.equals("S[dcl]/NP")
							||	child.equals("S[dcl]");
				}
				else if(parent.equals("(S\\NP)\\(S\\NP)")) {
					return 		child.equals("S[pss]\\NP")
							||	child.equals("S[ng]\\NP")
							||	child.equals("S[adj]\\NP")
							||	child.equals("S[to]\\NP");
				}
				else if(parent.equals("(S\\NP)/(S\\NP)")) {
					return 		child.equals("S[ng]\\NP");
				}
				else if(parent.equals("S/S")) {
					return 		child.equals("S[pss]\\NP")
							||	child.equals("S[ng]\\NP")
							||	child.equals("S[adj]\\NP")
							||	child.equals("S[to]\\NP");
				}
				else if(parent.equals("S\\S")) {
					return 		child.equals("S[ng]\\NP")
							||	child.equals("S[dcl]");
				}
				if(parent.equals("N\\N")) {
					return 		child.equals("S[to]\\NP");
				}
				return false;
			}
		}
		else {
			String leftChild = grammar.getCatFromID(r.B);
			String rightChild = grammar.getCatFromID(((Binary) r).C);
			if(type == Rule_Type.FW_PUNCT) {
				if(leftChild.equals(",")) {
					return 		rightChild.equals("N")
							||	rightChild.equals("NP")
							||	rightChild.equals("N/N")
							||	rightChild.equals("NP\\NP")
							||	rightChild.equals("PP\\PP")
							||	rightChild.equals("S/S")
							||	rightChild.equals("S\\S")
							
							||	rightChild.equals("S[dcl]\\S[dcl]") // Ryan: 	added for sentences like: " ... " , she said .
																	// 			(this rule occurs 3132 times in CCGbank 2-21)
							
							||	rightChild.equals("(S\\NP)\\(S\\NP)")
							||	rightChild.equals("(S\\NP)/(S\\NP)")
							||	rightChild.equals("((S\\NP)\\(S\\NP))\\((S\\NP)\\(S\\NP))")
							||	(rightChild.equals("S\\NP") || rightChild.matches("S\\[[a-z]*\\]\\\\NP"))
							||	(rightChild.equals("S") || rightChild.matches("S\\[[a-z]*\\]"));
				}
				else if(leftChild.equals(":") || leftChild.equals(";")) {
					return 		rightChild.equals("N")
							||	rightChild.equals("NP")
							||	rightChild.equals("S[dcl]")
							||	rightChild.equals("NP\\NP")
							||	(rightChild.equals("S\\NP") || rightChild.matches("S\\[[a-z]*\\]\\\\NP"))
							||	rightChild.equals("(S\\NP)\\(S\\NP)");
				}
				else if(leftChild.equals("LRB") || leftChild.equals("(")) {
					return 		rightChild.equals("N")
							||	rightChild.equals("NP")
							||	rightChild.equals("S[dcl]")
							||	rightChild.equals("NP\\NP")
							||	rightChild.equals("(S\\NP)\\(S\\NP)");
				}
			}
			else if(type == Rule_Type.BW_PUNCT) {
				if(rightChild.equals(",")) {
					return 		leftChild.equals("N")
							||	leftChild.equals("NP")
							||	leftChild.equals("PP")
							||	leftChild.equals("S[dcl]")
							||	leftChild.equals("N/N")
							||	leftChild.equals("NP\\NP")
							||	leftChild.equals("S/S")
							||	leftChild.equals("S\\S") 	// May be missing from CCGbank
							||	(leftChild.equals("S\\NP") || leftChild.matches("S\\[[a-z]*\\]\\\\NP"))
							
							//|| 	leftChild.equals("(S[dcl]\\NP)/S")	// Removed from Appendix A, replaced with below
							|| 	leftChild.equals("(S[dcl]\\NP)/S[dcl]")	// Ryan: rule occurs 176 times in 2-21, see above
							|| 	leftChild.equals("(S[dcl]\\NP)/S[em]")	// Ryan: rule occurs 66 times in 2-21, see above
							
							|| 	leftChild.equals("(S[dcl]\\S[dcl])\\NP")
							|| 	leftChild.equals("(S[dcl]\\NP)/NP")
							|| 	leftChild.equals("(S[dcl]\\NP)/PP")
							|| 	leftChild.equals("(NP\\NP)/(S[dcl]\\NP)")
							||	leftChild.equals("(S\\NP)\\(S\\NP)")
							||	leftChild.equals("(S\\NP)/(S\\NP)");
				}
				else if(rightChild.equals(":") || rightChild.equals(";")) {
					return 		leftChild.equals("N")
							||	leftChild.equals("NP")
							||	leftChild.equals("PP")
							||	leftChild.equals("S[dcl]")
							||	leftChild.equals("NP\\NP")
							||	leftChild.equals("S/S")
							||	(leftChild.equals("S\\NP") || leftChild.matches("S\\[[a-z]*\\]\\\\NP"))
							|| 	leftChild.equals("(S[dcl]\\NP)/S[dcl]")
							||	leftChild.equals("(S\\NP)\\(S\\NP)")
							||	leftChild.equals("(S\\NP)/(S\\NP)");
				}
				else if(rightChild.equals(".")) {
					return 		leftChild.equals("N")
							||	leftChild.equals("NP")
							||	(leftChild.equals("S") || leftChild.matches("S\\[[a-z]*\\]"))
							||	leftChild.equals("PP")
							||	leftChild.equals("NP\\NP")
							
							||	leftChild.equals("S\\S")							
							||	leftChild.equals("S[dcl]\\S[dcl]") 	// Ryan: 	added for sentences like: " ... " , she said .
																	// 			(this rule occurs 1421 times in CCGbank 2-21)
							
							||	(leftChild.equals("S\\NP") || leftChild.matches("S\\[[a-z]*\\]\\\\NP"))
							||	(leftChild.equals("S\\PP") || leftChild.matches("S\\[[a-z]*\\]\\\\PP"))
							||	(leftChild.equals("(S[dcl]\\S)\\NP") || leftChild.matches("(S[dcl]\\S\\[[a-z]*\\])\\\\NP"))
							||	leftChild.equals("(S\\NP)\\(S\\NP)");
				}
				else if(rightChild.equals("RRB") || rightChild.equals(")")) {
					return 		leftChild.equals("N")
							||	leftChild.equals("NP")
							||	leftChild.equals("S[dcl]")
							||	leftChild.equals("N\\N")
							||	leftChild.equals("N/N")
							||	leftChild.equals("NP\\NP")
							||	leftChild.equals("S[dcl]\\NP")
							||	leftChild.equals("S/S")
							||	leftChild.equals("S\\S")
							||	leftChild.equals("(N/N)/(N/N)")
							||	leftChild.equals("(S\\NP)\\(S\\NP)")
							||	leftChild.equals("(S\\NP)/(S\\NP)");
				}
			}
			else if(type == Rule_Type.FW_PUNCT_TC) {
				return 		leftChild.equals(",") && rightChild.equals("NP") && parent.equals("(S\\NP)\\(S\\NP)");
			}
			else if(type == Rule_Type.BW_PUNCT_TC) {
				return 		leftChild.equals("NP") && rightChild.equals(",") && parent.equals("S/S")
						||	leftChild.equals("S[dcl]/S[dcl]") && rightChild.equals(",") && parent.equals("S/S")
						||	leftChild.equals("S[dcl]\\S[dcl]") && rightChild.equals(",") && parent.equals("S/S")
						||	leftChild.equals("S[dcl]/S[dcl]") && rightChild.equals(",") && parent.equals("S\\S")
						||	leftChild.equals("S[dcl]/S[dcl]") && rightChild.equals(",") && parent.equals("(S\\NP)\\(S\\NP)")
						||	leftChild.equals("S[dcl]/S[dcl]") && rightChild.equals(",") && parent.equals("(S\\NP)/(S\\NP)");
			}
			else if(type == Rule_Type.FW_CONJOIN) {
				if(leftChild.equals(",")) {
					return 		rightChild.equals("N")
							||	rightChild.equals("NP")
							||	(rightChild.equals("S") || rightChild.matches("S\\[[a-z]*\\]"))
							||	rightChild.equals("N/N")
							||	rightChild.equals("NP\\NP")
							||	(rightChild.equals("S\\NP") || rightChild.matches("S\\[[a-z]*\\]\\\\NP"))
							||	rightChild.equals("(S\\NP)\\(S\\NP)")
							
							|| rightChild.equals("NP[conj]")			// 1740 occurrences in 2-21 (see "Other Rules" section of APpendix A in C&C 2007)
							|| rightChild.equals("S[dcl][conj]");		// 1409 occurrences in 2-21 (ditto)
							//|| rightChild.equals("S[dcl]\\NP[conj]");	//  491 occurrences in 2-21 (omitted from Appendix A)
				}
				else if(leftChild.equals(";")) {
					return 		rightChild.equals("NP")
							||	(rightChild.equals("S") || rightChild.matches("S\\[[a-z]*\\]"))
							||	(rightChild.equals("S\\NP") || rightChild.matches("S\\[[a-z]*\\]\\\\NP"));
				}
				return true;
			}
		}
		if(			type == Rule_Type.BW_CONJOIN
				||	type == Rule_Type.FW_CONJOIN_TC
				||	type == Rule_Type.FW_CONJOIN_TR
				||	type == Rule_Type.FW_SUBSTITUTION
				||	type == Rule_Type.BW_SUBSTITUTION
				) {
			return false;
		}
		// If rule type otherwise unfiltered, return true
		return true;
	}

	@SuppressWarnings("unused")
	private static void readTCRestrictedGrammar() throws Exception {
		int lowSec = 2;
		int highSec = 21;
		String autoDir = "data/CCGbank/AUTO/";
		Collection<Sentence> corpus = CCGbankReader.getCCGbankData(lowSec, highSec, autoDir);
		int unknownWordMin = 0;
		Grammar grammar = CCGbankTrainer.readGrammar(corpus, unknownWordMin);
		
		grammar.pruneRules(tcRulesToRemove(corpus));
		File saveDir = new File("grammar_restricted_tc");
		
		//File saveDir = new File("grammar");
		
		saveDir.mkdirs();
		grammar.save(saveDir);		
	}

	private static HashSet<Rule> tcRulesToRemove(Collection<Sentence> corpus) {
		HashSet<Rule> disallowed = new HashSet<Rule>();
		GrammarStatistics stats = GrammarStatistics.readStatistics(corpus);
		FrequencyList<Rule> rules = stats.getRuleCounts();
		for(FrequencyListEntry<Rule> entry : rules.sortedList()) {
			Rule r = entry.value();
			double f = entry.frequency();
			Rule_Type t = r.getType();
			if(		((t == Rule_Type.BW_PUNCT_TC 
					|| t == Rule_Type.BW_CONJOIN_TC
					|| t == Rule_Type.FW_PUNCT_TC
					|| t == Rule_Type.FW_CONJOIN_TC) && f < tcMin) // Binary typechange
				||	(t == Rule_Type.TYPE_CHANGE && f < tcMin) // Unary typechange
				|| 	t == Rule_Type.BW_CONJOIN) {
				disallowed.add(entry.value());
			}
		}		
		return disallowed;
	}

}
