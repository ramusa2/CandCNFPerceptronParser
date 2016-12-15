package perceptron.core.representations.parsing;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class FeatureBackPointer implements Externalizable {
	
	private int[] features;
	
	private FeatureNode B, C;
	
	public FeatureBackPointer(){}
	
	public FeatureBackPointer(int[] bpFeatures, FeatureNode unaryChild) {
		this.features = bpFeatures;
		this.B = unaryChild;
		this.C = null;
	}
	
	public FeatureBackPointer(int[] bpFeatures, FeatureNode leftChild, FeatureNode rightChild) {
		this.features = bpFeatures;
		this.B = leftChild;
		this.C = rightChild;
	}
	
	public boolean isUnary() {
		return this.C == null;
	}
	
	public FeatureNode leftChild() {
		return B;
	}
	
	public FeatureNode rightChild() {
		return C;
	}
	
	public int[] features() {
		return features;
	}

	public double getViterbiScoreOfChildren() {
		return this.isUnary() ? this.B.viterbiScore() : this.B.viterbiScore() + this.C.viterbiScore();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(this.features);
		out.writeObject(this.B);
		out.writeObject(this.C);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		this.features = (int[]) in.readObject();
		this.B = (FeatureNode) in.readObject();
		this.C = (FeatureNode) in.readObject();
	}

	
	public String toString() {
		String ret = "[";
		for(int f : this.features) {
			ret += f+", ";
		}
		if(ret.length() > 1) {
			ret = ret.substring(0, ret.length()-2);
		}
		ret += "] "+(this.isUnary() ?  "UNARY" : "BINARY");
		return ret;
	}

}
