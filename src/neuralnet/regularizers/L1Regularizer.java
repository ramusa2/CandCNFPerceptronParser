package neuralnet.regularizers;

import org.jblas.DoubleMatrix;

/**
 * Adds L1 regularization to a hidden layer (simply layer).
 * 
 * @author ramusa2
 *
 */
public class L1Regularizer extends Regularizer {

	@Override
	public DoubleMatrix regularize(DoubleMatrix mat, double learningRate) {
		for(int i=0; i<mat.data.length; i++) {
			mat.data[i] += delta(mat.data[i], learningRate);
		}
		return mat;
	}

	@Override
	public double regularize(double parameter, double learningRate) {
		return parameter + delta(parameter, learningRate);
	}
	
	/**
	 * Returns Caps the step to avoid changing sign (stop at 0).
	 */
	private double delta(double parameter, double learningRate) {
		return Math.signum(parameter)*Math.min(Math.abs(parameter), learningRate);		
	}

}
