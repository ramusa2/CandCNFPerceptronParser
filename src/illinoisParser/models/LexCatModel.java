package illinoisParser.models;

import illinoisParser.BackPointer;
import illinoisParser.Chart;
import illinoisParser.CoarseChartItem;
import illinoisParser.CoarseLexicalCategoryChartItem;
import illinoisParser.ComplexDistribution;
import illinoisParser.Distribution;
import illinoisParser.FineBackPointer;
import illinoisParser.FineChartItem;
import illinoisParser.Grammar;
import illinoisParser.Log;
import illinoisParser.Model;
import illinoisParser.Rule_Direction;
import illinoisParser.Util;
import illinoisParser.variables.ConditioningVariables;
import illinoisParser.variables.VariablesFactory;
/**
 * An implementation of Hockenmaier's ACL '02 LexCat model, in which the 
 * lexical head (category) is generated at its maximal projection
 *  
 * @author ramusa2
 */
public class LexCatModel extends Model {
	
	/**
	 * Model name (static)
	 */
	public static String MODEL_NAME = "lexcat";

	// Rule types (shared codebook values)
	private Integer Lex,Left,Right,Unary,Top;

	// Model distributions
	protected Distribution pType, pHead, pSister, pHeadLexCat, pTopCat, pWord;

	// Beam search distributions
	private Distribution pBeamPOS, pBeamWord, pBeamCategory;

	public LexCatModel(Grammar g) {
		grammar = g;   
		setTypes();
		createDistributions();
		//lexcatFreqs = new  ConcurrentHashMap<Integer, Double>();
	}

	private void setTypes() {
		// Need shared codebook values:
		Left  = grammar.getTypeID("#LEFT#");
		Right = grammar.getTypeID("#RIGHT#");
		Unary = grammar.getTypeID("#UNARY#");
		Top   = grammar.getTypeID("#TOP#");
		Lex   = grammar.getTypeID("#LEX#");
	}

	private void createDistributions() {
		// Add beam search distributions, just in case
		pBeamCategory = new ComplexDistribution(this, "pBeamHeadCategory",
				new Distribution(this, "pBeamHeadCategoryBackoff"));
		Distributions.add(pBeamCategory);
		pBeamPOS = new Distribution(this,"pBeamPOS");
		Distributions.add(pBeamPOS);
		pBeamWord = new Distribution(this,"pBeamWord");
		Distributions.add(pBeamWord);
		// Create model distributions
		pType = new Distribution(this,"pExpansion");
		//pType = new ComplexDistribution(this,"pExpansion",
		//		new Distribution(this, "pExpansionBackoff"));

		pHead = new Distribution(this,"pHeadCat");
		//pHead = new ComplexDistribution(this,"pHead",
		//		new Distribution(this, "pHeadBackoff"));

		// pWord is like pHead for word productions; the separation is necessary for decoding pretty contexts. 
		// Given that exp=Lex only for word productions, this shouldn't affect the distributions
		pWord = new Distribution(this,"pWord");

		pSister = new ComplexDistribution(this,"pNonHeadCat",
				new Distribution(this, "pNonHeadCatBackoff"));

		pHeadLexCat = new ComplexDistribution(this,"pLexCat",
				new Distribution(this, "pLexCatBackoff"));
		pTopCat = new Distribution(this,"pTopCat");

		// Store model distributions
		Distributions.add(pType);
		Distributions.add(pHead);
		Distributions.add(pWord);
		Distributions.add(pSister);
		Distributions.add(pHeadLexCat);
		Distributions.add(pTopCat);
	}

	@Override
	public FineChartItem getFineLexicalChartItem(CoarseLexicalCategoryChartItem ci, Chart coarseChart) {
		return new LexCatChartItem(ci);
	}

	@Override
	public void addCountsFromFineLexicalChartItem(FineChartItem fineLexicalCI,
			Chart coarseChart) {
		LexCatChartItem P = (LexCatChartItem) fineLexicalCI;
		int word = coarseChart.words[P.headIndex()];
		int type = this.Lex;		
		try {
			// Beam search counts
			this.addBeamPriorCounts((LexCatChartItem) fineLexicalCI, coarseChart);
			// TODO: clarify these two distributions with Julia:
			// P(exp | P, c_p)			
			super.addObservedCount(this.pType, this.typeCond(P,  true), type);
			// P( word | c_p)
			super.addObservedCount(this.pWord, VariablesFactory.cache(P.category()), word);
		}
		catch (Exception e) {
			Util.logln("Failed to add lexical (beam) counts for: "+fineLexicalCI);
		}
	}

