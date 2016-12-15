package multitagger;

import java.util.ArrayList;

import org.jblas.DoubleMatrix;

import supertagger.lsbeta.MultitaggerTrainingItem;
import supertagger.lsbeta.MultitaggerTrainingSentence;
import supertagger.nn.StringEmbeddings;

public class Multitagger {
	
	private ContextSensitiveFirstClassifier first;
	
	private ContextSensitiveLaterClassifier later;
	
	private StringEmbeddings categoryEmbeddings;
	
	private int categoryEmbeddingDimension;
	
	private StringEmbeddings wordEmbeddings;
	
	private int wordEmbeddingDimension;
	
	private DoubleMatrix history;
	
	public Multitagger(int numHiddenNodes, int numCatsToLookAt, int contextWindowSize, 
			int wordEmbeddingDimension, int categoryEmbeddingDimension,  int surroundingContextDepth,
			StringEmbeddings learnedWordEmbeddings, StringEmbeddings initialCategoryEmbeddings,
			int mappingDimension) {
		this.categoryEmbeddings = initialCategoryEmbeddings;
		this.wordEmbeddings = learnedWordEmbeddings;
		this.categoryEmbeddingDimension = categoryEmbeddingDimension;
		this.wordEmbeddingDimension = wordEmbeddingDimension;
		this.first = new ContextSensitiveFirstClassifier(numHiddenNodes, numCatsToLookAt, contextWindowSize, wordEmbeddingDimension,
				categoryEmbeddingDimension, surroundingContextDepth, learnedWordEmbeddings, initialCategoryEmbeddings, mappingDimension);
		this.later = new ContextSensitiveLaterClassifier(numHiddenNodes, numCatsToLookAt, contextWindowSize, wordEmbeddingDimension,
				categoryEmbeddingDimension, surroundingContextDepth, learnedWordEmbeddings, initialCategoryEmbeddings, mappingDimension);
		this.history = new DoubleMatrix(this.categoryEmbeddingDimension, 1);
	}
	
	public static Multitagger getOriginalWordTagger() {
		return null;
	}
	
	public static Multitagger getOriginalCategoryTagger() {
		return null;
	}
	
	public static Multitagger getContextSensitiveWordTagger() {
		return null;
	}
	
	public static Multitagger getContextSensitiveCategoryTagger() {
		return null;
	}
	

	public double trainOn(MultitaggerTrainingSentence sentence, int i, double learningRate, double cumProbCutoff, int maxK) {
		MultitaggerTrainingItem item = sentence.getItems().get(i);
		int goldRank = item.getGoldIndex();
		if(goldRank < 0) {
			return 0.0;
			// Can't train on missing categories? (or maybe should return single top category)
		}
		double firstProb = first.predict(sentence, i);
		double continueProb = 1.0-firstProb;
		ArrayList<Double> probs = new ArrayList<Double>();
		probs.add(firstProb);
		ArrayList<Double> cumProbs = new ArrayList<Double>();
		cumProbs.add(1.0 - continueProb);
		int k = 1;
		DoubleMatrix category = this.categoryEmbeddings.getVec(item.getCat(0));
		this.history = category.mul(firstProb);
		while(continueProb >= cumProbCutoff && k < maxK) {
			category = this.categoryEmbeddings.getVec(item.getCat(k));
			
			double laterProb = later.predict(sentence, i, k, history);
			probs.add(laterProb);
			continueProb *= (1.0 - laterProb);
			cumProbs.add(1.0 - continueProb);
			this.history.addi(category.mul(cumProbs.get(k)));
			k++;
			
			
		}
		
		return 0.0;
	}

}
