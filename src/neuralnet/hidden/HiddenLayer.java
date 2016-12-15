package neuralnet.hidden;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;

import org.jblas.DoubleMatrix;

import neuralnet.regularizers.Regularizer;
import neuralnet.simplelayers.SimpleLayer;
import neuralnet.transferfunctions.TransferFunction;

public class HiddenLayer implements Externalizable {
	
	private SimpleLayer layer;
	
	private TransferFunction transfer;

	private ArrayList<Regularizer> regularizers;
	
	private ArrayList<Double> regularizerCoefficients;
	
	public HiddenLayer(SimpleLayer simpleLayer, TransferFunction transferFunction) {
		this.layer = simpleLayer;
		this.transfer = transferFunction;
		this.regularizers = new ArrayList<Regularizer>();
		this.regularizerCoefficients = new ArrayList<Double>();
	}
	
	public void addRegularizer(Regularizer regularizer, double coefficient) {
		this.regularizers.add(regularizer);
		this.regularizerCoefficients.add(coefficient);
	}

	public DoubleMatrix output(DoubleMatrix inputVector) {		
		DoubleMatrix out;
		if(this.layer != null) {
			out = this.layer.forward(inputVector);
		}
		else {
			out = inputVector;
		}
		if(this.transfer != null) {
			out = this.transfer.forward(out);
		}
		return out;
	}

	public DoubleMatrix backward(DoubleMatrix outputGradient) {
		// First, calculate the backpropagated gradient...
		DoubleMatrix intermediate;
		// ...pass gradient through transfer function...
		if(this.transfer != null) {
			intermediate = this.transfer.backward(outputGradient);
		}
		else {
			intermediate = outputGradient;
		}
		// ...and calculate layer gradient w.r.t. inputs.
		DoubleMatrix back;
		if(this.layer != null) {
			back = this.layer.backward(intermediate);
		}
		else {
			back = intermediate;
		}
		
		// Second, accumulate parameter gradients according to layer function.
		// Note: this is actually done in backward step
		//this.layer.updateParameterGradients(intermediate);
		
		// Finally, return backpropagated gradient.
		return back;
	}

	public void updateParameters(double learningRate) {
		this.layer.updateParameters(learningRate);
		this.layer.applyRegularizersToParameters(this.regularizers, this.regularizerCoefficients, learningRate);
	}
	
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(this.layer);
		out.writeObject(this.transfer);
		out.writeObject(this.regularizers);
		out.writeObject(this.regularizerCoefficients);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		this.layer = (SimpleLayer) in.readObject();
		this.transfer = (TransferFunction) in.readObject();
		this.regularizers = (ArrayList<Regularizer>) in.readObject();
		this.regularizerCoefficients = (ArrayList<Double>) in.readObject();
	}

}
