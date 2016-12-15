package neuralnet.lossfunctions;

import org.jblas.DoubleMatrix;

/**
 * Abstract class to define subgradients for the output of a network on a single example
 * according to the particular loss function defined by the subclass.
 * 
 * @author ramusa2
 *
 */
public abstract class LossFunction {

	/**
	 * Returns the loss function for a multiclass classification task given a vector
	 * of predicted response for each class and the target/gold class index
	 */
	public abstract DoubleMatrix getLoss(DoubleMatrix predicted, int targetClass);
	
	/**
	 * Returns the loss for a regression or classification task given a predicted
	 * value and a target (gold) value/label.
	 */
	public abstract double getLoss(double predicted, double target);
	

}
