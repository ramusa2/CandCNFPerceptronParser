package neuralnet.test.multitagging.input;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.jblas.DoubleMatrix;

import neuralnet.input.ContextExtractor;
import neuralnet.test.multitagging.MultitaggerExample;

public class SentenceLengthContextExtractor extends ContextExtractor<MultitaggerExample> implements Externalizable {
	
	private DoubleMatrix output;

	public SentenceLengthContextExtractor() {
		this.output = new DoubleMatrix(1, 1);
	}

	@Override
	public void updateParameters(DoubleMatrix gradient) {
		// No parameters to update
	}

	@Override
	public int getOutputDimension() {
		return 1;
	}

	@Override
	public DoubleMatrix extract(MultitaggerExample context) {
		this.output.put(0, context.getSentence().length());
		return this.output;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(this.output);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		this.output = (DoubleMatrix) in.readObject();
	}

}
