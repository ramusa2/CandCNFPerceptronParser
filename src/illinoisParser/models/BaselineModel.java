package illinoisParser.models;

import illinoisParser.BackPointer;
import illinoisParser.Chart;
import illinoisParser.CoarseChartItem;
import illinoisParser.CoarseLexicalCategoryChartItem;
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

public class BaselineModel extends Model {
	// Rule types (shared codebook values)
	private Integer Lex,Left,Right,Unary,Top;

	// Model distributions
	protected Distribution pType, pHead, pSister;

	public BaselineModel(Grammar g) {
		grammar = g;  
		setTypes();
		createDistributions();
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
		// Create model distributions
		pType = new Distribution(this,"pExpansion");

		pHead = new Distribution(this,"pHeadCat");

		pSister = new Distribution(this,"pNonHeadCat");

		// Store model distributions
		Distributions.add(pType);
		Distributions.add(pHead);
		Distributions.add(pSister);
	}

	@Override
	public void addBeamPriorCounts(FineChartItem fineCI, Chart chart) throws Exception {}

	@Override
	public double getBeamPriorLogProbability(FineChartItem fineCI, Chart chart) throws Exception {
		return Log.ONE;
	}

	@Override
	public double getFigureOfMerit(FineChartItem fineCI, Chart chart) throws Exception {
		return fineCI.getViterbiProb();
	}

	@Override
	public FineChartItem getFineLexicalChartItem(
			CoarseLexicalCategoryChartItem ci, Chart coarseChart) {
		return new BaselineChartItem(ci);
	}

	@Override
	public void addCountsFromFineLexicalChartItem(FineChartItem fineLexicalCI,
			Chart coarseChart) {
		BaselineChartItem P = (BaselineChartItem) fineLexicalCI;
		int word = coarseChart.words[P.X()];
		int type = this.Lex;		
		try {
			this.addBeamPriorCounts((BaselineChartItem) fineLexicalCI, coarseChart);
			// P( Lex | P)			
			super.addObservedCount(this.pType, this.typeCond(P,  true), type);
			// P( H | P, Lex )
			super.addObservedCount(this.pHead, this.headCond(P, type, true), word);
		}
		catch (Exception e) {
			e.printStackTrace();
			Util.Error("Failed to add lexical (beam) counts for: "+fineLexicalCI);
		}
	}

	public ConditioningVariables typeCond(BaselineChartItem P, boolean cache) {
		if(cache) {
			return VariablesFactory.cache(P.category());
		}
		return VariablesFactory.get(P.category());	
	}

	@Override
	public void setLogProbabilityOfLexicalChartItem(
			FineChartItem fineLexicalCI, Chart coarseChart) throws Exception {
		BaselineChartItem P = (BaselineChartItem) fineLexicalCI;
		int word = coarseChart.words[P.X()];
		int type = this.Lex;
		double logProb = Log.ONE;
		try {
			// P(exp | P, c_p)
			logProb += this.pType.logProb(this.typeCond(P, false), type);
			// P(H | P, exp, c_p)
			logProb += this.pHead.logProb(this.headCond(P, type, false), word);
		}
		catch (Exception e) {
			Util.Error("Failed to set lexical probability for: "+fineLexicalCI);
			logProb = Log.ZERO;
		}
		fineLexicalCI.setViterbiProb(logProb, null);
	}

	@Override
	public FineChartItem getFineUnaryChartItem(CoarseChartItem ci,
			FineChartItem fineChildCI) {
		return new BaselineChartItem(ci);
	}

	@Override
	public void addCountsFromFineUnaryChartItem(FineChartItem fineParentCI,
			FineChartItem fineChildCI, Chart coarseChart) throws Exception {
		BaselineChartItem P = (BaselineChartItem) fineParentCI;
		BaselineChartItem H = (BaselineChartItem) fineChildCI;
		// Add beam counts
		this.addBeamPriorCounts(P, coarseChart);
		int type = (P.category() == grammar.getTopCatID()) ? this.Top : this.Unary;
		try {
			// P(exp | P, c_p)			
			super.addObservedCount(this.pType, this.typeCond(P,  true), type);
			// P(H | P, exp, c_p)
			super.addObservedCount(this.pHead, this.headCond(P, type, true), H.category());
		}
		catch (Exception e) {
			e.printStackTrace();
			Util.Error("Failed to add unary counts for: "+fineParentCI);
		}
	}

