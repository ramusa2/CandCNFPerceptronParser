package neuralnet.simplelayers;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;

import org.jblas.DoubleMatrix;

import neuralnet.Module;
import neuralnet.regularizers.Regularizer;

/**
 * A SimplerLayer is a Module with parameters.
 * @author ramusa2
 *
 */
public abstract class SimpleLayer extends Module implements Externalizable {
	
	protected int numBackpropsSinceLastParameterUpdate;

	protected SimpleLayer(int numberOfInputs, int numberOfNodes) {
		super(true);
		this.inputGradient = new DoubleMatrix(numberOfInputs, 1);
		this.output = new DoubleMatrix(numberOfNodes, 1);
		this.numBackpropsSinceLastParameterUpdate = 0;
	}
	
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeInt(this.numBackpropsSinceLastParameterUpdate);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		super.readExternal(in);
		this.numBackpropsSinceLastParameterUpdate = in.readInt();
	}
	
	public abstract void applyRegularizersToParameters(ArrayList<Regularizer> regularizers, 
			ArrayList<Double> coefficients, double learningRate);
	
	/*

	@Override
	protected DoubleMatrix calculateOutput(DoubleMatrix inputVector,
			DoubleMatrix outputVector) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void updateParameterGradients(DoubleMatrix input,
			DoubleMatrix outputGradient) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected DoubleMatrix backward(DoubleMatrix inputVector,
			DoubleMatrix outputGradient) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void zeroParameterGradient() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void updateParameters(DoubleMatrix accumulatedParameterGradients,
			double learningRate) {
		// TODO Auto-generated method stub
		
	}
	*/

}
