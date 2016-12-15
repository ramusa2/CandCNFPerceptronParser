package perceptron.core.representations.parsing;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import illinoisParser.AutoDecoder;
import illinoisParser.Chart;
import illinoisParser.FineBackPointer;
import illinoisParser.Sentence;
import perceptron.parser.PerceptronChart;
import perceptron.parser.PerceptronParser;
import perceptron.parser.ccnormalform.NormalFormChartItem;

public class PackedFeatureForest implements Externalizable {

	private FeatureNode root;

	private FeatureTree gold;
	
	public PackedFeatureForest(){}

	private PackedFeatureForest(FeatureNode myRoot, FeatureTree myGold) {
		this.root = myRoot;
		this.gold = myGold;
	}

	public PackedFeatureForest(PerceptronChart chart, PerceptronParser parser, boolean addNewFeaturesToParser) {
		this.gold = buildFeatureTreeFromAuto(chart.getSentence(), parser, addNewFeaturesToParser);
		if(chart.successfulFineParse()) {
			this.root = buildPackedForest(chart, parser, addNewFeaturesToParser);
		}
		else {
			this.root = null;
		}
	}

	private FeatureNode buildPackedForest(PerceptronChart chart,
			PerceptronParser parser, boolean addNewFeaturesToParser) {
		HashMap<NormalFormChartItem, FeatureNode> cached = new HashMap<NormalFormChartItem, FeatureNode>();
		return buildPackedFeatureForestRecurse((NormalFormChartItem) chart.fineRoot(), parser, cached, addNewFeaturesToParser);		
	}

	private FeatureNode buildPackedFeatureForestRecurse(NormalFormChartItem item,
			PerceptronParser parser, HashMap<NormalFormChartItem, FeatureNode> cached, boolean addNewFeaturesToParser) {
		FeatureNode cachedNode = cached.get(item);
		if(cachedNode != null) {
			return cachedNode;
		}
		FeatureNode node = new FeatureNode(parser.getFeaturesForItem(item, addNewFeaturesToParser));
		cached.put(item, node);
		for(FineBackPointer bp : item.children()) {
			int[] bpFeatures = parser.getFeaturesForBackPointer(item, bp, addNewFeaturesToParser);
			FeatureNode B = cached.get(bp.B());
			if(B == null) {
				B = buildPackedFeatureForestRecurse((NormalFormChartItem) bp.B(), parser, cached, addNewFeaturesToParser);
			}
			FeatureBackPointer fbp;
			if(bp.isUnary()) {
				fbp = new FeatureBackPointer(bpFeatures, B);
			}
			else {
				FeatureNode C = cached.get(bp.C());
				if(C == null) {
					C = buildPackedFeatureForestRecurse((NormalFormChartItem) bp.C(), parser, cached, addNewFeaturesToParser);
				}
				fbp = new FeatureBackPointer(bpFeatures, B, C);
			}
			node.addChild(fbp);
		}
		return node;
	}

	private FeatureTree buildFeatureTreeFromAuto(Sentence sen, PerceptronParser parser,
			boolean addNewFeaturesToParser) {
		AutoDecoder auto = new AutoDecoder(sen, sen.getCCGbankParse());
		Chart chart = auto.getFineChart(parser);
		FeatureNode goldTreeRoot = buildPackedFeatureForestRecurse((NormalFormChartItem) chart.fineRoot(),
				parser, new HashMap<NormalFormChartItem, FeatureNode>(), addNewFeaturesToParser);
		return buildFeatureTreeFromSingletonForest(goldTreeRoot);

	}

	private FeatureTree buildFeatureTreeFromSingletonForest(FeatureNode node) {
		if(node.isLeaf()) {
			return new FeatureTree(node.features());
		}
		FeatureBackPointer bp = node.children().get(0);
		if(bp.isUnary()) {
			return new FeatureTree(node.features(), 
					new FeatureTreeBackPointer(bp.features(), 
							buildFeatureTreeFromSingletonForest(bp.leftChild())));
		}
		return new FeatureTree(node.features(), 
				new FeatureTreeBackPointer(bp.features(), 
						buildFeatureTreeFromSingletonForest(bp.leftChild()),
						buildFeatureTreeFromSingletonForest(bp.rightChild())));
	}

