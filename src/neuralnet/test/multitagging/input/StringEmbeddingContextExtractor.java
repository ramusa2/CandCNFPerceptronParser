package neuralnet.test.multitagging.input;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import neuralnet.input.ContextExtractor;
import neuralnet.test.multitagging.MultitaggerExample;

import org.jblas.DoubleMatrix;

import supertagger.nn.StringEmbeddings;

public abstract class StringEmbeddingContextExtractor extends ContextExtractor<MultitaggerExample> implements Externalizable {

	private int dimension;

	private StringEmbeddings embeddings;
	
	private DoubleMatrix output;

	public StringEmbeddingContextExtractor(int embeddingDimension, StringEmbeddings stringEmbeddings) {
		this.dimension = embeddingDimension;
		this.output = new DoubleMatrix(this.dimension, 1);
		this.embeddings = stringEmbeddings;
	}

	@Override
	public DoubleMatrix extract(MultitaggerExample context) {
		this.output = this.embeddings.getVec(this.getKey(context));
		return this.output;
	}

	
	protected abstract String getKey(MultitaggerExample context);

	@Override
	public void updateParameters(DoubleMatrix gradient) {
		this.output.addi(gradient, this.output);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(this.dimension);
		out.writeObject(this.output);
		out.writeObject(this.embeddings);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
	ClassNotFoundException {
		this.dimension = in.readInt();
		this.output = (DoubleMatrix) in.readObject();
		this.embeddings = (StringEmbeddings) in.readObject();
	}

	@Override
	public int getOutputDimension() {
		return this.dimension;
	}
}
