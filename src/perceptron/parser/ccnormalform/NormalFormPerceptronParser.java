package perceptron.parser.ccnormalform;

import java.util.ArrayList;
import java.util.HashMap;

import ccgparser.util.OfficialCandCFeature;

import perceptron.core.representations.parsing.FeatureTree;
import perceptron.core.representations.parsing.FeatureTreeBackPointer;
import perceptron.parser.PerceptronParser;

import illinoisParser.BackPointer;
import illinoisParser.Binary;
import illinoisParser.Chart;
import illinoisParser.CoarseChartItem;
import illinoisParser.CoarseLexicalCategoryChartItem;
import illinoisParser.FineBackPointer;
import illinoisParser.FineChartItem;
import illinoisParser.Grammar;
import illinoisParser.Rule_Direction;
import illinoisParser.SupervisedParsingConfig;
import illinoisParser.variables.ConditioningVariables;
import illinoisParser.variables.VariablesFactory;

/**
 * This class implements the Clark & Curran normal form parsing model, and
 * can be training stochastically using perceptron updates.
 * 
 * @author ramusa2
 * 
 * @see <a href="http://www.mitpressjournals.org/doi/pdf/10.1162/coli.2007.33.4.493">Wide-Coverage Efficient Statistical Parsing
with CCG and Log-Linear Models</a>
 *
 */
public class NormalFormPerceptronParser extends PerceptronParser {

	
	public static final String NF_NAME = "Normal Form";

	// Lexcat features
	private static final int LC_WORD = 0; 	// 'a' in candc-1.00 format
	private static final int LC_POS = 1;	// 'b'

	// Unary rule features
	private static final int UNARY_RULE = 2;		// 'm'
	private static final int UNARY_RULE_WORD = 3;	// 'p'
	private static final int UNARY_RULE_POS = 4;	// 'r'

	// Root category features
	private static final int TOP_CAT = 5;		// 'c'
	private static final int TOP_CAT_WORD = 6;	// 'd'
	private static final int TOP_CAT_POS = 7;	// 'e'

	// Binary rule features
	private static final int BINARY_RULE = 8;		// 'n'
	private static final int BINARY_RULE_WORD = 9;	// 'q'
	private static final int BINARY_RULE_POS = 10;	// 's'

	// Binary rule surface dependency features
	private static final int BINARY_RULE_WORD_WORD = 11;	// 't'
	private static final int BINARY_RULE_TAG_WORD = 12;		// 'v'
	private static final int BINARY_RULE_WORD_TAG = 13;		// 'u'
	private static final int BINARY_RULE_TAG_TAG = 14;		// 'w'	

	// Binary rule distance features
	private static final int BINARY_RULE_WORD_DW = 15;	// '15'
	private static final int BINARY_RULE_WORD_DP = 16;	// '16'
	private static final int BINARY_RULE_WORD_DV = 17;	// '17'
	private static final int BINARY_RULE_POS_DW = 18;	// '18'
	private static final int BINARY_RULE_POS_DP = 19;	// '19'
	private static final int BINARY_RULE_POS_DV = 20;	// '20'

	public NormalFormPerceptronParser(Grammar g,
			SupervisedParsingConfig coarseParsingConfig) {
		super(g, coarseParsingConfig, NF_NAME);
	}

	@Override
	public FineChartItem getFineLexicalChartItem(
			CoarseLexicalCategoryChartItem ci, Chart chart) {
		NormalFormChartItem fineItem = new NormalFormChartItem(ci);
		return fineItem;
	}

	@Override
	public FineChartItem getFineUnaryChartItem(CoarseChartItem ci,
			FineChartItem fineChildCI) {
		NormalFormChartItem fineChild = (NormalFormChartItem) fineChildCI;
		NormalFormChartItem fineParent = new NormalFormChartItem(ci, fineChild);
		return fineParent;
	}

	@Override
	public FineChartItem getFineRootChartItem(CoarseChartItem coarseRoot) {
		return new NormalFormChartItem(coarseRoot);
	}

