package illinoisParser;

public class FrontierTree implements Comparable<FrontierTree> {
	
	private final int leftK;
	
	private final int rightK;
	
	private final Tree<? extends FineChartItem> tree;
	
	private double ruleProb;
	
	/**
	 * Lexical category constructor
	 */
	public FrontierTree(Tree<? extends FineChartItem> t, double probOfRule) {
		tree = t;
		leftK = -1;
		rightK = -1;
		ruleProb = probOfRule;
	}
	/**
	 * Unary parent constructor
	 */
	public FrontierTree(Tree<? extends FineChartItem> t, int leftChildK, double probOfRule) {
		tree = t;
		leftK = leftChildK;
		rightK = -1;
		ruleProb = probOfRule;
	}
	
	/**
	 * Binary parent constructor
	 */
	public FrontierTree(Tree<? extends FineChartItem> t, int leftChildK, int rightChildK, double probOfRule) {
		tree = t;
		leftK = leftChildK;
		rightK = rightChildK;
		ruleProb = probOfRule;
	}
	
	public double prob() {
		return tree.probability();
	}
	
	public Tree<? extends FineChartItem> tree() {
		return tree;
	}
	
	public boolean isLexCat() {
		return tree.backPointer == null;
	}
	
	public boolean isUnary() {
		return leftK >= 0 && rightK == -1;
	}
	
	public int leftK() {
		return leftK;
	}
	
	public int rightK() {
		return rightK;
	}
	@Override
	public int compareTo(FrontierTree o) {
		return (int) Math.signum(o.tree.prob - this.tree.prob); // With PriorityQueue, we'll pick the lowest elements first
	}
	
	public double ruleProb() {
		return ruleProb;
	}
}
