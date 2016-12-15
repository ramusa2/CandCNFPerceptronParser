package perceptron.reranker;

import illinoisParser.FineChartItem;
import illinoisParser.Sentence;
import illinoisParser.Tree;

import java.util.Collection;
import perceptron.core.CandidateStructure;
import perceptron.core.PerceptronExample;
import perceptron.core.PerceptronModel;
import perceptron.core.TemporaryCandidateStructure;

public class PerceptronReranker extends PerceptronModel {

	protected PerceptronReranker(Collection<String> featureNames,
			boolean useRelativeUpdate) {
		//super(featureNames, useRelativeUpdate);
		// TODO Auto-generated constructor stub
	}

	protected void prepareData(Collection<Sentence> trainingData) {
		// TODO Auto-generated method stub

	}

	protected void initializeModel() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void train() {
		// TODO Auto-generated method stub

	}

	@Override
	public Tree<? extends FineChartItem> classify(Sentence sen) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected TemporaryCandidateStructure getInitialCandidateWithFeatures(
			Sentence sen) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CandidateStructure getBestStructure(PerceptronExample ex) {
		CandidateStructure modelBest = null;
		double maxSc = Double.NEGATIVE_INFINITY;
		for(CandidateStructure candidate : ex.candidates()) {
			double sc = this.score(candidate);
			if(sc > maxSc || modelBest == null) {
				modelBest = candidate;
				maxSc = sc;
			}
		}
		return modelBest;
	}

	/*

	public PerceptronReranker trainPerceptronReranker(Grammar g, Collection<TopKParserResult> sentences) {
		// Initialize set of frequent features
		int minFeatureFreq = 5;
		Collection<String> commonFeatures = getCommonFeatures(g, sentences, minFeatureFreq);
		PerceptronReranker reranker = new PerceptronReranker(commonFeatures, false);

		// Convert topK results to instance space
		PerceptronTrainingData data = convertToInstanceSpace(g, reranker, sentences);

		// Train
		int numIterations = 10;
		reranker.train(data, numIterations);

		return reranker;
	}

	private Collection<String> getCommonFeatures(Grammar g,
			Collection<TopKParserResult> sentences, int minFreq) {
		HashMap<String, Integer> freqs = new HashMap<String, Integer>();
		for(TopKParserResult res : sentences) {
			for(Parse r : res.results()) {
				for(String feat : extractRawFeatures(g, r)) {
					Integer f = freqs.get(feat);
					if(f==null) {
						f = 0;
					}
					freqs.put(feat, f+1);
				}
			}
		}		
		ArrayList<String> common = new ArrayList<String>();
		for(String feat : freqs.keySet()) {
			if(freqs.get(feat) >= minFreq) {
				common.add(feat);
			}
		}
		return common;
	}


	private PerceptronTrainingData convertToInstanceSpace(Grammar g,
			PerceptronReranker reranker, Collection<TopKParserResult> sentences) {
		PerceptronTrainingData data = new PerceptronTrainingData();
		for(TopKParserResult res : sentences) {
			if(res.size() > 0) {
				PerceptronExample ex = new PerceptronExample();
				double goldLogProb = res.gold().getViterbiProbability();
				for(Parse r : res.results()) {
					double score = ;

					ex.add(this.extractCandidate(g, r, score, goldLogProb));
				}
			}
		}	
		return data;
	}


	private static HashSet<String> extractRawFeatures(Grammar g, Parse r) {
		HashSet<String> treeFeatures = new HashSet<String>();
		Tree t = r.getViterbiParse();
		treeFeatures.add("generativeProb");
		treeFeatures.add("topCat="+g.getCatFromID(t.B().category()));
		int[] lexCats = t.getLexicalCategoryIDs();
		for(int lc : lexCats) {
			treeFeatures.add("lexCat="+g.getCatFromID(lc));
		}
		treeFeatures.add("rootCat="+g.getCatFromID(lexCats[t.headIndex()]));
		return treeFeatures;
	}

	private CandidateStructure extractCandidate(Grammar g, Parse r, double score, double goldLogProb) {
		// Get raw features
		HashSet<String> treeFeatures = extractRawFeatures(g, r);

		// Filter features
		treeFeatures = super.filterFeatures(treeFeatures);

		// Make candidate
		int[] feats = new int[treeFeatures.size()];
		double[] featWeights = new double[feats.length];
		int i=0;
		for(String f : treeFeatures) {
			feats[i] = super.featureID(f);
			if(f.equals("generativeProb")) {
				featWeights[i] = Math.exp( - goldLogProb);
			}
			else {
				featWeights[i] = 1.0;
			}
			i++;
		}
		return new CandidateStructure(score, feats, featWeights);
	}
	 */

}
