package neuralnet.transferfunctions;

import org.jblas.DoubleMatrix;
import org.jblas.ranges.RangeUtils;

public class IdentityTransferFunction extends TransferFunction {

	protected IdentityTransferFunction() {
		super(1, 1);
	}

	@Override
	protected DoubleMatrix calculateOutput(DoubleMatrix inputVector,
			DoubleMatrix outputVector) {
		outputVector.put(RangeUtils.all(), RangeUtils.all(), inputVector);
		return outputVector;
	}

	@Override
	protected DoubleMatrix backward(DoubleMatrix inputVector,
			DoubleMatrix outputGradient) {
		return outputGradient;
	}

}
