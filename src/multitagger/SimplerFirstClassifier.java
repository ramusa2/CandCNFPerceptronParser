package multitagger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.PriorityQueue;

import neuralnet.deprecated.LinearLayer;

import multitagger.layers.BinaryOutputLayer;
import multitagger.layers.LogisticLayer;
import multitagger.layers.LogisticLayerWithHingeLoss;
import multitagger.layers.EmbeddingMultitaggerInputLayer;
import multitagger.layers.MultitaggerInputLayer;
import multitagger.layers.SimplerLookupLayer;
import multitagger.layers.inputlayers.ProbabilityBitVectorLookupLayer;
import multitagger.layers.inputlayers.SingleFeatureLookupLayer;
import supertagger.lsbeta.MultitaggerTrainingData;
import supertagger.lsbeta.MultitaggerTrainingItem;
import supertagger.lsbeta.MultitaggerTrainingSentence;
import util.Util;

public class SimplerFirstClassifier extends FirstClassifier {

	protected SimplerFirstClassifier(MultitaggerInputLayer myInputLayer,
			LinearLayer myLinearLayer, BinaryOutputLayer myOutputLayer) {
		super(myInputLayer, myLinearLayer, myOutputLayer);
		
		
		
	}
	
	public static SimplerFirstClassifier getDevClassifier(MultitaggerTrainingData data) {
		int numCats = 100;
		HashMap<String, Integer> catFreqs = getViterbiCategoryFrequencies(data);	
		ArrayList<String> freqCats = getKMostFrequent(catFreqs, numCats);	
		int smallEmbedDim = 20;
		int nCIL = 4;
		int cxtWidth = 4;
		int numHiddenNodes = 100;
		MultitaggerInputLayer i = new SimplerLookupLayer(smallEmbedDim, freqCats, catFreqs, nCIL, cxtWidth);
		LinearLayer l = new LinearLayer(i.getOutputDimension(), numHiddenNodes);
		//BinaryOutputLayer o = new LogisticLayer();
		BinaryOutputLayer o = new LogisticLayerWithHingeLoss(0.0);
		return new SimplerFirstClassifier(i, l, o);
	}
	
	private static HashMap<String, Integer> getViterbiCategoryFrequencies(
			MultitaggerTrainingData data) {
		HashMap<String, Integer> catFreqs = new HashMap<String, Integer>();
		for(MultitaggerTrainingSentence sen : data.getData()) {
			for(MultitaggerTrainingItem item : sen.getItems()) {
				Util.increment(catFreqs, item.getCat(0));
			}
		}
		return catFreqs;
	}
	

	private static ArrayList<String> getKMostFrequent(HashMap<String, Integer> map, int k) {
		PriorityQueue<StringInt> queue = new PriorityQueue<StringInt>();
		for(String s : map.keySet()) {
			Integer f = map.get(s);
			if(queue.size() < k) {
				queue.add(new StringInt(s, f));
			}
			else {
				StringInt old = queue.peek();
				if(f > old.i) {
					queue.poll();
					queue.add(new StringInt(s, f));
				}
			}
		}

		ArrayList<String> list = new ArrayList<String>();
		while(!queue.isEmpty()) {
			list.add(queue.poll().str);
		}
		Collections.reverse(list);
		return list;
	}

	@Override
	protected boolean shouldUpdate(double predictedResponse,
			double correctResponse) {
		return this.outputLayer.calculateGradientOfCostFunction(predictedResponse, correctResponse) != 0.0;
	}

	public static FirstClassifier getSimplestClassifier(
			MultitaggerTrainingData trainingData, int k, int numHiddenNodes, int numCats) {
		HashMap<String, Integer> catFreqs = getViterbiCategoryFrequencies(trainingData);	
		ArrayList<String> freqCats = getKMostFrequent(catFreqs, numCats);	
		SingleFeatureLookupLayer i = new SingleFeatureLookupLayer(k);
		for(String cat : freqCats) {
			i.addFeatureValue(cat);
		}
		LinearLayer l = new LinearLayer(i.getOutputDimension(), numHiddenNodes);
		BinaryOutputLayer o = new LogisticLayer();
		//BinaryOutputLayer o = new LogisticLayerWithHingeLoss(0.0);
		return new SimpleFirstClassifier(i, l, o);
	}

	public static FirstClassifier getSoftBitVectorClassifier(
			MultitaggerTrainingData trainingData, int numHiddenNodes, int numCats) {
		HashMap<String, Integer> catFreqs = getViterbiCategoryFrequencies(trainingData);	
		ArrayList<String> freqCats = getKMostFrequent(catFreqs, numCats);	
		ProbabilityBitVectorLookupLayer i = new ProbabilityBitVectorLookupLayer();
		for(String cat : freqCats) {
			i.addFeatureValue(cat);
		}
		LinearLayer l = new LinearLayer(i.getOutputDimension(), numHiddenNodes);
		BinaryOutputLayer o = new LogisticLayer();
		//BinaryOutputLayer o = new LogisticLayerWithHingeLoss(0.0);
		return new SimpleFirstClassifier(i, l, o);
	}

}

class StringInt implements Comparable<StringInt> {
	String str;

	Integer i;

	StringInt(String s, int in) {
		this.str = s;
		this.i = in;
	}

	@Override
	public int compareTo(StringInt o) {
		return this.i - o.i;
	}

	public String toString() {
		return this.str+" "+this.i;
	}
}
