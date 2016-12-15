package neuralnet.input;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.jblas.DoubleMatrix;
import org.jblas.ranges.RangeUtils;

public class SimpleInputLayer<T> extends InputLayer<T> implements Externalizable {
	
	protected ContextExtractor<T> extractor;
	
	public SimpleInputLayer() {}
	
	public SimpleInputLayer(ContextExtractor<T> contextExtractor, boolean standardizeFeatures) {
		super(contextExtractor.getOutputDimension(), standardizeFeatures);
		this.extractor = contextExtractor;
	}

	@Override
	protected void fillOutputVector(DoubleMatrix output, T context) {
		DoubleMatrix cxt = this.extractor.extract(context);
		if(cxt == null) {
			cxt = this.extractor.extract(context);
		}
		output.put(RangeUtils.all(), RangeUtils.all(), cxt);
	}

	@Override
	public int getOutputVectorDimension() {
		return this.extractor.getOutputDimension();
	}

	@Override
	public final void updateParameters(DoubleMatrix outputGradient) {
		// Do nothing.
	}
	
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeObject(this.extractor);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		super.readExternal(in);
		this.extractor = (ContextExtractor<T>) in.readObject();
	}

}
