package illinoisParser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.PriorityQueue;

/**
 * A FineChartItem is an augmented version of the CoarseChartItem used for CKY parsing; 
 * the fine item contains probability/scoring information, as well as extra variables
 * used in defining the equivalence class.
 * 
 * @author ramusa2
 *
 */
abstract public class FineChartItem implements Serializable {

	private static final long serialVersionUID = 1977257783899634643L;

	protected final CoarseChartItem coarseItem;

	public final int headIndex;

	protected HashSet<FineBackPointer> children;
	//protected ArrayList<FineBackPointer> children;

	private ArrayList<Tree<? extends FineChartItem>> topK;
	private PriorityQueue<FrontierTree> frontier;

	private double viterbiProb;
	private FineBackPointer viterbiBP;
	private Tree<? extends FineChartItem> viterbiTree;

	public FineChartItem(CoarseChartItem coarse, int head) {
		coarseItem = coarse;
		headIndex = head;
		//children = new ArrayList<FineBackPointer>();
		children = new HashSet<FineBackPointer>();
		viterbiProb = Log.ZERO;
		viterbiBP = null;
		viterbiTree = null;
	}

	public FineChartItem(CoarseLexicalCategoryChartItem coarse) {
		coarseItem = coarse;
		headIndex = coarse.X();
		//children = new ArrayList<FineBackPointer>();
		children = new HashSet<FineBackPointer>();
		viterbiProb = Log.ONE;
		viterbiBP = null;
		viterbiTree = null;
	}

	public int headIndex() {
		return headIndex;
	}


	public int X() {
		return coarseItem.X();
	}

	public int Y() {
		return coarseItem.Y();
	}

	public void addChild(FineBackPointer bp) {
		children.add(bp);
	}
	
	public Collection<FineBackPointer> children() {
		return this.children;
	}

	public int category() {
		return coarseItem.category();
	}

	public void setViterbiProb(double vitProb, FineBackPointer bp) {
		if(vitProb >= this.viterbiProb) {
			this.viterbiProb = vitProb;
			this.viterbiBP = bp;
		}
		if(bp != null) {
			this.children.add(bp);
		}
	}

	public double getViterbiProb() {
		return this.viterbiProb;
	}

	public FineBackPointer getViterbiBP() {
		return viterbiBP;
	}

	public Tree<? extends FineChartItem> getViterbiTree(Chart chart) {
		if(this.viterbiTree == null) {
			this.viterbiTree = this.createViterbiTree(chart);
		}
		return viterbiTree;
	}

	public ArrayList<Tree<? extends FineChartItem>> getKBestTrees(Model m, int k) {
		this.getKthBestTree(m, k);
		ArrayList<Tree<? extends FineChartItem>> upToK = new ArrayList<Tree<? extends FineChartItem>>();
		for(int i=0; i<k && i <topK.size(); i++) {
			upToK.add(topK.get(i));
		}
		return upToK;
	}

	public Tree<? extends FineChartItem> getKthBestTree(Model m, int k) {
		// If k=0, return the viterbi tree
		if(k == 0) {
			if(viterbiTree == null) {
				viterbiTree = this.createViterbiTree(this.chart());
			}
			return viterbiTree;
		}
		// If this is a lexical category, we only have one possible tree -- return null, since k > 0
		if(this.coarseItem instanceof CoarseLexicalCategoryChartItem) {
			return null;
		}
		// Otherwise, for internal nodes:
		if(topK != null && topK.size() > k) {
			// If we already calculated this tree, just return it
			return topK.get(k);
		}
		if(frontier == null) {
			// If we haven't calculated any topK for this node yet, initialize fields and start from scratch
			if(topK == null) {
				topK = new ArrayList<Tree<? extends FineChartItem>>();
			}
			frontier = new PriorityQueue<FrontierTree>();
			// For each backpointer, add the 1-best tree to the frontier
			for(FineBackPointer bp : this.children) {
				FrontierTree next;
				if(bp.isUnary()) {
					Tree<? extends FineChartItem> childTree = bp.B().getKthBestTree(m, 0);
					double ruleProb;
					try {
						ruleProb = m.getLogUnaryRuleProbability(this, bp.B(), this.chart());
					}
					catch(Exception e) {
						ruleProb = Log.ZERO;
					}
					double prob = ruleProb + childTree.probability();
					Tree<? extends FineChartItem> tree = new Tree(this, bp, prob, childTree);
					next = new FrontierTree(tree, 0, ruleProb);
				}
				else {
					Tree<? extends FineChartItem> leftChildTree = bp.B().getKthBestTree(m, 0);
					Tree<? extends FineChartItem> rightChildTree = bp.C().getKthBestTree(m, 0);
					double ruleProb;
					try {
						ruleProb = m.getLogBinaryRuleProbability(this, bp, this.chart());
					}
					catch(Exception e) {
						ruleProb = Log.ZERO;
					}
					double prob = ruleProb + leftChildTree.probability() + rightChildTree.probability();
					Tree<? extends FineChartItem> tree = new Tree(this, bp, prob, leftChildTree, rightChildTree);
					next = new FrontierTree(tree, 0, 0, ruleProb);
				}
				frontier.add(next);
			}
		}
		// Now that we know frontier is populated, fill the topK list with k trees.
		while(topK.size() <= k && !frontier.isEmpty()) {			
			this.popAndUpdateFrontier(m);
		}
		// Return the kth-best tree...
		if(topK.size() > k) {
			return topK.get(k);
		}
		// ... or null if fewer than k trees exist.
		return null;
	}

