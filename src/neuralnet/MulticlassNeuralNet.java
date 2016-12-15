package neuralnet;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;

import neuralnet.hidden.HiddenLayer;
import neuralnet.input.InputLayer;
import neuralnet.output.MulticlassOutputLayer;

import org.jblas.DoubleMatrix;

public class MulticlassNeuralNet <T> implements Externalizable {
	
	protected InputLayer<T> inputLayer;
	
	protected ArrayList<HiddenLayer> hiddenLayers;
	
	protected MulticlassOutputLayer outputLayer;
	
	public MulticlassNeuralNet(InputLayer<T> input, MulticlassOutputLayer output, HiddenLayer... hidden) {
		this.inputLayer = input;
		this.hiddenLayers = new ArrayList<HiddenLayer>();
		for(HiddenLayer h : hidden) {
			this.hiddenLayers.add(h);
		}
		this.outputLayer = output;
	}

	public void standardizeFeatures(ArrayList<T> trainData) {
		this.inputLayer.learnFeatureNormalizers(trainData);
	}
	
	public void whitenFeatures(ArrayList<T> trainData) {
		this.inputLayer.learnWhiteningMatrix(trainData);
	}
	
	public DoubleMatrix predict(T context) {
		DoubleMatrix vec = this.inputLayer.getOutput(context);
		for(HiddenLayer layer : this.hiddenLayers) {
			vec = layer.output(vec);
		}
		return this.outputLayer.predict(vec);
	}
	
	public void trainOn(T context, int target, double learningRate) {
		//Forward
		DoubleMatrix vec = this.inputLayer.getOutput(context);
		//System.out.println(vec);
		for(HiddenLayer layer : this.hiddenLayers) {
			vec = layer.output(vec);
		}
		// Backprop
		DoubleMatrix back = this.outputLayer.getBackpropagatedGradient(vec, target);
		for(int i=this.hiddenLayers.size()-1; i>=0; i--) {
			back = this.hiddenLayers.get(i).backward(back);
		}
		// Update weights
		this.inputLayer.updateParameters(back);
		for(HiddenLayer layer : this.hiddenLayers) {
			layer.updateParameters(learningRate);
		}
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(this.inputLayer);
		out.writeObject(this.hiddenLayers);
		out.writeObject(this.outputLayer);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		this.inputLayer = (InputLayer) in.readObject();
		this.hiddenLayers = (ArrayList<HiddenLayer>) in.readObject();
		this.outputLayer = (MulticlassOutputLayer) in.readObject();
	}
	
	public int hardPredict(T context) {
		DoubleMatrix out = this.predict(context);
		int maxID = -1;
		double max = Double.NEGATIVE_INFINITY;
		for(int i=0; i<out.length; i++) {
			if(out.get(i) > max) {
				maxID = i;
				max = out.get(i);
			}
		}
		return maxID;
	}
}