	@Override
	public NormalFormChartItem getFineBinaryChartItem(CoarseChartItem ci,
			BackPointer bp, FineChartItem fineLeftChildCI,
			FineChartItem fineRightChildCI) {
		NormalFormChartItem fineHead;
		if(bp.direction() == Rule_Direction.Right) {
			fineHead = (NormalFormChartItem) fineRightChildCI;
		}
		else {
			fineHead = (NormalFormChartItem) fineLeftChildCI;
		}
		NormalFormChartItem fineParent = new NormalFormChartItem(ci, fineHead, 
				fineLeftChildCI.headIndex(), fineRightChildCI.headIndex());
		return fineParent;
	}

	@Override
	protected ArrayList<ConditioningVariables> getLexicalFeatures(
			NormalFormChartItem fineItem) {
		ArrayList<ConditioningVariables> feats = new ArrayList<ConditioningVariables>();
		feats.add(VariablesFactory.get(LC_WORD, fineItem.category(), fineItem.headWord()));
		feats.add(VariablesFactory.get(LC_POS, fineItem.category(), fineItem.headPOS()));
		return feats;
	}

	@Override
	protected ArrayList<ConditioningVariables> getUnaryFeatures(
			NormalFormChartItem fineParent, NormalFormChartItem fineChildCI) {
		ArrayList<ConditioningVariables> feats = new ArrayList<ConditioningVariables>();
		// Rule features
		feats.add(VariablesFactory.get(UNARY_RULE, fineParent.category(), fineChildCI.category()));
		feats.add(VariablesFactory.get(UNARY_RULE_WORD, fineParent.category(), 
				fineChildCI.category(), fineParent.headWord()));
		feats.add(VariablesFactory.get(UNARY_RULE_POS, fineParent.category(), 
				fineChildCI.category(), fineParent.headPOS()));
		// Root features
		if(fineParent.category() == grammar.getTopCatID()) {
			feats.addAll(getRootFeatures(fineChildCI));
		}
		return feats;
	}

	private ArrayList<ConditioningVariables> getRootFeatures(
			NormalFormChartItem fineChildCI) {
		ArrayList<ConditioningVariables> feats = new ArrayList<ConditioningVariables>();
		feats.add(VariablesFactory.get(TOP_CAT, fineChildCI.category()));
		feats.add(VariablesFactory.get(TOP_CAT_WORD, fineChildCI.category(), fineChildCI.headWord()));
		feats.add(VariablesFactory.get(TOP_CAT_POS, fineChildCI.category(), fineChildCI.headPOS()));
		return feats;
	}

	@Override
	protected ArrayList<ConditioningVariables> getBinaryFeatures(
			NormalFormChartItem fineParent, FineBackPointer bp) {
		ArrayList<ConditioningVariables> feats = new ArrayList<ConditioningVariables>();
		FineChartItem fineLeftChildCI = bp.B();
		FineChartItem fineRightChildCI = bp.C();
		int ruleID = grammar.getRuleID(bp.rule());
		// Add features
		// Common features
		feats.add(VariablesFactory.get(BINARY_RULE, ruleID));
		feats.add(VariablesFactory.get(BINARY_RULE_WORD, ruleID, fineParent.headWord()));
		feats.add(VariablesFactory.get(BINARY_RULE_POS, ruleID, fineParent.headPOS()));
		// Normal-form features
		NormalFormChartItem left = (NormalFormChartItem) fineLeftChildCI;
		NormalFormChartItem right = (NormalFormChartItem) fineRightChildCI;
		int lw = left.headWord();
		int lt = left.headPOS();
		int rw = right.headWord();
		int rt =right.headPOS();
		// TODO: check type
		feats.add(VariablesFactory.get(BINARY_RULE_WORD_WORD, ruleID, lw, rw));
		feats.add(VariablesFactory.get(BINARY_RULE_TAG_WORD, ruleID, lt, rw));
		feats.add(VariablesFactory.get(BINARY_RULE_WORD_TAG, ruleID, lw, rt));
		feats.add(VariablesFactory.get(BINARY_RULE_TAG_TAG, ruleID, lt, rt));
		// Word distance
		int hw = fineParent.headWord();
		int ht = fineParent.headPOS();
		int dw = fineParent.dW();
		int dp = fineParent.dP();
		int dv = fineParent.dV();
		feats.add(VariablesFactory.get(BINARY_RULE_WORD_DW, ruleID, hw, dw));
		feats.add(VariablesFactory.get(BINARY_RULE_WORD_DP, ruleID, hw, dp));
		feats.add(VariablesFactory.get(BINARY_RULE_WORD_DV, ruleID, hw, dv));
		feats.add(VariablesFactory.get(BINARY_RULE_POS_DW, ruleID, ht, dw));
		feats.add(VariablesFactory.get(BINARY_RULE_POS_DP, ruleID, ht, dp));
		feats.add(VariablesFactory.get(BINARY_RULE_POS_DV, ruleID, ht, dv));
		return feats;
	}

