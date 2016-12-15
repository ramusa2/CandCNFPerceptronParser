package neuralnet.test.multitagging.input;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.jblas.DoubleMatrix;

import neuralnet.test.multitagging.MultitaggerExample;
import supertagger.nn.StringEmbeddings;

public class CategoryEmbeddingContextExtractor extends StringEmbeddingContextExtractor implements Externalizable {

	protected int catToRead;
	
	public CategoryEmbeddingContextExtractor(int embeddingDimension,
			StringEmbeddings stringEmbeddings, int catToExtract) {
		super(embeddingDimension, stringEmbeddings);
		this.catToRead = catToExtract;
	}

	@Override
	protected String getKey(MultitaggerExample context) {
		return context.getItem().getCat(this.catToRead);
	}
	
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeInt(this.catToRead);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
	ClassNotFoundException {
		super.readExternal(in);
		this.catToRead = in.readInt();
	}
}
