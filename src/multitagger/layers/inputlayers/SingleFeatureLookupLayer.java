package multitagger.layers.inputlayers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Scanner;

import org.jblas.DoubleMatrix;

import supertagger.lsbeta.MultitaggerTrainingSentence;
import multitagger.layers.MultitaggerInputLayer;

public class SingleFeatureLookupLayer extends MultitaggerInputLayer {

	protected final static String MISSING = "MISSING";

	protected HashMap<String, DoubleMatrix> lookupTable;
	
	protected HashMap<String, Integer> index;

	protected int dimension;

	protected String inputFeature;
	
	protected int inputFeatureIndex;

	protected int[] range;

	public SingleFeatureLookupLayer(int vectorDimension) {
		this.dimension = vectorDimension;
		this.range = new int[dimension];
		this.index= new HashMap<String, Integer>();
		for(int i=0; i<range.length; i++) {
			range[i] = i;
		}
		this.lookupTable = new HashMap<String, DoubleMatrix>();
		this.addFeatureValue(MISSING);
	}

	public void addFeatureValue(String feature) {
		if(!this.index.containsKey(feature)) {
			this.index.put(feature, this.index.size());
		}
		this.lookupTable.put(feature, super.getRandomVec(this.dimension));		
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(this.dimension);
		out.writeObject(this.lookupTable);
		out.writeObject(range);
		out.writeObject(this.index);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
	ClassNotFoundException {
		this.dimension = in.readInt();
		this.lookupTable = (HashMap<String, DoubleMatrix>) in.readObject();
		this.range = (int[]) in.readObject();
		this.index = (HashMap<String, Integer>) in.readObject();
	}

	@Override
	public int getOutputDimension() {
		return this.dimension*this.index.size();
	}

	@Override
	protected void fillOutputVector(DoubleMatrix output,
			MultitaggerTrainingSentence sentence, int position) {
		String cat = sentence.getItems().get(position).getCat(0);
		Integer ind = this.index.get(cat);
		if(ind == null) {
			ind = this.index.get(MISSING);
			cat = MISSING;
		}
		for(int i=0; i<this.getOutputDimension(); i++) {
			output.put(i, 0);
		}
		DoubleMatrix vec = this.getFeatureVector(cat);
		for(int i=this.dimension*ind; i<this.dimension*(ind+1); i++) {
			int j = i - this.dimension*ind;
			this.outputVector.put(i, vec.get(j));
		}
		this.inputFeatureIndex = ind;
	}

	private DoubleMatrix getFeatureVector(String feature) {
		DoubleMatrix vec = this.lookupTable.get(feature);
		if(vec == null) {
			vec = this.lookupTable.get(MISSING);
		}
		return vec;
	}

	@Override
	public void updateParameters(DoubleMatrix outputGradient,
			double learningRate) {
		outputGradient.muli(learningRate);
		DoubleMatrix vec = this.getFeatureVector(this.inputFeature);
		int offset = this.dimension*this.inputFeatureIndex;
		for(int i=0; i<this.dimension; i++) {
			vec.put(i, vec.get(i) + outputGradient.get(i+offset));
		}
	}

	@Override
	public void saveWeightsToFile(File file) {
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(file);
			for(String key : this.lookupTable.keySet()) {
				pw.print(key);
				DoubleMatrix features = this.lookupTable.get(key);
				for(int i=0; i<features.length; i++) {
					pw.print(" "+features.get(i));
				}
				pw.println();
			}
			pw.close();
		} catch (FileNotFoundException e) {
			System.out.println("Failed to save lookup layer weights to: "+file.getPath());
		}
	}

	@Override
	public void loadWeightsFromFile(File file) {
		Scanner sc = null;
		try {
			sc = new Scanner(file);
			String line;
			while(sc.hasNextLine()) {
				line = sc.nextLine().trim();
				if(!line.isEmpty()) {
					String[] toks = line.split("\\s+");
					if(toks.length == this.dimension+1) {
						DoubleMatrix weights = new DoubleMatrix(this.dimension, 1);
						for(int i=1; i<toks.length; i++) {
							weights.put(i-1, Double.parseDouble(toks[i]));
						}
						this.lookupTable.put(toks[0], weights);
					}
					else {
						System.out.println("Malformed weights file; dimensionality does not match " +
								"(expected "+this.dimension+" outputs, " +
								"read "+(toks.length-1)+" weights).\nAborting.");
						sc.close();
						return;
					}
				}
			}
			sc.close();
		} catch (FileNotFoundException e) {
			System.out.println("Failed to load lookup layer weights from: "+file.getPath());
		}
	}
}
