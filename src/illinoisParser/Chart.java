package illinoisParser;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

import perceptron.parser.PerceptronParser;

import supertagger.LexicalCategoryEntry;
import supertagger.SupertagAssignment;

/**
 * @author bisk1 A CYK Parse chart
 */
public strictfp class Chart implements Externalizable {
	private static final long serialVersionUID = 11122010;

	/**
	 * The grammar with chart-parsing rules (also decodes this chart's word/symbol IDs)
	 */
	public Grammar grammar;

	/**
	 * Actual 2-D Array of Cells
	 */
	Cell[][] chart;
	/**
	 * A 2-D array of Cells for the fine chart
	 */
	Cell[][] fine;
	/**
	 * Underlying sentence
	 */
	Sentence sentence;
	CoarseChartItem coarseRoot;
	FineChartItem fineRoot;
	/**
	 * Number of parses in the forest
	 */
	double parses = 0;
	/**
	 * Fast access to underlying tag sequence
	 */
	public int[] tags;
	/**
	 * Fast access to underlying word sequence
	 */
	public int[] words;
	/**
	 * Specifies which constituents are valid (allowed to be filled), as a
	 * function of punctuation
	 */
	boolean[][] disallowed_constituents;
	/**
	 * The length of the underlying sentence/dimensions of the chart
	 */
	int length;
	/**
	 * Reference to the model
	 */
	//protected Model model;
	/**
	 * Unique id
	 */
	int id; //TODO(bisk1): Fill


	/**
	 * Default constructor
	 */
	public Chart() {}

	public Chart(Sentence base_sentence, Grammar g) {
		this.grammar = g;
		this.id = base_sentence.getID();
		sentence = base_sentence;
		if (!Configuration.ignorePunctuation) {
			this.disallowed_constituents = new boolean[getSentence().getTokens().length]
					[getSentence().getTokens().length];
			computeSpans();
		}
		// Precompute int values for tags and words
		this.tags = new int[this.getSentence().length()];
		this.words = new int[this.getSentence().length()];
		this.length = this.getSentence().length();
	}

	public Grammar getGrammar() {
		return grammar;
	}

	public void computeWordsAndTagsWithUnk(Grammar grammar) {
		for (int i = 0; i < tags.length; i++) {
			tags[i] = grammar.getPOSID(getSentence().get(i).getPOS());
			words[i] = grammar.getWordOrPOSID(this.getSentence().get(i));
		}	  
	}

	void getLex(int i) throws Exception { throw new Exception("not implemented"); }



	//@SuppressWarnings("unchecked")
	@Override
	public void readExternal(ObjectInput in) throws IOException,
	ClassNotFoundException {
		// TODO: must update grammar
		sentence = (Sentence) in.readObject();
		tags = (int[]) in.readObject();
		words = (int[]) in.readObject();
		parses = in.readDouble();

		// chart = (Cell[][]) in.readObject();
		// see writeExternal, below

		coarseRoot = (CoarseChartItem) in.readObject();
		if (chart != null) {
			coarseRoot.cell = chart[0][chart.length - 1];
		}
		fineRoot = (FineChartItem) in.readObject();
		id = in.readInt();
		if (!Configuration.ignorePunctuation) {
			disallowed_constituents = (boolean[][]) in.readObject();
		}
		// TODO: don't try to reconstruct chart, we don't need this.
		reconstructCharts();
	}

	private void reconstructCharts() {
		int l = sentence.length();
		chart = new Cell[l][l];
		// Fill chart array with empty cells
		for(int i=0; i<l; i++) {
			for(int s=0; s<l-i; s++) {
				chart[i][i+s] = new Cell(this, i, i+s);
			}
		}
		// TODO: We may not need to reconstruct chart
		// Recurse on coarse top
		if(this.coarseRoot != null) {
			addItemsToCellsRecurse(this.coarseRoot);
		}
	}

	private void addItemsToCellsRecurse(CoarseChartItem item) {
		if(item.cell != null) {
			return;
		}
		int X = item.X();
		int Y = item.Y();
		Cell cell = chart[X][Y];
		if(item.setCell(cell)) { // true iff we added item to cell this time
			for(BackPointer bp : item.children) {
				addItemsToCellsRecurse(bp.B);
				if(bp.C != null) {
					addItemsToCellsRecurse(bp.C);
				}
			}
		}
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		// TODO: do we need to store grammar?
		out.writeObject(getSentence());
		out.writeObject(tags);
		out.writeObject(words);
		out.writeDouble(parses);


		//out.writeObject(chart);
		// Note: serializing the chart array is *ugly*
		// 
		// (i.e., attempting to determine the contiguous block
		// for something as nested/big as the parse chart
		// is extremely painful for the serializer)


		out.writeObject(coarseRoot);
		out.writeObject(fineRoot);
		out.writeInt(id);
		if (!Configuration.ignorePunctuation) {
			out.writeObject(disallowed_constituents);
		}
	}



	public final void cleanChart() {
		if(this.coarseRoot != null) {
			this.markUsed(this.coarseRoot);
		}
		for (int s = 0; s < chart.length; s++) {
			for (int i = 0; i < chart.length - s; i++) {
				chart[i][i + s].removeUnusedCats();
			}
		}
	}

	String CCGdependencies() {
		if (!Configuration.CCGcat_CCG) {
			return "";
		}
		if (!this.successfulFineParse()) {
			return "<s> " + this.length + "\n<\\s>\n";
		}
		Tree BestTree = this.fineRoot.getViterbiTree(this);
		DepRel[][] depr = dpCCGRecurse(BestTree);
		return printDepRel(depr) + "\n";
	}

	String CCGDependencies(Tree t) {
		if (!this.successfulFineParse()) {
			return "<s> " + this.length + "\n<\\s>\n";
		}
		DepRel[][] depr = dpCCGRecurse(t);
		return printDepRel(depr) + "\n";
	}

	DepRel[][] dpCCGRecurse(Tree a) {
		a.ccgDepRel = new DepRel[chart.length][chart.length];
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

	private final int offset(int i) {
		if (!Configuration.ignorePunctuation) {
			return i;
		}
		int seen = -1;
		int j = 0;
		for (; j < getSentence().getTokens().length; j++) {
			if (!TAGSET.Punct(getSentence().getTokens()[j].getPOS())) {
				seen += 1;
			}
			if (seen == i) {
				return j;
			}
		}
		return j;
	}

	private final String insertPunctuation(int i) {
		String val = "";
		if (Configuration.ignorePunctuation) {
			if (i == -1) {
				for (int j = 0; j < getSentence().getTokens().length; j++) {
					String word = getSentence().get(j).getWord();
					String pos = getSentence().get(j).getPOS().toString();

					if (TAGSET.Punct(getSentence().getTokens()[j].getPOS())) {
						val += (j+1) + "\t" + word + "\t" + pos + "\t_\t" + (j+1) + "\tPUNC\n";
					} else {
						return val;
					}
				}
			} else if (offset(i) + 1 != offset(i + 1)) {
				int max = offset(i + 1);
				for (int j = offset(i) + 1; j < max; j++) {
					String word = getSentence().get(j).getWord();
					String pos = getSentence().get(j).getPOS().toString();
					val += (j+1) + "\t" + word + "\t" + pos + "\t_\t" + (j+1) + "\tPUNC\n";
				}
			}
		}
		return val;
	}

	private String printDepRel(DepRel[][] depRel) {
		int count = 0;
		String[] docStrip = new String[getSentence().length()];
		for (int i = 0; i < getSentence().length(); i++) {
			docStrip[i] = getSentence().get(i).getWord();
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

	String featureStructure(Tree BestTree) throws Exception {
		String out = "";
		//Tree BestTree = TOP().topK[0];

		fsRecurse(BestTree);
		if (Configuration.DEBUG) {
			out = BestTree.ccgcat.print();
		}

		CCGcat.resetCounters();
		return out;
	}

	public void fsRecurse(Tree A) throws Exception {
		if(A.B == null) {
			if(A.type == Rule_Type.PRODUCTION) {
				A.ccgcat = getCCGbankProdCat(A);
				return;
			}
		}
		else {
			if (A.C == null) {
				fsRecurse(A.B);
				switch (A.type) {
				case TYPE_CHANGE:
				case TYPE_TOP:
				case TO_TYPE_TOP:
					A.ccgcat = CCGcat.typeChangingRule(A.B.ccgcat, getCat(A.A));
					if (A.B.ccgcat.heads() != null) {
						A.ccgcat.setHeads(A.B.ccgcat.heads());
					}
					break;
				case TO_PRODUCTION:
					A.ccgcat = A.B.ccgcat.copy();
					A.ccgcat.setHeads(A.B.ccgcat.heads());
					break;
				case FW_TYPERAISE:
				case BW_TYPERAISE:
					A.ccgcat = CCGcat.typeRaiseTo(A.B.ccgcat, getCat(A.A));
					if(A.B.ccgcat.heads() == null) {
						System.out.println(grammar.getCatFromID(A.A)+" -> "+grammar.getCatFromID(A.B.A));
						//fsRecurse(A.B);
					}
					else {
						A.ccgcat.setHeads(A.B.ccgcat.heads());
					}
					break;
				default:
					throw new Exception("Chart -- Can't handle: " + A.type);
				}
				if (A.ccgcat == null) {
					throw new Exception("Null Unary CCGcat");
				}
			} else {
				fsRecurse(A.B);
				fsRecurse(A.C);
				if (A.B.ccgcat == null) {
					throw new Exception("B's CCGcat is null: "
							+ grammar.prettyCat(A.B.A));
				}
				if (A.C.ccgcat == null) {
					throw new Exception("C's CCGcat is null: "
							+ grammar.prettyCat(A.C.A));
				}
				switch (A.type) {
				case FW_APPLY:
					A.ccgcat = CCGcat.apply(A.B.ccgcat, A.C.ccgcat);
					break;
				case FW_COMPOSE:
				case FW_2_COMPOSE:
				case FW_3_COMPOSE:
				case FW_XCOMPOSE:
					A.ccgcat = CCGcat.compose(A.B.ccgcat, A.C.ccgcat);
					break;
				case FW_PUNCT:
					A.ccgcat = CCGcat.punctuation(A.C.ccgcat, A.B.ccgcat);
					break;
				case COORDINATION:
					A.ccgcat = CCGcat.coordinate(A.B.ccgcat, A.C.ccgcat);
					break;
				case FW_CONJOIN_TR:
				case FW_CONJOIN:
					A.ccgcat = CCGcat.conjunction(A.C.ccgcat, A.B.ccgcat);
					break;
				case BW_APPLY:
					A.ccgcat = CCGcat.apply(A.C.ccgcat, A.B.ccgcat);
					break;
				case BW_COMPOSE:
				case BW_2_COMPOSE:
				case BW_3_COMPOSE:
				case BW_XCOMPOSE:
					A.ccgcat = CCGcat.compose(A.C.ccgcat, A.B.ccgcat);
					break;
				case BW_PUNCT:
					A.ccgcat = CCGcat.punctuation(A.B.ccgcat, A.C.ccgcat);
					break;
				case BW_CONJOIN_TR:
				case BW_CONJOIN:
					A.ccgcat = CCGcat.coordinate(A.B.ccgcat, A.C.ccgcat);
					break;
				case BW_PUNCT_TC:
				case BW_CONJOIN_TC:
					A.ccgcat = CCGcat.typeChangingRule(A.B.ccgcat, getCat(A.A));
					break;
				case FW_PUNCT_TC:
				case FW_CONJOIN_TC:
					A.ccgcat = CCGcat.typeChangingRule(A.C.ccgcat, getCat(A.A));
					break;
				case BW_SUBSTITUTION:
					A.ccgcat = CCGcat.substitute(A.B.ccgcat, A.C.ccgcat);
					break;
				case FW_SUBSTITUTION:
					A.ccgcat = CCGcat.substitute(A.C.ccgcat, A.B.ccgcat);
					break;
				default:
					throw new Exception("Chart -- Can't handle: " + A.type);
				}
				if (A.ccgcat == null) {
					Util.SimpleError(A.type);
					Util.SimpleError(getCat(A.A) + "\t->\t" + getCat(A.B.A)
							+ "\t" + getCat(A.C.A));
					Util.SimpleError(A.toString(grammar, 0));
					Util.SimpleError(A.B.ccgString());
					Util.SimpleError(A.C.ccgString());
					throw new Exception("Null");
				}
				if (A.ccgcat == null) {
					throw new Exception("Null Binary CCGcat: " + A.type
							+ "\t" + A.B.ccgcat.catString()
							+ "\t" + A.C.ccgcat.catString());
				}
			}
		}
		return;
	}

	private String getCat(Integer cat) {
		return grammar.prettyCat(cat).replaceAll("\\\\\\.","\\\\")
				.replaceAll("/\\.","/");
	}

	private CCGcat getCCGbankProdCat(Tree A) {
		String word = getSentence().get(A.headIndex).getWord();
		String category = grammar.getCatFromID(A.A);
		String pos = getSentence().get(A.headIndex).getPOS().toString();
		return CCGcat.lexCat(word, category, pos, A.headIndex);
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
			return "\\comb{" + s + "}{> punc}";
		case FW_CONJOIN:
		case FW_CONJOIN_TR:
			return "\t&";
		case BW_CONJOIN:
		case BW_CONJOIN_TR:
			return "\\conj{" + s + "}";
		case FW_TYPERAISE:
			return "\\ftype{" + s + "}";
		case BW_TYPERAISE:
			return "\\btype{" + s + "}";
		case TYPE_CHANGE:
			return "\\comb{" + s + "}{TC}";
		default:
			return "";
		}
	}


	/**
	 * Prints all chart-items in chart
	 * 
	 * @throws Exception
	 */
	void debugChart() throws Exception {
		Cell[][] local_chart = this.chart;
		if (local_chart == null) {
			// Util.log("F:  " + sentence.asTags() + "\nnull\n");
			return;
		}
		Util.log("S:  " + parses + "\t" + getSentence().toString() + "\n");
		Util.logln("");
		for (int s = 0; s < local_chart.length; s++) {
			Cell A = local_chart[s][s];
			Util.log(s + "," + s);
			Util.log("\tCats:\n");
			for (CoarseChartItem cat : A.values()) {
				cat.logEquivClass();
			}
			Util.log("\n");
		}

		for (int s = 1; s < local_chart.length; s++) {
			for (int i = 0; i < local_chart.length - s; i++) {
				Cell A = local_chart[i][i + s];
				Util.log(i + "," + (i + s) + "\n");
				if (A != null) {
					for (CoarseChartItem cat : A.values()) {
						cat.logEquivClass();
					}
				}
				Util.log("\n");
			}
		}
		Util.log("\n");
	}
	//
	private final ArrayList<ArrayList<Integer>> token_indices
	= new ArrayList<ArrayList<Integer>>();

	/**
	 * Original: Don't allow for crossing brackets. Formulation: [ x ] should be
	 * parsed [ x -> x followed by [ x ] -> x 1) x1 x2 ... xn is a valid
	 * constituent 2) [ x ] is a valid consituent Take 2: TOOD: What about
	 * crossing of a boundary: A `` B
	 */
	private final void computeSpans() {
		POS[] localtags = getSentence().getPOSTags();
		int slength = localtags.length;
		boolean[] punc = new boolean[slength];
		for (int i = 0; i < slength; ++i) {
			punc[i] = TAGSET.Punct(localtags[i]);
		}
		// TODO: resolve this?
		//    The commented constraints disallow certain cells.  They are being left
		//    here until I decide I don't actually need them.  :)

		//    int first_non_P = 0;
		//    int last_non_P = slength-1;
		//    // first non punct
		//    while(punc[first_non_P] && first_non_P < slength) { ++first_non_P; }
		//    while(punc[last_non_P] && last_non_P > 0) { --last_non_P; }
		//
		//    // Block out columns to top of punctuation at the end of the sentence
		//    for(int i = last_non_P+1; i < slength; ++i){
		//      for(int j = first_non_P+1; j < i; ++j) {
		//        disallowed_constituents[j][i] = true;
		//      }
		//    }
		//
		//    // Block out rows to top of punctuation at the beggining of the sentence
		//    for(int i = 0; i < first_non_P; ++i){
		//      for(int j = i+1; j < slength-1; ++j) {
		//        disallowed_constituents[i][j] = true;
		//      }
		//    }

		// Disallow cells on first non-lexical level that are both punc
		for(int i =0; i < slength-1; ++i){
			if(punc[i] && punc[i+1]) {
				disallowed_constituents[i][i+1] = true;
			}
		}
	}

	// TODO(bisk1): KILL
	/**
	 * Determins whether the punctuation in the span [l,r] allows for a
	 * constituent
	 * 
	 * @param l
	 * @param r
	 * @return boolean
	 */
	final boolean punctuationBracketing(int l, int r) {
		return Configuration.supervisedTraining
				|| Configuration.ignorePunctuation
				|| !disallowed_constituents[l][r];
	}

	public void coarseParse(Grammar grammar, SupervisedParsingConfig config) throws Exception {
		// Set up the chart
		this.chart = new Cell[this.getSentence().length()][this.getSentence().length()];
		this.computeWordsAndTagsWithUnk(grammar);
		this.computeSpans();

		// Run the CYK algorithm
		for (int s = 0; s < chart.length; s++) {
			for (int i = 0; i < chart.length - s; i++) {
				if (s == 0) {
					lexicalCell(i, config);
				} else {
					binaryCell(i, i + s);
				}
			}
		}
		getUnary(this.chart[0][this.getSentence().length()-1], Rule_Type.TYPE_TOP);
	}

	public void coarseParseWithSupertags(Grammar grammar, SupervisedParsingConfig config,
			SupertagAssignment allowedLexcats, boolean useGoldTagsAsWell) throws Exception {
		// Set up the chart
		this.chart = new Cell[this.getSentence().length()][this.getSentence().length()];
		this.computeWordsAndTagsWithUnk(grammar);
		//this.computeSpans();

		// Run the CYK algorithm
		for (int s = 0; s < chart.length; s++) {
			for (int i = 0; i < chart.length - s; i++) {
				if (s == 0) {
					lexicalCellWithSupertags(i, config, allowedLexcats, useGoldTagsAsWell);
				} else {
					binaryCell(i, i + s);
				}
			}
		}
		getUnary(this.chart[0][this.getSentence().length()-1], Rule_Type.TYPE_TOP);
	}

	/**
	 * Fill lexical cell of chart (i,i) with chart type specific getLex and then
	 * if appropriate try to type raise the categories.
	 * @param i
	 * @param chart
	 * @throws Exception
	 */
	protected void lexicalCell(int i, SupervisedParsingConfig config) throws Exception {
		Cell cell = new Cell(this, i);
		this.chart[i][i] = cell;
		for(Integer lexCat : grammar.getLexicalCategories(this.words[i])) {
			CoarseChartItem ci = new CoarseLexicalCategoryChartItem(cell, lexCat);
			cell.addCat(ci);
		}
		getUnary(cell, Rule_Type.TYPE_CHANGE );
		if (Configuration.typeRaising) {
			getUnary(cell, Rule_Type.FW_TYPERAISE);
			getUnary(cell, Rule_Type.BW_TYPERAISE);
		}
	}

	/**
	 * Fill lexical cell of chart (i,i) with provided lexical categories and then
	 * if appropriate try to type raise the categories.
	 * @param i
	 * @param chart
	 * @throws Exception
	 */
	protected void lexicalCellWithSupertags(int i, SupervisedParsingConfig config,
			SupertagAssignment allowedLexicalCategories, boolean useGoldTagsAsWell) throws Exception {
		Cell cell = new Cell(this, i);
		this.chart[i][i] = cell;
		if(useGoldTagsAsWell) {
			Integer lexCat = grammar.getCatID(allowedLexicalCategories.getGold(i));
			CoarseChartItem ci = new CoarseLexicalCategoryChartItem(cell, lexCat);
			cell.addCat(ci);
		}
		for(LexicalCategoryEntry supertag : allowedLexicalCategories.getAll(i)) {
			Integer lexCat = grammar.getCatID(supertag.category());
			CoarseChartItem ci = new CoarseLexicalCategoryChartItem(cell, lexCat);
			cell.addCat(ci);
		}
		getUnary(cell, Rule_Type.TYPE_CHANGE );
		if (Configuration.typeRaising) {
			getUnary(cell, Rule_Type.FW_TYPERAISE);
			getUnary(cell, Rule_Type.BW_TYPERAISE);
		}
	}

	/**
	 * Fills a binary cell (i,j)
	 * @param i
	 *    Start of span
	 * @param j
	 *    End of span
	 * @param chart
	 * @throws Exception
	 */
	protected void binaryCell(int i, int j) throws Exception {
		Cell cell = new Cell(this, i, j);
		this.chart[i][j] = cell;
		//if (this.punctuationBracketing(i, j)) {
		Cell B, C;
		for (int k = i; k <= j - 1; k++) {
			B = this.chart[i][k];
			C = this.chart[k + 1][j];
			for (Integer b_cat : B.cats()) {
				Collection<Integer> possible_c_cats = grammar.rightCats(b_cat);
				if (possible_c_cats != null) {
					for (Integer c_cat : possible_c_cats) {
						if (C.cats().contains(c_cat)) {
							grammar.combine(cell, b_cat, B.values(b_cat),
									c_cat, C.values(c_cat));
						}
					}
				}
			}
		}
		getUnary(cell, Rule_Type.TYPE_CHANGE);
		getUnary(cell, Rule_Type.FW_TYPERAISE);
		getUnary(cell, Rule_Type.BW_TYPERAISE);
		// TODO: should we allow type raising higher up in the chart?
		//if (!model.config.lexTROnly && Configuration.typeRaising) {
		//	getUnary(cell, Rule_Type.FW_TYPERAISE);
		//	getUnary(cell, Rule_Type.BW_TYPERAISE);
		//}
		//}
	}

	/**
	 * Determines if a chart has been/can be successfully completed with a TOP node
	 * @param chart
	 * @return
	 *    Whether a parse was successfully found
	 * @throws Exception
	 */
	protected boolean checkForSuccess() throws Exception {
		// Empty sentence ( e.g. a sentence of just punctuation )
		if (this.length == 0) {
			this.parses = 0;
			this.coarseRoot = null;
			return false;
		}

		// Check if successful parse
		Cell A = chart[0][this.length - 1];
		if (this.getSentence().length() > Configuration.longestTestSentence
				|| A.isEmpty()) {
			this.parses = 0;
			this.coarseRoot = null;
			return false;
		}

		getUnary(A, Rule_Type.TO_TYPE_TOP); // Level 3 TO_TOP
		getUnary(A, Rule_Type.TYPE_TOP); // Level 3 TOP
		if (this.coarseRoot == null
				|| (this.coarseRoot.children.isEmpty()
						&& (this.coarseRoot.topK == null || this.coarseRoot.topK.length == 0))) {
			this.parses = 0;
			this.coarseRoot = null;
			return false;
		}
		A.addCat(this.coarseRoot);
		this.parses += this.coarseRoot.parses;
		// Util.Println(TIMES.intValue() + "\tS: " + sentence.asTags());
		return true;
	}

	/**
	 * Attempts to apply rules of a given type to all categories in the cell
	 * @param cell
	 *    Cell to fill
	 * @param type
	 *    Type of rule to apply
	 * @throws Exception
	 */
	void getUnary(Cell cell, Rule_Type type) throws Exception {
		HashMap<CoarseChartItem, CoarseChartItem> newCats =
				new HashMap<CoarseChartItem, CoarseChartItem>();
		for (CoarseChartItem cat : cell.values()) {
			IntPair B = new IntPair(cat.category());
			for (Rule r : grammar.getRules(B)) {
				Unary u = (Unary) r;
				if (u.getType().equals(type)) {
					if (NF.unaryNF(cat.type(), u.Type)) {
						CoarseChartItem c;
						switch(Configuration.NF) {
						case None:
						case Eisner_Orig:
							c = new CoarseChartItem(u.A, cell);
							break;
						default:
							c = new CoarseChartItem(cell, u.A, u.getType(), -1);
						}
						CoarseChartItem c_prev;
						if ((c_prev = cell.getCat(c)) != null) {
							c = c_prev;
						} else if ((c_prev = newCats.get(c)) != null) {
							c = c_prev;
						} else {
							newCats.put(c, c);
						}
						if (c.addChild(u, cat, null)) {
							c.parses += cat.parses;
						}
						if (cell.X() == 0
								&& cell.Y() == (this.length - 1)
								&& type.equals(Rule_Type.TYPE_TOP)) {
							if (this.coarseRoot != null && this.coarseRoot != c) {
								Util.Error("Warning: producing multiple coarse TOP items ");
							}
							this.coarseRoot = c;
						}
					}
				}
			}
		}
		cell.addAllCats(newCats);
	}

	public Tree<? extends FineChartItem> getViterbiParse() {
		if(this.successfulFineParse()) {
			Tree<? extends FineChartItem> vit = this.fineRoot.getViterbiTree(this);
			try {
				this.fsRecurse(vit);
				return vit;
			}
			catch(Exception e) {
				e.printStackTrace();
				Util.logln("Fine parse failure; error occured while attempting to build feature structure.");
				return null;
			}
		}
		return null;
	}

	public boolean successfulCoarseParse() {
		return this.coarseRoot != null;
	}

	public boolean successfulFineParse() {
		return this.fineRoot != null  && this.fineRoot.getViterbiBP() != null;
	}

	public void fineParse(Model model, SupervisedParsingConfig config) throws Exception {
		if(this.successfulCoarseParse()) {
			// Mark which items belong to a successful parse (the rest will be ignored)
			this.markUsed(this.coarseRoot);
			this.debug(" Starting fine parsing for: "+this.getSentence().asWords());
			// Run the CYK algorithm
			for (int s = 0; s < chart.length; s++) {
				for (int i = 0; i < chart.length - s; i++) {
					Cell cell = this.getCoarseCell(i, i+s);
					this.createFineItemsForCell(cell, model);
					if(config.useBeamSearch() && s != chart.length-1) {
						this.applyBeamToCell(cell, model, config.getLogBeamWidth());
					}
				}
			}
			for(FineChartItem fine : this.coarseRoot.fineItems()) {
				if(this.fineRoot == null || this.fineRoot.getViterbiProb() < fine.getViterbiProb()) {
					this.fineRoot = fine;
				}
			}
		}
	}

	private void markUsed(CoarseChartItem ci) {
		if(ci.used) {
			return;
		}
		ci.used = true;
		for(BackPointer bp : ci.children) {
			this.markUsed(bp.B);
			if(bp.C != null) {
				this.markUsed(bp.C);
			}
		}
	}

	private void debug(String s) {
		if(this.DEBUG_CHART) {
			this.debugpw.println(s);
		}
	}

	private void createFineItemsForCell(Cell cell, Model model) {
		HashSet<CoarseChartItem> itemsProcessed = new HashSet<CoarseChartItem>();
		if(cell.X() == cell.Y()) {
			this.debug("Creating fine items for lexical cell ["+cell.X()+", "+cell.Y()+"]: \""+this.getSentence().get(cell.X()).getWord()+"\"");
		}
		else {
			this.debug("Creating fine items for cell ["+cell.X()+", "+cell.Y()+"], spanning : \""+this.getSentence().asWords(cell.X(), cell.Y())+"\"");

		}
		for(CoarseChartItem ci : cell.values()) {
			if(ci.used) {
				try {
					itemsProcessed = createFineItem(ci, model, itemsProcessed);
				}
				catch(Exception e) {
					e.printStackTrace();
					Util.Error("Unable to create fine children for: "+ci);
				}
			}
		}
	}
	private void createFineItemsForCell(Cell cell, PerceptronParser parser, boolean scoreForest) {
		HashSet<CoarseChartItem> itemsProcessed = new HashSet<CoarseChartItem>();
		if(cell.X() == cell.Y()) {
			this.debug("Creating fine items for lexical cell ["+cell.X()+", "+cell.Y()+"]: \""+this.getSentence().get(cell.X()).getWord()+"\"");
		}
		else {
			this.debug("Creating fine items for cell ["+cell.X()+", "+cell.Y()+"], spanning : \""+this.getSentence().asWords(cell.X(), cell.Y())+"\"");

		}
		for(CoarseChartItem ci : cell.values()) {
			if(ci.used) {
				try {
					itemsProcessed = createFineItem(ci, parser, itemsProcessed, scoreForest);
				}
				catch(Exception e) {
					e.printStackTrace();
					//Util.Error("Unable to create fine children for: "+ci);
				}
			}
		}
	}

	private HashSet<CoarseChartItem> createFineItem(CoarseChartItem ci, Model model,
			HashSet<CoarseChartItem> itemsProcessed) throws Exception {
		if (ci instanceof CoarseLexicalCategoryChartItem) {
			FineChartItem fineLexicalCI = model.getFineLexicalChartItem((CoarseLexicalCategoryChartItem) ci, this);
			try {
				model.setLogProbabilityOfLexicalChartItem(fineLexicalCI, this);
			}
			catch(Exception e) {
				Util.Error("Unable to set probability of pre-lexical item: "+fineLexicalCI);
			}
			fineLexicalCI = ci.addFineChartItem(fineLexicalCI);
			this.debug("  Added fine lexical category: "+fineLexicalCI.toString());
			itemsProcessed.add(ci);
			return itemsProcessed;
		}
		for (BackPointer bp : ci.children) {
			if(bp.isUnary()) {
				if(!itemsProcessed.contains(bp.B())) {
					itemsProcessed = createFineItem(bp.B(), model, itemsProcessed);
				}
				for(FineChartItem fineChildCI : bp.B.fineItems()) {
					if(fineChildCI.getViterbiProb() > Double.NEGATIVE_INFINITY) {
						FineChartItem fineParentCI = model.getFineUnaryChartItem(ci, fineChildCI);
						double viterbiProb = model.getLogProbabilityOfFineUnaryChartItem(fineParentCI, fineChildCI, this);
						if(viterbiProb > Double.NEGATIVE_INFINITY) {
							fineParentCI = ci.addFineChartItem(fineParentCI);
							fineParentCI.setViterbiProb(viterbiProb, new FineBackPointer((Unary) bp.r, fineChildCI));
							this.debug("  Added unary backpointer:\n"+
									"     Parent:    "+fineParentCI.toString()+"\n"+
									"     Child:     "+fineChildCI.toString()+"\n"+
									"     Rule:      "+grammar.prettyRule(bp.r)+"\n"+
									"     Rule logP: "+(viterbiProb - fineChildCI.getViterbiProb()));
						}
					}
				}
			}
			else {
				// All binary children were already processed in previous cells
				for(FineChartItem fineLeftChildCI : bp.B.fineItems()) {
					for(FineChartItem fineRightChildCI : bp.C.fineItems()) {
						if(fineLeftChildCI.getViterbiProb() > Double.NEGATIVE_INFINITY
								&& fineRightChildCI.getViterbiProb() > Double.NEGATIVE_INFINITY) {
							FineChartItem fineParentCI = model.getFineBinaryChartItem(ci, bp,
									fineLeftChildCI, fineRightChildCI);
							FineBackPointer fineBP = new FineBackPointer((Binary) bp.r, fineLeftChildCI, fineRightChildCI);
							double viterbiProb = model.getLogProbabilityOfFineBinaryChartItem(fineParentCI, fineBP, this);
							if(viterbiProb > Double.NEGATIVE_INFINITY) {							
								fineParentCI = ci.addFineChartItem(fineParentCI);
								fineParentCI.setViterbiProb(viterbiProb, fineBP);
								this.debug("  Added binary backpointer:\n"+
										"     Parent:    "+fineParentCI.toString()+"\n"+
										"     Left:      "+fineLeftChildCI.toString()+"\n"+
										"     Right:     "+fineRightChildCI.toString()+"\n"+
										"     Rule:      "+grammar.prettyRule(bp.r)+"\n"+
										"     Rule logP: "+(viterbiProb
												- (fineLeftChildCI.getViterbiProb()+fineRightChildCI.getViterbiProb())));
							}
						}
					}
				}
			}
		}
		itemsProcessed.add(ci);		
		return itemsProcessed;
	}


	private HashSet<CoarseChartItem> createFineItem(CoarseChartItem ci, PerceptronParser parser,
			HashSet<CoarseChartItem> itemsProcessed, boolean scoreForest) throws Exception {
		if (ci instanceof CoarseLexicalCategoryChartItem) {
			FineChartItem fineLexicalCI = parser.getFineLexicalChartItem((CoarseLexicalCategoryChartItem) ci, this);
			parser.setScoreOfLexicalItem(fineLexicalCI);
			fineLexicalCI = ci.addFineChartItem(fineLexicalCI);
			itemsProcessed.add(ci);
			return itemsProcessed;
		}
		if(ci == this.coarseRoot) {
			FineChartItem fineParentCI = parser.getFineRootChartItem(ci);
			fineParentCI = ci.addFineChartItem(fineParentCI);
			for (BackPointer bp : ci.children) {
				if(!itemsProcessed.contains(bp.B())) {
					itemsProcessed = createFineItem(bp.B(), parser, itemsProcessed, scoreForest);
				}

				for(FineChartItem fineChildCI : bp.B.fineItems()) {
					if(fineChildCI.getViterbiProb() > Double.NEGATIVE_INFINITY) {
						if(scoreForest) {
							double score = parser.getScoreOfUnaryChartItem(fineParentCI, fineChildCI);
							if(score > Double.NEGATIVE_INFINITY) {
								fineParentCI.setViterbiProb(score, new FineBackPointer((Unary) bp.r, fineChildCI));	
							}
						}
						else {
							fineParentCI.setViterbiProb(0.0, new FineBackPointer((Unary) bp.r, fineChildCI));	
						}
					}
				}
			}
			itemsProcessed.add(ci);		
			return itemsProcessed;
		}
		for (BackPointer bp : ci.children) {
			if(bp.isUnary()) {
				// TODO: see which option saves more time
				/*
				if(bp.B().fineItems().isEmpty()) {
					createFineItem(bp.B(), parser, itemsProcessed, scoreForest);
				}
				 */				
				if(!itemsProcessed.contains(bp.B())) {
					itemsProcessed = createFineItem(bp.B(), parser, itemsProcessed, scoreForest);
				}

				for(FineChartItem fineChildCI : bp.B.fineItems()) {
					if(fineChildCI.getViterbiProb() > Double.NEGATIVE_INFINITY) {
						FineChartItem fineParentCI = parser.getFineUnaryChartItem(ci, fineChildCI);
						if(scoreForest) {
							double score = parser.getScoreOfUnaryChartItem(fineParentCI, fineChildCI);
							if(score > Double.NEGATIVE_INFINITY) {
								fineParentCI = ci.addFineChartItem(fineParentCI);
								fineParentCI.setViterbiProb(score, new FineBackPointer((Unary) bp.r, fineChildCI));	
							}
						}
						else {
							fineParentCI = ci.addFineChartItem(fineParentCI);
							fineParentCI.setViterbiProb(0.0, new FineBackPointer((Unary) bp.r, fineChildCI));	
						}
					}
				}
			}
			else {
				// All binary children were already processed in previous cells
				for(FineChartItem fineLeftChildCI : bp.B.fineItems()) {
					for(FineChartItem fineRightChildCI : bp.C.fineItems()) {
						if(fineLeftChildCI.getViterbiProb() > Double.NEGATIVE_INFINITY
								&& fineRightChildCI.getViterbiProb() > Double.NEGATIVE_INFINITY) {
							FineChartItem fineParentCI = parser.getFineBinaryChartItem(ci, bp,
									fineLeftChildCI, fineRightChildCI);
							FineBackPointer fineBP = new FineBackPointer((Binary) bp.r, fineLeftChildCI, fineRightChildCI);
							if(scoreForest) {
								double viterbiProb = parser.getScoreOfBinaryChartItem(fineParentCI, fineBP);
								if(viterbiProb > Double.NEGATIVE_INFINITY) {							
									fineParentCI = ci.addFineChartItem(fineParentCI);
									fineParentCI.setViterbiProb(viterbiProb, fineBP);
								}
							}
							else {

								fineParentCI = ci.addFineChartItem(fineParentCI);
								fineParentCI.setViterbiProb(1.0, fineBP);
							}
						}
					}
				}
			}
		}
		itemsProcessed.add(ci);		
		return itemsProcessed;
	}

	private void applyBeamToCell(Cell cell, Model model, double logBeamWidth) {
		try {
			double maxLogProb = Double.NEGATIVE_INFINITY;
			for(CoarseChartItem ci : cell.values()) {			
				for(FineChartItem fineCI : ci.fineItems()) {
					double figureOfMerit = model.getFigureOfMerit(fineCI, this);
					maxLogProb = Math.max(maxLogProb, figureOfMerit);
				}
			}		
			double logCutoff = maxLogProb + logBeamWidth;
			for(CoarseChartItem ci : cell.values()) {			
				HashSet<FineChartItem> retainedItems = new HashSet<FineChartItem>();
				for(FineChartItem fineCI : ci.fineItems()) {
					double figureOfMerit = model.getFigureOfMerit(fineCI, this);
					if(figureOfMerit >= logCutoff) {
						retainedItems.add(fineCI);
					}
				}
				ci.setFineGrained(retainedItems);
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			Util.Error("Exception while applying beam.");
		}
	}

	public void initializeCoarseChart() { 
		// Fills the 2-D chart array with empty cells
		this.chart = new Cell[this.getSentence().length()][this.getSentence().length()];
		for (int i = 0; i < chart.length; i++) {
			chart[i][i] = new Cell(this, i);
		}
		for (int s = 1; s < chart.length; s++) {
			for (int i = 0; i < chart.length - s; i++) {
				if (chart[i][i + s] == null) {
					chart[i][i + s] = new Cell(this, i, i + s);
				}
			}
		}
		this.computeWordsAndTagsWithUnk(this.grammar);
	}

	public Cell getCoarseCell(int xy) {
		return this.getCoarseCell(xy, xy);
	}

	public Cell getCoarseCell(int x, int y) {
		return chart[x][y];
	}

	public void setCoarseRoot(CoarseChartItem topCI) {
		this.coarseRoot = topCI;
	}


	// Temporary debugging code; TODO Ryan should make this cleaner
	private boolean DEBUG_CHART = false;
	private PrintWriter debugpw;

	// Set up debugging script
	public void setupDebugger(PrintWriter parserpw) {
		debugpw = parserpw;
		DEBUG_CHART = true;
	}

	public CoarseChartItem coarseRoot() {
		return coarseRoot;
	}

	public FineChartItem fineRoot() {
		return fineRoot;
	}

	public void fineParseWithPerceptronModel(PerceptronParser parser, boolean scoreForest) {
		if(this.successfulCoarseParse()) {
			// Mark which items belong to a successful parse (the rest will be ignored)
			this.markUsed(this.coarseRoot);
			this.debug(" Starting fine parsing for: "+this.getSentence().asWords());
			// Run the CYK algorithm
			for (int s = 0; s < chart.length; s++) {
				for (int i = 0; i < chart.length - s; i++) {
					Cell cell = this.getCoarseCell(i, i+s);
					this.createFineItemsForCell(cell, parser, scoreForest);
				}
			}
			for(FineChartItem fine : this.coarseRoot.fineItems()) {
				if(this.fineRoot == null || this.fineRoot.getViterbiProb() < fine.getViterbiProb()) {
					this.fineRoot = fine;
				}
			}
		}
	}

	public void writeCoarseParse(PrintWriter pw) {
		pw.println(this.getSentence().autoString);
		pw.println(this.getSentence().toString());
		this.writeCoarseForest(pw);
		pw.println(DELIMITER);
	}

	private static String DELIMITER = "###";

	private void writeCoarseForest(PrintWriter pw) {
		for(int s=0; s<this.length-1; s++) {
			for(int i=0; i<this.length-s; i++) {
				pw.println(i+","+(i+s));
				for(Integer cat : this.chart[i][i+s].cats()) {
					for(CoarseChartItem ci : this.chart[i][i+s].getCats(cat)) {
						if(ci.used) {
							pw.println("  "+getCoarseItemString(ci));
							for(BackPointer bp : ci.children) {
								CoarseChartItem b = bp.B();
								String line = "    "+getCoarseItemString(b);
								if(bp.C() != null) {
									CoarseChartItem c = bp.C();
									line += " "+getCoarseItemString(c);
									Binary r = (Binary) bp.r;
									line += " "+r.getType()+" "+r.arity+" "+r.head;
								}
								else {
									line += " "+bp.r.getType();
								}
								pw.println(line);
							}
						}
					}
				}
			}
		}
	}

	public static Chart readCoarseParse(Scanner sc, Grammar g) {
		String goldAutoString = sc.nextLine().trim();
		String sentenceString = sc.nextLine().trim();
		Sentence sen = new Sentence(sentenceString);
		sen.addCCGbankParse(goldAutoString);
		Chart chart = new Chart(sen, g);
		chart.populateCoarseForest(sc);
		return chart;
	}

	private void populateCoarseForest(Scanner sc) {
		String line;
		Cell cell = null;
		CoarseChartItem ci = null;
		while(sc.hasNextLine() && (!(line = sc.nextLine()).equals(DELIMITER))) {
			if(line.matches("\\d+,\\d+")) {
				String[] indices = line.trim().split(",");
				int x = Integer.parseInt(indices[0]);
				int y = Integer.parseInt(indices[1]);
				cell = this.chart[x][y];
			}
			else if(line.startsWith("    ")) {
				String[] bpToks = line.trim().split("\\s+");
				int x = Integer.parseInt(bpToks[0]);
				int y = Integer.parseInt(bpToks[1]);
				Cell bCell = this.chart[x][y];
				CoarseChartItem b = bCell.getCat(this.getCoarseItemFromString(bCell, bpToks, 0));
				if(bpToks.length > 3 + ciStringLength()) {
					// Binary bp
					x = Integer.parseInt(bpToks[0]);
					y = Integer.parseInt(bpToks[1]);
					Cell cCell = this.chart[x][y];
					CoarseChartItem c = bCell.getCat(this.getCoarseItemFromString(cCell, bpToks, 0));
					Rule_Type type = Rule_Type.valueOf(bpToks[bpToks.length-3]);
					int arity = Integer.parseInt(bpToks[bpToks.length-2]);
					Rule_Direction dir = Rule_Direction.valueOf(bpToks[bpToks.length-1]);
					ci.addChild(new Binary(ci.category(), b.category(), c.category(), type, arity, dir), b, c);
				}
				else {
					// Unary bp
					Rule_Type type = Rule_Type.valueOf(bpToks[bpToks.length-1]);
					ci.addChild(new Unary(ci.category(), b.category(), type), b);
				}
			}
			else if(line.startsWith("  ")) {
				if(ci != null) {
					if(ci.children.isEmpty()) {
						CoarseLexicalCategoryChartItem lexCI = new CoarseLexicalCategoryChartItem(cell, ci.category());
						cell.addCat(lexCI);
					}
					else {
						cell.addCat(ci);
					}
				}
				ci = this.getCoarseItemFromString(cell, line); 
			}
			else if(line.trim().equals(DELIMITER)) {
				return;
			}
		}
	}

	private static String getCoarseItemString(CoarseChartItem ci) {
		return ci.category()+" "+ci.arity()+" "+ci.punc();
	}

	private static int ciStringLength() {
		return 3;
	}

	private CoarseChartItem getCoarseItemFromString(Cell cell, String s) {
		String[] ciToks = s.trim().split("\\s+");
		int cat = this.grammar.getCatID(ciToks[0]);
		int arity = Integer.parseInt(ciToks[1]);
		Punctuation punc = Punctuation.valueOf(ciToks[2]);
		return cell.getCat(new CoarseChartItem(cell, cat, Rule_Type.NULL, arity, punc));
	}

	private CoarseChartItem getCoarseItemFromString(Cell cell, String[] bpToks,
			int start) {
		String s = "";
		for(int i=start; i<ciStringLength(); i++) {
			s += bpToks[i]+" ";
		}
		return this.getCoarseItemFromString(cell, s.trim());
	}

	public Sentence getSentence() {
		return sentence;
	}
}
