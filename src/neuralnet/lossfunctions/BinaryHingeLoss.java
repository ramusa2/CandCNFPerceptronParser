package neuralnet.lossfunctions;

import java.util.Arrays;

import org.jblas.DoubleMatrix;

/**
 * Calculates the hinge loss with specified margin for a binary classification task 
 * where the decision boundary is at zero.
 * @author ramusa2
 *
 */
public class BinaryHingeLoss extends LossFunction {
	
	protected double margin;
	
	public BinaryHingeLoss(double classificationMargin) {
		this.margin = classificationMargin;
	}

	@Override
	public DoubleMatrix getLoss(DoubleMatrix predicted, int targetClass) {
		double goldResponse = predicted.get(targetClass);
		double otherResponse = Double.NEGATIVE_INFINITY;
		for(int i=0; i<predicted.data.length; i++) {
			if(i != targetClass) {
				otherResponse = Math.max(otherResponse, predicted.data[i]);
			}
		}
		double loss = Math.min(goldResponse - otherResponse - margin, 0.0);
		DoubleMatrix lossVec = new DoubleMatrix(predicted.rows, predicted.columns); 
		Arrays.fill(lossVec.data, loss);
		lossVec.put(targetClass, -lossVec.get(targetClass));
		return lossVec;
	}

	@Override
	public double getLoss(double predicted, double target) {
		if(target == 1.0) {
			return Math.min(predicted - margin, 0.0);
		}
		else if(target == -1.0) {
			return Math.max(- margin - predicted, 0.0);
		}
		System.err.println("Hinge loss function supports +/-1.0 classification; target value is "+target);
		return 0.0;
	}

}
