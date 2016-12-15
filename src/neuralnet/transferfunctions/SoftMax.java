package neuralnet.transferfunctions;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.jblas.DoubleMatrix;

public class SoftMax extends TransferFunction implements Externalizable {

	public SoftMax(int numInputs, int numOutputs) {
		super(numInputs, numOutputs);
	}

	@Override
	protected DoubleMatrix calculateOutput(DoubleMatrix inputVector,
			DoubleMatrix outputVector) {
		double max = Double.NEGATIVE_INFINITY;
		for(double v : inputVector.data) {
			max = Math.max(max,  v);
		}
		double Z = 0.0;
		double[] inputData = inputVector.data;
		for(int i=0; i<inputData.length; i++) {
			double x = Math.exp(inputData[i] - max);
			outputVector.put(i, x);
			Z += x;
		}
		outputVector.divi(Z, outputVector);
		return outputVector;
	}

	@Override
	protected DoubleMatrix backward(DoubleMatrix inputVector,
			DoubleMatrix outputGradient) {
		// Note: assumes prior output is cached
		for(int i=0; i<this.inputGradient.length; i++) {
			double o_i = this.output.get(i); 
			double grad_i = outputGradient.get(i);
			//double grad_i = o_i*(1-o_i) * outputGradient.get(i);
			this.inputGradient.put(i, grad_i);
		}
		return this.inputGradient;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {}

}
