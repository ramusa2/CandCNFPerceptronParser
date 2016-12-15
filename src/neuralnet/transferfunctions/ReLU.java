package neuralnet.transferfunctions;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.jblas.DoubleMatrix;

/**
 * Rectified linear unit layer
 * @author ramusa2
 *
 */
public class ReLU extends TransferFunction implements Externalizable {

	public ReLU(int numInputs, int numOutputs) {
		super(numInputs, numOutputs);
	}

	@Override
	protected DoubleMatrix calculateOutput(DoubleMatrix inputVector,
			DoubleMatrix outputVector) {
		double[] inputData = inputVector.data;
		for(int i=0; i<inputData.length; i++) {
			outputVector.put(i, Math.max(0.0, inputData[i]));
		}
		return outputVector;
	}

	@Override
	protected DoubleMatrix backward(DoubleMatrix inputVector,
			DoubleMatrix outputGradient) {
		double[] inputData = inputVector.data;
		double[] outputData = outputGradient.data;
		for(int i=0; i<inputData.length; i++) {
			this.inputGradient.put(i, gradReLU(inputData[i])*outputData[i]);
		}
		return this.inputGradient;
	}

	private double gradReLU(double d) {
		if(d <= 0.0) {
			return 0.0;
		}
		return d;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		super.readExternal(in);
	}

}