	private Chart chart() {
		return this.coarseItem.cell.chart;
	}

	private void popAndUpdateFrontier(Model m) {
		FrontierTree next = frontier.poll();
		if(next == null) {
			return; // frontier empty
		}
		topK.add(next.tree());
		FineBackPointer bp = next.tree().backPointer;
		double ruleProb = next.ruleProb();
		if(next.isUnary()) {
			Tree<? extends FineChartItem> childNext = bp.B().getKthBestTree(m, next.leftK()+1);
			if(childNext != null) {
				double prob = ruleProb + childNext.probability();
				Tree<? extends FineChartItem> tree = new Tree(this, bp, prob, childNext);
				FrontierTree shoulder = new FrontierTree(tree, next.leftK()+1, ruleProb);
				if(!frontier.contains(shoulder)) {
					frontier.add(shoulder);
				}
			}
		}
		else {
			// Use new left and old right child
			Tree<? extends FineChartItem> newLeft = bp.B().getKthBestTree(m, next.leftK()+1);
			Tree<? extends FineChartItem> oldRight = next.tree().C();
			if(newLeft != null) {
				double lprob = ruleProb + newLeft.probability() + oldRight.probability();
				Tree<? extends FineChartItem> ltree = new Tree(this, bp, lprob, newLeft, oldRight);
				FrontierTree shoulder = new FrontierTree(ltree, next.leftK()+1, next.rightK(), ruleProb);
				if(!frontier.contains(shoulder)) {
					frontier.add(shoulder);
				}
			}

			// Use old left and new right child
			Tree<? extends FineChartItem> oldLeft = next.tree().B();
			Tree<? extends FineChartItem> newRight = bp.C().getKthBestTree(m, next.rightK()+1);
			if(newRight != null) {
				double rprob = ruleProb + oldLeft.probability() + newRight.probability();
				Tree<? extends FineChartItem> rtree = new Tree(this, bp, rprob, oldLeft, newRight);
				FrontierTree shoulder = new FrontierTree(rtree, next.leftK(), next.rightK()+1, ruleProb);
				if(!frontier.contains(shoulder)) {
					frontier.add(shoulder);
				}
			}
		}
	}

	public Tree<? extends FineChartItem> createViterbiTree(Chart chart) {
		if(this.viterbiBP == null) {
			if(coarseItem instanceof CoarseLexicalCategoryChartItem) {
				int index = coarseItem.X();
				//int wordOrUnkID = chart.words[index];
				int realWordID = chart.getGrammar().getWordID(chart.getSentence().get(index).getWord());
				Rule r = chart.getGrammar().getProductionRule(coarseItem.category(), realWordID);
				return Tree.getLexicalCategoryTree(this, r);
			}
			return null;
		}
		if (this.viterbiBP.C() == null) {
			return new Tree(this, this.viterbiBP, this.viterbiProb, this.viterbiBP.B().createViterbiTree(chart));
		}
		else {
			return new Tree(this, this.viterbiBP, this.viterbiProb, 
					this.viterbiBP.B().createViterbiTree(chart), this.viterbiBP.C().createViterbiTree(chart));
		}
	}
	@Override
	public int hashCode() {
		int result = 17;
		result = result * 31 + coarseItem.hashCode();
		result = result * 31 + headIndex;
		return result;
	}

	@Override
	public boolean equals(Object oth) {
		if(this == oth) {
			return true;
		}
		if (oth == null || !(oth instanceof FineChartItem)) {
			return false;
		}
		FineChartItem o = (FineChartItem) oth;
		return o.headIndex == this.headIndex
				&& o.coarseItem.equals(this.coarseItem);
	}


	@Override
	public String toString() {
		return coarseItem.toString()+", logprob="+viterbiProb +", headIndex="+headIndex;
	}
	public String toString(Grammar g, String tab) {
		return coarseItem.toString(g)+"\n"+tab+"\theadIndex="+headIndex;
	}

	public boolean isLeaf() {
		return this.children.isEmpty();
	}

	public CoarseChartItem coarseItem() {
		return this.coarseItem;
	}
}