	private static final int[] EMPTY = new int[]{};

	@Override
	public int[] getFeaturesForItem(FineChartItem fineItem, boolean addFeaturesToParser) {
		NormalFormChartItem item = (NormalFormChartItem) fineItem;
		ArrayList<ConditioningVariables> feats;
		if(item.isLeaf()) {
			feats = this.getLexicalFeatures(item);
		}
		else {
			return EMPTY;
		}
		if(addFeaturesToParser) {
			return super.registerFeatures(feats);
		}
		return super.filterActiveFeatures(feats);
	}

	@Override
	public int[] getFeaturesForBackPointer(FineChartItem fineItem,
			FineBackPointer bp, boolean addFeaturesToParser) {
		NormalFormChartItem item = (NormalFormChartItem) fineItem;
		ArrayList<ConditioningVariables> feats;
		if(bp.isUnary()) {
			if(fineItem.category() == grammar.getTopCatID()) {
				feats = this.getRootFeatures((NormalFormChartItem) bp.B());
			}
			else {
				feats = this.getUnaryFeatures(item, (NormalFormChartItem) bp.B());
			}
		}
		else {
			feats = this.getBinaryFeatures(item, bp);
		}
		if(addFeaturesToParser) {
			return super.registerFeatures(feats);
		}
		return super.filterActiveFeatures(feats);
	}


	public String getCandCString(ConditioningVariables feature) {
		int type = feature.getVariableValue(0);
		String letter = getCandCTypeID(type);
		String parent, head, child, lh, rh;
		int dist;
		Binary r;
		String ruleString;
		switch(type) {
		case LC_WORD:
		case LC_POS:
			parent = grammar.getCatFromID(feature.getVariableValue(1));
			head = grammar.getLexFromID(feature.getVariableValue(2));
			return letter+" "+parent+" "+head;
		case TOP_CAT:
			parent = grammar.getCatFromID(feature.getVariableValue(1));
			return letter+" "+parent;
		case TOP_CAT_WORD:
		case TOP_CAT_POS:
			parent = wrap(grammar.getCatFromID(feature.getVariableValue(1)));
			head = grammar.getLexFromID(feature.getVariableValue(2));
			return letter+" "+parent+" "+head;
		case UNARY_RULE:
			parent = wrap(grammar.getCatFromID(feature.getVariableValue(1)));
			child = wrap(grammar.getCatFromID(feature.getVariableValue(2)));
			return letter+" "+child+" "+parent+" "+parent;
		case UNARY_RULE_WORD:
		case UNARY_RULE_POS:
			parent = wrap(grammar.getCatFromID(feature.getVariableValue(1)));
			child = wrap(grammar.getCatFromID(feature.getVariableValue(2)));
			head = grammar.getLexFromID(feature.getVariableValue(3));
			return letter+" "+child+" "+parent+" "+parent+" "+head;		
		case BINARY_RULE:
			r = (Binary) grammar.getRuleFromID(feature.getVariableValue(1));
			ruleString = getCandCRuleString(r);
			return letter+" "+ruleString;
		case BINARY_RULE_WORD:
		case BINARY_RULE_POS:
			r = (Binary) grammar.getRuleFromID(feature.getVariableValue(1));
			ruleString = getCandCRuleString(r);
			head = grammar.getLexFromID(feature.getVariableValue(2));
			return letter+" "+ruleString+" "+head;
		case BINARY_RULE_WORD_WORD:
		case BINARY_RULE_TAG_WORD:
		case BINARY_RULE_WORD_TAG:
		case BINARY_RULE_TAG_TAG:
			r = (Binary) grammar.getRuleFromID(feature.getVariableValue(1));
			ruleString = getCandCRuleString(r);
			lh = grammar.getLexFromID(feature.getVariableValue(2));
			rh = grammar.getLexFromID(feature.getVariableValue(3));
			return letter+" "+ruleString+" "+lh+" "+rh;
		case BINARY_RULE_WORD_DW:
		case BINARY_RULE_WORD_DP:
		case BINARY_RULE_WORD_DV:
		case BINARY_RULE_POS_DW:
		case BINARY_RULE_POS_DP:
		case BINARY_RULE_POS_DV:
			r = (Binary) grammar.getRuleFromID(feature.getVariableValue(1));
			ruleString = getCandCRuleString(r);
			head = grammar.getLexFromID(feature.getVariableValue(2));
			dist = feature.getVariableValue(3);
			return letter+" "+ruleString+" "+head+" "+dist;
		default:
			return "C&C string not implemented";
		}
	}

