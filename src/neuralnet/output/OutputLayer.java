package neuralnet.output;

import java.io.Externalizable;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import neuralnet.lossfunctions.LossFunction;

import neuralnet.simplelayers.LinearLayer;
import neuralnet.transferfunctions.TransferFunction;

public abstract class OutputLayer implements Externalizable {
	
	protected LinearLayer weights;
	
	protected TransferFunction transfer;
	
	protected LossFunction loss;
	
	protected OutputLayer(int numInputs, TransferFunction transferFunction, LossFunction lossFunction) {
		this.weights = new LinearLayer(numInputs, transferFunction.numberOfInputs());
		this.transfer = transferFunction;
		this.loss = lossFunction;
	}
	
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(this.weights);
		out.writeObject(this.transfer);
		out.writeObject(this.loss);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		this.weights = (LinearLayer) in.readObject();
		this.transfer = (TransferFunction) in.readObject();
		this.loss = (LossFunction) in.readObject();
	}
	
	public void loadWeightsFromFile(File file) {
		this.weights.loadWeightsFromFile(file);
	}
}
