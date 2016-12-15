package neuralnet.input;

import java.io.Externalizable;

import org.jblas.DoubleMatrix;

public abstract class ContextExtractor<T> implements Externalizable {
	
	public abstract DoubleMatrix extract(T context);
	
	public abstract void updateParameters(DoubleMatrix gradient);

	public abstract int getOutputDimension();

}