	private String getCandCRuleString(Binary r) {
		String parent = wrap(grammar.getCatFromID(r.A));
		String leftChild = wrap(grammar.getCatFromID(r.B));
		String rightChild = wrap(grammar.getCatFromID(r.C));
		return leftChild+" "+rightChild+" "+parent;
	}

	private static final HashMap<String, String> wrapCache = new HashMap<String, String>();

	private String wrap(String cat) {
		String cached = wrapCache.get(cat);
		if(cached != null) {
			return cached;
		}
		else if(cat.contains("\\") || cat.contains("/")) {
			cached = "("+checkModifier(cat)+")";
			wrapCache.put(cat,  cached);
			return cached;
		}
		wrapCache.put(cat,  cat);
		return cat;
	}

	private String checkModifier(String cat) {
		return cat.replaceAll("(S\\\\NP)\\\\(S\\\\NP)", "(S[X]\\NP)\\(S[X]\\NP)").replaceAll("(S\\\\NP)/(S\\\\NP)", "(S[X]\\NP)/(S[X]\\NP)");
	}

	private String getCandCTypeID(int type) {
		switch(type) {
		case LC_WORD:
			return "a";
		case LC_POS:
			return "b";
		case UNARY_RULE:
			return "m";
		case UNARY_RULE_WORD:
			return "p";
		case UNARY_RULE_POS:
			return "r";
		case TOP_CAT:
			return "c";
		case TOP_CAT_WORD:
			return "d";
		case TOP_CAT_POS:
			return "e";
		case BINARY_RULE:
			return "n";
		case BINARY_RULE_WORD:
			return "q";
		case BINARY_RULE_POS:
			return "s";
		case BINARY_RULE_WORD_WORD:
			return "t";
		case BINARY_RULE_TAG_WORD:
			return "u";
		case BINARY_RULE_WORD_TAG:
			return "v";
		case BINARY_RULE_TAG_TAG:
			return "w";
		case BINARY_RULE_WORD_DW:
			return "F";
		case BINARY_RULE_WORD_DP:
			return "G";
		case BINARY_RULE_WORD_DV:
			return "H";
		case BINARY_RULE_POS_DW:
			return "I";
		case BINARY_RULE_POS_DP:
			return "J";
		case BINARY_RULE_POS_DV:
			return "K";
		default:
			return "???";
		}
	}

