package neuralnet.lossfunctions;

import org.jblas.DoubleMatrix;

/**
 * The negative log-likelihood loss function for multiclass classification
 * @author ramusa2
 *
 */
public class NegativeLogLikelihood extends LossFunction {

	@Override
	public DoubleMatrix getLoss(DoubleMatrix predicted, int targetClass) {
		DoubleMatrix loss = new DoubleMatrix(predicted.rows, predicted.columns);
		for(int i=0; i<loss.length; i++) {
			double l_i = (i==targetClass ? 1.0 : 0.0) - predicted.get(i);
			loss.put(i, l_i);
		}
		return loss;
	}

	@Override
	public double getLoss(double predicted, double target) {
		return 0.0;
	}
	
	

}
