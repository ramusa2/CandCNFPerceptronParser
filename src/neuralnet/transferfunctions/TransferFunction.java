package neuralnet.transferfunctions;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import neuralnet.Module;

import org.jblas.DoubleMatrix;

public abstract class TransferFunction extends Module implements Externalizable {
	
	protected TransferFunction(int numInputs, int numOutputs) {
		super(false);
		this.inputGradient = new DoubleMatrix(numInputs, 1);
		this.output = new DoubleMatrix(numOutputs, 1);
	}
	
	public int numberOfInputs() {
		return this.inputGradient.rows;
	}
	
	public int numberOfOutputs() {
		return this.output.rows;
	}
	
	/*
	public abstract DoubleMatrix backpropagateGradient(DoubleMatrix outputGradient);
*/
	@Override
	protected final void updateParameterGradients(DoubleMatrix input,
			DoubleMatrix outputGradient) {
		// Transfer functions have no parameters.
	}
	
	@Override
	protected final void doParameterUpdate(double learningRate) {
		// Transfer functions have no parameters.
	}
	
	@Override
	protected final void clearAccumulatedParameterGradients() {
		// Transfer functions have no parameters.
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

	public static TransferFunction getIdentityFunction() {
		return new IdentityTransferFunction();
	}

}