	public String getPrettyFeatureString(ConditioningVariables feature) {
		int type = feature.getVariableValue(0);
		String letter = tab(getPrettyFeatureLabel(type), 4);
		String parent, head, child, lh, rh;
		int dist;
		Binary r;
		String ruleString;
		switch(type) {
		case LC_WORD:
		case LC_POS:
			parent = grammar.getCatFromID(feature.getVariableValue(1));
			head = grammar.getLexFromID(feature.getVariableValue(2));
			return letter+" "+parent+" "+head;
		case TOP_CAT:
			parent = grammar.getCatFromID(feature.getVariableValue(1));
			return letter+" "+parent;
		case TOP_CAT_WORD:
		case TOP_CAT_POS:
			parent = wrap(grammar.getCatFromID(feature.getVariableValue(1)));
			head = grammar.getLexFromID(feature.getVariableValue(2));
			return letter+" "+parent+" "+head;
		case UNARY_RULE:
			parent = wrap(grammar.getCatFromID(feature.getVariableValue(1)));
			child = wrap(grammar.getCatFromID(feature.getVariableValue(2)));
			return letter+" "+child+" "+parent+" "+parent;
		case UNARY_RULE_WORD:
		case UNARY_RULE_POS:
			parent = wrap(grammar.getCatFromID(feature.getVariableValue(1)));
			child = wrap(grammar.getCatFromID(feature.getVariableValue(2)));
			head = grammar.getLexFromID(feature.getVariableValue(3));
			return letter+" "+child+" "+parent+" "+parent+" "+head;		
		case BINARY_RULE:
			r = (Binary) grammar.getRuleFromID(feature.getVariableValue(1));
			ruleString = getCandCRuleString(r);
			return letter+" "+ruleString;
		case BINARY_RULE_WORD:
		case BINARY_RULE_POS:
			r = (Binary) grammar.getRuleFromID(feature.getVariableValue(1));
			ruleString = getCandCRuleString(r);
			head = grammar.getLexFromID(feature.getVariableValue(2));
			return letter+" "+ruleString+" "+head;
		case BINARY_RULE_WORD_WORD:
		case BINARY_RULE_TAG_WORD:
		case BINARY_RULE_WORD_TAG:
		case BINARY_RULE_TAG_TAG:
			r = (Binary) grammar.getRuleFromID(feature.getVariableValue(1));
			ruleString = getCandCRuleString(r);
			lh = grammar.getLexFromID(feature.getVariableValue(2));
			rh = grammar.getLexFromID(feature.getVariableValue(3));
			return letter+" "+ruleString+" "+lh+" "+rh;
		case BINARY_RULE_WORD_DW:
		case BINARY_RULE_WORD_DP:
		case BINARY_RULE_WORD_DV:
		case BINARY_RULE_POS_DW:
		case BINARY_RULE_POS_DP:
		case BINARY_RULE_POS_DV:
			r = (Binary) grammar.getRuleFromID(feature.getVariableValue(1));
			ruleString = getCandCRuleString(r);
			head = grammar.getLexFromID(feature.getVariableValue(2));
			dist = feature.getVariableValue(3);
			return letter+" "+ruleString+" "+head+" "+dist;
		default:
			return "Pretty feature string not implemented";
		}
	}

	private String tab(String str, int i) {
		int toAdd = i - (str.length()/4);
		for(int t=0; t<toAdd; t++) {
			str += "\t";
		}
		return str;
	}

	private String getPrettyFeatureLabel(int type) {
		switch(type) {
		case LC_WORD:
			return "LexCat+Word";
		case LC_POS:
			return "LexCat+POS";
		case UNARY_RULE:
			return "Unary";
		case UNARY_RULE_WORD:
			return "Unary+Word";
		case UNARY_RULE_POS:
			return "Unary+POS";
		case TOP_CAT:
			return "RootCat";
		case TOP_CAT_WORD:
			return "RootCat+Word";
		case TOP_CAT_POS:
			return "RootCat+POS";
		case BINARY_RULE:
			return "Binary";
		case BINARY_RULE_WORD:
			return "Binary+Word";
		case BINARY_RULE_POS:
			return "Binary+POS";
		case BINARY_RULE_WORD_WORD:
			return "Binary+WordWord";
		case BINARY_RULE_TAG_WORD:
			return "Binary+POSWord";
		case BINARY_RULE_WORD_TAG:
			return "Binary+WordPOS";
		case BINARY_RULE_TAG_TAG:
			return "Binary+POSPOS";
		case BINARY_RULE_WORD_DW:
			return "Binary+Word+dW";
		case BINARY_RULE_WORD_DP:
			return "Binary+Word+dP";
		case BINARY_RULE_WORD_DV:
			return "Binary+Word+dV";
		case BINARY_RULE_POS_DW:
			return "Binary+POS+dW";
		case BINARY_RULE_POS_DP:
			return "Binary+POS+dP";
		case BINARY_RULE_POS_DV:
			return "Binary+POS+dV";
		default:
			return "???";
		}
	}