	@Override
	public void setLogProbabilityOfLexicalChartItem(FineChartItem fineLexicalCI,
			Chart coarseChart) {
		LexCatChartItem P = (LexCatChartItem) fineLexicalCI;
		int word = coarseChart.words[P.headIndex()];
		int type = this.Lex;
		double logProb = Log.ONE;
		try {
			// P(exp | P, c_p)
			logProb += this.pType.logProb(this.typeCond(P, false), type);
			// P( word | c_p)
			logProb += this.pWord.logProb(VariablesFactory.get(P.category()), word);
		}
		catch (Exception e) {
			Util.logln("Failed to set lexical probability for: "+fineLexicalCI);
			logProb = Log.ZERO;
		}
		fineLexicalCI.setViterbiProb(logProb, null);
	}

	@Override
	public FineChartItem getFineUnaryChartItem(CoarseChartItem ci, FineChartItem fineChildCI) {
		LexCatChartItem fineChild = (LexCatChartItem) fineChildCI;
		LexCatChartItem fineParent = new LexCatChartItem(ci, fineChild);
		return fineParent;
	}

	@Override
	public void addCountsFromFineUnaryChartItem(FineChartItem fineParentCI,
			FineChartItem fineChildCI, Chart coarseChart) throws Exception {
		LexCatChartItem P = (LexCatChartItem) fineParentCI;
		LexCatChartItem H = (LexCatChartItem) fineChildCI;
		int type = (P.category() == grammar.getTopCatID()) ? this.Top : this.Unary;
		try {
			// P(exp | P, c_p)			
			super.addObservedCount(this.pType, this.typeCond(P,  true), type);
			// P(H | P, exp, c_p)
			super.addObservedCount(this.pHead, this.headCond(P, type, true), H.category());
			// Check if we're going to TOP
			if(type == this.Top) {
				// P(c_TOP)
				super.addObservedCount(this.pTopCat, this.ctopCond(H), P.headLexCat());
			}
			else {
				// Add beam counts
				this.addBeamPriorCounts(P, coarseChart);
			}
		}
		catch (Exception e) {
			Util.logln("Failed to add unary counts for: "+fineParentCI);
		}
	}

	boolean PROB_DEBUG = false;
	
	@Override
	public double getLogUnaryRuleProbability(FineChartItem fineParentCI,
			FineChartItem fineChildCI, Chart coarseChart) throws Exception {
		LexCatChartItem P = (LexCatChartItem) fineParentCI;
		LexCatChartItem H = (LexCatChartItem) fineChildCI;
		int type = (P.category() == grammar.getTopCatID()) ? this.Top : this.Unary;
		double logProb = Log.ONE;
		// P(exp | P, c_p)
		logProb += this.pType.logProb(this.typeCond(P, false), type);
		// P(H | P, exp, c_p)
		logProb += this.pHead.logProb(this.headCond(P, type, false), H.category());
		// Check if we're going to TOP
		if(type == this.Top) {
			// P(c_TOP)
			logProb += this.pTopCat.logProb(this.ctopCond(H), P.headLexCat());
		}
		return logProb;
	}

	private ConditioningVariables ctopCond(LexCatChartItem H) {
		return VariablesFactory.getEmpty();
	}

	private ConditioningVariables headCond(LexCatChartItem P, int type, boolean cache) {
		if(cache) {
			return VariablesFactory.cache(P.category(), type, P.headLexCat());
		}
		return VariablesFactory.get(P.category(), type, P.headLexCat());
	}

	/* Interpolated P(H|...) distribution with P(H|P) backoff
	 * 
	private ConditioningVariables headCond(LexCatChartItem P, int type, boolean cache) {
		if(cache) {
			ConditioningVariables typeBackoff = VariablesFactory.cache(P.category(), type);
			ConditioningVariables typeRemainder = VariablesFactory.cache(P.headLexCat());
			return VariablesFactory.cache(typeBackoff, typeRemainder);
		}
		ConditioningVariables typeBackoff = VariablesFactory.get(P.category(), type);
		ConditioningVariables typeRemainder = VariablesFactory.get(P.headLexCat());
		return VariablesFactory.get(typeBackoff, typeRemainder);
	}
	*/

	public ConditioningVariables typeCond(LexCatChartItem P, boolean cache) {
		if(cache) {
			return VariablesFactory.cache(P.category(), P.headLexCat());
		}
		return VariablesFactory.get(P.category(), P.headLexCat());	
	}

