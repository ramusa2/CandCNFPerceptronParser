package multitagger.layers;

import java.util.HashMap;

import org.jblas.DoubleMatrix;

import supertagger.lsbeta.MultitaggerTrainingSentence;
import supertagger.nn.StringEmbeddings;

public abstract class LookupInputLayer extends EmbeddingMultitaggerInputLayer {
	
	protected HashMap<String, DoubleMatrix> lookupTable;
	
	protected int lookupDimension;

	protected LookupInputLayer(int dim) {
		super(null, 0, null, 0);
		this.lookupDimension = dim;
		this.lookupTable = new HashMap<String, DoubleMatrix>();
	}
	
	public void addRandomVector(String entry) {
		this.lookupTable.put(entry, super.getRandomVec(this.lookupDimension));
	}

	public void addVector(String entry, DoubleMatrix vec) {
		this.lookupTable.put(entry, vec);
	}
}
