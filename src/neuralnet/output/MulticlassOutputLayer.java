package neuralnet.output;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import neuralnet.lossfunctions.LossFunction;
import neuralnet.transferfunctions.TransferFunction;

import org.jblas.DoubleMatrix;

public class MulticlassOutputLayer extends OutputLayer implements Externalizable {
	
	public MulticlassOutputLayer(int numInputs, TransferFunction transferFunction,
			LossFunction lossFunction) {
		super(numInputs, transferFunction, lossFunction);
	}
	
	public final DoubleMatrix predict(DoubleMatrix input) {
		return this.transfer.forward(this.weights.forward(input));
	}
	
	public final DoubleMatrix getBackpropagatedGradient(DoubleMatrix input, int targetClass) {
		return this.weights.backward(this.transfer.backward(this.loss.getLoss(this.predict(input), targetClass)));
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
