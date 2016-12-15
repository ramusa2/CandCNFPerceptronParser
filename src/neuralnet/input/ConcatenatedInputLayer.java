package neuralnet.input;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

import neuralnet.FeatureNormalizer;
import neuralnet.test.multitagging.MultitaggerExample;

import org.jblas.DoubleMatrix;

public class ConcatenatedInputLayer<T> extends InputLayer<T> implements Externalizable {
	
	protected ArrayList<InputLayer<T>> segments;
	
	protected ArrayList<int[]> ranges;

	public ConcatenatedInputLayer(InputLayer<T>... inputsToConcatenate) {
		super(getDimension(inputsToConcatenate), checkStandardized(inputsToConcatenate));
		this.segments = new ArrayList<InputLayer<T>>();
		int cur = 0;
		this.ranges = new ArrayList<int[]>();
		for(InputLayer<T> l : inputsToConcatenate) {
			this.segments.add(l);
			int[] range = new int[l.getOutputVectorDimension()];
			for(int i=0; i<range.length; i++) {
				range[i] = cur++;
			}
			this.ranges.add(range);
		}
	}
	
	public ConcatenatedInputLayer(ArrayList<InputLayer> inputsToConcatenate) {
		super(getDimensionFromList(inputsToConcatenate), checkStandardizedFromList(inputsToConcatenate));
		this.segments = new ArrayList<InputLayer<T>>();
		int cur = 0;
		this.ranges = new ArrayList<int[]>();
		for(InputLayer<T> l : inputsToConcatenate) {
			this.segments.add(l);
			int[] range = new int[l.getOutputVectorDimension()];
			for(int i=0; i<range.length; i++) {
				range[i] = cur++;
			}
			this.ranges.add(range);
		}
	}

	private static boolean checkStandardized(InputLayer[] inputs) {
		for(InputLayer l : inputs) {
			if(l.standardized) {
				return true;
			}
		}
		return false;
	}
	private static boolean checkStandardizedFromList(List<InputLayer> inputs) {
		for(InputLayer l : inputs) {
			if(l.standardized) {
				return true;
			}
		}
		return false;
	}

	private static int getDimension(InputLayer[] inputs) {
		int dim = 0;
		for(InputLayer l : inputs) {
			dim += l.getOutputVectorDimension();
		}
		return dim;
	}
	
	private static int getDimensionFromList(ArrayList<InputLayer> inputs) {
		int dim = 0;
		for(InputLayer l : inputs) {
			dim += l.getOutputVectorDimension();
		}
		return dim;
	}

	@Override
	public void learnFeatureNormalizers(ArrayList<T> data) {
		for(InputLayer<T> l : this.segments) {
			if(l.standardized) {
				l.learnFeatureNormalizers(data);
			}
		}
	}

	@Override
	protected void fillOutputVector(DoubleMatrix output, T context) {
		int segIndex = 0;
		for(InputLayer<T> l : this.segments) {
			DoubleMatrix vec = l.getOutput(context);
			output.put(this.ranges.get(segIndex++), vec);
		}
	}

	@Override
	public int getOutputVectorDimension() {
		return this.dimension;
	}

	@Override
	public void updateParameters(DoubleMatrix outputGradient) {
		int segIndex = 0;
		for(InputLayer<T> l : this.segments) {
			l.updateParameters(outputGradient.get(this.ranges.get(segIndex++)));
		}
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeObject(this.segments);
		out.writeObject(this.ranges);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
	ClassNotFoundException {
		super.readExternal(in);
		this.segments = (ArrayList<InputLayer<T>>) in.readObject();
		this.ranges = (ArrayList<int[]>) in.readObject();
	}

}
