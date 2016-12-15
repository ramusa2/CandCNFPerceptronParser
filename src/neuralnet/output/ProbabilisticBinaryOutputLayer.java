package neuralnet.output;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

import neuralnet.lossfunctions.LossFunction;
import neuralnet.transferfunctions.TransferFunction;

import org.jblas.DoubleMatrix;

public class ProbabilisticBinaryOutputLayer extends SingleValueOutputLayer implements Externalizable {

	protected int inputDimension;
	protected double cachedOutput;
	
	public ProbabilisticBinaryOutputLayer(int numInputs, TransferFunction transferFunction,
			LossFunction lossFunction) {
		super(numInputs, transferFunction, lossFunction);
		this.cachedOutput = Double.NaN;
	}
	
	public final double output(DoubleMatrix input) {
		this.inputDimension = input.length;
		this.cachedOutput = this.transfer.forward(input).get(0);
		return this.cachedOutput;
	}

	public DoubleMatrix calculateGradientOfLossFunction(double target) {
		double delta = this.loss.getLoss(cachedOutput, target);
		DoubleMatrix grad = new DoubleMatrix(this.inputDimension, 1);
		Arrays.fill(grad.data, delta);
		return this.transfer.backward(grad);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeInt(this.inputDimension);
		out.writeDouble(this.cachedOutput);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		super.readExternal(in);
		this.inputDimension = in.readInt();
		this.cachedOutput = in.readDouble();
	}
}
