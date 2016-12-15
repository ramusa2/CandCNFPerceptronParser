package perceptron.core;


public class TemporaryCandidateStructure extends CandidateStructure {
	
	private double score;
	private String[] featureNames;
	private double[] featureWeights;
	
	public TemporaryCandidateStructure(double s, String[] feats, double[] featWeights) {
		score = s;
		featureNames = feats;
		featureWeights = featWeights;
	}
	
	public double score() {
		return score;
	}
	
	public String[] featureNames() {
		return featureNames;
	}
	
	public double[] featureWeights() {
		return featureWeights;
	}
}