	/* Interpolated P(exp|...) distribution with P(exp|P) backoff
	 * 
	public ConditioningVariables typeCond(LexCatChartItem P, boolean cache) {
		if(cache) {
			ConditioningVariables typeBackoff = VariablesFactory.cache(P.category());
			ConditioningVariables typeRemainder = VariablesFactory.cache(P.headLexCat());
			return VariablesFactory.cache(typeBackoff, typeRemainder);
		}
		ConditioningVariables typeBackoff = VariablesFactory.get(P.category());
		ConditioningVariables typeRemainder = VariablesFactory.get(P.headLexCat());
		return VariablesFactory.get(typeBackoff, typeRemainder);
	}
	*/

	@Override
	public FineChartItem getFineBinaryChartItem(CoarseChartItem ci,  BackPointer bp,
			FineChartItem fineLeftChildCI, FineChartItem fineRightChildCI) {
		LexCatChartItem fineHead;
		if(bp.direction() == Rule_Direction.Right) {
			fineHead = (LexCatChartItem) fineRightChildCI;
		}
		else {
			fineHead = (LexCatChartItem) fineLeftChildCI;
		}
		LexCatChartItem fineParent = new LexCatChartItem(ci, fineHead);
		return fineParent;
	}

	@Override
	public void addCountsFromFineBinaryChartItem(FineChartItem fineParentCI, FineBackPointer bp, Chart coarseChart) throws Exception {
		LexCatChartItem P = (LexCatChartItem) fineParentCI;
		// Add beam counts
		this.addBeamPriorCounts(P, coarseChart);
		LexCatChartItem H, S;
		int type;
		if(bp.direction() == Rule_Direction.Right) {
			H = (LexCatChartItem) bp.C();
			S = (LexCatChartItem) bp.B();
			type = this.Right;
		}
		else {
			H = (LexCatChartItem) bp.B();
			S = (LexCatChartItem) bp.C();
			type = this.Left;
		}		

		try {
			// P( exp | P, c_p)
			super.addObservedCount(this.pType, this.typeCond(P, true), type);
			// P( H | P, exp, c_p)
			super.addObservedCount(this.pHead, this.headCond(P, type, true), H.category());
			// P( S | P, exp, H # c_p)
			super.addObservedCount(this.pSister, this.sisterCond(P, H, type, true), S.category());
			// P( c_s | S # H, exp, P )
			super.addObservedCount(this.pHeadLexCat, this.c_sCond(P, H, S, type, false), S.headLexCat());
		}
		catch (Exception e) {
			Util.logln("Failed to add binary counts for: "+fineParentCI);
		}
	}

	@Override
	public double getLogBinaryRuleProbability(FineChartItem fineParentCI, FineBackPointer bp,
			Chart coarseChart) throws Exception {
		LexCatChartItem P = (LexCatChartItem) fineParentCI;
		LexCatChartItem H, S;
		int type;
		String pType = "";
		if(bp.direction() == Rule_Direction.Right) {
			H = (LexCatChartItem) bp.C();
			S = (LexCatChartItem) bp.B();
			type = this.Right;
			pType = "Right";
		}
		else {
			H = (LexCatChartItem) bp.B();
			S = (LexCatChartItem) bp.C();
			type = this.Left;
			pType = "Left";
		}
		double logProb = Log.ONE;
		// P( exp | P, c_p )
		logProb += this.pType.logProb(this.typeCond(P, false), type);
		// P( H | P, exp, c_p )
		logProb += this.pHead.logProb(this.headCond(P, type, false), H.category());
		// P( S | P, exp, H # c_p )
		logProb += this.pSister.logProb(this.sisterCond(P, H, type, false), S.category());
		// P( c_s | S # H, exp, P )
		logProb += this.pHeadLexCat.logProb(this.c_sCond(P, H, S, type, false), S.headLexCat());
		return logProb;
	}

	private ConditioningVariables c_sCond(LexCatChartItem P, LexCatChartItem H,
			LexCatChartItem S, int type, boolean cache) {
		if(cache) {
			ConditioningVariables c_sBackoff = VariablesFactory.cache(S.category());
			ConditioningVariables c_sRemainder = VariablesFactory.cache(P.category(), type, H.category());
			return VariablesFactory.cache(c_sBackoff, c_sRemainder);
		}
		ConditioningVariables c_sBackoff = VariablesFactory.get(S.category());
		ConditioningVariables c_sRemainder = VariablesFactory.get(P.category(), type, H.category());
		return VariablesFactory.get(c_sBackoff, c_sRemainder);
	}

