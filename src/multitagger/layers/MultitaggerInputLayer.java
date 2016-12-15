package multitagger.layers;

import java.io.Externalizable;
import java.io.File;

import org.jblas.DoubleMatrix;

import supertagger.lsbeta.MultitaggerTrainingSentence;
import supertagger.nn.Module;

public abstract class MultitaggerInputLayer extends Module implements Externalizable {
	
	protected DoubleMatrix outputVector;
	
	protected MultitaggerTrainingSentence inputSentence;
	
	protected int inputPosition;

	protected DoubleMatrix getRandomVec(int dim) {
		// TODO: fix variance?
		DoubleMatrix vec = new DoubleMatrix(dim, 1);
		for(int i=0; i<dim; i++) {
			vec.put(i, Math.random()-0.5);
		}
		return vec;
	}
	
	public final DoubleMatrix getOutput(MultitaggerTrainingSentence sentence, int position) {
		if(this.outputVector == null) {
			this.outputVector = new DoubleMatrix(this.getOutputDimension(), 1);
		}
		this.inputSentence = sentence;
		this.inputPosition = position;
		fillOutputVector(this.outputVector, sentence, position);
		return this.outputVector;
	}

	public abstract int getOutputDimension();

	/**
	 * Fill output with extracted features for predicting category at position in sentence.
	 */
	protected abstract void fillOutputVector(DoubleMatrix output,
			MultitaggerTrainingSentence sentence, int position);

	@Override
	protected DoubleMatrix getOutput(DoubleMatrix input) {
		System.err.println("Use getOutput(MultitaggerTrainingSentence, int).");
		return null;
	}

	@Override
	public DoubleMatrix calculateGradientWithRespectToInput(DoubleMatrix input,
			DoubleMatrix nextGradient) {
		System.err.println("Never backpropagate gradient from input layer.");
		return null;
	}

	@Override
	public abstract void updateParameters(DoubleMatrix outputGradient,
			double learningRate);

	public abstract void saveWeightsToFile(File file);
	public abstract void loadWeightsFromFile(File file);
}
