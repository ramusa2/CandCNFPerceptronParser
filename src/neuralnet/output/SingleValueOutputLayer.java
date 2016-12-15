package neuralnet.output;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

import org.jblas.DoubleMatrix;

import neuralnet.lossfunctions.LossFunction;
import neuralnet.transferfunctions.TransferFunction;

public class SingleValueOutputLayer extends OutputLayer implements Externalizable {

	protected SingleValueOutputLayer(int numInputs, TransferFunction transferFunction,
			LossFunction lossFunction) {
		super(numInputs, transferFunction, lossFunction);
	}
	
	public double predict(DoubleMatrix input) {
		return this.transfer.forward(this.weights.forward(input)).get(0);
	}
	
	public DoubleMatrix getBackpropagatedGradient(DoubleMatrix input, double target) {
		double delta = this.loss.getLoss(this.predict(input), target);
		DoubleMatrix grad = new DoubleMatrix(input.length, 1);
		Arrays.fill(grad.data, delta);
		return this.weights.backward(this.transfer.backward(grad));
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
