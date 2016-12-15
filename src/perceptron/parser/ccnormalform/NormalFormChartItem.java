package perceptron.parser.ccnormalform;

import java.util.Collection;

import perceptron.parser.PerceptronChart;
import illinoisParser.CoarseChartItem;
import illinoisParser.CoarseLexicalCategoryChartItem;
import illinoisParser.FineBackPointer;
import illinoisParser.FineChartItem;
import illinoisParser.Grammar;

public class NormalFormChartItem extends FineChartItem {

	private static final long serialVersionUID = 1792726690089827198L;

	private final int headLexCat, headWord, headPOS;
	private final int dW, dP, dV;

	public NormalFormChartItem(CoarseLexicalCategoryChartItem coarseItem) {
		super(coarseItem);
		headLexCat = coarseItem.category();
		// TODO: if we don't recreate chart, we need to change this
		headWord = coarseItem.cell.chart.words[headIndex];
		headPOS = coarseItem.cell.chart.tags[headIndex];
		// Lexical dist = 0
		dW = 0;
		dP = 0;
		dV = 0;
	}

	public NormalFormChartItem(CoarseChartItem coarseItem, NormalFormChartItem fineHead) {
		super(coarseItem, fineHead.headIndex);
		headLexCat = fineHead.headLexCat;
		headWord = fineHead.headWord;
		headPOS = fineHead.headPOS;
		dW = fineHead.dW;
		dP = fineHead.dP;
		dV = fineHead.dV;
	}

	// Binary constructor
	public NormalFormChartItem(CoarseChartItem coarseItem, 
			NormalFormChartItem fineHead, int leftHead, int rightHead) {
		super(coarseItem, fineHead.headIndex);
		headLexCat = fineHead.headLexCat;
		headWord = fineHead.headWord;
		headPOS = coarseItem.cell.chart.tags[headIndex];
		dW = Math.min(3, rightHead - leftHead - 1);
		PerceptronChart chart = (PerceptronChart)coarseItem.cell.chart;
		dP = Math.min(3, chart.punctDist(leftHead, rightHead));
		dV = Math.min(2, chart.verbDist(leftHead, rightHead));
	}

	/**
	 * Constructor for fine root item (can't/doesn't store head information in equivalence class).
	 * @param category
	 */
	public NormalFormChartItem(CoarseChartItem coarseRoot) {
		super(coarseRoot, -1);
		headLexCat = -1;
		headWord = -1;
		headPOS = -1;
		dW = -1;
		dP = -1;
		dV = -1;
	}

	public int headLexCat() {
		return headLexCat;
	}

	public int headWord() {
		return headWord;
	}

	public int headPOS() {
		return headPOS;
	}

	public int dW() {
		return dW;
	}

	public int dP() {
		return dP;
	}

	public int dV() {
		return dV;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = result * 31 + this.headLexCat;
		return result;
	}

	@Override
	public boolean equals(Object oth) {
		if(this == oth) {
			return true;
		}
		if (oth == null || !(oth instanceof NormalFormChartItem)) {
			return false;
		}
		NormalFormChartItem o = (NormalFormChartItem) oth;
		return o.headLexCat == this.headLexCat
				&& dV == o.dV
				&& dP == o.dP
				&& dW == o.dW
				&& super.equals(oth);
	}


	@Override
	public String toString() {
		return super.toString()+", headLexCat="+coarseItem.cell.chart.getGrammar().getCatFromID(headLexCat)
				+", headWord="+coarseItem.cell.chart.getGrammar().getLexFromID(headWord)
				+", headPOS="+coarseItem.cell.chart.getGrammar().getLexFromID(headPOS);
	}
	
	public String toString(Grammar g, String tab) {
		return super.toString(g, tab)+
				"\n"+tab+"\theadLexCat="+g.getCatFromID(headLexCat)
				+"\n"+tab+"\theadWord="+g.getLexFromID(headWord)
				+"\n"+tab+"\theadPOS="+g.getLexFromID(headPOS);
	}
	
	public Collection<FineBackPointer> children() {
		return this.children;
	}
}
