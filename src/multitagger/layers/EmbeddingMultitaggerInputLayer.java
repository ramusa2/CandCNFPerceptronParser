package multitagger.layers;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.jblas.DoubleMatrix;

import supertagger.lsbeta.MultitaggerTrainingSentence;
import supertagger.nn.Module;
import supertagger.nn.StringEmbeddings;

public abstract class EmbeddingMultitaggerInputLayer extends MultitaggerInputLayer {

	private static final String WORD_EMBEDDINGS_FILE = "word_embeddings";
	private static final String CATEGORY_EMBEDDINGS_FILE = "category_embeddings";

	protected static final String WORD_START = "WORD_START";

	protected static final String WORD_END = "WORD_END";

	protected static final String WORD_UNKNOWN = "WORD_UNKNOWN";

	protected static final String CAT_START = "CAT_START";

	protected static final String CAT_END = "CAT_END";

	protected static final String CAT_UNKNOWN = "CAT_UNKNOWN";

	protected StringEmbeddings wordEmbeddings;

	protected StringEmbeddings categoryEmbeddings;

	protected DoubleMatrix outputVector;

	protected MultitaggerTrainingSentence inputSentence;

	protected int inputPosition;

	protected EmbeddingMultitaggerInputLayer(StringEmbeddings initialWordEmbeddings, 
			int wordEmbeddingDimension,
			StringEmbeddings initialCategoryEmbeddings, 
			int categoryEmbeddingDimension) {
		this.wordEmbeddings = initialWordEmbeddings;
		this.categoryEmbeddings = initialCategoryEmbeddings;
		this.outputVector = null;
		this.inputSentence = null;
		this.inputPosition = -1;
		if(this.wordEmbeddings != null) {
			wordEmbeddings.setDefault(WORD_START, getRandomVec(wordEmbeddingDimension));
			wordEmbeddings.setDefault(WORD_END, getRandomVec(wordEmbeddingDimension));
			wordEmbeddings.setDefault(WORD_UNKNOWN, getRandomVec(wordEmbeddingDimension));
		}
		if(this.categoryEmbeddings != null) {
			categoryEmbeddings.setDefault(CAT_START, getRandomVec(categoryEmbeddingDimension));
			categoryEmbeddings.setDefault(CAT_END, getRandomVec(categoryEmbeddingDimension));
			categoryEmbeddings.setDefault(CAT_UNKNOWN, getRandomVec(categoryEmbeddingDimension));
		}
	}

	protected DoubleMatrix getRandomVec(int dim) {
		DoubleMatrix vec = new DoubleMatrix(dim, 1);
		for(int i=0; i<dim; i++) {
			vec.put(i, Math.random()-0.5);
		}
		return vec;
	}

	protected DoubleMatrix getCategoryEmbedding(String category) {
		DoubleMatrix vec = this.categoryEmbeddings.getVec(category);
		if(vec == null) {
			vec = this.categoryEmbeddings.getVec(CAT_UNKNOWN);
		}
		return vec;
	}

	protected DoubleMatrix getWordEmbedding(String word) {
		DoubleMatrix vec = this.wordEmbeddings.getVec(word);
		if(vec == null) {
			vec = this.wordEmbeddings.getVec(WORD_UNKNOWN);
		}
		return vec;
	}

	public abstract int getOutputDimension();

	/**
	 * Fill output with extracted features for predicting category at position in sentence.
	 */
	protected abstract void fillOutputVector(DoubleMatrix output,
			MultitaggerTrainingSentence sentence, int position);

	@Override
	public DoubleMatrix calculateGradientWithRespectToInput(DoubleMatrix input,
			DoubleMatrix nextGradient) {
		System.err.println("Never backpropagate gradient from input layer.");
		return null;
	}

	@Override
	public abstract void updateParameters(DoubleMatrix outputGradient,
			double learningRate);

	/*
	protected StringEmbeddings interpolateEmbeddings(StringEmbeddings vecs, double[] probs, int k) {
		StringEmbeddings
	}
	 */



	public void saveWeightsToFile(File saveDir) {
		saveDir.mkdir();
		// Word embeddings
		this.wordEmbeddings.saveToFile(new File(saveDir.getPath()+File.separator+WORD_EMBEDDINGS_FILE));
		// Category embeddings
		this.categoryEmbeddings.saveToFile(new File(saveDir.getPath()+File.separator+CATEGORY_EMBEDDINGS_FILE));
	}

	public void loadWeightsFromFile(File loadDir) {
		this.wordEmbeddings = StringEmbeddings.loadFromFile(new File(loadDir.getPath()+File.separator+WORD_EMBEDDINGS_FILE));
		this.categoryEmbeddings = StringEmbeddings.loadFromFile(new File(loadDir.getPath()+File.separator+CATEGORY_EMBEDDINGS_FILE));
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(this.wordEmbeddings);
		out.writeObject(this.categoryEmbeddings);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		this.wordEmbeddings = (StringEmbeddings) in.readObject();
		this.categoryEmbeddings = (StringEmbeddings) in.readObject();
	}

}
