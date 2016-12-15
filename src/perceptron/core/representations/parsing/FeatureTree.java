package perceptron.core.representations.parsing;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.HashMap;

public class FeatureTree implements Externalizable {

	private int[] features;
	private FeatureTreeBackPointer bp;
	
	public FeatureTree(){}
	
	public FeatureTree(int[] myFeatures) {
		this.features = myFeatures;
		this.bp = null;
	}
	
	public FeatureTree(int[] myFeatures, FeatureTreeBackPointer myBP) {
		this.features = myFeatures;
		this.bp = myBP;
	}
	
	public boolean isLeaf() {
		return this.bp == null;
	}
	
	public FeatureTreeBackPointer backpointer() {
		return this.bp;
	}

	public boolean matches(FeatureTree other) {
		if(this.isLeaf() && other.isLeaf()) {
			return Arrays.equals(this.features,  other.features);
		}
		if(!this.isLeaf() && !other.isLeaf()) {
			return this.bp.matches(other.bp);
		}
		return false;
	}

	public int[] features() {
		return features;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(this.features);
		out.writeObject(this.bp);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
	ClassNotFoundException {
		this.features = (int[]) in.readObject();
		this.bp = (FeatureTreeBackPointer) in.readObject();
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

	public HashMap<Integer, Integer> getMapOfActiveFeatureCounts() {
		HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
		getMapOfActiveFeaturesRecurse(map);
		return map;
	}

	private void getMapOfActiveFeaturesRecurse(HashMap<Integer, Integer> map) {
		incrementFeatureCounts(this.features, map);
		if(!this.isLeaf()) {
			incrementFeatureCounts(this.bp.features(), map);
			this.bp.leftChild().getMapOfActiveFeaturesRecurse(map);
			if(!this.bp.isUnary()) {
				this.bp.rightChild().getMapOfActiveFeaturesRecurse(map);
			}
		}
	}

	private void incrementFeatureCounts(int[] feats,
			HashMap<Integer, Integer> map) {
		for(int f : feats) {
			Integer c = map.get(f);
			if(c == null) {
				c = 0;
			}
			map.put(f, c+1);
		}
	}
}
