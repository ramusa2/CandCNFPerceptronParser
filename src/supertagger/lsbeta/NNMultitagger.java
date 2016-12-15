package supertagger.lsbeta;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

import neuralnet.deprecated.LinearLayer;

import org.jblas.DoubleMatrix;

import supertagger.LexicalCategoryEntry;
import supertagger.SupertagAssignment;
import supertagger.lewissteedman.LSSupertagger;
import supertagger.nn.LogisticRegressionNN;
import supertagger.nn.modules.LookupTableLayer;

public class NNMultitagger {

	private LSSupertagger supertagger;

	private CategoryEmbeddingPlusViterbiProbLookupLayer lookupLayer;

	private LogisticRegressionNN firstClassifier;



	public NNMultitagger(LSSupertagger tagger, int numHiddenNodes, int numCatsToLookAt) {
		this.supertagger = tagger;
		this.lookupLayer = new CategoryEmbeddingPlusViterbiProbLookupLayer(
				numCatsToLookAt,
				tagger.getCatIDMap(),
				tagger.getHiddenLayer().getWeights().rowsAsList());
		this.firstClassifier = new LogisticRegressionNN(lookupLayer, numHiddenNodes);
	}

	public double probOfReturningOnlyViterbiCategory(SupertagAssignment tags, int i) {
		DoubleMatrix input = this.lookupLayer.getInput(tags, i);
		return this.firstClassifier.predict(input);
	}
	
	public boolean returnOnlyViterbiCategory(SupertagAssignment tags, int i) {
		DoubleMatrix input = this.lookupLayer.getInput(tags, i);
		return this.firstClassifier.hardPredict(input) == 1.0;
	}
	
	public double trainOn(SupertagAssignment tags, int i, boolean firstIsGold, double learningRate) {
		DoubleMatrix input = this.lookupLayer.getInput(tags, i);
		double correctLabel = firstIsGold ? 1.0 : 0.0;
		return this.firstClassifier.trainOn(input, correctLabel, learningRate);
	}

}

// Embedding: category embedding of of first K categories in list, along with probs of each category
class CategoryEmbeddingPlusViterbiProbLookupLayer extends LookupTableLayer {

	int K; // take first category, and next two

	int D; // embedding dimension for categories

	HashMap<String, Integer> catIDLookup;

	List<DoubleMatrix> catEmbeddings;

	private DoubleMatrix input;

	private int[] currentCatIDs;

	public CategoryEmbeddingPlusViterbiProbLookupLayer(int numCatsToLookAt, HashMap<String, Integer> catMap, List<DoubleMatrix> initialEmbeddings) {
		super(numCatsToLookAt, initialEmbeddings.get(0).length+1);
		this.catIDLookup = catMap;
		this.K = numCatsToLookAt;
		this.currentCatIDs = new int[K];
		this.D = initialEmbeddings.get(0).length;
		this.catEmbeddings = initialEmbeddings;
		this.input = new DoubleMatrix(K*(D+1), 1);
	}


	@Override
	public DoubleMatrix getWordEmbedding(String cat) {
		return null;
	}

	@Override
	public DoubleMatrix getOutput(DoubleMatrix rawInput) {
		return rawInput;
	}


	public DoubleMatrix getInput(SupertagAssignment tags, int i) {
		// TODO: make a more efficient method that doesn't require a 425+ element array for the top K
		LexicalCategoryEntry[] list = tags.getAll(i);
		int index = 0;
		for(int c=0; c<K; c++) {
			this.currentCatIDs[c] = this.catIDLookup.get(list[c].category());
			this.input.put(index++, list[c].score());
			DoubleMatrix embedding = this.catEmbeddings.get(this.catIDLookup.get(list[c].category()));
			for(int w = 0; w<embedding.length; w++) {
				this.input.put(index++, embedding.get(w));
			}
		}
		return this.input;
	}


	@Override
	public void updateParameters(DoubleMatrix outputGradient,
			double learningRate) {

		for(int c=0; c<K; c++) {
			DoubleMatrix embed = this.catEmbeddings.get(this.currentCatIDs[c]);
			for(int w=0; w<this.D; w++) {
				embed.put(w, embed.get(w) + outputGradient.get(w+1)*learningRate); // w+1: skip prob feature
			}
		}

	}


	@Override
	protected Integer getEmbeddingID(String word) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void saveWeightsToFile(File file) {
		// TODO Auto-generated method stub

	}


	@Override
	public void loadWeightsFromFile(File file) {
		// TODO Auto-generated method stub

	}

}
