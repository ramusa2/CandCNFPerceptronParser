package multitagger;

import java.io.Externalizable;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import neuralnet.deprecated.LinearLayer;

import org.jblas.DoubleMatrix;

import multitagger.layers.BinaryOutputLayer;
import multitagger.layers.LogisticLayer;
import multitagger.layers.LogisticLayerWithHingeLoss;
import multitagger.layers.EmbeddingMultitaggerInputLayer;
import multitagger.layers.MultitaggerInputLayer;
import multitagger.layers.SimpleInputLayer;
import supertagger.lsbeta.MultitaggerTrainingSentence;
import supertagger.nn.StringEmbeddings;
import util.Util;

public class FirstClassifier implements Externalizable {
	
	private static final String INPUT_FILE = "input";
	
	private static final String LINEAR_FILE = "linear";

	protected MultitaggerInputLayer inputLayer;

	protected LinearLayer linearLayer;

	protected BinaryOutputLayer outputLayer;

	private DoubleMatrix outputCostGradient;
	
	public double gamma;

	protected FirstClassifier(
			MultitaggerInputLayer myInputLayer,
			LinearLayer myLinearLayer,
			BinaryOutputLayer myOutputLayer) {
		this.inputLayer = myInputLayer;
		this.linearLayer = myLinearLayer;
		this.outputLayer = myOutputLayer;
		this.outputCostGradient = new DoubleMatrix(this.linearLayer.getNumberOfNodes());
		this.gamma = 0.5;
	}

	public double predict(MultitaggerTrainingSentence sentence, int position) {
		return this.outputLayer.output(this.linearLayer.output(this.inputLayer.getOutput(sentence, position)));
	}

	public double hardPredict(MultitaggerTrainingSentence sentence, int position) {
		return this.makeHardPrediction(this.predict(sentence, position));
	}

	protected final double makeHardPrediction(double softPrediction) {
		return softPrediction < gamma ? 0.0 : 1.0;
	}

	public double trainOn(MultitaggerTrainingSentence sentence, int position, double correctResponse, 
			double learningRate) {
		return this.trainOn(sentence, position, correctResponse, learningRate, Regularization.NONE, 0.0, false, 1.0);
	}
	
	public double trainOn(MultitaggerTrainingSentence sentence, int position, double correctResponse, 
			double learningRate, Regularization regType, double regWeight, boolean useDropout) {
		return this.trainOn(sentence, position, correctResponse, learningRate, regType, regWeight, useDropout, 1.0);
	}

	public double trainOn(MultitaggerTrainingSentence sentence, int position, double correctResponse, 
			double learningRate, Regularization regType, double regWeight, boolean useDropout, double falsePositiveLossScale) {
		
		// Check prediction with current parameters
		DoubleMatrix inputOutput = this.inputLayer.getOutput(sentence, position);
		DoubleMatrix linearOutput;
		if(useDropout) {
			linearOutput = this.linearLayer.outputWithDropout(inputOutput, true);
		}
		else {
			linearOutput = this.linearLayer.output(inputOutput);
		}
		double predictedResponse = this.outputLayer.output(linearOutput);
		// See if we should update (for some training regimes, this will always be true)
		if(this.shouldUpdate(predictedResponse, correctResponse)) {
			double loss = this.outputLayer.calculateGradientOfCostFunction(predictedResponse, correctResponse);
			/*
			double loss = 0.0;
			if(correctResponse == 1.0) {
				loss = Math.max(0.0, this.gamma - predictedResponse);
			}
			else {
				loss = Math.min(0.0, this.gamma - predictedResponse);
			}
			*/
			
			
			if(correctResponse == 0.0 && this.makeHardPrediction(predictedResponse) == 1.0) {
				loss *= falsePositiveLossScale;
			}
			for(int j=0; j<this.outputCostGradient.length; j++) {
				this.outputCostGradient.put(j, loss*learningRate);
			}
			// Update gamma at misclassification
			//this.gamma -= loss*0.1*learningRate;
			/*
			if(correctResponse != this.makeHardPrediction(predictedResponse)) {
				this.gamma += (Math.signum(correctResponse - 0.5))*learningRate*0.1;
			}
			*/
			// Update gradient w.r.t. linear layer	
			DoubleMatrix linearCostGradient;
			if(useDropout) {
				linearCostGradient = this.linearLayer.calculateGradientWithRespectToInputWithDropout(inputOutput, this.outputCostGradient);
				this.linearLayer.updateParametersWithDropout(inputOutput, this.outputCostGradient, 1.0, regType, regWeight*learningRate);
			}
			else {
				linearCostGradient = this.linearLayer.calculateGradientWithRespectToInput(inputOutput, this.outputCostGradient);
				this.linearLayer.updateParameters(inputOutput, this.outputCostGradient, 1.0, regType, regWeight*learningRate);
			}
			// Do backpropagation update
			this.inputLayer.updateParameters(linearCostGradient, 1.0);			
		}
		// Return 1.0 if the classifier predicted the right response (before the update), else. 0.0
		return this.makeHardPrediction(predictedResponse) == correctResponse ? 1.0 : 0.0;
	}

