package neuralnet.transferfunctions;

import java.io.Externalizable;

import org.jblas.DoubleMatrix;

public class Logistic extends TransferFunction implements Externalizable {

	public Logistic(int numInputs) {
		super(numInputs, 1);
	}

	@Override
	protected DoubleMatrix calculateOutput(DoubleMatrix inputVector,
			DoubleMatrix outputVector) {
		outputVector.put(0, logistic(inputVector));
		return outputVector;
	}

	private double logistic(DoubleMatrix inputVector) {
		return 1.0/(1.0+Math.exp(-input.sum()));
	}

	@Override
	protected DoubleMatrix backward(DoubleMatrix inputVector,
			DoubleMatrix outputGradient) {
		double out = outputGradient.get(0);
		inputVector.muli(out*(1.0-out), this.inputGradient);
		return this.inputGradient;
	}

}
