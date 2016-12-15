package perceptron.core.representations.parsing;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

public class FeatureTreeBackPointer implements Externalizable {
	
	private int[] features;
	
	private FeatureTree B, C;
	
	public FeatureTreeBackPointer(){}
	
	public FeatureTreeBackPointer(int[] bpFeatures, FeatureTree unaryChild) {
		this.features = bpFeatures;
		this.B = unaryChild;
		this.C = null;
	}
	
	public FeatureTreeBackPointer(int[] bpFeatures, FeatureTree leftChild, FeatureTree rightChild) {
		this.features = bpFeatures;
		this.B = leftChild;
		this.C = rightChild;
	}
	
	public boolean isUnary() {
		return this.C == null;
	}
	
	public FeatureTree leftChild() {
		return B;
	}
	
	public FeatureTree rightChild() {
		return C;
	}
	
	public int[] features() {
		return features;
	}
	
	public boolean matches(FeatureTreeBackPointer other) {
		if(!Arrays.equals(this.features, other.features)) {
			return false;
		}
		if(this.B.matches(other.B)) {
			if(!this.isUnary() && !other.isUnary()) {
				return this.C.matches(other.C);
			}
			return true;
		}
		return false;
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
		this.B = (FeatureTree) in.readObject();
		this.C = (FeatureTree) in.readObject();
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