	private ConditioningVariables sisterCond(LexCatChartItem P,
			LexCatChartItem H, int type, boolean cache) {
		if(cache) {
			ConditioningVariables sisterBackoff = VariablesFactory.cache(P.category(), type, H.category());
			ConditioningVariables sisterRemainder = VariablesFactory.cache(P.headLexCat());
			return VariablesFactory.cache(sisterBackoff, sisterRemainder);
		}
		ConditioningVariables sisterBackoff = VariablesFactory.get(P.category(), type, H.category());
		ConditioningVariables sisterRemainder = VariablesFactory.get(P.headLexCat());
		return VariablesFactory.get(sisterBackoff, sisterRemainder);
	}

	@Override
	public void addBeamPriorCounts(FineChartItem fineCI, Chart chart) throws Exception {
		LexCatChartItem ci = (LexCatChartItem) fineCI;
		int head = ci.headIndex();
		int word = chart.words[ci.headIndex()];
		int tag  = chart.tags[head];
		int cat = ci.category();
		// P(tag)
		super.addObservedCount(this.pBeamPOS, VariablesFactory.getEmpty(), tag);
		// P(word | tag)
		ConditioningVariables tagAsCond = VariablesFactory.cache(tag);
		super.addObservedCount(this.pBeamWord, tagAsCond, word);
		// P(cat | tag # word)
		ConditioningVariables wordAsCond = VariablesFactory.cache(word);
		ConditioningVariables catCond = VariablesFactory.cache(tagAsCond, wordAsCond);
		super.addObservedCount(this.pBeamCategory, catCond, cat);
	}

	@Override
	public double getBeamPriorLogProbability(FineChartItem fineCI, Chart chart) throws Exception {
		LexCatChartItem ci = (LexCatChartItem) fineCI;
		int headWord = chart.words[ci.headIndex()];
		return this.getBeamPriorLogProbability(headWord, chart.tags[ci.headIndex()], ci.headLexCat());
	}

	private double getBeamPriorLogProbability(int word, int tag, int cat) throws Exception {
		double logProb = Log.ONE;
		// P(tag)
		logProb += this.pBeamPOS.logProb(VariablesFactory.getEmpty(), tag);
		// P(word | tag)
		ConditioningVariables tagAsCond = VariablesFactory.get(tag);
		logProb += this.pBeamWord.logProb(tagAsCond, word);
		// P(cat | tag # word)
		ConditioningVariables wordAsCond = VariablesFactory.get(word);
		ConditioningVariables catCond = VariablesFactory.get(tagAsCond, wordAsCond);
		logProb += this.pBeamCategory.logProb(catCond, cat);
		return logProb;
	}

	@Override
	public double getFigureOfMerit(FineChartItem fineCI, Chart chart) throws Exception {
		double fom = fineCI.getViterbiProb() + this.getBeamPriorLogProbability(fineCI, chart);
		return fom;
	}



	public String prettyCond(ConditioningVariables cond,
			Distribution distribution) {
		String name = distribution.identifier;
		if(name.equals("pBeamHeadCategory")) {
			return "pos:<"+grammar.getLexFromID(cond.getVariableValue(0))+"> w:<"+grammar.getLexFromID(cond.getVariableValue(1))+">";
		}
		else if(name.equals("pBeamPOS")) {
			return "empty context";			
		}
		else if(name.equals("pBeamWord")) {
			return "pos:<"+grammar.getLexFromID(cond.getVariableValue(0))+">";
		}
		else if(name.equals("pExpansion")) {
			return "P:"+grammar.getCatFromID(cond.getVariableValue(0))
					+ " cp:"+grammar.getCatFromID(cond.getVariableValue(1));
		}
		else if(name.equals("pHeadCat")) {
			return "P:"+grammar.getCatFromID(cond.getVariableValue(0))
					+ " T:"+grammar.getLexFromID(cond.getVariableValue(1))
					+ " cp:"+grammar.getCatFromID(cond.getVariableValue(2));
		}
		else if(name.equals("pNonHeadCat")) {
			return "P:"+grammar.getCatFromID(cond.getVariableValue(0))
					+ " H:"+grammar.getCatFromID(cond.getVariableValue(2))
					+ " T:"+grammar.getLexFromID(cond.getVariableValue(1))
					+ " # cp:"+grammar.getCatFromID(cond.getVariableValue(3));
		}
		else if(name.equals("pWord")) {
			return "P:"+grammar.getCatFromID(cond.getVariableValue(0));
		}
		else if(name.equals("pLexCat")) {
			return "S:"+grammar.getCatFromID(cond.getVariableValue(0))
					+ " # H:"+grammar.getCatFromID(cond.getVariableValue(3))
					+ " T:"+grammar.getLexFromID(cond.getVariableValue(2))
					+ " P:"+grammar.getCatFromID(cond.getVariableValue(1));
		}
		else if(name.equals("pTopCat")) {
			return "empty context";	
		}
		return "null";
	}

