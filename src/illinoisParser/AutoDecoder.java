package illinoisParser;

import perceptron.parser.PerceptronChart;
import perceptron.parser.PerceptronParser;


/**
 * Helper class for constructing coarse parses from AUTO strings.
 * 
 * @author ramusa2
 *
 */
public class AutoDecoder {
	private int numLeavesSeen;
	private Sentence sentence;
	private String autoString;

	public AutoDecoder(Sentence sen, String auto) {
		numLeavesSeen = 0;
		sentence = sen;
		autoString = auto;
	}

	public AutoDecoder(Sentence sen) {
		this(sen, sen.getCCGbankParse());
	}

	/**
	 * Decodes an AUTO parse, adding its rules to the grammar and returning the root of the parse
	 * tree as a ChartItem
	 * @param grammar
	 * @return
	 * @throws Exception 
	 */
	public Chart getCoarseChart(Grammar grammar) {

		this.numLeavesSeen = 0;
		// Initialize Chart
		Chart chart = new PerceptronChart(sentence, grammar);
		chart.initializeCoarseChart();
		// Process AUTO string
		String[] autoTokens = autoString.split("[<>]");
		try {
			CoarseChartItem sententialHead = decodeRecurse(chart, grammar, autoTokens, 1, 0);
			Cell rootCell = chart.getCoarseCell(0, sentence.length()-1);
			// Add TOP node
			// TODO: do we need the Rule_Type TYPE_TOP here? Do we even want it?
			Unary toTop = grammar.createUnaryRule(grammar.TOP, sententialHead.category(),
					Rule_Type.TYPE_TOP);
			CoarseChartItem topCI = new CoarseChartItem(rootCell, toTop.A, toTop.getType(), -1);
			topCI.addChild(toTop, sententialHead, null);
			rootCell.addCat(topCI);
			chart.setCoarseRoot(topCI);
			return chart;
		}
		catch(NullPointerException e) {
			return null;
		}
	}

	/**
	 * Decodes an AUTO parse, adding its rules to the grammar and returning the root of the parse
	 * tree as a ChartItem
	 * @param grammar
	 * @return
	 * @throws Exception 
	 */
	public Chart getFineChart(Model model) {//throws Exception {
		// Initialize coarse chart
		Chart chart = this.getCoarseChart(model.grammar);
		if(chart == null || chart.coarseRoot == null) {
			Util.logln("Failed to build AUTO parse (bad CCGcat combine?): "+sentence.asWords());
			Util.logln(this.autoString);
			return null;
		}
		// Build fine chart
		this.buildFineChart(chart.coarseRoot, model, chart);
		return chart;
	}
	
	private final void buildFineChart(CoarseChartItem ci, Model model, Chart coarseChart) {
		if (ci instanceof CoarseLexicalCategoryChartItem) {
			FineChartItem fineLexicalCI = model.getFineLexicalChartItem((CoarseLexicalCategoryChartItem)ci, coarseChart);
			fineLexicalCI = ci.addFineChartItem(fineLexicalCI);
			return;
		}
		for (BackPointer bp : ci.children) {
			if(bp.isUnary()) {
				buildFineChart(bp.B, model, coarseChart);
				for(FineChartItem fineChildCI : bp.B.fineItems()) {
					FineChartItem fineParentCI = model.getFineUnaryChartItem(ci, fineChildCI);
					FineBackPointer fineBP = new FineBackPointer((Unary) bp.r, fineChildCI);
					fineParentCI.addChild(fineBP);
					double prob;
					try {
						prob = model.getLogProbabilityOfFineUnaryChartItem(fineParentCI, fineChildCI, coarseChart);
					} catch (Exception e) {
						prob = Log.ZERO;
					}
					fineParentCI.setViterbiProb(prob, fineBP);
					fineParentCI = ci.addFineChartItem(fineParentCI);
					if(fineParentCI.category() == model.grammar.TOP) {
						coarseChart.fineRoot = fineParentCI;
					}
				}
			}
			else {
				buildFineChart(bp.B, model, coarseChart);
				buildFineChart(bp.C, model, coarseChart);
				for(FineChartItem fineLeftChildCI : bp.B.fineItems()) {
					for(FineChartItem fineRightChildCI : bp.C.fineItems()) {
						FineChartItem fineParentCI = model.getFineBinaryChartItem(ci, bp,
								fineLeftChildCI, fineRightChildCI);
						FineBackPointer fineBP = new FineBackPointer((Binary) bp.r, fineLeftChildCI, fineRightChildCI);
						fineParentCI.addChild(fineBP);
						//FineBackPointer fineBP = fineParentCI.children.iterator().next();
						double prob;
						try {
							prob = model.getLogProbabilityOfFineBinaryChartItem(fineParentCI, fineBP, coarseChart);
						} catch (Exception e) {
							prob = Log.ZERO;
						}
						fineParentCI.setViterbiProb(prob, fineBP);
						fineParentCI = ci.addFineChartItem(fineParentCI);
					}
				}
			}
		}
	}
	

