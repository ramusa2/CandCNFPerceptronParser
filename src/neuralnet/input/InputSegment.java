package neuralnet.input;

import java.io.Externalizable;

import org.jblas.DoubleMatrix;

/**
 * An InputSegment uses a portion of the input/context to extract
 * a subset of an InputLayer's feature vector.
 * 
 * @author ramusa2
 *
 */
public abstract class InputSegment implements Externalizable {
	
	private DoubleMatrix outputVector;
	
	/**
	 * Interface with InputLayer; subclasses should overload this method
	 * with appropriate arguments in order to save state before returning 
	 * super(getOutput());
	 */
	public DoubleMatrix getOutput() {
		if(this.outputVector == null) {
			this.outputVector = new DoubleMatrix(this.getOutputVectorDimension(), 1);
		}
		this.fillOutputVector(this.outputVector);
		return this.outputVector;
	}
	
	/**
	 * Subclasses need to use state to 
	 * @param output
	 */
	protected abstract void fillOutputVector(DoubleMatrix output);

	public abstract int getOutputVectorDimension();
	
	/**
	 * Subclasses should use previously saved state to update parameters, if necessary
	 */
	public abstract void updateParameters(DoubleMatrix outputGradient);
}
