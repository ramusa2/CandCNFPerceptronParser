package neuralnet.output;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

import neuralnet.simplelayers.LinearLayer;
import neuralnet.transferfunctions.TransferFunction;

import org.jblas.DoubleMatrix;

public class PerceptronOutputLayer extends SingleValueOutputLayer implements Externalizable {
	
	public PerceptronOutputLayer(int numInputs) {
		super(numInputs, TransferFunction.getIdentityFunction(), null);
		//this.weights = new LinearLayer(numInputs, 1);
	}
	
	public final double predict(DoubleMatrix input) {
		return this.weights.forward(input).get(0);
	}
	
	public final double hardPredict(DoubleMatrix input) {
		return (this.predict(input) >= 0.0) ? 1.0 : -1.0; 
	}
	
	public final DoubleMatrix getBackpropagatedGradient(DoubleMatrix input, double target) {
		double predicted = this.hardPredict(input);
		double delta = (predicted == target) ? 0.0 : target;
		DoubleMatrix grad = new DoubleMatrix(1, 1);
		grad.put(0, delta);
		return this.weights.backward(grad);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeObject(this.weights);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		super.readExternal(in);
		this.weights = (LinearLayer) in.readObject();
	}

	public void updateParameters(double learningRate) {
		this.weights.updateParameters(learningRate);
	}

}