	@Override
	public double getLogUnaryRuleProbability(
			FineChartItem fineParentCI, FineChartItem fineChildCI,
			Chart coarseChart) throws Exception {
		BaselineChartItem P = (BaselineChartItem) fineParentCI;
		BaselineChartItem H = (BaselineChartItem) fineChildCI;
		int type = (P.category() == grammar.getTopCatID()) ? this.Top : this.Unary;
		double logProb = Log.ONE;
		try {
			// P(exp | P, c_p)
			logProb += this.pType.logProb(this.typeCond(P, false), type);
			// P(H | P, exp, c_p)
			logProb += this.pHead.logProb(this.headCond(P, type, false), H.category());
		}
		catch (Exception e) {
			e.printStackTrace();
			Util.Error("Failed to set lexical probability for: "+fineParentCI);
			logProb = Log.ZERO;
		}
		return logProb;
	}

	@Override
	public FineChartItem getFineBinaryChartItem(CoarseChartItem ci,
			BackPointer bp, FineChartItem fineLeftChildCI,
			FineChartItem fineRightChildCI) {
		BaselineChartItem fineParent = new BaselineChartItem(ci);
		return fineParent;
	}

	@Override
	public void addCountsFromFineBinaryChartItem(FineChartItem fineParentCI,
			FineBackPointer bp, Chart coarseChart) throws Exception {
		BaselineChartItem P = (BaselineChartItem) fineParentCI;
		BaselineChartItem H, S;
		int type;
		if(bp.direction() == Rule_Direction.Right) {
			H = (BaselineChartItem) bp.C();
			S = (BaselineChartItem) bp.B();
			type = this.Right;
		}
		else {
			H = (BaselineChartItem) bp.B();
			S = (BaselineChartItem) bp.C();
			type = this.Left;
		}
		// Add beam counts
		this.addBeamPriorCounts(P, coarseChart);
		try {
			// P(exp | P)			
			super.addObservedCount(this.pType, this.typeCond(P,  true), type);
			// P(H | P, exp)
			super.addObservedCount(this.pHead, this.headCond(P, type, true), H.category());
			// P(S | P, exp, c_p)
			super.addObservedCount(this.pSister, this.sisterCond(P, type, H, true), S.category());
		}
		catch (Exception e) {
			e.printStackTrace();
			Util.Error("Failed to add unary counts for: "+fineParentCI);
		}
	}

	private ConditioningVariables headCond(BaselineChartItem P, int type,
			boolean cache) {
		if(cache) {
			return VariablesFactory.cache(P.category(), type);
		}
		return VariablesFactory.get(P.category(), type);	
	}

	private ConditioningVariables sisterCond(BaselineChartItem P, int type, BaselineChartItem H,
			boolean cache) {
		if(cache) {
			return VariablesFactory.cache(P.category(), type, H.category());
		}
		return VariablesFactory.get(P.category(), type, H.category());	
	}

	@Override
	public double getLogBinaryRuleProbability(
			FineChartItem fineParentCI, FineBackPointer bp, Chart coarseChart)
			throws Exception {
		BaselineChartItem P = (BaselineChartItem) fineParentCI;
		BaselineChartItem H, S;
		int type;
		if(bp.direction() == Rule_Direction.Right) {
			H = (BaselineChartItem) bp.C();
			S = (BaselineChartItem) bp.B();
			type = this.Right;
		}
		else {
			H = (BaselineChartItem) bp.B();
			S = (BaselineChartItem) bp.C();
			type = this.Left;
		}
		double logProb = Log.ONE;
		try {
			// P(exp | P, c_p)
			logProb += this.pType.logProb(this.typeCond(P, false), type);
			// P(H | P, exp, c_p)
			logProb += this.pHead.logProb(this.headCond(P, type, false), H.category());
			// P(S | P, exp, c_p)
			logProb += this.pSister.logProb(this.sisterCond(P, type, H, true), S.category());
		}
		catch (Exception e) {
			e.printStackTrace();
			Util.Error("Failed to set lexical probability for: "+fineParentCI);
			logProb = Log.ZERO;
		}
		return logProb;
	}

}
class BaselineChartItem extends FineChartItem {

	private static final long serialVersionUID = 7294182791232731321L;

	public BaselineChartItem(CoarseChartItem coarseItem) {
		super(coarseItem, coarseItem.X());
	}

	public BaselineChartItem(CoarseLexicalCategoryChartItem coarseItem) {
		super(coarseItem);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public boolean equals(Object oth) {
		if(this == oth) {
			return true;
		}
		if (oth == null || !(oth instanceof BaselineChartItem)) {
			return false;
		}
		return super.equals(oth);
	}


	@Override
	public String toString() {
		return super.toString();
	}
}
