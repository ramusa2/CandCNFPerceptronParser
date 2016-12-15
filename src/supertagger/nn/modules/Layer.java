package supertagger.nn.modules;

import org.jblas.DoubleMatrix;

import supertagger.nn.Module;

/**
 * The Layer class defines the methods for a parameterized layer in a neural net. In
 * addition to the basic methods, a parameterized layer must be able to update its 
 * parameters based on its inputs and a back-propagated gradient.
 * 
 * @author ramusa2
 *
 */
public abstract class Layer extends Module {

	private DoubleMatrix input;

	@Override
	protected DoubleMatrix getOutput(DoubleMatrix input) {
		this.input = input;
		return calculateOutput(this.input);
	}

	protected abstract DoubleMatrix calculateOutput(DoubleMatrix input);

	@Override 
	public final void updateParameters(DoubleMatrix outputGradient, double learningRate){
		updateParameters(this.input, outputGradient, learningRate);
	}

	protected abstract void updateParameters(DoubleMatrix input, DoubleMatrix outputGradient, double learningRate);
	/*
	{
		DoubleMatrix gradient = calculateGradientWithRespectToParameters(input, outputGradient);
		updateParametersIfApplicable(gradient, learningRate);
	}
	

	protected abstract void updateParametersIfApplicable(
			DoubleMatrix gradientWithRespectToParameters, double learningRate);

	protected abstract DoubleMatrix calculateGradientWithRespectToParameters(DoubleMatrix input, DoubleMatrix outputGradient);
	*/
}