	protected boolean shouldUpdate(double predictedResponse, double correctResponse) {
		return true;
	}

	public static FirstClassifier getSampleClassifier() {
		int numCatsToLookAt = 4;
		int contextWindowSize = 2;
		int wordEmbeddingDimension = 50;
		int categoryEmbeddingDimension = 50;
		int surroundingContextDepth = 5;
		StringEmbeddings learnedWordEmbeddings = Util.loadLearnedWordEmbeddings();
		StringEmbeddings initialCategoryEmbeddings = Util.loadWord2VecCategoryEmbeddings();
		int numHiddenNodes = 100;
		return getSampleClassifier(numCatsToLookAt, contextWindowSize,
				wordEmbeddingDimension, categoryEmbeddingDimension, surroundingContextDepth, 
				learnedWordEmbeddings, initialCategoryEmbeddings, numHiddenNodes);
	}


	public static FirstClassifier getSampleClassifier(int numCatsToLookAt, int contextWindowSize, 
			int wordEmbeddingDimension, int categoryEmbeddingDimension,  int surroundingContextDepth,
			StringEmbeddings learnedWordEmbeddings, StringEmbeddings initialCategoryEmbeddings, int numHiddenNodes) {
		MultitaggerInputLayer input = new SimpleInputLayer(numCatsToLookAt, contextWindowSize,
				wordEmbeddingDimension, categoryEmbeddingDimension, surroundingContextDepth, 
				learnedWordEmbeddings, initialCategoryEmbeddings);
		LinearLayer hidden = new LinearLayer(input.getOutputDimension(), numHiddenNodes);
		//BinaryOutputLayer output = new LogisticLayer();
		BinaryOutputLayer output = new LogisticLayerWithHingeLoss(0.2);
		return new SimpleFirstClassifier(input, hidden, output);
	}
	
	public void scaleWeightsDownAfterDropoutTraining() {
		this.linearLayer.scaleWeightsDownAfterDropoutTraining();
	}

	public void scaleWeightsUpForDropoutTraining() {
		this.linearLayer.scaleWeightsUpForDropoutTraining();
	}

	public void save(File saveDir) {
		try {
			saveDir.mkdirs();
			this.inputLayer.saveWeightsToFile(new File(saveDir+File.separator+this.INPUT_FILE));
			this.linearLayer.saveWeightsToFile(new File(saveDir+File.separator+this.LINEAR_FILE));
		} catch(Exception e) {
			System.err.println("Failed to save classifier to "+saveDir);
		}
	}
	
	public void loadWeights(File loadDir) {
		try {
			this.inputLayer.loadWeightsFromFile(new File(loadDir+File.separator+this.INPUT_FILE));
			this.linearLayer.loadWeightsFromFile(new File(loadDir+File.separator+this.LINEAR_FILE));
		} catch(Exception e) {
			System.err.println("Failed to load classifier from "+loadDir);
		}
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(this.inputLayer);
		out.writeObject(this.linearLayer);
		out.writeObject(this.outputLayer);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		this.inputLayer = (MultitaggerInputLayer) in.readObject();
		this.linearLayer = (LinearLayer) in.readObject();
		this.outputLayer = (BinaryOutputLayer) in.readObject();
	}
}
