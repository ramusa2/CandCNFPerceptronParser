package illinoisParser;

import java.util.ArrayList;

/**
 * A ParseResult stores the best parse(s) for a sentence; it's intended to be a wrapper class for the parse tree, 
 * the dependency structure(s), the sentence itself, and any other information we'd like to maintain.
 * 
 * @author ramusa2
 *
 */
public class ParseResult {
	
	private Tree viterbiParse;
	private Sentence sen;
	private boolean coarseFailure;
	
	public ParseResult(Sentence sentence, Tree viterbi, boolean coarseParseFailure) {
		sen = sentence;
		viterbiParse = viterbi;
		coarseFailure = coarseParseFailure;
	}	

	public boolean isParseFailure() {
		return isCoarseParseFailure() || isFineParseFailure();
	}

	public boolean isCoarseParseFailure() {
		return coarseFailure;
	}

	public boolean isFineParseFailure() {
		return !isCoarseParseFailure() && viterbiParse == null;
	}
	
	public Tree getViterbiParse() {
		return viterbiParse;
	}
	
	public Sentence getSentence() {
		return sen;
	}
	
	public double getViterbiProbability() {
		if(this.viterbiParse != null) {
			return this.viterbiParse.prob;
		}
		return Double.NEGATIVE_INFINITY;
	}
	
	public String viterbiCCGDependencies() {
		if (this.viterbiParse == null) {
			return "<s> 0\n<\\s>\n";
		}
		DepRel[][] depr = dpCCGRecurse(this.viterbiParse);
		return printDepRel(depr) + "\n";
	}

	private DepRel[][] dpCCGRecurse(Tree a) {
		a.ccgDepRel = new DepRel[sen.length()][sen.length()];
		if (a.C == null && a.B != null) {
			SemanticTuple.copyDepRel(a.ccgDepRel, dpCCGRecurse(a.B));
		} else if (a.C != null && a.B != null) {
			// Left Traverse
			SemanticTuple.copyDepRel(a.ccgDepRel, dpCCGRecurse(a.B));
			// Right Traverse
			SemanticTuple.copyDepRel(a.ccgDepRel, dpCCGRecurse(a.C));
		}

		if (a.ccgcat != null) {
			DepList filled = a.ccgcat.filled_ccgDependencies();
			if (filled != null) {
				while (filled != null) {
					SemanticTuple.addDependency(a.ccgDepRel, filled);
					filled = filled.next();
				}
			}
		}
		return a.ccgDepRel;
	}
	
	private String printDepRel(DepRel[][] depRel) {
		int count = 0;
		String[] docStrip = new String[sen.length()];
		for (int i = 0; i < sen.length(); i++) {
			docStrip[i] = sen.get(i).getWord();
		}

		String out = "";
		if (depRel != null) {
			for (int i = 0; i < depRel.length; i++) {  // was chart.length
				for (int j = 0; j < depRel.length; j++) { // was chart.length
					if (depRel[i][j] != null) {
						if (j == depRel.length - 1) {} else { // was chart.length
							if (depRel[i][j].extracted) {
								if (depRel[i][j].bounded) {
									// arg, head, cat, slot, argWord, headWord
									out += i + " \t " + j + " \t " + depRel[i][j].cat
											+ " \t " + depRel[i][j].slot + " \t " + docStrip[i]
													+ " " + docStrip[j] + " <XB>" + "\n";
									count += 1;
								}
								if (!(depRel[i][j].bounded)) {
									// arg, head, cat, slot, argWord, headWord
									out += i + " \t " + j + " \t " + depRel[i][j].cat
											+ " \t " + depRel[i][j].slot + " \t " + docStrip[i]
													+ " " + docStrip[j] + " <XU>" + "\n";
									count += 1;
								}
							} else {
								out += i + " \t " + j + " \t " + depRel[i][j].cat
										+ " \t " + depRel[i][j].slot
										+ " \t " + docStrip[i] + " " + docStrip[j] + "\n";
								count += 1;
							}
						}
					}
				}
			}
			out = "<s> " + count + "\n" + out + "<\\s>";
		} else {
			out += "depRel is null: ";// + catString());
		}
		return out;
	}


	public String getLaTeXString(Grammar grammar, boolean isChinese) {
		return this.buildLaTeXString(grammar, isChinese);
	}

	private String buildLaTeXString(Grammar grammar, boolean isChinese) {
		ArrayList<ArrayList<ArrayList<String>>> viterbiParseList = new ArrayList<ArrayList<ArrayList<String>>>();
		for (int i = 0; i < sen.length(); i++) {
			viterbiParseList.add(new ArrayList<ArrayList<String>>());
			for (int j = 0; j < sen.length(); j++) {
				viterbiParseList.get(i).add(new ArrayList<String>());
			}
		}
		this.buildTeXCells(viterbiParseList, grammar, viterbiParse);
		String s = buildTeX(viterbiParseList, sen, grammar, isChinese);	
		return s;
	}
	