	public Chart getFineChart(PerceptronParser parser) {//throws Exception {
		// Initialize coarse chart
		Chart chart = this.getCoarseChart(parser.grammar());
		if(chart == null || chart.coarseRoot == null) {
			Util.logln("Failed to build AUTO parse (bad CCGcat combine?): "+sentence.asWords());
			Util.logln(this.autoString);
			return null;
		}
		// Build fine chart
		this.buildFineChart(chart.coarseRoot, parser, chart);
		return chart;
	}
	
	private final void buildFineChart(CoarseChartItem ci, PerceptronParser parser, Chart coarseChart) {
		if (ci instanceof CoarseLexicalCategoryChartItem) {
			FineChartItem fineLexicalCI = parser.getFineLexicalChartItem((CoarseLexicalCategoryChartItem)ci, coarseChart);
			fineLexicalCI = ci.addFineChartItem(fineLexicalCI);
			return;
		}
		for (BackPointer bp : ci.children) {
			if(bp.isUnary()) {
				buildFineChart(bp.B, parser, coarseChart);
				for(FineChartItem fineChildCI : bp.B.fineItems()) {
					FineChartItem fineParentCI = parser.getFineUnaryChartItem(ci, fineChildCI);
					FineBackPointer fineBP = new FineBackPointer((Unary) bp.r, fineChildCI);
					fineParentCI.addChild(fineBP);
					double prob;
					try {
						prob = parser.getScoreOfUnaryChartItem(fineParentCI, fineChildCI);
					} catch (Exception e) {
						prob = Log.ZERO;
					}
					fineParentCI.setViterbiProb(prob, fineBP);
					fineParentCI = ci.addFineChartItem(fineParentCI);
					if(fineParentCI.category() == parser.grammar().TOP) {
						coarseChart.fineRoot = fineParentCI;
					}
				}
			}
			else {
				buildFineChart(bp.B, parser, coarseChart);
				buildFineChart(bp.C,parser, coarseChart);
				for(FineChartItem fineLeftChildCI : bp.B.fineItems()) {
					for(FineChartItem fineRightChildCI : bp.C.fineItems()) {
						FineChartItem fineParentCI = parser.getFineBinaryChartItem(ci, bp,
								fineLeftChildCI, fineRightChildCI);
						FineBackPointer fineBP = new FineBackPointer((Binary) bp.r, 
								fineLeftChildCI, fineRightChildCI);
						fineParentCI.addChild(fineBP);
						double prob;
						try {
							prob = parser.getScoreOfBinaryChartItem(fineParentCI, fineBP);
						} catch (Exception e) {
							prob = Log.ZERO;
						}
						fineParentCI.setViterbiProb(prob, fineBP);
						fineParentCI = ci.addFineChartItem(fineParentCI);
					}
				}
			}
		}
	}
	
	public Tree<? extends FineChartItem> buildTree(Model model) {
		Chart chart = this.getFineChart(model);
		if(chart != null && chart.successfulFineParse()) {
			return chart.getViterbiParse();
		}
		return null;
	}

	/**
	 * Recursive helper method for decoding an AUTO parse
	 * @param tokens the tokenized version of the AUTO parse, split by nodes
	 * @param k the current token (node) we are processing
	 * @param leftIndex the index of the left-most leaf in the current constituent
	 * @return a ChartItem corresponding to the current node, with all
	 *         sub-constituents attached
	 * @throws Exception
	 */
	private CoarseChartItem decodeRecurse(Chart chart, Grammar grammar, 
			String[] tokens, int k, int leftIndex) {
		String[] params = tokens[k].split(" ");
		boolean isLeaf = params[0].equals("L");
		if (isLeaf) {
			return createLeafNode(chart, grammar, params);
		}
		else {
			String category = params[1];
			boolean isUnary = Integer.valueOf(params[3]) == 1;
			if (isUnary) {
				CoarseChartItem childCI = decodeRecurse(chart, grammar, tokens, k+2, leftIndex);
				return createUnaryNode(chart, grammar, category, childCI);
			}
			else {
				int leftChildIndex = k + 2;
				int rightChildIndex = getRightChildIndex(tokens, leftChildIndex);
				CoarseChartItem leftChildCI = decodeRecurse(chart, grammar, tokens, leftChildIndex, leftIndex);
				int rightStartIndex = leftChildCI.Y() + 1;
				CoarseChartItem rightChildCI = decodeRecurse(chart, grammar, tokens, rightChildIndex,
						rightStartIndex);
				return createBinaryNode(chart, grammar, params, leftChildCI, rightChildCI);
			}
		}
	}

