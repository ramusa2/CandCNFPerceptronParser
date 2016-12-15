package neuralnet;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

import org.jblas.DoubleMatrix;

public abstract class Module implements Externalizable {

	protected final boolean isParameterized;

	protected DoubleMatrix input;

	protected DoubleMatrix output;

	// TODO: do we need to cache inputGradient if parameter gradients are accumulated during each backprop step?
	protected DoubleMatrix inputGradient;


	protected Module(boolean willUpdateParameters) {
		this.isParameterized = willUpdateParameters;
	}

	/**
	 * Calculates and returns the output of this layer given the specified input
	 * and caches both the input vector and the output vector.
	 */
	public final DoubleMatrix forward(DoubleMatrix inputVector) {
		this.input = inputVector;
		this.calculateOutput(this.input, this.getCleanOutputVector());
		return this.output;
	}

	/**
	 * Calculates the output of this layer given an input vector, and stores the result
	 * in outputVector.
	 * 
	 * Precondition: outputVector is initialized to the zero vector.
	 */
	protected abstract DoubleMatrix calculateOutput(DoubleMatrix inputVector, DoubleMatrix outputVector);


	/**
	 * Performs a backpropagation step using the cached input vector and the
	 * provided outputGradient. 
	 * 
	 * For parmaeterized modules, cache the gradient updates used for batch learning.
	 * 
	 * This method returns a DoubleMatrix containing the gradient with respect to its
	 * input.
	 */
	public final DoubleMatrix backward(DoubleMatrix outputGradient) {
		this.inputGradient = this.backward(this.input, outputGradient);
		if(this.isParameterized) {
			this.updateParameterGradients(outputGradient);
		}
		return this.inputGradient;
	}

	/**
	 * Used to accumulate parameter gradients if this model is parameterized.
	 */
	public void updateParameterGradients(DoubleMatrix outputGradient) {
		this.updateParameterGradients(this.input, outputGradient);
	}

	/**
	 * Used to accumulate parameter gradients if this model is parameterized.
	 */
	protected abstract void updateParameterGradients(DoubleMatrix input,
			DoubleMatrix outputGradient);

	protected abstract DoubleMatrix backward(DoubleMatrix inputVector, DoubleMatrix outputGradient);

	/**
	 * If this layer has parameters, update them by applying the accumulated gradients
	 * scaled by the specified learning rate
	 */
	public void updateParameters(double learningRate) {
		if(this.isParameterized) {
			this.doParameterUpdate(learningRate);
			this.clearAccumulatedParameterGradients();
		}
	}

	protected abstract void doParameterUpdate(double learningRate);
	
	protected abstract void clearAccumulatedParameterGradients();

	protected final void clear(DoubleMatrix tensor) {
		if(tensor != null) {
			Arrays.fill(tensor.data, 0.0);
		}
	}

	private DoubleMatrix getCleanOutputVector() {
		if(this.output == null) {
			return null;
			// TODO: initialize output vector
		}
		this.clear(this.output);
		return this.output;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		// TODO --  implement here, or leave it to subclasses?
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		// TODO --  implement here, or leave it to subclasses?
	}
}

/*

[output] forward(input)
[gradInput] backward(input, gradOutput)
zeroGradParameters()
updateParameters(learningRate)

 */
