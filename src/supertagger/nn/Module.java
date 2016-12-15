package supertagger.nn;

import org.jblas.DoubleMatrix;

/**
 * Following Torch, the Module class represents an abstract layer in the network.
 * Module subclasses must allow for evaluation (input/output signal) as well as calculating
 * a gradient given a cost/objective function from from later layers.
 * 
 * @author ramusa2
 *
 */
public abstract class Module {
	
	private DoubleMatrix output;

	public final DoubleMatrix output(DoubleMatrix input) {
		this.output = getOutput(input);
		return this.output;
	}
	
	protected abstract DoubleMatrix getOutput(DoubleMatrix input);
	
	public abstract DoubleMatrix calculateGradientWithRespectToInput(DoubleMatrix input, DoubleMatrix nextGradient);
	
	public abstract void updateParameters(DoubleMatrix outputGradient, double learningRate);

	
	//private DoubleMatrix gradientWithRespectToInput;
	
	//protected abstract void updateParameters(DoubleMatrix input, DoubleMatrix outputGradient, double learningRate);

	/*
	public final DoubleMatrix getGradientWithRespectToInput(DoubleMatrix nextGradient) {
		this.gradientWithRespectToInput = calculateGradientWithRespectToInput(this.input, nextGradient);
		return this.gradientWithRespectToInput;
	}
	*/

}
