package perceptron.core;

import illinoisParser.FineChartItem;
import illinoisParser.Sentence;
import illinoisParser.Tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import java.util.Map.Entry;


public abstract class PerceptronModel {

	private ConcurrentHashMap<String, Integer> featureToKeyMap;
	private double[] w;
	boolean updateTypeIsRelative;
	
	private final int minFeatFreq;
	
	protected PerceptronModel() { minFeatFreq = 2;}

	protected PerceptronModel(boolean useRelativeUpdate, int featFreqMin) {
		updateTypeIsRelative = useRelativeUpdate;
		minFeatFreq = featFreqMin;
	}
	
	protected int featureID(String feature) {
		return this.featureToKeyMap.get(feature);
	}

	public void train(PerceptronTrainingData data, int numIterations) {
		for(int T=0; T<numIterations; T++) {
			for(PerceptronExample ex : data.examples()) {
				this.trainOn(ex);
			}
		}
	}

	private void trainOn(PerceptronExample ex) {
		CandidateStructure modelBest = this.getBestStructure(ex);
		if(!ex.correct(modelBest)) {
			if(updateTypeIsRelative) {
				this.relativeUpdate(ex.best(), modelBest);
			}
			else {
				this.absoluteUpdate(ex.best(), modelBest);
			}
		}
	}

	abstract public CandidateStructure getBestStructure(PerceptronExample ex);

	protected double score(CandidateStructure candidate) {
		double sc = 0.0;
		int[] feats = candidate.features();
		double[] featWeights = candidate.featureWeights();
		for(int i=0; i<feats.length; i++) {
			sc += w[feats[i]]*featWeights[i];
		}
		return sc;
	}

	private void absoluteUpdate(CandidateStructure gold,
			CandidateStructure modelBest) {
		this.update(gold.features(), 1.0);
		this.update(modelBest.features(), -1);
	}

	private void relativeUpdate(CandidateStructure gold,
			CandidateStructure modelBest) {
		double delta = gold.score() - modelBest.score();
		this.update(gold.features(), delta);
		this.update(modelBest.features(), -delta);
	}

	private void update(int[] feats, double delta) {
		for(int f : feats) {
			w[f] += delta;
		}
	}
	
	public int[] filterFeatures(String[] raw) {
		HashSet<Integer> filtered = new HashSet<Integer>();
		for(String f : raw) {
			Integer fID = this.featureToKeyMap.get(f);
			if(fID != null) {
				filtered.add(fID);
			}
		}
		int[] ids = new int[filtered.size()];
		int i=0;
		for(Integer id : filtered) {
			ids[i++] = id;
		}
		return ids;
	}
	
	public void trainModel(Collection<Sentence> trainingData) {
		initializeModel(trainingData);
		train();
	}
	
	protected void initializeModel(Collection<Sentence> trainingData) {
		// Get feature frequencies
		ArrayList<TemporaryCandidateStructure> temp = new ArrayList<TemporaryCandidateStructure>();
		for(Sentence sen : trainingData) {
			temp.add(this.getInitialCandidateWithFeatures(sen));
		}
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		for(TemporaryCandidateStructure ex : temp) {
			for(String feat : ex.featureNames()) {
				incr(map, feat);
			}
		}
		// Set up filtered weight vector/feature map
		featureToKeyMap = new ConcurrentHashMap<String, Integer>();
		int i=0;
		for(Entry<String, Integer> entry : map.entrySet()) {
			if(entry.getValue() >= this.minFeatFreq) {
				featureToKeyMap.put(entry.getKey(),  i);
				i++;
			}
		}
		w = new double[featureToKeyMap.size()];
		Arrays.fill(w, 0.0);
	}
	
	
	private void incr(HashMap<String, Integer> map, String key) {
		Integer f = map.get(key);
		if(f==null) {
			f = 0;
		}
		map.put(key, f+1);
	}

	// Implement these submethods depending on the discriminative parser 
	// (e.g. reranker, log-linear parser, etc.)
	abstract protected TemporaryCandidateStructure getInitialCandidateWithFeatures(Sentence sen);
	abstract protected void train();
	abstract public Tree<? extends FineChartItem> classify(Sentence sen); 
}
