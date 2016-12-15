package supertagger.nn.modules;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

import org.jblas.DoubleMatrix;

import supertagger.nn.Module;

/**
 * A LookupTableLayer is the first layer in the network; given a set of active
 * input variables (represented as an array of integer variable indices), the
 * layer feeds the linear combination of the stored lookup weights forward through
 * the network. Given a back-propagated gradient, the lookup weights can be adjusted
 * during training. 
 * 
 * @author ramusa2
 *
 */
public abstract class LookupTableLayer extends Module {
	
	/** The list of indices of the current active features, cached for use in back-propagation. **/
	protected int[] activeInputs;
	
	/** Number of total (potential) input variables. **/
	protected int numVariables;
	
	/** Number of nodes in the next layer of the network. **/
	protected final int numOutputs;
	
	/** Number of hidden nodes for each input variable (i.e., number of features stored 
	 * in the lookup table for each variable). **/
	protected final int numFeaturesPerInputVariable;
	

	/** Maps input strings to their index in the word embeddings lookup table. **/
	protected ConcurrentHashMap<String, Integer> stringToIndex;
	
	/** Stores the embedding parameters for this layer; because only a few variables are active at a time,
	 * we need a way to access them efficiently. **/
	protected ConcurrentHashMap<Integer, double[]> wordEmbeddingsLookupTable;
	

	/**
	 * Creates a LookupTableLayer of the specified dimensions; feature weights
	 * are initialized using Gaussian noise, with a mean of zero and standard 
	 * deviation of one.
	 * 
	 * @param numberOfInputVariables		size of the input variable space
	 * @param dimensionsPerInput	number of features to match each variable to
	 */
	public LookupTableLayer(int numberOfInputVariables, int dimensionsPerInput) {
		this.numVariables = numberOfInputVariables;
		this.numFeaturesPerInputVariable = dimensionsPerInput;
		this.numOutputs = this.numVariables*this.numFeaturesPerInputVariable;
		this.stringToIndex = new ConcurrentHashMap<String, Integer>();
		this.wordEmbeddingsLookupTable = new ConcurrentHashMap<Integer, double[]>();
	}
	
	/**
	 * Returns a weight vector where each element is drawn from a Gaussian
	 * distribution with a mean of zero and a standard deviation of one.
	 */
	protected double[] getWeightVectorOfGaussianNoise(int dimension) {
		double[] vec = new double[dimension];
		Random random = new Random();
		for(int f=0; f<dimension; f++) {
			vec[f] = random.nextGaussian();
		}
		return vec;
	}

	/**
	 * Overwrites the current word embedding for the variable associated with a particular
	 * index (e.g., to initialize parameters with pre-trained word embeddings).
	 */
	public void setEmbeddingForVariable(int inputVariableIndex, double[] newEmbedding) {
		this.wordEmbeddingsLookupTable.put(inputVariableIndex, newEmbedding);
	}

	/**
	 * Returns the current embedding for a word.
	 */
	public abstract DoubleMatrix getWordEmbedding(String word);

	/**
	 * Overwrites the word embedding for a variable.
	 */
	public void setEmbeddingForVariable(String variable, double[] newEmbedding) {
		Integer index = this.stringToIndex.get(variable);
		if(index == null) {
			index = this.stringToIndex.size();
			this.numVariables++;
			this.stringToIndex.put(variable, index);
		}
		this.wordEmbeddingsLookupTable.put(index, newEmbedding);
	}
	
	/**
	 * Returns the number of output units for this layer (depends on the 
	 * size of the context window and on the number of features per
	 * input variable).
	 */	
	public final int getNumberOfOutputs() {
		return this.numOutputs;
	}

	/**
	 * Returns the number of features per input variable.
	 */
	public final int getNumberOfFeaturesPerInput() {
		return this.numFeaturesPerInputVariable;
	}

	/**
	 * Returns null; the lookup table layer should be called with a sparse list of active variables.
	 */
	@Override
	public DoubleMatrix getOutput(DoubleMatrix rawInput) {
		System.out.println("Call output method for specific lookup layer (e.g. using a sentence and a position).");
		return null;
	}

	/**
	 * Returns null; input layer should never be asked to return its input gradient
	 */
	@Override
	public final DoubleMatrix calculateGradientWithRespectToInput(
			DoubleMatrix input, DoubleMatrix nextGradient) {
		return null;
	}

	/**
	 * Given a list of indices indicating the active variables, return the output
	 * signal (a linear combination of the current weights for those active features).
	 * 
	 * Note: overwrites any existing cached active variable indices, storing activeVariables instead.
	 
	public abstract DoubleMatrix output(int[] activeVariables);
	*/

	/**
	 * Update the feature weights for the cached active variables based on
	 * the back-propagated gradient.
	 */
	public abstract void updateParameters(DoubleMatrix outputGradient, double learningRate);

	/** Each child class needs to implement the interger mapping
	 * between words and their indices; for known words this should be
	 * straightforward, but different models may have multiple unknown
	 * word tokens (e.g. based on capitalization) and should implement
	 * that logic here. **/
	protected abstract Integer getEmbeddingID(String word);

	/**
	 * Save the lookup table weights to file (or multiple files, e.g. 
	 * if there are different sets of discrete features).
	 */
	public abstract void saveWeightsToFile(File file);
	
	/**
	 * Loads lookup table weights from file (or multiple files).
	 * Precondition: weight vectors in file match dimensionality of this object.
	 */
	public abstract void loadWeightsFromFile(File file);
}
