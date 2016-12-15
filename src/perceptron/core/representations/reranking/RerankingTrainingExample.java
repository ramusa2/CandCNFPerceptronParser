package perceptron.core.representations.reranking;

import java.util.ArrayList;
import java.util.Collection;

public class RerankingTrainingExample {
	
	private RerankingCandidate gold;
	
	private ArrayList<RerankingCandidate> candidates;

	public RerankingTrainingExample(RerankingCandidate goldCandidate) {
		gold = goldCandidate;
		candidates = new ArrayList<RerankingCandidate>();
	}
	
	public void addCandidate(RerankingCandidate newCandidate) {
		candidates.add(newCandidate);
	}
	
	public RerankingCandidate gold() {
		return gold;
	}
	
	public Collection<RerankingCandidate> candidates() {
		return candidates;
	}

}
