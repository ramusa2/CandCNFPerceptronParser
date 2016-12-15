package neuralnet.test.multitagging.input;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import neuralnet.input.ContextExtractor;
import neuralnet.test.multitagging.MultitaggerExample;

import org.jblas.DoubleMatrix;

public class ProbabilityContextExtractor extends ContextExtractor<MultitaggerExample> implements Externalizable {

	private int dimension;
	
	private DoubleMatrix output;

	public ProbabilityContextExtractor(int numCatsToLookAt) {
		this.dimension = numCatsToLookAt;
		this.output = new DoubleMatrix(this.dimension, 1);
	}

	@Override
	public DoubleMatrix extract(MultitaggerExample context) {
		for(int c=0; c<this.output.length; c++) {
			this.output.put(c, context.getItem().getProb(c));
		}
		return this.output;
	}

	@Override
	public void updateParameters(DoubleMatrix gradient) {
		this.output.addi(gradient, this.output);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(this.dimension);
		out.writeObject(this.output);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
	ClassNotFoundException {
		this.dimension = in.readInt();
		this.output = (DoubleMatrix) in.readObject();
	}

	@Override
	public int getOutputDimension() {
		return this.dimension;
	}
}
