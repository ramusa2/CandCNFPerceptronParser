package supertagger.nn;

import illinoisParser.Sentence;

import multitagger.layers.LogisticLayer;
import neuralnet.deprecated.LinearLayer;

import org.jblas.DoubleMatrix;

import supertagger.lewissteedman.LSVariableEntry;
import supertagger.nn.modules.LookupTableLayer;

public class LogisticRegressionNN {
	


	protected LookupTableLayer lookupLayer;
	protected LinearLayer firstLinearLayer;
	protected LogisticLayer outputLayer;


	
	protected DoubleMatrix costGrad;
	
	public LogisticRegressionNN() {}

	public LogisticRegressionNN(LookupTableLayer lookup, int numHiddenNodes) {
		this.lookupLayer = lookup;
		this.firstLinearLayer = new LinearLayer(this.lookupLayer.getNumberOfOutputs(), numHiddenNodes);
		this.outputLayer = new LogisticLayer();
		this.costGrad = new DoubleMatrix(this.firstLinearLayer.getNumberOfNodes());
	}
	
	public double hardPredict(DoubleMatrix input) {
		return this.predict(input) < 0.5 ? 0.0 : 1.0;
	}

	public double predict(DoubleMatrix input) {
		return this.outputLayer.output(firstLinearLayer.output(lookupLayer.getOutput(input)));
	}

	/**
	 * Adjusts the network parameters based on the gradient of the cross-entropy loss function
	 * for probabilities assigned by the network (based on the specified active variables) and
	 * the target correct label.
	 *  
	 * The gradient is back-propagated through the network, and the parameters at each layer
	 * are adjusted using a step scaled by the learning rate.
	 * @return 
	 */
	public double trainOn(DoubleMatrix input, double correctLabel, double learningRate) {
		DoubleMatrix lookupOutput = this.lookupLayer.output(input);
		DoubleMatrix linearOutput = this.firstLinearLayer.output(lookupOutput);
		double prediction = this.outputLayer.output(linearOutput);
		
			
		double prob = prediction;
		double pred = this.hardPredict(input);
		/*
		if(pred == correctLabel) {
			System.out.println("Correct: (prob: "+prob+")");
		}
		else {
			System.out.println("Incorrect: (prob: "+prob+")");
		}
		*/
		
		double loss = this.outputLayer.calculateGradientOfCostFunction(prediction, correctLabel);
		for(int i=0; i<this.costGrad.length; i++) {
			this.costGrad.put(i, loss*learningRate);
		}
		// Update gradient w.r.t. linear layer
		//linearOutput.muli(loss, this.costGrad);		
		DoubleMatrix linGrad = 
				this.firstLinearLayer.calculateGradientWithRespectToInput(lookupOutput, this.costGrad);
		this.firstLinearLayer.updateParameters(this.costGrad, learningRate);
		this.lookupLayer.updateParameters(linGrad, learningRate);
		return pred == correctLabel ? 1.0 : 0.0;
	}
	
}
