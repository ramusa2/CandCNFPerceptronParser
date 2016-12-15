package multitagger.layers;

import java.io.Serializable;

import org.jblas.DoubleMatrix;

public abstract class BinaryOutputLayer implements Serializable {

	public abstract double output(DoubleMatrix input);

	public abstract double calculateGradientOfCostFunction(double output, double target);

}
