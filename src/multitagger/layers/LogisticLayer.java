package multitagger.layers;


import org.jblas.DoubleMatrix;

public class LogisticLayer extends BinaryOutputLayer {

	public LogisticLayer() {}

	@Override
	public double output(DoubleMatrix input) {
		return 1.0/(1.0+Math.exp(-input.sum()));
	}

	@Override
	public double calculateGradientOfCostFunction(double output, double target) {
		return target - output;
	}
}
