package perceptron.core;

import java.util.ArrayList;
import java.util.Collection;


public class PerceptronExample {
	
	private CandidateStructure best;
	private ArrayList<CandidateStructure> candidates;
	
	public PerceptronExample() {
		best = null;
		candidates = new ArrayList<CandidateStructure>();
	}
	
	public CandidateStructure best() {
		return best;
	}
	
	public boolean correct(CandidateStructure candidate) {
		return candidate.score() >= best.score();
	}
	
	public void add(CandidateStructure candidate) {
		if(best == null || candidate.score() > best.score()) {
			best = candidate;
		}
		candidates.add(candidate);
	}
	
	public Collection<CandidateStructure> candidates() {
		return candidates;
	}
}
