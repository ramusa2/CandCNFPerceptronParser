package neuralnet.simplelayers;

import java.io.Externalizable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Scanner;

import neuralnet.regularizers.Regularizer;

import org.jblas.DoubleMatrix;

import util.Util;

/**
 * A layer of nodes with parameters specifying the interpolation of inputs, plus a bias.
 * @author ramusa2
 *
 */
public class LinearLayer extends SimpleLayer implements Externalizable {

	private DoubleMatrix parameters;
	
	private DoubleMatrix bias;
	
	private int numInputs;
	
	private int numOutputs;
	
	private DoubleMatrix accumulatedParameterGradients;
	
	private DoubleMatrix accumulatedBiasGradients;
	
	public LinearLayer(int numberOfInputs, int numNodes) {
		super(numberOfInputs, numNodes);
		this.numInputs = numberOfInputs;
		this.numOutputs = numNodes;
		this.parameters = new DoubleMatrix(numNodes, this.numInputs);
		this.accumulatedParameterGradients = new DoubleMatrix(numNodes, this.numInputs);
		this.bias = new DoubleMatrix(numNodes, 1);
		this.accumulatedBiasGradients = new DoubleMatrix(numNodes, 1);
		this.parameters = Util.initializeRandomMatrix(numNodes, this.numInputs);
	}

	@Override
	protected DoubleMatrix calculateOutput(DoubleMatrix inputVector,
			DoubleMatrix outputVector) {
		this.parameters.mmuli(inputVector, outputVector);
		outputVector.addi(this.bias, outputVector);
		return outputVector;
	}

	@Override
	protected void updateParameterGradients(DoubleMatrix input,
			DoubleMatrix outputGradient) {
		// TODO: use in-place multiplication?
		this.numBackpropsSinceLastParameterUpdate++;
		this.accumulatedParameterGradients.addi(outputGradient.mmul(input.transpose()), this.accumulatedParameterGradients);
		this.accumulatedBiasGradients.addi(outputGradient, this.accumulatedBiasGradients);
	}

	@Override
	protected DoubleMatrix backward(DoubleMatrix inputVector,
			DoubleMatrix outputGradient) {
		// TODO: use in-place multiplication?
		this.inputGradient = outputGradient.transpose().mmul(this.parameters).transpose();
		return this.inputGradient;
	}

	@Override
	protected void doParameterUpdate(double learningRate) {
		this.parameters.addi(this.accumulatedParameterGradients.muli(learningRate, this.accumulatedParameterGradients));
		this.bias.addi(this.accumulatedBiasGradients.muli(learningRate, this.accumulatedBiasGradients));
	}
	
	@Override
	public void applyRegularizersToParameters(ArrayList<Regularizer> regularizers, 
			ArrayList<Double> coefficients, double learningRate) {
		if(this.numBackpropsSinceLastParameterUpdate > 0) {
			for(int r=0; r<regularizers.size(); r++) {
				double stepSize = coefficients.get(r)*learningRate*this.numBackpropsSinceLastParameterUpdate;
				if(stepSize > 0.0) {
					regularizers.get(r).regularize(this.parameters, stepSize);
					regularizers.get(r).regularize(this.bias, stepSize);
				}
			}
		}
		this.numBackpropsSinceLastParameterUpdate = 0;
	}

	@Override
	protected void clearAccumulatedParameterGradients() {
		super.clear(this.accumulatedParameterGradients);
		super.clear(this.accumulatedBiasGradients);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeInt(this.numInputs);
		out.writeInt(this.numOutputs);
		out.writeObject(this.parameters);
		out.writeObject(this.bias);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		super.readExternal(in);
		this.numInputs = in.readInt();
		this.numOutputs = in.readInt();
		this.parameters = (DoubleMatrix) in.readObject();
		this.bias = (DoubleMatrix) in.readObject();
	}

	/**
	 * Note: Deprecated. Holdover from previous implementation of neural net code.
	 * 
	 * Loads lookup table weights from file.
	 * Precondition: weight vectors in file match dimensionality of this object.
	 */
	public void loadWeightsFromFile(File file) {
		Scanner sc = null;
		try {
			sc = new Scanner(file);
			String line;
			int index = 0;
			while(sc.hasNextLine()) {
				line = sc.nextLine().trim();
				if(!line.isEmpty()) {
					String[] toks = line.split("\\s+");
					if(toks.length == this.parameters.columns+1) {
						DoubleMatrix weights = new DoubleMatrix(1, this.parameters.columns);
						for(int i=0; i<toks.length-1; i++) {
							weights.put(1, i-1, Double.parseDouble(toks[i]));
						}
						this.parameters.putRow(index, weights);
						double biasValue = Double.parseDouble(toks[this.parameters.columns]);
						this.bias.put(index, biasValue);
					}
					else {
						System.out.println("Malformed weights file; dimensionality does not match " +
								"(expected "+this.parameters.columns+" outputs, " +
										"read "+(toks.length-1)+" weights).\nAborting.");
						sc.close();
						return;
					}
					index++;
				}
			}
			sc.close();
		} catch (FileNotFoundException e) {
			System.out.println("Failed to load linear layer weights from: "+file.getPath());
		}
	}
}
