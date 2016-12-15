package neuralnet;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;

import org.jblas.DoubleMatrix;

import neuralnet.input.InputLayer;
import neuralnet.output.PerceptronOutputLayer;

public class Perceptron<T> implements Externalizable {
	
	protected InputLayer<T> inputLayer;
	
	protected PerceptronOutputLayer perceptron;
	
	public Perceptron(InputLayer<T> input) {
		this.inputLayer = input;
		this.perceptron = new PerceptronOutputLayer(this.inputLayer.getOutputVectorDimension());
	}
	
	public void standardizeFeatures(ArrayList<T> trainData) {
		this.inputLayer.learnFeatureNormalizers(trainData);
	}
	
	public void whitenFeatures(ArrayList<T> trainData) {
		this.inputLayer.learnWhiteningMatrix(trainData);
	}
	
	public double predict(T context) {
		DoubleMatrix vec = this.inputLayer.getOutput(context);
		return this.perceptron.predict(vec);
	}
	
	public double hardPredict(T context) {
		DoubleMatrix vec = this.inputLayer.getOutput(context);
		return this.perceptron.hardPredict(vec);
	}
	
	public void trainOn(T context, double target, double learningRate) {
		// Input
		DoubleMatrix vec = this.inputLayer.getOutput(context);
		// Update weights
		DoubleMatrix back = this.perceptron.getBackpropagatedGradient(vec, target);
		this.perceptron.updateParameters(learningRate);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(this.inputLayer);
		out.writeObject(this.perceptron);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		this.inputLayer = (InputLayer<T>) in.readObject();
		this.perceptron = (PerceptronOutputLayer) in.readObject();
	}

}