	public String getPrettyFeatureTree(FeatureTree tree) {
		return this.prettyFeatureTreeRecurse(tree, 0);
	}

	private String prettyFeatureTreeRecurse(FeatureTree tree, int t) {
		String str = tab(t);
		int[] features = tree.features();
		ArrayList<String> nodeFeatureStrings = getPrettyFeatureStrings(features);
		if(tree.isLeaf()) {
			int category = this.getFeature(features[0]).getVariableValue(1);
			int word = this.getFeature(features[0]).getVariableValue(2);
			str += grammar.getCatFromID(category)+" -> \""+grammar.getLexFromID(word)+"\"\n";
			for(String prettyFeat : nodeFeatureStrings) {
				str += tab(t)+"  "+prettyFeat+"\n";
			}
			return str+"\n";
		}
		FeatureTreeBackPointer bp = tree.backpointer();
		int[] bpFeatures = bp.features();
		ArrayList<String> bpFeatureStrings = getPrettyFeatureStrings(bpFeatures);
		if(bp.isUnary()) {
			int category;
			int childCategory;
			if(t==0) {
				category = this.grammar().getTopCatID();
				childCategory = this.getFeature(bpFeatures[0]).getVariableValue(1);
			}
			else {
				category = this.getFeature(bpFeatures[0]).getVariableValue(1);
				childCategory = this.getFeature(bpFeatures[0]).getVariableValue(2);
			}
			str += grammar.getCatFromID(category);
			for(String prettyFeat : nodeFeatureStrings) {
				str += tab(t)+"  "+prettyFeat+"\n";
			}
			str += "\n"+tab(t)+"  "+grammar.getCatFromID(category)+" -> "+grammar.getCatFromID(childCategory)+"\n";
			for(String prettyFeat : bpFeatureStrings) {
				str += tab(t+1)+prettyFeat+"\n";
			}
			return str+"\n"+prettyFeatureTreeRecurse(bp.leftChild(), t+1);
		}
		else {
			int ruleID = this.getFeature(bpFeatures[0]).getVariableValue(1);
			Binary rule = (Binary) grammar.getRuleFromID(ruleID);
			
			String parentCategory = grammar.getCatFromID(rule.A);
			String leftChildCategory = grammar.getCatFromID(rule.B);
			String rightChildCategory = grammar.getCatFromID(rule.C);
			str += parentCategory;
			for(String prettyFeat : nodeFeatureStrings) {
				str += tab(t+1)+prettyFeat+"\n";
			}
			str += "\n"+tab(t)+"  "+parentCategory+" -> "+leftChildCategory+" "+rightChildCategory+"\n";
			for(String prettyFeat : bpFeatureStrings) {
				str += tab(t+1)+prettyFeat+"\n";
			}
			return str+"\n"+prettyFeatureTreeRecurse(bp.leftChild(), t+1)+"\n"+prettyFeatureTreeRecurse(bp.rightChild(), t+1);
		}
	}

	private ArrayList<String> getPrettyFeatureStrings(int... features) {
		ArrayList<String> list = new ArrayList<String>();
		for(int f : features) {
			list.add(getPrettyFeatureString(this.getFeature(f)));
		}
		return list;
	}

	private String tab(int tab) {
		String str = "";
		for(int t=0; t<tab; t++) {
			str += "\t";
		}
		return str;
	}
}
