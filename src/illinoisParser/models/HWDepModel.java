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
 * An implementation of Hockenmaier's ACL '02 HWDep model, in which the 
 * lexical head (category) is generated at its maximal projection
 *  
 * @author ramusa2
 */
public class HWDepModel extends Model {

	/**
	 * Model name (static)
	 */
	public static String MODEL_NAME = "hwdep";

	// Rule types (shared codebook values)
	protected Integer Lex,Left,Right,Unary,Top;

	// Model distributions
	protected Distribution pType, pHead, pSister, pHeadLexCat, pTopCat, pWord, pTopWord;

	// Beam search distributions
	private Distribution pBeamPOS, pBeamWord, pBeamCategory;

	public HWDepModel(Grammar g) {
		grammar = g;   
		setTypes();
		createDistributions();
	}

	protected void setTypes() {
		// Need shared codebook values:
		Left  = grammar.getTypeID("#LEFT#");
		Right = grammar.getTypeID("#RIGHT#");
		Unary = grammar.getTypeID("#UNARY#");
		Top   = grammar.getTypeID("#TOP#");
		Lex   = grammar.getTypeID("#LEX#");
	}

	protected void createDistributions() {
		// Add beam search distributions, just in case
		pBeamCategory = new ComplexDistribution(this, "pBeamHeadCategory",
				new Distribution(this, "pBeamHeadCategoryBackoff"));
		Distributions.add(pBeamCategory);
		pBeamPOS = new Distribution(this,"pBeamPOS");
		Distributions.add(pBeamPOS);
		pBeamWord = new Distribution(this,"pBeamWord");
		Distributions.add(pBeamWord);

		// Create model distributions
		pType = new ComplexDistribution(this,"pExpansion",
				new Distribution(this, "pExpansionBackoff"));

		pHead = new ComplexDistribution(this,"pHead",
				new Distribution(this, "pHeadBackoff"));

		pWord = new ComplexDistribution(this,"pWord",
				new Distribution(this, "pWordBackoff"));

		pTopWord = new Distribution(this,"pTopWord");

		pSister = new ComplexDistribution(this,"pNonHeadCat",
				new ComplexDistribution(this, "pNonHeadCatBackoff",
						new Distribution(this, "pNonHeadCatBackoff2")));

		pHeadLexCat = new ComplexDistribution(this,"pLexCat",
				new Distribution(this, "pLexCatBackoff"));

		pTopCat = new Distribution(this,"pTopCat");

		// Store model distributions
		Distributions.add(pType);
		Distributions.add(pHead);
		Distributions.add(pWord);
		Distributions.add(pTopWord);
		Distributions.add(pSister);
		Distributions.add(pHeadLexCat);
		Distributions.add(pTopCat);
	}

	@Override
	public FineChartItem getFineLexicalChartItem(CoarseLexicalCategoryChartItem ci, Chart coarseChart) {
		return new HWDepChartItem(ci);
	}

	@Override
	public void addCountsFromFineLexicalChartItem(FineChartItem fineLexicalCI,
			Chart coarseChart) {
		HWDepChartItem P = (HWDepChartItem) fineLexicalCI;
		int type = this.Lex;		
		try {
			// Beam search counts
			this.addBeamPriorCounts((HWDepChartItem) fineLexicalCI, coarseChart);
			// P(exp | P, c_p)			
			super.addObservedCount(this.pType, this.typeCond(P,  true), type);
		}
		catch (Exception e) {
			Util.logln("Failed to add lexical (beam) counts for: "+fineLexicalCI);
		}
	}

	@Override
	public void setLogProbabilityOfLexicalChartItem(FineChartItem fineLexicalCI,
			Chart coarseChart) {
		HWDepChartItem P = (HWDepChartItem) fineLexicalCI;
		int type = this.Lex;
		double logProb = Log.ONE;
		try {
			// P(exp | P, c_p)
			logProb += this.pType.logProb(this.typeCond(P, false), type);
		}
		catch (Exception e) {
			Util.logln("Failed to set lexical probability for: "+fineLexicalCI);
			logProb = Log.ZERO;
		}
		fineLexicalCI.setViterbiProb(logProb, null);
	}

