package neuralnet.lossfunctions;

import java.util.Arrays;

import org.jblas.DoubleMatrix;

/**
 * Calculates the hinge loss with specified margin for a 0.0-1.0 classification task.
 * @author ramusa2
 *
 */
public class ProbabilisticHingeLoss extends LossFunction {
	
	private final double MID = 0.5;
	
	protected double margin;
	
	public ProbabilisticHingeLoss(double classificationMargin) {
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
			return -Math.max(MID + margin - predicted, 0.0);
		}
		else if(target == 0.0) {
			return -Math.min(MID - margin - predicted, 0.0);
		}
		System.err.println("Probabilistic hinge loss function supports 0-1 classification; target value is "+target);
		return 0.0;
	}

}