	public String prettyOutcome(ConditioningVariables cond, int out, Distribution d) {
		if(d == this.pType) {
			return "T:"+grammar.getLexFromID(out);
		}
		if(d == this.pHead) {
			return "H:"+grammar.getCatFromID(out);
		}
		if(d == this.pWord) {
			return "w:<"+grammar.getLexFromID(out)+">";
		}
		if(d == this.pSister) {
			return "S:"+grammar.getCatFromID(out);
		}
		if(d == this.pHeadLexCat) {
			return "cs:"+grammar.getCatFromID(out);
		}
		if(d == this.pTopCat) {
			return "ctop:"+grammar.getCatFromID(out);
		}
		if(d == this.pBeamCategory) {
			return "beamC:"+grammar.getCatFromID(out);
		}
		if(d == this.pBeamPOS) {
			return "beam_cc:<"+grammar.getLexFromID(out)+">";
		}
		if(d == this.pBeamWord) {
			return "beam_cw:<"+grammar.getLexFromID(out)+">";
		}
		return "null";
	}

	private int getTypeID(String type) {
		int typeID;
		if(type.equals("Top")) {
			typeID = this.Top;
		}
		else if (type.equals("Lex")){
			typeID = this.Lex;
		}
		else if (type.equals("Unary")){
			typeID = this.Unary;
		}
		else if(type.equals("Right")) {
			typeID = this.Right;
		}
		else {
			typeID = this.Left;
		}
		return typeID;
	}

	public double getTypeLogProb(LexCatChartItem P, String type) throws Exception {
		int typeID = getTypeID(type);
		return this.pType.logProb(this.typeCond(P, false), typeID);
	}


	public double getHeadLogProb(LexCatChartItem P, LexCatChartItem H, String type) throws Exception {
		int typeID = getTypeID(type);
		return this.pHead.logProb(this.headCond(P, typeID, false), H.category());
	}


	public double getSisterLogProb(LexCatChartItem P, LexCatChartItem H, LexCatChartItem S, 
			String type) throws Exception {
		int typeID = getTypeID(type);
		return this.pSister.logProb(this.sisterCond(P, H, typeID, false), S.category());
	}

	public double getSisterLexCatLogProb(LexCatChartItem P, LexCatChartItem H, LexCatChartItem S, 
			String type) throws Exception {
		int typeID = getTypeID(type);
		return this.pHeadLexCat.logProb(this.c_sCond(P, H, S, typeID, false), S.headLexCat());
	}

	public double getTopLexCatLogProb(LexCatChartItem P, LexCatChartItem H) throws Exception {
		return this.pTopCat.logProb(this.ctopCond(H), P.headLexCat());
	}
}

class LexCatChartItem extends FineChartItem {
	private static final long serialVersionUID = -3011732632851184822L;

	private final int headLexCat;

	public LexCatChartItem(CoarseLexicalCategoryChartItem coarseItem) {
		super(coarseItem);
		headLexCat = coarseItem.category();
	}

	public LexCatChartItem(CoarseChartItem coarseItem, LexCatChartItem fineHead) {
		super(coarseItem, fineHead.headIndex);
		headLexCat = fineHead.headLexCat;
	}

	public int headLexCat() {
		return headLexCat;
	}

	@Override
	public int hashCode() {
		int result = 17;
		result = result * 31 + super.hashCode();
		result = result * 31 + headLexCat;
		return result;
	}

	@Override
	public boolean equals(Object oth) {
		if(this == oth) {
			return true;
		}
		if (oth == null || !(oth instanceof LexCatChartItem)) {
			return false;
		}
		LexCatChartItem o = (LexCatChartItem) oth;
		return super.equals(oth)
				&& o.headLexCat == this.headLexCat;		
	}


	@Override
	public String toString() {
		return super.toString()+", headLexCat="+coarseItem.cell.chart.getGrammar().getCatFromID(headLexCat);
	}
}
