package neuralnet.regularizers;

import org.jblas.DoubleMatrix;

/**
 * Adds L2 regularization to a hidden layer (simply layer).
 * 
 * @author ramusa2
 *
 */
public class L2Regularizer extends Regularizer {

	@Override
	public DoubleMatrix regularize(DoubleMatrix mat, double learningRate) {
		double scale = 1.0 - learningRate;
		return mat.muli(scale, mat);
	}

	@Override
	public double regularize(double parameter, double learningRate) {
		return parameter*(1.0-learningRate);
	}

}
