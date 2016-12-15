package perceptron.core.representations.reranking;

public class RerankingCandidate {
	
	private final int[] features;
	
	private final double score;

	public RerankingCandidate(double myScore, int... activeFeatures) {
		score = myScore;
		features = activeFeatures;
	}
	
	public double score() {
		return score;
	}
	
	public int[] features() {
		return features;
	}

}