	/**
	 * Recursively fills a "set of cells with strings for TeX"
	 */
	public void buildTeXCells(ArrayList<ArrayList<ArrayList<String>>> viterbiParse,
			Grammar grammar, Tree tree) {
		if (tree.B!= null) {
			buildTeXCells(viterbiParse, grammar, tree.B);
			if (tree.C != null) {
				buildTeXCells(viterbiParse, grammar, tree.C);
			}
		}
		viterbiParse.get(tree.getX()).get(tree.getY()).add(tree.rule.getType().toString());
		viterbiParse.get(tree.getX()).get(tree.getY()).add(
				grammar.prettyCat(tree.A).replaceAll("\\\\\\.","\\\\").replaceAll("/\\.","/").replaceAll("\\\\", "\\\\bs "));
	}

	/**
	 * Builds a TeX Beamer slide with the parse.  Information is extracted from viterbiParse which has
	 * category span information
	 * @param viterbiParse Category span information
	 * @return A TeX Beamer slide
	 */
	public static String buildTeX(ArrayList<ArrayList<ArrayList<String>>> viterbiParse, 
			Sentence sentence, Grammar grammar, boolean isChinese) {
		String TeXParse = "\\begin{frame}\\centering\n" +
				"\\adjustbox{max height=\\dimexpr\\textheight-5.5cm\\relax,\n" +
				"           max width=\\textwidth}{\n";
		// Start at len , 0
		//TeXParse += "viterbi: " + BestTree.prob + "\n";
		TeXParse += "\\deriv{" + sentence.length() + "}{\n";

		if (isChinese) {
			TeXParse += "\\text{\\chinese ";
		} else {
			TeXParse += "{\\rm ";
		}
		TeXParse += Util.escape_chars(sentence.get(0).getWord());

		if (isChinese) {
			TeXParse += "\\stopchinese}";
		} else {
			TeXParse += "}";
		}
		for (int i = 1; i < sentence.length(); i++) {
			if (isChinese) {
				TeXParse += "& \\text{\\chinese ";
			} else {
				TeXParse += "& {\\rm ";
			}
			//TeXParse += Util.escape_chars(sentence.get(0).getWord());
			// TODO: add an asterisk if this word is UNK
			TeXParse += Util.escape_chars(sentence.get(i).getWord());

			if (isChinese) {
				TeXParse += "\\stopchinese}";
			} else {
				TeXParse += "}";
			}
		}
		TeXParse += "\\\\\n";

		TeXParse += "\\uline{1}";
		for (int i = 1; i < sentence.length(); i++) {
			TeXParse += "& \\uline{1}";
		}
		TeXParse += "\\\\\n";

		boolean repeat = false;
		for (int s = 0; s < sentence.length(); s++) {
			int extra = 0;
			for (int i = 0; i < sentence.length() - s; i++) {
				ArrayList<String> strings = viterbiParse.get(i).get(i + s);
				Rule_Type type;
				String cat;
				if (!strings.isEmpty()) {
					if (strings.size() % 2 == 1) {
						cat = Util.escape_chars(strings.remove(0));//
						TeXParse += "\\mc{" + (s + 1) + "}{\\it " + cat + "}";
						extra = s;
					} else {
						type = Rule_Type.valueOf(strings.remove(0));
						TeXParse += Type(type, s + 1);
						if (type.equals(Rule_Type.FW_CONJOIN)) {
							strings.remove(0);
						} else {
							extra = s;
						}
					}
					if (!strings.isEmpty()) {
						repeat = true;
					}
				}
				if (i == sentence.length() - s - 1) {
					TeXParse += "\\\\\n";
				} else if (extra == 0) {
					TeXParse += "\t&";
				} else {
					extra -= 1;
				}
			}
			if (repeat) {
				s -= 1;
				repeat = false;
			}
		}
		TeXParse += "}";
		return TeXParse + "}\n\\end{frame}\n";
	}


	private static String Type(Rule_Type type, int s) {
		switch (type) {
		case FW_APPLY:
			return "\\fapply{" + s + "}";
		case FW_COMPOSE:
		case FW_2_COMPOSE:
		case FW_3_COMPOSE:
			return "\\fcomp{" + s + "}";
		case FW_XCOMPOSE:
			return "\\fxcomp{" + s + "}";
		case BW_APPLY:
			return "\\bapply{" + s + "}";
		case BW_COMPOSE:
		case BW_2_COMPOSE:
		case BW_3_COMPOSE:
			return "\\bcomp{" + s + "}";
		case BW_XCOMPOSE:
			return "\\bxcomp{" + s + "}";
		case TYPE_TOP:
			return "\\comb{" + s + "}{TOP}";
		case FW_PUNCT:
			return "\\comb{" + s + "}{> punc}";
		case BW_PUNCT:
			return "\\comb{" + s + "}{< punc}";
		case FW_CONJOIN:
			return "";
		case BW_CONJOIN:
			return "\\conj{" + s + "}";
		case FW_TYPERAISE:
			return "\\ftype{" + s + "}";
		case BW_TYPERAISE:
			return "\\btype{" + s + "}";
		case TYPE_CHANGE:
			/*
	    case FW_TYPECHANGE:
	    case BW_TYPECHANGE:
	      return "\\comb{" + s + "}{TC}";
			 */
		default:
			return "";
		}
	}
}