	public boolean trainable() {
		return this.root != null;
	}

	public FeatureTree getViterbiTree(PerceptronParser parser) {	
		if(this.root == null) {
			return null;
		}
		this.calculateViterbi(this.root, parser);
		return this.buildViterbiTree(this.root);
	}

	private void calculateViterbi(FeatureNode node, PerceptronParser parser) {
		if(node.viterbiScore() != Double.NEGATIVE_INFINITY) {
			return;
		}
		if(node.isLeaf()) {
			node.checkAndSetViterbi(parser.score(node.features()), null);
			return;
		}
		double nodeScore = parser.score(node.features());
		for(FeatureBackPointer bp : node.children()) {
			calculateViterbi(bp.leftChild(), parser);
			if(!bp.isUnary()) {
				calculateViterbi(bp.rightChild(), parser);
			}
			double score = nodeScore + parser.score(bp.features())
					+bp.getViterbiScoreOfChildren();
			node.checkAndSetViterbi(score, bp);
		}
	}

	private FeatureTree buildViterbiTree(FeatureNode node) {
		if(node.isLeaf()) {
			return new FeatureTree(node.features());
		}
		FeatureBackPointer bp = node.viterbi();
		if(bp.isUnary()) {
			return new FeatureTree(node.features(), 
					new FeatureTreeBackPointer(bp.features(), 
							buildViterbiTree(bp.leftChild())));
		}
		return new FeatureTree(node.features(), 
				new FeatureTreeBackPointer(bp.features(), 
						buildViterbiTree(bp.leftChild()),
						buildViterbiTree(bp.rightChild())));
	}

	public FeatureTree getGoldTree() {
		return this.gold;
	}

	public PackedFeatureForest pruneFeatureForest(PackedFeatureForest unpruned,
			HashMap<Integer, Integer> featureMap) {
		FeatureNode newRoot = null;
		if(this.root != null) {
			newRoot = pruneFeatureForestRecurse(this.root, featureMap);
		}
		else {
			System.out.println("Warning: unpruned feature forest has a null root");
		}
		FeatureTree newGold = pruneFeatureTreeRecurse(this.gold, featureMap);
		return new PackedFeatureForest(newRoot, newGold);
	}

	private FeatureNode pruneFeatureForestRecurse(FeatureNode node,
			HashMap<Integer, Integer> featureMap) {
		FeatureNode cachedNewNode = node.pruned();
		if(cachedNewNode != null) {
			return cachedNewNode;
		}
		
		FeatureNode newNode = new FeatureNode(pruneFeatures(node.features(), featureMap));
		node.setPruned(newNode);
		if(node.isLeaf()) {
			return newNode;
		}

		for(FeatureBackPointer bp : node.children()) {
			int[] bpFeatures = pruneFeatures(bp.features(), featureMap);
			
			FeatureNode B = pruneFeatureForestRecurse(bp.leftChild(), featureMap);
			FeatureBackPointer fbp;
			if(bp.isUnary()) {
				fbp = new FeatureBackPointer(bpFeatures, B);
			}
			else {
				FeatureNode C = pruneFeatureForestRecurse(bp.rightChild(), featureMap);
				fbp = new FeatureBackPointer(bpFeatures, B, C);
			}
			newNode.addChild(fbp);
		}
		return newNode;
	}

