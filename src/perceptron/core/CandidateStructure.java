package perceptron.core;

import java.util.ArrayList;

public class CandidateStructure {
	
	private double score;
	private int[] features;
	private double[] featureWeights;
	
	protected CandidateStructure() {}
	
	public CandidateStructure(double s, int[] feats, double[] featWeights) {
		score = s;
		features = feats;
		featureWeights = featWeights;
	}
	
	public CandidateStructure(double s, ArrayList<Integer> feats, ArrayList<Double> featWeights) {
		score = s;
		int l = feats.size();
		features = new int[l];
		featureWeights = new double[l];
		for(int i=0; i<l; i++) {
			features[i] = feats.get(i);
			featureWeights[i] = featWeights.get(i);
		}
	}
	
	public double score() {
		return score;
	}
	
	public int[] features() {
		return features;
	}
	
	public double[] featureWeights() {
		return featureWeights;
	}
}
