package perceptron.core.representations.parsing;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Arrays;

public class FeatureNode implements Externalizable {

	private int[] features;

	private double viterbiScore;

	private FeatureBackPointer viterbiBP;

	private ArrayList<FeatureBackPointer> children;
	
	private boolean processed;
	
	private FeatureNode pruned;
	
	public FeatureNode(){}

	public FeatureNode(int[] nodeFeatures) {
		features = nodeFeatures;
		this.viterbiScore = Double.NEGATIVE_INFINITY;
		this.viterbiBP = null;
		this.children = null;	
		this.processed = false;
		this.pruned = null;
	}
	
	public void setProcessed(boolean newProcessed) {
		this.processed = newProcessed;
	}
	
	public boolean processed() {
		return processed;
	}
	
	public void setPruned(FeatureNode newPruned) {
		this.pruned = newPruned;
	}
	
	public FeatureNode pruned() {
		return pruned;
	}

	public void addChild(FeatureBackPointer bp) {
		if(this.children == null) {
			this.children = new ArrayList<FeatureBackPointer>();
		}
		this.children.add(bp);
	}

	public ArrayList<FeatureBackPointer> children() {
		return children;
	}

	public double viterbiScore() {
		return this.viterbiScore;
	}

	public FeatureBackPointer viterbi() {
		return this.viterbiBP;
	}

	public boolean isLeaf() {
		return this.children == null;
	}

	public int[] features() {
		return features;
	}

	public void checkAndSetViterbi(double score, FeatureBackPointer bp) {
		if(this.viterbiBP == null || score > this.viterbiScore) {
			this.viterbiBP = bp;
			this.viterbiScore = score;
		}
	}

	public boolean matches(FeatureNode node) {
		return Arrays.equals(this.features, node.features);
	}

	@Override
	public boolean equals(Object oth) {
		if(!(oth instanceof FeatureNode) || oth == null) {
			return this == oth;
		}
		return Arrays.equals(this.features, ((FeatureNode)oth).features);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(this.features);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(this.features);
		out.writeObject(this.children);
		out.writeDouble(this.viterbiScore);
		out.writeObject(this.viterbiBP);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void readExternal(ObjectInput in) throws IOException,
	ClassNotFoundException {
		this.features = (int[]) in.readObject();
		this.children = (ArrayList<FeatureBackPointer>) in.readObject();
		this.viterbiScore = in.readDouble();
		this.viterbiBP = (FeatureBackPointer) in.readObject();

	}
	
	public String toString() {
		String ret = "[";
		for(int f : this.features) {
			ret += f+", ";
		}
		if(ret.length() > 1) {
			ret = ret.substring(0, ret.length()-2);
		}
		ret += "] "+(this.isLeaf() ?  "LEAF" : "INTERNAL");
		return ret;
	}
}
