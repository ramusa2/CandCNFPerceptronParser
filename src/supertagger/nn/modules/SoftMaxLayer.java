package supertagger.nn.modules;

import neuralnet.lossfunctions.NegativeLogLikelihood;
import neuralnet.output.MulticlassOutputLayer;
import neuralnet.transferfunctions.SoftMax;

public class SoftMaxLayer extends MulticlassOutputLayer {

	public SoftMaxLayer(int numInputs, int numLabels) {
		super(numInputs, new SoftMax(numLabels, numLabels), new NegativeLogLikelihood());
	}
/*
	@Override
	protected DoubleMatrix calculateOutput(DoubleMatrix input) {
		double logZ = Double.NEGATIVE_INFINITY;
		for(int i=0; i<input.length; i++) {
			logZ = logAdd(logZ, input.get(i));
		}
		DoubleMatrix out = new DoubleMatrix(input.length);
		for(int i=0; i<input.length; i++) {
			out.put(i, Math.exp(input.get(i) - logZ));
		}
		return out;		
	}

	private static double logAdd(double a, double b) {
		if (b > a) {
			double temp = a;
			a = b;
			b = temp;
		}
		if (a == Double.NEGATIVE_INFINITY) {
			return a;
		}
		double negDiff = b - a;
		if (negDiff < -20) {
			return a;
		}
		return a + java.lang.Math.log(1.0 + java.lang.Math.exp(negDiff));
	}

	@Override
	protected DoubleMatrix calculateGradientOfCostFunction(DoubleMatrix output,
			DoubleMatrix target) {
		return target.sub(output);
	}
	
/*
	public DoubleMatrix getFinalGradient(int correctOutput) {
		DoubleMatrix target = new DoubleMatrix(this.output.length);
		target.put(correctOutput,  1.0);
		return getFinalGradient(target);
	}

	public DoubleMatrix getFinalGradient(DoubleMatrix targetVector) {
		return targetVector.sub(this.output);
	}

	@Override
	public DoubleMatrix getGradient(DoubleMatrix nextGradient) {
		return nextGradient.mmul(this.input.transpose());
	}

	@Override
	public DoubleMatrix getCost(DoubleMatrix nextGradient) {
		return this.parameters.transpose().mmul(nextGradient);
	}
	

	public void updateParameters(DoubleMatrix finalGradient, double learningRate) {
		DoubleMatrix scaled = finalGradient.mul(learningRate);
		this.parameters.addi(this.getGradient(scaled));
		this.bias.addi(scaled);
	}
	*/

}