	/**
	 * Read in attributes for a leaf in the derivation, add the rule to the grammar, and return 
	 * a ChartItem with that lexical category.
	 * @param grammar
	 * @param params
	 * @return
	 * @throws Exception 
	 */
	private CoarseChartItem createLeafNode(Chart chart, Grammar grammar, String[] params) {
		int index = numLeavesSeen;
		numLeavesSeen++;
		LexicalToken lt = sentence.get(index);      
		String category = params[1];
		Unary prodRule = grammar.createLexicalRule(category, lt);
		Cell cell = chart.getCoarseCell(index);
		CoarseChartItem prodCI = new CoarseLexicalCategoryChartItem(cell, prodRule.A);
		prodCI.setCCGcat(CCGcat.lexCat(lt.getWord(), category, lt.getPOS().toString(), index));
		cell.addCat(prodCI);
		return prodCI;
	}

	/**
	 * Create a unary parent in the derivation
	 * @throws Exception 
	 */
	private CoarseChartItem createUnaryNode(Chart chart, Grammar grammar, 
			String category, CoarseChartItem childCI)  {
		int x = childCI.X();
		int y = childCI.Y();
		Cell cell = chart.getCoarseCell(x, y);
		Unary rule;
		CoarseChartItem unaryCI;
		// Determine rule type using CCG cat;
		CCGcat ccg_cat = CCGcat.typeRaiseTo(childCI.getCCGcat(), category);
		Rule_Type type = null;
		if (ccg_cat != null && ccg_cat.catString().equals(category)) {
			if (ccg_cat.argDir() == CCGcat.FW) { // left child
				type = Rule_Type.FW_TYPERAISE; // ???
			} else if (ccg_cat.argDir() == CCGcat.BW) { // right child
				type = Rule_Type.BW_TYPERAISE; // ???
			}
		} else {
			ccg_cat = CCGcat.typeChangingRule(childCI.getCCGcat(), category);
			type = Rule_Type.TYPE_CHANGE;
		}
		Integer catID = grammar.getCatID(category);
		rule = grammar.createUnaryRule(catID, childCI.category(), type);
		unaryCI = new CoarseChartItem(cell, rule);
		unaryCI.setCCGcat(ccg_cat);
		unaryCI.addChild(rule, childCI);
		cell.addCat(unaryCI);
		return unaryCI;
	}

	/**
	 * Create a binary parent in the derivation
	 * @throws Exception 
	 */
	private CoarseChartItem createBinaryNode(Chart chart, Grammar grammar, String[] params, 
			CoarseChartItem leftChildCI, CoarseChartItem rightChildCI) {
		String category = params[1];
		Rule_Direction dir = Rule_Direction.None;
		if(params[2].equals("LEFT")) {
			dir = Rule_Direction.Left;
		}
		else if (params[2].equals("RIGHT")) {
			dir = Rule_Direction.Right;
		}
		else {
			int headInt = Integer.valueOf(params[2]);
			if (headInt == 0) {
				dir = Rule_Direction.Left;
			} else if (headInt == 1) {
				dir = Rule_Direction.Right;
			}
		}
		int x = leftChildCI.X();
		int y = rightChildCI.Y();
		Cell cell = chart.getCoarseCell(x, y);
		CCGcat ccg_cat = CCGcat.combine(leftChildCI.getCCGcat(), rightChildCI.getCCGcat(),
				category);
		Integer catID = grammar.getCatID(category);
		int arity = 0;
		if(ccg_cat.type() == Rule_Type.FW_COMPOSE
				|| ccg_cat.type() == Rule_Type.BW_COMPOSE) {
			arity = 1;
		}
		Binary rule = grammar.createBinaryRule(catID, leftChildCI.category(), rightChildCI.category(), ccg_cat.type(), dir, arity);
		CoarseChartItem binaryCI = new CoarseChartItem(cell, rule);
		binaryCI.setCCGcat(ccg_cat);
		binaryCI.addChild(rule, leftChildCI, rightChildCI);
		cell.addCat(binaryCI);
		return binaryCI;
	}

	private static int getRightChildIndex(String[] tokens, int k) {
		int numLeavesToSee = 1;
		for (; k < tokens.length; k += 2) {
			String[] params = tokens[k].split(" ");
			boolean isLeaf = params[0].equals("L");
			if (isLeaf) {
				numLeavesToSee--;
				if (numLeavesToSee == 0) {
					return k + 2;
				}

				// currently at a binary rule, add another to-be-seen leaf to the count
			} else if (Integer.valueOf(params[3]) == 2) {
				numLeavesToSee++;
			}
		}
		return -1;
	}
}