	private FeatureTree pruneFeatureTreeRecurse(FeatureTree tree,
			HashMap<Integer, Integer> featureMap) {
		if(tree.isLeaf()) {
			return new FeatureTree(pruneFeatures(tree.features(), featureMap));
		}
		FeatureTreeBackPointer bp = tree.backpointer();
		FeatureTreeBackPointer newBP;
		if(bp.isUnary()) {
			FeatureTree newLeft = pruneFeatureTreeRecurse(bp.leftChild(), featureMap);
			newBP = new FeatureTreeBackPointer(pruneFeatures(bp.features(), featureMap), newLeft);
		}
		else {
			FeatureTree newLeft = pruneFeatureTreeRecurse(bp.leftChild(), featureMap);
			FeatureTree newRight = pruneFeatureTreeRecurse(bp.rightChild(), featureMap);
			newBP = new FeatureTreeBackPointer(pruneFeatures(bp.features(), featureMap), newLeft, newRight);
		}
		return new FeatureTree(pruneFeatures(tree.features(), featureMap), newBP);
	}

	private int[] pruneFeatures(int[] originalFeatures, HashMap<Integer, Integer> featureMap) {
		ArrayList<Integer> feats = new ArrayList<Integer>();
		for(int f : originalFeatures) {
			Integer newF = featureMap.get(f);
			if(newF != null) {
				feats.add(newF);
			}
		}
		int[] newFeats = new int[feats.size()];
		for(int f=0; f<feats.size(); f++) {
			newFeats[f] = feats.get(f);
		}
		return newFeats;
	}

	public HashSet<Integer> getActiveFeatures() {
		HashSet<Integer> active = new HashSet<Integer>();
		if(this.root != null) {
			this.getActiveFeaturesRecurse(this.root, active);
		}
		this.getGoldFeaturesRecurse(this.gold, active);
		return active;
	}

	private void getGoldFeaturesRecurse(FeatureTree tree,
			HashSet<Integer> active) {
		for(int f : tree.features()) {
			active.add(f);
		}
		if(!tree.isLeaf()) {
			FeatureTreeBackPointer bp = tree.backpointer();
			for(int f : bp.features()) {
				active.add(f);
			}
			getGoldFeaturesRecurse(bp.leftChild(), active);
			if(!bp.isUnary()) {
				getGoldFeaturesRecurse(bp.rightChild(), active);
			}
		}
	}

	private void getActiveFeaturesRecurse(FeatureNode node,
			HashSet<Integer> active) {
		if(node.processed()) {
			return;
		}
		node.setProcessed(true);
		for(int f : node.features()) {
			active.add(f);
		}
		if(!node.isLeaf()) {
			for(FeatureBackPointer bp : node.children()) {
				for(int f : bp.features()) {
					active.add(f);
				}
				getActiveFeaturesRecurse(bp.leftChild(), active);
				if(!bp.isUnary()) {
					getActiveFeaturesRecurse(bp.rightChild(), active);
				}
			}
		}
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(this.root);
		out.writeObject(this.gold);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		this.root = (FeatureNode) in.readObject();
		this.gold = (FeatureTree) in.readObject();
	}

	public boolean containsTree(FeatureTree goldTree) {
		if(this.root == null) {
			return false;
		}
		return checkForestContainsTree(this.root, goldTree);
	}

	private boolean checkForestContainsTree(FeatureNode forestNode,
			FeatureTree treeNode) {
		if(!Arrays.equals(forestNode.features(), treeNode.features())) {
			return false;
		}
		if(forestNode.isLeaf() || treeNode.isLeaf()) {
			return forestNode.isLeaf() && treeNode.isLeaf();
		}
		FeatureTreeBackPointer treeBP = treeNode.backpointer();
		for(FeatureBackPointer bp : forestNode.children()) {
			if(Arrays.equals(bp.features(), treeBP.features())) {
				boolean matches = true;
				if(bp.isUnary() || treeBP.isUnary()) {
					matches = bp.isUnary() && treeBP.isUnary() 
							&& checkForestContainsTree(bp.leftChild(), treeBP.leftChild());
				}
				else {
					matches = checkForestContainsTree(bp.leftChild(), treeBP.leftChild())
							&& checkForestContainsTree(bp.rightChild(), treeBP.rightChild());
				}
				if(matches) {
					return true;
				}
			}
		}
		return false;
	}
}
