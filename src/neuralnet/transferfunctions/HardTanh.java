package neuralnet.transferfunctions;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.jblas.DoubleMatrix;

/**
 * Hard-tanh transfer function
 * @author ramusa2
 *
 */
public class HardTanh extends TransferFunction implements Externalizable {

	public HardTanh(int numInputs, int numOutputs) {
		super(numInputs, numOutputs);
	}

	@Override
	protected DoubleMatrix calculateOutput(DoubleMatrix inputVector,
			DoubleMatrix outputVector) {
		double[] inputData = inputVector.data;
		for(int i=0; i<inputData.length; i++) {
			outputVector.put(i, hardTanh(inputData[i]));
		}
		return outputVector;
	}

	private static final double hardTanh(double d) {
		if(d <= -1.0) {
			return -1.0;
		}
		if(d >= 1.0) {
			return 1.0;
		}
		return d;
	}

	@Override
	protected DoubleMatrix backward(DoubleMatrix inputVector,
			DoubleMatrix outputGradient) {
		double[] inputData = inputVector.data;
		double[] outputData = outputGradient.data;
		for(int i=0; i<inputData.length; i++) {
			this.inputGradient.put(i, gradHardTanh(inputData[i], outputData[i]));
		}
		return this.inputGradient;
	}

	private double gradHardTanh(double in, double grad) {
		if(in < -1.0) {
			return 0.0;
		}
		if(in > 1.0) {
			return 0.0;
		}
		return grad;
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
