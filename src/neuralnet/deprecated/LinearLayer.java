package neuralnet.deprecated;

import java.io.Externalizable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

import multitagger.Regularization;

import org.jblas.DoubleMatrix;

import supertagger.nn.modules.Layer;

public class LinearLayer extends Layer implements Externalizable {
	
	private static final double DROPOUT_PROB = 0.5;
	
	private DoubleMatrix parameters;
	private DoubleMatrix bias;

	private DoubleMatrix linGrad;

	private DoubleMatrix output;
	
	private DoubleMatrix updateIntermediate;
	
	private boolean[] dropoutMask;

	/**
	 * Constructs a new linear layer initialized with Gaussian noise, 
	 * where the noise has mean 0 and variance equal to the inverse of
	 * the square route of the number of inputs.
	 */
	public LinearLayer(int numInputs, int numOutputs) {
		this.parameters = new DoubleMatrix(numOutputs, numInputs);
		this.dropoutMask = new boolean[numInputs];
		Arrays.fill(this.dropoutMask, true);
		double variance = 1.0/Math.sqrt(numInputs);
		for(int c=0; c<numOutputs; c++) {
			this.parameters.putRow(c, getRandomGaussianNoise(numInputs, variance));
		}
		this.bias = new DoubleMatrix(numOutputs);
		this.linGrad = new DoubleMatrix(numInputs); 
		this.output = new DoubleMatrix(1, numOutputs);
		this.updateIntermediate = new DoubleMatrix(numOutputs, numInputs);
	}
	private DoubleMatrix getRandomGaussianNoise(int dimension, double variance) {
		DoubleMatrix gaussian = new DoubleMatrix(1, dimension);
		Random random = new Random();
		for(int f=0; f<dimension; f++) {
			double draw = random.nextGaussian() * variance;
			gaussian.put(1, f-1, draw); // Why f-1? JBLAS seems to increment the column index itself, so we counterbalance it. Why do they do this? No clue.
		}
		return gaussian;
	}

	@Override
	protected DoubleMatrix calculateOutput(DoubleMatrix input) {
		return this.parameters.mmuli(input, this.output).addi(bias);
	}

	public DoubleMatrix outputWithDropout(DoubleMatrix input, boolean updateDropoutMask) {
		if(updateDropoutMask) {
			this.updateDropoutMask();
		}
		return this.applyMask(this.calculateOutput(input));
	}

	private final void updateDropoutMask() {
		for(int i=0; i<this.dropoutMask.length; i++) {
			this.dropoutMask[i] = Math.random() > DROPOUT_PROB;
		}
	}
	
	@Override
	public DoubleMatrix calculateGradientWithRespectToInput(
			DoubleMatrix input, DoubleMatrix nextGradient) {
		this.linGrad = nextGradient.transpose().mmul(this.parameters).transpose();
		return this.linGrad;
		//return this.parameters.transpose().mmuli(nextGradient, this.linGrad);
	}
	public DoubleMatrix calculateGradientWithRespectToInputWithDropout(
			DoubleMatrix input, DoubleMatrix nextGradient) {
		return this.calculateGradientWithRespectToInput(input, this.applyMask(nextGradient));
		/*
		DoubleMatrix transposed = this.parameters.transpose();
		DoubleMatrix temp = new DoubleMatrix(1, 1);
		for(int i=0; i<this.parameters.columns; i++) {
			double grad = 0.0;
			if(this.dropoutMask[i]) {
				grad = transposed.getRow(i).mmuli(nextGradient, temp).get(0);
			}
			this.linGrad.put(i, grad);
		}
		return this.linGrad;
		*/
	}

	@Override
	public void updateParameters(DoubleMatrix input, DoubleMatrix outputGradient, double learningRate) {
		this.updateParameters(input, outputGradient, learningRate, Regularization.NONE, 0.0);
	}

	public void updateParameters(DoubleMatrix input, DoubleMatrix outputGradient, 
			double learningRate, Regularization regType, double regWeight) {
		// Scale gradient by learning rate
		outputGradient.muli(learningRate);
		// Update bias (bias unaffected by regularization)
		this.bias.addi(outputGradient);
		// Apply update from regularization, if applicable
		if(regType == Regularization.L1) {
			double p_ij;
			double delta;
			double step = regWeight*learningRate;
			for(int i=0; i<this.parameters.rows; i++) {
				for(int j=0; j<this.parameters.columns; j++) {
					p_ij = this.parameters.get(i, j);
					delta = Math.signum(p_ij) * Math.min(step, Math.abs(p_ij));
					this.parameters.put(i, j, p_ij - delta);
				}
			}
		}
		else if(regType == Regularization.L2) {
			this.parameters.mmuli(1.0 - learningRate*regWeight);
		}
		// Apply update from cost function
		outputGradient.mmuli(input.transpose(), this.updateIntermediate);
		this.parameters.addi(this.updateIntermediate);
	}
	public void updateParametersWithDropout(DoubleMatrix input, DoubleMatrix outputGradient, 
			double learningRate, Regularization regType, double regWeight) {
		this.updateParameters(input, this.applyMask(outputGradient), learningRate, regType, regWeight);
	}
	
	private final DoubleMatrix applyMask(DoubleMatrix vec) {
		for(int i=0; i<vec.length; i++) {
			if(!this.dropoutMask[i]) {
				vec.put(i, 0.0);
			}
		}
		return vec;
	}
	
	/**
	 * Saves the lookup table weights to file
	 */
	public void saveWeightsToFile(File file) {
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(file);
			for(int index=0; index<this.parameters.rows; index++) {
				DoubleMatrix features = this.parameters.getRow(index);
				String line = "";
				for(int i=0; i<features.length; i++) {
					line += features.get(i)+" ";
				}
				line += this.bias.get(index);
				pw.println(line);
			}
			pw.close();
		} catch (FileNotFoundException e) {
			System.out.println("Failed to save lookup layer weights to: "+file.getPath());
		}
	}
	
	/**
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
	public DoubleMatrix getWeights() {
		return this.parameters;
	}
	public int getNumberOfNodes() {
		return this.bias.length;
	}
	public void scaleWeightsDownAfterDropoutTraining() {
		this.parameters.muli(1.0 - DROPOUT_PROB);
	}
	public void scaleWeightsUpForDropoutTraining() {
		this.parameters.muli(1.0/(1.0 - DROPOUT_PROB));
	}
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(this.parameters);
		out.writeObject(this.bias);
		out.writeObject(this.linGrad);
		out.writeObject(this.output);
		out.writeObject(this.updateIntermediate);
		out.writeObject(this.dropoutMask);
	}
	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		this.parameters = (DoubleMatrix) in.readObject();
		this.bias = (DoubleMatrix) in.readObject();
		this.linGrad = (DoubleMatrix) in.readObject();
		this.output = (DoubleMatrix) in.readObject();
		this.updateIntermediate = (DoubleMatrix) in.readObject();
		this.dropoutMask = (boolean[]) in.readObject();		
	}
}
