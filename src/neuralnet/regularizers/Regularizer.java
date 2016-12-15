package neuralnet.regularizers;

import org.jblas.DoubleMatrix;

/**
 * Abstract class to define subgradients for a set of parameters
 * according to the regularization function defined by the subclass.
 * 
 * @author ramusa2
 *
 */
public abstract class Regularizer {
	
	/**
	 * Applies regularization to mat with appropriate learning rate;
	 * modifies mat directly and returns result.
	 */
	public abstract DoubleMatrix regularize(DoubleMatrix mat, double learningRate);

	
	/**
	 * Applies regularization to the parameter with appropriate learning rate;
	 * returns result.
	 */
	public abstract double regularize(double parameter, double learningRate);

}