	@Override
	public FineChartItem getFineUnaryChartItem(CoarseChartItem ci, FineChartItem fineChildCI) {
		HWDepChartItem fineChild = (HWDepChartItem) fineChildCI;
		return new HWDepChartItem(ci, fineChild);
	}

	@Override
	public void addCountsFromFineUnaryChartItem(FineChartItem fineParentCI,
			FineChartItem fineChildCI, Chart coarseChart) throws Exception {
		HWDepChartItem P = (HWDepChartItem) fineParentCI;
		HWDepChartItem H = (HWDepChartItem) fineChildCI;
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
				// P(w_TOP | c_p)
				super.addObservedCount(this.pTopWord, this.wtopCond(H, true), H.headWord());
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
		HWDepChartItem P = (HWDepChartItem) fineParentCI;
		HWDepChartItem H = (HWDepChartItem) fineChildCI;
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
			// P(w_TOP)
			logProb += this.pTopWord.logProb(this.wtopCond(H, false), H.headWord());
		}
		return logProb;
	}

	protected ConditioningVariables ctopCond(HWDepChartItem H) {
		return VariablesFactory.getEmpty();
	}

	private ConditioningVariables wtopCond(HWDepChartItem H, boolean cache) {
		if(cache) {
			return VariablesFactory.cache(H.headLexCat());
		}
		return VariablesFactory.get(H.headLexCat());
	}

	protected ConditioningVariables headCond(HWDepChartItem P, int type, boolean cache) {
		if(cache) {
			ConditioningVariables typeBackoff = VariablesFactory.cache(P.category(), type, P.headLexCat());
			ConditioningVariables typeRemainder = VariablesFactory.cache(P.headWord());
			return VariablesFactory.cache(typeBackoff, typeRemainder);
		}
		ConditioningVariables typeBackoff = VariablesFactory.get(P.category(), type, P.headLexCat());
		ConditioningVariables typeRemainder = VariablesFactory.get(P.headWord());
		return VariablesFactory.get(typeBackoff, typeRemainder);
	}

	public ConditioningVariables typeCond(HWDepChartItem P, boolean cache) {
		if(cache) {
			ConditioningVariables typeBackoff = VariablesFactory.cache(P.category(), P.headLexCat());
			ConditioningVariables typeRemainder = VariablesFactory.cache(P.headWord());
			return VariablesFactory.cache(typeBackoff, typeRemainder);
		}
		ConditioningVariables typeBackoff = VariablesFactory.get(P.category(), P.headLexCat());
		ConditioningVariables typeRemainder = VariablesFactory.get(P.headWord());
		return VariablesFactory.get(typeBackoff, typeRemainder);
	}

	@Override
	public FineChartItem getFineBinaryChartItem(CoarseChartItem ci,  BackPointer bp,
			FineChartItem fineLeftChildCI, FineChartItem fineRightChildCI) {
		HWDepChartItem fineHead;
		if(bp.direction() == Rule_Direction.Right) {
			fineHead = (HWDepChartItem) fineRightChildCI;
		}
		else {
			fineHead = (HWDepChartItem) fineLeftChildCI;
		}
		HWDepChartItem fineParent = new HWDepChartItem(ci, fineHead);
		return fineParent;
	}

	@Override
	public void addCountsFromFineBinaryChartItem(FineChartItem fineParentCI, FineBackPointer bp, Chart coarseChart) throws Exception {
		HWDepChartItem P = (HWDepChartItem) fineParentCI;
		// Add beam counts
		this.addBeamPriorCounts(P, coarseChart);
		HWDepChartItem H, S;
		int type;
		if(bp.direction() == Rule_Direction.Right) {
			H = (HWDepChartItem) bp.C();
			S = (HWDepChartItem) bp.B();
			type = this.Right;
		}
		else {
			H = (HWDepChartItem) bp.B();
			S = (HWDepChartItem) bp.C();
			type = this.Left;
		}		

		try {
			// P( exp | P, c_p # w_p)
			super.addObservedCount(this.pType, this.typeCond(P, true), type);
			// P( H | P, exp, c_p # w_p)
			super.addObservedCount(this.pHead, this.headCond(P, type, true), H.category());
			// P( S | P, exp, H # c_p # w_p)
			super.addObservedCount(this.pSister, this.sisterCond(P, H, type, true), S.category());
			// P( c_s | S # H, exp, P )
			super.addObservedCount(this.pHeadLexCat, this.c_sCond(P, H, S, type, false), S.headLexCat());
			// P( w_s | c_s # P, H, S, w_p )
			super.addObservedCount(this.pWord, this.w_sCond(P, H, S, false), S.headWord());
		}
		catch (Exception e) {
			Util.logln("Failed to add binary counts for: "+fineParentCI);
		}
	}

	@Override
	public double getLogBinaryRuleProbability(FineChartItem fineParentCI, FineBackPointer bp,
			Chart coarseChart) throws Exception {
		HWDepChartItem P = (HWDepChartItem) fineParentCI;
		HWDepChartItem H, S;
		int type;
		String pType = "";
		if(bp.direction() == Rule_Direction.Right) {
			H = (HWDepChartItem) bp.C();
			S = (HWDepChartItem) bp.B();
			type = this.Right;
			pType = "Right";
		}
		else {
			H = (HWDepChartItem) bp.B();
			S = (HWDepChartItem) bp.C();
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
		// P( w_s | c_s # P, H, S, w_p )
		logProb += this.pWord.logProb(this.w_sCond(P, H, S, false), S.headWord());
		return logProb;
	}

	protected ConditioningVariables c_sCond(HWDepChartItem P, HWDepChartItem H,
			HWDepChartItem S, int type, boolean cache) {
		if(cache) {
			ConditioningVariables c_sBackoff = VariablesFactory.cache(S.category());
			ConditioningVariables c_sRemainder = VariablesFactory.cache(P.category(), type, H.category());
			return VariablesFactory.cache(c_sBackoff, c_sRemainder);
		}
		ConditioningVariables c_sBackoff = VariablesFactory.get(S.category());
		ConditioningVariables c_sRemainder = VariablesFactory.get(P.category(), type, H.category());
		return VariablesFactory.get(c_sBackoff, c_sRemainder);
	}
	

	protected ConditioningVariables w_sCond(HWDepChartItem P, HWDepChartItem H,
			HWDepChartItem S, boolean cache) {
		if(cache) {
			ConditioningVariables w_sBackoff = VariablesFactory.cache(S.headLexCat());
			ConditioningVariables w_sRemainder = VariablesFactory.cache(P.category(), H.category(), S.category(), P.headWord());
			return VariablesFactory.cache(w_sBackoff, w_sRemainder);
		}
		ConditioningVariables w_sBackoff = VariablesFactory.get(S.headLexCat());
		ConditioningVariables w_sRemainder = VariablesFactory.get(P.category(), H.category(), S.category(), P.headWord());
		return VariablesFactory.get(w_sBackoff, w_sRemainder);
	}

	protected ConditioningVariables sisterCond(HWDepChartItem P,
			HWDepChartItem H, int type, boolean cache) {
		if(cache) {
			ConditioningVariables sisterBackoff = VariablesFactory.cache(P.category(), type, H.category());
			ConditioningVariables sisterRemainder = VariablesFactory.cache(P.headLexCat());
			ConditioningVariables sisterRemainder2 = VariablesFactory.cache(P.headWord());
			return VariablesFactory.cache(
					VariablesFactory.cache(sisterBackoff, sisterRemainder), 
					sisterRemainder2);
		}
		ConditioningVariables sisterBackoff = VariablesFactory.get(P.category(), type, H.category());
		ConditioningVariables sisterRemainder = VariablesFactory.get(P.headLexCat());
		ConditioningVariables sisterRemainder2 = VariablesFactory.get(P.headWord());
		return VariablesFactory.get(
				VariablesFactory.get(sisterBackoff, sisterRemainder), 
				sisterRemainder2);
	}

	@Override
	public void addBeamPriorCounts(FineChartItem fineCI, Chart chart) throws Exception {
		HWDepChartItem ci = (HWDepChartItem) fineCI;
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
		HWDepChartItem ci = (HWDepChartItem) fineCI;
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
					+ " cp:"+grammar.getCatFromID(cond.getVariableValue(1))
					+ " # wp:"+grammar.getCatFromID(cond.getVariableValue(2));
		}
		else if(name.equals("pHeadCat")) {
			return "P:"+grammar.getCatFromID(cond.getVariableValue(0))
					+ " T:"+grammar.getLexFromID(cond.getVariableValue(1))
					+ " cp:"+grammar.getCatFromID(cond.getVariableValue(2))
					+ " # wp:"+grammar.getCatFromID(cond.getVariableValue(3));
		}
		else if(name.equals("pNonHeadCat")) {
			return "P:"+grammar.getCatFromID(cond.getVariableValue(0))
					+ " H:"+grammar.getCatFromID(cond.getVariableValue(2))
					+ " T:"+grammar.getLexFromID(cond.getVariableValue(1))
					+ " # cp:"+grammar.getCatFromID(cond.getVariableValue(3))
					+ " # wp:"+grammar.getCatFromID(cond.getVariableValue(4));
		}
		else if(name.equals("pWord")) {
			return "cs:"+grammar.getCatFromID(cond.getVariableValue(0))
					+ " # P:"+grammar.getCatFromID(cond.getVariableValue(1))
					+ " H:"+grammar.getLexFromID(cond.getVariableValue(2))
					+ " S:"+grammar.getCatFromID(cond.getVariableValue(3))
					+ " wp:"+grammar.getCatFromID(cond.getVariableValue(4));
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

	public double getTypeLogProb(HWDepChartItem P, String type) throws Exception {
		int typeID = getTypeID(type);
		return this.pType.logProb(this.typeCond(P, false), typeID);
	}


	public double getHeadLogProb(HWDepChartItem P, HWDepChartItem H, String type) throws Exception {
		int typeID = getTypeID(type);
		return this.pHead.logProb(this.headCond(P, typeID, false), H.category());
	}


	public double getSisterLogProb(HWDepChartItem P, HWDepChartItem H, HWDepChartItem S, 
			String type) throws Exception {
		int typeID = getTypeID(type);
		return this.pSister.logProb(this.sisterCond(P, H, typeID, false), S.category());
	}

	public double getSisterLexCatLogProb(HWDepChartItem P, HWDepChartItem H, HWDepChartItem S, 
			String type) throws Exception {
		int typeID = getTypeID(type);
		return this.pHeadLexCat.logProb(this.c_sCond(P, H, S, typeID, false), S.headLexCat());
	}

	public double getTopLexCatLogProb(HWDepChartItem P, HWDepChartItem H) throws Exception {
		return this.pTopCat.logProb(this.ctopCond(H), P.headLexCat());
	}
}

class HWDepChartItem extends FineChartItem {
	private static final long serialVersionUID = -3011732632851184822L;

	private final int headLexCat, headWord;

	public HWDepChartItem(CoarseLexicalCategoryChartItem coarseItem) {
		super(coarseItem);
		headLexCat = coarseItem.category();
		headWord = coarseItem.cell.chart.words[headIndex];
	}

	public HWDepChartItem(CoarseChartItem coarseItem, HWDepChartItem fineHead) {
		super(coarseItem, fineHead.headIndex);
		headLexCat = fineHead.headLexCat;
		headWord = fineHead.headWord;
	}

	public int headLexCat() {
		return headLexCat;
	}

	public int headWord() {
		return headWord;
	}

	@Override
	public int hashCode() {
		int result = 17;
		result = result * 31 + super.hashCode();
		result = result * 31 + headLexCat;
		result = result * 31 + headWord;
		return result;
	}

	@Override
	public boolean equals(Object oth) {
		if(this == oth) {
			return true;
		}
		if (oth == null || !(oth instanceof HWDepChartItem)) {
			return false;
		}
		HWDepChartItem o = (HWDepChartItem) oth;
		return super.equals(oth)
				&& o.headLexCat == this.headLexCat
				&& o.headWord == this.headWord;
	}


	@Override
	public String toString() {
		return super.toString()+", headLexCat="+coarseItem.cell.chart.getGrammar().getCatFromID(headLexCat)
				+", headWord="+coarseItem.cell.chart.getGrammar().getLexFromID(headWord);
	}
}
