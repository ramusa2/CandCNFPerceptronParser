package neuralnet.transferfunctions;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

import org.jblas.DoubleMatrix;

/**
 * Hard max transfer function
 * @author ramusa2
 *
 */
public class HardMax extends TransferFunction implements Externalizable {
	
	protected HardMax(int numInputs, int numOutputs) {
		super(numInputs, numOutputs);
	}

	private int hardMaxID = -1;

	@Override
	protected DoubleMatrix calculateOutput(DoubleMatrix inputVector,
			DoubleMatrix outputVector) {
		hardMaxID = -1;
		int i=0;
		double max = Double.NEGATIVE_INFINITY;
		for(double v : inputVector.data) {
			if(v >= max) {
				max = v;
				hardMaxID = i;
			}
		}
		if(hardMaxID > -1) {
			outputVector.put(hardMaxID, max);
		}
		return outputVector;
	}

	@Override
	protected DoubleMatrix backward(DoubleMatrix inputVector,
			DoubleMatrix outputGradient) {
		if(this.inputGradient == null) {
			this.inputGradient = new DoubleMatrix(inputVector.rows, inputVector.columns);
		}
		else {
			Arrays.fill(this.inputGradient.data, 0.0);
		}
		if(this.hardMaxID > -1) {
			this.inputGradient.put(this.hardMaxID, outputGradient.get(this.hardMaxID));
		}
		return this.inputGradient;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		super.readExternal(in);
	}

}
